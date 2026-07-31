        try (BufferedWriter writer = Files.newBufferedWriter(Path.of(outFile), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
             Stream<String> lines = Files.lines(Path.of(inFile))) {
            // Read the file using Files.lines and collect it into a List
            lines.map(line -> line.trim().replaceAll("aa", "bb"))
                    .forEach(line -> {
                        try {
                            writer.write(line);
                            writer.newLine();
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
            writer.flush();
        }