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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.fluidops.ajax.components.FComboBox;
import com.fluidops.iwb.api.operator.OperatorFactory;
import com.fluidops.iwb.api.operator.OperatorNode;
import com.fluidops.util.Rand;
import com.fluidops.util.StringUtil;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;

/**
 * A {@link ConfigurationFormElement} for drop downs
 * 
 * @author as
 *
 */
public class DropdownConfigurationFormElement extends AbstractConfigurationFormElement<FComboBox> {
	
	protected Class<?> targetType;
	
	@Override
	public OperatorNode toOperatorNode() {
		if (component.getSelected() == null 
				|| component.getSelected().size() == 0 
				|| StringUtil.isNullOrEmpty(component.getSelected().get(0).toString()))
			return null;
		return OperatorFactory.textInputToOperatorNode(component.getSelected().get(0).toString(), targetType);
	}

	@Override
	protected FComboBox createComponent(FormElementConfig formElementConfig) {

		FComboBox cb = new FComboBox(Rand.getIncrementalFluidUUID());
		cb.setEnableSorting(false);
		targetType = formElementConfig.targetType;

		List<String> choices = Lists.newArrayList();
		choices.add("");
		
		if (targetType.equals(Boolean.class)) 
		{
			choices.add("false");
			choices.add("true");
		}
		else if (targetType.isEnum())
		{
			choices.addAll(getSelectableChoices(formElementConfig.parameterConfig.selectValues()));
		}

		else if (formElementConfig.parameterConfig.selectValues().length>0)
		{
			choices.addAll(Arrays.asList(formElementConfig.parameterConfig
					.selectValues()));
		}
		
		else if (formElementConfig.parameterConfig.selectValuesFactory()!=VoidSelectValuesFactory.class) {
			// support ParameterConfigDoc#selectValuesFactory
			try {
				choices.addAll( formElementConfig.parameterConfig.selectValuesFactory().newInstance().getSelectValues() );
			} catch (Exception e) {
				throw Throwables.propagate(e);
			} 
		}

		cb.addChoices(choices);
		
		if(formElementConfig.hasPresetValues())
			cb.setPreSelected(OperatorFactory.operatorToText(formElementConfig.presetValue, targetType));

		return cb;
	}

	/**
	 * Creates a list of selectable enum constants. 
	 * Select values are implicitly defined if the field is an enum:
	 * we compute the intersection between doc.selectValues()
	 * and the constants defined in the enum (i.e., the selectValues
	 * field can be used to further constrain the enum); 
	 * if selectValues is null or empty we provide the full enum as selection
	 * @param selectValues
	 * @return
	 */
	private List<String> getSelectableChoices(String[] selectValues)
	{
		Object[] enumConstants = targetType.getEnumConstants();
		List<String> selectableEnumConstants = Lists.newArrayList();
		if (enumConstants != null)
		{
			for (int i = 0; i < enumConstants.length; i++)
			{
				String constantStr = enumConstants[i].toString();

				// we include the enum constant if
				// (i) select values is undefined/null, or
				// (ii) select values is non-empty and contains the constant
				Boolean include = false;
				if (selectValues != null && selectValues.length > 0)
				{
					for (int j = 0; j < selectValues.length && !include; j++)
					{
						include |= constantStr.equals(selectValues[j]);
					}
				}
				else
				{
					include = true;
				}

				if (include)
					selectableEnumConstants.add(enumConstants[i].toString());
			}
		}

		Collections.sort(selectableEnumConstants);
		return selectableEnumConstants;
	}

}
