    static public String formatMillis(long millis) {
        StringBuilder    buf=new StringBuilder(12);
        String           tmp;
        if(millis<0) { buf.append('-'); millis=Math.abs(millis); }
        tmp=("0" + (millis/3600000));          buf.append((tmp.length()>2) ? tmp.substring(1) : tmp);
        buf.append(":");
        tmp=("0" + ((millis%3600000)/60000));  buf.append(tmp.substring(tmp.length()-2));
        buf.append(":");
        tmp=("0" + ((millis%60000)/1000));     buf.append(tmp.substring(tmp.length()-2));
        buf.append(".");
        tmp=("00" + (millis%1000));            buf.append(tmp.substring(tmp.length()-3));
        return buf.toString();
        }