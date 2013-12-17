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

import java.util.List;


import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FContainer;
import com.fluidops.ajax.components.FDropdownContainer;
import com.fluidops.ajax.components.FGridLayouter;
import com.fluidops.ajax.components.FImageButton;
import com.fluidops.ajax.components.FLabel;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.operator.Operator;
import com.fluidops.iwb.api.operator.OperatorNode;
import com.fluidops.iwb.api.operator.OperatorUtil;
import com.fluidops.util.Rand;
import com.google.common.collect.Lists;

/**
 * Base class for {@link SimpleFormElementContainer} and {@link AddFormElementContainer}
 * 
 * @author as
 *
 */
public abstract class FormElementContainer extends FDropdownContainer implements OperatorConversion  {

	protected final List<ConfigurationFormElement<? extends FComponent>> formElements = Lists.newArrayList();
	protected final FormElementConfig fcCfg;
	
	private boolean showAddRemoveButton = true;
	
	/**
	 * @param id
	 * @param labelString
	 * @param dropdownCmp
	 * @param fcCfg
	 */
	public FormElementContainer(String id, FormElementConfig fcCfg, boolean showAddRemoveButton) {
		super(id, "", new FContainer("i"+Rand.getIncrementalFluidUUID()), false, "hide", "show");
		this.fcCfg = fcCfg;
		this.showAddRemoveButton = showAddRemoveButton;
		operatorEditor.initializeAvailableOptions(fcCfg.targetType);
		initializeInnerContainer();
		if (fcCfg.required() || fcCfg.hasPresetValues())
			extend();
	}

	
	protected FContainer getInnerContainer() {
		return (FContainer)dropdownCmp;
	}

	protected void initializeInnerContainer() {

		// add forms from defaults
		if (fcCfg.hasPresetValues())
			addFormElementsFromDefaults();
		else if (fcCfg.required())
			addFormElement();
		
		addOperatorEditorButton();
		
		if (showAddRemoveButton && !isDynamicOperatorSpecified()) {
			addAddButton();
		}
	}
	
	/**
	 * Returns true if the user has entered a dynamic operator string,
	 * false otherwise
	 * @return
	 */
	protected boolean isDynamicOperatorSpecified() {
		return operatorEditor.isDynamicOperatorSpecified();
	}
	
	/**
	 * Returns a dynamic {@link OperatorNode} if an operator string
	 * has been entered, null otherwise.
	 * Use {@link #isDynamicOperatorSpecified()} for checking.
	 * @return
	 */
	protected OperatorNode getDynamicOperatorIfAvailable() {		
		return operatorEditor.getOperatorNode();
	}
	
	private final AddButton addButton = new AddButton();
	
	/**
	 * Removes and add the {@link AddButton}, in order to
	 * always keep it as the last element in the form
	 */
	private void addAddButton() {
		operatorEditor.appendClazz("floatLeft");
		getInnerContainer().remove(addButton);
		getInnerContainer().add(addButton);
	}
	
	private final OperatorEditor operatorEditor = new OperatorEditor(this, "op");
	
	private void addOperatorEditorButton() {
		// remove the floatLeft class, is added again if required
		operatorEditor.removeClazz("floatLeft");
		getInnerContainer().remove(operatorEditor);
		getInnerContainer().add(operatorEditor);
	}

	/**
	 * Add a new form element dependent on the {@link FormElementConfig#targetType}
	 * to the inner container (without any defaults being set)
	 */
	public void addFormElement() {		
		addFormElement(fcCfg.copyWithoutDefaults());		
	}
	
	/**
	 * Validates all registered {@link #formElements} using the respective
	 * {@link ConfigurationFormElement#validator()} of the element. If any
	 * of the nested elements cannot be validated, this method returns
	 * false. Otherwise true is returned.
	 * 
	 * @return
	 */
	public boolean validateFormElements() {
		for (ConfigurationFormElement<? extends FComponent> el : formElements) {
			if (!el.validator(fcCfg).validate(el.getComponent(fcCfg)))
				return false;
		}
		return true;
	}
	
	/**
	 * Add a new form element dependent on the {@link FormElementConfig#targetType}.
	 * 
	 * @param fcCfg
	 */
	private void addFormElement(FormElementConfig fcCfg) {
		ConfigurationFormElement<? extends FComponent> el = ConfigurationFormElementFactory.getFormElementForType(fcCfg.targetType);
		FComponent cmp = el.getComponent(fcCfg);
		if (showAddRemoveButton) {
			FContainer layoutCnt = new FGridLayouter(Rand.getIncrementalFluidUUID());
			layoutCnt.add(cmp);
			layoutCnt.add(new RemoveButton(el, layoutCnt));
			cmp = layoutCnt;
		}		
		
		getInnerContainer().add(cmp);
		formElements.add(el);
		
		addOperatorEditorButton();
		
		if (showAddRemoveButton && !isDynamicOperatorSpecified())
			addAddButton();
	}
	
	/**
	 * Add those form elements that are specified in the defaults set
	 * to this {@link FormElementContainer}.
	 */
	private void addFormElementsFromDefaults() {
		
		Operator defaults = fcCfg.presetValue;
		if (defaults.isList()) {			
			for (Operator item : defaults.getListItems())
				addFormElement( fcCfg.copyWithNewDefault(item) );
		}
		
		else if (defaults.isDynamic()) {
			// pre-populate with operator
			operatorEditor.setPresetOperatorString(defaults.serialize());
			reinitializeForDynamicOperator();
		}
		
		else if (OperatorUtil.isEmptyStructOperator(defaults)) {
			// do nothing: empty does not have an item (e.g. empty list)
		}

		else {
			addFormElement( fcCfg );
		}
	}
	
	/**
	 * Reinitialize the inner container, i.e. removes all elements and
	 * {@link #initializeInnerContainer()} again. Does not repopulate
	 * the view
	 */
	void reinitialize() {
		
		getInnerContainer().removeAll();
		addFormElement();
		
		addOperatorEditorButton();
		
		if (showAddRemoveButton && !isDynamicOperatorSpecified())
			addAddButton();
	}
	
	/**
	 * Reinitialize the inner container if a dynamic operator was set, 
	 * i.e. removes all elements, makes a note that operator is defined
	 * and adds the operator button. Does not repopulate the view
	 */
	void reinitializeForDynamicOperator() {
		
		getInnerContainer().removeAll();
		getInnerContainer().add(new FLabel(Rand.getIncrementalFluidUUID(), "Initialized from dynamic operator"));
		addOperatorEditorButton();
	}

	@Override
	protected void extend() {
		
		// only add a form element, if there is no such and if
		// the preset value does not correspond to an empty list.
		// Neither must a dynamic operator being specified
		if (formElements.size()==0 && !OperatorUtil.isEmptyStructOperator(fcCfg.presetValue) 
				&& !isDynamicOperatorSpecified()) {
			addFormElement();
		}
		
		super.extend();
	}
	
	
	/**
	 * The add button to add further form elements.	 * 
	 * @author as
	 */
	class AddButton extends FImageButton {

		public AddButton() {
			super("a"+Rand.getIncrementalFluidUUID(), EndpointImpl.api().getRequestMapper().getContextPath()+"/ajax/icons/add.png", 
					"Click to add fields to enter the parameter value.");
		}

		@Override
		public void onClick() {
			addFormElement();
			FormElementContainer.this.populateView();
		}		
	}

	/**
	 * The add button to add further form elements.	 * 
	 * @author as
	 */
	class RemoveButton extends FImageButton {

		private final ConfigurationFormElement<? extends FComponent> elToRemove;
		private final FComponent cmpToRemove;
		
		public RemoveButton(ConfigurationFormElement<? extends FComponent> elToRemove, FComponent cmpToRemove) {
			super("a"+Rand.getIncrementalFluidUUID(), EndpointImpl.api().getRequestMapper().getContextPath()+"/ajax/icons/delete.png", 
					"Click to remove specific field");
			this.elToRemove = elToRemove;
			this.cmpToRemove = cmpToRemove;
			appendClazz("addButton");
		}

		@Override
		public void onClick() {
			getInnerContainer().remove(cmpToRemove);
			formElements.remove(elToRemove);
			FormElementContainer.this.populateView();
		}		
	}

}
