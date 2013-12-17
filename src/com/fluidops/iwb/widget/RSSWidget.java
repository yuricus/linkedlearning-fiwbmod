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

import java.util.List;

import com.fluidops.ajax.FClientUpdate;
import com.fluidops.ajax.FClientUpdate.Prio;
import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FContainer;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.model.ParameterConfigDoc;
import com.fluidops.iwb.model.TypeConfigDoc;
import com.fluidops.iwb.widget.AbstractWidget;
import com.fluidops.iwb.widget.WidgetEmbeddingError;
import com.fluidops.iwb.widget.config.WidgetBaseConfig;
import com.fluidops.util.StringUtil;
import com.google.common.collect.Lists;

/**
 * The widget uses a JQuery plugin to display RSS feeds from a given feed URL
 * 
 * @author ango
 *
 */
@TypeConfigDoc("The RSS widget displays RSS feeds from a given feed URL")
public class RSSWidget extends AbstractWidget<RSSWidget.Config>
{

	public static class Config extends WidgetBaseConfig
	{
		@ParameterConfigDoc(
				desc = "The feed URL",
				required = true)
		public String feedURL;

		@ParameterConfigDoc(
				desc = "The maximal number of the displayed feed items",
				defaultValue="5")
		public Integer maxItems = 5;

		@ParameterConfigDoc(
				desc = "The parameter specifies whether the description of the feed item should be displayed or not",
				defaultValue="false")
		public Boolean showDescription = false;
		
		@ParameterConfigDoc(
				desc = "The description character limit. There is no limit per default.")
		public Integer descCharacterLimit;

		@ParameterConfigDoc(
				desc = "The parameter specifies whether the creation date of the feed item should be displayed or not",
				defaultValue = "false")
		public Boolean showDate = false;
	}

	@Override
	public FComponent getComponent(String id)
	{
		final Config conf = get();

		if(conf.feedURL == null)
			return WidgetEmbeddingError.getErrorLabel(id,
					WidgetEmbeddingError.ErrorType.MISSING_INPUT_VARIABLE,
					"The feed URL is not specified");
		
		FContainer cont = new FContainer(id){
			@Override
			public String render()
			{
				String characterLimit = "";
				if(conf.descCharacterLimit != null)
					characterLimit += "DescCharacterLimit: "+conf.descCharacterLimit;
			    
				addClientUpdate(new FClientUpdate(Prio.VERYEND,
			                    " jQuery(document).ready(function($){ $('#"+getComponentid()+"').FeedEk({"+
								"  FeedUrl : '"+conf.feedURL+"',"+
								"  MaxCount : "+conf.maxItems+","+
								"  ShowDesc : "+conf.showDescription+","+
						        "  ShowPubDate: "+conf.showDate+"," +
						        "  "+characterLimit+" });});"));
				return ""; 

			}
		};		 

		// set size
		if (StringUtil.isNotNullNorEmpty(conf.width))
			cont.addStyle("width", conf.width + "px");
		if (StringUtil.isNotNullNorEmpty(conf.height))
			cont.addStyle("height", conf.height + "px");
		
		cont.appendClazz("rssWidget");
		
		return  cont;  }

	@Override
	public String getTitle()
	{
		return "RSS feed";
	}

	@Override
	public List<String> jsURLs( )
	{
		String cp = EndpointImpl.api().getRequestMapper().getContextPath();
		return Lists.newArrayList(cp+"/jquery/FeedEk.js");
	}

	@Override
	public Class<?> getConfigClass()
	{
		return RSSWidget.Config.class;
	}

}