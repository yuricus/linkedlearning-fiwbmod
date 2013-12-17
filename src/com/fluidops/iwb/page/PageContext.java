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

package com.fluidops.iwb.page;

import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openrdf.model.Value;
import org.openrdf.repository.Repository;

import com.fluidops.ajax.FSession;
import com.fluidops.ajax.components.FPage;
import com.fluidops.iwb.layout.WidgetContainer;
import com.fluidops.iwb.widget.Widget;
import com.fluidops.util.PropertyMap;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

/**
 * main IWB page context (object used by different processing workflow steps)
 * 
 * @author aeb,pha
 */
public class PageContext
{
	/**
	 * Create a child {@link PageContext} for the given argument
	 */
	public static PageContext createChildPageContext(PageContext parent) {
		if (parent==null)
			throw new IllegalArgumentException("Provided parent page context must not be null.");
		PageContext childPageContext = new PageContext();
		childPageContext.repository = parent.repository;
		childPageContext.httpRequest = parent.getRequest();
        childPageContext.value = parent.value;
        childPageContext.contextPath = parent.contextPath;
        return childPageContext;
	}
	
	public PageContext() {
		
	}
	
	public PageContext(Value value, Repository repository) {
		this.value = value;
		this.repository = repository;
	}

	public String title;
	public String contentType = "text/html";
	public String contextPath;
	
	//needs to be set somewhere in the rendering pipeline.
	public boolean mobile = false;
	public FPage page;
	public WidgetContainer container;
	public Collection<Widget<?>> widgets;
	
	public Value value;
	
	public Repository repository;
	
	// Session and HTTP related context state
	public FSession session;
	protected HttpServletRequest httpRequest;
	@SuppressWarnings(value="URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification="Accessed externally")
	public HttpServletResponse httpResponse;
	
	/**
	 * Retrieve the HTTP request from the page context, which is either taken
	 * from the associated page (as this might be more fresh due to AJAX requests)
	 * or (if the page is not available) from this context itself.
	 * 
	 * @return
	 */
	public HttpServletRequest getRequest() {
		if (page!=null)
			return page.request;
		return httpRequest;
	}
	
	public void setRequest(HttpServletRequest request) {
		this.httpRequest = request;
	}
	
	private PropertyMap<String, String[]> cachedRequestParameters;
	
	/**
	 * Returns the request parameters (if any) as a {@link PropertyMap}.
	 *  
	 * @param req
	 * @return
	 */
	public PropertyMap<String, String[]> getRequestParameters() {
		if (cachedRequestParameters!=null)
			return cachedRequestParameters;
		
		// TODO we should consider storing the request parameters in a separate
		// field independent from the request. The reason is that the request
		// might not be up2date at call time of this method (i.e. the webapp
		// might clear fields within the request stub)
		PropertyMap<String, String[]> requestParameters = new PropertyMap<>();
		HttpServletRequest req = getRequest();
		if (req != null) {
			for (Entry<String, String[]> e : req.getParameterMap().entrySet()) {
				requestParameters.put(e.getKey(), e.getValue());
			}
		}
		cachedRequestParameters = requestParameters;
		return requestParameters;
	}
	
	/**
	 * Returns the request parameter for the specified key (if available),
	 * null otherwise.
	 * 
	 * <p>You should only use this method when you are sure the
     * parameter has only one value. If the parameter might have
     * more than one value, use {@link #getRequestParameters()}.
	 * 
	 * @param parameterName
	 * @return
	 */
	public String getRequestParameter(String parameterName) {
		List<String> list = getRequestParameters().getListValue(parameterName);
		if (list==null || list.isEmpty())
			return null;
		return list.get(0);
	}
}
