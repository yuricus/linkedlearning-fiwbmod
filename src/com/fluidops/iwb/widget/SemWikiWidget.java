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

package com.fluidops.iwb.widget;

import java.util.List;

import org.openrdf.model.URI;

import com.fluidops.ajax.components.FComponent;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.ui.editor.LazyEditTabComponentHolder;
import com.fluidops.iwb.ui.editor.SemWiki;
import com.fluidops.iwb.ui.editor.SemWiki.WikiTab;
import com.fluidops.iwb.widget.WidgetEmbeddingError.ErrorType;
import com.fluidops.iwb.widget.config.WidgetVoidConfig;
import com.google.common.collect.Lists;

/**
 * Encapsulation of the SemWiki widget. This widget uses the
 * {@link SemWiki} component to render the actual view, i.e.
 * the wiki view and if available the edit and revisions tab.
 * 
 * This widget interprets the HTTP request parameters
 *  - action: one of {view|edit|revisions}
 *  - version: an optional version string (timestamp) representing the revision
 *  - redirectedFrom: an optional URI if this page has been redirected to
 *  
 * @author Uli
 * @author as
 * @see SemWiki
 */
public class SemWikiWidget extends AbstractWidget<WidgetVoidConfig>
{
	   
	@Override
	public FComponent getComponent(String id)
	{
		
		if (!(pc.value instanceof URI))
			return WidgetEmbeddingError.getErrorLabel(id, ErrorType.GENERIC, "SemWikiWidget can only be used for URI resources.");
		
		String tab = pc.getRequestParameter("action");
		String version = pc.getRequestParameter("version");
		String redirectedFrom = pc.getRequestParameter("redirectedFrom");

		SemWiki sw = new SemWiki(id, (URI)pc.value, pc.repository, version);
		if (tab!=null) {
			if (tab.equals("edit"))
				sw.setActiveTab(WikiTab.EDIT);
			else if (tab.equals("revisions"))
				sw.setActiveTab(WikiTab.REVISIONS);
		}
		if (redirectedFrom!=null) {
			// use the redirectedFrom request parameter
			sw.setRedirectedFrom( EndpointImpl.api().getNamespaceService().guessURI(redirectedFrom) );
		}

		return sw;
	}

	@Override
	public Class<WidgetVoidConfig> getConfigClass()
	{
		return WidgetVoidConfig.class;
	}

	@Override
	public String getTitle()
	{
		return "Semantic Wiki";
	}

	@Override
	public List<String> jsURLs() {
		
		List<String> res = Lists.newArrayList();
		
		// add JS of semwiki edit view directly to widgets urls, bug 11476
		res.addAll(LazyEditTabComponentHolder.jsURLsHelper());
		
		return res;
	}	

}
