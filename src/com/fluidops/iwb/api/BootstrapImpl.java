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

package com.fluidops.iwb.api;

import java.rmi.Naming;

import com.fluidops.api.Bootstrap;

public class BootstrapImpl implements Bootstrap
{
    public Class getApiClass()
    {
        return API.class;
    }

    public Object getApiStub() throws Exception
    {
        return (API)Naming.lookup ("OM");
    }

    public void init() throws Exception
    {
    }

    public LoginMode getLoginMode()
    {
        return LoginMode.NONE;
    }

	@Override
	public Object getCompletor( String language )
	{
		return null;
	}
}
