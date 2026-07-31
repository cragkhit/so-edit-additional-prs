    String[] vals1 = str1.split("\\.");
    String[] vals2 = str2.split("\\.");
    int i=0;
    while(i<vals1.length&&i<val2.length&&vals[i].equals(vals[i]) {
      i++;
    }
    
    if (i<vals1.length&&i<val2.length) {
        return vals1[i].compareTo(vals2[i]);
    }
    
    if (i<vals1.length) return -1; // End of val1 but not val2
    if (i<vals2.length) return  1; // End of val2 but not val1
    
    return 0;