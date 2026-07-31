    //Loading the class at runtime
    Class<?> someInterface = Class.forName("SomeInterface");
    		
    SomeInterface instance = (SomeInterface)Proxy.newProxyInstance(someInterface.getClassLoader(), new Class<?>[]{someInterface}, new InvocationHandler() {
    			
    	@Override
    	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    				
    		//Handle the invocations
    		if(method.getName().equals("someMethod")){
    			return 1;
    		}
    		else return -1;
    		}
    	});	
    	System.out.println(instance.someMethod());
        }
      }