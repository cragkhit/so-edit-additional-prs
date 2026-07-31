    @Override
    protected boolean authenticate(Request request, Response response) {
        ClientInfo info = request.getClientInfo();
        if (info.isAuthenticated()) {
            // The request is already authenticated.
            return true;
        } else if (userService.isUserLoggedIn()) {
            // The user is logged in, create Restlet user.
            com.google.appengine.api.users.User gaeUser = userService
                    .getCurrentUser();
            User restletUser = new User(gaeUser.getUserId());
            restletUser.setEmail(gaeUser.getEmail());
            restletUser.setFirstName(gaeUser.getNickname());
            info.setUser(restletUser);
            info.setAuthenticated(true);
            // Admin
            if (userService.isUserAdmin()) {
                Role role = new Role(Application.getCurrent(), "admin");
                info.getRoles().add(role);
            }
            return true;
        } else {
            // The GAE user service says user not logged in, let's redirect him
            // to the login page.
            String loginUrl = userService.createLoginURL(request
                    .getOriginalRef().toString());
            response.redirectTemporary(loginUrl);
            return false;
        }
    }