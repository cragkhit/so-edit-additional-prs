require "csv"

path = ARGV[0] || "matcha_results_2024-05-07_manual_validation.csv"

decisions = {
  "443" => "The selected Stack Overflow TimSort block is byte-for-byte identical to its original revision, and Dragonwell already contains the same algorithm. There is no edit to apply.",
  "444" => "The selected Stack Overflow putMapEntries block is byte-for-byte identical to its original revision and already matches Dragonwell's HashMap implementation.",
  "445" => "The selected Stack Overflow Timer.sched block is byte-for-byte identical to its original revision and already matches Dragonwell's implementation.",
  "446" => "Dragonwell already has the recommended BigInteger/BigInteger divide overload immediately after the compact long/BigInteger overload. Replacing the compact overload would remove an intentional optimized dispatch path rather than fix overflow.",
  "447" => "The selected Stack Overflow code is the existing BigInteger/BigInteger overload, which Dragonwell already contains verbatim. There is no missing change.",
  "448" => "The Stack Overflow edit casts the component to AbstractButton and applies pressed/rollover coloring. MetalTabbedPaneUI.update receives a tabbed pane, not a button, so the cast and button-state logic do not apply.",
  "449" => "The Stack Overflow edit defines a custom BasicTableHeaderUI subclass that suppresses sorting when extra modifiers are pressed. Inserting that customization into the JDK base implementation would change global Swing behavior and is not an applicable fix.",
  "450" => "The target already contains the shared toHexString helper. The Stack Overflow revision adds complete multi-party Diffie-Hellman demo programs, which are unrelated to this PKCS#11 leading-zero regression test.",
  "451" => "The target already contains the shared toHexString helper. The Stack Overflow revision's multi-party Diffie-Hellman demos are unrelated to X500Name DER construction testing.",
  "452" => "The Stack Overflow revision concerns Android Fragment UI updates and runOnUiThread. The target is an OpenJDK JFR test thread that deliberately waits briefly; none of the edited Android constructs exist or apply.",
  "453" => "The selected Stack Overflow revision changes only the final newline of a standalone mouse-wheel demo. IMLookAndFeel merely shares a small Swing main-method pattern, so there is no code change to apply.",
  "454" => "DHKeyAgreement2 already contains the shared hex helper. The Stack Overflow revision replaces a different four-party fragment and adds four- and five-party demo programs, which do not apply to this two-party provider test.",
  "455" => "DHKeyAgreement3 already contains the shared hex helper. The Stack Overflow revision's separate four- and five-party demos are unrelated to this intentional three-party provider test.",
  "456" => "BlowfishTestVector already contains the shared hex helper. Adding multi-party Diffie-Hellman demo programs would be unrelated to Blowfish test-vector formatting.",
  "457" => "TestLeadingZeroes already contains the shared hex helper. The Stack Overflow revision's multi-party Diffie-Hellman demos do not apply to this TLS leading-zero regression test.",
  "458" => "The Stack Overflow edit renames create3ByteImage to create3ByteRGBImage, but Dragonwell uses the helper for both RGB and GRB layouts. The proposed name would be inaccurate for one caller.",
  "459" => "The recorded ConsoleConfig target no longer exists. Current Nacos has a dedicated, configurable ConsoleCorsConfig with tests. Adding the answer's servlet-filter fallback would duplicate the current CORS architecture.",
  "460" => "The Stack Overflow edit moves URL decoding into the file-protocol branch. Atlas PathUtils.basedir already performs decode there, so the recommendation is already present.",
  "461" => "The Stack Overflow edit changes a standalone dynamic-proxy example to invoke a method reflectively. OracleUtilsTest already creates the OracleConnection proxy and directly tests unwrap and pingDatabase; the example's reflective print call is unrelated.",
  "462" => "AntiCollisionHashMap already contains the fail-fast and exhausted-iterator checks from the Stack Overflow code. The selected latest snippet removes hasNext and remove behind an ellipsis, so there is no safe improvement to apply."
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
