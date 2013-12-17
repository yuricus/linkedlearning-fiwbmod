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

package com.fluidops.iwb.ajax;

import java.util.Map;

import org.openrdf.model.Value;

import com.fluidops.ajax.helper.Highlight;
import com.fluidops.iwb.api.ImageResolver;
import com.fluidops.iwb.api.ReadDataManager;

/**
 * Custom FValue, where occurrences of matching terms in the label are highlighted. 
 * 
 * @author andriy.nikolov
 *
 */
public class FValueWithMatchHighlight extends FValue {
	
	// private List<String> queryTokens;
	private Highlight highlighter = null;
	
	/**
	 * @param id
	 * @param value
	 */
	public FValueWithMatchHighlight(String id, Value value, Highlight highlight) {
		super(id, value);
		this.highlighter = highlight;
	}

	/**
	 * @param id
	 * @param value
	 * @param name
	 * @param dm
	 * @param valueCfg
	 */
	public FValueWithMatchHighlight(String id, Value value, String name,
			ReadDataManager dm, ValueConfig valueCfg, String query, Highlight highlight) {
		super(id, value, name, dm, valueCfg);
		this.highlighter = highlight;
	}

	/**
	 * @param id
	 * @param value
	 * @param name
	 * @param imageResolver
	 * @param variableResolver
	 * @param dm
	 * @param showLabels
	 */
	public FValueWithMatchHighlight(String id, Value value, String name,
			ImageResolver imageResolver, Map<String, String> variableResolver,
			ReadDataManager dm, boolean showLabels, Highlight highlight) {
		super(id, value, name, imageResolver, variableResolver, dm, showLabels);
		this.highlighter = highlight;
	}

	@Override
	protected String processLabel(String label) {
		if(highlighter!=null) {
			return highlighter.perform(label);
		} else {
			return label;
		}
	}

}
