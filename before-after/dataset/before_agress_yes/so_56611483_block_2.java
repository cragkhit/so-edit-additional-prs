 public void getFiles(String dir) throws IOException {
        File directory = new File(dir);
        //Verify if it is a valid file name
        if (!directory.exists()) {
            System.out.println(String.format("Directory %s does not exist", dir));
            return;
        }
        //Verify if it is a directory and not a file path
        if (!directory.isDirectory()) {
            System.out.println(String.format("Provided value %s is not a directory", dir));
            return;
        }
        File[] files = directory.listFiles(logFilefilter);
        
        // create an additional scanner to store the userInput (the word you want to find).
        Scanner userInput = new Scanner(System.in);
        System.out.println("Please enter the word that you want to find in the .smali files: ");
        String seekWord = userInput.nextLine(); //this line will store the line you are looking for
        // create an arrayList to store all the files that contain the word.
        List<File> filesWithWord = new ArrayList<>();
        //Let's list out the filtered files
        for (File f : files) {
            Scanner sc = new Scanner(f);
            while (sc.hasNext()) {
                // look in each line and if the line contains the word store the file.
                String word = sc.nextLine();
                if (word.contentEquals(seekWord)) {
                    System.out.println(f.getName());
                    filesWithWord.add(f);
                    continue; // no need to go through the rest of the lines.
                }
            }
        }
        // create another file to store the results
        File fileWithFoundFiles = new File("path/to/file/files.txt");
        //make sure the parents of the file exist.
        fileWithFoundFiles.getParentFile().mkdirs();
        if (!fileWithFoundFiles.exists()) {
            fileWithFoundFiles.createNewFile();
        }
        try (FileWriter writer = new FileWriter(fileWithFoundFiles)) { //try-with resources so your rescoure gets closed automatically
            for (File f : filesWithWord) {
                //write the fileNames to the file
                writer.write(f.getName() + "\n");
            }
        } catch (Exception e) {
          System.out.println(e.getMessage()); 
       }
    }