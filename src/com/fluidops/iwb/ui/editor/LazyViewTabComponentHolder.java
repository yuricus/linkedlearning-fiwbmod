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

import java.util.Set;

import com.fluidops.ajax.components.FContainer;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

/**
 * A {@link SemWikiComponentHolder} for visualizing the actual wiki view tab.
 * This view tab is controlled by the string template {@value #STRINGTEMPLATE}
 * and variables as defined in the template can be used.
 * 
 * @author as
 *
 */
public class LazyViewTabComponentHolder extends SemWikiComponentHolder {


	public static final String STRINGTEMPLATE = "com/fluidops/iwb/ui/templates/SemWikiView";
	
	public static final String RENDERED_VIEW_CONTENT = "renderedViewContent";

	private String[] cachedJSUrls = null;
	
	public LazyViewTabComponentHolder(SemWiki semWiki) {
		super(semWiki);
	}	
	
	@Override
	protected void initializeContainer(FContainer container) {
		container.setClazz("viewContentClazz");
	}

	@Override
	protected void initializeView(FContainer container) {	
		
		String renderedContent = SemWikiUtil.getRenderedViewContent(semWiki.getWikiText(), semWiki.getSubject(), semWiki.getVersion(), container);
		container.setStringTemplateClazz(STRINGTEMPLATE);
		container.setStringTemplateParameter("renderedViewContent", renderedContent);		
	}
	
	@Override
	@SuppressWarnings(value="EI_EXPOSE_REP", justification="Checked")
	public String[] jsURLs() {
		if (cachedJSUrls==null) {
			Set<String> jsUrls = SemWikiUtil.getWidgetsJSUrls(semWiki.getWikiText(), semWiki.getSubject());
			cachedJSUrls = jsUrls.toArray(new String[jsUrls.size()]);
		}			
		return cachedJSUrls;
	}
	
	
}
