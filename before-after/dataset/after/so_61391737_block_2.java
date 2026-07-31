    public static List<String> splitEqually(String text, int size) {
        List<String> ret = new ArrayList<String>((text.length() + size - 1) / size);
        StringBuilder str1 = new StringBuilder();
        for (int start = 0; start < text.length(); start += size) {
            String temp = text.substring(start, Math.min(text.length(), start + size));
            if (temp.length() == size) {
                ret.add(temp);
                System.out.println(temp.length());
            } else {
                int n = size - temp.length();
                str1.append(temp);
                for (int j =0 ; j< n ; j++){
                    str1.append(" ");
                }
                System.out.println(str1.length());
                ret.add(str1.toString());
            }
        }
        return ret;
    }