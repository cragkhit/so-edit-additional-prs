    public static void main(String[] args) {
        final Collection<File> all = new ArrayList<File>();
        addFilesRecursively(new File(args[0]), all);
        System.out.println(all);
    }
    private static void addFilesRecursively(File file, Collection<File> all) {
        final File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                all.add(child);
                addFilesRecursively(child, all);
            }
        }
    }