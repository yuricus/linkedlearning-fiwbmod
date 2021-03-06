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

import java.rmi.RemoteException;
import java.util.List;

import org.openrdf.model.URI;

import com.fluidops.api.endpoint.EndpointDescription;
import com.fluidops.api.endpoint.RMIRemote;
import com.fluidops.api.endpoint.RMIUtils;
import com.fluidops.api.security.impl.RMISessionContext;
import com.fluidops.iwb.api.API;
import com.fluidops.network.RMIBase;

/**
 * Remote access to data catalog service
 * 
 * @author msc
 */
public class DataCatalogServiceRemote extends RMIBase implements DataCatalogService, RMIRemote
{
    /**
     * 
     */
    private static final long serialVersionUID = -3311138402022229654L;
    
    /**
     * The provider service this class delegates to
     */
    DataCatalogService delegate;

    /**
     * Constructor 
     * 
     * @throws RemoteException
     */
    public DataCatalogServiceRemote() throws RemoteException
    {
        super();
    }
    
    @Override
    public void init(RMISessionContext sessionContext,
            EndpointDescription bootstrap) throws Exception
    {
        delegate = (DataCatalogService) RMIUtils.getDelegate(sessionContext,
                bootstrap, ((API) bootstrap.getServerApi())
                        .getDataCatalogService(), DataCatalogService.class);
    }
    
    @Override
	public DataEndpoint loadDataEndpoint(URI dataEndpointId)
	throws IllegalArgumentException, Exception
	{
		return delegate.loadDataEndpoint(dataEndpointId);
	}
	
    @Override
	public List<DataEndpoint> loadDataEndpoints() throws Exception
	{
    	return delegate.loadDataEndpoints();
	}
	
    @Override
	public boolean dataEndpointExists(URI dataEndpointId) throws Exception
	{
		return delegate.dataEndpointExists(dataEndpointId);
	}
}
