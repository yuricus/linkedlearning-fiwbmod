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

import org.apache.commons.lang.StringEscapeUtils;

import com.fluidops.ajax.FClientUpdate;
import com.fluidops.ajax.FClientUpdate.Prio;
import com.fluidops.ajax.FEvent;
import com.fluidops.ajax.components.FButton;
import com.fluidops.ajax.components.FContainer;
import com.fluidops.ajax.components.FHTML;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.util.Rand;

/**
 * Component for rendering the FLINT SPARQL interface in an iframe.
 * The actual inserted query can be retrieved via a registered
 * {@link SubmitListener}.
 * 
 * @author anna.gossen, as
 *
 */
public class SparqlEditor extends FContainer {

	
	protected static final String SPARQL_IFRAME_ID = "sparql-iframe";
	protected static final String EVENT_PARAM_NAME = "msg";
	
	protected String query = "";
	protected String submitBtnLabel = "Done";
	/**
	 * A (optional) callback which is invoked once a client submits the query
	 */
	protected SubmitListener submitListener;
	
	private String frameId;
	
	public SparqlEditor(String id) {
		this(id, "");
	}

	public SparqlEditor(String id, String query) {
		super(id);
		this.query = query;
		initialize();
	}
	
	private void initialize() {
		
		frameId = "sparql-iframe-" +Rand.getIncrementalFluidUUID();
		FHTML editor = new FHTML("sparql",
				"<iframe id='" + frameId + "' frameborder='0' width='100%' height='450px' src='"
				+ EndpointImpl.api().getRequestMapper().getContextPath() + "/sparqleditor' " 
				+ "onload=\"var iframe = document.getElementById('" + frameId + "');" 
						+ " var flint = iframe.contentWindow.flintEditor;"
						+ " flint.getEditor().getCodeEditor().setValue('"+ StringEscapeUtils.escapeJavaScript(StringEscapeUtils.escapeHtml(query)) + "');"
						+ "\">" +
				"</iframe>");
		add(editor);

		FButton submit = new FButton("update", submitBtnLabel)
		{
			@Override
			public void onClick()
			{
				addClientUpdate(new FClientUpdate(
						Prio.VERYBEGINNING,
								  "var frame = document.getElementById('" + frameId + "');"
								+ " frame.contentWindow.flintEditor.getEditor().getCodeEditor().save(); "
								+ " var query = frame.contentWindow.document.getElementById('flint-code').value;"
								+ " catchPostEventIdEncode('"+SparqlEditor.this.getId()+"',9, query,'"+EVENT_PARAM_NAME+"');"));
			}
		};
		add(submit, "floatLeft");		

	}
	
	/**
	 * @param submitBtnLabel the submitBtnLabel to set
	 */
	public void setSubmitBtnLabel(String submitBtnLabel) {
		this.submitBtnLabel = submitBtnLabel;
	}

	
	/**
	 * Set a custom {@link SubmitListener} which is invoked once the
	 * user clicks the submit button.
	 * 
	 * @param submitListener the submitListener to set
	 */
	public void setSubmitListener(SubmitListener submitListener) {
		this.submitListener = submitListener;
	}


	@Override
	public void handleClientSideEvent(FEvent event) {
		
		// callback when the user clicked on the "Done" button
		query = event.getPostParameter(EVENT_PARAM_NAME);
		
		if (submitListener!=null)
			submitListener.onSubmit(query);		
	}
	
	
	/**
	 * Interface to provide custom functionality once a user
	 * has submitted a query using the button.
	 * 
	 * @author as
	 */
	public static interface SubmitListener {
		public void onSubmit(String query);
	}
	
}
