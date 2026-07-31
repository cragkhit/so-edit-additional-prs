Java
private static void zipFolder(Path sourceFolderPath, Path zipPath) throws Exception {
   ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipPath.toFile()));
   Files.walkFileTree(sourceFolderPath, new SimpleFileVisitor<Path>() {
       public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
           zos.putNextEntry(new ZipEntry(sourceFolderPath.relativize(file).toString()));
           Files.copy(file, zos);
           zos.closeEntry();
           return FileVisitResult.CONTINUE;
        }
    });
    zos.close();
 }
./
[![Zip][1]][1]
./somethigelse/
[![First folder][2]][2]
./somethingother/
[![Second folder][3]][3]
  [1]: https://i.stack.imgur.com/2AfoM.png
  [2]: https://i.stack.imgur.com/1URnv.png
  [3]: https://i.stack.imgur.com/oBa5r.png