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

package com.fluidops.iwb.widget.admin;

import java.util.ServiceLoader;

import org.openrdf.query.algebra.evaluation.function.Function;

import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FTable;
import com.fluidops.ajax.components.FTable.FilterPos;
import com.fluidops.ajax.models.FTableModel;
import com.fluidops.iwb.widget.AbstractWidget;
import com.fluidops.iwb.widget.config.WidgetBaseConfig;

/**
 * A widget for displaying all registered SPARQL functions in a table.
 * 
 * @author marlon.braun
 * 
 */
public class SPARQLFunctionsWidget extends AbstractWidget<SPARQLFunctionsWidget.Config>
{

	public static class Config extends WidgetBaseConfig
	{

	}

	@Override
	public String getTitle()
	{
		return "SPARQL Functions Table";
	}

	@Override
	public Class<?> getConfigClass()
	{
		return Config.class;
	}

	@Override
	protected FComponent getComponent(String id)
	{
		FTable table = new FTable(id);
		FTableModel tm = new FTableModel();

		tm.addColumn("Name");
		tm.addColumn("Function");
		tm.addColumn("Type");
		
		table.setModel(tm);
		table.setShowCSVExport(true);
		table.setNumberOfRows(20);
		table.setEnableFilter(true);
		table.setOverFlowContainer(true);
		table.setFilterPos(FilterPos.TOP);
		
		ServiceLoader<Function> codecSetLoader = ServiceLoader.load(Function.class);
		for (Function cp : codecSetLoader)
		{
			String[] data = new String[3];

			data[0] = cp.getClass().getSimpleName();
			data[1] = cp.getURI();
			data[2] = cp.getClass().getName().startsWith("org.openrdf") ? "built-in" : "custom";

			tm.addRow(data);
		}
		
		table.setModel(tm);

		return table;
	}

}
