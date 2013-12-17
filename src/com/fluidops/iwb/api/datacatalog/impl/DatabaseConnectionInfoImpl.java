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

import org.apache.log4j.Logger;
import org.openrdf.model.Graph;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.util.GraphUtil;
import org.openrdf.model.util.GraphUtilException;

import com.fluidops.iwb.api.datacatalog.DatabaseConnectionInfo;
import com.fluidops.iwb.model.Vocabulary.RSO;
import com.fluidops.util.StringUtil;
import com.fluidops.util.user.PwdSafe;

/**
 * Implementation of a {@link DatabaseConnectionInfo} that constructs itself from an
 * RDF representation aligned with fluidOps Relational Schema Ontology. The password
 * of the connection info is loaded from secrets.xml, currently assuming there
 * is a RDFMetaInformationProvider provider that stores the password.
 * 
 * @author msc
 */
public class DatabaseConnectionInfoImpl implements DatabaseConnectionInfo
{
	private static final Logger logger = 
    		Logger.getLogger(DatabaseConnectionInfoImpl.class.getName());

	protected String schemaName;
	protected String driverClass;
	protected String connectionString;
	protected String user;
	protected String host; // host in secrets.xml for password access
	
	public DatabaseConnectionInfoImpl(Graph graph, URI databaseConnectionInfoUri)
	throws InvalidSchemaSpecificationException
	{
		try
		{
			Literal schemaNameLit = GraphUtil.getOptionalObjectLiteral(graph, databaseConnectionInfoUri, RSO.PROP_CONN_SCHEMA_NAME);
			if (schemaNameLit!=null)
				schemaName = schemaNameLit.stringValue();
			
			Literal driverClassLit = GraphUtil.getOptionalObjectLiteral(graph, databaseConnectionInfoUri, RSO.PROP_CONN_DRIVER_CLASS);
			if (driverClassLit!=null)
				driverClass = driverClassLit.stringValue();

			Literal connectionStringLit = GraphUtil.getOptionalObjectLiteral(graph, databaseConnectionInfoUri, RSO.PROP_CONN_CONNECTION_STRING);
			if (connectionStringLit!=null)
				connectionString = connectionStringLit.stringValue();

			Literal hostLit = GraphUtil.getOptionalObjectLiteral(graph, databaseConnectionInfoUri, RSO.PROP_CONN_HOST);
			if (hostLit!=null)
				host = hostLit.stringValue();

			
			Literal userLit = GraphUtil.getOptionalObjectLiteral(graph, databaseConnectionInfoUri, RSO.PROP_CONN_USER);
			if (userLit!=null)
				user = userLit.stringValue();
		}
		catch (GraphUtilException e)
		{
			logger.warn(e.getMessage());
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public String getSchemaName() 
	{
		return schemaName;
	}

	public String getDriverClass() 
	{
		return driverClass;
	}

	public String getConnectionString()
	{
		return connectionString;
	}

	public String getUser()
	{
		return user;
	}

	/**
	 * The ID of the provider storing the password. Returns null if no passwd.
	 * 
	 * @param providerId
	 * @return
	 */
	public String getPassword()
	{
		if (StringUtil.isEmpty(host))
			return null; // no passwd
		
		return PwdSafe.getPwd("IWBProvider", host, user);
	}
}
