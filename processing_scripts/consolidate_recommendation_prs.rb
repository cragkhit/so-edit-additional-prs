require "csv"
require "uri"

SOURCE_CSV = ARGV[0] || "matcha_recommendation_github_files.csv"
TARGET_CSV = ARGV[1] || "matcha_results_2024-05-07_manual_validation.csv"

def read_mixed_line_endings_csv(path)
  text = File.binread(path).gsub("\r\n", "\n")
  CSV.parse(text, headers: true, liberal_parsing: true, row_sep: "\n")
end

def github_identity(url)
  uri = URI.parse(url.to_s.strip)
  parts = uri.path.split("/").reject(&:empty?)
  raise "Unexpected GitHub URL: #{url.inspect}" if parts.length < 5 || parts[2] != "blob"
  {
    owner: parts[0].downcase,
    repo: parts[1].downcase,
    basename: parts.last,
    url_path: uri.path
  }
end

recommendations = read_mixed_line_endings_csv(SOURCE_CSV)
target = CSV.read(TARGET_CSV, headers: true)

matches = {}
recommendations.each do |recommendation|
  github = github_identity(recommendation["github_file_url"])
  question_id = recommendation["stackoverflow_url"].to_s[/\d+/]
  recommendation_basename = File.basename(
    recommendation["recommendation_file"].to_s
  )
  candidates = target.select do |row|
    file_parts = row["GitHub Java File Path"].to_s.split("/")
    row_question_id = row["Link SO"].to_s[/\d+/]
    file_parts[0].to_s.downcase == github[:owner] &&
      file_parts[1].to_s.downcase == github[:repo] &&
      row_question_id == question_id
  end

  # Handle repository transfers/renames by falling back to the exact relative
  # file path and Stack Overflow question ID.
  if candidates.empty?
    candidates = target.select do |row|
      relative = row["GitHub Java File Path"].to_s.split("/").drop(2).join("/")
      row_question_id = row["Link SO"].to_s[/\d+/]
      row_question_id == question_id &&
        github[:url_path].end_with?("/#{relative}")
    end
  end

  suffix_matches = candidates.select do |row|
    relative = row["GitHub Java File Path"].to_s.split("/").drop(2).join("/")
    github[:url_path].end_with?("/#{relative}")
  end
  candidates = suffix_matches unless suffix_matches.empty?

  basename_matches = candidates.select do |row|
    File.basename(row["GitHub Java File Path"].to_s) == github[:basename]
  end
  candidates = basename_matches unless basename_matches.empty?

  snippet_matches = candidates.select do |row|
    File.basename(row["Stack Overflow Java File Path"].to_s) ==
      recommendation_basename
  end
  candidates = snippet_matches unless snippet_matches.empty?

  if candidates.empty?
    raise "Recommendation #{recommendation['id']} has no match"
  end
  candidates.each do |row|
    key = row["No"]
    if matches.key?(key)
      raise "Multiple recommendation records map to consolidated No #{key}"
    end
    matches[key] = recommendation
  end
end

link_header = "Open PR Link"
status_header = "Recommendation PR Status"
reason_header = "Recommendation PR Reason"
removed_header = "Recommendation PR Link"
added_headers = [status_header, reason_header]
headers = target.headers.reject do |header|
  header == removed_header || added_headers.include?(header)
end + added_headers

output_rows = target.map do |row|
  recommendation = matches[row["No"]]
  pr_value = recommendation&.[]("pr_url").to_s.strip
  link = pr_value.match?(%r{\Ahttps://github\.com/.+/pull/\d+\z}) ? pr_value : nil
  status = if recommendation
             pr_value == "SKIPPED" ? "SKIPPED" : "SUBMITTED"
           end
  reason = recommendation&.[]("notes").to_s.strip

  headers.map do |header|
    case header
    when link_header then row[link_header].to_s.strip.empty? ? link : row[link_header]
    when status_header then status
    when reason_header then reason.empty? ? nil : reason
    else row[header]
    end
  end
end

CSV.open(TARGET_CSV, "wb", write_headers: true, headers: headers) do |csv|
  output_rows.each { |row| csv << row }
end

puts "Recommendation inventory records: #{recommendations.length}"
puts "Consolidated rows updated: #{matches.length}"
puts "Submitted PR links copied: #{recommendations.count { |row| row['pr_url'].to_s.start_with?('https://') }}"
puts "Skipped reasons copied: #{recommendations.count { |row| row['pr_url'].to_s.strip == 'SKIPPED' }}"
puts "Updated #{TARGET_CSV}"
