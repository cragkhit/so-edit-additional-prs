require "csv"

path = ARGV[0] || "matcha_results_2024-05-07_manual_validation.csv"

decisions = {
  "429" => "The Stack Overflow revision only adds prose questioning a different ArrayDeque-based solution and interview advice. It does not change the largestRectangleArea implementation matched in this repository.",
  "434" => "The target is a reactive Spring Cloud Gateway and already configures CORS with CorsWebFilter. The Stack Overflow edit adds a servlet Filter fallback for a different runtime model, so it would be redundant and incompatible here.",
  "435" => "The Stack Overflow edit changes preferred sizes inside a standalone GridBagLayout demo. ActionPanel.main only opens the project-specific panel in a test frame and contains none of the edited demo components.",
  "436" => "The Stack Overflow edit changes preferred sizes inside a standalone GridBagLayout demo. ScriptTaskParamsPanel.main only opens the project-specific panel in a test frame and contains none of the edited demo components.",
  "437" => "The Stack Overflow edit changes preferred sizes inside a standalone GridBagLayout demo. SubBpmCodeParamsPanel.main only opens the project-specific panel in a test frame and contains none of the edited demo components.",
  "438" => "QuickHashMap already performs the fail-fast modification check and exhausted-iterator check shown in the Stack Overflow code. The selected latest snippet removes hasNext and remove behind an ellipsis, so applying it would reduce iterator functionality.",
  "439" => "The Stack Overflow revision only prepends class fields such as R_MIN, m_Data, and m_Ranges to the selected distance method. NormalizableDistance already declares all of those fields with the same values, so there is no missing change.",
  "440" => "The selected Stack Overflow revision is byte-for-byte identical to its original code. Its demo calls initComponents, while SwingApplet already invokes its own working initUI method on the event-dispatch thread; renaming it would not apply an edited recommendation.",
  "441" => "The Stack Overflow revision replaces a four-party Diffie-Hellman fragment with complete four- and five-party demo programs. Keytool only shares the toHexString helper, which already matches; adding the DH demos would be unrelated.",
  "442" => "The selected Stack Overflow revision is byte-for-byte identical to its original ComparableTimSort block, and Dragonwell already contains the same sorting algorithm. There is no edit to apply."
}

table = CSV.read(path, headers: true)
table.each do |row|
  next unless decisions.key?(row["No"])
  row["Recommendation PR Status"] = "SKIPPED"
  row["Recommendation PR Reason"] = decisions.fetch(row["No"])
end

temporary = "#{path}.tmp"
CSV.open(temporary, "wb", write_headers: true, headers: table.headers) do |csv|
  table.each { |row| csv << row }
end
File.rename(temporary, path)

puts "Recorded #{decisions.length} top-down audit decisions"
