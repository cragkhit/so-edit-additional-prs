require "csv"

SOURCE_CSV = ARGV[0] || "matcha_results_2024-05-07.csv"
OUTPUT_CSV = ARGV[1] || "matcha_results_2024-05-07_manual_validation.csv"

source = if File.exist?(SOURCE_CSV)
           CSV.read(SOURCE_CSV, headers: false)
         end
output = CSV.read(OUTPUT_CSV, headers: true)

required_headers = [
  "No",
  "GH Project",
  "Link SO",
  "GitHub Java File Path",
  "GitHub Method Name",
  "GitHub Start Line",
  "GitHub End Line",
  "Stack Overflow Java File Path",
  "Stack Overflow Method Name",
  "Stack Overflow Start Line",
  "Stack Overflow End Line",
  "Match Status",
  "Useful Pairs Cross-Check",
  "Open PR Link",
  "Old PR Status",
  "Recommendation PR Status",
  "Recommendation PR Reason"
]
missing_headers = required_headers - output.headers
raise "Missing headers: #{missing_headers.join(', ')}" unless missing_headers.empty?
expected_rows = source ? source.length : 793
unless output.length == expected_rows
  raise "Expected #{expected_rows} output rows, found #{output.length}"
end

def split_source_path(path, dataset_directory)
  relative = path.sub(%r{\A/root/#{Regexp.escape(dataset_directory)}/}, "")
  match = relative.match(/\A(.+\.java)_([^#]+)#(\d+)#(\d+)\z/)
  raise "Unexpected source path: #{path}" unless match
  [match[1], match[2], match[3], match[4]]
end

actual = output.map do |row|
  [
    row["GitHub Java File Path"],
    row["GitHub Method Name"],
    row["GitHub Start Line"],
    row["GitHub End Line"],
    row["Stack Overflow Java File Path"],
    row["Stack Overflow Method Name"],
    row["Stack Overflow Start Line"],
    row["Stack Overflow End Line"]
  ]
end
if source
  expected = source.map do |github_path, so_path|
    split_source_path(github_path, "2_github_projects_for_search") +
      split_source_path(so_path, "3_stackoverflow_snippets_for_index")
  end
  raise "Split source fields or their order changed" unless actual == expected
end

raise "Duplicate workbook No values" unless output.map { |row| row["No"] }.uniq.length == output.length
raise "Blank split path field" if actual.any? do |fields|
  fields.any? { |field| field.to_s.empty? }
end

question_mismatches = output.count do |row|
  link_id = row["Link SO"].to_s[/\d+/]
  path_id = row["Stack Overflow Java File Path"].to_s[%r{\A(\d+)/}, 1]
  link_id != path_id
end
raise "#{question_mismatches} Stack Overflow ID mismatches" unless question_mismatches.zero?

project_mismatches = output.count do |row|
  owner = row["GitHub Java File Path"].to_s.split("/").first.to_s
  row["GH Project"].to_s.downcase != owner.downcase
end
raise "#{project_mismatches} GitHub project mismatches" unless project_mismatches.zero?

statuses = output.group_by { |row| row["Match Status"] }.transform_values(&:length)
cross_check_values = output.map { |row| row["Useful Pairs Cross-Check"].to_s }.uniq
unless (cross_check_values - ["", "FOUND"]).empty?
  raise "Unexpected Useful Pairs Cross-Check value"
end
found_count = output.count { |row| row["Useful Pairs Cross-Check"] == "FOUND" }
pr_links = output.map { |row| row["Open PR Link"].to_s.strip }.reject(&:empty?)
raise "Expected 55 Open PR links, found #{pr_links.length}" unless pr_links.length == 55
unless pr_links.all? { |link| link.match?(%r{\Ahttps://github\.com/.+/pull/\d+\z}) }
  raise "Invalid Open PR link format"
end
old_pr_count = output.count { |row| row["Old PR Status"] == "OLD" }
raise "Expected 36 OLD PR records, found #{old_pr_count}" unless old_pr_count == 36
unexpected_old_values = output.map { |row| row["Old PR Status"].to_s }.uniq - ["", "OLD"]
raise "Unexpected Old PR Status value" unless unexpected_old_values.empty?
recommendation_skips = output.count do |row|
  row["Recommendation PR Status"] == "SKIPPED"
end
raise "Expected 742 rows with skipped recommendations" unless recommendation_skips == 742
missing_skip_reasons = output.count do |row|
  row["Recommendation PR Status"] == "SKIPPED" &&
    row["Recommendation PR Reason"].to_s.strip.empty?
end
raise "Skipped recommendation without a reason" unless missing_skip_reasons.zero?
puts "Validation passed"
puts "Rows: #{output.length}"
puts "Statuses: #{statuses}"
puts(source ? "Split source fields preserved: true" : "Original source comparison: skipped (source CSV unavailable)")
puts "Stack Overflow ID mismatches: 0"
puts "GitHub project mismatches: 0"
puts "Useful Pairs marked FOUND: #{found_count}"
puts "Open PR links: #{pr_links.length}"
puts "Old PR records: #{old_pr_count}"
puts "Recommendation skip reasons: #{recommendation_skips}"
