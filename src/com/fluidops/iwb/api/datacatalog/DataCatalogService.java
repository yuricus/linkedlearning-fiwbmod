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

import java.rmi.Remote;
import java.util.List;

import org.openrdf.model.URI;



/**
 * Services related to the storage, retrieval, and access of/to data catalogs
 * that are stored in the RDF database using the IWB's data endpoint ontology.
 * 
 * @author msc
 */
public interface DataCatalogService extends Remote
{	
	/**
	 * Checks whether a data endpoint for the given id exists in the RDF database.
	 */
	public boolean dataEndpointExists(URI dataEndpointId) throws Exception;
	
	/**
	 * Returns the {@link DataEndpoint} PoJo object associated with the
	 * specified data catalog entry, which allows browsing through the
	 * database's meta information. If no catalog entry for the specified
	 * id exists, an {@link IllegalArgumentException} is thrown.
	 * 
	 * @param id the catalog entry id
	 */
	public DataEndpoint loadDataEndpoint(URI dataEndpointId)
	throws IllegalArgumentException, Exception;
	
	/**
	 * Returns a list of all {@link DataEndpoint} PoJo objects registered in
	 * the data catalog, which allows browsing through the databases' meta
	 * information. If no data entries exist, the empty list is returned.
	 */	
	public List<DataEndpoint> loadDataEndpoints() throws Exception;
}