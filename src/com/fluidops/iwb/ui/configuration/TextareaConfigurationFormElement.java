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

import com.fluidops.ajax.components.FTextArea;
import com.fluidops.iwb.api.operator.OperatorFactory;
import com.fluidops.iwb.api.operator.OperatorNode;
import com.fluidops.util.Rand;
import com.fluidops.util.StringUtil;

/**
 * A {@link ConfigurationFormElement} for text areas
 * 
 * @author as
 *
 */
public class TextareaConfigurationFormElement extends AbstractConfigurationFormElement<FTextArea> {

	protected Class<?> targetType;
	
	@Override
	public OperatorNode toOperatorNode() {
		if (StringUtil.isNullOrEmpty(component.getValue()))
			return null;
		return OperatorFactory.textInputToOperatorNode(component.getValue(), targetType);
	}

	@Override
	protected FTextArea createComponent(FormElementConfig formElementConfig) {
		targetType=formElementConfig.targetType;
		FTextArea ta = new FTextArea(Rand.getIncrementalFluidUUID());
		ta.setValue(OperatorFactory.operatorToText(
				formElementConfig.presetValue, formElementConfig.targetType));
		return ta;
	}

}
