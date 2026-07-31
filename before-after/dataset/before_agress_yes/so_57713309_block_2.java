    public static String reverse (String str){
    	  String reverse= "";
    	  if (str == null) {
    		   str = "null";
    	  } //New code
    	  for(int i=str.length()-1; i>=0; i--){
    		  reverse+=str.charAt(i);
    	  }
    	  return reverse;
	 }