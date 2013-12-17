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

import com.fluidops.iwb.api.operator.OperatorNode;

/**
 * A {@link FormElementContainer} which can be used for simple extensible
 * subforms (e.g. nested configurations). The subform is extended if
 * the element is required or if there exist default values.
 * 
 * @author as
 *
 */
public class SimpleFormElementContainer extends FormElementContainer {

	public SimpleFormElementContainer(String id, FormElementConfig fcCfg) {
		super(id, fcCfg, false);
	}	

	@Override
	public OperatorNode toOperatorNode() {
		
		// check first if the user has specified a dynamic operator
		// through the operator form
		if (isDynamicOperatorSpecified()) {
			return getDynamicOperatorIfAvailable();
		}
		
		if (formElements.isEmpty())
			return null;		
		return formElements.iterator().next().toOperatorNode();
	}
}
