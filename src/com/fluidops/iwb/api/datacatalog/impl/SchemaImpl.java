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
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.openrdf.model.Graph;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.util.GraphUtil;
import org.openrdf.model.util.GraphUtilException;

import com.fluidops.iwb.api.datacatalog.Schema;
import com.fluidops.iwb.api.datacatalog.Table;
import com.fluidops.iwb.model.Vocabulary.RSO;

/**
 * Implementation of a {@link Schema} that constructs itself from an
 * RDF representation aligned with fluidOps Relational Schema Ontology.
 * 
 * @author msc, mm
 */
public class SchemaImpl implements Schema
{
	private static final Logger logger = 
    		Logger.getLogger(SchemaImpl.class.getName());
	
	protected String name;
	protected String fullName;
	
	protected List<Table> tables=null;
	
	protected Graph graph;
	protected URI schemaUri;
	
	
	public SchemaImpl(Graph graph, URI schemaUri) 
	throws InvalidSchemaSpecificationException
	{
		this.graph=graph;
		this.schemaUri=schemaUri;
		
		try
		{
			Literal nameLit = GraphUtil.getOptionalObjectLiteral(graph, schemaUri, RSO.PROP_SCHEMA_NAME);
			if (nameLit!=null)
				name = nameLit.stringValue();
				
			Literal fullNameLit = GraphUtil.getOptionalObjectLiteral(graph, schemaUri, RSO.PROP_SCHEMA_FULL_NAME);
			if (fullNameLit!=null)
				fullName = fullNameLit.stringValue();
	
		}
		catch (GraphUtilException e)
		{
			logger.warn(e.getMessage());
			throw new RuntimeException(e);			
		}
	}
	
	@Override
	public String getName()
	{
		return name;
	}
	
	@Override
	public String getFullName()
	{
		return fullName;
	}

	@Override
	public synchronized List<Table> getTables()
	{
		
		if(tables==null) {
			tables=new ArrayList<Table>();
			try {
				Iterator<Value> values = GraphUtil.getObjectIterator(graph, schemaUri, RSO.PROP_TABLE);
				while (values.hasNext())
				{
					Value v = values.next();
					if (v instanceof URI)
					
							tables.add(new TableImpl(graph,(URI)v,this));

				}
			} catch (InvalidSchemaSpecificationException e) {
				logger.warn(e.getMessage());
				throw new RuntimeException(e);
			}
		}
		return tables;
	}
	
	@Override 
	public Table getTable(String tableName)
	{
		getTables();
		for (Table table : tables)
		{
			if (table.getName().equals(tableName))
				return table;
		}
		
		return null;
	}
}
