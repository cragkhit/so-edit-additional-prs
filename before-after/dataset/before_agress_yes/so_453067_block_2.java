    public int lineCount(String filename) throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(filename));
        int count = 0;
        int ch;
        while ((ch = in.read()) != -1) {
            if (ch == '\n') {
                ++count;
            }
        }
        return count;
    }