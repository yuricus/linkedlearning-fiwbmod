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

import java.util.Collections;
import java.util.List;

import com.fluidops.iwb.api.operator.OperatorFactory;
import com.fluidops.iwb.api.operator.OperatorNode;
import com.google.common.collect.Lists;

/**
 * A simple container which allows to add further form elements with the click of
 * a button. This container can be used for list scenarios, where the goal is
 * to configure a list property with items.
 * 
 * @author as
 *
 */
public class AddFormElementContainer extends FormElementContainer {


	
	public AddFormElementContainer(String id, FormElementConfig fcCfg) {
		super(id, fcCfg, true);
	}


	@Override
	public OperatorNode toOperatorNode() {
		
		// check first if the user has specified a dynamic operator
		// through the operator form
		if (isDynamicOperatorSpecified()) {
			return getDynamicOperatorIfAvailable();
		}

		List<OperatorNode> items = Lists.newArrayList();
		for (OperatorConversion o : formElements) {
			OperatorNode opNode = o.toOperatorNode();
			if (opNode==null)
				continue;
			items.add(opNode);
		}
		
		// return an empty list operator if the form is extended 
		// (and there are no form elements)
		if (formElements.size()==0 && isExtended)
			return OperatorFactory.listToOperatorNode(Collections.<OperatorNode>emptyList(), fcCfg.targetType);
		
		if (items.size()==0)
			return null;
		
		return OperatorFactory.listToOperatorNode(items, fcCfg.targetType);
	}

}
