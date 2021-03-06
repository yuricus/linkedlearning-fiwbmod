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

package com.fluidops.iwb.layout;

import java.util.Collections;
import java.util.List;

import com.fluidops.ajax.components.FContainer;
import com.fluidops.iwb.page.PageContext;
import com.fluidops.iwb.widget.AbstractWidget;

/**
 * container for a single (full screen) widget
 * 
 * @author aeb
 */
public class SingleWidgetContainer implements WidgetContainer
{
	@Override
	public void add(AbstractWidget<?> widget, String id)
	{
		container.add( widget.getComponentUAE(id) );
	}

	FContainer container = new FContainer( "c" );
	
	@Override
	public FContainer getContainer()
	{
		return container;
	}

	@Override
	public void postRegistration(PageContext pc)
	{
	}

	@Override
	public List<String> jsUrls() {
		// TODO should return JSUrls of visible widgets
		return Collections.emptyList();
	}
}
