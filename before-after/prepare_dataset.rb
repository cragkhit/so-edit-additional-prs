# frozen_string_literal: true

require "csv"
require "digest"
require "fileutils"

workspace = File.expand_path("..", __dir__)
output_root = File.join(__dir__, "dataset")
source_csv = ARGV[0] || File.join(workspace, "matcha_results_2024-05-07_manual_validation_FINAL.csv")
validation_csv = ARGV[1] || File.join(workspace, "manual_validation_result_with_pair_paths.csv")
revision_root = ARGV[2] || "/Users/chaiyong/Downloads/do_not_delete/Matcha_Study/java_files"

before_dir = File.join(output_root, "before")
after_dir = File.join(output_root, "after")
FileUtils.mkdir_p(before_dir)
FileUtils.mkdir_p(after_dir)

source_rows = CSV.read(source_csv, headers: true, encoding: "bom|utf-8")
validation_rows = CSV.read(validation_csv, headers: true, encoding: "bom|utf-8")
validation_by_no = validation_rows.to_h { |row| [row["No"].to_s, row] }

def parse_block_key(relative_path)
  match = File.basename(relative_path).match(/\A(\d+)_(\d+)_/)
  raise "Unrecognized Stack Overflow filename: #{relative_path}" unless match

  {
    question_id: match[1],
    block_number: match[2],
    snippet_id: "so_#{match[1]}_block_#{match[2]}"
  }
end

def file_metadata(path)
  content = File.binread(path)
  {
    sha256: Digest::SHA256.hexdigest(content),
    bytes: content.bytesize,
    lines: content.empty? ? 0 : content.lines.count
  }
end

groups = {}
source_rows.each do |row|
  relative_path = row["Stack Overflow Java File Path"]
  identity = parse_block_key(relative_path)
  group = groups[identity[:snippet_id]] ||= identity.merge(rows: [], selected_paths: [])
  group[:rows] << row
  group[:selected_paths] << relative_path unless group[:selected_paths].include?(relative_path)
end

pair_manifest = []
groups.keys.sort_by { |id| id.scan(/\d+/).map(&:to_i) }.each do |snippet_id|
  group = groups.fetch(snippet_id)
  question_id = group[:question_id]
  block_number = group[:block_number]
  directory = File.join(revision_root, question_id)
  original_source = File.join(directory, "#{question_id}_#{block_number}_original.java")
  recent_source = File.join(directory, "#{question_id}_#{block_number}_recent.java")

  selected_relative = if group[:selected_paths].any? { |path| path.end_with?("_recent.java") }
                        group[:selected_paths].find { |path| path.end_with?("_recent.java") }
                      else
                        group[:selected_paths].first
                      end
  selected_source = File.join(revision_root, selected_relative)
  after_source = File.file?(recent_source) ? recent_source : selected_source
  after_rule = File.file?(recent_source) ? "RECENT_SNAPSHOT" : "SELECTED_LATEST_AVAILABLE"

  missing = []
  missing << "MISSING_ORIGINAL_SNAPSHOT" unless File.file?(original_source)
  missing << "MISSING_AFTER_SNAPSHOT" unless File.file?(after_source)
  status = missing.empty? ? "ELIGIBLE" : "EXCLUDED"

  before_relative = status == "ELIGIBLE" ? "dataset/before/#{snippet_id}.java" : ""
  after_relative = status == "ELIGIBLE" ? "dataset/after/#{snippet_id}.java" : ""

  before_meta = {}
  after_meta = {}
  if status == "ELIGIBLE"
    before_target = File.join(__dir__, before_relative)
    after_target = File.join(__dir__, after_relative)
    FileUtils.cp(original_source, before_target)
    FileUtils.cp(after_source, after_target)
    before_meta = file_metadata(before_target)
    after_meta = file_metadata(after_target)
  end

  validations = group[:rows].map { |row| validation_by_no[row["No"].to_s] }.compact
  accepted = validations.count { |row| row["Result Compare Afte Resolve Conflict"].to_s.include?("Yes") }
  rejected = validations.count { |row| row["Result Compare Afte Resolve Conflict"].to_s.include?("No") }
  validation_group = if accepted.positive? && rejected.positive?
                       "MIXED"
                     elsif accepted.positive?
                       "ALL_ACCEPTED"
                     elsif rejected.positive?
                       "ALL_REJECTED"
                     else
                       "NO_VALIDATION"
                     end
  recommendation_types = validations
    .map { |row| row["Recommendation Type (Summary)"].to_s.strip }
    .reject(&:empty?)
    .map { |value| value.downcase == "bug fixing" ? "Bug Fixing" : value }
    .uniq
    .sort

  pair_manifest << {
    "Snippet ID" => snippet_id,
    "Stack Overflow Question ID" => question_id,
    "Code Block Number" => block_number,
    "Status" => status,
    "Exclusion Reason" => missing.join(";"),
    "Before Dataset Path" => before_relative,
    "After Dataset Path" => after_relative,
    "Original Source Path" => File.file?(original_source) ? original_source : "",
    "After Source Path" => File.file?(after_source) ? after_source : "",
    "After Selection Rule" => after_rule,
    "Selected Paths In Study" => group[:selected_paths].join(";"),
    "Before SHA256" => before_meta[:sha256].to_s,
    "After SHA256" => after_meta[:sha256].to_s,
    "Before Lines" => before_meta[:lines].to_s,
    "After Lines" => after_meta[:lines].to_s,
    "Before Bytes" => before_meta[:bytes].to_s,
    "After Bytes" => after_meta[:bytes].to_s,
    "Identical Before After" => status == "ELIGIBLE" ? (before_meta[:sha256] == after_meta[:sha256] ? "YES" : "NO") : "",
    "Study Pair Count" => group[:rows].length,
    "Study Pair IDs" => group[:rows].map { |row| row["No"] }.join(";"),
    "Accepted Pair Count" => accepted,
    "Rejected Pair Count" => rejected,
    "Validation Group" => validation_group,
    "Recommendation Types" => recommendation_types.join(";")
  }
end

pair_columns = pair_manifest.first.keys
CSV.open(File.join(output_root, "snippet_pairs.csv"), "w", write_headers: true, headers: pair_columns) do |csv|
  pair_manifest.each { |row| csv << pair_columns.map { |column| row[column] } }
end

manifest_by_snippet = pair_manifest.to_h { |row| [row["Snippet ID"], row] }
mapping_columns = [
  "No", "GH Project", "Link SO", "Snippet ID", "Dataset Status",
  "Before Dataset Path", "After Dataset Path", "Stack Overflow Java File Path",
  "Stack Overflow Method Name", "Stack Overflow Start Line", "Stack Overflow End Line",
  "Final Manual Validation", "Recommendation Type"
]

CSV.open(File.join(output_root, "study_pair_mapping.csv"), "w", write_headers: true, headers: mapping_columns) do |csv|
  source_rows.each do |row|
    identity = parse_block_key(row["Stack Overflow Java File Path"])
    manifest = manifest_by_snippet.fetch(identity[:snippet_id])
    validation = validation_by_no[row["No"].to_s]
    csv << [
      row["No"],
      row["GH Project"],
      row["Link SO"],
      identity[:snippet_id],
      manifest["Status"],
      manifest["Before Dataset Path"],
      manifest["After Dataset Path"],
      row["Stack Overflow Java File Path"],
      row["Stack Overflow Method Name"],
      row["Stack Overflow Start Line"],
      row["Stack Overflow End Line"],
      validation&.[]("Result Compare Afte Resolve Conflict").to_s,
      validation&.[]("Recommendation Type (Summary)").to_s
    ]
  end
end

eligible = pair_manifest.count { |row| row["Status"] == "ELIGIBLE" }
excluded = pair_manifest.length - eligible
identical = pair_manifest.count { |row| row["Identical Before After"] == "YES" }
fallbacks = pair_manifest.count { |row| row["After Selection Rule"] == "SELECTED_LATEST_AVAILABLE" }

puts "Unique SO snippet histories: #{pair_manifest.length}"
puts "Eligible before/after pairs: #{eligible}"
puts "Excluded histories: #{excluded}"
puts "Identical before/after pairs: #{identical}"
puts "After snapshots using latest-available fallback: #{fallbacks}"
puts "Study-row mappings: #{source_rows.length}"
