        ServerSocket ss = new ServerSocket(8123);
        
        new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(5000);
                } catch(InterruptedException e) {
                }
                ss.close();
            }
        }.start();
        Socket s = ss.accept();