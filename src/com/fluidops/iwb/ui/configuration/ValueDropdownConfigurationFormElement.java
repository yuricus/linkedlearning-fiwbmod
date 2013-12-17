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

import org.openrdf.model.Value;

import com.fluidops.iwb.ajax.FValueDropdown;
import com.fluidops.iwb.api.operator.Operator;
import com.fluidops.iwb.api.operator.OperatorException;
import com.fluidops.iwb.api.operator.OperatorFactory;
import com.fluidops.iwb.api.operator.OperatorNode;
import com.fluidops.iwb.autocompletion.AutoCompleteFactory;
import com.fluidops.iwb.autocompletion.AutoSuggester;
import com.fluidops.iwb.model.ParameterConfigDoc;
import com.fluidops.util.Rand;

import com.google.common.base.Throwables;

/**
 * A {@link ConfigurationFormElement} for FValueDropdown
 * 
 * @author christian.huetter
 * 
 */
public class ValueDropdownConfigurationFormElement extends AbstractConfigurationFormElement<FValueDropdown> {

	public static class ValueDropdownFormElementConfig extends FormElementConfig {
		private List<Value> values;

		public ValueDropdownFormElementConfig(String fieldName, String label,
				ParameterConfigDoc parameterConfig, Class<?> targetType,
				Operator presetValue, List<Value> values)
		{
			super(fieldName, label, parameterConfig, targetType, presetValue);
			this.values = values;
		}

		public List<Value> getValues() {
			return values;
		}

		public void setValues(List<Value> values) {
			this.values = values;
		}
	}

	@Override
	public OperatorNode toOperatorNode() {
		if (component.getRdfValue() == null)
			return null;
		return OperatorFactory.valueToOperatorNode(component.getRdfValue());
	}

	@Override
	protected FValueDropdown createComponent(FormElementConfig formElementConfig) {
		if (!(formElementConfig instanceof ValueDropdownFormElementConfig))
			throw new IllegalArgumentException("Expect configuration of type ValueDropdownFormElementConfig.");
			
		ValueDropdownFormElementConfig config = (ValueDropdownFormElementConfig) formElementConfig;
		
		// extract preset String
		String preset = null;
		if (formElementConfig.presetValue != null) {
			try {
				preset = formElementConfig.presetValue.evaluate(String.class);
			} catch (OperatorException e) {
				Throwables.propagate(e);
			}
		}
		
		// find the corresponding Value
		Value init = null;
		if (preset != null) {
			for (Value val : config.getValues()) {
				if (val != null && preset.equals(val.stringValue())) {
					init = val;
					break;
				}
			}
		}
		
		FValueDropdown dd = new FValueDropdown(Rand.getIncrementalFluidUUID(), init, false);

		if (config.getValues() != null) {
			AutoSuggester suggester = AutoCompleteFactory.createFixedListAutoSuggester(config.getValues());
			dd.setSuggester(suggester);
			dd.setEnableSorting(false);
		} else
			throw new RuntimeException("For a DROPDOWN component, the 'values' parameter has to be specified.");

		return dd;
	}
}
