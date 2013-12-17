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

import com.fluidops.iwb.api.datacatalog.Column;
import com.fluidops.iwb.api.datacatalog.Index.IndexType;
import com.fluidops.iwb.api.datacatalog.PrimaryKey;
import com.fluidops.iwb.model.Vocabulary.RSO;

/**
 * Implementation of a {@link PrimaryKey} that constructs itself from an
 * RDF representation aligned with fluidOps Relational Schema Ontology.
 * 
 * @author msc
 */
public class PrimaryKeyImpl implements PrimaryKey
{
	private static final Logger logger = 
    		Logger.getLogger(PrimaryKeyImpl.class.getName());

	String name;
	String shortName;
	String fullName;
	IndexType indexType;
	
	List<Column> columns;

	
	public PrimaryKeyImpl(Graph graph, URI pKeyUri)
	throws InvalidSchemaSpecificationException
	{
		try
		{
			Literal nameLit = GraphUtil.getOptionalObjectLiteral(graph, pKeyUri, RSO.PROP_CONSTRAINT_NAME);
			if (nameLit!=null)
				name = nameLit.stringValue();

			Literal shortNameLit = GraphUtil.getOptionalObjectLiteral(graph, pKeyUri, RSO.PROP_CONSTRAINT_SHORT_NAME);
			if (shortNameLit!=null)
				shortName = shortNameLit.stringValue();
			
			Literal fullNameLit = GraphUtil.getOptionalObjectLiteral(graph, pKeyUri, RSO.PROP_CONSTRAINT_FULL_NAME);
			if (fullNameLit!=null)
				fullName = fullNameLit.stringValue();

			URI indexTypeUri = GraphUtil.getOptionalObjectURI(graph, pKeyUri, RSO.PROP_INDEX_TYPE);
			if (indexTypeUri!=null)
				indexType = IndexImpl.indexTypeFromUri(indexTypeUri);
			
			List<Column> tmpColumns = new ArrayList<Column>();
			Iterator<Value> columnUris = GraphUtil.getObjectIterator(graph, pKeyUri, RSO.PROP_CONSTRAINT_COLUMN);
			while (columnUris.hasNext())
			{
				Value columnUri = columnUris.next();
				if (columnUri instanceof URI)
					tmpColumns.add(new ColumnImpl(graph,(URI)columnUri));
			}
			columns = ColumnImpl.assertValidAndOrder(tmpColumns);
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
	public String shortName()
	{
		return shortName;
	}
	
	@Override
	public String fullName()
	{
		return fullName;
	}
	
	@Override
	public IndexType getIndexType()
	{
		return indexType;
	}
	
	@Override
	public List<Column> getColumns()
	{
		return columns;
	}
}
