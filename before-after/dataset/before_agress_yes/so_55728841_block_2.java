    public static void main(String[] xxx) {
        System.setOut(new PrintStream(System.out) {
            final OutputStream fos;
            {
                try {
                    fos = new FileOutputStream(new File("/myfile.txt"));
                } catch (FileNotFoundException e) {
                    throw new AssertionError("cant create file", e);
                }
            }
            @Override
            public void write(byte[] buf, int off, int len) {
                super.write(buf, off, len);
                try {
                    fos.write(buf, off, len);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            @Override
            public void close() {
                try {
                    fos.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } finally {
                    super.close();
                }
            }
        });
        System.out.println("this works");
        //System.out.close(); // maybe required at the end of execution
    }