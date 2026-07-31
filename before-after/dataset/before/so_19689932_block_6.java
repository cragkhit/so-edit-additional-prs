        public static class SynchRun implements Runnable {
            private final long start;
            public SynchRun(long s) {
                start = s;
            }
            @Override public void run() {
                sleep();
                System.out.println(System.currentTimeMillis() - start);
            }
        
            private static synchronized void sleep() {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {}
            }
        }