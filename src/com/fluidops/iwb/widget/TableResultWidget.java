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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryInterruptedException;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;

import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FHTML;
import com.fluidops.ajax.components.FHtmlString;
import com.fluidops.ajax.components.FTable;
import com.fluidops.ajax.components.FTable.FilterPos;
import com.fluidops.ajax.models.FTableModel;
import com.fluidops.iwb.Global;
import com.fluidops.iwb.ajax.FValue.ValueConfig;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.ImageResolver;
import com.fluidops.iwb.api.ReadDataManagerImpl;
import com.fluidops.iwb.api.ReadDataManagerImpl.SparqlQueryType;
import com.fluidops.iwb.model.ParameterConfigDoc;
import com.fluidops.iwb.model.ParameterConfigDoc.Type;
import com.fluidops.iwb.model.TypeConfigDoc;
import com.fluidops.iwb.user.UserManager.ValueAccessLevel;
import com.fluidops.iwb.util.IWBFileUtil;
import com.fluidops.iwb.util.QueryResultUtil;
import com.fluidops.iwb.util.TableResultComparator;
import com.fluidops.iwb.widget.WidgetEmbeddingError.ErrorType;
import com.fluidops.iwb.widget.WidgetEmbeddingError.NotificationType;
import com.fluidops.iwb.widget.config.ColumnConfig;
import com.fluidops.iwb.widget.config.WidgetQueryConfig;
import com.fluidops.util.StringUtil;

/**
 * Show the results of a query in a table. 
 * 
 * @author (pha), as
 * @see QueryResultUtil for the utility class that generates the table models
 * 
 * 
 * 
 *  Example usage:
 * 
 * {{ #widget : TableResult
 * 
    | query = 'select ?z ?lng ?lat 
               where {
                 ?z basekb_ns:m.01sx ?x.
                 ?x basekb_ns:m.01q8 ?lng. 
                 ?x basekb_ns:m.01qh ?lat.
               } limit 30' 
    | columnConfiguration = {{
                        {{variableName='lat' | datatype='xsd:double'}} |
                        {{variableName='lng' | columnName='Ich bin ein Text.          ..?üöäß' | datatype='<http://www.w3.org/2001/XMLSchema#double>'}} |
                        {{variableName='z'   | columnName='Ich bin der Name eines Ortes.........!?' }} |
      }}
   }}

 * 
 */
@TypeConfigDoc( "The Table Result widget displays the results of a query in a table, allowing customised dashboards to be dynamically loaded on wikipages." )
public class TableResultWidget extends AbstractWidget<TableResultWidget.Config>
{    

	/**
	 * A limit on the number of cells which can be displayed (needed to avoid tables breaking due to over-long AJAX responses). 
	 * The number of rows to display is adjusted according to this constant.
	 */
	private static final int MAX_DISPLAYABLE_CELLS = 1000;
	
	private static final Logger logger = Logger.getLogger(TableResultWidget.class.getName());
	
	/**
	 * Query Widget needs the query string and the query language
	 * 
	 * @author pha
	 */
	public static class Config extends WidgetQueryConfig
	{

		@ParameterConfigDoc(
				desc = "optional title which will be displayed in the table header")  
				public String queryName;

		@ParameterConfigDoc(
				desc = "Specifies whether the query should be evaluated on the historic repository", 
				defaultValue="false")  
				public Boolean historic = false;

		@ParameterConfigDoc(
				desc = "Show labels or values", 
				defaultValue="true")  
				public Boolean labels = true;

		@ParameterConfigDoc(
				desc = "Property file for image resolver") 
				public String imageFile;

		@ParameterConfigDoc(
				desc = "Resolve images by RDF", 
				defaultValue="false") 
				public Boolean imageByProperty = false;

		@Deprecated
		@ParameterConfigDoc(
				desc = "Value resolvers. The parameter is deprecated. " +
						"Please define value resolvers in the columnConfiguration instead ")  
				public String valueResolver;

		@ParameterConfigDoc(
				desc = "Hide the table if it contains no entries", 
				defaultValue="false")  
				public Boolean hideTableIfEmpty = false;

		@ParameterConfigDoc(
				desc = "Shows a single line of text for single result queries", 
				defaultValue="false")  
				public Boolean singleResult = false;

		@ParameterConfigDoc(
				desc = "Number of displayed rows",  
				defaultValue="30") 
				public Integer numberOfDisplayedRows = 30;
		
    	@ParameterConfigDoc(
    			desc = "Specifies display values and sort orders for columns in the table output. " +
    					"If no display value for a column is specified, then the variable name is used. " +
    					"If no sort order for a column is supplied, then string sorting is applied.", 
    			type=Type.LIST)
        public List<ColumnConfig> columnConfiguration;
		
	}


	@Override
	public FComponent getComponent(final String id)
	{
		final Config config = get();	
		
		//don't allow defining value resolvers both in legacy parameter and in column configuration
		if(StringUtil.isNotNullNorEmpty(config.valueResolver) && hasValueResolvers(config.columnConfiguration))
			return WidgetEmbeddingError.getErrorLabel(id, ErrorType.GENERIC, 
					"The configuration parameter 'valueResolver' is deprecated: " +
					"Please define value resolvers in the columnConfiguration only."); 

		//repository to work with. may be the general one or the history
		Repository rep = config.historic ? Global.historyRepository : pc.repository;

		// query is required
		if (config.query==null)
			return WidgetEmbeddingError.getErrorLabel(id,ErrorType.NO_QUERY);

		String query = config.query.trim();

		// only select queries are supported, however PREFIX maybe in the beginning
		try {
			SparqlQueryType type = ReadDataManagerImpl.getSparqlQueryType(query, true);
			if (!type.equals(SparqlQueryType.SELECT))
				return WidgetEmbeddingError.getErrorLabel(id,ErrorType.NO_SELECT_QUERY);
		} catch (MalformedQueryException e) {
			// ignore: is treated appropriately below
		}

		try  {               

			// set up variable resolver
			Map<String,String> varResolvers = getValueResolvers(config.valueResolver, config.columnConfiguration);

			// set up image resolver, if required
			ImageResolver ir = null;
			String imageFile = null;
			if (config.imageFile!=null)
				imageFile = config.imageFile.isEmpty()?"" : IWBFileUtil.CONFIG_DIRECTORY + config.imageFile;

			if (imageFile!=null || config.imageByProperty)
				ir = new ImageResolver(imageFile, config.imageByProperty);
     

			// compute the table model, i.e. evaluate the query
			ValueConfig valueCfg = new ValueConfig(ir, varResolvers, config.labels); 
			FTable ftable = createTable(id, rep, query, valueCfg, config.infer);

			//display a single line result as text and not as a table
			if(config.singleResult) {
				Object o = null;
				try	{
					o = ftable.getModel().getValueAt(0, 0);
				} catch (Exception e) {
					//This happens if the table is empty
				}

				if(o == null) {
					return WidgetEmbeddingError.getNotificationLabel(id, NotificationType.NO_DATA, config.noDataMessage);
				}

				return new FHTML(id, StringEscapeUtils.unescapeHtml(o.toString()));
			}
			
			ftable.setNoDataMessage(WidgetEmbeddingError.getNoDataMessage(config.noDataMessage));

			if (config.hideTableIfEmpty && ftable.getViewRowCount()==0)
				return new FHtmlString(id);
			
			applyColumnConfiguration(ftable);
			
			//set the table's title
			String queryName = config.queryName;
			if(StringUtil.isNotNullNorEmpty(queryName))
				ftable.setTitle(queryName);

			ftable.setSortingLimit(10000);
			ftable.setShowCSVExport(true);
			ftable.setNumberOfRows(getMaxNumberOfDisplayableRows(ftable, config));
			ftable.setEnableFilter(true);
			ftable.setOverFlowContainer(true);
			ftable.setFilterPos(FilterPos.TOP);

	        if(StringUtil.isNotNullNorEmpty(config.width))
	        	ftable.addStyle("width", config.width +"px");
	        if(StringUtil.isNotNullNorEmpty(config.height))
	        {
	        	ftable.addStyle("height", config.height+"px");
	        	ftable.addStyle("overflow", "hidden");
	        }
	        
			return ftable;

		} catch (RepositoryException e)  {        	
			logger.error(e.getMessage(), e);
			return WidgetEmbeddingError.getErrorLabel(id, ErrorType.QUERY_EVALUATION, config.query);

		} catch (IllegalArgumentException e) { 

			// invalid specification of widget, message contains details
			return WidgetEmbeddingError.getErrorLabel(id, ErrorType.INVALID_WIDGET_CONFIGURATION, e.getMessage());
		} catch (MalformedQueryException e) {  

			return WidgetEmbeddingError.getErrorLabel(id, ErrorType.SYNTAX_ERROR, e.getMessage());        
		} catch (QueryInterruptedException e) {

			logger.info("QueryInterruptedException (QueryTimeout): " + e.getMessage());
			String sQuery = config.query;
			if(pc.value!=null) {
				ValueAccessLevel val = EndpointImpl.api().getUserManager().getValueAccessLevel(pc.value);
				if(!val.equals(ValueAccessLevel.WRITE))
					sQuery = "";
			}
			
			return WidgetEmbeddingError.getErrorLabel(id, ErrorType.QUERY_TIMEOUT, sQuery);
		} catch (QueryEvaluationException e) {
			
			logger.info("QueryEvaluationException: " + e.getMessage());
			return WidgetEmbeddingError.getErrorLabel(id, ErrorType.QUERY_EVALUATION, config.query);
		}              
	}



	/**
	 * check if value resolvers are defined in columnConfiguration
	 * @param columnConfiguration
	 * @return
	 */
	private boolean hasValueResolvers(List<ColumnConfig> columnConfiguration)
	{
		if(columnConfiguration != null)
			for(ColumnConfig cc : columnConfiguration)
			{
				if(cc.valueResolver != null)
					return true;
			}
		return false;
	}



	/**
	 * Create the table using the given parameters. Subclasses can override this
	 * 
	 * @param id
	 * @param rep
	 * @param query
	 * @param valueCfg
	 * @return
	 * @throws RepositoryException
	 * @throws MalformedQueryException
	 * @throws QueryEvaluationException
	 */
	protected FTable createTable(String id, Repository rep, String query,
			ValueConfig valueCfg, boolean infer) throws RepositoryException,
			MalformedQueryException, QueryEvaluationException
	{
		FTableModel tm = QueryResultUtil.sparqlSelectAsTableModel(rep, query,
				true, infer, pc.value, valueCfg);
		
		return new FTable(id, tm);
	}

	/**
	 * Applies the {@link Config#columnConfiguration} to the given table, including
	 * the column display names and sort order comparators.
	 * 
	 * @param ftable
	 */
	private void applyColumnConfiguration(FTable ftable) {
		
		Map<String,ColumnConfig> columnMapping = generateColumnMapping(get());
		FTableModel tm = (FTableModel) ftable.getTableModel();
				
		Vector<String> columnIdentifiers=new Vector<String>();
		int width=tm.getColumnCount();
		
		// set sort orders and column display values
		for(int i=0;i<width;i++) {
			String oldColName=tm.getColumnName(i);
			
			if (columnMapping.containsKey(oldColName)) {
				//set column name
				if (columnMapping.get(oldColName).displayName!=null) 
					columnIdentifiers.add(columnMapping.get(oldColName).displayName);
				else 
					columnIdentifiers.add(oldColName);

				//set column width
				if (columnMapping.get(oldColName).columnWidth!=null) 
					ftable.setColumnWidth(i, columnMapping.get(oldColName).columnWidth);
			
				//set sort order
				if(columnMapping.get(oldColName).datatype!=null) 
					ftable.setDatatypeComparator(i,TableResultComparator.getComparator(columnMapping.get(oldColName).datatype.getTypeURI()));
				else 
					ftable.setDatatypeComparator(i,TableResultComparator.getComparator(ValueFactoryImpl.getInstance().createURI("http://www.fluidops.com")));
			} else
				columnIdentifiers.add(oldColName);
		}

		tm.setColumnIdentifiers(columnIdentifiers);
	}
	
	@Override
	public Class<?> getConfigClass() {
		return TableResultWidget.Config.class;
	}


	@Override
	public String getTitle()  {
		String title = get().queryName;
		return title !=null ? title :  "Table Result";
	}


	/**
	 * Return the value resolvers as defined by the user input.
	 * The string valueResolver is a legacy parameter, but still supported.
	 * The variable resolver will be preferably taken from the columnConfiguration.
	 * @param valueResolver 
	 * @param columnConfiguration 
	 */    
	protected Map<String,String> getValueResolvers(String valueResolver, List<ColumnConfig> columnConfiguration )
	{
		
		Map<String,String> varResolvers = new HashMap<String,String>();
		
		if(StringUtil.isNotNullNorEmpty(valueResolver) && !hasValueResolvers(columnConfiguration))
		{
			String[] s = valueResolver.split(",");
			for (int i=0;i<s.length;i++)
			{
				String[] s2 = s[i].split("=");
				if (s2.length==2)
					varResolvers.put(s2[0],s2[1]);
			}
		}

		if(columnConfiguration != null)
		{
			for(ColumnConfig cc : columnConfiguration)
			{
				if(StringUtil.isNotNullNorEmpty(cc.variableName) && cc.valueResolver != null)
					varResolvers.put(cc.variableName,cc.valueResolver.toString());
			}
		}
		return varResolvers;
	}
	
	
	
	/**
	 * 
	 *  Generates a mapping from the variable names to the respective ColumnConfig object.
	 * 
	 */
	private HashMap<String,ColumnConfig> generateColumnMapping(Config c) {
		HashMap<String,ColumnConfig> result=new HashMap<String,ColumnConfig>();
		if(c.columnConfiguration==null) return result;		
		for (ColumnConfig cc : c.columnConfiguration)
			 result.put(cc.variableName, cc);
		 
		return result;
	}
	
	/**
	 *
	 * Browsers cannot process long AJAX query responses, which makes tables break sometimes, 
	 * if the number of displayed rows is too big.
	 * To avoid this, we apply a limit (MAX_DISPLAYABLE_CELLS) on the number of cells to be displayed.
	 * 
	 * @param ftable
	 * @param config
	 * @return
	 */
	private int getMaxNumberOfDisplayableRows(FTable ftable, Config config) {
		int maxNumberOfDisplayableRows = MAX_DISPLAYABLE_CELLS / ftable.getTableModel().getColumnCount();
		
		if(maxNumberOfDisplayableRows<config.numberOfDisplayedRows) {
			logger.warn("Can display maximum " + maxNumberOfDisplayableRows + " rows due to browser limitations");
			return maxNumberOfDisplayableRows;
		} 
		
		return config.numberOfDisplayedRows;
	}
	
}