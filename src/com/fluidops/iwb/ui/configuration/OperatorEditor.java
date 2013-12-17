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

import java.lang.reflect.Field;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;

import com.fluidops.ajax.FClientUpdate;
import com.fluidops.ajax.components.FButton;
import com.fluidops.ajax.components.FContainer;
import com.fluidops.ajax.components.FDialog;
import com.fluidops.ajax.components.FDialog.Effect;
import com.fluidops.ajax.components.FDialog.Position;
import com.fluidops.ajax.components.FHTML;
import com.fluidops.ajax.components.FImageButton;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.operator.OperatorFactory;
import com.fluidops.iwb.api.operator.OperatorNode;
import com.fluidops.iwb.ui.configuration.SparqlEditor.SubmitListener;
import com.fluidops.util.Rand;
import com.fluidops.util.StringUtil;
import com.google.common.collect.Lists;

/**
 * A special FContainer, containing a button which opens
 * a dialog - the operator form. This class is implemented
 * as container to allow the use for FDialog
 */
public class OperatorEditor extends FContainer {

	/**
	 * 
	 */
	private final FormElementContainer formElementContainer;
	
	/**
	 * The provided user query (not enclosed by $)
	 */
	private String operatorQuery = "";
	private List<String> possibleOptions = Lists.newArrayList();

	public OperatorEditor(FormElementContainer formElementContainer, String id) {
		super(id);
		this.formElementContainer = formElementContainer;
		initialize();
	}

	public void setPresetOperatorString(String operatorString) {
		
		this.operatorQuery = removeEnclosingOperatorSymbol(operatorString);
	}
	
	private String removeEnclosingOperatorSymbol(String serialized) {
		String token = serialized.startsWith("$") ? serialized.substring(1) : serialized;
		token = token.endsWith("$") ? token.substring(0, token.length()-1) : token;
		return token;
	}

	/**
	 * Returns true, if some dynamic operator string has been entered, false
	 * otherwise
	 * 
	 * @return
	 */
	public boolean isDynamicOperatorSpecified() {
		return !StringUtil.isNullOrEmpty(operatorQuery);
	}

	/**
	 * Returns a dynamic {@link OperatorNode} if an operator string has been
	 * entered, null otherwise
	 * 
	 * @return
	 */
	public OperatorNode getOperatorNode() {

		if (isDynamicOperatorSpecified())
			return OperatorFactory
					.textInputToDynamicOperatorNode("$" + operatorQuery + "$");
		return null;
	}

	/**
	 * Initialize the available options that can be used in the SELECT query as
	 * projections.
	 * 
	 * @param configType
	 */
	public void initializeAvailableOptions(Class<?> configType) {
		// initialize the available options from the config type
		for (Field f : ConfigurationFormUtil.getConfigFieldsSorted(configType)) {
			possibleOptions.add(f.getName());
		}
	}

	private void initialize() {

		// TODO: due to the current design of FDialog we require a div for the
		// dialog being rendered inside the page. Here we use a dummy container
		// and refresh the content of this one on each click of the "Op" btn
		final FContainer dialogContainer = new FContainer(
				Rand.getIncrementalFluidUUID());
		add(dialogContainer);

		FButton op_btn = new FImageButton(
				"op",
				EndpointImpl.api().getRequestMapper().getContextPath()
						+ "/images/widget/op.png",
				"Use a dynamic SPARQL SELECT operator to define this configuration option (Advanced)") {
			@Override
			public void onClick() {
				dialogContainer.removeAll();
				FDialog dialog = createOperatorDialog();
				dialogContainer.add(dialog);
				dialogContainer.populateView();
				dialog.show();
			}
		};
		add(op_btn);
	}

	private FDialog createOperatorDialog() {

		final FDialog dialog = new FDialog("opdlg"
				+ Rand.getIncrementalFluidUUID(), "Operator input form",
				Position.CENTER, Effect.SCALE);
		
		dialog.setMinHeight(550);
		dialog.setMinWidth(1000);

		FHTML description = new FHTML(
				Rand.getIncrementalFluidUUID(),
				"<div style=\"font-family:verdana;font-size: 0.9em;\"><p>Use the following form to provide a dynamic SPARQL SELECT operator for populating the configuration."
				+ "In the SELECT query you may use one or more of the following configuration options as projections: </p>" 
				+ "<p><i>" + (possibleOptions.isEmpty() ? "primitive type - leftmost projection is used": StringUtil.toString(possibleOptions, ", ") ) + "</i></p>"
				+ "</div>");
		dialog.add(description);

		// add text area for operator
		final SparqlEditor spi = new SparqlEditor("spi", operatorQuery);
		dialog.add(spi);

		spi.setSubmitListener(new SubmitListener() {
			
			@Override
			public void onSubmit(String query) {
				
				// empty query: clear the operator
				// TODO think about this
				if (query.equals("")) {
					operatorQuery = "";
					dialog.hide();
					OperatorEditor.this.formElementContainer.reinitialize();
					formElementContainer.populateView();
					return;
				}
				
				// validate the input, and inform the user on errors
				try {
					validateInput(query);
				} catch (RuntimeException e) {
					getPage().addClientUpdate(
							new FClientUpdate("alert('"
									+ StringEscapeUtils.escapeJavaScript(e
											.getMessage()) + "');"));
					return;
				}

				// refresh the form with the valid value
				operatorQuery = query;
				dialog.hide();
				OperatorEditor.this.formElementContainer
						.reinitializeForDynamicOperator();
				formElementContainer.populateView();
				
			}
		});
	
		// show a confirmation question, if some input has been made already in the form independent from the operator itself
		// TODO activate confirmation question once problem described in bug 11003 is fixed
//		if (StringUtil.isNullOrEmpty(operatorString) && toOperatorNode()!=null)
//			btn_done.setConfirmationQuestion("This operation will clear all manually made config settings. Do you want to continue?");

		FButton btn_clear = new FButton(Rand.getIncrementalFluidUUID(),
				"Clear Operator") {
			@Override
			public void onClick() {
				operatorQuery = "";
				dialog.hide();
				OperatorEditor.this.formElementContainer.reinitialize();
				formElementContainer.populateView();
			}
		};
		// TODO activate confirmation question once problem described in bug 11003 is fixed
//		btn_clear.setConfirmationQuestion("This operation will clear the specified dynamic operator. Do you want to continue?");
		dialog.add(btn_clear, "floatRight");

		return dialog;
	}

	/**
	 * Validate the given input according to dynamic SPARQL operators. Throws
	 * meaningful {@link IllegalArgumentException} in case of any errors
	 * 
	 * @param input
	 */
	private void validateInput(String input) throws IllegalArgumentException {

		if (StringUtil.isNullOrEmpty(input))
			throw new IllegalArgumentException(
					"Please provide valid non-empty input, or use the cancel operation.");

		if (!input.toUpperCase().contains("SELECT"))
			throw new IllegalArgumentException(
					"Dynamic operator must be a valid SPARQL SELECT query.");

		// try actually parsing
		OperatorFactory.textInputToDynamicOperatorNode("$" + input + "$");
	}
}