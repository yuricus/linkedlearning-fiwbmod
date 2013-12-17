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

package com.fluidops.iwb.widget.visualization;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResult;

import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FTree;
import com.fluidops.ajax.models.ExtendedTreeNode;
import com.fluidops.ajax.models.FTreeModel;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.ReadDataManager;
import com.fluidops.iwb.api.ReadDataManagerImpl;
import com.fluidops.iwb.api.RequestMapper;
import com.fluidops.iwb.model.ParameterConfigDoc;
import com.fluidops.iwb.model.ParameterConfigDoc.Type;
import com.fluidops.iwb.model.TypeConfigDoc;
import com.fluidops.iwb.provider.ProviderUtils;
import com.fluidops.iwb.widget.AbstractWidget;
import com.fluidops.iwb.widget.WidgetEmbeddingError;
import com.fluidops.iwb.widget.WidgetEmbeddingError.NotificationType;
import com.fluidops.iwb.widget.config.WidgetQueryConfig;
import com.google.common.collect.Lists;


/**
 * A flexible component to render hierarchical data in a tree.
 * 
 * Features:
 *  - Specification of a bootstrap query and (recursive) child configurations
 *  - The actual node can be referenced via ?:node in child configuration queries
 *  - The layout can be configured using a templating mechanism
 *  - Icons can be rendered using the special ?icon projection variable
 *  
 *  
 *  Examples:
 *  
 *  a) three level query showing classes and instances (limited to 10), list properties for each instance 
 *  
 *  <source>
 	{{#widget: Tree
 	| query = 'SELECT ?Class WHERE { ?Class rdf:type owl:Class }'
 	| childConfig = {{ 
 	     query = 'SELECT ?Instance WHERE { ?Instance rdf:type ?:node } LIMIT 10' | 
 	     childConfig = 
 	          {{ query = 'SELECT DISTINCT ?Property WHERE { ?:node ?Property ?o }' }} 
 	     }}
 	 }}  
 *  </source>
 *  
 *  b) Two level query showing classes and instances (limited to 10), applying custom template for instances === 
 *  
 * <source>
   {{#widget: Tree
	 | query = 'SELECT ?Class WHERE { ?Class rdf:type owl:Class }'
	 | childConfig = {{
	      query = 'SELECT ?instance ?comment WHERE { ?instance rdf:type ?:node . OPTIONAL { ?instance rdfs:comment ?comment } } LIMIT 10' 
	      | template = '{{{instance}}} (Comment: {{{comment}}})' 
	      }}
	 }}
 * </source>
 *  
 *  
 *  c) Example using images
 *  
 *  <source>
 *  {{#widget: Tree
    | query = 'SELECT ?Class ?icon WHERE { ?Class rdf:type owl:Class . BIND ("/favicon.ico" AS ?icon) } LIMIT 2'
    }}
 *  </source>
 *  
 *  
 *  d) Recursive category tree (abstract query)
 *  
 *  The following query recursively computes categories and sub-categories as well as instances in the category (leafs)
 *  
 *  <source>
 	{{#widget: Tree
    | query = 'SELECT ?category WHERE { BIND ( :RootCategory AS ?category ) }'
    | childConfig = 
          {{ query = 'SELECT ?subCategory WHERE { ?subCategory :hasSubCategory ?:node . }' 
           | childConfig = {{ query = 'SELECT ?instance WHERE { ?instance :isInCategory ?:node }' }} 
           | recursive = true 
          }}
    }} 
 *  </source>
 *  
 * @author as
 *
 */
@TypeConfigDoc( "The tree result widget allows to visualize hierarchical data in a tree." )
public class TreeResultWidget extends AbstractWidget<TreeResultWidget.Config> {

	private static final Logger logger = Logger.getLogger(TreeResultWidget.class);
	
	public static class Config extends WidgetQueryConfig {
		
		@ParameterConfigDoc(
				desc = "Configuration of children for the root node",
				type=Type.CONFIG )
		public ChildNodeConfig childConfig;
		
		@ParameterConfigDoc(
				desc = "Specifies an (optional) template pattern used for the current node's display. Variables from the query can be accessed as {{{varName}}}. Default shows label of the node.")
		public String template;
	
		@ParameterConfigDoc(
				desc = "Defines the title of the tree")
		public String title;
		
		@ParameterConfigDoc(
				desc = "Defines the page size of non-leaf nodes, i.e. the number of elements that are rendered on a single page. Defaults to unlimited.")
		public Integer treePageSize;
	}

	
	public static class ChildNodeConfig {
		
		@ParameterConfigDoc(
				desc = "The query string to apply containing a projection ?node. In the query the current node can be referenced via ?:node, special projection variable ?icon can be used to customize the icon for a given node",  
				required=true,        		
				type=Type.TEXTAREA) 
		public String query;
		
		@ParameterConfigDoc(
				desc = "Specifies an (optional) template pattern used for the current node's display. Variables from the query can be accessed as {{{varName}}}. Default shows label of the node.")
		public String template;
		
		@ParameterConfigDoc(
				desc = "Configuration of children for the root node",
				type=Type.CONFIG )
		public ChildNodeConfig childConfig;
		
		@ParameterConfigDoc(
				desc = "Specify if this node configuration shall be applied recursively.", defaultValue="false")
		public Boolean recursive;
	}

	
	@Override
	protected FComponent getComponent(String id) {

		Config c = get();
		c.noDataMessage = c.noDataMessage==null?WidgetQueryConfig.DEFAULT_NO_DATA_MESSAGE:c.noDataMessage;
		
		// TODO validate config:
		// - check that all queries are valid SELECT queries with at least one projection
		// - check if bootstrap query does not contain ?:node
		// - check if each childquery contains a reference to ?:node
		
		// Create tree model
		ChildNodeConfig rootQuery = new ChildNodeConfig();
		rootQuery.query = c.query;
		rootQuery.template = c.template;
		rootQuery.childConfig = c.childConfig;
		TreeNode root = new TreeNode(new PseudoRootNodeElement(rootQuery), -1);		// -1 is the level of the pseudo root node
		FTreeModel<TreeNodeElement> tm = new FTreeModel<TreeNodeElement>(root);
		
		if (tm.isLeaf(root))
			return WidgetEmbeddingError.getNotificationLabel(id, NotificationType.NO_DATA, c.noDataMessage);
		
		FTree tree = new FTree(id, tm);
		if (c.treePageSize!=null)
			tree.setPageSize(c.treePageSize);
		return tree;
	}

	@Override
	public String getTitle() {
		return get().title == null ? "" : get().title;
	}

	@Override
	public Class<?> getConfigClass() {
		return Config.class;
	}
	
	protected ReadDataManager getDataManager() {
		return EndpointImpl.api().getDataManager();
	}
	
	
	/**
	 * Constructs the Child-TreeNode Element for the given level (where 1 is the first child).
	 * Uses recursion to get to the actual child, so the start is always the {@link Config#childConfig}
	 * 
	 * @param current
	 * @param level
	 * @param bindings
	 * @return
	 */
	private TreeNodeElement constructChild(TreeNodeElement current, BindingSet bindings) {
		
		ChildNodeConfig nodeConfig = current.childNodeConfig;
		// root with no leaves
		if (nodeConfig==null) {
			return new TreeNodeElement(bindings, current.template);
		}
		if (nodeConfig.recursive!=null && nodeConfig.recursive) {
			return new RecursiveTreeNodeElement(nodeConfig, bindings, nodeConfig.template);
		}
		// leaf
		if (nodeConfig.childConfig==null) {
			return new TreeNodeElement(bindings, nodeConfig.template);
		}
		return new TreeNodeElement(nodeConfig.childConfig, bindings, nodeConfig.template);
	}
	
	protected class TreeNode extends ExtendedTreeNode<TreeNodeElement> {

		private static final long serialVersionUID = -5578996866273037783L;
		private int level = -1;
		
		/**
		 * cache for the children to compute them only once (but lazy)
		 */
		protected List<TreeNode> childrenCache = null;
		
		public TreeNode(TreeNodeElement node, int level) {
			super(node);
			this.level = level;
		}

		@Override
		public List<? extends ExtendedTreeNode<TreeNodeElement>> getChildren() {

			if (childrenCache!=null)
				return childrenCache;

			TreeNodeElement current = getObj();			
			childrenCache = evaluateNode(current);
			return childrenCache;
		}
		
		protected List<TreeNode> evaluateNode(TreeNodeElement current) {
			
			if (current.getChildQuery()==null) {
				childrenCache = Collections.emptyList();
				return childrenCache;
			}
						
			ReadDataManager dm = getDataManager();
			
			List<TreeNode> res = new ArrayList<TreeNode>();
			TupleQueryResult qres = null;
			try {
				qres = dm.sparqlSelect(current.getChildQuery(), true, pc.value, false);
				
				while (qres.hasNext()) {
					TreeNodeElement child = constructChild(current, qres.next());
					child.setNodeBindingNames(qres.getBindingNames());
					res.add(createNode(child));
				}
				
			} catch (MalformedQueryException e) {
				// can be ignored, is dealt with before
				logger.trace("Malformed query exception encountered: " + e.getMessage());
			} catch (QueryEvaluationException e) {
				logger.debug("Query evaluation error: " + e.getMessage());
				res.add( createNode(new ErrorTreeNodeElement(e.getMessage())) );
				return res;		// try again next time
			} finally {
				ReadDataManagerImpl.closeQuietly(qres);
			}
			
			return res;
		}

		@Override
		public List<?> setValues(TreeNodeElement nodeElement) {			
        	return Lists.newArrayList(nodeElement.render());
		}
		
		
		private TreeNode createNode(TreeNodeElement el) {
			if (el instanceof RecursiveTreeNodeElement) 
				return new RecursiveTreeNode(el, level+1);
			return new TreeNode(el, level+1);
		}
		
		
	}
	
	
	private class RecursiveTreeNode extends TreeNode {

		private static final long serialVersionUID = 1L;

		public RecursiveTreeNode(TreeNodeElement node, int level) {
			super(node, level);
		}

		@Override
		public List<? extends ExtendedTreeNode<TreeNodeElement>> getChildren() {
			if (childrenCache!=null)
				return childrenCache;
			
			@SuppressWarnings("unchecked")
			List<TreeNode> res = (List<TreeNode>)super.getChildren();
			
			TreeNodeElement current = getObj();
			ChildNodeConfig actualConfig = current.childNodeConfig;
			// if we have some further child
			if (actualConfig.childConfig!=null) {
				TreeNodeElement el = new TreeNodeElement(actualConfig.childConfig, current.bindings, current.template);
				el.setNodeBindingNames(current.bindingNames);
				List<TreeNode> tmp = this.evaluateNode(el);
				res.addAll( tmp );
			}
			childrenCache = res;
			return res;
		}		
	}
	
	/**
	 * Representation of a TreeNodeElement
	 * 
	 * Node is rendered according to given data in bindings
	 * by applying the template. The default template is such
	 * that it renders an icon (if configured) and next to it
	 * the label of the first projection variable in the binding
	 * set.
	 * 
	 * The projection value of the first variable is used as the
	 * node identifier, and can be accessed in child queries 
	 * via ?:node
	 * 
	 * @author as
	 *
	 */
	protected class TreeNodeElement {
		
		protected final ChildNodeConfig childNodeConfig;
		private final BindingSet bindings;
		private final String template;
		
		// the binding name for the node value, i.e. the
		// first variable from the project list of the query
		private String nodeBindingName;
		private List<String> bindingNames;
						
		/**
		 * Constructor for a leaf node
		 * @param bindings
		 * @param template
		 */
		public TreeNodeElement(BindingSet bindings, String template) {
			this(null, bindings, template);		
		}
		
		/**
		 * Constructor for a parental node (not root)
		 * 
		 * @param childQuery
		 * @param bindings
		 * @param template
		 */
		public TreeNodeElement(ChildNodeConfig childNodeConfig, BindingSet bindings, String template) {
			super();
			this.childNodeConfig = childNodeConfig;
			this.bindings = bindings;
			this.template = template==null ? "{{{node}}}" : template;
		}
		
		/**
		 * Constructor for the root node
		 * @param childNodeConfig
		 */
		public TreeNodeElement(ChildNodeConfig childNodeConfig) {
			this(childNodeConfig, null, null);
		}
		
		
		/**
		 * Sets the binding names available in the query.
		 * 
		 * The first binding name is used for the node value, i.e. the
		 * first variable from the projection list of the query. 
		 * This bindingName is used to retrieve the value from
		 * the {@link #bindings}.
		 * 
		 * @param bindingName
		 */
		public void setNodeBindingNames(List<String> bindingNames) {
			this.bindingNames = bindingNames;
			this.nodeBindingName = bindingNames.iterator().next();
		}
		
		/**
		 * Return the query to compute children of the current node. The
		 * query is already prepared, i.e. ?:node has been replaced with
		 * the value of this node.
		 * 
		 * If the nodeValue is not a URI, the query is replaced to return
		 * a warning, i.e. only URIs are supported for children nodes.
		 * 
		 * @return
		 */
		public String getChildQuery() {
			if (childNodeConfig==null)
				return null;	// leaf node
			Value nodeValue = getNodeValue();
			if (nodeValue instanceof URI)
				return childNodeConfig.query.replace("?:node", ProviderUtils.uriToQueryString((URI)nodeValue));
			return "SELECT * WHERE { BIND(\"WARN: only URIs supported for computation of child nodes\" AS ?res) }";
		}
		
		/**
		 * Renders this node according to the following rules, by applying
		 * string replacements of {{{varName}}} in the template. 
		 *  
		 * 1) {{{node}}} is replaced with the label of the nodeValue
		 * 2) for each binding name, replace {{{bindingName}}} with label
		 * 3) Make a link of the output to the nodeValue, using 
		 *    {@link RequestMapper#getAHrefEncoded(Value, String, String)}
		 * 4) icon is prepended to output determined by {@link #getIconRender()}
		 *    
		 * Implementation note: 
		 *  - the default template is "{{{node}}}"  where by default 
		 *    the icon is not rendered.
		 *  - if there is no binding available in the bindingset for a 
		 *    given binding name, it is rendered as "n/a". E.g. if the
		 *    projection variable "opt" is accessed in the template as
		 *    {{{opt}}}, but there is no binding due to optional for 
		 *    this in the bindingset, then it is rendered as n/a
		 * 
		 * @return
		 */
		public String render() {
			
			String out = template.replace("{{{node}}}", getDataManager().getLabelHTMLEncoded(getNodeValue()));
			for (String bindingName : bindingNames) {
				String replacement = bindings.hasBinding(bindingName) ? getDataManager().getLabelHTMLEncoded(bindings.getValue(bindingName)) : "n/a";
				out = out.replace("{{{"+bindingName+"}}}", replacement);
			}
			return  getIconRender() + EndpointImpl.api().getRequestMapper().getAHrefEncoded(getNodeValue(), out, null);
		}
		
		private Value getNodeValue() {
			return bindings.getValue(nodeBindingName);
		}
		
		
		/**
		 * Determines how the icon is rendered, using the following
		 * rules
		 * 
		 * a) use the bindings "icon" value
		 * b) do not render an icon
		 * 
		 * @return
		 */
		private String getIconRender() {
			if (!bindings.hasBinding("icon"))
				return "";
			String icon = bindings.getValue("icon").stringValue();
			
			return "<img src=\""+icon+"\" style=\"max-height: 24px;\" align=\"absmiddle\" />&nbsp;";
			
		}
	}
	
	protected class PseudoRootNodeElement extends TreeNodeElement {
		public PseudoRootNodeElement(ChildNodeConfig childNodeConfig) {
			super(childNodeConfig, null, null);
		}
		@Override
		public String render() {
			return "<b>" + getTitle() + "</b>";   
		}
		@Override
		public String getChildQuery() {
			return childNodeConfig.query;
		}
		
	}
	
	protected class RecursiveTreeNodeElement extends TreeNodeElement {

		public RecursiveTreeNodeElement(ChildNodeConfig childNodeConfig,
				BindingSet bindings, String template) {
			super(childNodeConfig, bindings, template);
		}
		
	}
	
	protected class ErrorTreeNodeElement extends TreeNodeElement {
		
		public ErrorTreeNodeElement(String error) {
			super(null, null, null);
		}

		@Override
		public String render() {
			return "<b>Error while evaluating query</b>";
		}			
	}
}
