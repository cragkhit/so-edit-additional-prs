    class UnpackGzipAndReturnFileTransformer extends AbstractFilePayloadTransformer<File> {
    private static final int BUFFER_SIZE = 64 * 1024; // 64 kB
    @Override
    // @see http://java-performance.info/java-io-bufferedinputstream-and-java-util-zip-gzipinputstream/
    protected File transformFile(File payload) throws Exception {
        byte[] buffer = new byte[BUFFER_SIZE];
        OutputStream os = null;
        try (InputStream gzis = new GZIPInputStream(new FileInputStream(payload), BUFFER_SIZE);) {
            String uncompressedFilename = payload.getCanonicalPath().replace(".gz", ".json");
            os = new FileOutputStream(uncompressedFilename);
            int len;
            while ((len = gzis.read(buffer)) > 0) {
                os.write(buffer, 0, len);
            }
            FileUtils.deleteQuietly(payload);
            return new File(uncompressedFilename);
        } finally {
            if (os != null) {
                os.close();
            }
        }
    }
    }
    class CustomFileSplitter extends FileSplitter {
    @Override
    protected void addHeaders(Message<?> message, Map<String, Object> headers) {
        File file = null;
        if (message.getPayload() instanceof File) {
            file = (File) message.getPayload();
        } else if (message.getPayload() instanceof String) {
            file = new File((String) message.getPayload());
        }
        if (file != null) {
            if (!headers.containsKey(FILE_PATH)) {
                try {
                    headers.put(FILE_PATH, file.getCanonicalPath());
                } catch (IOException ioe) {
                    throw new RuntimeException(ioe);
                }
            }
            if (!headers.containsKey(FileHeaders.FILENAME)) {
                headers.put(FileHeaders.FILENAME, file.getName());
            }
        }
    }
    }