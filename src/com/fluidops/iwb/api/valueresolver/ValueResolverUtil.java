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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.ServiceLoader;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.openrdf.model.Literal;
import org.openrdf.model.Value;

import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.ReadDataManagerImpl;

/**
 * Convenience functions for value resolvers.
 * 
 * @author as
 *
 */
public class ValueResolverUtil {

	
	private static Logger logger = Logger.getLogger(ValueResolverUtil.class);
	
	
	/**
	 * This method initializes custom {@link ValueResolver} extensions
	 * using the Java {@link ServiceLoader} mechanism. It is invoked
	 * on startup of the application, and looks up all registered
	 * {@link ValueResolverFactory} in META-INF. See {@link ValueResolver}
	 * for details.
	 */
	public static void initializeValueResolverExtensions() {
		
		logger.info("Initializing value resolver extensions");
		
		ServiceLoader<ValueResolverFactory> serviceLoader = ServiceLoader.load(ValueResolverFactory.class);
		Iterator<ValueResolverFactory> iter = serviceLoader.iterator();
		while (iter.hasNext()) {
			ValueResolverFactory vrf = iter.next();
			ValueResolver valueResolver = vrf.create();
			logger.debug("Registered custom value resolver: " + valueResolver.name());
			ValueResolverRegistry.getInstance().register(valueResolver);
		}
	}
	
	/**
	 * Default handling for URIs and Literals as of
	 * {@link #resolveDefault(Value, String)} using
	 * {@link ValueResolver#UNDEFINED} if the value
	 * is not defined.
	 * 
	 * @param value
	 * @return
	 */
	public static String resolveDefault(Value value) {
		return resolveDefault(value, ValueResolver.UNDEFINED);
	}
	
	
	/**
	 * Default handling for URIs and Literals.
	 * 
	 * @param value
	 *            The value itself (URI/Literal)
	 * @param def
	 *            The default text in case the value cannot be resolved
	 * @return Returns a String (= Literal) or a link (= URI)
	 */
	public static String resolveDefault(Value value, String def) {
		if (value == null || value.stringValue().isEmpty())
			return def; // default value

		if (value instanceof Literal)
			return StringEscapeUtils.escapeHtml(value.stringValue());
		else
			return EndpointImpl.api().getRequestMapper()
					.getAHrefFromValue(value);
	}
	
	
	/**
     * Converts a system date like '2011-03-31T19:54:33' to user-readable date.
     * If the input is not a valid system date, the value is returned as is.
     * 
     * @param sysdate
     * @return
     */
	@SuppressWarnings("deprecation")
	public static String resolveSystemDate(String sysdate) {

		Date d = ReadDataManagerImpl.ISOliteralToDate(sysdate);
		if (d == null)
			return StringEscapeUtils.escapeHtml(sysdate);

		DateFormat df = null;
		if (d.getHours() == 0 && d.getMinutes() == 0 && d.getSeconds() == 0)
			df = new SimpleDateFormat("MMMMM dd, yyyy");
		else
			df = new SimpleDateFormat("MMMMM dd, yyyy, HH:mm:ss");
		return df.format(d);
	    
	}
}
