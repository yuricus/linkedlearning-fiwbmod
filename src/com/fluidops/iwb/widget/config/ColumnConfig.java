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

package com.fluidops.iwb.widget.config;

import com.fluidops.iwb.api.valueresolver.ValueResolverSelectValuesFactory;
import com.fluidops.iwb.model.ParameterConfigDoc;
import com.fluidops.iwb.model.ParameterConfigDoc.Type;
import com.fluidops.iwb.widget.TableResultWidget;

/**
 * AdditionalLy to the configuration defined in {@link VariableConfig} 
 * provides a possibility to define width of a table column in percent.
 * 
 * @author ango
 * @see TableResultWidget
 * @see VariableConfig
 *
 */
public class ColumnConfig extends VariableConfig
{
	@ParameterConfigDoc(
			desc = "The width of the column in percent (e.g '30').")
	public Double columnWidth;
	
	@ParameterConfigDoc(
			desc = "The value resolver to display values in a specific way.",
			type = Type.DROPDOWN,
			selectValuesFactory = ValueResolverSelectValuesFactory.class)
	public String valueResolver;
}
