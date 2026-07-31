    void writeFile(String fileName, String... values) {
    	File file = new File(fileName);
    	boolean fileExists = file.exists();
    	try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, true))) {
    		if (fileExists) {
    			if (!newLineExists(file)) {
    				bw.newLine();
    			}
    		}
    		for (String value : values) {
    			bw.write(value);
    			bw.newLine();
    		}
    	} catch (IOException e) {
    		e.printStackTrace();
    	}
    }
    
    boolean newLineExists(File file) throws IOException {
    	RandomAccessFile fileHandler = new RandomAccessFile(file, "r");
    	long fileLength = fileHandler.length() - 1;
    	fileHandler.seek(fileLength);
    	byte readByte = fileHandler.readByte();
    	fileHandler.close();
    	
    	if (readByte == 0xA || readByte == 0xD) {
    		return true;
    	}
    	return false;
    }