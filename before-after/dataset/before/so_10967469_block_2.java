	public static void openWebpage(URI uri) {
		Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
		if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
			try {
				desktop.browse(uri);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	public static void openWebpage(String url) {
		try {
			openWebpage(new URI(url));
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}