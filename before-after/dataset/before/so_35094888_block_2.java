    private boolean done = false;
    private void startServer() {
        Thread t = new Thread(new Runnable() {
            public void Run() {
                socketConnection();
            });
        }
        t.start();
    }
    private void socketConnection() {
        try {
            serverSocket = new ServerSocket(9090);
            System.out.println("Listening: " + serverSocket.getLocalPort());
        } catch (IOException e) {
            e.printStackTrace();
        }
    
        while (!done) {
            try {
                final Socket socket = serverSocket.accept();
    	        Thread t = new Thread(new Runnable() {
    		        public void Run() {
        			    handle(socket);
        		    }
    	    	});
                t.start();
        	} catch (Exception e) {
        	    e.printStackTrace();
    	    }
        }
    }
    
    private void handle(Socket socket) {
        if (socket == null) return;
        try {
        	dataInputStream = new DataInputStream(socket.getInputStream());
        	System.out.println("ip: " + socket.getInetAddress());
    	    System.out.println("message: " + dataInputStream.readUTF());
        } catch (IOException e) {
        	e.printStackTrace();
        } finally {
        	try {
    	        socket.close();
        	} catch (IOException e) {
        	    e.printStackTrace();
        	}
    	
        	if (dataInputStream != null) {
    	        try {
            		dataInputStream.close();
    	        } catch (IOException e) {
            		e.printStackTrace();
    	        }
        	}
        }
    }