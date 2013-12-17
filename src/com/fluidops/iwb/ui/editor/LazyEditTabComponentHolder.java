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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.impl.ValueFactoryImpl;

import com.fluidops.ajax.FClientUpdate;
import com.fluidops.ajax.FClientUpdate.Prio;
import com.fluidops.ajax.FEvent;
import com.fluidops.ajax.FEventType;
import com.fluidops.ajax.components.FButton;
import com.fluidops.ajax.components.FComboBox;
import com.fluidops.ajax.components.FContainer;
import com.fluidops.ajax.components.FTextArea;
import com.fluidops.ajax.components.FTextInput2;
import com.fluidops.ajax.helper.JSONWrapper;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.NamespaceService;
import com.fluidops.iwb.api.ReadDataManager;
import com.fluidops.iwb.api.ReadWriteDataManagerImpl;
import com.fluidops.iwb.api.RequestMapperImpl;
import com.fluidops.iwb.ui.configuration.WikiWidgetConfigurationForm;
import com.fluidops.iwb.user.UserManager.ValueAccessLevel;
import com.fluidops.iwb.wiki.Wikimedia;
import com.fluidops.util.Rand;
import com.fluidops.util.StringUtil;
import com.google.common.collect.Lists;

/**
 * A {@link SemWikiComponentHolder} for the edit tab of the {@link SemWiki}. The
 * edit tab is in general shown dependent on the ACL configuration.
 * 
 * The rendering of this tab is controlled by the string template
 * {@value #STRINGTEMPLATE} and can use the template variables
 * as defined in the provided default implementation.
 * 
 * @author as
 *
 */
public class LazyEditTabComponentHolder extends SemWikiComponentHolder {
	
	private static final String STRINGTEMPLATE = "com/fluidops/iwb/ui/templates/SemWikiEdit";
	
	/**
	 * The threshold specifying how many template links can be displayed as anchor links separated by |
	 * If the page contains more templates, they are displayed as items in a dropdown combo box. 
	 */
	private static final int MAX_LINKS_FOR_TEMPLATES = 4;
	
	/* components to be accessed in the string template */
	
	public static final String TEMPLATE_PAGE_LINKS = "templatePageLinks";
	public static final String TEXTAREA_ID = "textarea";
	public static final String COMMENT_ID = "comment";
	public static final String PREVIEW_ID = "preview";
	public static final String PREVIEW_BUTTON_ID = "previewButton";
	public static final String CANCEL_BUTTON_ID = "cancelButton";
	public static final String SUBMIT_BUTTON_ID = "submitButton";
	
	private static Logger logger = Logger.getLogger(LazyEditTabComponentHolder.class);

	private FTextInput2 commentTxt;
	private FTextArea editTextArea;
	private PreviewContainer previewContainer;
	
	public LazyEditTabComponentHolder(SemWiki semWiki) {
		super(semWiki);
	}
	
	@Override
	protected void initializeView(FContainer container) {
		
		
		container.setStringTemplateClazz(STRINGTEMPLATE);
		
		// add list of type includes parameter for rendering
		container.setStringTemplateParameter(TEMPLATE_PAGE_LINKS, displayTemplateLinks(container, computeTemplatePageLinks()));
		
		// textarea
		container.add(createTextArea());
		
		// comment text input
		container.add(createCommentInput());
		
		// preview + preview button
		container.add(createPreview());
		container.add(createPreviewButton());
		
		// cancel button
		container.add(createCancelButton());
		
		// submit button
		container.add(createSubmitButton());

	}
	
	private FTextArea createTextArea() {
		editTextArea = new FTextArea(TEXTAREA_ID)
        {
            @Override
            public void populateView() {

				if (getPage() != null)
					addClientUpdate(new FClientUpdate(	Prio.END, "getDomElementById('"	+ getComponentid() + "').style.height=(document.body.clientHeight*0.7)+'px';"));

				// make the textarea a markitup-editor. editorSettings are in the jquery.markitup.js
				addClientUpdate(new FClientUpdate(Prio.END,
						"jQuery(document).ready(function($){" + "$('#" + this.getComponentid()	+ "').markItUp(editorSettings);});"));
				                
                super.populateView();               
            }
            
            @Override
            public void handleClientSideEvent(FEvent event)
            {
            	// 1. check if save action is triggered from wiki editor
            	String newContent = event.getPostParameter("saveWiki");
            	if(StringUtil.isNotNullNorEmpty(newContent))
            	{
            		if (semWiki.saveWiki(commentTxt.returnValues().toString(), newContent))
            			semWiki.reloadWikiPage();
            		return;
            	}
            	
            	// 2. check if configuration form shall be shown
        		String paramString = event.getPostParameter("editorParameters");
        		if(StringUtil.isNullOrEmpty(paramString))return;

        		JSONObject editorParameters;
        		try
        		{
        			JSONWrapper<JSONObject> wrapper = new JSONWrapper<JSONObject>(new JSONObject(paramString));
        			editorParameters = wrapper.getData();
        		}
        		catch (JSONException e)
        		{
        			logger.error(e.getMessage());
        			return;
        		}

        		String title = "Add widget<sup>(<a title='Help:Widgets' href='"+ new RequestMapperImpl().getRequestStringFromValue
        				(EndpointImpl.api().getNamespaceService().guessValue("Help:Widgets")) +"'>?</a>)</sup>";
        		WikiWidgetConfigurationForm.showWidgetConfigurationForm(getPage().getPopupWindowInstance(), title, editorParameters);
            }
        };
        editTextArea.value = semWiki.getWikiText();
        return editTextArea;
	}
	
	
	private FTextInput2 createCommentInput() {
		commentTxt = new FTextInput2(COMMENT_ID);        
        commentTxt.addStyle("margin-bottom", "10px");
        return commentTxt;
	}
	
	
	private PreviewContainer createPreview() {
		previewContainer = new PreviewContainer(PREVIEW_ID);
		return previewContainer;
	}
	
	private FButton createPreviewButton() {
		FButton prevbtn = new FButton(PREVIEW_BUTTON_ID, "Preview") {
			
			@Override
			public String getOnClick() {
				return "catchPostEventIdEncode('" + getId()
						+ "',9,getDomElementById('" + editTextArea.getComponentid()
						+ "').value,'msg');";
			}

			public void handleClientSideEvent(FEvent evt) {
				if (evt.type == FEventType.POST_EVENT)
					showPreview(evt.getPostParameter("msg"));
			}

			private void showPreview(String newContent) {
				// assert limited write access
				if (SemWikiUtil.violatesWriteLimited(semWiki.getAccessLevel(), newContent)) {
					addClientUpdate(new FClientUpdate(
							"alert('" + SemWikiUtil.WRITE_LIMITED_ERROR_MESSAGE + "')"));
					return;
				}

				previewContainer.updatePreview(newContent);						
			}

			@Override
			public void onClick() {
				// the method getOnclick() is used to get textarea per post event
			}
		};
		return prevbtn;
	}
	
	private FButton createCancelButton() {
		FButton cancelbtn = new FButton(CANCEL_BUTTON_ID, "Cancel") {
			@Override
			public void onClick() {
				addClientUpdate(new FClientUpdate(Prio.VERYBEGINNING, "$(window).unbind('beforeunload');"));
				semWiki.reloadWikiPage();
			}
		};
		cancelbtn.setConfirmationQuestion("Do you really want to discard all changes?");
		return cancelbtn;
	}
	
	private FButton createSubmitButton() {
		FButton fb = new FButton(SUBMIT_BUTTON_ID, "Save") {

			@Override
			public void onClick() {
				getPage().getPopupWindowInstance().hide();
			}

			@Override
			public String getOnClick() {
				return "$(window).unbind('beforeunload'); catchPostEventIdEncode('" + getId()
						+ "',9,getDomElementById('" + editTextArea.getComponentid()
						+ "').value,'msg');";
			}

			public void handleClientSideEvent(FEvent evt) {
				onClick(evt.getPostParameter("msg"));
			}

			public void onClick(String newContent) {
				if (semWiki.saveWiki(commentTxt.returnValues().toString(), newContent))
					semWiki.reloadWikiPage();	// reload only if save was successful.
			}
		};
		return fb;
	}
	
	
	/**
	 * Computes the set of template page links, i.e. the template pages for
	 * all rdf:type of this resource. The set of links is reduced to those 
	 * templates for which the current user has at least {@link ValueAccessLevel#WRITE_LIMITED}
	 * access.
	 * 
	 * @return
	 */
	private List<URI> computeTemplatePageLinks() {
		
		List<URI> templateResources = Lists.newArrayList();
		
		NamespaceService ns  = EndpointImpl.api().getNamespaceService();                
        URI templateUri;
        
        // Take the templates of the RDF types attached to the current instance.
        for (Resource r : semWiki.getTypeIncludes()) {
			if((r!=null) && (r instanceof URI)) {
				// check write permissions
				if (!EndpointImpl.api().getUserManager().hasValueAccess(r, ValueAccessLevel.WRITE_LIMITED))
					continue;

				templateUri = ValueFactoryImpl.getInstance().createURI(
						ns.templateNamespace(), r.stringValue());
				templateResources.add(templateUri);

			}
				
		}
        
        List<URI> includedTemplateURIs = Wikimedia.parseIncludedTemplates(semWiki.getWikiText(), semWiki.getSubject());
        for(URI includedTemplateURI : includedTemplateURIs) {
        	if (templateResources.contains(includedTemplateURI)) continue;
        	if (EndpointImpl.api().getUserManager().hasValueAccess(includedTemplateURI, ValueAccessLevel.WRITE_LIMITED))
        		templateResources.add(includedTemplateURI);
        }

        return templateResources;
	}
	
	/**
	 * Generates a list of String containing HTML renderings of linked template URIs.
	 * If not more than MAX_LINKS_FOR_TEMPLATES templates are to be displayed, they are rendered as HTML links divided by '|'
	 * Otherwise, they are displayed as combo box options. 
	 * 
	 * @param container
	 * @param templateResources
	 * @return
	 */
	private List<String> displayTemplateLinks(FContainer container, List<URI> templateResources) {
		if(templateResources.isEmpty())
			return Collections.emptyList();
		
		ReadDataManager dm = ReadWriteDataManagerImpl.getDataManager(semWiki.getRepository());
		List<String> res = Lists.newArrayList();
		if(templateResources.size()<=MAX_LINKS_FOR_TEMPLATES) {
			for(URI r : templateResources) {
				res.add("<a href=\"" + EndpointImpl
						.api()
						.getRequestMapper()
						.getRequestStringFromValueForAction(r,
								"edit") + "\">" + dm.getLabel(r) + "</a>");
			}
		} else {
			FComboBox comboBox = generateTemplateLinksComboBox(container, templateResources, dm);
			res.add(comboBox.render());
		}
		
		return res;
	}
	
	private static FComboBox generateTemplateLinksComboBox(FContainer container, List<URI> templateResources, ReadDataManager dm) {
		FComboBox comboBox = new FComboBox(Rand.getIncrementalFluidUUID()) {
			@Override
			public void onChange() {
				URI value = null;
				
				ArrayList<?> selectedList = getSelected();
				if(!selectedList.isEmpty()) {
					Object obj = selectedList.iterator().next();
					if(obj instanceof URI)
						value = (URI) obj;
				}
				
				if(value!=null) {
					addClientUpdate(new FClientUpdate("document.location='"+EndpointImpl
														.api()
														.getRequestMapper()
														.getRequestStringFromValueForAction(value,
																"edit")+"'"));
					populateView();
				}
			}
		};
		
		comboBox.addComponentStyle("vertical-align", "middle");
		comboBox.setEnableSorting(false);
		
		comboBox.addChoice("See list...", "");
		for(URI r : templateResources)
			comboBox.addChoice(dm.getLabel(r), r);
		
		container.getPage().register(comboBox);
		
		return comboBox;
	}

	@Override
	public String[] jsURLs() {
		List<String> javascripts = jsURLsHelper();
        return javascripts.toArray(new String[0]);
	}
	
	/**
	 * Compute the required JS URLs by the sem wiki widget in
	 * edit mode.
	 * 
	 * @return
	 */
	public static List<String> jsURLsHelper() {
		List<String> javascripts = new ArrayList<String>();
        
        String cp = EndpointImpl.api().getRequestMapper().getContextPath();
        javascripts.add( cp+"/markitup/jquery.markitup.js");
        return javascripts;
	}
	
	/**
	 * A special {@link FContainer} for wrapping up the
	 * rendered preview and the components. This container
	 * becomes handy as it invokes {@link #populateView()}
	 * on the registered widget components (if any), while
	 * rendering the {@link #renderedPreview}.
	 * 
	 * @author as
	 *
	 */
	protected class PreviewContainer extends FContainer {

		private String renderedPreview;
		
		public PreviewContainer(String id) {
			super(id);
		}
		
		public void updatePreview(String newContent) {
			previewContainer.removeAll();
			String renderedPreview = SemWikiUtil.getRenderedViewContent(newContent, semWiki.getSubject(), semWiki.getVersion(), this);
			this.renderedPreview = renderedPreview;
			previewContainer.populateView();
		}

		@Override
		public String render() {
			return renderedPreview;
		}		
	}
}
