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

import java.util.List;

import org.apache.log4j.Logger;
import org.openrdf.model.Graph;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.impl.GraphImpl;
import org.openrdf.model.util.GraphUtil;
import org.openrdf.model.util.GraphUtilException;
import org.openrdf.model.vocabulary.RDF;

import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.datacatalog.DataEndpoint;
import com.fluidops.iwb.api.datacatalog.DatabaseConnectionInfo;
import com.fluidops.iwb.api.datacatalog.DatabaseInfo;
import com.fluidops.iwb.api.datacatalog.RelationalDatabaseEndpoint;
import com.fluidops.iwb.api.datacatalog.Schema;
import com.fluidops.iwb.model.Vocabulary.RSO;

/**
 * Implementation of {@link RelationalDatabaseEndpoint} interface that bootstraps
 * itself from the underlying RDF graph, given only the dataEndpointId as parameter.
 * 
 * @author msc
 */
@SuppressWarnings("deprecation")
public class RelationalDatabaseEndpointImpl implements RelationalDatabaseEndpoint
{
	private static final Logger logger = 
    		Logger.getLogger(RelationalDatabaseEndpointImpl.class.getName());

	// the data endpoint ID
	protected URI dataEndpointId;
    
    // endpoint content
	protected Schema schema;
	protected DatabaseInfo databaseInfo;
	protected DatabaseConnectionInfo databaseConnectionInfo;

	
    /**
	 * Constructor for class
	 */
	public RelationalDatabaseEndpointImpl(URI dataEndpointId) 
	throws InvalidSchemaSpecificationException
	{
		this.dataEndpointId = dataEndpointId;
		initializeFromRdf(dataEndpointId);
	}

	@Override
	public URI getDataEndpointId() 
	{
		return dataEndpointId;
	}
	
	@Override
	public Schema getSchema()
	{
		return schema;
	}
	
	@Override
	public DatabaseInfo getDatabaseInfo()
	{
		return databaseInfo;
	}
	
	@Override
	public DatabaseConnectionInfo getDatabaseConnectionInfo()
	{
		return databaseConnectionInfo;
	}
	
	/**
	 * Retrieves the URI of the context hosting the {@link DataEndpoint}.
	 * 
	 * @param dataEndpointId
	 * @return the URI of the context
	 * @throws IllegalArgumentException in case there is no such context or the context is not unique
	 */
	protected URI getRelationalDatabaseSerializerContextUri(URI dataEndpointId)
	throws IllegalArgumentException
	{		
		List<Statement> res = EndpointImpl.api().getDataManager().
				getStatementsAsList(dataEndpointId, RDF.TYPE, RSO.TYPE_RELATIONAL_DATA_ENDPOINT,false); 
		
		if (res.isEmpty())
			throw new IllegalArgumentException("RelationalDataEndpoint with id='" + dataEndpointId + "' does not exist.");
		else if (res.size()>1)
			throw new IllegalArgumentException("RelationalDataEndpoint with id='" + dataEndpointId + "' is ambigous.");
		
		Resource ret = res.get(0).getContext();
		if (ret==null)
			throw new IllegalArgumentException("RelationalDataEndpoint with id='" + dataEndpointId + "' has no context associated.");
		else if (!(ret instanceof URI))
			throw new IllegalArgumentException("RelationalDataEndpoint with id='" + dataEndpointId + "' is in non-URI context.");
	
		return (URI)ret;
	}
	
	
	protected void initializeFromRdf(URI dataEndpointId) 
	throws IllegalArgumentException, InvalidSchemaSpecificationException
	{
		URI contextUri = getRelationalDatabaseSerializerContextUri(dataEndpointId);
		
    	
    	List<Statement> stmts = EndpointImpl.api().getDataManager().getStatementsAsList(null, null, null, false, contextUri);
    	
    	if (stmts.isEmpty())
    		throw new IllegalArgumentException("The data endpoint with id '" + dataEndpointId + "' is invalid.");
    	
		initializeFromGraph(new GraphImpl(stmts), dataEndpointId);
	}

	protected void initializeFromGraph(Graph graph, URI dataEndpointId) 
	throws InvalidSchemaSpecificationException
	{
		try
		{
			URI databaseInfoUri = GraphUtil.getOptionalObjectURI(graph, dataEndpointId, RSO.PROP_DATABASE_META_INFORMATION);
			URI databaseConnectionInfoUri = GraphUtil.getOptionalObjectURI(graph, dataEndpointId, RSO.PROP_DATABASE_CONNECTION_INFORMATION);			
			URI schemaUri = GraphUtil.getOptionalObjectURI(graph, dataEndpointId, RSO.PROP_SCHEMA);
			
			if (databaseInfoUri!=null)
				databaseInfo = new DatabaseInfoImpl(graph,databaseInfoUri);
			if (databaseConnectionInfoUri!=null)
				databaseConnectionInfo = new DatabaseConnectionInfoImpl(graph,databaseConnectionInfoUri);
			if (schemaUri!=null)
				schema = new SchemaImpl(graph,schemaUri);
		}
		catch (GraphUtilException e)
		{
			System.out.println("OK");
			logger.warn(e.getMessage());
			throw new RuntimeException(e);
		}
	}
}
