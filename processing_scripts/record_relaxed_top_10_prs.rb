require "csv"

csv_path = ARGV[0] || "matcha_results_2024-05-07_manual_validation_FINAL.csv"
rows = CSV.read(csv_path, headers: true)

updates = {
  "497" => {
    link: "https://github.com/AbFab3D/AbFab3D/pull/20",
    reason: "Relaxed audit: hardened Project.extractZip with deterministic ZIP/entry stream closure, IOException propagation, and zip-slip path validation. Submitted PR."
  },
  "349" => {
    link: "https://github.com/actframework/actframework/pull/1434",
    reason: "Relaxed audit: the matched newBundle edit remained inapplicable, but the same AppConfig class leaked the OkHttp response used to load remote configuration. Submitted a focused try-with-resources fix."
  },
  "614" => {
    link: "https://github.com/AddstarMC/Minigames/pull/404",
    reason: "Relaxed audit: retained the existing UTF-8 behavior while making the bundle stream and reader ownership explicit with try-with-resources. Submitted PR."
  },
  "195" => {
    link: "https://github.com/aicis/fresco/pull/441",
    reason: "Relaxed audit: changed the SPDZ recursive test cleanup helper to propagate file-walk failures instead of printing and suppressing them. Submitted PR."
  },
  "196" => {
    link: "https://github.com/aicis/fresco/pull/441",
    reason: "Relaxed audit: changed the duplicated TinyTables recursive test cleanup helper to propagate file-walk failures instead of printing and suppressing them. Covered by the same submitted PR."
  },
  "217" => {
    link: "https://github.com/alchitry/Alchitry-Labs/pull/38",
    reason: "Relaxed audit: the matched ln method remained unchanged, but its adjacent intRoot dependency divided by zero for a zero radicand and accepted invalid indices. Submitted a focused edge-case fix."
  }
}

found = {}
rows.each do |row|
  update = updates[row["No"]]
  next unless update

  raise "Duplicate row No #{row['No']}" if found[row["No"]]
  unless ["SKIPPED", "SUBMITTED"].include?(row["Recommendation PR Status"])
    raise "Unexpected prior status for No #{row['No']}: #{row['Recommendation PR Status'].inspect}"
  end
  existing_link = row["Open PR Link"].to_s.strip
  unless existing_link.empty? || existing_link == update[:link]
    raise "Conflicting PR link for No #{row['No']}: #{existing_link}"
  end

  row["Recommendation PR Status"] = "SUBMITTED"
  row["Recommendation PR Reason"] = update[:reason]
  row["Open PR Link"] = update[:link]
  found[row["No"]] = true
end

missing = updates.keys - found.keys
raise "Missing expected rows: #{missing.join(', ')}" unless missing.empty?

temporary_path = "#{csv_path}.tmp"
CSV.open(temporary_path, "w", write_headers: true, headers: rows.headers) do |csv|
  rows.each { |row| csv << row }
end
File.rename(temporary_path, csv_path)

puts "Recorded #{updates.length} rows for 5 relaxed-audit PRs in #{csv_path}"
