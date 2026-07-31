    List<Future<Object>> futures = executorService.invokeAll(tasks);
        
    CompletableFuture.runAsync(() -> {
        futures.forEach(future -> {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        });
        System.out.println("Ended doing things");
    });