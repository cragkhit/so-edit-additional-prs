        BufferedWriter writer = Files.newBufferedWriter(...);
        // Read the file using Files.lines and collect it into a List
        Files.lines(Paths.get("<inputFilePath>"))
                .map(line -> line.trim().replaceAll("aa","bb"))
                .forEach(line -> {
                    try {
                        writer.write(line);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
        writer.flush();
        writer.close();