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

package com.fluidops.iwb.wiki;

import java.util.Date;
import java.util.Map;

import org.apache.log4j.Logger;
import org.openrdf.model.URI;

import com.fluidops.ajax.components.FComponent;
import com.fluidops.iwb.wiki.FluidWikiModel.TemplateResolver;
import com.fluidops.iwb.wiki.parserfunction.ParserFunctionUtil;

/**
 * The implementation of the template resolver which in particular
 * takes care for resolving wiki templates from the file system
 * 
 * @author as
 *
 */
public class FluidTemplateResolver implements TemplateResolver {

	private final static Logger logger = Logger.getLogger(FluidTemplateResolver.class);
	
	private final FluidWikiModel wikiModel;
	private final Date pageVersion;		// the version of the page
	
	private boolean ignoreErrors = false;
	
	@edu.umd.cs.findbugs.annotations.SuppressWarnings(value="EI_EXPOSE_REP2", justification="Checked")	
	public FluidTemplateResolver(FluidWikiModel wikiModel, Date pageVersion) {
		super();
		this.wikiModel = wikiModel;
		this.pageVersion = pageVersion;
	}

	public String resolveTemplate(String namespace,
            String templateName, Map<String, String> templateParameters, URI page, FComponent parent) 
    {                
       
        //safesubst is not supported
        if(templateName.startsWith("safesubst"))
            return "&nbsp;"; 
        
        // Special case: template lang-XX where XX=country code
        // This is used as a marker template - return the text / Uli
        if ( templateName.startsWith("lang-") )
        	return templateParameters.get("1");
        
        try {
	        String templ = wikiModel.getIncludedTemplate(templateName, namespace, pageVersion);
	
	        if (templ!=null)
	            return wikiModel.resolveIncludedTemplate(templ, templateName, templateParameters);
              
	        return null;
        } catch (Exception e) {
        	if (ignoreErrors) {
        		logger.trace("Error while resolving template: " + e.getMessage(), e);
        		return null;
        	}
        	return ParserFunctionUtil.renderError(e);
        }
        
    }
	
	/**
	 * Flag indicating if errors are ignored, e.g. if a template name cannot be
	 * parsed to a valid URI
	 * @param ignoreErrors
	 */
	public void setIgnoreErrors(boolean ignoreErrors) {
		this.ignoreErrors = ignoreErrors;
	}
}
