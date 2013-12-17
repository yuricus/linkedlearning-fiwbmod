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

import com.fluidops.iwb.model.ParameterConfigDoc;

/**
 * A Chart config for the special type - Serial Chart,
 * including Bar, Line, Radar and Stock charts.
 * These charts can display multiple datasets and 
 * the data for these datasets can be built dynamically
 * using the 'groupByDatasetVariable' parameter
 * @author ango
 *
 */
public class WidgetSerialChartConfig extends WidgetChartConfig
{

    @ParameterConfigDoc(
            desc = "Apply grouping according to reserved ?datasets variable inside " +
            		"the query. This parameter can be used in combination with a single " +
            		"output variable; the chart data will be grouped according to the " +
            		"*distinct values* of the ?dataset variable (which must be present " +
            		"and non-null in all bindings), where each distinct variable value" +
            		"will lead to a new dimension of the output variable. ", 
            required=false,
            defaultValue="false") 
    public Boolean groupByDatasetVariable = false;
    
}
