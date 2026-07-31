    public static double sigmoid(double t){
        return (1/(1 + Math.pow(Math.E, (-1 * x[i][j]))));
    }
    public static double[][] sigmoid(double[][] x, boolean deriv){
        double[][] = result = new double[x.length][x[0].length];
        for (int i=0;i<x.length;i++){
            for (int j=0;j<x[i].length;j++){
                result[i][j] = 0;
                result[i][j] = sigmoid(x[i][j]);
            }
        }
        return result;
    }