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

package com.fluidops.iwb.ui.configuration;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.fluidops.ajax.components.FPopupWindow;
import com.fluidops.iwb.model.ParameterConfigDoc;
import com.google.common.collect.Lists;

/**
 * Convenience classes for use in {@link ConfigurationFormBase} instances
 * 
 * @author as
 *
 */
public class ConfigurationFormUtil {

	
	/**
	 * Returns the configuration fields of the given config class sorted. 
	 * Sorting is performed according to the following order
	 * 
	 * 1) required parameters
	 * 2) non-required config class
	 * 3) non-required super class(es) of config class
	 * 
	 * @param configClass
	 * @return
	 */
	public static List<Field> getConfigFieldsSorted(Class<?> configClass) {
		List<Field> fields = Lists.newArrayList();
		getConfigFields(configClass, fields);
		Collections.sort(fields, new Comparator<Field>() {
			@Override
			public int compare(Field a, Field b)
			{
				ParameterConfigDoc pa = a.getAnnotation(ParameterConfigDoc.class);
				ParameterConfigDoc pb = b.getAnnotation(ParameterConfigDoc.class);
				if (pa==null)
					return 1;
				if (pb==null)
					return -1;
				if (pa.required()) {
					if (!pb.required())
						return -1;
				} else if (pb.required())
					return 1;
				return 0;				
			}
		});
		return fields;
	}
		
			
	/**
	 * Return all 
	 * @param configClass
	 * @param allFields
	 * @return
	 */
	private static List<Field> getConfigFields(Class<?> configClass, List<Field> allFields) {

		if(configClass != null && !configClass.equals(Object.class))
		{
			Field[] fields = configClass.getDeclaredFields();
			if(fields != null)	
				for (Field f: fields)
					if (f.getAnnotation(ParameterConfigDoc.class)!=null && !Modifier.isStatic(f.getModifiers()))
						allFields.add(f);

			getConfigFields(configClass.getSuperclass(), allFields);
		}
		
		return allFields;
	}
	
	/**
	 * Populates and shows a popup with the given implementation of the
	 * {@link ConfigurationFormBase}
	 * 
	 * @param popup
	 * @param title the title of the popup
	 * @param configurationForm the configuration form to show
	 */
	public static void showConfigurationFormInPopup(FPopupWindow popup, String title, ConfigurationFormBase configurationForm) {
		popup.removeAll();
		popup.setTitle(title);
		popup.setTop("60px");
		popup.setWidth("60%");
		popup.setLeft("20%");
		popup.setClazz("ConfigurationForm");
		popup.setDraggable(true);
		popup.add(configurationForm);
		popup.populateAndShow();
	}
}
