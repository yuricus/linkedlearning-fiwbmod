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

import com.fluidops.iwb.api.datacatalog.ColumnDataType;
import com.fluidops.iwb.model.Vocabulary.RSO;


/**
 * Implementation of a {@link ColumnDataType} that constructs itself from an
 * RDF representation aligned with fluidOps Relational Schema Ontology.
 * 
 * @author msc
 */
public class ColumnDataTypeImpl implements ColumnDataType
{
	private static final Logger logger = 
    		Logger.getLogger(ColumnDataTypeImpl.class.getName());

	protected String name;
	protected String fullName;
	
	protected String typeName;
	
	public ColumnDataTypeImpl(Graph graph, URI columnDataTypeUri)
	throws InvalidSchemaSpecificationException
	{
		try
		{
			Literal nameLit = GraphUtil.getOptionalObjectLiteral(graph, columnDataTypeUri, RSO.PROP_COLUMN_DATATYPE_NAME);
			if (nameLit!=null)
				name = nameLit.stringValue();
	
			Literal fullNameLit = GraphUtil.getOptionalObjectLiteral(graph, columnDataTypeUri, RSO.PROP_COLUMN_DATATYPE_FULL_NAME);
			if (fullNameLit!=null)
				fullName = fullNameLit.stringValue();
			
			Literal typeNameLit = GraphUtil.getOptionalObjectLiteral(graph, columnDataTypeUri, RSO.PROP_COLUMN_DATATYPE_TYPE_NAME);
			if (typeNameLit!=null)
				typeName = typeNameLit.stringValue();
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
	public String getTypeName()
	{
		return typeName;
	}
	
	
}
