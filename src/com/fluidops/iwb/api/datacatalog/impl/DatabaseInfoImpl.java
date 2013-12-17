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

import com.fluidops.iwb.api.datacatalog.DatabaseInfo;
import com.fluidops.iwb.model.Vocabulary.RSO;

/**
 * Implementation of a {@link DatabaseInfo} that constructs itself from an
 * RDF representation aligned with fluidOps Relational Schema Ontology.
 * 
 * @author msc
 */
public class DatabaseInfoImpl implements DatabaseInfo
{
	private static final Logger logger = 
    		Logger.getLogger(DatabaseInfoImpl.class.getName());
	
	protected String productName;
	protected String productVersion;
	protected String databaseInfo;

	
	public DatabaseInfoImpl(Graph graph, URI databaseInfoUri)
	throws InvalidSchemaSpecificationException
	{
		try
		{
			Literal productNameLit = GraphUtil.getOptionalObjectLiteral(graph, databaseInfoUri, RSO.PROP_PRODUCT_NAME);
			if (productNameLit!=null)
				productName = productNameLit.stringValue();
			
			Literal productVersionLit = GraphUtil.getOptionalObjectLiteral(graph, databaseInfoUri, RSO.PROP_PRODUCT_VERSION);
			if (productVersionLit!=null)
				productVersion = productVersionLit.stringValue();

			Literal databaseInfoLit = GraphUtil.getOptionalObjectLiteral(graph, databaseInfoUri, RSO.PROP_DATABASE_INFO);
			if (databaseInfoLit!=null)
				databaseInfo = databaseInfoLit.stringValue();
		}
		catch (GraphUtilException e)
		{
			logger.warn(e.getMessage());
			throw new RuntimeException(e);
		}
	}

	@Override
	public String getProductName()
	{
		return productName;
	}
	
	@Override
	public String getProductVersion()
	{
		return productVersion;
	}
	
	@Override
	public String getDatabaseInfo()
	{
		return databaseInfo;
	}
}
