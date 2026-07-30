require "csv"
require "rexml/document"
require "open3"

include REXML

PR_WORKBOOK = ARGV[0] || "Matcha_Result with PR Link - May 2025.xlsx"
CSV_PATH = ARGV[1] || "matcha_results_2024-05-07_manual_validation.csv"

def unzip_entry(path, entry)
  data, status = Open3.capture2("unzip", "-p", path, entry)
  raise "Unable to read #{entry} from #{path}" unless status.success?
  data
end

def read_sheet(path, sheet_name)
  shared = []
  shared_doc = Document.new(unzip_entry(path, "xl/sharedStrings.xml"))
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
    "//*[local-name()='sheet'][@name='#{sheet_name}']"
  )
  raise "#{sheet_name} sheet not found in #{path}" unless sheet

  sheet_doc = Document.new(
    unzip_entry(path, "xl/#{targets.fetch(sheet.attributes['r:id'])}")
  )
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

rows = read_sheet(PR_WORKBOOK, "Open PR List")

def split_github_path(path)
  relative = path.to_s.sub(
    %r{\A/root/2_github_projects_for_search/},
    ""
  )
  match = relative.match(/\A(.+\.java)_([^#]+)#(\d+)#(\d+)\z/)
  raise "Unexpected GitHub path in Open PR List: #{path.inspect}" unless match
  [match[1], match[2], match[3], match[4]]
end

pr_rows = rows.select do |row|
  row["J"].to_s.strip == "Yes" && !row["K"].to_s.strip.empty?
end
raise "Expected 36 PR links, found #{pr_rows.length}" unless pr_rows.length == 36
missing_paths = pr_rows.select { |row| row["G"].to_s.strip.empty? }

pr_by_key = {}
pr_rows.reject { |row| row["G"].to_s.strip.empty? }.each do |row|
  key = split_github_path(row["G"])
  link = row["K"].to_s.strip
  if pr_by_key.key?(key) && pr_by_key[key] != link
    raise "Conflicting PR links for #{key.join(' | ')}"
  end
  pr_by_key[key] = link
end

table = CSV.read(CSV_PATH, headers: true)
pr_by_project = {}
missing_paths.each do |row|
  project = row["B"].to_s.strip.downcase
  candidates = table.select do |csv_row|
    csv_row["GH Project"].to_s.strip.downcase == project
  end
  unless candidates.length == 1
    raise "Project fallback #{project.inspect} has #{candidates.length} CSV candidates"
  end
  pr_by_project[project] = row["K"].to_s.strip
end

pr_header = "Open PR Link"
old_status_header = "Old PR Status"
headers = table.headers.reject do |header|
  header == pr_header || header == old_status_header
end + [pr_header, old_status_header]
matched_keys = []

output_rows = table.map do |row|
  key = [
    row["GitHub Java File Path"],
    row["GitHub Method Name"],
    row["GitHub Start Line"],
    row["GitHub End Line"]
  ]
  link = pr_by_key[key]
  if link
    matched_keys << key
  else
    link = pr_by_project[row["GH Project"].to_s.strip.downcase]
  end
  existing_link = row[pr_header]
  headers.map do |header|
    case header
    when pr_header
      link || existing_link
    when old_status_header
      link ? "OLD" : nil
    else
      row[header]
    end
  end
end

unmatched = pr_by_key.keys - matched_keys.uniq
unless unmatched.empty?
  raise "#{unmatched.length} Open PR entries did not match the consolidated CSV"
end

CSV.open(CSV_PATH, "wb", write_headers: true, headers: headers) do |csv|
  output_rows.each { |row| csv << row }
end

puts "Open PR rows: #{pr_rows.length}"
puts "Unique GitHub path keys: #{pr_by_key.length}"
puts "Unique project fallbacks: #{pr_by_project.length}"
puts "Old PR records marked: #{output_rows.count { |row| row.last == 'OLD' }}"
puts "Updated #{CSV_PATH}"
