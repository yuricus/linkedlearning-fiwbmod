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
import com.fluidops.iwb.api.operator.OperatorFactory;
import com.fluidops.iwb.api.operator.OperatorNode;
import com.fluidops.util.Rand;


/**
 * An implementation of {@link ConfigurationFormElement} which allows to deal with
 * lists, i.e. it allows to add additional sub configuration instances.
 * 
 * @author as
 * @see AddFormElementContainer
 */
public class ListInputConfigurationFormElement extends AbstractConfigurationFormElement<AddFormElementContainer> {

	@Override
	public OperatorNode toOperatorNode() {
		return component.toOperatorNode();
	}

	@Override
	protected AddFormElementContainer createComponent(
			FormElementConfig formElementConfig) {
		AddFormElementContainer cnt = new AddFormElementContainer("a"+Rand.getIncrementalFluidUUID(), formElementConfig);
		return cnt;
	}

	@Override
	public Validator validator(FormElementConfig formElementConfig) {
		return new ListFormValidator();
	}
	
	/**
	 * {@link Validator} for a nested list sub form. A sub form is
	 * valid if one of the following conditions holds:
	 * 
	 * a) if the subform is not required, and the subform is empty
	 * b) if a dynamic SELECT operator has been provided
	 * c) if the form is not empty and all form elements are valid
	 * 
	 * @author as
	 *
	 */
	protected class ListFormValidator implements Validator {

		@Override
		public boolean validate(FComponent c) {
			OperatorNode op = toOperatorNode();
			if (required && op==null)
				return false;
			if (op==null)
				return true;
			if (OperatorFactory.toOperator(op).isDynamic())
				return true;
			return component.validateFormElements();
		}		
	}
	
}