    private static abstract class LongTask implements Runnable {
        private final AtomicBoolean called = new AtomicBoolean();
        private final CountDownLatch latch = new CountDownLatch(1);
        @Override
        public final void run() {
            if (called.compareAndSet(false, true)) {
                try {
                    callLongRunningMethod();
                } finally {
                    latch.countDown();
                }
            }
            try {
                latch.await();
                callLongRunningMethod();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        protected abstract void callLongRunningMethod();
    }