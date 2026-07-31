    public class ValidateTestCase {
    
    	@Test
    	public void testHappyPath() {
    		Validate.stateNotNull("", "");
    	}
    
        @Test
        public void testNullMessage() {
                try {
                    Validate.stateNotNull(null, null);
                    Assert.fail();
                }
                catch (IllegalStateException e) {
                    String expected = "Exception message is a null object!"
                    Assert.assertEquals(expected, e.getMessage());
                }
        }
    
        @Test(expected=IllegalStateException.class)
        public void testNullObject() {
            Validate.stateNotNull(null, "test");
        }
    }