     public static List<String> splitEqually(String text, int size) {
        List<String> ret = new ArrayList<String>((text.length() + size - 1) / size);
        for (int start = 0; start < text.length(); start += size) {
            String temp = text.substring(start, Math.min(text.length(), start + size));
            if (temp.length() == size) {
                ret.add(temp);
            } else {
                int n = size - temp.length();
                for (int j =0 ; j< n+1 ; j++){
                    temp = temp + " ";
                }
                ret.add(temp);
            }
        }
        return ret;
    }