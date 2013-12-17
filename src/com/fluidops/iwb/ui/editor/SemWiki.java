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

import static com.fluidops.util.StringUtil.isEmpty;

import java.util.Date;
import java.util.Set;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.repository.Repository;

import com.fluidops.ajax.FClientUpdate;
import com.fluidops.ajax.FClientUpdate.Prio;
import com.fluidops.ajax.components.FContainer;
import com.fluidops.ajax.components.FHTML;
import com.fluidops.ajax.components.FLabel;
import com.fluidops.ajax.components.FLabel.ElementType;
import com.fluidops.ajax.components.FTabPane2Lazy;
import com.fluidops.iwb.api.Context;
import com.fluidops.iwb.api.Context.ContextLabel;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.user.UserManager.ValueAccessLevel;
import com.fluidops.iwb.util.DateTimeUtil;
import com.fluidops.iwb.widget.SemWikiWidget;
import com.fluidops.iwb.wiki.WikiStorage;
import com.fluidops.iwb.wiki.WikiStorage.WikiRevision;
import com.fluidops.iwb.wiki.Wikimedia;
import com.fluidops.util.Rand;
import com.fluidops.util.StringUtil;

/**
 * {@link SemWiki} is the main UI component for rendering the platform's
 * wiki based UI. It is used from {@link SemWikiWidget} and maintains
 * a tabpane for lazy loading of the different view. The different 
 * views, i.e., view, edit, and revision, are shown depending on
 * the ACL settings for the given user.
 * 
 * This component implements the following behavior for rendering:
 * a) more than 1 tab: set active tab according to request ({@link #setActiveTab(WikiTab)}
 * b) exactly 1 tab: hide tab pane control component (CSS class "hideTabPaneControl")
 * c) 0 tabs visible: hide tab pane and show warning
 * 
 * The individual tab control items  can be styled with the following CSS classes:
 *  
 * - View: {@value #CSS_TAB_VIEW}
 * - Edit: {@value #CSS_TAB_EDIT}
 * - Revision: {@value #CSS_TAB_REVISION}
 * 
 * @author as
 * @see FTabPane2Lazy
 * @see SemWikiComponentHolder
 * 
 */
public class SemWiki extends FContainer {

	private static Logger logger = Logger.getLogger(SemWiki.class);	
    
	public final static String CSS_TAB_VIEW = "viewTab";
	public final static String CSS_TAB_EDIT = "editTab";
	public final static String CSS_TAB_REVISION = "revisionsTab";
	
	// wiki tabs
    public static enum WikiTab
    {
        REVISIONS,
        EDIT,
        VIEW
    }
	
	private final URI subject;
	
	/** Refers to the version of the wiki that is displayed. 
     * If version==null or version=="" the latest version is shown.
     * Otherwise, if version is a valid parseable timestamp (in ms),
     * the Wiki tab displays the version that was valid at that time.
     * In the latter case, there is no edit tab.
     */
    private final Date version;
    private final ValueAccessLevel accessLevel;
    private WikiTab activeTab = WikiTab.VIEW;
    /**
     * contains the redirected source, if #REDIRECT was used
     */
    private URI redirectedFrom;
    private Date renderTimestamp;
    private String wikiText;
    /**
     * The set of rdf:types of this resource, may be empty
     */
    private Set<Resource> typeIncludes;    
	
    private final Repository repository;

    
	public SemWiki(String id, URI value, Repository repository, String versionStr) {
		super(id);
		this.subject = value;
		this.repository = repository;
		this.version = SemWikiUtil.toDateVersion(versionStr);
		this.accessLevel = EndpointImpl.api().getUserManager().getValueAccessLevel(value);
	}
	
	
	public String getWikiText() {
		return wikiText;
	}
	
	public Date getVersion() {
		return version == null ? null : (Date) version.clone();
	}
	
	public URI getSubject() {
		return subject;
	}
	
	public ValueAccessLevel getAccessLevel() {
		return accessLevel;
	}
	
	public Repository getRepository() {
		return repository;
	}
	
	/**
	 * Returns the set of rdf:type of this resource, may be empty
	 * @return
	 */
	public Set<Resource> getTypeIncludes() {
		if (!initialized)
			throw new IllegalStateException("SemWiki not initialized");
		return typeIncludes;
	}
	
	public void setActiveTab(WikiTab activeTab) {
		this.activeTab = activeTab;
	}
	
	/**
	 * Set the URI from which the system has been redirected to
	 * the current page, i.e. typically a page having #REDIRECT
	 * specified.
	 * 
	 * @param redirectedFrom
	 */
	public void setRedirectedFrom(URI redirectedFrom) {
		this.redirectedFrom = redirectedFrom;
	}
	
	
	@Override
	public void initializeView() {
		initialize();
		super.initializeView();
	}


	private boolean initialized = false;
	
	protected void initialize() {
		if (initialized)
			return;
		
		if (accessLevel==null) {
			add(new FLabel(Rand.getIncrementalFluidUUID(), "Access to this resource denied."));
			return;
		}
			
		FTabPane2Lazy tabPane = new FTabPane2Lazy("tabPane");
		//enable client-side caching
		tabPane.setEnableClientSideTabCaching(true);
		tabPane.drawAdvHeader(true);
		tabPane.setTabsTemplate("com/fluidops/iwb/ui/editor/TabLabelSimple");
		tabPane.setTabLabelType(ElementType.DIV);
		tabPane.setClazz("semWiki");
		tabPane.setTabControlClazz("semWikiControl");
		this.add(tabPane);
		
		typeIncludes = SemWikiUtil.getTypesForIncludeScheme(repository, subject);
		wikiText = getWikitextContent();
		renderTimestamp = new Date();
		
		// ACL checks
		boolean hasViewTab = true;
		boolean hasEditTab = hasEditTab();
		boolean hasRevisionTab = hasEditTab || version!=null;	// for revision browsing we allow the tab
		
		// add the tabs (Note: this has to be done in reverse order
		// to maintain them correctly in browser as per float:right)
		if (hasRevisionTab)
			tabPane.addTab("Revisions", "Page revisions", new LazyRevisionsTabComponentHolder(this), CSS_TAB_REVISION);
		if (hasEditTab)
			tabPane.addTab("Edit", "Edit mode", new LazyEditTabComponentHolder(this), CSS_TAB_EDIT);
		if (hasViewTab)
			tabPane.addTab("View", "View mode", new LazyViewTabComponentHolder(this), CSS_TAB_VIEW);
		
		// if a specific version (= revision) was requested, show an appropriate title
		if (this.version != null) {
			tabPane.setTitle("<font color=\"darkgray\">Displaying version from " + DateTimeUtil.getDate(this.version, "EEE, d MMM yyyy HH:mm:ss") + " (editing disabled).</font>");
		}
		// if this page was redirected to, show an appropriate title
		if (this.redirectedFrom != null) {
			tabPane.setTitle("<font color=\"darkgray\">Redirected from " + EndpointImpl.api().getDataManager().getLabel(redirectedFrom) + "</font>");
		}
			
		
		// determine the actual rendering
		// a) more than 1 tab: set active tab according to request
		// b) exactly 1 tab: hide tab pane control component
		// c) 0 tabs visible: hide tab pane and show warning
		if (tabPane.getNumberOfTabs()>1) {
			
			int activeTabIndex = 0;
			switch (activeTab) {
			// determine the activeTabIndex based on the availability
			case VIEW: 			activeTabIndex += (hasEditTab?1:0) + (hasRevisionTab?1:0); break;
			case EDIT: 			activeTabIndex += (hasRevisionTab?1:0); break;
			case REVISIONS:		activeTabIndex += 0; break;
			default:			throw new UnsupportedOperationException("Tab not applicable: " + activeTab);
			}
			tabPane.setActiveTabWithoutRefresh(activeTabIndex);
		} else if (tabPane.getNumberOfTabs()==1) {
			tabPane.appendClazz("hideTabPaneControl");
		} else {
			tabPane.addTab("Error", new FHTML(Rand.getIncrementalFluidUUID(), "Error: No tab content available."));
		} 
		
		
		
		initialized=true;
	}
	
	/**
	 * Determine if the editTab is visible depending on given ACLs.
	 * 
	 * We allow editing if the user has full write access or if the user
	 * has limited write level and the page does not contain widgets;
	 * Further, we disable the edit tab if an older version is explicitly
	 * requested from the Wiki (to avoid overriding with old versions)
	 * 
	 * @return
	 */
	private boolean hasEditTab() {
        boolean hasEditTab = 
                version==null && // only allow editing latest version
                accessLevel!=null && 
                (accessLevel.equals(ValueAccessLevel.WRITE) || 
                (accessLevel.equals(ValueAccessLevel.WRITE_LIMITED) && !SemWikiUtil.violatesWriteLimited(accessLevel, wikiText)));
        return hasEditTab;
	}
	

	private String getWikitextContent() {

		String content = Wikimedia.getWikiContent(subject, version);
		if (content != null) {
			return content;
		} 
				
		// In case an entity does not yet have wiki page associated,
		// we allow to load a default page from a template.
		// The template is selected based on the type of the resource,
		// e.g. if you have have an entity Peter rdf:type Person,
		// the template will be loaded from Template:Person.

		// try to find a template page for one of the types
		StringBuilder includes = new StringBuilder();
		for (Resource r : typeIncludes) {
			// convert type to URI
			if (r == null || !(r instanceof URI))
				continue;
			URI type = (URI) r;

			// append #include for type
			if (includes.length() > 0)
				includes.append("\n");
			String typeUri = EndpointImpl.api().getNamespaceService()
					.getAbbreviatedURI(type);
			if (typeUri == null) {
				typeUri = type.stringValue();
				includes.append("{{Template:").append(typeUri).append("}}");
			} else
				includes.append("{{Template:").append(typeUri).append("}}");
		}

		return includes.toString();		
	}
	
	
	/**
     * Save the given wiki content using the underlying wikistorage.
     * If the wiki page contains semantic links, these are stored as
     * well. In case an error occurs while saving semantic links
     * (e.g. due to read only repositories) the wiki page is not 
     * saved and an appropriate error message is thrown wrapped
     * in a exception.
     * 
     * @param comment
     * @param newContent
     * @return true if the wiki page was save, false, otherwise
     */
    public boolean saveWiki(String comment, String newContent)
    {
        // just to make sure not to write based on old versions
        if (version!=null)
            throw new RuntimeException("Editing of non-latest version is not allowed. Aborting Save.");
        
        if (newContent == null)
            return false;
        
        String oldContent = wikiText;
        
        ValueAccessLevel al = EndpointImpl.api().getUserManager().getValueAccessLevel(subject);
        if (al == null || al.compareTo(ValueAccessLevel.READ)<=0)
        {
            logger.warn("Illegal access: wiki for resource " + subject + " cannot be saved.");
            return false; // no action
        }
        
        // now we can be sure the reader has at least WRITE_LIMITED access (i.e., al>=WRITE_LIMITED)
        
        WikiStorage ws = Wikimedia.getWikiStorage();
        WikiRevision latestRev = ws.getLatestRevision(subject);
        
        // assert limited write access
        if (SemWikiUtil.violatesWriteLimited(al, newContent))
        {
            addClientUpdate(new FClientUpdate("alert('" + SemWikiUtil.WRITE_LIMITED_ERROR_MESSAGE + "')"));
            return false;
        }
        
        if ( latestRev!=null && renderTimestamp!=null )
        {
            Date now = new Date();
            now.setTime(System.currentTimeMillis());
            if (latestRev.date.after(now))
                throw new RuntimeException(
                        "The Wiki modification date lies in the future, "
                            + "overriding would have no effect. Please fix your system "
                            + "clock settings or contact technical support");
            if (latestRev.date.after(renderTimestamp))
            {
                String user = latestRev.user;
                if (user!=null)
                    user = user.replace("\\", "\\\\");
                    
                throw new RuntimeException(
                        "The Wiki has been modified by user "
                                + user + " in the meantime. "
                                + "Please save your edits in an external application, "
                                + "reload the page and apply the edits again.");
            }
        }
        
        // Bug 5812 - XSS in revision comment
        comment = StringEscapeUtils.escapeHtml(comment);
        
        if (comment == null || isEmpty(comment.trim()))
            comment = "(no comment)";

        try
        {
	        ws.storeWikiContent(subject, newContent, comment, new Date());	        
        }
        catch (Exception e)
        {
        	logger.warn("Error while storing the wiki content: " + e.getMessage());
        	throw new RuntimeException(e);
        }
        try {
        	SemWikiUtil.saveSemanticLinkDiff(oldContent,newContent,subject,Context.getFreshUserContext(ContextLabel.WIKI));
        } catch (RuntimeException e) {
        	// undo the latest store operation if we cannot store semantic links   
        	ws.deleteRevision(subject, ws.getLatestRevision(subject));
        	throw e;
        }
        return true;
    }
    
    public void reloadWikiPage()   {
        // we do not send a document.location=document.location, because it does not work for pages
        // with HTML anchors (TODO fix also at other places, should build JS-side support)
        String referer = getPage().request.getHeader("referer");
        if (!StringUtil.isNullOrEmpty(referer))
            addClientUpdate( new FClientUpdate(Prio.VERYEND, "document.location=\"" + referer + "\";"));
        else // fallback
            addClientUpdate( new FClientUpdate(Prio.VERYEND, "document.location=document.location;"));
    }
   
}
