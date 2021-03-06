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

package com.fluidops.iwb.util;

/**
 * A configurable is something that can be configured. Currently Provider and WidgetConfig are the implementing classes.
 * The actual purpose is to offer a unified configuration UI (like the ConfigurationForm for Widgets and Providers) for all kinds of configurable things 
 * @author ango
 *
 */
public interface Configurable
{
	/**
	 * Get the class of the configurable. In case of the widget config it's in the widget field of the config. 
	 * In case of providers it's the class of the configurable
	 */
	Class<?> getConfigurablesClass();
	
	/**
	 * Get the class of the configurable's config. It's the configClass of the instantiated widget or provider
	 */
	Class<?> getConfigurationClass();
	
}
