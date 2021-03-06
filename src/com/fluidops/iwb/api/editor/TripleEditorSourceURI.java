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

package com.fluidops.iwb.api.editor;

import org.openrdf.model.URI;
import org.openrdf.query.QueryEvaluationException;

public interface TripleEditorSourceURI
{	
	/**
	 * Initialize a URI triple editor source. Must be called prior to
	 * accessing any internal data.
	 * 
	 * @param literal the subject for which to extract information
	 * @param initialValuesDisplayed number of values displayed in initial view
	 * @param includeInverseProperties whether to show inverse properties or not
	 * 
	 * @throws QueryEvaluationException
	 */
	public void initialize(URI uri, int initialValuesDisplayed, boolean includeInverseProperties) throws QueryEvaluationException;
}