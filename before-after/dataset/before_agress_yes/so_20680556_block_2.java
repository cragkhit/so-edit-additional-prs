     class CustomDirectoryFilter implements FileFilter {
        
    private String notAllowedFileName = "testFolder";
        
      @Override
      public boolean accept(File pathname) {
        
        if (pathname.isDirectory()) {
          File[] subFiles = pathName.listFiles();
          for (File file : subFiles){
            if (file.getName().equals(notAllowedFileName)){
               return false;
            }
          }
          return true;
        }
        return false; 
      }
    }