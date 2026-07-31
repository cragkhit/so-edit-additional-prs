		final Path root = Paths.get("/root/dir");
		Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file,	BasicFileAttributes attrs) throws IOException {
				if (attrs.isRegularFile()) {
					Files.copy(file, root.relativize(file.getFileName()));
					Files.delete(file);
				}
				return FileVisitResult.CONTINUE;
			}
			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}			
		});