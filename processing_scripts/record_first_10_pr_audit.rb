require "csv"

path = ARGV[0] || "matcha_results_2024-05-07_manual_validation.csv"

decisions = {
  "497" => [
    "SKIPPED",
    "The latest Stack Overflow edit closes ZipFile inside the entry loop, which can stop multi-entry extraction after the first file and still does not guarantee closure on exceptions. Applying it would introduce incorrect resource handling."
  ],
  "349" => [
    "SKIPPED",
    "The selected Stack Overflow revision contains no substantive edit to newBundle. ActFramework already reads the bundle with its configured encoding and closes the stream; hard-coding UTF-8 would remove supported configurability."
  ],
  "614" => [
    "SKIPPED",
    "The target already uses StandardCharsets.UTF_8, which is the type-safe equivalent of the Stack Overflow snippet's UTF-8 string literal. There is no improvement to apply."
  ],
  "14" => [
    "SUBMITTED",
    "Submitted a null guard and focused unit test in https://github.com/aionnetwork/aion/pull/1180 from local commit 98947173."
  ],
  "434" => [
    "SKIPPED",
    "The target is a reactive Spring Cloud Gateway and already configures CORS with CorsWebFilter. The Stack Overflow edit adds a servlet Filter fallback for a different runtime model, so it would be redundant and incompatible here."
  ],
  "438" => [
    "SKIPPED",
    "QuickHashMap already performs the fail-fast modification check and exhausted-iterator check shown in the Stack Overflow code. The selected latest snippet removes hasNext and remove behind an ellipsis, so applying it would reduce iterator functionality."
  ],
  "446" => [
    "SKIPPED",
    "Dragonwell already has the recommended BigInteger/BigInteger divide overload immediately after the compact long/BigInteger overload. Replacing the compact overload would remove an intentional optimized dispatch path rather than fix overflow."
  ],
  "452" => [
    "SKIPPED",
    "The Stack Overflow revision concerns Android Fragment UI updates and runOnUiThread. The target is an OpenJDK JFR test thread that deliberately waits briefly; none of the edited Android constructs exist or apply."
  ],
  "459" => [
    "SKIPPED",
    "The recorded ConsoleConfig target no longer exists. Current Nacos has a dedicated, configurable ConsoleCorsConfig with tests. Adding the answer's servlet-filter fallback would duplicate the current CORS architecture."
  ],
  "462" => [
    "SKIPPED",
    "AntiCollisionHashMap already contains the fail-fast and exhausted-iterator checks from the Stack Overflow code. The selected latest snippet removes hasNext and remove behind an ellipsis, so there is no safe improvement to apply."
  ]
}

table = CSV.read(path, headers: true)
table.each do |row|
  next unless decisions.key?(row["No"])
  row["Recommendation PR Status"], row["Recommendation PR Reason"] =
    decisions.fetch(row["No"])
  if row["No"] == "14"
    row["Open PR Link"] = "https://github.com/aionnetwork/aion/pull/1180"
  end
end

temporary = "#{path}.tmp"
CSV.open(temporary, "wb", write_headers: true, headers: table.headers) do |csv|
  table.each { |row| csv << row }
end
File.rename(temporary, path)

puts "Recorded #{decisions.length} audit decisions"
