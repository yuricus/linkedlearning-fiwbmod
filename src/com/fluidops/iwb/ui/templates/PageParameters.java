/*
 * Copyright (C) 2008-2013, fluid Operations AG
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.

 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.

 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package com.fluidops.iwb.ui.templates;

import java.util.Map;

import org.openrdf.model.URI;

import com.fluidops.iwb.ajax.SearchTextInput;
import com.fluidops.iwb.extensions.PrinterExtensions;
import com.fluidops.iwb.page.PageContext;
import com.google.common.collect.Maps;

/**
 * This class defines the parameters that can be used in the string templates 
 * for UI customizations. 
 * 
 * The default string templates are defined by
 * 
 * a) top menu bar: {@link ServletPageParameters#TOP_MENUBAR_TEMPLATE}
 * b) lower menu bar: {@link ServletPageParameters#LOWER_MENUBAR_TEMPLATE}
 * 
 * These can be customized by means {@link PrinterExtensions}.
 * 
 * Please see the static class fields for available page parameters.
 * 
 * @author as
 *
 */
public class PageParameters {
	
	/* names of available page parameters in the string template */
	/**
	 * The title of the page, i.e. the label of the current resource
	 */
	public static final String PAGE_TITLE = "pageTitle";
			
	/**
	 * The context path of the web app 
	 */
	public static final String CONTEXT_PATH = "contextPath";
	
	/**
	 * The toolbar buttons (e.g. admin, logout, help, etc)
	 */
	public static final String TOOLBAR_BUTTONS = "toolBarButtons";
	
	/**
	 * The full user URI
	 */
	public static final String USER_URI = "userUri";
	
	/**
	 * The simple name of the logged in user
	 */
	public static final String USER_NAME = "user";
	
	/**
	 * Indicates if user is logged into the system, unset if not
	 */
	public static final String LOGGED_IN = "loggedIn";
	
	/**
	 * The role string of the current user
	 */
	public static final String ROLE_STRING = "roleString";
	
	/**
	 * The container for the search field
	 */
	public static final String SEARCH_FIELD = "searchfield";	
	
	
	protected PageContext pc;		
	protected String pageTitle;		
	protected String htmlHead;
	protected String userScript;
	protected String contextPath;
	protected String body;
	protected String toolBarButtons;
	protected URI userUri;
	protected String userName;
	protected String loggedIn;
	protected String roleString;
	protected boolean hasReadAccess;
	protected SearchTextInput searchInput;
			
	
	public String getPageTitle() {
		return pageTitle;
	}
	
	public PageContext getPageContext() {
		return pc;
	}
	
	/**
	 * Compute the string template arguments to be used.
	 * 
	 * @return
	 */
	public Map<String, Object> toStringBuilderArgs() {
		Map<String, Object> res = Maps.newHashMap();
		res.put(PAGE_TITLE, pageTitle);		
		res.put(CONTEXT_PATH, pc.contextPath);		
		res.put(TOOLBAR_BUTTONS, toolBarButtons);
		res.put(USER_URI, userUri);
		res.put(USER_NAME, userName);
		res.put(LOGGED_IN, loggedIn);
		res.put(ROLE_STRING, roleString);
		res.put(SEARCH_FIELD, hasReadAccess ? searchInput.htmlAnchor() : "");
		return res;	
	}
}