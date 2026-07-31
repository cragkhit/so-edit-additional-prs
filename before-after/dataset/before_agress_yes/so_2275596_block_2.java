    package com.stackoverflow.q2275443;
    
    import java.util.Arrays;
    import java.util.concurrent.Callable;
    import java.util.concurrent.ExecutorService;
    import java.util.concurrent.Executors;
    import java.util.concurrent.TimeUnit;
    
    public class Test {
    
        public static void main(String[] args) throws Exception {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.invokeAll(Arrays.asList(new Task()), 2, TimeUnit.SECONDS);
        }
    
        static class Task implements Callable<String> {
            public String call() throws Exception {
                try {
                    System.out.println("Started..");
                    Thread.sleep(4000); // 4 seconds.
                    System.out.println("Finished!");
                } catch (InterruptedException e) {
                    System.out.println("Terminated!");
                }
                return null;
            }
        }
        
    }