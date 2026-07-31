#!/usr/bin/env ruby
# frozen_string_literal: true

require "csv"
require "fileutils"
require "optparse"
require_relative "lib/clone_analyzer"

root = File.expand_path("..", __dir__)
options = {
  input: File.join(__dir__, "clone_pairs.csv"),
  snippets: File.join(root, "matcha-review-app", "public", "pairs.json"),
  output: File.join(__dir__, "clone_analysis_results.csv"),
  limit: nil
}

OptionParser.new do |parser|
  parser.banner = "Usage: ruby clone_quality/analyze_clones.rb [options]"
  parser.on("--input PATH", "Compact pair CSV") { |value| options[:input] = value }
  parser.on("--snippets PATH", "JSON containing extracted snippets") { |value| options[:snippets] = value }
  parser.on("--output PATH", "Analysis output CSV") { |value| options[:output] = value }
  parser.on("--limit N", Integer, "Analyze only the first N pairs") { |value| options[:limit] = value }
end.parse!

pairs = CloneQuality::PairLoader.from_json(options[:input], options[:snippets])
pairs = pairs.first(options[:limit]) if options[:limit]
analyzer = CloneQuality::Analyzer.new
results = pairs.map.with_index do |pair, index|
  warn "Analyzing #{index + 1}/#{pairs.length}" if ((index + 1) % 100).zero?
  analyzer.analyze(pair)
end

FileUtils.mkdir_p(File.dirname(File.expand_path(options[:output])))
CSV.open(options[:output], "w", write_headers: true, headers: CloneQuality::Analyzer::RESULT_COLUMNS) do |csv|
  results.each { |result| csv << CloneQuality::Analyzer::RESULT_COLUMNS.map { |column| result[column] } }
end

counts = results.group_by { |result| [result["Clone Decision"], result["Clone Type"]] }
puts "Wrote #{results.length} rows to #{options[:output]}"
counts.sort.each { |key, rows| puts "#{key.reject(&:empty?).join("/").ljust(22)} #{rows.length}" }
