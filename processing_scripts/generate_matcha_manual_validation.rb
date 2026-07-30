require "csv"
require "rexml/document"
require "open3"

include REXML

SOURCE_CSV = ARGV[0] || "matcha_results_2024-05-07.csv"
SOURCE_XLSX = ARGV[1] || "Matcha_Result All (Sep2024) IW.xlsx"
OUTPUT_CSV = ARGV[2] || "matcha_results_2024-05-07_manual_validation.csv"
USEFUL_PAIRS_XLSX = ARGV[3] || "Matcha_Pair_Analysis_29Jul2026.xlsx"
# Raw and summarized code columns C, D, J, and K are intentionally excluded.
COLUMNS = (["A", "B"] + ("E".."I").to_a + ("L".."O").to_a).freeze

def unzip_entry(path, entry)
  data, status = Open3.capture2("unzip", "-p", path, entry)
  raise "Unable to read #{entry} from #{path}" unless status.success?
  data
end

def read_result_all(path)
  shared_doc = Document.new(unzip_entry(path, "xl/sharedStrings.xml"))
  shared = []
  XPath.each(shared_doc, "//*[local-name()='si']") do |si|
    parts = []
    XPath.each(si, ".//*[local-name()='t']") { |node| parts << node.text.to_s }
    shared << parts.join
  end

  # In this source workbook, Result All is rId1 -> xl/worksheets/sheet1.xml.
  sheet_doc = Document.new(unzip_entry(path, "xl/worksheets/sheet1.xml"))
  rows = []
  XPath.each(sheet_doc, "//*[local-name()='sheetData']/*[local-name()='row']") do |row|
    values = {}
    XPath.each(row, "./*[local-name()='c']") do |cell|
      column = cell.attributes["r"].sub(/\d+$/, "")
      raw = XPath.first(cell, "./*[local-name()='v']")&.text
      value = cell.attributes["t"] == "s" && raw ? shared[raw.to_i] : raw
      values[column] = value
    end
    rows << values
  end
  rows
end

def read_useful_pair_ids(path)
  shared_doc = Document.new(unzip_entry(path, "xl/sharedStrings.xml"))
  shared = []
  XPath.each(shared_doc, "//*[local-name()='si']") do |si|
    parts = []
    XPath.each(si, ".//*[local-name()='t']") { |node| parts << node.text.to_s }
    shared << parts.join
  end

  workbook_doc = Document.new(unzip_entry(path, "xl/workbook.xml"))
  rels_doc = Document.new(unzip_entry(path, "xl/_rels/workbook.xml.rels"))
  targets = {}
  XPath.each(rels_doc, "//*[local-name()='Relationship']") do |relationship|
    targets[relationship.attributes["Id"]] = relationship.attributes["Target"]
  end
  sheet = XPath.first(
    workbook_doc,
    "//*[local-name()='sheet'][@name='Useful Pairs']"
  )
  raise "Useful Pairs sheet not found in #{path}" unless sheet

  target = targets.fetch(sheet.attributes["r:id"])
  sheet_doc = Document.new(unzip_entry(path, "xl/#{target}"))
  ids = []
  XPath.each(sheet_doc, "//*[local-name()='sheetData']/*[local-name()='row']") do |row|
    next if row.attributes["r"].to_i < 3
    cell = XPath.first(row, "./*[local-name()='c'][starts-with(@r,'A')]")
    next unless cell
    raw = XPath.first(cell, "./*[local-name()='v']")&.text
    value = cell.attributes["t"] == "s" && raw ? shared[raw.to_i] : raw
    ids << value.to_s unless value.to_s.empty?
  end
  ids.uniq
end

def source_pair(row, index)
  github_path, so_path = row
  path_parts = github_path.split("/")
  {
    source_row: index,
    github_path: github_path,
    so_path: so_path,
    owner: path_parts[3].to_s.downcase,
    question_id: so_path[%r{/(\d+)/}, 1].to_s
  }
end

def split_method_path(path, dataset_directory)
  relative = path.sub(%r{\A/root/#{Regexp.escape(dataset_directory)}/}, "")
  match = relative.match(/\A(.+\.java)_([^#]+)#(\d+)#(\d+)\z/)
  raise "Unexpected method-path format: #{path}" unless match
  {
    file_path: match[1],
    method_name: match[2],
    start_line: match[3].to_i,
    end_line: match[4].to_i
  }
end

sheet_rows = read_result_all(SOURCE_XLSX)
useful_pair_ids = read_useful_pair_ids(USEFUL_PAIRS_XLSX)
headers = COLUMNS.map { |column| sheet_rows.first[column].to_s }
workbook_records = sheet_rows.drop(1).first(793).map do |row|
  {
    values: COLUMNS.map { |column| row[column] },
    project: row["B"].to_s.downcase,
    question_id: row["E"].to_s[
      %r{stackoverflow\.com/(?:questions/|a/)?(\d+)}, 1
    ].to_s
  }
end

pairs = CSV.read(SOURCE_CSV, headers: false).map.with_index(1) do |row, index|
  source_pair(row, index)
end

unless pairs.length == workbook_records.length
  raise "Record count differs: CSV=#{pairs.length}, workbook=#{workbook_records.length}"
end

# Both sources preserve the same order inside each Stack Overflow question /
# GitHub-owner group. This resolves repeated questions and repeated projects.
csv_groups = pairs.group_by { |pair| [pair[:question_id], pair[:owner]] }
xlsx_groups = workbook_records.group_by do |record|
  [record[:question_id], record[:project]]
end

mismatched_groups = (csv_groups.keys | xlsx_groups.keys).select do |key|
  csv_groups[key].to_a.length != xlsx_groups[key].to_a.length
end
unless mismatched_groups.empty?
  raise "Question/project multiplicities differ: #{mismatched_groups.inspect}"
end

matches = pairs.map do |pair|
  key = [pair[:question_id], pair[:owner]]
  position = csv_groups.fetch(key).index(pair)
  record = xlsx_groups.fetch(key).fetch(position)
  { pair: pair, record: record, status: "MATCHED" }
end

record_ids = matches.map { |match| match[:record].object_id }
raise "A workbook record was reused" unless record_ids.uniq.length == matches.length

# UNSURE records, if introduced by a future matching-rule revision, go last.
ordered = matches.sort_by do |match|
  [match[:status] == "UNSURE" ? 1 : 0, match[:pair][:source_row]]
end

CSV.open(
  OUTPUT_CSV,
  "wb",
  write_headers: true,
  headers: headers + [
    "GitHub Java File Path",
    "GitHub Method Name",
    "GitHub Start Line",
    "GitHub End Line",
    "Stack Overflow Java File Path",
    "Stack Overflow Method Name",
    "Stack Overflow Start Line",
    "Stack Overflow End Line",
    "Match Status",
    "Useful Pairs Cross-Check"
  ]
) do |csv|
  ordered.each do |match|
    github = split_method_path(
      match[:pair][:github_path],
      "2_github_projects_for_search"
    )
    stack_overflow = split_method_path(
      match[:pair][:so_path],
      "3_stackoverflow_snippets_for_index"
    )
    csv << match[:record][:values] + [
      github[:file_path],
      github[:method_name],
      github[:start_line],
      github[:end_line],
      stack_overflow[:file_path],
      stack_overflow[:method_name],
      stack_overflow[:start_line],
      stack_overflow[:end_line],
      match[:status],
      useful_pair_ids.include?(match[:record][:values][0].to_s) ? "FOUND" : nil
    ]
  end
end

puts "Wrote #{OUTPUT_CSV}"
puts "Rows: #{ordered.length}"
puts "MATCHED: #{ordered.count { |row| row[:status] == 'MATCHED' }}"
puts "UNSURE: #{ordered.count { |row| row[:status] == 'UNSURE' }}"
puts "FOUND in Useful Pairs: #{useful_pair_ids.length}"
