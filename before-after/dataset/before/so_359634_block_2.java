    public boolean isDouble(String value)
    {
        boolean seenDot = false;
        for (int i=0; i < value.length(); i++)
        {
            char c = value.charAt(i);
            if (c >= '0' && c <= '9')
            {
                continue;
            }
            if (c == '-' && i == 0)
            {
                continue;
            }
            if (c == '.' && !seenDot)
            {
                seenDot = true;
                continue;
            }
            return false;
        }
        try
        {
            Double.parseDouble(value);
            return true;
        }
        catch (NumberFormatException e)
        {
            return false;
        }
    }