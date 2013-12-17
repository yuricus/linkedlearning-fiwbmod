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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.openrdf.model.Graph;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.util.GraphUtil;

import com.fluidops.iwb.api.datacatalog.ForeignKey;
import com.fluidops.iwb.api.datacatalog.ForeignKeyColumnReference;
import com.fluidops.iwb.model.Vocabulary.RSO;

/**
 * Implementation of a {@link ForeignKey} that constructs itself from an
 * RDF representation aligned with fluidOps Relational Schema Ontology.
 * 
 * @author msc, mm
 */

public class ForeignKeyImpl implements ForeignKey
{
	private static final Logger logger = 
    		Logger.getLogger(ForeignKeyImpl.class.getName());
	
	protected String name;
	protected String fullName;
	
	protected List<ForeignKeyColumnReference> columnReferences=null;
	protected Graph graph;
	protected URI fKeyUri;
	
	
	public ForeignKeyImpl(Graph graph, URI fKeyUri)
	throws InvalidSchemaSpecificationException
	{
		this.graph=graph;
		this.fKeyUri=fKeyUri;

		//set name field
		Iterator<Value> names=GraphUtil.getObjects(graph, fKeyUri, RSO.PROP_CONSTRAINT_NAME).iterator();
		if(names.hasNext())
			name=names.next().stringValue();
		else
			throw new InvalidSchemaSpecificationException("No name for foreign key " + fKeyUri + " found");
		
		if(names.hasNext())
			throw new InvalidSchemaSpecificationException("Multiple names for foreign key " + fKeyUri + " found");
		
		// set fullName field
		Iterator<Value> fullNames=GraphUtil.getObjects(graph, fKeyUri, RSO.PROP_CONSTRAINT_FULL_NAME).iterator();
		if(fullNames.hasNext())
			fullName=fullNames.next().stringValue();
		else
			throw new InvalidSchemaSpecificationException("No full name for foreign key " + fKeyUri + " found");
		
		if(fullNames.hasNext())
			throw new InvalidSchemaSpecificationException("Multiple full names for foreign key " + fKeyUri + " found");
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
	public synchronized List<ForeignKeyColumnReference> getColumnReferences()
	{
		// set column references
		if(columnReferences==null) {
			try {
				columnReferences=new LinkedList<ForeignKeyColumnReference>();
				Iterator<Value> constraintColumns=GraphUtil.getObjects(graph, fKeyUri, RSO.PROP_CONSTRAINT_COLUMN).iterator();
				
				while(constraintColumns.hasNext()) {
					Value v=constraintColumns.next();
					if(v instanceof URI) {
						URI u=(URI) v;
						columnReferences.add(new ForeignKeyColumnReferenceImpl(graph,fKeyUri,u));
					} else {
						throw new InvalidSchemaSpecificationException("Column " + v + " must be a URI");
					}
				}

			} catch (InvalidSchemaSpecificationException e) {
				logger.warn(e.getMessage());
				throw new RuntimeException(e);
			}	
		}					
				
		return columnReferences;
	}
}
