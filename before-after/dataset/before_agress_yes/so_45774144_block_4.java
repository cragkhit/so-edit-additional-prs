	    S1L1Reset.setUI(new BasicButtonUI() {
	    	@Override
	        public void update(Graphics g, JComponent c) {
	            if (c.isOpaque()) {
	                g.setColor(c.getBackground());
	                g.fillRoundRect(0, 0, c.getWidth(),c.getHeight(), 20, 20);
	            }
	            paint(g, c);
	        }
	    });