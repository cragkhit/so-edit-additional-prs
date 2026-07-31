    private static final ThreadLocal<DocumentBuilder> builderLocal =
        new ThreadLocal<DocumentBuilder>() {
            @Override protected DocumentBuilder initialValue() {
                return
                    DocumentBuilderFactory.newInstance()
                        .newDocumentBuilder();
            }
        };