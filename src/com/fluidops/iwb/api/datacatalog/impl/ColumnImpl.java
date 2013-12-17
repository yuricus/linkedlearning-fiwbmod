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

import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.openrdf.model.Graph;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.util.GraphUtil;
import org.openrdf.model.util.GraphUtilException;

import com.fluidops.iwb.api.datacatalog.Column;
import com.fluidops.iwb.api.datacatalog.ColumnDataType;
import com.fluidops.iwb.model.Vocabulary.RSO;


/**
 * Implementation of a {@link Column} that constructs itself from an
 * RDF representation aligned with fluidOps Relational Schema Ontology.
 * 
 * @author msc
 */
public class ColumnImpl implements Column
{
	private static final Logger logger = 
    		Logger.getLogger(ColumnImpl.class.getName());
	
	protected String name;		
	protected String shortName;
	protected String fullName;
	
	ColumnDataType columnDataType;
	protected int ordinalPosition;
	
	public ColumnImpl(Graph graph, URI columnUri)
	throws InvalidSchemaSpecificationException
	{
		try
		{
			// first, there must be exactly one column ordinal position
			Literal ordinalPositionLit = GraphUtil.getOptionalObjectLiteral(graph, columnUri, RSO.PROP_POSITION);
			if (ordinalPositionLit!=null)
			{
				try
				{
					ordinalPosition = Integer.valueOf(ordinalPositionLit.stringValue());
				}
				catch (NumberFormatException e)
				{
					logger.warn(e.getMessage());
					throw new InvalidSchemaSpecificationException("Invalid column ID: " + ordinalPositionLit.stringValue() + " for column " + columnUri);
				}
			}
			else
				throw new InvalidSchemaSpecificationException("No ordinal position provided for column " + columnUri);
				
			
			// the rest of the column info is stored inside the associated column object
			// TODO: adjust naming for variable
			URI columnDataUri = GraphUtil.getOptionalObjectURI(graph, columnUri, RSO.PROP_CONSTITUENT);
			if (columnDataUri==null)
				throw new InvalidSchemaSpecificationException("Data specification for column " + columnUri + " missing.");
			
			Literal nameLit = GraphUtil.getOptionalObjectLiteral(graph, columnDataUri, RSO.PROP_COLUMN_NAME);
			if (nameLit!=null)
				name = nameLit.stringValue();
	
			Literal shortNameLit = GraphUtil.getOptionalObjectLiteral(graph, columnDataUri, RSO.PROP_COLUMN_SHORT_NAME);
			if (shortNameLit!=null)
				shortName = shortNameLit.stringValue();
	
			Literal fullNameLit = GraphUtil.getOptionalObjectLiteral(graph, columnDataUri, RSO.PROP_COLUMN_FULL_NAME);
			if (fullNameLit!=null)
				fullName = fullNameLit.stringValue();
			
			URI datatypeUri = GraphUtil.getOptionalObjectURI(graph, columnDataUri, RSO.PROP_DATATYPE);
			if (datatypeUri!=null)
				columnDataType = new ColumnDataTypeImpl(graph,datatypeUri);
			else
				throw new InvalidSchemaSpecificationException("Column " + columnUri + " missing datatype information.");			
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
	public String getShortName()
	{
		return shortName;
	}
	
	@Override
	public String getFullName()
	{
		return fullName;
	}
	
	@Override
	public ColumnDataType getColumnDataType()
	{
		return columnDataType;
	}
	
	@Override
	public int getOrdinalPosition()
	{
		return ordinalPosition;
	}
	
	/**
	 * Checks whether the ordinal positions of the columns in the list are
	 * valid and complete (i.e., starting at 1 and incremental) and returns
	 * an list of columns ordered by their column IDs back. Throws an
	 * {@link InvalidSchemaSpecificationException} if not valid and complete.
	 * 
	 * @param columns 
	 * @return the same list of columns, but ordered by ordinal position
	 * @throws InvalidSchemaSpecificationException
	 */
	public static List<Column> assertValidAndOrder(List<Column> columns)
	throws InvalidSchemaSpecificationException
	{
		Column[] res = new Column[columns.size()];
		for (Column column : columns)
		{
			int colPos = column.getOrdinalPosition(); // starting at 1
			int insertPos = colPos-1;					// starting at 0
			
			if (insertPos<0 || insertPos>=columns.size())
				throw new InvalidSchemaSpecificationException("Illegal ordinalPosition " + colPos + " detected.");
			if (res[insertPos]!=null)
				throw new InvalidSchemaSpecificationException("Duplicate ordinalPosition " + colPos + " detected.");
			
			res[insertPos] = column;
		}
		
		return Arrays.asList((Column[])res);
	}
}
