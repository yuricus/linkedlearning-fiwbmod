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

package com.fluidops.iwb.api.datacatalog;

import java.util.List;

/**
 * Interface for tables in relational databases.
 *
 * @author msc
 */
public interface Table 
{
	// possible table types/classifications
	public static enum TableType 
	{
		ALIAS,
		GLOBAL_TEMPORARY,
		LOCAL_TEMPORARY,
		SYNONYM,
		TABLE,
		VIEW,
		UNKNOWN
	}

	public String getName();
	
	public String getFullName();
	
	public TableType getTableType();
	
	public Schema getSchema();
	
	public List<Column> getColumns();
	
	public PrimaryKey getPrimaryKey();
	
	public List<ForeignKey> getForeignKeys();
	
	public List<Index> getIndices();
}
