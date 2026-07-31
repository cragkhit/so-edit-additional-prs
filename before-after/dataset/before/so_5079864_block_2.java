    public static void copyBeanProperties(
        final Object source,
        final Object target,
        final Iterable<String> properties){
        final BeanWrapper src = new BeanWrapperImpl(source);
        final BeanWrapper trg = new BeanWrapperImpl(target);
        for(final String propertyName : properties){
            trg.setPropertyValue(
                propertyName,
                src.getPropertyValue(propertyName)
            );
        }
    }