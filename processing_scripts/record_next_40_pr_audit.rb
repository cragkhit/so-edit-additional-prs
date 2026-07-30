require "csv"

path = ARGV[0] || "matcha_results_2024-05-07_manual_validation.csv"

decisions = {
  "463" => "The selected Stack Overflow revision is byte-for-byte identical to its original newBundle snippet. Current Alink already reads the bundle with StandardCharsets.UTF_8, so there is no change to apply.",
  "400" => "The selected Stack Overflow revision only adds a final newline to an unrelated mouse-wheel demo. CreateColorChooserDialog merely shares a Swing main-method pattern.",
  "688" => "The Stack Overflow edit only adds the oneDecimal DecimalFormat field. StringUtils already defines that field and uses it in humanReadableInt.",
  "3" => "The Stack Overflow revision reconstructs a surrounding demo main method but does not change the integer-parsing helper. MathUtils.isInteger already has the same parse-and-catch implementation.",
  "674" => "The answer rewrites a specific flatten-directory walk using a temporary directory. Ambiverse Util conditionally deletes directories while preserving configured exception names, so the answer's destructive workflow is not applicable.",
  "727" => "Submitted a fix so remove(Object) removes the matched WeakReference entry immediately instead of leaving a cleared entry observable until a later size() cleanup.",
  "728" => "The Stack Overflow edit changes remove(Object), not removeReleased. The applicable removal fix is represented by the PR recorded on the paired remove row (No. 727).",
  "432" => "The selected Stack Overflow snippet is unchanged from its original revision, and Tree.inOrder already implements the standard recursive traversal.",
  "433" => "The selected Stack Overflow snippet is unchanged from its original revision, and Tree.postOrder already implements the standard recursive traversal.",
  "225" => "The Stack Overflow revision changes the output contract from HH:MM:SS to include milliseconds. Arc's established formatMillis intentionally returns HH:MM:SS, so applying it would be a behavior change rather than a fix.",
  "226" => "The selected Stack Overflow TimSort block is byte-for-byte identical to its original revision, and Arc already contains the same sorting algorithm.",
  "514" => "The selected revision changes only the final newline of a standalone mouse-wheel demo. This NetBeans run method schedules repaintView and has no mouse-wheel code.",
  "515" => "The selected revision changes only the final newline of a standalone mouse-wheel demo. This NetBeans run method schedules repaintView and has no mouse-wheel code.",
  "516" => "The selected revision changes only the final newline of a standalone mouse-wheel demo. This NetBeans run method schedules repaintView and has no mouse-wheel code.",
  "517" => "The selected revision changes only the final newline of a standalone mouse-wheel demo. This NetBeans run method schedules repaintView and has no mouse-wheel code.",
  "518" => "The selected revision changes only the final newline of a standalone mouse-wheel demo. SecurityMultiViewElement has no corresponding listener or demo GUI.",
  "519" => "The selected revision changes only the final newline of a standalone mouse-wheel demo. This NetBeans run method schedules repaintView and has no mouse-wheel code.",
  "520" => "The Stack Overflow edit renames a local JavaFX Group from root to view in a different AnimatedChart constructor. The target init method consistently uses root, so there is no bug or applicable change.",
  "521" => "The historical answer adds nested finally blocks around streams, but this NetBeans QA helper no longer exists on the current master branch. There is no current file to patch.",
  "522" => "The answer appends a separate servlet-filter fallback after a complete Spring CorsFilter solution. HugeGraph already uses the Spring configuration; adding the fallback would duplicate CORS handling and the pasted snippet is not a compilable replacement.",
  "523" => "The answer changes an empty handler on a third outbound Netty Bootstrap connection. RocketMQ's backend handler only forwards traffic to an existing inbound channel and contains no such bootstrap or third connection.",
  "524" => "The answer changes an empty handler on a third outbound Netty Bootstrap connection. RocketMQ's frontend handler has no bootstrap, third connection, empty handler call, or DiscardServerHandler.",
  "525" => "The answer implements a destructive directory-flattening walk. IoTDB's visitor only collects regular files for consensus snapshots, so deleting or moving the visited files would violate its purpose.",
  "526" => "The answer changes an empty handler on a third outbound Netty Bootstrap connection. Pulsar's test PortForwarder frontend handler only forwards to one existing outbound channel.",
  "527" => "The answer changes an empty handler on a third outbound Netty Bootstrap connection. Pulsar's test PortForwarder backend handler only forwards to one existing inbound channel.",
  "546" => "The Stack Overflow revision changes only sample Sudoku data in main and even introduces an inconsistent soduku variable name. The matched printSudoku method is unchanged and already correct.",
  "117" => "The Stack Overflow revision only adds documentation links around several unrelated thread-coordination alternatives. DummyWorker.run contains none of the edited code.",
  "655" => "The Stack Overflow revision appends a classpath workaround and a replacement demo PApplet class; it does not edit split. Arduino's production split implementation has no corresponding change.",
  "656" => "The Stack Overflow revision appends a classpath workaround and a replacement demo PApplet class; it does not edit parseInt. Arduino's production parseInt implementation has no corresponding change.",
  "712" => "The Stack Overflow revision is a whitespace-only reformat of the JMF JpegImagesToMovie example. ArtiSynth has adapted the same logic and there is no behavioral change to port.",
  "713" => "The Stack Overflow revision only reformats createDataSink. ArtiSynth's adapted method already has the same behavior.",
  "714" => "The Stack Overflow revision only reformats dataSinkUpdate. ArtiSynth's adapted method already has the same behavior.",
  "715" => "The Stack Overflow revision only reformats createMediaLocator. ArtiSynth's adapted method already has the same behavior.",
  "716" => "The Stack Overflow revision only reformats the image-stream read method. ArtiSynth's adapted method already has the same behavior.",
  "717" => "The Stack Overflow revision only reformats ImageSourceStream. ArtiSynth's adapted class already has the same behavior.",
  "355" => "The only applicable part of the answer would replace a method-local Random with ThreadLocalRandom. The target already has the corrected Fisher-Yates bound, and this instance method has no demonstrated concurrent contention warranting the unrelated optimization.",
  "408" => "The selected revision changes only the final newline of a standalone mouse-wheel demo. FinalResizer merely shares the Swing invokeLater main-method pattern.",
  "793" => "The selected Stack Overflow escape snippet is byte-for-byte identical to its original revision and already matches UserDictionaryConverter.escape.",
  "118" => "BCryptPasswordEncoder.encode already matches the selected Stack Overflow implementation, including strength and SecureRandom handling.",
  "39" => "The selected Stack Overflow JdbcBlackHole close snippet is byte-for-byte identical to its original revision, and the Redshift driver already contains the same null check and SQLException handling."
}

submitted = {
  "727" => "https://github.com/ant-media/Ant-Media-Server/pull/7990"
}

table = CSV.read(path, headers: true)
missing = decisions.keys - table.map { |row| row["No"] }
raise "Missing rows: #{missing.join(', ')}" unless missing.empty?

table.each do |row|
  next unless decisions.key?(row["No"])

  if submitted.key?(row["No"])
    row["Recommendation PR Status"] = "SUBMITTED"
    row["Open PR Link"] = submitted.fetch(row["No"])
  else
    row["Recommendation PR Status"] = "SKIPPED"
  end
  row["Recommendation PR Reason"] = decisions.fetch(row["No"])
end

temporary = "#{path}.tmp"
CSV.open(temporary, "wb", write_headers: true, headers: table.headers) do |csv|
  table.each { |row| csv << row }
end
File.rename(temporary, path)

puts "Recorded #{decisions.length} top-down audit decisions"
puts "Submitted PRs: #{submitted.length}"
