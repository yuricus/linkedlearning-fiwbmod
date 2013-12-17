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
 * Represents a schema in a relational database.
 * 
 * @author msc
 */
public interface Schema 
{
	public String getName();
	
	public String getFullName();
	
	public List<Table> getTables();
	
	/**
	 * Gets a table by name. Returns null if no table with the specified
	 * name exists.
	 * 
	 * @param name the name of the table to fetch
	 * @return the Table object
	 */
	public Table getTable(String name);
}
