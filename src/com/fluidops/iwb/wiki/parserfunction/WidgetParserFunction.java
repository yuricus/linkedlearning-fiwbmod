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

import static com.google.common.base.Strings.isNullOrEmpty;
import info.bliki.wiki.model.IWikiModel;
import info.bliki.wiki.template.AbstractTemplateFunction;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.fluidops.ajax.components.FComponent;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.operator.Operator;
import com.fluidops.iwb.page.PageContext;
import com.fluidops.iwb.ui.AnnotateLinkComponent;
import com.fluidops.iwb.widget.Widget;
import com.fluidops.iwb.widget.WidgetEmbeddingError;
import com.fluidops.iwb.widget.WidgetEmbeddingError.ErrorType;
import com.fluidops.iwb.wiki.FluidWikiModel;

/**
 * A parser function which creates a widget component and renders the html anchor at
 * its position. The rendered {@link FComponent} is registered to the {@link FluidWikiModel}
 * such that it gets properly populated.
 * 
 * @author as
 *
 */
public class WidgetParserFunction extends AbstractTemplateFunction implements PageContextAwareParserFunction {

	private static Logger logger = Logger.getLogger(WidgetParserFunction.class);
	
	private final FComponent parent;
	
	private PageContext pc;	
	
	
	/**
	 * @param parent
	 */
	public WidgetParserFunction(FComponent parent) {
		super();
		this.parent = parent;
	}

	@Override
	public String parseFunction(List<String> parts, IWikiModel model,
			char[] src, int beginIndex, int endIndex, boolean isSubst)
			throws IOException {		
		if (parts.size()==0)
			return null;
		
		String widgetName = parts.get(0).trim(); // the short name or fully qualified class
		FComponent cmp = createWidgetComponent(widgetName, ParserFunctionUtil.getTemplateParameters(parts));
		FluidWikiModel.addRenderedComponent( cmp );
		return ParserFunctionUtil.getAnchor(cmp, parent);
	}

	/**
	 * Create the {@link FComponent} representing the widget or an {@link WidgetEmbeddingError}
	 * label (in case of an error).
	 * 
	 * @param widgetName
	 * @param templateParameters
	 * @return
	 */
	@edu.umd.cs.findbugs.annotations.SuppressWarnings(value="REC_CATCH_EXCEPTION", justification="Used for error handling")
	public FComponent createWidgetComponent(String widgetName, Map<String, String> templateParameters) {
		
		try
		{
			String widgetClassName = EndpointImpl.api().getWidgetService().getWidgetClass( widgetName );
			
            if (widgetClassName==null)
            	return WidgetEmbeddingError.getErrorLabel("w"+AnnotateLinkComponent.getNextId(), ErrorType.GENERIC, 
            			"Error: Illegal widget or widget improperly registered in the configuration: " + widgetName);

			Class<?> widgetClass = Class.forName( widgetClassName );
			Widget<?> widget = (Widget<?>) widgetClass.newInstance();
			
			// build page context from parent's page context
			PageContext childPageContext = PageContext.createChildPageContext(pc);
            
			widget.setPageContext(childPageContext);
			
			//************ WORKAROUND ***************\\
			// in this case, unnamed parameters are used, such as {{#widget: Address | 'Bruchsal'}}
			// Actually we ask here, whether the field "1" is set (this would correspond to 1 = 'Bruchsal')
			// This is a workaround, since the previous if statement was broken, since bliki 3.0.16
			if(templateParameters.size()==0) {
                widget.setMapping(Operator.createNoop());
			}
			else if(!isNullOrEmpty(templateParameters.get("1")) 
					&& !isNullOrEmpty(templateParameters.get("1").trim()))
			{
			    String mappingString = templateParameters.get( "1" );
			    widget.setMapping( Operator.parse(mappingString, pc.value) );
			}
			else // named parameters are used
			{						    
			    widget.setMapping( Operator.parseStruct(templateParameters, pc.value) );
			}
			
			String id = "w"+AnnotateLinkComponent.getNextId();
			FComponent comp = widget.getComponentUAE( id );
			
			// make sure ID is indeed used as widget ID
            if (!comp.getId().equals(id))
                throw new RuntimeException(
                        "Wrong widget ID in constructed widget (class='"
                                + comp.getClass() + "'). Is " + comp.getId() 
                                + " but should be " + id
                                + ". Please implement getComponent(id) properly "
                                + "(make sure you use the parameter id).");
            
			return comp;

		} catch (Exception e)  {			
			logger.error("Error during widget parsing: " + e.getMessage());
			logger.debug("Details:", e);			
			return WidgetEmbeddingError.getErrorLabel(
					"w"+AnnotateLinkComponent.getNextId(),
					ErrorType.EXCEPTION, e.toString());
		}
	}
	
	@Override
	public void setPageContext(PageContext pc) {
		this.pc = pc;		
	}

	@Override
	public String getFunctionName() {
		return "#widget";
	}	
}
