    public Connection data(String... keyvals){
      Validate.notNull(keyvals,"Data key value pairs must not be null");
      Validate.isTrue(keyvals.length % 2 == 0,"Must supply an even number of key value pairs");
      for (int i=0; i < keyvals.length; i+=2) {
        String key=keyvals[i];
        String value=keyvals[i + 1];
        Validate.notEmpty(key,"Data key must not be empty");
        Validate.notNull(value,"Data value must not be null");
        req.data(KeyVal.create(key,value));
      }
      return this;
    }