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

package com.fluidops.iwb.ui.editor;

import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FContainer;
import com.fluidops.ajax.components.FPage;
import com.fluidops.ajax.components.FTabPane2Lazy.ComponentHolder;
import com.fluidops.util.Rand;


/**
 * The abstract {@link ComponentHolder} for any tab component used in 
 * {@link SemWiki}. This implementation is based on a {@link FContainer}
 * which can be lazily initialized using {@link #initializeView(FContainer)}
 * 
 * @author as
 *
 */
public abstract class SemWikiComponentHolder implements ComponentHolder {

	protected final SemWiki semWiki;
	
	private FContainer container;

	protected SemWikiComponentHolder(SemWiki semWiki) {
		this.semWiki = semWiki;
	}

	@Override
	public FComponent getComponent() {
		
		if (container!=null)
			return container;
		
		// create a new container, which on first population initializes
		// its view with the actual components
		container = new FContainer(Rand.getIncrementalFluidUUID()) {
			private boolean initialized=false;
			@Override
			public void populateView() {
				if (!initialized) {
					SemWikiComponentHolder.this.initializeView(this);
					initialized = true;
				}
				super.populateView();
			}				
		};
		
		initializeContainer(container);
		
		return container;
	}
	
	/**
	 * Callback to initialize the content of this lazily loaded view.
	 * The visual content can be added to the given container.
	 * 
	 * This method is only called once the view actually needs to be
	 * rendered. Note in particular that the container is already
	 * registered and thus has a valid {@link FPage}.
	 * 
	 * This initialization method is invoked only once.
	 * 
	 * @param container
	 */
	protected abstract void initializeView(FContainer container);

	/**
	 * The method can be overridden by subclasses to define settings
	 * of the container, e.g. CSS classes. The reason for this
	 * additional callback is that when calling {@link #initializeView(FContainer)}
	 * the outer HTML anchor is already populated (for reason of
	 * lazy loading)
	 * 
	 * @param container
	 */
	protected void initializeContainer(FContainer container) {
		
	}
	
	@Override
	public String[] jsURLs() {
		return null;
	}

	@Override
	public String[] cssURLs() {
		return null;
	}			
}