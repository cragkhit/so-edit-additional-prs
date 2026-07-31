    public static CountDownLatch latch = new CountDownLatch(1);
    private static class Job implements Runnable{
    private int balance;
    
    public void run(){
        try {
        latch.await();
        } catch (InterruptedException e) {}
        for (int i = 0; i < 50000; i++) {
            //existing code
        }
    }
    }
    
    public static void main(String[] args) {
        //existing code
        alpha.start();
        beta.start();
        latch.countDown();
    
    }