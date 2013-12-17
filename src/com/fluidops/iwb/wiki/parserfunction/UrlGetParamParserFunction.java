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

package com.fluidops.iwb.wiki.parserfunction;

import info.bliki.wiki.model.IWikiModel;
import info.bliki.wiki.template.AbstractTemplateFunction;

import java.io.IOException;
import java.util.List;

import com.fluidops.iwb.page.PageContext;


/**
 * The #urlget parser function renders a given HTTP get parameter into a wikipage.
 * {{#urlget:parameter-name|default-value}}
 * 
 * @author johannes.trame
 *
 */
public class UrlGetParamParserFunction extends AbstractTemplateFunction implements PageContextAwareParserFunction {
	
	private PageContext pc;

	@Override
	public String parseFunction(List<String> parts, IWikiModel model,
			char[] src, int beginIndex, int endIndex, boolean isSubst)
			throws IOException {

		if (parts.size()==0)
			return null;
		
		String param = parts.get(0).trim();
		String value= pc.getRequestParameter(param);
		
		//if the request parameter does not exist, return the default value
		if(value==null && parts.size()>1)
			return parts.get(1).trim();
		
		return (value == null) ? null : value.trim();
	}
	
	@Override
	public void setPageContext(PageContext pc) {
		this.pc = pc;		
	}

	@Override
	public String getFunctionName() {
		return "#urlget";
	}	
}
