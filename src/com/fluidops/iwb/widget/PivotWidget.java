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

package com.fluidops.iwb.widget;

import static com.fluidops.iwb.api.EndpointImpl.api;

import java.util.regex.Matcher;

import org.openrdf.model.URI;

import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.PivotControl;
import com.fluidops.iwb.model.ParameterConfigDoc;
import com.fluidops.iwb.model.TypeConfigDoc;
import com.fluidops.iwb.widget.config.WidgetQueryConfig;
import com.fluidops.util.StringUtil;

/**
 * displays PivotViewer as Widget via embedded Silverlight control
 * 
 * @author pha
 */
@TypeConfigDoc("The pivot widget provides an interactive way to browse in a collection of items, that are visualized as images.")
public class PivotWidget extends AbstractWidget<PivotWidget.Config>
{

    public static class Config extends WidgetQueryConfig
    {
		@ParameterConfigDoc(
				desc = "The maximum number of the displayed entities. Default: 1000")
        public Integer maxEntities;
				
		@ParameterConfigDoc(
				desc = "The maximum number of the displayed facets. Default: no limit")
        public Integer maxFacets;
    }

    @Override
    public FComponent getComponent(String id)
    {
		URI uri = null;
		if (pc.value instanceof URI)
			uri = (URI) pc.value;

        Config config = get();
        if (config==null) {
        	config=new Config();
        	if (uri != null)
            {
                config.query = "CONSTRUCT { ?uri ?p ?o . } WHERE {   <"
                    + uri.stringValue()
                    + "> ?relationship  ?uri . ?uri ?p ?o }";
            } else {
            	config.query = "";
            }
        }
        String query = config.query;

        String servlet = api().getRequestMapper().getExternalUrl(pc.getRequest(), "/query.cxml");
        
        if (uri != null) query = query.replaceAll("\\?\\?", "<"+ Matcher.quoteReplacement(uri.stringValue()) + ">");

        //encode query (max length problem on mac and newest silverlight version 16.06.11)
        query = PivotControl.encodeQuery(query);
        query = servlet+"?q="+query;
        if (uri != null) query = query+"&uri="+StringUtil.urlEncode(uri.stringValue());
        
        if(config.maxEntities != null)
        	query += "&maxEntities="+config.maxEntities;
        if(config.maxFacets != null)
        	query += "&maxFacets="+config.maxFacets;
        
        return new PivotControl(id, query, useJSPivotViewer());
    }

    @Override
    public String getTitle()
    {
        return "Pivot Viewer";
    }

    @Override
    public Class<Config> getConfigClass()
    {
        return Config.class;
    }

    /**
     * Returns true if the JS version of the Pivot view is to be used, false
     * if the traditional Silverlight version is to be used
     * @return
     */
    public static boolean useJSPivotViewer() {
    	return com.fluidops.iwb.util.Config.getConfig().getBoolean("enableJSPivotViewer", false);
    }
}
