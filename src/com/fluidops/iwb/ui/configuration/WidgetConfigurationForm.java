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

import java.rmi.RemoteException;
import java.util.List;

import org.apache.log4j.Logger;
import org.openrdf.model.URI;

import com.fluidops.ajax.components.FComboBox;
import com.fluidops.ajax.components.FPopupWindow;
import com.fluidops.ajax.components.FTextArea;
import com.fluidops.ajax.components.FTextInput2;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.operator.Operator;
import com.fluidops.iwb.api.operator.OperatorFactory;
import com.fluidops.iwb.api.operator.OperatorNode;
import com.fluidops.iwb.util.validator.ConvertibleToUriValidator;
import com.fluidops.iwb.widget.Widget;
import com.fluidops.iwb.widget.WidgetConfig;
import com.fluidops.util.Rand;
import com.fluidops.util.StringUtil;
import com.google.common.base.Throwables;

/**
 * Widget configuration for for Admin:Widgets page, i.e. to configure
 * the system wide widgets.
 * 
 * This form offers addition elements to control the widget configuration:
 * 
 * a) value: the resource to apply the widget to
 * b) applyToInstances: whether this configuration should be applied to instances
 * c) preCondition: a textarea to formulate a pre-condition as SPARQL ASK query
 * 
 * @author as
 *
 */
public class WidgetConfigurationForm extends WidgetConfigurationFormBase {

	private static Logger logger = Logger.getLogger(WidgetConfigurationForm.class);
	
	/**
	 * Populates and shows the popup with a {@link WidgetConfigurationForm} that
	 * is filled with the given {@link WidgetConfig} (if available). If no
	 * widget config is given, an empty form is displayed.
	 * 
	 * @param popup
	 * @param title
	 * @param widgetConfig the widget config or null (for an empty form)
	 */
	public static void showWidgetConfigurationForm(FPopupWindow popup, String title, WidgetConfig widgetConfig) {
		WidgetConfigurationForm form = new WidgetConfigurationForm(Rand.getIncrementalFluidUUID(), widgetConfig);
		ConfigurationFormUtil.showConfigurationFormInPopup(popup, title, form);
	}
	
	
    protected FTextInput2 value;    
    protected FComboBox applyToInstances;    
    protected FTextArea preCondition;
    
    protected Operator oldInput = null;
        
	/**
	 * @param id
	 * @param widgetConfig
	 */
	private WidgetConfigurationForm(String id, WidgetConfig widgetConfig) {
		super(id, widgetConfig);
		if(widgetConfig!=null) {
			oldInput = widgetConfig.input;
		}
	}

	@Override
	protected void submitData(OperatorNode data) {
		
		if (data==null)
			return;		// do nothing, empty form
		
		Boolean forInstances = Boolean.parseBoolean((String)applyToInstances.getSelected().get(0));
        URI uri = EndpointImpl.api().getNamespaceService().guessURI(value.getValue());
        String condition = preCondition.getValue();
        
        Class<? extends Widget<?>> widgetClass = getWidgetClass();
        
        Operator widgetInput = OperatorFactory.toOperator(data);
        
		if (!isEditMode() && widgetConfigExists(widgetClass, widgetInput, uri, forInstances)) {

			// TODO show this error in an additional popup as everything gets deleted
			getPage().getPopupWindowInstance().showError(
							widgetClass.getSimpleName()	+ " configuration "
									+ (forInstances ? "for the resources of the type "	: "for the resource ")
									+ "<br />'"	+ uri + "'<br /> already exists. Click 'edit' to change it.");
			return;
		}
        
        try {
        	if(!isEditMode()) {
        		EndpointImpl.api().getWidgetSelector().addWidget(widgetClass, widgetInput, uri, forInstances, condition);
        	} else if(oldInput!=null) {
        		EndpointImpl.api().getWidgetSelector().updateWidget(widgetClass, oldInput, widgetInput, uri, forInstances, condition);
        	}
        	
        	getPage().getPopupWindowInstance().showInfoAndRefresh("Info", "Widget successfully " + (isEditMode() ? "edited" : "added") );
        }
        catch (Exception e)  {
            logger.error("Widget configuration failed: " + e.getMessage());
            getPage().getPopupWindowInstance().showWarning("Widget " + (isEditMode() ? "editing" : "adding") + "failed: "+ e.getMessage());
        }
	}

	@Override
	protected void addAdditionalFormElements() {		
		super.addAdditionalFormElements();
		
		// the value to which the widget configuration is applied
		value = new FTextInput2("Value");
		value.setValidator(new ConvertibleToUriValidator());
		if (isEditMode()) {
			value.setEnabledWithoutRefresh(false);
	        if (!(widgetConfig.value instanceof URI))
	        	throw new IllegalStateException("WidgetConfig#value must be a URI: " + widgetConfig.value);
	        String configValue = EndpointImpl.api().getRequestMapper().getReconvertableUri((URI)widgetConfig.value, false);
	        value.setValueWithoutRefresh(configValue);	        
		}
        addFormElement("Value", value, true, new ConvertibleToUriValidator());        
        
        // defines if the widget is applied on the resource or its instances
        applyToInstances = new FComboBox("Apply to instances");
        applyToInstances.addChoice("true", "true");
        applyToInstances.addChoice("false", "false");
        if (isEditMode()) {
        	applyToInstances.setEnabled(false);
	        applyToInstances.setSelected(String.valueOf(widgetConfig.applyToInstances));
        }
        addFormElement("Apply to instances", applyToInstances, true, new VoidValidator());
        
        // text area to define a pre-condition
        preCondition = new FTextArea("pre-Condition");
        preCondition.appendClazz("smallTextArea");
        if (isEditMode()) {
	        if(StringUtil.isNotNullNorEmpty(widgetConfig.preCondition))
	        	preCondition.value = widgetConfig.preCondition;
        }
        addFormElement("Pre-Condition", preCondition, false, new VoidValidator());        
	}	

	/**
	 * Returns true if an widget configuration for the given settings exists, 
	 * i.e. there can only be a single widget configuration per class, resource 
	 * and applyToInstances.
	 * 
	 * @param widgetClass
	 * @param uri
	 * @param forInstances
	 * @return
	 */
	private boolean widgetConfigExists(Class<? extends Widget<?>> widgetClass,
			Operator widgetInput,
			URI uri, Boolean forInstances) {
		List<WidgetConfig> configs = getWidgetConfigs();
		for (WidgetConfig c : configs) {
			if (c.input.toString().equals(widgetInput.toString()) 
					&& c.widget.equals(widgetClass) 
					&& c.value.equals(uri) 
					&& c.applyToInstances == forInstances) {

				return true;
			}
		}
		return false;
	}
	
	private List<WidgetConfig> getWidgetConfigs() {
		try {
			return EndpointImpl.api().getWidgetSelector().getWidgets();
		} catch (RemoteException ex) {
			throw Throwables.propagate(ex);
		}
	}
}
