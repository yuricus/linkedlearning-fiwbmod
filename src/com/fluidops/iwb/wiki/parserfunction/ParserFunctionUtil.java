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

package com.fluidops.iwb.wiki.parserfunction;

import info.bliki.wiki.filter.TemplateParser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.openrdf.model.URI;
import org.openrdf.model.Value;

import com.fluidops.ajax.XMLBuilder.Element;
import com.fluidops.ajax.components.FComponent;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.provider.ProviderUtils;

/**
 * Utility functions for use with parser functions
 * 
 * @author as
 *
 */
public class ParserFunctionUtil {

	
	/**
	 * Return the template parameters from the parts in the same way they would
	 * have been computed by the Bliki engine.
	 * 
	 * Compare: info.bliki.wiki.filter.TemplateParser.parseTemplate(Appendable, int, int)
	 * 
	 * @param parts
	 * @return
	 */
	public static Map<String, String> getTemplateParameters(List<String> parts) {
		LinkedHashMap<String, String> parameterMap = new LinkedHashMap<String, String>();
		List<String> unnamedParameters = new ArrayList<String>();
		for (int i = 1; i < parts.size(); i++) {
			String temp = parts.get(i);
			TemplateParser.createSingleParameter(temp, parameterMap, unnamedParameters);			
		}
		TemplateParser.mergeParameters(parameterMap, unnamedParameters);
		return parameterMap;
	}
	
	/**
     * Returns the HTML anchor of the given component.
     * We enter bogus text into the (usually empty) tag to ensure the
     * bliki XML filter doesn't turn the tag "<div ...></div>" into "<div .../>"
     * which will break the DOM tree on the client side.
     * 
     * @param comp
     * @return
     */
    public static String getAnchor( FComponent comp, FComponent parent )
    {
    	comp.setParent(parent);
    	Element anchor = comp.htmlAnchor();
    	// If there's no text, add bogus text
    	if ( anchor.getChild("text()",0)==null )
    		anchor.text("&nbsp;");    		
    	return anchor.toString();
    }
    
	public static String renderNoDataMessage(String noDataMessage) {
    	return "<div class=\"no_data\">" + noWiki(noDataMessage) + "</div>";
	}

	public static String renderError(String errorMessage) {
    	return "<div class=\"parserFunctionError\">" + noWiki(errorMessage) + "</div>";
    }    
	
    public static String renderError(Exception exception) {
    	return renderError(exception.getMessage());
    }
    
    /**
     * Adds the <nowiki> markup around the given text, which 
     * prevents parsing by the bliki engine, but still performs
     * escaping of HTML
     */
    public static String noWiki(String text) {
    	return "<nowiki>" + text + "</nowiki>";    	
    }
    
    /**
	 * Helper method to return either a prefixed URI or the full URI
	 * as <http://...>. The default namespace is represented by
	 * :someProperty
	 * 
	 * @param uri
	 * @return
	 */
	public static String uriToString(URI uri) {
		String res = EndpointImpl.api().getNamespaceService().getAbbreviatedURI(uri);
		if (res!=null) {
			// TODO current NamespaceServiceImpl#getAbbreviatedURI() removes the :
			// for properties in the default namespace. This is not consistent
			// with the scheme of parser functions. Once we decide to change
			// the implementation, we can remove this check.
			if (!res.contains(":"))
				return ":" + res;	
			return res;
		}
		return ProviderUtils.uriToQueryString(uri);
	}
	
	/**
	 * Return the {@link #uriToString(URI)} value for URIs, 
	 * the {@link Value#stringValue()} otherwise
	 *
	 * @param value
	 * @return
	 */
	public static String valueToString(Value value) {
		if (value instanceof URI)
			return uriToString((URI)value);
		return value.stringValue();
	}

}
