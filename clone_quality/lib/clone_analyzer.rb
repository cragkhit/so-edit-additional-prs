# frozen_string_literal: true

require "csv"
require "digest"
require "json"

module CloneQuality
  Token = Struct.new(:kind, :value, :line, keyword_init: true)

  JAVA_KEYWORDS = %w[
    abstract assert boolean break byte case catch char class const continue
    default do double else enum extends final finally float for goto if
    implements import instanceof int interface long native new package private
    protected public return short static strictfp super switch synchronized
    this throw throws transient try void volatile while true false null record
    sealed permits non-sealed var yield
  ].freeze

  MULTI_CHAR_OPERATORS = %w[
    >>>= <<= >>= ... :: -> == != <= >= && || ++ -- += -= *= /= %= &= |= ^=
    << >> >>> ?.
  ].sort_by { |operator| -operator.length }.freeze

  TYPE_KEYWORDS = %w[boolean byte char double float int long short void var].freeze

  class JavaLexer
    def tokenize(source, start_line: 1)
      tokens = []
      index = 0
      line = start_line

      while index < source.length
        case source[index]
        when " ", "\t", "\r", "\f"
          index += 1
        when "\n"
          line += 1
          index += 1
        when "/"
          if source[index, 2] == "//"
            index = source.index("\n", index + 2) || source.length
          elsif source[index, 2] == "/*"
            index, line = skip_block_comment(source, index + 2, line)
          else
            tokens << Token.new(kind: :symbol, value: "/", line: line)
            index += 1
          end
        when '"', "'"
          value, index, line = read_quoted(source, index, line)
          tokens << Token.new(kind: :literal, value: value, line: line)
        else
          if source[index].match?(/[A-Za-z_$]/)
            value, index = read_while(source, index) { |char| char.match?(/[A-Za-z0-9_$]/) }
            kind = JAVA_KEYWORDS.include?(value) ? :keyword : :identifier
            tokens << Token.new(kind: kind, value: value, line: line)
          elsif source[index].match?(/[0-9]/)
            match = source[index..].match(/\A(?:0[xX][0-9A-Fa-f_]+|0[bB][01_]+|(?:\d[\d_]*)(?:\.\d[\d_]*)?(?:[eE][+\-]?\d[\d_]*)?)[fFdDlL]?/)
            value = match[0]
            index += value.length
            tokens << Token.new(kind: :literal, value: value, line: line)
          else
            operator = MULTI_CHAR_OPERATORS.find { |candidate| source[index, candidate.length] == candidate }
            value = operator || source[index]
            tokens << Token.new(kind: :symbol, value: value, line: line)
            index += value.length
          end
        end
      end
      tokens
    end

    private

    def read_while(source, index)
      finish = index
      finish += 1 while finish < source.length && yield(source[finish])
      [source[index...finish], finish]
    end

    def skip_block_comment(source, index, line)
      while index < source.length
        line += 1 if source[index] == "\n"
        return [index + 2, line] if source[index, 2] == "*/"
        index += 1
      end
      [index, line]
    end

    def read_quoted(source, index, line)
      quote = source[index]
      finish = index + 1
      escaped = false
      while finish < source.length
        char = source[finish]
        line += 1 if char == "\n"
        finish += 1
        if escaped
          escaped = false
        elsif char == "\\"
          escaped = true
        elsif char == quote
          break
        end
      end
      [source[index...finish], finish, line]
    end
  end

  class Analyzer
    DEFAULTS = {
      min_shared_tokens: 50,
      min_shared_statements: 5,
      type3_short_coverage: 0.70,
      type3_span_coverage: 0.50,
      uncertain_jaccard: 0.45
    }.freeze

    RESULT_COLUMNS = [
      "No", "GH Project", "Link SO",
      "GitHub Java File Path", "GitHub Method Name", "GitHub Start Line", "GitHub End Line",
      "Stack Overflow Java File Path", "Stack Overflow Method Name",
      "Stack Overflow Start Line", "Stack Overflow End Line",
      "Clone Decision", "Clone Type",
      "GH Matched Start Line", "GH Matched End Line",
      "SO Matched Start Line", "SO Matched End Line",
      "Coverage GH", "Coverage SO", "Text Similarity",
      "Parameterized Similarity", "Structural Similarity",
      "Semantic Evidence", "Confidence", "Reason", "Review Required",
      "Parse Mode GH", "Parse Mode SO",
      "GitHub Snippet SHA256", "Stack Overflow Snippet SHA256"
    ].freeze

    def initialize(config = {})
      @config = DEFAULTS.merge(config.transform_keys(&:to_sym))
      @lexer = JavaLexer.new
    end

    def analyze(pair)
      gh = prepare(pair.fetch("github"))
      so = prepare(pair.fetch("stackoverflow"), stackoverflow: true)

      exact = containment_match(gh[:tokens], so[:tokens], mode: :exact)
      return result_for_match(pair, gh, so, exact, "TYPE_1") if exact

      parameterized = containment_match(gh[:tokens], so[:tokens], mode: :parameterized)
      return result_for_match(pair, gh, so, parameterized, "TYPE_2") if parameterized

      structural = structural_evidence(gh, so)
      metrics = similarity_metrics(gh[:tokens], so[:tokens], structural)

      if type3?(structural)
        result_for_type3(pair, gh, so, structural, metrics)
      elsif metrics[:parameterized] >= @config[:uncertain_jaccard]
        base_result(pair, gh, so).merge(
          "Clone Decision" => "UNCERTAIN",
          "Clone Type" => "",
          "Coverage GH" => format_score(structural[:coverage_gh]),
          "Coverage SO" => format_score(structural[:coverage_so]),
          "Text Similarity" => format_score(metrics[:text]),
          "Parameterized Similarity" => format_score(metrics[:parameterized]),
          "Structural Similarity" => format_score(metrics[:structural]),
          "Confidence" => "LOW",
          "Reason" => "Similarity is suggestive but does not satisfy the conservative Type III rule.",
          "Review Required" => "YES"
        )
      else
        base_result(pair, gh, so).merge(
          "Clone Decision" => "NOT_CLONE",
          "Clone Type" => "",
          "Coverage GH" => format_score(structural[:coverage_gh]),
          "Coverage SO" => format_score(structural[:coverage_so]),
          "Text Similarity" => format_score(metrics[:text]),
          "Parameterized Similarity" => format_score(metrics[:parameterized]),
          "Structural Similarity" => format_score(metrics[:structural]),
          "Confidence" => "MEDIUM",
          "Reason" => "No Type I/II containment or substantial ordered statement core was found.",
          "Review Required" => "NO"
        )
      end
    end

    private

    def prepare(fragment, stackoverflow: false)
      raw = fragment.fetch("code")
      cleaned = stackoverflow ? clean_stackoverflow(raw) : raw
      tokens = @lexer.tokenize(cleaned, start_line: fragment.fetch("startLine").to_i)
      {
        raw: raw,
        tokens: tokens,
        statements: statements(tokens),
        parse_mode: parse_mode(tokens)
      }
    end

    def clean_stackoverflow(source)
      source.lines.reject do |line|
        stripped = line.strip
        stripped.match?(/\A```/) ||
          stripped.match?(/\A-{5,}\z/) ||
          stripped.match?(/\A\#{1,6}\s+/) ||
          stripped.match?(/\A\d+\.\s+(update|add|change|create)\b/i) ||
          stripped.match?(/\A!\[.*\]\(.*\)\z/)
      end.join.gsub(/^\s*`|`\s*$/, "")
    end

    def parse_mode(tokens)
      return "NO_JAVA_TOKENS" if tokens.empty?

      pairs = { "{" => "}", "(" => ")", "[" => "]" }
      stack = []
      valid = tokens.all? do |token|
        if pairs.key?(token.value)
          stack << pairs[token.value]
        elsif pairs.value?(token.value)
          break false unless stack.pop == token.value
        end
        true
      end
      valid && stack.empty? ? "BALANCED_TOKEN_SEQUENCE" : "TOKEN_FALLBACK"
    end

    def exact_value(token)
      "#{token.kind}:#{token.value}"
    end

    def loose_value(token)
      case token.kind
      when :identifier then "IDENTIFIER"
      when :literal then "LITERAL"
      when :keyword
        TYPE_KEYWORDS.include?(token.value) ? "TYPE" : "keyword:#{token.value}"
      else "#{token.kind}:#{token.value}"
      end
    end

    def containment_match(gh_tokens, so_tokens, mode:)
      return nil if gh_tokens.empty? || so_tokens.empty?

      shorter_name, shorter, longer_name, longer =
        if gh_tokens.length <= so_tokens.length
          [:github, gh_tokens, :stackoverflow, so_tokens]
        else
          [:stackoverflow, so_tokens, :github, gh_tokens]
        end
      mapper = mode == :exact ? method(:exact_value) : method(:loose_value)
      pattern = shorter.map(&mapper)
      text = longer.map(&mapper)

      kmp_occurrences(text, pattern).each do |start|
        candidate = longer[start, shorter.length]
        next if mode == :parameterized && !consistent_parameterization?(shorter, candidate)

        ranges = {
          shorter_name => token_range(shorter, 0, shorter.length - 1),
          longer_name => token_range(longer, start, start + shorter.length - 1)
        }
        return {
          ranges: ranges,
          coverage_gh: shorter_name == :github ? 1.0 : shorter.length.fdiv(gh_tokens.length),
          coverage_so: shorter_name == :stackoverflow ? 1.0 : shorter.length.fdiv(so_tokens.length)
        }
      end
      nil
    end

    def kmp_occurrences(text, pattern)
      return [] if pattern.empty? || pattern.length > text.length

      prefix = Array.new(pattern.length, 0)
      cursor = 0
      (1...pattern.length).each do |index|
        cursor = prefix[cursor - 1] while cursor.positive? && pattern[index] != pattern[cursor]
        cursor += 1 if pattern[index] == pattern[cursor]
        prefix[index] = cursor
      end
      matches = []
      cursor = 0
      text.each_with_index do |value, index|
        cursor = prefix[cursor - 1] while cursor.positive? && value != pattern[cursor]
        cursor += 1 if value == pattern[cursor]
        if cursor == pattern.length
          matches << index - pattern.length + 1
          cursor = prefix[cursor - 1]
        end
      end
      matches
    end

    def consistent_parameterization?(left, right)
      left_to_right = {}
      right_to_left = {}
      left.zip(right).all? do |a, b|
        next true unless a.kind == :identifier && b.kind == :identifier

        return false if left_to_right[a.value] && left_to_right[a.value] != b.value
        return false if right_to_left[b.value] && right_to_left[b.value] != a.value

        left_to_right[a.value] = b.value
        right_to_left[b.value] = a.value
        true
      end
    end

    def statements(tokens)
      result = []
      start = 0
      tokens.each_with_index do |token, index|
        next unless %w[; { }].include?(token.value)

        slice = tokens[start..index]
        result << statement(slice) unless slice.empty?
        start = index + 1
      end
      result << statement(tokens[start..]) if start < tokens.length
      result
    end

    def statement(tokens)
      {
        signature: tokens.map { |token| loose_value(token) }.join("\u001F"),
        tokens: tokens,
        start_line: tokens.first.line,
        end_line: tokens.last.line
      }
    end

    def structural_evidence(gh, so)
      matches = lcs_matches(
        gh[:statements].map { |statement| statement[:signature] },
        so[:statements].map { |statement| statement[:signature] }
      )
      return empty_structural if matches.empty?

      gh_indices = matches.map(&:first)
      so_indices = matches.map(&:last)
      gh_span = gh_indices.max - gh_indices.min + 1
      so_span = so_indices.max - so_indices.min + 1
      shared_tokens = matches.sum do |gh_index, so_index|
        [gh[:statements][gh_index][:tokens].length, so[:statements][so_index][:tokens].length].min
      end
      {
        matched_statements: matches.length,
        shared_tokens: shared_tokens,
        coverage_gh: matches.length.fdiv([gh[:statements].length, 1].max),
        coverage_so: matches.length.fdiv([so[:statements].length, 1].max),
        span_coverage_gh: matches.length.fdiv(gh_span),
        span_coverage_so: matches.length.fdiv(so_span),
        ranges: {
          github: {
            start: gh[:statements][gh_indices.min][:start_line],
            end: gh[:statements][gh_indices.max][:end_line]
          },
          stackoverflow: {
            start: so[:statements][so_indices.min][:start_line],
            end: so[:statements][so_indices.max][:end_line]
          }
        }
      }
    end

    def empty_structural
      {
        matched_statements: 0, shared_tokens: 0,
        coverage_gh: 0.0, coverage_so: 0.0,
        span_coverage_gh: 0.0, span_coverage_so: 0.0,
        ranges: {}
      }
    end

    def lcs_matches(left, right)
      rows = Array.new(left.length + 1) { Array.new(right.length + 1, 0) }
      left.each_index do |i|
        right.each_index do |j|
          rows[i + 1][j + 1] = if left[i] == right[j]
                                 rows[i][j] + 1
                               else
                                 [rows[i][j + 1], rows[i + 1][j]].max
                               end
        end
      end
      matches = []
      i = left.length
      j = right.length
      while i.positive? && j.positive?
        if left[i - 1] == right[j - 1]
          matches << [i - 1, j - 1]
          i -= 1
          j -= 1
        elsif rows[i - 1][j] >= rows[i][j - 1]
          i -= 1
        else
          j -= 1
        end
      end
      matches.reverse
    end

    def type3?(evidence)
      enough_size =
        evidence[:shared_tokens] >= @config[:min_shared_tokens] ||
        evidence[:matched_statements] >= @config[:min_shared_statements]
      shorter_coverage = [evidence[:coverage_gh], evidence[:coverage_so]].max
      span_coverage = [evidence[:span_coverage_gh], evidence[:span_coverage_so]].min
      enough_size &&
        shorter_coverage >= @config[:type3_short_coverage] &&
        span_coverage >= @config[:type3_span_coverage]
    end

    def similarity_metrics(gh_tokens, so_tokens, structural)
      {
        text: jaccard(gh_tokens.map { |token| exact_value(token) }, so_tokens.map { |token| exact_value(token) }),
        parameterized: jaccard(gh_tokens.map { |token| loose_value(token) }, so_tokens.map { |token| loose_value(token) }),
        structural: [structural[:coverage_gh], structural[:coverage_so]].max
      }
    end

    def jaccard(left, right)
      left_counts = left.each_with_object(Hash.new(0)) { |value, counts| counts[value] += 1 }
      right_counts = right.each_with_object(Hash.new(0)) { |value, counts| counts[value] += 1 }
      keys = left_counts.keys | right_counts.keys
      intersection = keys.sum { |key| [left_counts[key].to_i, right_counts[key].to_i].min }
      union = keys.sum { |key| [left_counts[key].to_i, right_counts[key].to_i].max }
      union.zero? ? 0.0 : intersection.fdiv(union)
    end

    def result_for_match(pair, gh, so, match, type)
      metrics = similarity_metrics(gh[:tokens], so[:tokens], empty_structural)
      base_result(pair, gh, so).merge(
        "Clone Decision" => "CLONE",
        "Clone Type" => type,
        "GH Matched Start Line" => match.dig(:ranges, :github, :start),
        "GH Matched End Line" => match.dig(:ranges, :github, :end),
        "SO Matched Start Line" => match.dig(:ranges, :stackoverflow, :start),
        "SO Matched End Line" => match.dig(:ranges, :stackoverflow, :end),
        "Coverage GH" => format_score(match[:coverage_gh]),
        "Coverage SO" => format_score(match[:coverage_so]),
        "Text Similarity" => format_score(metrics[:text]),
        "Parameterized Similarity" => format_score(metrics[:parameterized]),
        "Structural Similarity" => "1.0000",
        "Confidence" => "HIGH",
        "Reason" => type == "TYPE_1" ?
          "A complete fragment occurs verbatim after removing comments and layout." :
          "A complete fragment has identical normalized syntax with consistent identifier mapping.",
        "Review Required" => "NO"
      )
    end

    def result_for_type3(pair, gh, so, structural, metrics)
      base_result(pair, gh, so).merge(
        "Clone Decision" => "CLONE",
        "Clone Type" => "TYPE_3",
        "GH Matched Start Line" => structural.dig(:ranges, :github, :start),
        "GH Matched End Line" => structural.dig(:ranges, :github, :end),
        "SO Matched Start Line" => structural.dig(:ranges, :stackoverflow, :start),
        "SO Matched End Line" => structural.dig(:ranges, :stackoverflow, :end),
        "Coverage GH" => format_score(structural[:coverage_gh]),
        "Coverage SO" => format_score(structural[:coverage_so]),
        "Text Similarity" => format_score(metrics[:text]),
        "Parameterized Similarity" => format_score(metrics[:parameterized]),
        "Structural Similarity" => format_score(metrics[:structural]),
        "Confidence" => "MEDIUM",
        "Reason" => "A substantial ordered core of normalized statements was found with additions, deletions, or changes.",
        "Review Required" => "YES"
      )
    end

    def base_result(pair, gh, so)
      metadata = pair.fetch("metadata")
      {
        "No" => metadata["No"],
        "GH Project" => metadata["GH Project"],
        "Link SO" => metadata["Link SO"],
        "GitHub Java File Path" => pair.dig("github", "path"),
        "GitHub Method Name" => pair.dig("github", "method"),
        "GitHub Start Line" => pair.dig("github", "startLine"),
        "GitHub End Line" => pair.dig("github", "endLine"),
        "Stack Overflow Java File Path" => pair.dig("stackoverflow", "path"),
        "Stack Overflow Method Name" => pair.dig("stackoverflow", "method"),
        "Stack Overflow Start Line" => pair.dig("stackoverflow", "startLine"),
        "Stack Overflow End Line" => pair.dig("stackoverflow", "endLine"),
        "Semantic Evidence" => "",
        "Parse Mode GH" => gh[:parse_mode],
        "Parse Mode SO" => so[:parse_mode],
        "GitHub Snippet SHA256" => Digest::SHA256.hexdigest(gh[:raw]),
        "Stack Overflow Snippet SHA256" => Digest::SHA256.hexdigest(so[:raw])
      }
    end

    def token_range(tokens, start_index, end_index)
      { start: tokens[start_index].line, end: tokens[end_index].line }
    end

    def format_score(score)
      format("%.4f", score)
    end
  end

  class PairLoader
    def self.from_json(csv_path, snippets_path)
      metadata = CSV.read(csv_path, headers: true).map(&:to_h)
      snippets = JSON.parse(File.read(snippets_path, encoding: "utf-8"))
      unless metadata.length == snippets.length
        raise "Row mismatch: CSV has #{metadata.length}, snippets have #{snippets.length}"
      end

      metadata.zip(snippets).map do |row, snippet|
        unless row["No"].to_s == snippet["no"].to_s
          raise "Pair order mismatch: CSV No=#{row["No"]}, snippets No=#{snippet["no"]}"
        end
        {
          "metadata" => row,
          "github" => snippet.fetch("github"),
          "stackoverflow" => snippet.fetch("stackoverflow")
        }
      end
    end
  end
end
