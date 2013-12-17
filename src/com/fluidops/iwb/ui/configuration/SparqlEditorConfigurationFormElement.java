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

import com.fluidops.ajax.components.FButton;
import com.fluidops.ajax.components.FContainer;
import com.fluidops.ajax.components.FDialog;
import com.fluidops.ajax.components.FDialog.Effect;
import com.fluidops.ajax.components.FDialog.Position;
import com.fluidops.ajax.components.FHTML;
import com.fluidops.ajax.components.FTextArea;
import com.fluidops.iwb.api.operator.OperatorFactory;
import com.fluidops.iwb.api.operator.OperatorNode;
import com.fluidops.iwb.ui.configuration.SparqlEditor.SubmitListener;
import com.fluidops.util.Rand;
import com.fluidops.util.StringUtil;

/**
 * A special configuration form element containing a textarea 
 * and a button to trigger a dialog window with a sparql editor 
 * to enable SPARQL syntax validation and auto-suggestion 
 * @author ango
 */
public class SparqlEditorConfigurationFormElement extends
		AbstractConfigurationFormElement<FContainer>
{

	protected Class<?> targetType;

	@Override
	public OperatorNode toOperatorNode()
	{
		if (StringUtil.isNullOrEmpty(component.getValue()))
			return null;
		return OperatorFactory.textInputToOperatorNode(component.getValue(),
				targetType);
	}

	@Override
	protected FContainer createComponent(FormElementConfig formElementConfig)
	{
		targetType = formElementConfig.targetType;

		SparqlEditorDialog cont = new SparqlEditorDialog(Rand.getIncrementalFluidUUID(),
				OperatorFactory.operatorToText(formElementConfig.presetValue,
						formElementConfig.targetType));

		return cont;
	}

	private static class SparqlEditorDialog extends FContainer
	{
		private FTextArea ta;

		
		public SparqlEditorDialog(String id, String content)
		{
			super(id);
			initialize(content);
		}
		
		private void initialize(String content) {
			
			// TODO: due to the current design of FDialog we require a div for the
			// dialog being rendered inside the page. Here we use a dummy container
			// and refresh the content of this one on each click of the "Op" btn
			final FContainer dialogContainer = new FContainer(
					Rand.getIncrementalFluidUUID());
			add(dialogContainer);

            FButton op_btn = new FButton("sparqlInterface") {
                @Override
                public void onClick() {
                    dialogContainer.removeAll();
                    FDialog dialog = createSparqlDialog();
                    dialogContainer.add(dialog);
                    dialogContainer.populateView();
                    dialog.show();
                }
            };
            op_btn.appendClazz("sparqlInterfaceButton");

			ta = new FTextArea(Rand.getIncrementalFluidUUID());
			ta.setValue(content);
			add(ta);

			add(op_btn, "floatLeft");
			add(new FHTML(Rand.getIncrementalFluidUUID(), "<span style='font-size: 0.8em; color: #6C6C6C; padding-left: 5px;'>Use the advanced SPARQL interface</span>"));
		}

		@Override
		public String getValue()
		{
			return ta.getValue();
		}				

		@Override
		public Object returnValues() {
			return ta.returnValues();
		}

		/**
		 * creates a dialog window with an iframe containing the sparql editor
		 * and a button to paste the query into the textarea
		 * 
		 * @return
		 */
		public FDialog createSparqlDialog()
		{
			final FDialog dialog = new FDialog(Rand.getIncrementalFluidUUID(),
					"SPARQL Editor", Position.CENTER, Effect.SCALE);

			dialog.setMinHeight(550);
			dialog.setMinWidth(1000);

			SparqlEditor spi = new SparqlEditor("sparql" + Rand.getIncrementalFluidUUID(), ta.getValue());
			spi.setSubmitListener(new SubmitListener() {				
				@Override
				public void onSubmit(String query) {
					ta.setValue(query);	
					dialog.hide();
				}
			});
			dialog.add(spi);
				
			add(dialog);

			return dialog;
		}
	}

}