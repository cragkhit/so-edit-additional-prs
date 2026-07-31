    new Handler().postDelayed(new Runnable() {
    		@Override
    		public void run() {
    			for (int i = 0; i < 10; i++) {
    				try {
    					TimeUnit.SECONDS.sleep(timeCycle); 
    					runOnUiThread(new Runnable() {
    						@Override
    						public void run() {
    							randomNote();
    						}
    					});
    				} catch (InterruptedException e) {
    					e.printStackTrace();
    				}
    
    			}
    
    		}
    	}, 0);