        for (int i = 0; i < sp.length; i++) {
             if(stringOrInteger(sp[i]))
                 System.out.println(sp[i] + " is type Integer");
             else
                 System.out.println(sp[i] + " is type String");
        } 
    }
    private static boolean stringOrInteger(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }