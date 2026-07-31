    private volatile Exception exeption;
	@Override
	public Void answer(InvocationOnMock invocation) throws Throwable {
		try {
            invocation.callRealMethod();
        } 
        catch (Exception e) {
            this.exception = e;
        }
		this.latch.countDown();
		return null;
	}
    public Exception getException() {
        return this.exception;
    }