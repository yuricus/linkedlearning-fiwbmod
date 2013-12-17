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

import org.openrdf.model.impl.ValueFactoryImpl;

import com.fluidops.ajax.FClientUpdate;
import com.fluidops.ajax.FClientUpdate.Prio;
import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FContainer;
import com.fluidops.ajax.components.FDialog;
import com.fluidops.ajax.components.FDialog.Effect;
import com.fluidops.ajax.components.FDialog.Position;
import com.fluidops.ajax.components.FImageButton;
import com.fluidops.ajax.components.FLabel;
import com.fluidops.ajax.components.FLabel.ElementType;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.cms.util.IWBCmsUtil;
import com.fluidops.iwb.model.ParameterConfigDoc;
import com.fluidops.iwb.model.ParameterConfigDoc.Type;
import com.fluidops.iwb.model.TypeConfigDoc;
import com.fluidops.iwb.widget.WidgetEmbeddingError.NotificationType;
import com.fluidops.iwb.widget.config.WidgetBaseConfig;
import com.fluidops.util.StringUtil;

/**
 * The callout widget shows content (some text or/and an image) in a
 * dialog window on click or on page load. The initial view is a
 * clickable icon (configurable) .
 * 
 * @author ango
 */
@TypeConfigDoc("The callout widget displays a button. A click on the button triggers a dialog window with the callout content.")
public class CalloutWidget extends AbstractWidget<CalloutWidget.Config> {

	private static String cssClass = "calloutWidget";
	
	public static class Config extends WidgetBaseConfig {

		@ParameterConfigDoc(
				desc = "Location URI of the image for the callout button, e.g. http://external.com/myImg.png or File:myUploadedImage.png", 
				type = Type.SIMPLE)
		public String image;

		@ParameterConfigDoc(
				desc = "An optional title for the callout button", 
				defaultValue = "", 
				type = Type.SIMPLE)
		public String title = "";

		@ParameterConfigDoc(
				desc = "Callout content", 
				required = true, 
				type = Type.CONFIG)
		public CalloutConfig callout;

		@ParameterConfigDoc(
				desc = "Defines whether the callout should be displayed on page load or on button click", 
				defaultValue = "false")
		public Boolean calloutOnPageLoad = false;
	}
	
	
	public static class Size {
		
		@ParameterConfigDoc(
				desc = "The minimal height of the callout window in pixel", 
				defaultValue = "auto", type = Type.SIMPLE)
		public Integer minHeight;
		
		@ParameterConfigDoc(
				desc = "The maximal height of the callout window in pixel", 
				defaultValue = "auto", type = Type.SIMPLE)
		public Integer maxHeight;
		
		@ParameterConfigDoc(
				desc = "The minimal width of the callout window in pixel", 
				defaultValue = "auto", type = Type.SIMPLE)
		public Integer minWidth;
		
		@ParameterConfigDoc(
				desc = "The maximal width of the callout window in pixel", 
				defaultValue = "auto", type = Type.SIMPLE)
		public Integer maxWidth;
		
	}
	

	public static class CalloutConfig {

		@ParameterConfigDoc(
				desc = "The callout title", 
				defaultValue = "", type = Type.SIMPLE)
		public String title;

		@ParameterConfigDoc(
				desc = "Location URI of the image to be displayed in the callout content", 
				type = Type.SIMPLE)
		public String image;

		@ParameterConfigDoc(
				desc = "The callout text", 
				defaultValue = "", 
				type = Type.TEXTAREA)
		public String text;

		@ParameterConfigDoc(
				desc = "The position of the callout window in relation to the browser window", 
				defaultValue = "center")
		public Position position;

		@ParameterConfigDoc(
				desc = "The effect for showing and hiding the callout window", 
				defaultValue = "blind")
		public Effect effect;

		@ParameterConfigDoc(
				desc = "Callout size (minHeight, maxHeight, minWidth, maxWidth)", 
				type = Type.CONFIG)
		public Size size;
	}

	@Override
	public FComponent getComponent(String id) {

		final Config c = get();

		if (!validate(c))
			return WidgetEmbeddingError.getNotificationLabel(id,
					NotificationType.NO_DATA, "no callout content defined");

		String cp = EndpointImpl.api().getRequestMapper().getContextPath();

		String imageSource = StringUtil.isNullOrEmpty(c.image) ? cp	+ "/images/navigation/i.gif" : c.image;

		if (imageSource.startsWith(EndpointImpl.api().getNamespaceService()
				.fileNamespace()))
			imageSource = IWBCmsUtil.getAccessUrl(ValueFactoryImpl
					.getInstance().createURI(imageSource));

		c.callout.position = c.callout.position == null ? Position.CENTER : c.callout.position;
		c.callout.effect = c.callout.effect == null ? Effect.BLIND : c.callout.effect;


		final FDialog dialogContainer = new FDialog("dialCont", c.callout.title, c.callout.position, c.callout.effect);

		if(c.callout.size != null)
		{
			dialogContainer.setMinHeight(c.callout.size.minHeight);
			dialogContainer.setMaxHeight(c.callout.size.maxHeight);
			dialogContainer.setMinWidth(c.callout.size.minWidth);
			dialogContainer.setMaxWidth(c.callout.size.maxWidth);
			
		}
		
		if (c.calloutOnPageLoad != null && c.calloutOnPageLoad) 
			dialogContainer.setVisible(true);

		FContainer callout = new FContainer(id){
			@Override
			public String render(){
				addClientUpdate( new FClientUpdate( Prio.VERYEND, "jQuery(document).ready(function($){" +
			            "$('."+cssClass+"').draggable({ cursor: 'move', cancel: '.flDialogContent div' });});" ) );
				return super.render();
			}
		}; 

		// Title 
		if (StringUtil.isNotNullNorEmpty(c.title)) {
			FLabel title = new FLabel("title", c.title);
			title.setElement(ElementType.H4);
			callout.add(title);
		}

		final FImageButton openButton = new FImageButton("opimg", imageSource) {

			@Override
			public void onClick() {
				// triggers opening a dialog window via jQuery
				dialogContainer.show();
			}
		};

		addContent(dialogContainer, c.callout);

		callout.add(openButton);
		callout.add(dialogContainer);
		//allow css customizing of the widget
		callout.appendClazz(cssClass);
		dialogContainer.setDialogClass("calloutWindow");

		// set size
		if (StringUtil.isNotNullNorEmpty(c.width))
			callout.addStyle("width", c.width + "px");
		if (StringUtil.isNotNullNorEmpty(c.height))
			callout.addStyle("height", c.height + "px");

		return callout;
	}

	/**
	 * Add the callout content (image/text) to the dialog window
	 * @param dialogContainer
	 * @param callout
	 */
	private void addContent(FDialog dialogContainer, CalloutConfig c)
	{
		// image
		if (StringUtil.isNotNullNorEmpty(c.image)) {
			
			if (c.image.startsWith(EndpointImpl.api().getNamespaceService()
					.fileNamespace()))
				c.image = IWBCmsUtil.getAccessUrl(ValueFactoryImpl
						.getInstance().createURI(c.image));
			
			FLabel imageLabel = new FLabel("img", c.image);
			imageLabel.setElement(ElementType.IMG);
			dialogContainer.add(imageLabel);
		}

		// text
		if (StringUtil.isNotNullNorEmpty(c.text)) {
			FLabel textLabel = new FLabel("text", c.text);
			dialogContainer.add(textLabel);
		}

	}

	/**
	 * Validate if the config contains the minimum configuration
	 * @param config
	 * @return
	 */
	private boolean validate(Config config)
	{
		return (config != null
				&& config.callout != null
				&& (StringUtil.isNotNullNorEmpty(config.callout.image) || StringUtil.isNotNullNorEmpty(config.callout.text)));
	}

	@Override
	public String getTitle() {
		String title = get().title;
		return StringUtil.isNullOrEmpty(title) ? "" : title;
	}

	@Override
	public Class<?> getConfigClass() {
		return Config.class;
	}
}