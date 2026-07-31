    public interface Copyable<T extends Copyable<T>>
    {
      public T copy();
    }
    public class CopyableRandom extends Random implements Copyable<CopyableRandom>
    {
      private final AtomicLong seed;
    
      private final static long multiplier = 0x5DEECE66DL;
      private final static long addend = 0xBL;
      private final static long mask = (1L << 48) - 1;
    
      public CopyableRandom()
      {
        this(++seedUniquifier + System.nanoTime());
      }
    
      private static volatile long seedUniquifier = 8682522807148012L;
    
      public CopyableRandom(long seed)
      {
        this.seed = new AtomicLong(0L);
        seed = (seed ^ multiplier) & mask;
        this.seed.set(seed);
        haveNextNextGaussian = false;
      }
    
      private CopyableRandom(AtomicLong seed)
      {
        this.seed = new AtomicLong(0L);
        this.seed.set(seed.get());
        haveNextNextGaussian = false;
      }
    
      @Override
      protected int next(int bits)
      {
        long oldseed, nextseed;
        AtomicLong seed_ = this.seed;
        do
        {
          oldseed = seed_.get();
          nextseed = (oldseed * multiplier + addend) & mask;
        } while (!seed_.compareAndSet(oldseed, nextseed));
        return (int) (nextseed >>> (48 - bits));
      }
    
      private double nextNextGaussian;
      private boolean haveNextNextGaussian = false;
    
      @Override
      synchronized public double nextGaussian()
      {
        // See Knuth, ACP, Section 3.4.1 Algorithm C.
        if (haveNextNextGaussian)
        {
          haveNextNextGaussian = false;
          return nextNextGaussian;
        }
        else
        {
          double v1, v2, s;
          do
          {
            v1 = 2 * nextDouble() - 1; // between -1 and 1
            v2 = 2 * nextDouble() - 1; // between -1 and 1
            s = v1 * v1 + v2 * v2;
          } while (s >= 1 || s == 0);
          double multiplier_ = StrictMath.sqrt(-2 * StrictMath.log(s) / s);
          nextNextGaussian = v2 * multiplier_;
          haveNextNextGaussian = true;
          return v1 * multiplier_;
        }
      }
    
      @Override
      public CopyableRandom copy()
      {
        return new CopyableRandom(seed);
      }
    
      public static void main(String[] args)
      {
        CopyableRandom cr = new CopyableRandom();
    
        /* changes intern state of cr */
        for (int i = 0; i < 10; i++)
          System.out.println(cr.nextInt(50));
    
        CopyableRandom copy = cr.copy();
    
        System.out.println("\nTEST: INTEGER\n");
        for (int i = 0; i < 10; i++)
          System.out.println("CR\t= " + cr.nextInt(50) + "\nCOPY\t= " + copy.nextInt(50) + "\n");
    
        CopyableRandom anotherCopy = copy.copy();
        System.out.println("\nTEST: DOUBLE\n");
        for (int i = 0; i < 10; i++)
          System.out.println("CR\t= " + cr.nextDouble() + "\nCOPY\t= " + anotherCopy.nextDouble() + "\n");
      }
    }