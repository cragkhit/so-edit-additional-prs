    public static RequestBody createBody(final MediaType contentType, final InputStream inputStream) {
        if (inputStream == null) throw new NullPointerException("content == null");
        return new RequestBody() {
            @Override public MediaType contentType() {
                return contentType;
            }
            @Override public long contentLength() {
                return -1;
            }
            @Override public void writeTo(BufferedSink sink) throws IOException {
                Source source = null;
                try {
                    source = Okio.source(inputStream);
                    sink.writeAll(source);
                } finally {
                    Util.closeQuietly(source);
                }
            }
        };
    }