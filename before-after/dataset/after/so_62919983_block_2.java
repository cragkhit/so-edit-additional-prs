    public Random() {
        this(seedUniquifier() ^ System.nanoTime());
    }
    private static long seedUniquifier() {
        // L'Ecuyer, "Tables of Linear Congruential Generators of
        // Different Sizes and Good Lattice Structure", 1999
        for (;;) {
            long current = seedUniquifier.get();
            long next = current * 1181783497276652981L;
            if (seedUniquifier.compareAndSet(current, next))
                return next;
        }
    }
    private static final AtomicLong seedUniquifier
        = new AtomicLong(8682522807148012L);
  [1]: https://www2.cs.duke.edu/csed/java/jdk1.4.2/docs/api/java/util/Random.html#Random()
  [2]: https://github.com/openjdk/jdk/blob/master/src/java.base/share/classes/java/util/Random.java#L104