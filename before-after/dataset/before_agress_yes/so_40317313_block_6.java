    import java.util.List;
    
    import org.junit.rules.TestRule;
    import org.junit.runner.Description;
    import org.junit.runners.model.Statement;
    
    public class MyBaseTestRule implements TestRule { 
    	
    	private final int totalTests;
    	
    	public MyProjectTestRule(List<Object[]> list) {
    		this.totalTests = list.size();
    	}
    
        @Override
        public Statement apply(Statement stmt, Description desc) {
    
            return new Statement() {
    			
    	    @Override
    	    public void evaluate() throws Throwable {
    	    	for(int i=0; i<totalTests; i++) {
    	    		stmt.evaluate();
    	    	}
    	    }
            };
        }
    }