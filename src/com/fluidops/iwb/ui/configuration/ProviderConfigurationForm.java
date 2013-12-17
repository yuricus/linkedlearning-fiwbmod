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

import java.io.Serializable;
import java.lang.reflect.Field;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.openrdf.model.URI;

import com.fluidops.ajax.components.FAbstractTextInputWithAutosuggestion.ElementType;
import com.fluidops.ajax.components.FCheckBox;
import com.fluidops.ajax.components.FComboBox;
import com.fluidops.ajax.components.FPopupWindow;
import com.fluidops.ajax.components.FTextInput2;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.ProviderServiceImpl;
import com.fluidops.iwb.api.operator.Operator;
import com.fluidops.iwb.api.operator.OperatorFactory;
import com.fluidops.iwb.api.operator.OperatorNode;
import com.fluidops.iwb.provider.AbstractFlexProvider;
import com.fluidops.iwb.ui.ProviderEditTable;
import com.fluidops.iwb.user.IwbPwdSafe;
import com.fluidops.iwb.util.User;
import com.fluidops.iwb.util.validator.ConvertibleToUriValidator;
import com.fluidops.util.Rand;
import com.fluidops.util.StringUtil;
import com.fluidops.util.UnitConverter;
import com.fluidops.util.UnitConverter.Unit;
import com.google.common.base.Throwables;
import com.google.common.collect.Sets;

/**
 * A configuration form to manage {@link AbstractFlexProvider} instances. The
 * form is applied from {@link ProviderEditTable} and shown in a popup window.
 * 
 * @author as
 * @see ProviderEditTable
 */
public class ProviderConfigurationForm extends ConfigurationFormBase {

	private static Logger logger = Logger.getLogger(ProviderConfigurationForm.class);
	
	/**
	 * Populates and shows the popup with a {@link ProviderConfigurationForm} that
	 * is filled with the given {@link AbstractFlexProvider} (if available). If no
	 * provider is given, an empty form is displayed.
	 * 
	 * @param popup
	 * @param title
	 * @param provider the provider or null (for an empty form)
	 */
	public static void showProviderConfigurationForm(FPopupWindow popup, String title, AbstractFlexProvider<?> provider) {
		ProviderConfigurationForm p = new ProviderConfigurationForm("i"+Rand.getIncrementalFluidUUID(), provider);
		ConfigurationFormUtil.showConfigurationFormInPopup(popup, title, p);
	}
		
	protected FComboBox providersCB;
	protected Class<? extends AbstractFlexProvider<?>> selectedProviderClass;
	
	protected final AbstractFlexProvider<?> provider;
	
    /**
     * Input fields for the standard provider fields
     */
    public FTextInput2 providerID;
    public FTextInput2 pollInterval;
    public FTextInput2 userName = null;
    public FTextInput2 password = null;
    public FCheckBox providerDataEditable;
    
    /**
     *  Variable to temporarily store the pw of a user while he edits a provider.
     */
    private String tempPwStored = null;
    
	
	/**
	 * editMode: if true, the selected widget cannot be changed (i.e. changing an existing
	 * configuration)
	 */
	private boolean editMode = false;
	
	/**
	 * @param id
	 */
	ProviderConfigurationForm(String id, AbstractFlexProvider<?> provider) {
		super(id);
		this.hideReset = false;
		this.resetButtonLabel = "Cancel";
		this.provider = provider;
		initialize(provider);
	}

	@Override
	protected void submitData(OperatorNode data) {
		
		if (data==null)
			return;		// do nothing
		
		// validation
		URI _providerID = EndpointImpl.api().getNamespaceService().guessURI(providerID.getValue());
		
		if (_providerID==null)
            throw new IllegalArgumentException("providerID cannot be resolved to a valid URI");	// should never happen, validated in form
		
		if (!isEditMode() && providerExists(_providerID)) {
			// TODO use fresh popup for error message
			getPage().getPopupWindowInstance().showError("Provider with id '" + providerID.getValue() + "' already exists.");
            return;
		}
		
		int _pollIntervalInMinutes = Integer.parseInt(pollInterval.getValue());
		boolean _providerDataEditable = providerDataEditable.getChecked();		
		
		try {
			Serializable _providerConfig = (Serializable)OperatorFactory.toOperator(data).evaluate(configClass);
			
			// apply the User object (if a username was provided)
			if (containsUserField() && !StringUtil.isNullOrEmpty(userName.getValue())) {
				User user = new User();
	            user.username = userName.getValue();
	            
	            // Save the password
	            // In case it is not changed (== do not take the **** as PW), restore the tempPW
	            user.password = password.getValue().equals(User.MASKEDPASSWORD) ? tempPwStored : password.getValue();
	            // Clean the temporary data
	            tempPwStored = null;
	            
	            // TODO this is very restrictive, requires field to be called 'user'
	            configClass.getField("user").set(_providerConfig, user);
			}
			
			// Add provider with api method
	        EndpointImpl.api().getProviderService()
	                .addProvider(_providerID,
	                        getProviderClass().getName(),
	                        _pollIntervalInMinutes*60000L, _providerConfig,
	                        _providerDataEditable);
	        
	        // persist to disk
	        ProviderServiceImpl.save();
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			// should not occur, validation is done beforehand
			throw new IllegalStateException("Provider configuration failed: " + e.getMessage(), e);
		}
        
		//show success message and refresh the page in order to update the provider status     
		getPage().getPopupWindowInstance().showInfoAndRefresh("Info","Configuration successfully " +(isEditMode() ? "edited" : "added"));		
	}
	
	
	
	@Override
	protected void onReset() {
		// hide the popup in which this configuration is always shown
		getPage().getPopupWindowInstance().hide();
	}

	@Override
	protected boolean keepFieldAsFormElement(Field f) {
		if (f.getType().equals(User.class))
			return false;	// User field is handled as extra field
		return super.keepFieldAsFormElement(f);
	}

	/**
	 * Returns the currently selected widget class
	 * 
	 * @return
	 */
	protected Class<? extends AbstractFlexProvider<?>> getProviderClass() {
		return selectedProviderClass;
	}
	
	/**
	 * Determines the edit mode:
	 * true: edit an existing configuration, current selection cannot be changed
	 * false: new form, any item can be selected
	 * 
	 * @return
	 */
	protected boolean isEditMode() {
		return editMode;
	}
	
	/**
	 * Initialize this {@link ProviderConfigurationForm} instance:
	 * 
	 *  - initializes a provider selection combobox
	 *  - initializes the form component
	 * 
	 *  
	 * @param widgetConfig a filled widgetConfig or null
	 */
	@SuppressWarnings("unchecked")
	protected void initialize(AbstractFlexProvider<?> provider) {
		
		Operator defaults = Operator.createNoop();
		if (provider!=null) {
			defaults = OperatorFactory.toOperator(provider.config);
			editMode = true;
			selectedProviderClass = (Class<? extends AbstractFlexProvider<?>>) provider.getClass();
		}
		
		providersCB = initializeProvidersComboBox();
		providersCB.setEnabledWithoutRefresh(!isEditMode());
		
		setConfigurationClassInternal(providerConfigurationClass(selectedProviderClass), defaults);
	}
	
	/**
	 * Adds a provider dropdown box and additional required fields
	 * 
	 * If the provider configuration contains a {@link User} field,
	 * these are specially initialized with form elements
	 * 
	 * Implementation note:
	 * 
	 * The additional form elements are only refreshed if they have not been set,
	 * in order to keep previously entered data (e.g. by changing the selected
	 * provider)
	 * 
	 */
	@Override
	protected void addAdditionalFormElements() {
		
		addFormElement("Provider", providersCB, true, Validation.NONE);

		providerID = providerID==null ? new FTextInput2("Identifier") : providerID;
		if (isEditMode()) {
			providerID.setEnabledWithoutRefresh(false);
			String configValue = EndpointImpl.api().getRequestMapper().getReconvertableUri(provider.providerID, false);
			providerID.setValueWithoutRefresh(configValue);
		}
		addFormElement("Identifier", providerID, true, new ConvertibleToUriValidator(), "Identifier of the provider");
        
		pollInterval = pollInterval==null ? new FTextInput2("PollInterval") : pollInterval;
		if (isEditMode()) {
			providerID.setEnabledWithoutRefresh(false);
			// convert milliseconds back to minutes for display
			pollInterval.setValueWithoutRefresh(Integer.valueOf(UnitConverter.convertInputTo(Double.valueOf(provider.pollInterval),	Unit.MILLISECONDS, Unit.MINUTES).intValue()).toString());
		}
		addFormElement("Poll interval", pollInterval, true, new NumberValidator(), "Poll intervall in minutes");

        providerDataEditable = providerDataEditable==null ? new FCheckBox("providerDataWritable") : providerDataEditable;
		if (isEditMode()) {
			providerDataEditable.setCheckedNoUpdate(provider.providerDataEditable);
		}
		addFormElement("Provider data editable:", providerDataEditable, false, new VoidValidator());
        
		// check for User field and conditionally add fields
		if (containsUserField()) {
			userName = new FTextInput2("UserName");
			addFormElement("Username", userName, false,	new VoidValidator(), "Username for the provider.");

			password = new FTextInput2("password");
			password.setType(ElementType.PASSWORD);
			addFormElement("Password", password, false,	new VoidValidator(), "Password for the provider.");
			
			if (isEditMode()) {
				User user = null;
				try {
					user = (User) provider.config.getClass().getField("user").get(provider.config);
				} catch (Exception e) {
					throw new RuntimeException("Could not read user from config: " + e.getMessage(), e);
				}
				// check required: user might be optional
				if (user!=null) {
					userName.setValueWithoutRefresh(user.username);

					// Load the real password from the save and store it temporarily to allow
					// keeping the PW when it has not been changed (do not take the/ masked PW!!)
					tempPwStored = IwbPwdSafe.retrieveProviderUserPassword(provider.providerID, user.username);
					password.value = user.password; // Masked ! equals("*********") == true !
				}
			}
		}
	}
	
	private FComboBox initializeProvidersComboBox() {
		
        FComboBox res = new FComboBox("widgetTypes") {
			@SuppressWarnings("unchecked")
			@Override
			public void onChange() {

				// set the selection and redraw the view
				Class<? extends AbstractFlexProvider<?>> _newSelection = (Class<? extends AbstractFlexProvider<?>>) getSelected().get(0);
				if (selectedProviderClass.equals(_newSelection))
					return;		// not changed
				selectedProviderClass = _newSelection;
				setConfigurationClass(providerConfigurationClass(selectedProviderClass), Operator.createNoop());
			}        	
        };            

        Set<Class<? extends AbstractFlexProvider<?>>> providers = providers();
        for(Class<? extends AbstractFlexProvider<?>> providerClass : providers)
		    res.addChoice( providerClass.getSimpleName(), providerClass);
        
        if (selectedProviderClass==null && providers.size()>0)
        	selectedProviderClass = providers.iterator().next();
        
        res.setPreSelected(selectedProviderClass);       
       
        return res;
	}

	private Class<?> providerConfigurationClass(Class<? extends AbstractFlexProvider<?>> providerClass) {
		
		try {
			return providerClass.newInstance().getConfigClass();
		} catch (Exception e) {
			logger.debug("Invalid widget use of " + providerClass.getName() + ":"  + e.getMessage(), e);
			throw new IllegalStateException("widget class not accessible: " + providerClass.getName() + ": "  + e.getMessage());
		} 
	}

	@SuppressWarnings("unchecked")
	private Set<Class<? extends AbstractFlexProvider<?>>> providers() {
		Set<Class<? extends AbstractFlexProvider<?>>> res = Sets.newLinkedHashSet();

		try {
			for (String providerClass : EndpointImpl.api().getProviderService().getProviderClasses()) 
				res.add((Class<? extends AbstractFlexProvider<?>>)Class.forName(providerClass));
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw Throwables.propagate(e);
		}
		return res;
	}
	
    /**
     * Checks if the provider's configuration class contains a
     * {@link User} field named 'user' which has to be dealt with properly
     * 
     * Note: it is required that this field is named 'user', otherwise
     * an {@link IllegalStateException} will be thrown.
     * 
     */
    private boolean containsUserField() {
        for(Field field: configClass.getDeclaredFields()) {
        	// TODO think about this restriction
        	if (field.getType().equals(User.class) && !field.getName().equals("user"))
        		throw new IllegalStateException("A user field in a configuration class must be called 'user': " + configClass.getName());
        	if (field.getType().equals(User.class))
        		return true;
        }
        return false;
    }
    
	private boolean providerExists(URI id) {
		try {
			@SuppressWarnings("rawtypes")
			List<AbstractFlexProvider> providers = EndpointImpl.api().getProviderService().getProviders();

			for (AbstractFlexProvider<Serializable> prov : providers) {
				if (prov.providerID.equals(id))
					return true;
			}
		} catch (RemoteException e) {
			throw Throwables.propagate(e);
		}
		return false;
	}
}
