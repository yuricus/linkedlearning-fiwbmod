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

package com.fluidops.iwb.api;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.openrdf.model.URI;
import org.openrdf.model.Value;

import com.fluidops.ajax.components.FComponent;
import com.fluidops.iwb.Global;
import com.fluidops.iwb.extensions.PrinterExtensions;
import com.fluidops.iwb.page.PageContext;
import com.fluidops.iwb.ui.templates.ServletPageParameters;
import com.fluidops.iwb.user.UserManager;
import com.fluidops.iwb.util.Config;
import com.fluidops.util.StringUtil;
import com.fluidops.util.TemplateBuilder;
import com.google.common.collect.Sets;

public class PrinterImpl implements Printer
{
	
    private static final Logger logger = Logger.getLogger(PrinterImpl.class.getName());

    static UserManager userManager = EndpointImpl.api().getUserManager();
    
	@Override
	public void print(PageContext pc, HttpServletResponse response) throws IOException
	{
		
		String uriString = null;
		String abbreviatedUri = null;
		PrinterExtensions ext = Global.printerExtension;
		
		if ( pc.value instanceof URI )
		{
    		uriString = pc.value.stringValue();
    		if (uriString != null)
                uriString = StringUtil.urlEncode(uriString);
    		
    		abbreviatedUri = EndpointImpl.api().getNamespaceService().getAbbreviatedURI((URI)pc.value);
		}
		
		String htmlHead = computeHtmlHead(pc);		
		String googleAnalytics = ext.addGoogleAnalytics(Config.getConfig().getGoogleAnalytics());
		String user = userManager.getUserName(null);		
		String toolBarLeft = ext.addToolbarLeft(pc, htmlHead, user, userManager.isLoggedIn(null));
		
		ServletPageParameters pageParams = ServletPageParameters.computePageParameter(pc);
		
		
		String mainStringTemplate = pc.mobile ? 
				"com/fluidops/iwb/server/MobileServlet" :
					"com/fluidops/iwb/server/Servlet";
			
		TemplateBuilder tb = new TemplateBuilder( "tplForClass", mainStringTemplate);

		Map<String, Object> args = pageParams.toServletStringBuilderArgs();
		args.put("licenseNagging", ext.getLicenseWarning(pc));	
		args.put("tracking", googleAnalytics);							// TODO maybe solve otherwise", value
		args.put("uriString", uriString);								// TODO think if we still require this
		args.put("toolBarLeft", toolBarLeft);							// TODO maybe deprecate (not used), value
		args.put("abbreviatedURI", abbreviatedUri);						// TODO maybe remove, not documented nor used in both iwb and ecm, value
		args.put("term", pageParams.getPageTitle());					// keep for legacy reasons
		args.put("path", pc.contextPath);								// keep for legacy reasons
		
		String content = tb.renderTemplate( args );
		response.setContentType( pc.contentType );
		ServletOutputStream out = response.getOutputStream();
		out.write( content.getBytes() );
	}
	
	
	
	/**
	 * Computes the HTML head (i.e. the additional java scripts and
	 * CSS files) that are required. The java script URLs are collected
	 * (recursively) from the {@link PageContext#page} via {@link FComponent#jsURLs()},
	 * while the java scripts required by widgets are retrieved
	 * from the {@link PageContext#container}
	 * 
	 * @param pc
	 * @return
	 */
	public static String computeHtmlHead(PageContext pc) {
		
		Set<String> jsUrls = Sets.newLinkedHashSet();
		
		// collect jsUrls from FComponent#jsUrls recursively
		String[] cJSUrls = pc.page.jsURLs();
		if (cJSUrls!=null)
			jsUrls.addAll(Arrays.asList(cJSUrls));
		
		// collect jsUrls from AbstractWidget#jsUrls
		if(pc.container!=null)
			jsUrls.addAll( pc.container.jsUrls() );
		
		StringBuffer res = new StringBuffer();
			for ( String head : jsUrls )
				res.append( "<script type='text/javascript' src='" ).append( head ).append( "'></script>\n" );
		
		// append also CSS Urls
		String[] cssURLs = pc.page.cssURLs();
		if ( cssURLs != null )
			for ( String head : cssURLs )
				res.append( "<link rel='stylesheet' type='text/css' href='" ).append( head ).append( "'/>\n" );
		
		return res.toString();
	}
    
	/**
	 * A POST form that extracts the HTML anchor of the Wiki view,
	 * and sends it to the PDF servlet.
	 * @param htmlHead 
	 * 
	 * @return
	 */
	public static String getPdfGenerationPrefix(PageContext pc, String htmlHead)
	{
		Value v = pc.value;
		if (!(v instanceof URI))
			return "";
		String path = pc.contextPath;
        
        String templatePref = "<!DOCTYPE html PUBLIC\\\"-//W3C//DTD HTML 4.01 Transitional//EN\\\" \\\"http://www.w3.org/TR/html4/loose.dtd\\\">";
        templatePref = "<html><head>";
        templatePref += "<title>Print View for " + EndpointImpl.api().getRequestMapper().getReconvertableUri((URI)pc.value, true) + "</title><script type=\\'text/javascript\\' src=\\'file:webapps/ROOT/ajax/ajax.js\\'></script>";
        templatePref += htmlHead.replace("'", "\\'").replace("\n","");        
        templatePref += "<script type=\\'text/javascript\\' src=\\'" + path + "/ajax/ajax.js\\'></script>";
        templatePref += "<link rel=\\'stylesheet\\' href=\\'" + path + "/ajax/stylesheet_fajax.css\\' type=\\'text/css\\' />";
        templatePref += "<link rel=\\'stylesheet\\' href=\\'" + path + "/stylesheet_fiwb.css\\' type=\\'text/css\\' />";        
        templatePref += "<!--[if lte IE 7]><link rel=\\'stylesheet\\' href=\\'" + path + "/css/ie7hacks.css\\' type=\\'text/css\\' /><![endif]-->";
        templatePref += "<!--[if IE 8]><link rel=\\'stylesheet\\' href=\\'" + path + "/css/ie8hacks.css\\' type=\\'text/css\\' /><![endif]-->";
        templatePref += "<link rel=\\'stylesheet\\' href=\\'" + path + "/css/semwiki.css\\' type=\\'text/css\\' />";
//        templatePref += "</head><body onload=\\'javascript:fluInit();\\'>";
        templatePref += "</head><body>";

        return templatePref;
	}
	
	/**
	 * A POST form that extracts the HTML anchor of the Wiki view,
	 * and sends it to the PDF servlet.
	 * 
	 * @return
	 */
	public static String getPdfGenerationSuffix(PageContext pc)
	{
		Date nowDate = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss");
		String now = sdf.format(nowDate);
		

		String versionDateStr = null;
		String version = pc.getRequestParameter("version");
		if (!StringUtil.isNullOrEmpty(version))
		{
			try
			{
				Long timestamp = Long.valueOf(version);
				Date versionDate = new Date();
				versionDate.setTime(timestamp);
				versionDateStr = " (version from " + sdf.format(versionDate) + ")";
			}
			catch (Exception e)
			{
				logger.warn(e.getMessage());
			}
		}
		if (versionDateStr==null)
			versionDateStr = " (latest version)";
		
		
		String templateSuf = "<hr/>";
		templateSuf += "<i>Printable version of document <b>" + EndpointImpl.api().getRequestMapper().getReconvertableUri((URI)pc.value, true) + "</b>" + versionDateStr + ". The document was generated on " + now + " by user " + userManager.getUserName(null) + ".";
		templateSuf += "</body></html>";
		
		return templateSuf;
	}
}
