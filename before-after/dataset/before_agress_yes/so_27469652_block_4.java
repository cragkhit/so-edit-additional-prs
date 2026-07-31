		CountDownLatch latch = new CountDownLatch(1);
		start();
		Thread stopping = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					latch.await();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				stop();
			}
		});