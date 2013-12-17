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

import org.apache.log4j.Logger;
import org.openrdf.model.Graph;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.util.GraphUtil;
import org.openrdf.model.util.GraphUtilException;

import com.fluidops.iwb.api.datacatalog.Column;
import com.fluidops.iwb.api.datacatalog.ForeignKeyColumnReference;
import com.fluidops.iwb.model.Vocabulary.RSO;

/**
 * Implementation of a {@link ForeignKeyColumnReference} that constructs itself from an
 * RDF representation aligned with fluidOps Relational Schema Ontology.
 * 
 * @author msc, mm
 */
public class ForeignKeyColumnReferenceImpl implements ForeignKeyColumnReference
{
	private static final Logger logger = 
    		Logger.getLogger(ForeignKeyColumnReferenceImpl.class.getName());
	
	protected Column primaryKeyColumn=null;
	protected Column foreignKeyColumn=null;

	public ForeignKeyColumnReferenceImpl(Graph graph,URI fKeyUri, URI fKeyColumnReferenceUri)
	throws InvalidSchemaSpecificationException
	{
		try {
			// fetch tableColumn in table and set foreignKeyColumn
			URI fKeyColumnReferenceColumnInfo;
			fKeyColumnReferenceColumnInfo = GraphUtil.getOptionalObjectURI(graph,fKeyColumnReferenceUri,RSO.PROP_CONSTITUENT);
			URI table=GraphUtil.getOptionalSubjectURI(graph, RSO.PROP_TABLE_CONSTRAINT, fKeyUri);
				
			Iterator<Value> tableColumns=GraphUtil.getObjectIterator(graph, table,RSO.PROP_TABLE_COLUMN);
		
			while(tableColumns.hasNext() && foreignKeyColumn==null) {
				URI v=(URI) tableColumns.next();
				URI tableColumnInfo=GraphUtil.getOptionalObjectURI(graph,v,RSO.PROP_CONSTITUENT);
			
				if(tableColumnInfo.stringValue().equals(fKeyColumnReferenceColumnInfo.stringValue()))
					foreignKeyColumn=new ColumnImpl(graph,v);
			}
			
			if(this.foreignKeyColumn==null)
				throw new InvalidSchemaSpecificationException("Inconsistent foreign key " + fKeyUri);
		
			// fetch column position in target table
			Literal position=GraphUtil.getOptionalObjectLiteral(graph,fKeyColumnReferenceUri,RSO.PROP_POSITION);
		
			// fetch target table
			URI targetKey=GraphUtil.getOptionalObjectURI(graph,fKeyUri,RSO.PROP_REFERENCES_KEY);
			
			Resource targetTableAsResource=GraphUtil.getOptionalSubject(graph,RSO.PROP_TABLE_CONSTRAINT,targetKey);
			URI targetTable=null;
			
			if(targetTableAsResource instanceof URI)
				targetTable=(URI) targetTableAsResource;
			else
				throw new InvalidSchemaSpecificationException("Inconsistent foreign key " + fKeyUri+" : referenced key "+ targetKey+" does not have a table associated.");
			
			// fetch correct column
			tableColumns=GraphUtil.getObjectIterator(graph, targetTable,RSO.PROP_TABLE_COLUMN);
			
			while(tableColumns.hasNext() && this.primaryKeyColumn==null) {
				URI v=(URI) tableColumns.next();
				Literal tableColumnPosition=GraphUtil.getOptionalObjectLiteral(graph,v,RSO.PROP_POSITION);
			
				if(position.stringValue().equals(tableColumnPosition.stringValue()))
					this.primaryKeyColumn=new ColumnImpl(graph,v);
			}
			
			if(this.primaryKeyColumn==null)
				throw new InvalidSchemaSpecificationException("Inconsistent key referenced by foreign key " + fKeyUri+": primary key column could not be retrieved");
			
		} catch (GraphUtilException e) {
			logger.warn(e.getMessage());
			throw new RuntimeException(e);
		}
		
	}

	@Override
	public Column getPrimaryKeyColumn()
	{
		return primaryKeyColumn;
	}
	
	@Override
	public Column getForeignKeyColumn()
	{
		return foreignKeyColumn;
	}
}
