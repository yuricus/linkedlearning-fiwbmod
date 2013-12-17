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
import java.util.Set;

import org.apache.log4j.Logger;

import com.fluidops.ajax.components.FComboBox;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.operator.Operator;
import com.fluidops.iwb.widget.Widget;
import com.fluidops.iwb.widget.WidgetConfig;
import com.google.common.collect.Sets;

/**
 * Base class for widget configuration forms, i.e. WikiWidgetConfigurationForm
 * and WidgetConfigurationForm
 * 
 * @author as
 *
 */
public abstract class WidgetConfigurationFormBase extends ConfigurationFormBase {

	private static Logger logger = Logger.getLogger(WidgetConfigurationFormBase.class);

	protected FComboBox widgetsCB;
	protected Class<? extends Widget<?>> selectedWidgetClass;
	
	/**
     * The existing widget configuration or null for an empty form
     */
    protected final WidgetConfig widgetConfig;
	
	/**
	 * editMode: if true, the selected widget cannot be changed (i.e. changing an existing
	 * configuration)
	 */
	private boolean editMode = false;
	
	public WidgetConfigurationFormBase(String id) {
		this(id, null);
	}
	
	public WidgetConfigurationFormBase(String id, WidgetConfig widgetConfig) {
		super(id);
		this.widgetConfig = widgetConfig;
		initialize(widgetConfig);
	}
	
	/**
	 * Initialize this {@link WidgetConfigurationFormBase} instance:
	 * 
	 *  - initializes a widget selection combobox
	 *  - initializes the form component
	 *  - applies form components from widgetConfig (if available)
	 * 
	 * A pre-selected widget class can be provided with the widget
	 * config argunebt
	 *  
	 * @param widgetConfig a filled widgetConfig or null
	 */
	protected void initialize(WidgetConfig widgetConfig) {
		
		Operator defaults = Operator.createNoop();
		if (widgetConfig!=null) {
			defaults = widgetConfig.input;
			editMode = true;
			selectedWidgetClass = widgetConfig.widget;
		}
		
		widgetsCB = initializeWidgetsComboBox();
		widgetsCB.setEnabledWithoutRefresh(!isEditMode());
		
		setConfigurationClassInternal(widgetConfigurationClass(selectedWidgetClass), defaults);
	}
	
	/**
	 * Adds a widget dropdown box
	 */
	@Override
	protected void addAdditionalFormElements() {
		addFormElement("Widget", widgetsCB, true, Validation.NONE);
	}
	
	/**
	 * Determines the edit mode:
	 * true: edit an existing configuration, widget selection cannot be changed
	 * false: new form, any widget can be selected
	 * 
	 * @return
	 */
	protected boolean isEditMode() {
		return editMode;
	}

	/**
	 * Returns the widget short name or the fully qualified class
	 * for the currently selected widget.
	 * 
	 * @return
	 */
	protected String getWidgetName() {
		return widgetName(selectedWidgetClass);
	}
	
	/**
	 * Returns the currently selected widget class
	 * 
	 * @return
	 */
	protected Class<? extends Widget<?>> getWidgetClass() {
		return selectedWidgetClass;
	}
	
	private FComboBox initializeWidgetsComboBox() {
		
        FComboBox res = new FComboBox("widgetTypes") {
			@SuppressWarnings("unchecked")
			@Override
			public void onChange() {

				// set the selection and redraw the view
				Class<? extends Widget<?>> _newSelection = (Class<? extends Widget<?>>) getSelected().get(0);
				if (selectedWidgetClass.equals(_newSelection))
					return;		// not changed
				selectedWidgetClass = _newSelection;
				setConfigurationClass(widgetConfigurationClass(selectedWidgetClass), Operator.createNoop());
			}        	
        };            

        // TODO add the fully qualified class if the widget was preselected
        Set<Class<? extends Widget<?>>> widgets = widgets();
        for(Class<? extends Widget<?>> widgetClass : widgets)
		    res.addChoice( widgetName(widgetClass), widgetClass );
        
        if (selectedWidgetClass==null && widgets.size()>0)
        	selectedWidgetClass = widgets.iterator().next();
        
        res.setPreSelected(selectedWidgetClass);       
       
        return res;
	}
	
	/**
	 * Retrieve all available widgets from the widget service
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private Set<Class<? extends Widget<?>>> widgets() {
		Set<Class<? extends Widget<?>>> res = Sets.newLinkedHashSet();
        if (isEditMode()) {
        	try {
        		res.add((Class<? extends Widget<?>>) widgetConfig.getConfigurablesClass());
        	} catch (Exception e) {
        		throw new IllegalStateException("Could not add pre-selected widget '" + widgetConfig.getConfigurablesClass() + "': " + e.getMessage());
        	}
        }	        	
	     
		try {
			for(Class<? extends Widget<?>> widgetClass : EndpointImpl.api().getWidgetService().getWidgets())
			    res.add(widgetClass);
		} catch (RemoteException ignore) {
			logger.debug(ignore);
		}
		return res;
	}
	
	/**
	 * Returns the short name of the widget (if available), the
	 * fully qualified name otherwise.
	 * 
	 * @param widgetClass
	 * @return
	 */
	private String widgetName(Class<? extends Widget<?>> widgetClass) {
		
		try {
			return EndpointImpl.api().getWidgetService().getWidgetName(widgetClass.getName());
		} catch (RemoteException ignore) {
			logger.debug(ignore);
		}
		return widgetClass.getName();
	}
	
	private Class<?> widgetConfigurationClass(Class<? extends Widget<?>> widgetClass) {
		
		try {
			return widgetClass.newInstance().getConfigClass();
		} catch (Exception e) {
			logger.debug("Invalid widget use of " +widgetClass.getName() + ":"  + e.getMessage(), e);
			throw new IllegalStateException("widget class not accessible: " + widgetClass.getName() + ": "  + e.getMessage());
		} 
	}
}
