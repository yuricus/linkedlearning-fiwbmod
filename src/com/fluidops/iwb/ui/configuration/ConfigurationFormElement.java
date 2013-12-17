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

import com.fluidops.ajax.components.FForm.Validator;


/**
 * Interface for any configuration form element (e.g. text input, lists,
 * nested configuration, dropdowns, etc).
 * 
 * Actual implementations should inherit from {@link AbstractConfigurationFormElement}.
 * 
 * The {@link ConfigurationFormElementFactory} can be used to get the appropriate
 * {@link ConfigurationFormElementFactory} for a particular config class.
 * 
 * @author as
 * 
 * @see AbstractConfigurationFormElement
 * @see TextInputConfigurationFormElement
 * @see SubformInputConfigurationFormElement
 * @see ListInputConfigurationFormElement
 * 
 */
public interface ConfigurationFormElement<T> extends OperatorConversion {
	
	/**
	 * Retrieves the component for the given {@link FormElementConfig}
	 * 
	 * @param formElementConfig
	 * @return
	 */
	public T getComponent(FormElementConfig formElementConfig);
	
	/**
	 * Returns the {@link Validator} for this form element
	 * 
	 * @param formElementConfig
	 * @return
	 */
	public Validator validator(FormElementConfig formElementConfig);
}
