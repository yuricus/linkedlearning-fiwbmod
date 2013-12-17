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

package com.fluidops.iwb.api.valueresolver;


/**
 * The interface {@link ValueResolverFactory} which can be used
 * to instantiate {@link ValueResolver} implementations. Please 
 * consider the description in {@link ValueResolver} for details
 * about usage.
 * 
 * @author as
 *
 */
public interface ValueResolverFactory {

	/**
	 * Method to return the instance of the {@link ValueResolver}. The
	 * name of the resolver is implicitly defined by {@link ValueResolver#name()}.
	 * The name must be unique in the system, i.e. there must not be two
	 * value resolvers having the same name.
	 * 
	 * @return a non-null {@link ValueResolver}
	 */
	public ValueResolver create();	
}
