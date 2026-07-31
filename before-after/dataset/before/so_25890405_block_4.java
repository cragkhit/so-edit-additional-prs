    public int getGCD(int a, int b) {
     if (b == 0) { return a; }
     else { return getGCD(b, a%b); }
    }
    public int getGCDMultiple(int[] a) {
    
      // need some start value
      int gcd = a[ 0 ];
    
      // compute incrementally
      for( int i=1; i<a.length; i++ ) {
        gcd = getGCD( gcd, a[i] );
      }
    
      // return result
      return gcd;
    
    }