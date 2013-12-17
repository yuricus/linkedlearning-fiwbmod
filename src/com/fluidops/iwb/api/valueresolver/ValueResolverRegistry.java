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

import com.fluidops.util.ExtensibleEnum;
import com.fluidops.util.Singleton;

/**
 * The {@link ValueResolverRegistry} is an {@link ExtensibleEnum} keeping
 * all registered {@link ValueResolver}s, i.e. a mapping from the 
 * value resolver name to the actual {@link ValueResolver}.
 * 
 * The system wide singleton instance is initialized with a set of 
 * built-in {@link ValueResolver}s, as given by 
 * {@link #initializeSystemValueResolvers(ValueResolverRegistry)}. In 
 * addition it is possible to provide custom extensions, see
 * {@link ValueResolver} class documentation for details.
 * 
 * @author as
 *
 */
public class ValueResolverRegistry extends ExtensibleEnum<ValueResolver> {
	
	
	private static Singleton<ValueResolverRegistry>  instance = new Singleton<ValueResolverRegistry>() {
		@Override
		protected ValueResolverRegistry createInstance() throws Exception {
			ValueResolverRegistry res = new ValueResolverRegistry();
			initializeSystemValueResolvers(res);
			return res;
		}		
	};
	
	/**
	 * Get the singleton {@link ValueResolverRegistry} instance.
	 * @return
	 */
	public static ValueResolverRegistry getInstance() {
		return instance.instance();
	}
	
	private ValueResolverRegistry() { }
	
	/**
	 * Returns true, if there exists a {@link ValueResolver} for the given
	 * name, false otherwise.
	 * 
	 * @param name
	 * @return
	 */
	public boolean hasValueResolver(String name) {
		return hasElement(name);
	}
	
	/**
	 * Register a new {@link ValueResolver} under the name
	 * {@link ValueResolver#name()}
	 * 
	 * @param valueResolver
	 */
	public void register(ValueResolver valueResolver) {
		add(valueResolver.name(), valueResolver);
	}
	
	
	/**
	 * Register the system built-in {@link ValueResolver}s.
	 * 
	 * @param registry
	 */
	private static void initializeSystemValueResolvers(ValueResolverRegistry registry) {
		registry.register(new ValueResolver.DefaultValueResolver());				// DEFAULT
		registry.register(new ValueResolver.DefaultNoErrorValueResolver());			// DEFAULT_NOERROR
		registry.register(new ValueResolver.SysdateValueResolver());				// SYSDATE
		registry.register(new ValueResolver.MS_Timestamp2DateValueResolver());		// MS_TIMESTAMP2DATE
		registry.register(new ValueResolver.MS_Timestamp2DateTZValueResolver());	// MS_TIMESTAMP2DATETZ
		registry.register(new ValueResolver.S_Timestamp2DateValueResolver());		// S_TIMESTAMP2DATE
		registry.register(new ValueResolver.DateValueResolver());					// DATE
		registry.register(new ValueResolver.TimeValueResolver());					// TIME
		registry.register(new ValueResolver.DatetimeValueResolver());				// DATETIME
		registry.register(new ValueResolver.ImageValueResolver());					// IMAGE
		registry.register(new ValueResolver.ThumbnailValueResolver());				// THUMBNAIL
		registry.register(new ValueResolver.BigThumbnailValueResolver());			// BIGTHUMBNAIL
		registry.register(new ValueResolver.Double2IntValueResolver());				// DOUBLE2INT
		registry.register(new ValueResolver.LabelValueResolver());					// LABEL
		registry.register(new ValueResolver.URLValueResolver());					// URL
		registry.register(new ValueResolver.HTMLValueResolver());					// HTML
		registry.register(new ValueResolver.LoginLinkValueResolver());				// LOGINLINK
		registry.register(new ValueResolver.Byte2KBValueResolver());				// BYTE2KB
		registry.register(new ValueResolver.Byte2MBValueResolver());				// BYTE2MB
		registry.register(new ValueResolver.Byte2GBValueResolver());				// BYTE2GB
		registry.register(new ValueResolver.Byte2TBValueResolver());				// BYTE2TB
		registry.register(new ValueResolver.KByte2MBValueResolver());				// KBYTE2MB
		registry.register(new ValueResolver.KByte2GBValueResolver());				// KBYTE2GB
		registry.register(new ValueResolver.KByte2TBValueResolver());				// KBYTE2TB
		registry.register(new ValueResolver.PercentValueResolver());				// PERCENT
		registry.register(new ValueResolver.PercentNoConvertValueResolver());		// PERCENT_NOCONVERT
		registry.register(new ValueResolver.RoundDoubleValueResolver());			// ROUND_DOUBLE
		registry.register(new ValueResolver.CurrencyUSDValueResolver());			// CURRENCY_USD
		registry.register(new ValueResolver.CurrencyEURValueResolver());			// CURRENCY_EUR
		registry.register(new ValueResolver.CurrencyCNYValueResolver());			// CURRENCY_CNY
		registry.register(new ValueResolver.CommaSeparatedValueResolver());			// COMMA_SEPARATED
	}
}