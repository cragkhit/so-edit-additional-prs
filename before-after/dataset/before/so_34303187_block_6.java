    import java.util.*;
    import java.lang.*;
    import java.io.*;
    
    class Ideone
    {
    	
    	private static String output(InputStream inputStream) throws IOException {
    		StringBuilder sb = new StringBuilder();
    		BufferedReader br = null;
    		try {
    			br = new BufferedReader(new InputStreamReader(inputStream));
    			String line = null;
    			while ((line = br.readLine()) != null) {
    				sb.append(line + System.getProperty("line.separator"));
    			}
    		} finally {
    			br.close();
    		}
    		return sb.toString();
    	}
    	
    	public static void main (String[] args) throws java.lang.Exception
    	{
    		ProcessBuilder builder = new ProcessBuilder("ls", "-l");
    
        	try {
            	Process p = builder.start();
            	System.out.println("Output from 'ls -l':\n" + output(p.getInputStream()));
    
        	} catch (IOException e) {
            	System.out.println("There's a problem.");
        	}	
        	
    	}
    }