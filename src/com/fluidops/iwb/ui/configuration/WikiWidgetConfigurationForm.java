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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.openrdf.model.URI;

import com.fluidops.ajax.FClientUpdate;
import com.fluidops.ajax.FClientUpdate.Prio;
import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FPopupWindow;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.operator.Operator;
import com.fluidops.iwb.api.operator.OperatorFactory;
import com.fluidops.iwb.api.operator.OperatorNode;
import com.fluidops.iwb.widget.Widget;
import com.fluidops.iwb.widget.WidgetConfig;
import com.fluidops.iwb.wiki.FluidWikiModel;
import com.fluidops.iwb.wiki.FluidWikiModel.TemplateResolver;
import com.fluidops.util.Rand;
import com.fluidops.util.StringUtil;

/**
 * This class implements the configuration form as used for widget 
 * configuration in the wiki editor. It allows to configure widgets
 * and to serialize the configuration as widget definition in the
 * wiki page.
 * 
 * @author as
 *
 */
public class WikiWidgetConfigurationForm extends WidgetConfigurationFormBase {

	private static Logger logger = Logger.getLogger(WikiWidgetConfigurationForm.class);
	
	/**
	 * Populates and shows the popup with a {@link WikiWidgetConfigurationForm} that
	 * is filled from the given editor parameters. 
	 * 
	 * @param popup
	 * @param title
	 * @param widgetConfig the widget config or null (for an empty form)
	 */
	public static void showWidgetConfigurationForm(FPopupWindow popup, String title, JSONObject editorParameters) {
		WikiWidgetConfigurationForm form = new WikiWidgetConfigurationForm(Rand.getIncrementalFluidUUID(), editorParameters);
		ConfigurationFormUtil.showConfigurationFormInPopup(popup, title, form);
	}

	private final JSONObject editorParameters;
    
	private WikiWidgetConfigurationForm(String id, JSONObject editorParameters) {
		super(id, widgetConfigFromEditorParameters(editorParameters));
		this.editorParameters = editorParameters;
	}

	@Override
	protected void submitData(OperatorNode data) {

		String serializedWidget = OperatorFactory.toWidgetWikiSnippet(data, getWidgetName());	
		
		// snippet from old form:
		((FPopupWindow) getParent()).hide();
        addClientUpdate( new FClientUpdate( Prio.END, "jQuery(document).ready(function($){" +
                "insertWidgetConfig('"+StringEscapeUtils.escapeJavaScript(serializedWidget)+"', "+editorParameters+");});" ) );
	}

	
	
	/**
	 * Computes the {@link WidgetConfig} from the passed editorParameters (i.e. the
	 * selected wiki text). If the selected wiki text contains a valid widget definition,
	 * the corresponding {@link WidgetConfig} is returned, and can be used in the 
	 * super class {@link WidgetConfigurationFormBase} for rendering. Note that this 
	 * method returns null, if the selection does not contain a valid widget, resulting
	 * in an empty default {@link WidgetConfigurationFormBase}.
     * 
     * @param editorParameters parameters from the wiki text area editor, containing 
     * 				the actual position of the mouse and the text selection
     * @return
     * @throws JSONException 
     */
    private static WidgetConfig widgetConfigFromEditorParameters(JSONObject editorParameters) 
    {
    	String wikiSelection = null;
    	int caretPosition = 0;
    	try
    	{
    		if(!editorParameters.isNull("selection"))
    		{
    			wikiSelection = editorParameters.getString("selection");
    			
    			if(!StringUtil.isEmpty(wikiSelection) && wikiSelection.contains("#widget"))
    				return parseWikiText(wikiSelection);
    			return null;
    		}
    		else  if(!editorParameters.isNull("editorContent"))        
    		{
    			wikiSelection = editorParameters.getString("editorContent");
    			caretPosition = editorParameters.getInt("caretPosition");
    		}
    		else
    			return null;

    		int widgetStart = findWidgetConfigurationStart(wikiSelection, caretPosition+2);
    		int widgetEnd = findWidgetConfigurationEnd(wikiSelection, widgetStart);

    		// check if a configuration could be detected 
    		// and if the cursor is set in the scope of the detected widget configuration
    		if(widgetStart == -1 || widgetEnd == -1 || caretPosition >= widgetEnd)
    			return null;
    		else
    		{   		
    			String widgetConfigurationString = wikiSelection.substring(widgetStart, widgetEnd);
    			//modify editorParameters to enable javascript to replace the old widget configuration 
    			// by the new one (set the selection to replace, the correct start index and remove the editor content to avoid overhead)
    			editorParameters.put("caretPosition", widgetStart);
    			editorParameters.put("selection", widgetConfigurationString);
    			editorParameters.remove("editorContent");

    			return parseWikiText(widgetConfigurationString);
    		}
    	}
    	catch (JSONException e) {
    		throw new RuntimeException(e);
    	}
    }
    
    /**
     * Parse wiki text (i.e. a selection) and try to find widget definitions
     * within the text using the BLIKI template parser. If a single correct
     * widget was found, the corresponding {@link WidgetConfig} is returned.
     * If the selection contained multiple widget definitions, an appropriate
     * exception is thrown. In case the selection does not contain any valid
     * widget definition, <null> is returned.
     * 
     * @param wikitext
     * @return
     */
    private static WidgetConfig parseWikiText(String wikitext)  {
    	
    	final List<WidgetConfig> res = new ArrayList<WidgetConfig>();
    	final List<Exception> errors = new ArrayList<Exception>();
    	
    	FluidWikiModel wikiModel = new FluidWikiModel(null, null);
    	wikiModel.setReplaceIWBMagicWords(false);		// do not replace IWB magic words while parsing
        wikiModel.addTemplateResolver( new TemplateResolver()  {
		            @SuppressWarnings("unchecked")
					public String resolveTemplate(String namespace,
		                    String templateName, Map<String, String> templateParameters, URI page, FComponent parent)
		            {                
		                if ( templateName.startsWith("#widget"))
		                {
		                	try {
			                	String clazz = templateName.substring(templateName.lastIndexOf(":")+1).trim();
		            			clazz = EndpointImpl.api().getWidgetService().getWidgetClass( clazz );
		            			
		                        if (clazz == null)
		                            return null;
		                        
								Class<? extends Widget<?>> widgetClass = (Class<? extends Widget<?>>) Class.forName( clazz );
								
								Operator mapping;
								if(templateParameters.size()==0) {
								    mapping = Operator.createNoop();
								}
								else if(templateParameters.get( "1" )!=null) {
								    String mappingString = templateParameters.get( "1" );
								    mapping = Operator.parse(mappingString);								  			    
								}
								else {	
									// named parameters are used	
									mapping = Operator.parseStruct(templateParameters, page);
								}
								    
								res.add( new WidgetConfig(null, widgetClass, null, mapping, false));
		                	} catch (Exception e) {
		                		errors.add(e);
		                	}
		                }
		                
		                return null;
		            }
        		});     
                
        wikiModel.setUp();
        wikiModel.parseTemplates(wikitext);
        
        if (errors.size()!=0) {
        	logger.info("Errors occured while parsing wiki text:", errors.get(0));
        	throw new RuntimeException(errors.get(0));
        }
        
        if (res.size()==0)
        	return null;		// selection does not contain a valid widget definition
        
        if (res.size()>1)
        	throw new RuntimeException("Selection contained multiple widget definitions, while only one was expected");
        
        return res.get(0);
    }
	
	/**
	 * Searches for the start index of a widget configuration in a string,
	 * having a string and the pointer position that is supposed to be in the
	 * scope of the widget configuration. Returns -1 if nothing found
	 * 
	 * @param caretPosition
	 * @param searchString
	 * @return
	 */
	private static int findWidgetConfigurationStart(String searchString, int caretPosition) {
		StringBuilder sb = new StringBuilder(searchString);

		if (caretPosition == 0 || caretPosition > searchString.length())
			return -1;

		String textBeforeCaretPosition = searchString.substring(0, caretPosition);

		for (int i = 0; i < textBeforeCaretPosition.length(); i++) {
			String search = sb.substring(caretPosition - i);

			if (search.startsWith("#widget")) {
				textBeforeCaretPosition = searchString.substring(0,	caretPosition - i);

				for (int j = 2; j <= textBeforeCaretPosition.length(); j++) {
					search = sb.substring(caretPosition - i - j);

					if (search.startsWith("{{"))
						return caretPosition - i - j;
				}
				return -1;
			}
		}
		return -1;
	}
	
	/**
	 * Searches for the end of a widget configuration in a string having a
	 * string and the start index of the widget configuration. Returns -1 if
	 * nothing found
	 * 
	 * @param searchString
	 * @param widgetStart
	 */
	private static int findWidgetConfigurationEnd(String searchString, int widgetStart) {

		if (widgetStart < 0 || widgetStart > searchString.length())
			return -1;

		StringBuilder sb = new StringBuilder(searchString.substring(widgetStart));

		int numberOfBrackets = 0;

		for (int i = 0; i < sb.length(); i++) {
			if (sb.charAt(i) == '{') {
				numberOfBrackets++;
			} else if (sb.charAt(i) == '}') {
				numberOfBrackets--;
			}

			if (numberOfBrackets == 0) {
				return widgetStart + i + 1;
			}
		}
		return -1;
	}
}
