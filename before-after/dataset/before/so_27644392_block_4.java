    List<Class> classes =
        Stream.of("java.lang.Object", "java.lang.Integer", "java.lang.String")
              .map(className -> UtilException.uncheck(Class::forName, className))
              .collect(Collectors.toList());
              