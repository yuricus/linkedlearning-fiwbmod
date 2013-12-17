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

import org.apache.commons.lang.StringUtils;

import com.fluidops.iwb.Global;
import com.fluidops.iwb.ajax.SearchTextInput;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.PrinterImpl;
import com.fluidops.iwb.extensions.PrinterExtensions;
import com.fluidops.iwb.extensions.PrinterExtensions.Templates;
import com.fluidops.iwb.page.PageContext;
import com.fluidops.iwb.page.SearchPageContext;
import com.fluidops.iwb.user.UserManager;
import com.fluidops.iwb.user.UserManager.ValueAccessLevel;
import com.fluidops.util.Rand;
import com.fluidops.util.StringUtil;
import com.fluidops.util.TemplateBuilder;

/**
 * This class extends {@link PageParameters} by a set of parameters that are
 * available for the rendering of the main servlet.st file.
 * 
 * @author as
 *
 */
public class ServletPageParameters extends PageParameters {

	/**
	 * The default template for {@link Templates#TOP_MENUBAR}, see {@link ServletPageParameters#renderTopMenuBar(PrinterExtensions, PageParameters)}
	 */
	public static final String TOP_MENUBAR_TEMPLATE = "com/fluidops/iwb/ui/templates/TopMenuBar";
	
	/**
	 * The default template for {@link Templates#LOWER_MENUBAR}, see {@link ServletPageParameters#renderLowerMenuBar(PrinterExtensions, PageParameters)}
	 */
	public static final String LOWER_MENUBAR_TEMPLATE = "com/fluidops/iwb/ui/templates/LowerMenuBar";
	
	
	/**
	 * The parameter name for the lower menu bar
	 */
	public static final String LOWER_MENU_BAR = "lowerMenuBar";

	/**
	 * The parameter name for the top menu bar
	 */
	public static final String TOP_MENU_BAR = "topMenuBar";

		
	/**
	 * Additional scripts for the head area
	 */
	public static final String HTML_HEAD = "head";
		
	/**
	 * Additional user scripts
	 */
	public static final String USER_SCRIPT = "userScript";
		
	/**
	 * The main div for the body 
	 */
	public static final String BODY = "body";
	
	
	public static ServletPageParameters computePageParameter(PageContext pc) {			
		PrinterExtensions printerExtensions = Global.printerExtension;
		UserManager userManager = EndpointImpl.api().getUserManager();
		
		ServletPageParameters p = new ServletPageParameters();
		p.pc = pc;
		p.pageTitle = pc.title;
		p.htmlHead = PrinterImpl.computeHtmlHead(pc);
		p.userScript = printerExtensions.getUserScript();
		p.contextPath = EndpointImpl.api().getRequestMapper().getContextPath();
		// the actual page container, might be null (e.g. for SPARQL interface we don't need it)
		p.body = pc.container!=null ? pc.container.getContainer().htmlAnchor().toString() : "";
		p.toolBarButtons = printerExtensions.addToolbarButtons(pc, p.htmlHead);
		p.userUri = userManager.getUserURI(null);
		p.userName = userManager.getUserName(null);
		p.loggedIn = userManager.isLoggedIn(null) ? "true" : null;
		p.roleString = printerExtensions.getRoleString();
		// for non-resource based pages we assume read access by default
		p.hasReadAccess = pc.value!=null ? userManager.hasValueAccess(pc.value,ValueAccessLevel.READ) : true;
		p.searchInput = new SearchTextInput("searchInput"+Rand.getIncrementalFluidUUID(), p.contextPath);;
		
		// configure the default value for the search box, if required
		// i.e. if the current page is from the search page
		if (pc instanceof SearchPageContext) {
			configureSearchInput((SearchPageContext)pc, p.searchInput);
		}		
		
		pc.page.register(p.searchInput);
		
		// top menubar and lower menubar
		p.topMenuBar = renderTopMenuBar(printerExtensions, p);
		p.lowerMenuBar = renderLowerMenuBar(printerExtensions, p);
		
		return p;
	}
	
	/**
	 * Configures the search box with a default value. 
	 * 
	 * This method is invoked when the user is on the main search page.
	 * 
	 * @param pc
	 */
	private static void configureSearchInput(SearchPageContext pc, SearchTextInput searchInput) {
		
		// configure the search input dependent on the request
		// Check if the query did not come from the SparqlServlet (in this case, SPARQL is provided as a request parameter)
		String preSetLanguage = pc.getRequestParameter("queryLanguage");
		if(StringUtil.isNullOrEmpty(preSetLanguage) || !preSetLanguage.equalsIgnoreCase("SPARQL")) {
			// If keyword search, just display in the search input box
			if (pc.queryLanguage.equals("KEYWORD")) {
				searchInput.setSearchString(pc.query);
			} else {
				// Otherwise (SPARQL, SQL, ...), display together with the prefix
				// TODO: replace with a proper helper method, if such one exists: 
				// there is no dedicated method in StringUtils.
				// Currently, System.lineSeparator() does not work on Windows: default separator is "\r\n", 
				// while the query received from a SearchWidget only contains "\n"
				// So, for now deleting both \r and \n.
				String sQuery = StringUtils.remove(pc.query, "\r");
				sQuery = StringUtils.replace(sQuery, "\n", " ");
				searchInput.setSearchString(pc.queryLanguage + ": "+sQuery);
			}
		}
	}
	
	private static String renderTopMenuBar(PrinterExtensions printerExtensions, PageParameters pageParams) {
		
		String stringTemplate = printerExtensions.getStringTemplateName(pageParams.getPageContext(), Templates.TOP_MENUBAR);
		if (stringTemplate==null)
			stringTemplate = TOP_MENUBAR_TEMPLATE;
		TemplateBuilder tb = new TemplateBuilder( "tplForClass", stringTemplate );
		return tb.renderTemplate(pageParams.toStringBuilderArgs());
	}
	
	private static String renderLowerMenuBar(PrinterExtensions printerExtensions, PageParameters pageParams) {
		
		String stringTemplate = printerExtensions.getStringTemplateName(pageParams.getPageContext(), Templates.LOWER_MENUBAR);
		if (stringTemplate==null)
			stringTemplate = LOWER_MENUBAR_TEMPLATE;
		TemplateBuilder tb = new TemplateBuilder( "tplForClass", stringTemplate);
		return tb.renderTemplate(pageParams.toStringBuilderArgs());
	}
	
	protected String topMenuBar;
	protected String lowerMenuBar;
	
	/**
	 * Returns the arguments to be used for the main servlet string.
	 * Complements the {@link PageParameters#toStringBuilderArgs()}
	 * with the parameters as defined in this class.
	 * 
	 * @return
	 */
	public Map<String, Object> toServletStringBuilderArgs() {
		Map<String, Object> res = super.toStringBuilderArgs();
		res.put(HTML_HEAD, htmlHead);
		res.put(USER_SCRIPT, userScript);
		res.put(BODY, body);
		res.put(TOP_MENU_BAR, topMenuBar);
		res.put(LOWER_MENU_BAR, lowerMenuBar);
		return res;	
	}
}
