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
import com.fluidops.ajax.components.FForm.Validator;
import com.fluidops.iwb.api.operator.OperatorNode;
import com.fluidops.util.Rand;

/**
 * A {@link ConfigurationFormElement} to be used for {@link SubForm}.
 * 
 * @author as
 *
 */
public class SubformInputConfigurationFormElement extends AbstractConfigurationFormElement<SubForm> {

	@Override
	public OperatorNode toOperatorNode() {
		return component.toOperatorNode();
	}

	@Override
	protected SubForm createComponent(FormElementConfig formElementConfig) {
		return new SubForm("s"+Rand.getIncrementalFluidUUID(), formElementConfig);		
	}

	@Override
	public Validator validator(FormElementConfig formElementConfig) {
		return new SubFormValidator();
	}	
	
	
	/**
	 * {@link Validator} for a {@link SubForm}. A subform is
	 * valid if one of the following conditions holds:
	 * 
	 * a) if the subform is not required, and the subform is empty
	 * b) if the form is not empty and all form elements are valid
	 * 
	 * @author as
	 *
	 */
	protected class SubFormValidator implements Validator {

		@Override
		public boolean validate(FComponent c) {
			OperatorNode op = toOperatorNode();
			if (required && op==null)
				return false;
			if (op==null)
				return true;
			return component.canBeSubmitted();
		}		
	}
}