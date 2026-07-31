    public static int allignment(String dnaSequence1, String dnaSequence2 /*,int offset*/) {
        int bestScore = -1;
        int bestOffset = 0;
        String bestSequence = null;
        for(String tempSequence = dnaSequence2; tempSequence.length() > 0; tempSequence = tempSequence.substring(1)) {
            for(int length = tempSequence.length(); length > 0; length--) {
                String match = tempSequence.substring(0, length);
                int matchIndex;
                if (-1 != (matchIndex = dnaSequence1.indexOf(match))) {
                    if (length > bestScore) {
                        bestOffset = matchIndex;
                        bestScore = length;
                        bestSequence = match;
                    }
                }
            }
        }
        
        if (null != bestSequence) {
            System.out.println("Best alignment score :" + bestScore);
            System.out.println(dnaSequence1);
            System.out.print(space(bestOffset) + bestSequence);
        } else {
            System.out.print(dnaSequence1+" and "+dnaSequence2+" cannot be aligned");
        }
        int alignmentScore = dnaSequence1.compareToIgnoreCase(dnaSequence2);
        return alignmentScore;
    }
    public static String space(int bestOffset) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < bestOffset; i++) {
            builder.append(" ");
        }
        return builder.toString();
    }