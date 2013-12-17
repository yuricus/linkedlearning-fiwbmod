<%
	String url = com.fluidops.iwb.api.EndpointImpl.api().getRequestMapper().getStartPage();
	HttpSession sess = request.getSession(false);
	
	if (sess != null && !sess.isNew()) {
		// already had session, make sure to encode it for non-cookie based sessions
		url = response.encodeRedirectURL(url);
	}
	else {
		// no session yet -> do not try to encode session if in request url, which will fail 
	}
    response.sendRedirect(url);
 %>