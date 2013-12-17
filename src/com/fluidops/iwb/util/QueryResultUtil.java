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

package com.fluidops.iwb.util;

import java.util.ArrayList;
import java.util.List;

import org.openrdf.model.Statement;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;

import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FContainer;
import com.fluidops.ajax.components.FHtmlString;
import com.fluidops.ajax.components.FSelectableTable;
import com.fluidops.ajax.models.FSelectableTableModel;
import com.fluidops.ajax.models.FSelectableTableModelImpl;
import com.fluidops.ajax.models.FTableModel;
import com.fluidops.iwb.ajax.FValue;
import com.fluidops.iwb.ajax.FValue.ValueConfig;
import com.fluidops.iwb.api.ReadDataManager;
import com.fluidops.iwb.api.ReadDataManagerImpl;
import com.fluidops.iwb.api.ReadWriteDataManagerImpl;
import com.fluidops.iwb.service.CodeExecution;
import com.fluidops.iwb.service.CodeExecution.CodeExecutionContext;
import com.fluidops.iwb.service.CodeExecution.Config;
import com.fluidops.iwb.service.CodeExecution.WidgetCodeConfig;
import com.fluidops.iwb.widget.AbstractWidget;
import com.fluidops.iwb.widget.ActionableResultWidget;
import com.fluidops.iwb.widget.CodeExecutionWidget;
import com.fluidops.util.Rand;
import com.google.common.collect.Lists;


/**
 * Utility class to retrieve queries as table model 
 * 
 * @author as
 *
 */
public class QueryResultUtil
{
	
	/**
	 * Returns the given {@link TupleQueryResult} as a list of {@link BindingSet}. The
	 * iteration is closed as part of this method

	 * @param qRes
	 * @return
	 * @throws QueryEvaluationException 
	 */
	public static List<BindingSet> tupleQueryResultAsList(TupleQueryResult qRes) throws QueryEvaluationException {
		return tupleQueryResultAsList(qRes, Integer.MAX_VALUE);
	}
	
	/**
	 * Returns the given {@link TupleQueryResult} as a list of {@link BindingSet} up to
	 * the provided limit. The iteration is closed as part of this method.
	 * 
	 * @param qRes
	 * @param limit a positive integer representing the limit
	 * @return
	 * @throws QueryEvaluationException
	 */
	public static List<BindingSet> tupleQueryResultAsList(TupleQueryResult qRes, int limit) throws QueryEvaluationException {
		List<BindingSet> res = Lists.newArrayList();
		try {
			int count=0;
			while (count++<limit && qRes.hasNext()) {
				res.add(qRes.next());
			}
		} finally {
			qRes.close();
		}
		return res;
	}
	
	/**
	 * Returns the given {@link GraphQueryResult} as a list of statements. The
	 * iteration is closed as part of this method.
	 * @param res
	 * @return
	 * @throws QueryEvaluationException 
	 */
	public static List<Statement> graphQueryResultAsList(GraphQueryResult graph) throws QueryEvaluationException {
		return graphQueryResultAsList(graph, Integer.MAX_VALUE);
	}
	
	/**
	 * Returns the given {@link GraphQueryResult} as a list of statements up to
	 * the provided limit. The iteration is closed as part of this method.
	 * 
	 * @param graph
	 * @param limit a positive integer representing the limit
	 * @return
	 * @throws QueryEvaluationException
	 */
	public static List<Statement> graphQueryResultAsList(GraphQueryResult graph, int limit) throws QueryEvaluationException {
		List<Statement> res = new ArrayList<Statement>();
		try {
			int count=0;
			while (count++<limit && graph.hasNext()) {
				res.add(graph.next());
			}
		} finally {
			graph.close();
		}
		return res;
	}
	
	
	/**
     * Retrieve the result of a SPARQL select query as a table model.
     * 
     * @param rep
     * 			the repository to use
     * @param query
     * 			a valid SPARQL SELECT query
     * @param resolveNamespaces
     * 			boolean flag, to specifiy if abbreviated namespaces should be resolved
     * @param resolveValue
     * 					the value to use as replacement for ??
     * @param valueConfig
     * 			a {@link ValueConfig} to define how values shall be treated
	 *
     * @return
     * 			a populated table model
     * 
     * @throws RepositoryException
     * @throws MalformedQueryException
     * @throws QueryEvaluationException
     */
    public static FTableModel sparqlSelectAsTableModel(Repository rep, String query,
            boolean resolveNamespaces, boolean infer, Value resolveValue, 
            ValueConfig valueCfg)
            throws RepositoryException, MalformedQueryException,
            QueryEvaluationException
    {
    	ReadDataManager dm = ReadDataManagerImpl.getDataManager(rep);
    	
        TupleQueryResult result = null;
        try {
	        result = dm.sparqlSelect(query, resolveNamespaces,
	                resolveValue, infer);
	        FTableModel tm = new FTableModel();
	
	        for (String name : result.getBindingNames())
	            tm.addColumn(name);
	        int rowCounter = 0;
	        
	        // add the row content to the model
	        while (result.hasNext())
	        {
	            List<FComponent> row = 
	            	buildRow(result.next(), result.getBindingNames(), rowCounter, dm, valueCfg);
	            
	            tm.addRow(row.toArray());
	            rowCounter++;
	        }
	        return tm;
        } finally {
        	ReadDataManagerImpl.closeQuietly(result);
        }
        
    }
    
    
    
    /**
     * Return a table model for the query with the specified singleRowAction. A single row action is 
     * added as an additional column to each row. See class documentation for details.
     * 
     * @param rep
     * 				the repository to evaluate the query on
     * @param query
     * 				the SPARQL SELECT query
     * @param resolveNamespaces
     * 				flag to determine if namespaces should be resolved
     * @param resolveValue
     * 				the value to resolve for, i.e. the value that is inserted into the query for ??, can be null
     * @param valueCfg
     * 				maintain value information, such as the ImageResolver
     * @param singleRowAction
     * 				optional list of {@link RowAction}s which are applied (can be null)
     * @param ceCtx
     *  
     * @return
     * @throws RepositoryException
     * @throws MalformedQueryException
     * @throws QueryEvaluationException
     */
    public static FTableModel sparqlSelectAsTableModelWithSingleRowAction(Repository rep, String query,
            boolean resolveNamespaces, boolean infer, Value resolveValue, 
            ValueConfig valueCfg,
            List<WidgetCodeConfig> rowActions,
            CodeExecutionContext ceCtx)
            throws RepositoryException, MalformedQueryException,
            QueryEvaluationException
    {
    	
    	if (rowActions==null || rowActions.isEmpty())
    		return sparqlSelectAsTableModel(rep, query, resolveNamespaces, infer, resolveValue, valueCfg);
    	
    	ReadDataManager dm = ReadDataManagerImpl.getDataManager(rep);
    	
        TupleQueryResult result = null;
        try {
	        result = dm.sparqlSelect(query, resolveNamespaces, resolveValue, infer);
	        FTableModel tm = new FTableModel();
	
	        for (String name : result.getBindingNames())
	            tm.addColumn(name);
	        tm.addColumn("");
	        int rowCounter = 0;	    
	        
	        // add the row content to the model
	        while (result.hasNext())
	        {
	        	BindingSet b=result.next();
	            List<FComponent> row = buildRow(b, result.getBindingNames(), rowCounter, dm, valueCfg);
	              	
	            // add row actions (>1 => container, component otherwise)
	            if (rowActions.size()>1) {
	            	FContainer btnCnt = new FContainer("btnCnt"+Rand.getIncrementalFluidUUID());
	            	// we have to render in inverse order to be able to use float:right
	            	// and to have in addition the intended order
	            	for (int i=rowActions.size()-1; i>=0; i--) {
	            		FComponent c = RowActionComponentBuilder.buildActionComponent(rowActions.get(i), b, ceCtx);
	            		c.addStyle("float", "right");
	            		btnCnt.add(c);
	            	}
	            	btnCnt.addStyle("padding-right", "10px");
	            	row.add(btnCnt);
	            } else {
	            	FComponent c = RowActionComponentBuilder.buildActionComponent(rowActions.get(0), b, ceCtx);
	        		c.addStyle("float", "right");
	        		c.addStyle("margin-right", "10px");
	            	row.add(c);
	            }
	            
	            tm.addRow(row.toArray());
	            rowCounter++;
	        } 

	        return tm;
        } finally {
        	ReadWriteDataManagerImpl.closeQuietly(result);
        } 
    }
    
    
    /**
     * Return a {@link FSelectableTableModel} for the query to be used together with
     * column row actions. 
     * 
     * @param rep
     * 				the repository to evaluate the query on
     * @param query
     * 				the SPARQL SELECT query
     * @param resolveNamespaces
     * 				flag to determine if namespaces should be resolved
     * @param resolveValue
     * 				the value to resolve for, i.e. the value that is inserted into the query for ??, can be null
     * @param valueCfg
     * 				maintain value information, such as the ImageResolver
     * @param ceCtx
     *  
     * @return
     * @throws RepositoryException
     * @throws MalformedQueryException
     * @throws QueryEvaluationException
     */
    public static FSelectableTableModel<BindingSet> sparqlSelectAsTableModelForColumnActions(Repository rep, String query,
            boolean resolveNamespaces, boolean infer, Value resolveValue, 
            ValueConfig valueCfg)
            throws RepositoryException, MalformedQueryException,
            QueryEvaluationException
    {
		ReadDataManager dm = ReadDataManagerImpl.getDataManager(rep);

		TupleQueryResult result = null;
		try {
			result = dm.sparqlSelect(query, resolveNamespaces, resolveValue, infer);
			
			FSelectableTableModel<BindingSet> tm = new FSelectableTableModelImpl<BindingSet>(result.getBindingNames(), true);

			int rowCounter = 0;

			// add the row content to the model
			while (result.hasNext()) {
				BindingSet b = result.next();
				List<FComponent> row = buildRow(b, result.getBindingNames(),
						rowCounter, dm, valueCfg);
				
				tm.addRow(row.toArray(), b);
				rowCounter++;
			}
			return tm;
		} finally {
			ReadWriteDataManagerImpl.closeQuietly(result);
		}    
    }
   
    /**
     * Build a particular row for the retrieved results, convenience method
     * 
     * @param bindingSet
     * @param bindingNames
     * @param rowCounter
     * @param dm
     * @param cfg
     * 			a {@link ValueConfig}, must not be null
     * @return
     */
    protected static List<FComponent> buildRow(BindingSet bindingSet, List<String> bindingNames, int rowCounter, ReadDataManager dm, ValueConfig cfg) 
    {
    	List<FComponent> row = new ArrayList<FComponent>();

        int colCounter = 0;
        for (String name : bindingNames)
        {
        	String cmpId = "html" + rowCounter++ + "_"  + colCounter++;
            Value value = bindingSet.getValue(name);
            if (value != null) 
            {
                row.add(new FValue(cmpId, value, name, dm, cfg));
            }
            else
                row.add(new FHtmlString(cmpId, "&nbsp;", ""));
        }
        
        return row;
    }
    
    
    
    /**
     * A special convenience util to create an {@link FComponent} that can be used to trigger
     * actions in an {@link ActionableResultWidget} with row configuration. The idea here is
     * to reuse the {@link CodeExecutionWidget} functionality for rendering the component. 
     * In particular, we directly manipulate the configuration to take the correct arguments
     * at rendering time. 
     * 
     * @author as
     */
	public static class RowActionComponentBuilder {

		public static FComponent buildActionComponent(WidgetCodeConfig rowAction, BindingSet b, final CodeExecutionContext ceCtx) {
			return new RowActionComponentBuilder(b).buildActionComponent(rowAction, ceCtx);
		}
    	
    	private final BindingSet b;
    	
		private RowActionComponentBuilder(BindingSet b) {
			this.b = b;
		}
		
		public FComponent buildActionComponent(WidgetCodeConfig action, final CodeExecutionContext ceCtx) {
    		CodeExecutionWidget cw = new CodeExecutionWidget() ;
    		CodeExecution.WidgetCodeConfig cfg = WidgetCodeConfig.copy(action);
    		cfg.onFinish = action.onFinish==null ? "none" : action.onFinish;
    		cfg.args = new ArrayList<Object>();		         
            
            // pass the code execution context with the table as parent component
            if (action.passContext!=null && action.passContext) {
            	cfg.args.add(ceCtx); 
            	cfg.passContext = false;
            }
            
            for (Object arg : action.args) {
            	
            	// special handling for code execution in table:
            	// it is possible to reference bindings via the ?:varName notation
            	if (arg instanceof String) {
            		String param = (String)arg;
            		if (param.startsWith("?:")) {
            			String bindingName = param.substring(2);
            			if (!b.hasBinding(bindingName))
                			throw new IllegalArgumentException("Variable name " + bindingName + " cannot be used, as it is not part of the query result.");
            			cfg.args.add( b.getBinding(bindingName).getValue() );
            			continue;
            		}
            	}        	
            	cfg.args.add(arg);
            }
            
            cw.setConfig(cfg);
            FComponent cwWrapper = cw.getComponentUAE("ce"+Rand.getIncrementalFluidUUID());
            cwWrapper.appendClazz("rowAction");
    		return cwWrapper; 
		}    	
    }
    
    /**
     * This convenience util creates an {@link FComponent} that can be used as triggering
     * actions in an {@link ActionableResultWidget} with column actions. The idea here is
     * to reuse the {@link CodeExecutionWidget} and return the rendered component of
     * the {@link CodeExecutionWidget#getComponentUAE(String)} method. By doing so we
     * are able to inherit all functionality from the {@link CodeExecutionWidget}.
     * 
     * The implementation of this convenience method is a bit tricky, because we have
     * to manipulate the arguments after the onClick event has been triggered, i.e. after
     * the execute is invoked. The reason is that we do not know the selected items 
     * beforehand at construction time (as we do in the case of row actions).
     * 
     * A second trick is that we have to work on a copy of the code configuration, as
     * the instance is kept final within the widget, i.e. it keeps state when re-executing
     * the method.
     * 
     * @author as
     *
     */
    public static class ColumnActionComponentBuilder {

    	/**
    	 * Specialisation of {@link CodeExecutionWidget} for:
    	 * <ol>
    	 * <li>Use the code execution configurations provided in the table configs;</li>
    	 * <li>Avoid unrequired and harmful ACL checks (see Bug 11152).</li>
    	 * </ol>
    	 * 
		 * @author michele.mancioppi
		 */
		private final class SelectedActionWidget extends
				CodeExecutionWidget {

			/**
			 * Make {@link AbstractWidget#getComponent(String)} public
			 * so that we can invoke it directly to circumvent unnecessary
			 * ACL checks in {@link AbstractWidget#getComponentUAE(String)}.
			 * 
			 * @see com.fluidops.iwb.widget.CodeExecutionWidget#getComponent(java.lang.String)
			 */
			@Override
			public FComponent getComponent(String id) {
				return super.getComponent(id);
			}

			@Override
			protected Object executeScript(final Config codeConfig)
					throws Exception {
				
				// we require to work on a copy as the codeConfig belongs
				// to the state of the widget. The selected rows have to be
				// retrieved on demand.
				Config copyCodeConfig = Config.copy(codeConfig);
				List<BindingSet> selectedRows = table.getSelectedObjects();
				
				for (int i=0; i<copyCodeConfig.args.length; i++) {
					Object arg = copyCodeConfig.args[i];
					
					// special handling for code execution in table:
			    	// it is possible to reference bindings via the ?:varName notation
					// we replace the actual arguments with a List<Value> (which may be empty)
			    	if (arg instanceof String) {
			    		String param = (String)arg;
			    		if (param.startsWith("?:")) {
			    			String bindingName = param.substring(2);
			    			copyCodeConfig.args[i] = getArgument(selectedRows, bindingName);
			    			copyCodeConfig.signature[i] = List.class;
			    		}
			    	}
				}
				return super.executeScript(copyCodeConfig);
			}
		}

		public static FComponent buildActionComponent(WidgetCodeConfig columnAction, FSelectableTable<BindingSet> table, final CodeExecutionContext ceCtx) {
    		return new ColumnActionComponentBuilder(table).buildActionComponent(columnAction, ceCtx);
    	}
    	
    	private final FSelectableTable<BindingSet> table;

		private ColumnActionComponentBuilder(FSelectableTable<BindingSet> table) {
			this.table = table;
		}

		public FComponent buildActionComponent(WidgetCodeConfig action, final CodeExecutionContext ceCtx) {
			SelectedActionWidget cw = new SelectedActionWidget();
    		CodeExecution.WidgetCodeConfig cfg = WidgetCodeConfig.copy(action);
    		cfg.onFinish = action.onFinish==null ? "none" : action.onFinish;
    		cfg.args = Lists.newArrayList();        
            
            // pass the code execution context with the table as parent component
            if (action.passContext!=null && action.passContext) {
            	cfg.args.add(ceCtx); 
            	cfg.passContext = false;
            }
                  
            cfg.args.addAll(action.args);
            cw.setConfig(cfg);
            FComponent cwWrapper = cw.getComponent("ce"+Rand.getIncrementalFluidUUID());
            cwWrapper.appendClazz("selectedRowAction");
    		return cwWrapper; 
		}

		protected Object getArgument(List<BindingSet> selectedRows, String bindingName) {
			List<Value> res = Lists.newArrayList();
			for (BindingSet b : selectedRows) {
				if (!b.hasBinding(bindingName))
	    			throw new IllegalArgumentException("Variable name " + bindingName + " cannot be used, as it is not part of the query result.");
				res.add(b.getBinding(bindingName).getValue());
			}
			return res;
		}   	
    }
}