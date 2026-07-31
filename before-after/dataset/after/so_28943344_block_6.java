    GaeEnroler enroler = new GaeEnroler();
    GaeAuthenticator guard = new GaeAuthenticator(getContext());
    guard.setEnroler(enroler)
    guard.setNext(router);