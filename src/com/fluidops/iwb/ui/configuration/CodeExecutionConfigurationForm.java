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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.openrdf.model.Value;

import com.fluidops.ajax.components.FButton;
import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FForm;
import com.fluidops.ajax.components.FLabel;
import com.fluidops.ajax.components.FPopupWindow;
import com.fluidops.iwb.model.ParameterConfigDoc.Type;
import com.fluidops.iwb.api.operator.Operator;
import com.fluidops.iwb.api.operator.OperatorException;
import com.fluidops.iwb.api.operator.OperatorFactory;
import com.fluidops.iwb.api.operator.OperatorNode;
import com.fluidops.iwb.model.ParameterConfigDoc;
import com.fluidops.iwb.service.CodeExecution;
import com.fluidops.iwb.service.CodeExecution.Config;
import com.fluidops.iwb.service.CodeExecution.WidgetCodeConfig;
import com.fluidops.iwb.ui.configuration.ValueDropdownConfigurationFormElement.ValueDropdownFormElementConfig;
import com.fluidops.iwb.widget.CodeExecutionWidget;
import com.fluidops.util.StringUtil;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Configuration form for CodeExecutionWidget with user input.
 * 
 * @author christian.huetter
 */
public class CodeExecutionConfigurationForm extends ConfigurationFormBase
{
	private WidgetCodeConfig widgetConfig;
	private Config codeConfig;
	private FLabel msg;
	private FComponent comp;
	private CodeExecutionWidget codeExecutionWidget;
	
	private LinkedHashMap<String, FormElementConfig> formElements;
	
	/**
	 * This flag indicates a special case of the config form containing a single element of type CLASS.
	 */
	private boolean configClassOnly = false;

	/**
	 * Handler for submitting the form
	 */
	private class ConfigurationFormHandler extends FormHandler
	{
		@Override
		public void onSubmit(FForm form, List<FormData> list) {
			// show confirmation message
			if (!StringUtil.isNullOrEmpty(widgetConfig.confirm)) {
				showConfirmationDialog(widgetConfig.confirm);
			}
			else
				submitData(toOperatorNode());
		}

		@Override
		public void onSubmitProcessData(List<FormData> list) {
			// the method is not used in the form. 
			// the whole processing is accomplished in onSubmit(form)
		}
	}

	public CodeExecutionConfigurationForm(String id,
			WidgetCodeConfig widgetConfig, Config codeConfig,
			FLabel msg, FButton fButton,
			CodeExecutionWidget codeExecutionWidget)
	{
		super(id);
		this.widgetConfig = widgetConfig;
		this.codeConfig = codeConfig;
		this.msg = msg;
		this.comp = (fButton != null) ? fButton : this;
		this.codeExecutionWidget = codeExecutionWidget;
		
		this.formElements = CodeExecutionConfigurationForm.extractFormElements(widgetConfig);
		List<FormElementConfig> formElementsList = Lists.newArrayList(formElements.values());
		
		if (formElementsList.size() == 1 && formElementsList.get(0).parameterConfig.type() == Type.CONFIG)
		{
			this.configClassOnly = true;
			setConfigurationClassInternal(formElementsList.get(0).targetType, null);
		}
		else
		{
			this.configClass = null;
			this.presetValues = null;
			initializeFormComponents(formElementsList);
		}
		
		setFormHandler(new ConfigurationFormHandler());
	}

	private void showConfirmationDialog(String confirm) {
		final FPopupWindow popup = comp.getPage().getPopupWindowInstance(confirm);
		popup.setTitle("Confirmation");
		popup.setTop("60px");
		popup.setWidth("600px");
		popup.setClazz("ConfirmationDialog");
		popup.setDraggable(true);
		
		popup.addButton("Ok", new Runnable() {
			@Override
			public void run() {
				popup.removeAll();
				popup.hide();
				submitData(toOperatorNode());
			}
		});
		popup.addCloseButton("Cancel");
		
		popup.populateAndShow();
	}

	@Override
	protected void submitData(OperatorNode data)
	{
		Operator op;
		if (data == null)
			op = Operator.createNoop();
		else
			op = OperatorFactory.toOperator(data);
		
		// create a copy of the code config to allow consecutive code executions
		Config codeConfigCopy = Config.copy(codeConfig);
		
		// replace argument placeholders with user input
		for (int i = 0; i < codeConfigCopy.args.length; i++)
		{
			String argName = CodeExecutionWidget.extractPlaceholderName(codeConfigCopy.args[i]);
			if (argName != null)
			{
				FormElementConfig elem = formElements.get(argName);
				if (elem == null)
					throw new IllegalArgumentException("No form element found for argument '" + argName + "'.");
				
				if (this.configClassOnly)
					codeConfigCopy.args[i] = handleConfigClassOperator(op, elem);
				else 
					codeConfigCopy.args[i] = handleOperator(op, elem);
				
				if (elem.parameterConfig.type() == Type.LIST)
					codeConfigCopy.signature[i] = List.class;
				else
					codeConfigCopy.signature[i] = elem.targetType;
			}
		}
		
		getPage().getPopupWindowInstance().hide();
		codeExecutionWidget.execute(widgetConfig, codeConfigCopy, msg, comp);				
	}
	
	/**
	 * Default case: Extract and evaluate the given element from the operator structure
	 */
	private Object handleOperator(Operator op, FormElementConfig elem)
	{
		// operator is nested
		if (op.isStructure())
		{
			Operator item = op.getStructureItem(elem.fieldName);
			return evaluateOperator(item, elem);
		}
		// operator is a noop
		else
			return null;
	}

	/**
	 * Special case: Evaluate operator directly because there is only one element of type CONFIG
	 */
	private Object handleConfigClassOperator(Operator op, FormElementConfig elem)
	{
		// simple case without nesting
		return evaluateOperator(op, elem);
	}

	/**
	 * Evaluate the operator to the target type specified by the form element.
	 */
	private Object evaluateOperator(Operator op, FormElementConfig elem)
	{
		// no user input for current argument
		if (op == null || op.isNoop())
		{
			if (elem.required())
				throw new IllegalArgumentException("Argument '" + elem.fieldName + "' is required.");
			
			return null;
		}
		
		try {
			return op.evaluate(elem.targetType);
		} catch (OperatorException e) {
			throw Throwables.propagate(e);
		}
	}
	
	@Override
	public OperatorNode toOperatorNode()
	{
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
		
		return OperatorFactory.mapToOperatorNode(map);
	}

	/**
	 * Extract form elements from widget configuration.
	 * 
	 * @param widgetConfig
	 * @return form elements
	 */
	public static LinkedHashMap<String, FormElementConfig> extractFormElements(CodeExecution.WidgetCodeConfig widgetConfig)
	{
		LinkedHashMap<String, FormElementConfig> formElements = new LinkedHashMap<String, FormElementConfig>();
		for (CodeExecution.UserInputConfig userInput : widgetConfig.userInput)
		{
			ParameterConfigDoc parameterConfig = FormElementConfig.toParameterConfigDoc("", userInput.componentType, userInput.required);
			
			// map component type to target type
			Class<?> targetType = null;
			switch (userInput.componentType)
			{
				case SIMPLE:
				case TEXTAREA:
					targetType = String.class;
					break;
				case LIST:
					targetType = String.class; //results in List<String>
					break;
				case CONFIG:
					if (StringUtil.isNullOrEmpty(userInput.componentClass)) {
						throw new IllegalArgumentException("componentClass must not be null or empty for input components of type CONFIG.");
					}
					try {
						targetType = Class.forName(userInput.componentClass);
					} catch (ClassNotFoundException e) {
						throw new IllegalArgumentException("Cannot find component class " + userInput.componentClass);
					}
					break;
				case DROPDOWN:
					if (userInput.selectValues == null)
						throw new IllegalArgumentException("selectValues must be specified for input components of type DROPDOWN.");
					targetType = Value.class;
					break;
				case CHECKBOX:
					targetType = Boolean.class;
					break;
				case PASSWORD:
					targetType = String.class;
					break;
				default:
					throw new UnsupportedOperationException("Component type '" + userInput.componentType.toString() + "' not supported.");
			}
			
			String label = StringUtil.isNullOrEmpty(userInput.displayName) ? userInput.name : userInput.displayName;
			
			Operator presetValue = Operator.createNoop();
			if (StringUtil.isNotNullNorEmpty(userInput.presetValue))
				presetValue = OperatorFactory.toOperator(OperatorFactory.textInputToOperatorNode(userInput.presetValue, targetType));
			
			FormElementConfig formElement = null;
			if (userInput.componentType == Type.DROPDOWN)
				formElement = new ValueDropdownFormElementConfig(userInput.name, label, parameterConfig, targetType, presetValue, userInput.selectValues);
			else
				formElement = new FormElementConfig(userInput.name, label, parameterConfig, targetType, presetValue);
			formElements.put(userInput.name, formElement);
		}
		return formElements;
	}
}