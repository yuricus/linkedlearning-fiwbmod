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

import java.util.Map;

import org.openrdf.model.URI;

import com.fluidops.ajax.components.FComponent;
import com.fluidops.iwb.wiki.FluidWikiModel.TemplateResolver;
import com.fluidops.iwb.wiki.parserfunction.ParserFunctionUtil;
import com.fluidops.iwb.wiki.parserfunction.WidgetParserFunction;

/**
 * For legacy reasons we need to keep a special resolver for #widget (although
 * we nowadays use the {@link WidgetParserFunction}.
 * 
 * The issue with the legacy syntax is that a parser function cannot deal with
 * {{#widget : ...}} properly, i.e. the space before :
 * 
 * Note, however, that this method actual uses the code from
 * {@link WidgetParserFunction} for the rendering.
 * 
 * @author as
 * @see WidgetParserFunction
 */
public class LegacyWidgetTemplateResolver implements TemplateResolver {

	private final WidgetParserFunction widgetParser;
	
	public LegacyWidgetTemplateResolver(WidgetParserFunction widgetParser) {
		super();
		this.widgetParser = widgetParser;
	}

	public String resolveTemplate(String namespace,
            String templateName, Map<String, String> templateParameters, URI page, FComponent parent) 
    {                
    	
    	// for legacy reasons we need to keep this. The actual widget parsing is 
    	// now a parser function, however, a parser function cannot deal with
    	// {{#widget : ...}} properly, i.e. the space before :
    	if ( templateName.startsWith("#widget"))
    	{
    		String widgetName = templateName.substring(templateName.lastIndexOf(":")+1).trim();
    		FComponent comp = widgetParser.createWidgetComponent(widgetName, templateParameters);
			FluidWikiModel.addRenderedComponent( comp );
			return ParserFunctionUtil.getAnchor( comp, parent );          		
    	}
    	
    	return null;
    }
}
