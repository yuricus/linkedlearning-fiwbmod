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
 * A special {@link ConfigurationFormBase} which can be used as
 * subform to render the form of a configuration class.
 * 
 * @author as
 *
 */
public class SubForm extends ConfigurationFormBase {

	/**
	 * @param id
	 * @param defaultConfigClass
	 */
	public SubForm(String id, FormElementConfig fcCfg) {
		super(id, fcCfg.targetType, fcCfg.presetValue); 	
		this.hideSubmit = true;
		this.hideShowRequiredFieldsMessage = true;
		this.appendClazz("subcontainer");
	}

	@Override
	protected void submitData(OperatorNode data) {
		throw new UnsupportedOperationException("SubForm does not support submit operation.");
	}
}
