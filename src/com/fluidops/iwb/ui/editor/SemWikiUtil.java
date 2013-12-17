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

import info.aduna.iteration.Iterations;
import info.bliki.wiki.template.ITemplateFunction;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.Repository;

import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FContainer;
import com.fluidops.iwb.Global;
import com.fluidops.iwb.api.Context;
import com.fluidops.iwb.api.ReadDataManager;
import com.fluidops.iwb.api.ReadDataManagerImpl;
import com.fluidops.iwb.api.ReadWriteDataManager;
import com.fluidops.iwb.api.ReadWriteDataManagerImpl;
import com.fluidops.iwb.user.UserManager.ValueAccessLevel;
import com.fluidops.iwb.util.Config;
import com.fluidops.iwb.widget.AbstractWidget;
import com.fluidops.iwb.wiki.FluidWikiModel;
import com.fluidops.iwb.wiki.Wikimedia;
import com.fluidops.util.StringUtil;
import com.google.common.collect.Sets;

/**
 * Convenience functions related to {@link SemWiki}.
 * 
 * @author as
 */
public class SemWikiUtil {

	private static Logger logger = Logger.getLogger(SemWikiUtil.class);
	
	
	/**
     * Convert the given version string to a {@link Date}. If the
     * versionStr is null or empty, this method returns null,
     * indicating that the most recent version is to be shown
     * 
     * @param versionStr the version as timestamp
     * @return the {@link Date} corresponding to versionStr or null for the
     *   		most recent version
     */
	public static Date toDateVersion(String versionStr) {
		if (!StringUtil.isNullOrEmpty(versionStr)) {
			try {
				Date d = new Date();
				d.setTime(Long.valueOf(versionStr));
				return d;
			} catch (Exception e) {
				logger.warn("Illegal version request: " + versionStr);
			}
		}
		return null;
	}
	
	/**
	 * Returns the rendered view content as HTML for the given
	 * settings. The found widgets are registered to the
	 * provided parent container.
	 * 
	 * @param content
	 * @param subject
	 * @param version the version to retrieve or null (for the most up2date version)
	 * @param parent a special container used for rendering (see e.g {@link LazyViewTabComponentHolder})
	 * @return
	 */
	public static String getRenderedViewContent(String content, URI subject, Date version, FContainer parent) {
		// Initialize the HTML renderer.
		// This is a per-thread context used for rendering.
		Wikimedia.initializeHTMLRenderer();
		String wikiHtml = Wikimedia.getHTML(content, subject, parent, version);			
		for (FComponent widgetComponent : Wikimedia.getRenderedComponents()) {
			parent.add(widgetComponent);
		}
		Wikimedia.initializeHTMLRenderer();
		return wikiHtml;
	}
	
	/**
	 * Return the JS URLs required by any widget contained in the given wikiText. This
	 * method parses the wikiText using {@link Wikimedia#parseWidgets(String, URI)} and
	 * then applies the {@link AbstractWidget#jsURLs()} method.
	 * 
	 * @param wikiText
	 * @param subject
	 * @return a list containing all  distinct java scripts required by widgets contained in the wiki text, may be empty
	 */
	public static Set<String> getWidgetsJSUrls(String wikiText, URI subject) {

		Set<String> res = Sets.newLinkedHashSet();
		Wikimedia.initializeHTMLRenderer();
		for (Class<? extends AbstractWidget<?>> c : Wikimedia.parseWidgets(wikiText, subject)) {
			try {
				List<String> o = c.newInstance().jsURLs();
				if (o != null)
					res.addAll(o);
			} catch (Exception e) {
				logger.warn("Error while parsing widgets from class " + c.getName() + ": " + e.getMessage());
				logger.debug("Details: ", e);
			}
		}
		return res;
	}
	
	/**
     * Returns all the RDF types of the current resource.
     * @return
     */
	public static Set<Resource> getTypesForIncludeScheme(Repository repository, URI value) {
		// TODO extensive test case, then cleanup

		ReadDataManager dm = ReadDataManagerImpl.getDataManager(repository);

		String includeScheme = Config.getConfig().getWikiIncludeScheme();
		Set<Resource> types = new HashSet<Resource>();
		if (includeScheme.equals("type")) {
			Set<Resource> typesHlp = dm.getType(value, false);
			if (typesHlp != null && !typesHlp.isEmpty())
				types.add(typesHlp.iterator().next());
		}
		// selects (a random) most specific type out of the specified types
		else if (includeScheme.equals("mostSpecificTypes")) {
			Set<Resource> typesHlp = dm.getType(value, false);

			Set<Resource> typesHlpHavingSubclasses = new HashSet<Resource>();
			try {
				for (Resource type : typesHlp) {
					// get subclasses
					List<Statement> stmts = Iterations.asList(dm.getStatements(null,
							RDFS.SUBCLASSOF, type, false));
					for (Statement stmt : stmts) {
						Resource subClass = stmt.getSubject();
						if (!subClass.equals(type)) {
							typesHlpHavingSubclasses.add(type);
							break;
						}
					}
				}
			} catch (Exception e) {
				logger.warn(e.getMessage());
			}

			// a leaf is a type having no subclasses, try to find one
			for (Resource type : typesHlp) {
				if (!typesHlpHavingSubclasses.contains(type))
					types.add(type);
			}

			// if no types have been selected so far...
			if (types.isEmpty() && !typesHlp.isEmpty())
				types.addAll(typesHlp);
		} else if (includeScheme.equals("types-recursive"))
			types = dm.getType(value);
		else
			// includeScheme.equals("types")
			types = dm.getType(value, false);

		return types;
	}
	
	
	/**
     * Extracts semantic links from the old and from the new wiki page and collects the diff
     * inside the addStmts and remStmts variables.
     * 
     * @param wikiPageOld the old wiki page text (may be null or empty)
     * @param wikiPageNew (may be null or empty)
     * @param subject the URI represented by the wiki page
     * @param context the context in which to store the diff
     */
    public static void saveSemanticLinkDiff(String wikiPageOld, String wikiPageNew, URI subject, Context context)
    {
    	try
    	{
	    	List<Statement> oldStmts = Wikimedia.getSemanticRelations(wikiPageOld, subject);
	        List<Statement> newStmts = Wikimedia.getSemanticRelations(wikiPageNew, subject);
	
	        // calculate stmts to remove
	        Set<Statement> remStmts = new HashSet<Statement>();
	        remStmts.addAll(oldStmts);
	        remStmts.removeAll(newStmts);
	        
	        // calculate stmts to add
	        Set<Statement> addStmts = new HashSet<Statement>();
	        addStmts.addAll(newStmts);
	        // Note msc: 
	        // strictly speaking, we should do the following now:
	        //
	        // addStmts.removeAll(oldStmts);
	        // 
	        // , to add only those statements that were definitely added. Though we
	        // encountered problems with this (e.g. when pressing the Save button
	        // twice), resulting in statements that are visible in the Wiki as sem
	        // links, but not contained in the DB. Given that the addToContext() 
	        // will not create duplicates anyway, we write all statements, to avoid
	        // the above-mentioned problems.
	        
	        ReadWriteDataManager dm = null;
	        try
	        {
	        	dm = ReadWriteDataManagerImpl.openDataManager(Global.repository);
		        if (remStmts.size()>0)
		            dm.removeInEditableContexts(remStmts, context);
		        
		        if (addStmts.size()>0)
		            dm.addToContextNoDuplicates(addStmts, context);
	        }
	        finally
	        {
	        	ReadWriteDataManagerImpl.closeQuietly(dm);
	        }
	        
    	}
    	catch (Exception e)
    	{
    		String message = e.getMessage();
    		if (e instanceof UnsupportedOperationException)
    			message = "Write operations to the repository not supported (read only).";
    		logger.debug("Error while saving semantic links: " + message);
    		throw new RuntimeException("Error while saving semantic links: " + message, e);
    	}
    }
    
    
	/**
	 * If user has limited write access, assert that the page doesn't contain
	 * widgets or #sparql, #show, #ifexpr parser functions. The test itself is done by means
	 * of {@link WriteAccessFluidWikiModel}
	 * 
	 * Note: this method invocation uses the bliki engine and parses the
	 * given text once.
	 */
	public static boolean violatesWriteLimited(ValueAccessLevel al,
			String content) {
		if (al.equals(ValueAccessLevel.WRITE_LIMITED)) {
			return WriteAccessFluidWikiModel.violatesWriteLimited(content);
		}
		// everything allright
		return false;
	}
	
	/**
	 * The error message that is shown in case of write_limited (for preview and save)
	 */
	public static final String WRITE_LIMITED_ERROR_MESSAGE = "You do not have permission to include new widgets or use the SPARQL, #show, or #ifexpr parser function.";
	
	/**
	 * A special implementation of {@link FluidWikiModel} which can be used
	 * to check if a given wikitext does not violate write limited. See
	 * {@link #violatesWriteLimited(String)} for details.
	 *  
	 * @author as
	 */
	private static class WriteAccessFluidWikiModel extends FluidWikiModel {

		/**
		 * Returns true if the given wiki text violates write limited, i.e.
		 * if it contains #widget or #sparql. Note that templates are not
		 * resolved recursively.
		 * 
		 * @param wikiText
		 * @return
		 */
		public static boolean violatesWriteLimited(String wikiText) {
			WriteAccessFluidWikiModel wfm = new WriteAccessFluidWikiModel();
			wfm.setUp();
			wfm.parseTemplates(wikiText);
			return wfm.containsWriteAccessToken();
		}
		
		private boolean containsWidgets = false;
		private boolean containsSparql = false;
		private boolean containsShow = false;
		private boolean containsIfexpr = false;
		
		private WriteAccessFluidWikiModel() {
			super(ValueFactoryImpl.getInstance().createURI("http://www.fluidops.com/write_access"));
			
			// add a template resolver to check for #widget and #sparql
			addTemplateResolver(new TemplateResolver() {				
				@Override
				public String resolveTemplate(String namespace, String templateName,
						Map<String, String> templateParameters, URI page, FComponent parent) {
					if ( templateName.startsWith("#widget"))
						containsWidgets = true;
					else if ( templateName.startsWith("#show"))
						containsShow = true;
					else if ( templateName.startsWith("#sparql"))
						containsSparql = true;
					// Note: #ifexpr needs to be checkd in #getTemplateFunction() 
					return null;
				}
			});				
		}
				
		@Override
		public ITemplateFunction getTemplateFunction(String templateName) {
			// the built-in parser function #ifexpr cannot be checked 
			// in the template resolver. Thus a check is added here.
			if ( templateName.startsWith("#ifexpr"))
				containsIfexpr = true;
			return super.getTemplateFunction(templateName);
		}



		/**
		 * Returns true if and only if the parsed wiki text contains
		 * a #widget, #show, #ifexpr or #sparql, which causes write access
		 * @return
		 */
		public boolean containsWriteAccessToken() {
			return containsWidgets || containsSparql || containsShow || containsIfexpr;
		}
	}
}
