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

package com.fluidops.iwb.widget;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;

import com.fluidops.ajax.components.FSelectableTable;
import com.fluidops.ajax.components.FTable;
import com.fluidops.ajax.models.FSelectableTableModel;
import com.fluidops.ajax.models.FTableModel;
import com.fluidops.iwb.ajax.FValue.ValueConfig;
import com.fluidops.iwb.annotation.CallableFromWidget;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.model.ParameterConfigDoc;
import com.fluidops.iwb.model.ParameterConfigDoc.Type;
import com.fluidops.iwb.model.TypeConfigDoc;
import com.fluidops.iwb.service.CodeExecution.CodeExecutionContext;
import com.fluidops.iwb.service.CodeExecution.WidgetCodeConfig;
import com.fluidops.iwb.util.QueryResultUtil;
import com.fluidops.iwb.util.QueryResultUtil.ColumnActionComponentBuilder;


/**
 * ActionableResultWidget is a {@link TableResultWidget} with code execution support, 
 * uses {@link CodeExecutionWidget} functionality for executing code. The widget can 
 * be used to invoke actions using row-bindings as well as column values for multiple 
 * selected rows from the table as additional input. 
 * 
 * Bindings from the table can be accessed via the "?:varName" notation, where
 * varName must be a valid projection in the query result.
 * 
 * For each action basically any of the functionalities available in the
 * {@link CodeExecutionWidget} can be used. Please refer to the documentation
 * of that widget for further details.
 * 
 * If the user does not have privileges to use the {@link CodeExecutionWidget},
 * the usual {@link TableResultWidget} is rendered.
 * 
 * Example:
 * 
 * <source>
 	{{#widget: com.fluidops.iwb.widget.ActionableResultWidget
	| query = 'SELECT ?name ?city WHERE { ?? :name ?name . ?? :city ?city }'
	| rowActions = {{ 
	      {{ label='Hello' | clazz='com.fluidops.iwb.widget.ActionableResultWidget' | method='helloWorld' | args= {{ '?:name' | 'someConst' }} | render='btn' | passContext = true}} |
	      {{ label='Do Something' | clazz='com.fluidops.iwb.widget.ActionableResultWidget' | method='helloWorld' | args= {{ '?:city' | 'someConst' }} | render='btn' | passContext = true}}
	  }}
	}}
 * </source> 
 *  
 * @author as
 *
 */
@TypeConfigDoc("ActionableResult presents a tuple query result in a table and allows to invoke user-defined actions on these results.")
public class ActionableResultWidget extends TableResultWidget
{
	public static class Config extends TableResultWidget.Config
	{
		@ParameterConfigDoc(
			desc = "A set of user-defined actions invoked using row bindings from the table result as input. The input for a particular row can be referenced as ?:varName in the arguments for the action and is of type Value.", 
			type=Type.LIST)  
		public List<WidgetCodeConfig> rowActions;
		
		@ParameterConfigDoc(
			desc = "A set of user-defined actions invoked using column bindings for selected rows from the table result as input. The input for a particular column can be referenced as ?:varName in the arguments for the action and is of type List<Value> containing the values of selected rows.", 
			type=Type.LIST)  
		public List<WidgetCodeConfig> selectedRowActions;
	}	

	@Override
	protected FTable createTable(String id, Repository rep, String query,
			ValueConfig valueCfg, boolean infer) throws RepositoryException,
			MalformedQueryException, QueryEvaluationException
	{
		// if the user is not allowed to see the CodeExecutionWidget, we render the usual table
		if (!EndpointImpl.api().getUserManager().hasWidgetAccess(CodeExecutionWidget.class, null))
			return super.createTable(id, rep, query, valueCfg, infer);
		
		final Config c = (Config) get();

		if (c.rowActions!=null && c.selectedRowActions!=null)
			throw new IllegalArgumentException("Either row actions or columns actions can be specified.");
		
		FTable table;		
		if (c.rowActions!=null) {
			table = createTableRowActions(id, rep, query, valueCfg, infer, c);

			/*
			 * Make the actions column (the last of the table) not sortable
			 */
			table.setSortableColumn(table.getModel().getColumnCount() - 1, false);
		} else if (c.selectedRowActions!=null)
			table = createTableSelectedRowActions(id, rep, query, valueCfg, infer, c);
		else
			throw new IllegalArgumentException("Row actions or column actions must be specified.");

		return table;
	}

	/**
	 * Creates a {@link FTable} for the given configuration, adding
	 * a control element for each row action into the last column
	 * of the table.
	 */
	private FTable createTableRowActions(String id, Repository rep, String query,
			ValueConfig valueCfg, boolean infer, Config c) throws RepositoryException,
			MalformedQueryException, QueryEvaluationException {
		
		for (WidgetCodeConfig rowAction : c.rowActions)
			checkRowAction(rowAction);

		FTable table = new FTable(id);
		CodeExecutionContext ceCtx = new CodeExecutionContext(pc, table);
		FTableModel tm = QueryResultUtil
				.sparqlSelectAsTableModelWithSingleRowAction(rep, query, true,
						infer, pc.value, valueCfg, c.rowActions, ceCtx);
		table.setModel(tm);
		
		return table;
	}
	
	/**
	 * Creates a {@link FSelectableTable} for the given configuration, adding
	 * a control element below the table for each selected row action.
	 */
	private FTable createTableSelectedRowActions(String id, Repository rep, String query,
			ValueConfig valueCfg, boolean infer, Config c) throws RepositoryException,
			MalformedQueryException, QueryEvaluationException {
		    		
		for (WidgetCodeConfig action : c.selectedRowActions)
			checkRowAction(action);
		
		FSelectableTable<BindingSet> table = new FSelectableTable<BindingSet>(id);
		CodeExecutionContext ceCtx = new CodeExecutionContext(pc, table);
		FSelectableTableModel<BindingSet> tm = QueryResultUtil
				.sparqlSelectAsTableModelForColumnActions(rep, query, true,
						infer, pc.value, valueCfg);
		table.setModel(tm);
		
		// create action components
		if(tm.getRowCount()>0) {
			for (WidgetCodeConfig action : c.selectedRowActions) {
				table.addControlComponent(ColumnActionComponentBuilder.buildActionComponent(action, table, ceCtx), "floatLeft");
			}
		}
		
		return table;
	}

	@Override
	public String getTitle()
	{
		return "Table Result with code execution support";
	}
	
	@Override
	public Class<?> getConfigClass() {
		return Config.class;
	}	


	/**
	 * Check if the rowAction are specified correctly
	 * 
	 * @param rowAction
	 */
	private void checkRowAction(WidgetCodeConfig rowAction) throws IllegalArgumentException {

		// initialize default values
		rowAction.render = rowAction.render==null ? "btn" : rowAction.render;
		rowAction.args = rowAction.args==null ? Collections.<Object>emptyList() : rowAction.args;

		if (rowAction.method==null)
			throw new IllegalArgumentException("rowAction.method must not be null");

		if (rowAction.label==null)
			throw new IllegalArgumentException("rowAction.label must not be null");

		if (rowAction.clazz==null )
			throw new IllegalArgumentException("parameters rowAction.clazz and rowAction.method are required.");
	}


	/**
	 * Demo method for testing which alerts the name that was clicked on.
	 * 
	 * @param ceCtx
	 * @param name
	 */
	@CallableFromWidget
	public static void helloWorld(CodeExecutionContext ceCtx, Value name, String constant) {
		ceCtx.parentComponent.doCallback("alert('Hello, " + StringEscapeUtils.escapeHtml(constant) + ". Clicked on " + StringEscapeUtils.escapeHtml(name.stringValue()) + "');");
	}
	
	/**
	 * Demo method for testing which alerts the name that was clicked on.
	 * 
	 * @param ceCtx
	 * @param name
	 */
	@CallableFromWidget
	public static void testColumnActions(CodeExecutionContext ceCtx, List<Value> selectedValues) {
		StringBuilder sb = new StringBuilder();
		for (Value v : selectedValues)
			sb.append(v.stringValue()).append("; ");
		ceCtx.parentComponent.doCallback("alert('Selected the following rows: " + StringEscapeUtils.escapeHtml(sb.toString()) + "');");
	}
}
