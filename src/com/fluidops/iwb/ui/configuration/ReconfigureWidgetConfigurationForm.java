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

package com.fluidops.iwb.ui.configuration;

import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FContainer;
import com.fluidops.ajax.components.FPopupWindow;
import com.fluidops.iwb.api.operator.Operator;
import com.fluidops.iwb.api.operator.OperatorFactory;
import com.fluidops.iwb.api.operator.OperatorNode;
import com.fluidops.iwb.layout.AdHocSearchTabWidgetContainer;
import com.fluidops.iwb.widget.AbstractWidget;
import com.fluidops.iwb.widget.Widget;
import com.fluidops.iwb.widget.WidgetConfig;
import com.fluidops.util.Rand;

/**
 * The widget reconfiguration form which is used to change the settings of an already
 * configured and rendered widget. Takes initial values from the widget config object.
 * On submission, replaces the widget on the page with a reconfigured one.
 * 
 * This configuration for is used for ad hoc search analysis to reconfigure the
 * widgets matching a chart. See {@link AdHocSearchTabWidgetContainer}
 * 
 * @author andriy.nikolov
 * @author as
 *
 */
public class ReconfigureWidgetConfigurationForm extends WidgetConfigurationFormBase {

	
	/**
	 * Populates and shows the popup with a {@link ReconfigureWidgetConfigurationForm} that
	 * is filled with the given {@link AbstractWidget}'s configuration. The actual
	 * widgetComponent is required for rendering the adjusted widget after submitting,
	 * i.e. to populate changes to the configuration.
	 *  
	 * @param popup
	 * @param widget the widget to configure
	 */
	public static void showReconfigureWidgetConfigurationForm(FPopupWindow popup, AbstractWidget<?> widget, FComponent widgetComponent) {
		
		@SuppressWarnings("unchecked")
		WidgetConfig widgetConfig = new WidgetConfig(null, (Class<? extends Widget<?>>) widget.getClass(), null, widget.getMapping(), true);
		ReconfigureWidgetConfigurationForm form = new ReconfigureWidgetConfigurationForm(Rand.getIncrementalFluidUUID(), widget, widgetConfig, widgetComponent);		
		ConfigurationFormUtil.showConfigurationFormInPopup(popup, "Edit widget", form);
	}

	
	private final AbstractWidget<?> widget;
	private FComponent widgetComponent;
	
	private ReconfigureWidgetConfigurationForm(String id, AbstractWidget<?> widget, WidgetConfig widgetConfig, FComponent widgetComponent) {
		super(id, widgetConfig);
		this.widget = widget;
		this.widgetComponent = widgetComponent;
	}
	
	

	@Override
	protected void submitData(OperatorNode data) {

		if (data==null)
			throw new IllegalArgumentException("Please provide a valid configuration.");

		Operator widgetInput = OperatorFactory.toOperator(data);		
		widget.setMapping(widgetInput);

		FContainer parent = (FContainer) widgetComponent.getParent();
		parent.remove(widgetComponent);
		widgetComponent.setParent(null);

		widgetComponent = widget.getComponentUAE( widgetComponent.getId() );
		parent.add(widgetComponent);
		parent.populateView();
		getPage().getPopupWindowInstance().hide();
	}

}
