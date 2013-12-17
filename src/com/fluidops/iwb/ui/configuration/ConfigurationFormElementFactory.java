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

import org.openrdf.model.Value;

import com.fluidops.ajax.components.FComponent;
import com.fluidops.iwb.model.ParameterConfigDoc;
import com.fluidops.iwb.model.ParameterConfigDoc.Type;

/**
 * Factory to instantiate new {@link ConfigurationFormElement} for
 * particular {@link FormElementConfig} instances.
 * 
 * @author as
 *
 */
public class ConfigurationFormElementFactory {

	
	/**
	 * Return the appropriate {@link ConfigurationFormElement} based on the {@link Type} defined 
	 * in {@link FormElementConfig#parameterConfig}.
	 * 
	 * For the following targetTypes the default rendering component is changed (if it was set
	 * to the default, i.e., {@link Type#SIMPLE}.
	 * 
	 *  - boolean => {@link Type#DROPDOWN}
	 *  - enum => {@link Type#DROPDOWN}
	 *  
	 * @param fCfg
	 * @return
	 */
	public static ConfigurationFormElement<? extends FComponent> getFormElementForConfig(FormElementConfig fCfg) {
		
		ParameterConfigDoc paramConfig = fCfg.parameterConfig;
		
		Type selectedType = paramConfig.type();
		
		// set appropriate defaults for certain elements
		if (selectedType==Type.SIMPLE) {
			if(fCfg.targetType.equals(Boolean.class) || fCfg.targetType.equals(boolean.class) || fCfg.targetType.isEnum())
				selectedType = Type.DROPDOWN;
		}
		
			
		switch (selectedType) {
		case SIMPLE: 	   	return new TextInputConfigurationFormElement();
		case CONFIG:		return new ConfigInputConfigurationFormElement();
		case LIST:			return new ListInputConfigurationFormElement();
		case DROPDOWN:
			if (Value.class.isAssignableFrom(fCfg.targetType))
				return new ValueDropdownConfigurationFormElement();
			else
				return new DropdownConfigurationFormElement();
		case TEXTAREA:		return new TextareaConfigurationFormElement();
		case PASSWORD:		return new PasswordSimpleConfigurationFormElement();
		case CHECKBOX:		return new CheckboxConfigurationFormElement();
		case SPARQLEDITOR:  return new SparqlEditorConfigurationFormElement();
		default:		throw new RuntimeException("Type not yet implemented: " + paramConfig.type());
		}
	}
	
	
	
	public static ConfigurationFormElement<? extends FComponent> getFormElementForType(Class<?> type) {
		
		// TODO null checks
		
		if (type.equals(String.class))
			return new TextInputConfigurationFormElement();		
		
		// render booleans and enums as dropdown
		if(type.equals(Boolean.class) || type.equals(boolean.class) || type.isEnum())
			return new DropdownConfigurationFormElement();
		
		if (type.isPrimitive() || type.equals(Long.class) || type.equals(Integer.class) || type.equals(Double.class))
			return new TextInputConfigurationFormElement();	
		
		if (Value.class.isAssignableFrom(type))
			return new TextInputConfigurationFormElement();
		
		if (Object.class.equals(type))
			return new TextInputConfigurationFormElement();
		
		return new SubformInputConfigurationFormElement();
	}
}
