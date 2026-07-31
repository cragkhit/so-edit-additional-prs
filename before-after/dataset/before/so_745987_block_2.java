    /**
     * Compute the natural logarithm of x to a given scale, x > 0.
     */
    public static BigDecimal ln(BigDecimal x, int scale)
    {
        // Check that x > 0.
        if (x.signum() <= 0) {
            throw new IllegalArgumentException("x <= 0");
        }
        // The number of digits to the left of the decimal point.
        int magnitude = x.toString().length() - x.scale() - 1;
        if (magnitude < 3) {
            return lnNewton(x, scale);
        }
        // Compute magnitude*ln(x^(1/magnitude)).
        else {
            // x^(1/magnitude)
            BigDecimal root = intRoot(x, magnitude, scale);
            // ln(x^(1/magnitude))
            BigDecimal lnRoot = lnNewton(root, scale);
            // magnitude*ln(x^(1/magnitude))
            return BigDecimal.valueOf(magnitude).multiply(lnRoot)
                        .setScale(scale, BigDecimal.ROUND_HALF_EVEN);
        }
    }
    /**
     * Compute the natural logarithm of x to a given scale, x > 0.
     * Use Newton's algorithm.
     */
    private static BigDecimal lnNewton(BigDecimal x, int scale)
    {
        int        sp1 = scale + 1;
        BigDecimal n   = x;
        BigDecimal term;
        // Convergence tolerance = 5*(10^-(scale+1))
        BigDecimal tolerance = BigDecimal.valueOf(5)
                                            .movePointLeft(sp1);
        // Loop until the approximations converge
        // (two successive approximations are within the tolerance).
        do {
            // e^x
            BigDecimal eToX = exp(x, sp1);
            // (e^x - n)/e^x
            term = eToX.subtract(n)
                        .divide(eToX, sp1, BigDecimal.ROUND_DOWN);
            // x - (e^x - n)/e^x
            x = x.subtract(term);
            Thread.yield();
        } while (term.compareTo(tolerance) > 0);
        return x.setScale(scale, BigDecimal.ROUND_HALF_EVEN);
    }