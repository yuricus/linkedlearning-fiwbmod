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

package com.fluidops.iwb.extensions;

import com.fluidops.iwb.page.PageContext;

public interface PrinterExtensions {
	
	public static class Templates{
		public static final String TOP_MENUBAR = "TOP_MENUBAR";
		public static final String LOWER_MENUBAR = "LOWER_MENUBAR";
	}
	
	
	/**
	 * adds the html-code for additional toolbar buttons in the upper right corner
	 * of a wikipage
	 * @param htmlHead 
	 * 
	 * @param uri
	 * @return
	 */
	public String addToolbarButtons(PageContext pc, String htmlHead);
	
	/**
	 * allow customizations of the top left menu
	 * 
	 * @param pc
	 * @param htmlHead
	 * @param user		name of user logged in
	 * @param loggedIn	is the user logged in
	 * 
	 * @since 5.0
	 * 
	 * @return
	 */
	public String addToolbarLeft(PageContext pc, String htmlHead, String user, boolean loggedIn);
	
	/**
	 * adds the html/js code for google analytics tracking
	 * 
	 * @param trackingNumber
	 * @return
	 */
	public String addGoogleAnalytics(String trackingNumber);

	/**
	 * Adds the information about missing / wrong license information
	 * 
	 * @param pc The PageContext of the current site
	 * @return Returns a String containing HTML for the visualisation
	 */
	public String getLicenseWarning(PageContext pc);
	
	/**
	 * Can be used to add a user defined script, e.g. "<script>...</script>"
	 * @return Returns the script
	 */
	public String getUserScript();
	
	/**
	 * Name of current edition
	 * @return Returns the name of the current edition
	 */
	public String getCurrentEdition();	
	
	/**
	 * Returns a string representation of the user's role set
	 */
	public String getRoleString();
	
	/**
	 * Return string template location to be used for the templates as defined
	 * in {@link Templates}. If this method returns <null>, or if the provided
	 * template argument does not exist, the default templates are used.
	 * 
	 * Customizations might use information from the provided {@link PageContext}.
	 * 
	 * Example for customization:
	 * 
	 * <source>
	 *  if (template.equals(Templates.TOP_MENUBAR))
	 *  	return "com/fluidops/iwb/ui/templates/MyTopMenuBar";
	 * </source>
	 * 
	 * @param pc page context
	 * @param template the name of a valid template as defined by {@link Templates}
	 * @return fully qualified name of the string template (without file extension .st) 
	 * 			or <code>null</code> for the default handling, 
	 *          e.g. <code>com/fluidops/iwb/ui/templates/lowerMenuBar</code>
	 */
	public String getStringTemplateName(PageContext pc, String template);
}
