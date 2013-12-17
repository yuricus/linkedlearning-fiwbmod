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

package com.fluidops.iwb.api.datacatalog.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.Binding;
import org.openrdf.query.BindingSet;
import org.openrdf.query.TupleQueryResult;

import com.fluidops.iwb.Global;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.ReadDataManager;
import com.fluidops.iwb.api.ReadDataManagerImpl;
import com.fluidops.iwb.api.datacatalog.DataCatalogService;
import com.fluidops.iwb.api.datacatalog.DataEndpoint;
import com.fluidops.iwb.model.Vocabulary.RSO;
import com.fluidops.iwb.provider.ProviderUtils;

/**
 * Implementation of data catalog. The data catalog contains information
 * about so-called data endpoints, where an endpoint may, for instance, be
 * a relational database, described by connection information and extracted
 * schema information.
 * 
 * @author msc
 */
public class DataCatalogServiceImpl implements DataCatalogService
{    
	private static final Logger logger = 
    		Logger.getLogger(DataCatalogServiceImpl.class.getName());
	
	@Override
	public boolean dataEndpointExists(URI dataEndpointId)
	{
		String query = "ASK { " + dataEndpointExtractionQuery(dataEndpointId) + "}";
		
		ReadDataManager dm = ReadDataManagerImpl.getDataManager(Global.repository);
		
		try
		{
			//System.out.println(query);
			return dm.sparqlAsk(query, true, null, false);
		}
		catch (Exception e)
		{
			logger.warn(e.getMessage());
			throw new RuntimeException(e);
		}
	}

	
	@Override
	public DataEndpoint loadDataEndpoint(URI dataEndpointId)
	throws IllegalArgumentException
	{
		if (!dataEndpointExists(dataEndpointId))
			throw new IllegalArgumentException("Data endpoint with id '" + dataEndpointId + "' does not exist.");
	
		// note: for now, we only deal with RelationalDatabaseEndpoints, so
		// all we need is call this constructor (we may refine this code when
		// adding new endpoints, e.g. based on a pre-lookup of the endpoint
		// types for the respective id)
		try
		{
			return new RelationalDatabaseEndpointImpl(dataEndpointId);
		}
		catch (InvalidSchemaSpecificationException e)
		{
			logger.warn(e.getMessage());
			throw new RuntimeException(e);
		}
	}

	
	@Override
	public List<DataEndpoint> loadDataEndpoints()
	{
		List<DataEndpoint> registered = new ArrayList<DataEndpoint>();
		
		String query = "SELECT DISTINCT ?databaseEndpointId WHERE { ";
		query += dataEndpointExtractionQuery(null);
		query += " }";
		
		try
		{
			ReadDataManager dm = EndpointImpl.api().getDataManager();
			TupleQueryResult res =dm.sparqlSelect(query);
			while (res.hasNext())
			{
				BindingSet bs = res.next();
				Value dataEndpointId = bs.getBinding("databaseEndpointId").getValue();
				
				if (dataEndpointId instanceof URI)
					registered.add(loadDataEndpoint((URI)dataEndpointId));
			}

			return registered;
		}
		catch (Exception e)
		{
			logger.warn(e.getMessage());
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Returns a query stub binding the ?graph variable to the graph (=named
	 * context) in which the data endpoint with the given dataEndpointId is
	 * registered. If no data endpoint is registered for the given dataEndpointId,
	 * the query returns the empty result set.
	 * 
	 * If the passed dataEndpointId is null, all triples ?graph/?databaseEndpointId
	 * are retrieved, where ?databaseEndpointId is bound to the (internally used)
	 * ids for the data endpoint.
	 * 
	 * @param dataEndpointId the id of the catalog entry
	 * @return the query as described above
	 */
	protected static String dataEndpointExtractionQuery(URI dataEndpointId)
	{
		String idStr = dataEndpointId==null?"?databaseEndpointId":"<" + dataEndpointId.stringValue() + ">";
		
		String query = "";
		query += "GRAPH ?graph { " + idStr + " " 
				+ ProviderUtils.uriToQueryString(RDF.TYPE) + " " 
				+ ProviderUtils.uriToQueryString(RSO.TYPE_RELATIONAL_DATA_ENDPOINT) + "  } ";
		return query;
	}
	
	/**
	 * Converts a binding to a string value, returning the empty string if
	 * the binding or its value is null
	 * 
	 * @param b
	 * @return
	 */
	protected static String bindingToString(Binding b)
	{
		if (b==null || b.getValue()==null || b.getValue().stringValue()==null)
			return "";
		
		return b.getValue().stringValue();
	}


}