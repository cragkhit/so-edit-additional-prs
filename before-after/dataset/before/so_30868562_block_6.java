    @Bean
	public DaoAuthenticationProvider authProvider() {
		final DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
		authProvider.setUserDetailsService(userDetailsService);
		authProvider.setPasswordEncoder(passwordEncoder());
		return authProvider;
	}
    @Override
	  protected void configure(final AuthenticationManagerBuilder auth) throws Exception {
	        auth.authenticationProvider(authProvider());
	  }