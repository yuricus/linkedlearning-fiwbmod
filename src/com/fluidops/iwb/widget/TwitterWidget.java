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


import com.fluidops.ajax.FClientUpdate;
import com.fluidops.ajax.FClientUpdate.Prio;
import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FContainer;
import com.fluidops.iwb.model.ParameterConfigDoc;
import com.fluidops.iwb.model.TypeConfigDoc;
import com.fluidops.iwb.widget.WidgetEmbeddingError.ErrorType;
import com.fluidops.iwb.widget.config.WidgetBaseConfig;
import com.fluidops.util.Rand;
import com.fluidops.util.StringUtil;

/**
 * This widget displays a twitter feed provided a valid twitter id and a widget id. 
 * See https://dev.twitter.com/docs/embedded-timelines to get more information about the widget id. 
 * The widget is meant to be used for the right side of a wiki page (configured in Admin:Widgets) because of external javascript issues.
 */
@TypeConfigDoc(" The Twitter widget incorporates twitter feeds into wiki pages. To configure the widget provide a valid twitter id and a widget id in the widget configuration. See <a href='https://dev.twitter.com/docs/embedded-timelines'>https://dev.twitter.com/docs/embedded-timelines</a> to get more information about the widget id.")
public class TwitterWidget extends AbstractWidget<TwitterWidget.Config>
{
	public static class Config extends WidgetBaseConfig {
		
		@ParameterConfigDoc(
				desc="The twitter user screen name", 
				required=true
			)
		public String twitterID;
		
		@ParameterConfigDoc(
				desc="The ID of the generated widget. For more information see https://dev.twitter.com/docs/embedded-timelines", 
				required=true
			)
		public String widgetID;
	}

	@Override
	public FComponent getComponent( String id )
	{
		final Config c = get();
		
		if(StringUtil.isNullOrEmpty(c.twitterID) || StringUtil.isNullOrEmpty(c.widgetID))
			return WidgetEmbeddingError.getErrorLabel(id, ErrorType.INVALID_WIDGET_CONFIGURATION, "Not all required widget parameters are filled.");
		
		final FContainer twitter = new FContainer(id);

		twitter.add(new FComponent(Rand.getIncrementalFluidUUID()) {
			@Override
			public String render( )
			{
				String twitterName = c.twitterID.startsWith("@") ? c.twitterID.substring(1) != null ? c.twitterID.substring(1) : c.twitterID : c.twitterID;
				c.height = StringUtil.isNotNullNorEmpty(c.height)? c.height : "400";
				c.width = StringUtil.isNotNullNorEmpty(c.width)? c.width: "auto";
				
				addClientUpdate(new FClientUpdate(
							Prio.VERYEND,
							"!function(d,s,id){var js,fjs=d.getElementsByTagName(s)[0];if(!d.getElementById(id)){js=d.createElement(s);"
					        + "js.id=id;js.src='//platform.twitter.com/widgets.js';fjs.parentNode.insertBefore(js,fjs);}}"
					        + "(document,'script','twitter-wjs');"));
					
				return "<a class='twitter-timeline' "
						+ "href='https://twitter.com/"+twitterName+"' "
						+ "data-widget-id='"+c.widgetID+"' "
						+ "data-screen-name='"+twitterName+"' "
						+ "data-theme='light' "
						+ "width='"+c.width+"' "
						+ "height='"+c.height+"' "
						+ "lang='EN'>Tweets by @"+twitterName+"</a>";
			}

		});

		return twitter;

	}

	@Override
	public Class<Config> getConfigClass( )
	{
		return Config.class;
	}

	@Override
	public String getTitle( )
	{
		return "Twitter";
	}

}
