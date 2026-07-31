    public String toCsvRow() {
        return Stream.of(page, document, loan, type)
                .map(string -> {
                         if (string.contains("\"")) {
                             return string.replaceAll("\"", "\"\"");
                         }
                         return string;
                    })
                .map(string -> {
                         if (string.contains("\"") || string.contains(",")) {
                             return "\"" + string + "\"";
                         }
                         return string;
                    })
                .collect(Collectors.joining(","));
    }