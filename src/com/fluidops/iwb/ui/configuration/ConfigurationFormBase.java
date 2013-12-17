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

import static com.fluidops.ajax.XMLBuilder.at;
import static com.fluidops.ajax.XMLBuilder.atId;
import static com.fluidops.ajax.XMLBuilder.el;
import static com.fluidops.iwb.ui.configuration.ConfigurationFormElementFactory.getFormElementForConfig;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.fluidops.ajax.XMLBuilder.Element;
import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FContainer;
import com.fluidops.ajax.components.FForm;
import com.fluidops.ajax.components.FImageButton;
import com.fluidops.ajax.components.FTextInput2;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.operator.Operator;
import com.fluidops.iwb.api.operator.OperatorFactory;
import com.fluidops.iwb.api.operator.OperatorNode;
import com.fluidops.iwb.model.ParameterConfigDoc;
import com.fluidops.iwb.model.ParameterConfigDoc.Type;
import com.fluidops.iwb.util.User;
import com.fluidops.util.Rand;
import com.fluidops.util.StringUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Base implementation for all configuration forms (e.g. widget or provider configuration).
 * 
 * This class provides the functionality to render a form for a given configuration
 * class (e.g. a widget configuration class). It uses in particular the information
 * that can be retrieved from the {@link Field}s of the class and considers those
 * fields that are annotated with {@link ParameterConfigDoc}. The information
 * from the annotation is used to decide how the component is rendered, see
 * {@link ConfigurationFormElementFactory#getFormElementForConfig(FormElementConfig)}
 * for details.
 * 
 * @author as
 * @see ConfigurationFormElement
 * @see ConfigurationFormUtil
 */
public abstract class ConfigurationFormBase extends FForm implements OperatorConversion {

	
	protected Map<FormElementConfig, ConfigurationFormElement<? extends FComponent>> cfgElements = Maps.newHashMap();
	protected Class<?> configClass;
	protected Operator presetValues;
	
	public ConfigurationFormBase(String id) {
		super(id);
		this.setClazz("ConfigurationForm");
		setFormHandler(new ConfigurationFormHandler());
	}
	
	public ConfigurationFormBase(String id, Class<?> configClass, Operator presetValues) {
		this(id);
		setConfigurationClassInternal(configClass, presetValues);
	}
	
	public ConfigurationFormBase(String id, List<FormElementConfig> formElements, Operator presetValues) {
		this(id);
		this.presetValues = presetValues;		
		initializeFormComponents(formElements);
	}
	
	
	@Override
	public OperatorNode toOperatorNode() {
				
		if (configClass==null)
			throw new IllegalStateException("Illegal state: configuration class not specified.");
		
				
		Map<String, OperatorNode> map = Maps.newHashMap();
		for (Entry<FormElementConfig, ConfigurationFormElement<? extends FComponent>> e : cfgElements.entrySet()) {
			String fieldName = e.getKey().fieldName;
			OperatorNode opNode = e.getValue().toOperatorNode();
			if (opNode==null)
				continue;		// e.g. for empty fields
			map.put(fieldName, opNode);
		}
		
		if (map.isEmpty())
			return null;
		
		// special case: simple configuration without named parameters
		if (configClass.equals(String.class))
			return map.entrySet().iterator().next().getValue();
		
		return OperatorFactory.mapToOperatorNode(map);
	}
	
	/**
	 * Sets the configuration class and redraws the form for
	 * this class
	 * 
	 * @param configClass
	 */
	public void setConfigurationClass(Class<?> configClass, Operator presetValues) {
		setConfigurationClassInternal(configClass, presetValues);
		populateView();
	}
	
	/**
	 * Sets the configuration class and redraws the form for 
	 * this class. Removes all registered form components
	 * and reinitializes the form components with
	 * {@link #initializeFormComponents()}
	 * 
	 * @param configClass
	 * @param presetValues
	 */
	protected void setConfigurationClassInternal(Class<?> configClass, Operator presetValues) {
		this.configClass = configClass;
		this.presetValues = presetValues;
		// clear this form before adding all new data
		this.clearContent();
		this.removeAll();
		cfgElements.clear();
		initializeFormComponents(getFormElementConfigurations(configClass));
	}
	
	/**
	 * Submit the data of this configuration form. Use the {@link Operator}
	 * framework to either serialize this {@link OperatorNode} to a string
	 * representation or to evaluate it to a given target type.
	 * 
	 * @param data the data as an {@link OperatorNode}, null if an empty form was submitted
	 */
	protected abstract void submitData(OperatorNode data);
	
	/**
	 * Initializes this form based on the given {@link #configClass}. This method
	 * invoked {@link #addAdditionalFormElements()} which can be used in subclasses
	 * to add further form elements
	 * @param formElements 
	 */
	protected void initializeFormComponents(List<FormElementConfig> formElements) {
		
		addAdditionalFormElements();
		
		for (FormElementConfig fCfg : formElements) {
			
			ConfigurationFormElement<? extends FComponent> fEl = getFormElementForConfig(fCfg);
			addFormElement(fCfg, fEl);
			cfgElements.put(fCfg, fEl);
		}
			
	}	
	
	/**
	 * Actual implementations can override this methods to add additional form elements
	 */
	protected void addAdditionalFormElements() {
		
	}
	
	
	/**
	 * Special functionality for configuration form to add form elements
	 */
	private void addFormElement(FormElementConfig fCfg, ConfigurationFormElement<? extends FComponent> fEl) {
		addFormElement(fCfg.label, fEl.getComponent(fCfg), fCfg.required(), fEl.validator(fCfg), false, fCfg.help());		
	}

	@Override
	public void addFormElement(String label, final FComponent formElement, boolean isRequired, Validator v, boolean hide, String help) {
		
		/*
		 * override the parent functionality to render the help as a nice icon
		 */
		
		if (formElement instanceof FTextInput2)
			((FTextInput2) formElement).setValidator(v);
		
		FContainer cnt = new FContainer("fe"+Rand.getIncrementalFluidUUID()) {
			@Override
			public Object returnValues() {
				return formElement.returnValues();
			}			
		};
		cnt.appendClazz("formElement");
		cnt.add(formElement);
		if(!StringUtil.isNullOrEmpty(help))
		{
			FImageButton button = new FHelpButton("img"+Rand.getIncrementalFluidUUID(), 
					EndpointImpl.api().getRequestMapper().getContextPath()+"/images/navigation/i.gif", help);
			button.appendClazz("helpButton");
			cnt.add(button);
		}
		FormRow row = new FormRow(label, cnt, isRequired, v, false, "");
		this.add(cnt);
		this.formRows.add(row);
	}
	
	
	/**
	 * Retrieves all {@link FormElementConfig}s for the given configuration class. 
	 * 
	 * This is
	 * a) a configuration for each {@link Field} that is annotated with {@link ParameterConfigDoc}
	 * 
	 * The elements are sorted according to the rules as defined in {@link #getConfigFieldsSorted(Class)}.
	 * 
	 * @param configClass
	 * @return
	 */
	List<FormElementConfig> getFormElementConfigurations(Class<?> configClass) {
		
		if (configClass.equals(String.class))
			return Lists.newArrayList(
					new FormElementConfig("", FormElementConfig.toParameterConfigDoc("", Type.SIMPLE, true), String.class, presetValues));
				
		List<FormElementConfig> res = Lists.newArrayList();
		for (Field f : ConfigurationFormUtil.getConfigFieldsSorted(configClass)) {
			if (!keepFieldAsFormElement(f))
				continue;
			String fieldName = f.getName();
			Class<?> nestedConfigClass = f.getType();
			if (List.class.isAssignableFrom(nestedConfigClass)) {
				// use the list generic type
				nestedConfigClass = (Class<?>)((ParameterizedType)f.getGenericType()).getActualTypeArguments()[0];
			}
			Operator formElementDefaultValue = getChildOperator(presetValues, fieldName);
			FormElementConfig fCfg = new FormElementConfig(fieldName, f.getAnnotation(ParameterConfigDoc.class), 
					nestedConfigClass, formElementDefaultValue);
			res.add(fCfg);
		}
		return res;	
	}
	
	/**
	 * Returns true if this field should be used as form element. Subclasses
	 * may override this method to prevent some classes being used as form
	 * elements although they are annotated with {@link ParameterConfigDoc}.
	 * Consider {@link ProviderConfigurationForm} with the {@link User}
	 * class handling as an example.
	 * 
	 * @param f
	 * @return
	 */
	protected boolean keepFieldAsFormElement(Field f) {
		return true;
	}
	
	private Operator getChildOperator(Operator operator, String fieldName) {
		if (operator==null || !operator.isStructure())
			return Operator.createNoop();
		Operator child = operator.getStructureItem(fieldName);
		if (child==null)
			return Operator.createNoop();
		return child;
	}
	
		
	
	
	
	
	/**
	 * Form Handler for submitting the form
	 */
	private class ConfigurationFormHandler extends FormHandler
	{
		@Override
		public void onSubmit(FForm form, List<FormData> list) {
			submitData(toOperatorNode());
		}

		@Override
		public void onSubmitProcessData(List<FormData> list) {
			// the method is not used in the form. 
			// the whole processing is accomplished in onSubmit(form)
		}
	}
	
	public static class FHelpButton extends FImageButton{

		public FHelpButton(String id, String imageUrl, String tooltip) {
			super(id, imageUrl, tooltip);
		}
		
		@Override
		public String render()	{
			Element div = el("div", atId("div1"+Rand.getIncrementalFluidUUID()));
			Element a = el("a", at("style", "cursor: pointer;"), at("onClick", beforeClickJs + getOnClick() + afterClickJs));
			Element img = el("img", atId(getId() + "_img"), at("src", imageUrl), at("border", "0"));
			Element div2 = el("div", atId("div2"+Rand.getIncrementalFluidUUID()));div2.text(getTooltip());

			if (!StringUtil.isNullOrEmpty(getTooltip()))
				img.addAttribute(at("alt", getTooltip()));

			a.addChild(img);
			div.addChild(a);
			div.addChild(div2);
			return div.toString();
		}
	}
}
