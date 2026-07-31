        List<File> resultList = new ArrayList<>();
    
        // get all the files from a directory
        File[] fList = directory.listFiles();
        // this was missing
        if (fList == null) {
          return null;
        }
        resultList.addAll(Arrays.asList(fList));
        for (File file : fList) {
          if (file.isFile()) {
            System.out.println(file.getAbsolutePath());
            try {
              System.out.println(checkmime(file.getAbsolutePath()));
            } catch (Exception ex) {
    
            }
          } else if (file.isDirectory()) {
            // ask here if it was null
            List<File> files = listf(file.getAbsolutePath());
            if (files != null) {
              resultList.addAll(files);
            }
          }
        }
        //System.out.println(fList);
        return resultList;
      }