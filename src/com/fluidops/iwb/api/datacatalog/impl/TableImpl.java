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
import org.openrdf.model.vocabulary.RDF;

import com.fluidops.iwb.api.datacatalog.Column;
import com.fluidops.iwb.api.datacatalog.ForeignKey;
import com.fluidops.iwb.api.datacatalog.Index;
import com.fluidops.iwb.api.datacatalog.PrimaryKey;
import com.fluidops.iwb.api.datacatalog.Schema;
import com.fluidops.iwb.api.datacatalog.Table;
import com.fluidops.iwb.model.Vocabulary.RSO;

/**
 * Implementation of a relational table that constructs itself from an
 * RDF representation aligned with fluidOps Relational Schema Ontology.
 * 
 * @author msc, mm
 */
public class TableImpl implements Table
{
	private static final Logger logger = 
    		Logger.getLogger(TableImpl.class.getName());

	protected String name;
	protected String fullName;
	protected TableType tableType;
	
	protected Schema schema;
	protected List<Column> columns=null;
	protected PrimaryKey pKey=null;
	protected List<ForeignKey> fkeys=null;
	protected List<Index> indices=null;
	
	protected Graph graph;
	protected URI tableUri;


	@SuppressWarnings("deprecation")
	public TableImpl(Graph graph, URI tableUri, Schema schema)
	throws InvalidSchemaSpecificationException
	{
		try
		{
			this.schema = schema;
			this.graph=graph;
			this.tableUri=tableUri;
			
			Literal nameLit = GraphUtil.getOptionalObjectLiteral(graph, tableUri, RSO.PROP_TABLE_NAME);
			if (nameLit!=null)
				name = nameLit.stringValue();
			
			Literal fullNameLit = GraphUtil.getOptionalObjectLiteral(graph, tableUri, RSO.PROP_TABLE_FULL_NAME);
			if (fullNameLit!=null)
				fullName = fullNameLit.stringValue();
	
			URI tableTypeUri = GraphUtil.getOptionalObjectURI(graph, tableUri, RSO.PROP_TABLE_TYPE);
			if (tableTypeUri!=null)
				tableType = tableTypeFromUri(tableTypeUri);
			
			

		}
		catch (GraphUtilException e)
		{
			logger.warn(e.getMessage());
			throw new RuntimeException(e);
		}
	}
	
	public synchronized void generateConstraints() 
			throws InvalidSchemaSpecificationException {
		
		// key & foreign key extraction is a bit more tricky: there must be a RSO.PROP_TABLE_CONSTRAINT
		// pointing to an object of type RSO.TYPE_PRIMARY_KEY or RSO.TYPE_FOREIGN_KEY, respectively;
		// we also enforce that exactly one primary key exists for the table
		Iterator<Value> keyUris = GraphUtil.getObjectIterator(graph, tableUri, RSO.PROP_TABLE_CONSTRAINT);
		URI pKeyUri = null; // the one and only primary key
	
		if(fkeys==null) {
			fkeys = new ArrayList<ForeignKey>();
			while (keyUris.hasNext())
			{
				Value keyUriAsValue = keyUris.next(); // may be key, foreign key, etc.
				if (keyUriAsValue instanceof URI)
				{
					URI keyUri = (URI)keyUriAsValue;
					if (graph.match(keyUri, RDF.TYPE, RSO.TYPE_PRIMARY_KEY).hasNext())
					{
						if (pKeyUri==null)
							pKeyUri = keyUri;
						else
							throw new InvalidSchemaSpecificationException("Duplicate primary key for table " + tableUri);
					}
					else if (graph.match(keyUri, RDF.TYPE, RSO.TYPE_FOREIGNKEY).hasNext())
					{
						fkeys.add(new ForeignKeyImpl(graph, keyUri));
					}
				} // otherwise: shouldn't happen, so let's ignore
			}
			
		}
		
			// the code above must have initialized the pKeyUri variable:
		if(pKey==null) {
			if (pKeyUri==null)
				throw new InvalidSchemaSpecificationException("No primary key for table " + tableUri + " found");
			else
				pKey = new PrimaryKeyImpl(graph,pKeyUri);
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
	public TableType getTableType()
	{
		return tableType;
	}
	
	
	public void setTableType(TableType tableType)
	{
		this.tableType = tableType;
	}
	
	@Override
	public Schema getSchema()
	{
		return schema;
	}
	
	@Override
	public List<Column> getColumns()
	{
		if(columns==null) {
			try {
				List<Column> tmpColumns = new ArrayList<Column>();
				Iterator<Value> columnUris = GraphUtil.getObjectIterator(graph, tableUri, RSO.PROP_TABLE_COLUMN);
				while (columnUris.hasNext())
				{
					Value columnUri = columnUris.next();
					if (columnUri instanceof URI)
							tmpColumns.add(new ColumnImpl(graph,(URI)columnUri));	
				}
				columns = ColumnImpl.assertValidAndOrder(tmpColumns);
			} catch (InvalidSchemaSpecificationException e) {
				logger.warn(e.getMessage());
				throw new RuntimeException(e);
			}
		}
		return columns;
	}
	
	@Override
	public PrimaryKey getPrimaryKey()
	{
		try {
			generateConstraints();
		} catch (InvalidSchemaSpecificationException e) {
			logger.warn(e.getMessage());
			throw new RuntimeException(e);
		}
		
		return pKey;
	}
	
	@Override
	public List<ForeignKey> getForeignKeys()
	{
		try {
			generateConstraints();
		} catch (InvalidSchemaSpecificationException e) {
			logger.warn(e.getMessage());
			throw new RuntimeException(e);
		}		
		return fkeys;
	}
	
	@Override
	public List<Index> getIndices()
	{
		
		if(indices==null) {
			indices=new ArrayList<Index>();
			Iterator<Value> indexUris = GraphUtil.getObjectIterator(graph, tableUri, RSO.PROP_TABLE_INDEX);
			while (indexUris.hasNext())
			{
				Value indexUri = indexUris.next();
				if (indexUri instanceof URI)
					try {
						indices.add(new IndexImpl(graph,(URI)indexUri));
					} catch (InvalidSchemaSpecificationException e) {
						logger.warn(e.getMessage());
						throw new RuntimeException(e);
					}
			}
		}
		return indices;
	}
	
	/**
	 * Sets the table type by URI
	 */
	public static TableType tableTypeFromUri(URI tableTypeUri)
	{
		TableType tt = TableType.UNKNOWN;
		if (tableTypeUri!=null)
		{
			if (tableTypeUri.equals(RSO.INDIVIDUAL_TABLE_TYPE_ALIAS))
				tt = TableType.ALIAS;
			else if (tableTypeUri.equals(RSO.INDIVIDUAL_TABLE_TYPE_LOCAL_TEMPORARY))
				tt = TableType.LOCAL_TEMPORARY;
			else if (tableTypeUri.equals(RSO.INDIVIDUAL_TABLE_TYPE_GLOBAL_TEMPORARY))
				tt = TableType.SYNONYM;
			else if (tableTypeUri.equals(RSO.INDIVIDUAL_TABLE_TYPE_TABLE))
				tt = TableType.TABLE;
			else if (tableTypeUri.equals(RSO.INDIVIDUAL_TABLE_TYPE_VIEW))
				tt = TableType.VIEW;
			else // tableTypeUri.equals(RSO.INDIVIDUAL_INDEX_TYPE_UNKOWN) or invalid
				tt = TableType.UNKNOWN;
		}
		return tt;
	}
}
