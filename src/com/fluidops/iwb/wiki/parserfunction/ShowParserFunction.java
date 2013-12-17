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
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringEscapeUtils;
import org.openrdf.model.URI;
import org.openrdf.model.Value;

import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.NamespaceService;
import com.fluidops.iwb.api.ReadDataManager;
import com.fluidops.iwb.api.operator.OperatorUtil;
import com.fluidops.iwb.api.valueresolver.ValueResolver;
import com.fluidops.iwb.api.valueresolver.ValueResolverRegistry;
import com.fluidops.iwb.page.PageContext;
import com.fluidops.util.StringUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;


/**
 * The #show parser function allows to access values from the RDF database 
 * associated to a specified resource.
 * 
 * Examples:
 * 
 * <source>
 * a) Showing a value of 'myProperty':  {{#show: {{this}} | :myProperty}}
 * b) Showing the incoming value of 'myProperty' pointing to the current resource: {{#show: {{this}} | ^:myProperty}}
 * c) Showing the current resource:  {{#show: {{this}}}}
 * d) Using FULLPAGENAME alias: {{#show: {{FULLPAGENAME}} | :myProperty}}
 * e) Using a fully qualified URI: {{#show: <http://www.example.org/someResource> | :someProperty}}
 * f) Using an abbreviated URI: {{#show: ex:someResource | :someProperty}}
 * g) Using a property from some known namespace: {{#show: ex:someResource | ex:someProperty}}
 * h) With no data message: {{#show: {{this}} | :notExist | noDataMessage=No data}}
 * i) With value resolver DATETIME: {{#show: {{this}} | :dateTime | valueResolver=DATETIME}}
 * j) Multiple options: {{#show: {{this}} | :dateTime | noDataMessage=No data| valueResolver=DATETIME}}
 * </source>
 * 
 * In addition, it allows to print the string value of the specified resource:
 * 
 * <source>
 * {{#show: {{this}} }} => http://example.org/stringValue
 * </source>
 * 
 * Note: the latter is returned in <nowiki> tags such that it isn't rendered
 * as a link by the wiki engine.
 * 
 * 
 * This parser function is an adapted version from
 * http://semantic-mediawiki.org/wiki/Help:Inline_queries#Parser_function_.23show

 * @author as
 *
 */
@edu.umd.cs.findbugs.annotations.SuppressWarnings(
	    value="URF_UNREAD_FIELD", 
	    justification="Page context is currently not used, will probably be later")
public class ShowParserFunction extends AbstractTemplateFunction implements PageContextAwareParserFunction {
	
	public enum Property {

		VALUE_RESOLVER("valueResolver"),

		/**
		 * Message that is printed verbatim (i.e. no further parsing is done)
		 * when there is no value for the property.
		 */
		NO_DATA_MESSAGE("noDataMessage");
		
		private Property(String literal) {
			this.literal = literal;
		}

		private final String literal;
		
		public static Set<String> getLiterals() {
			Set<String> res = Sets.newTreeSet();

			for (Property prop : Property.values()) {
				res.add(prop.literal);
			}

			return res;
		}

	}

	// will be used later
	@SuppressWarnings("unused")
	private PageContext pc;

	@Override
	public String parseFunction(List<String> parts, IWikiModel model,
			char[] src, int beginIndex, int endIndex, boolean isSubst)
			throws IOException {

		if (parts.size()==0)
			return null;
		
		URI resource = parseResource(parts.get(0), model);
		if (resource==null)
			return ParserFunctionUtil.renderError("Not a valid URI: " + parts.get(0));
		
		if (parts.size()==1)
			return "<nowiki>" + StringEscapeUtils.escapeHtml(resource.stringValue()) + "</nowiki>";
		
		// check for incoming 
		String propertyPart = parts.get(1);
		boolean incoming = false;
		if (isIncomingProperty(propertyPart)) {
			incoming = true;
			propertyPart = propertyPart.substring(propertyPart.indexOf("^")+1);
		}
		
		URI property = parseProperty(propertyPart, model);
		if (property==null)
			return ParserFunctionUtil.renderError("Not a valid URI: " + parts.get(0));
		
		Map<String, String> options = ParserFunctionUtil.getTemplateParameters(parts.subList(1, parts.size()));
		
		try {
			return incoming ? 
					renderShowIncoming(resource, property, options) :
					renderShowOutgoing(resource, property, options);
		} catch (Exception e) {
			return ParserFunctionUtil.renderError(e);
		}
	}
	
	private String renderShowOutgoing(URI resource, URI property, Map<String, String> options) {
		ReadDataManager dm = EndpointImpl.api().getDataManager();		
		List<Value> values = dm.getProps(resource, property);
		return renderShow(values, options);
	}
	
	private String renderShowIncoming(URI resource, URI property, Map<String, String> options) {
		ReadDataManager dm = EndpointImpl.api().getDataManager();		
		List<Value> values = Lists.newArrayList();
		values.addAll(dm.getInverseProps(resource, property));
		return renderShow(values, options);
	}
	
	/**
	 * Render the object value(s) of the statement associated to
	 * the given resource and property.
	 * 
	 * Optionally use a valueResolver via {@link #getValueResolverName(Map)}
	 * as specified in the options. If the value resolver results in an
	 * error for the given {@link Value}, the system will fallback to
	 * {@link ValueResolver#DEFAULT}. See {@link ValueResolver#resolveValues(String, List)}
	 * for details.
	 * 
	 * If there are no values available for this property, the optional
	 * setting "noDataMessage" can be used in the options.
	 * 
	 * @param values
	 * @param options
	 * @return
	 */
	private String renderShow(List<Value> values, Map<String, String> options) {
		
		// TODO think about using pc.repository

		SetView<String> unknownOptions = Sets.difference(options.keySet(), Property.getLiterals());

		if (!unknownOptions.isEmpty()) {
			throw new IllegalArgumentException("The following options are not understood: " + StringUtil.toString(unknownOptions, ", ")); 
		}

		if (values.size()==0 && options.containsKey(Property.NO_DATA_MESSAGE.literal))
			return ParserFunctionUtil.renderNoDataMessage(options.get(Property.NO_DATA_MESSAGE.literal));
		
		String valueResolver = getValueResolverName(options);						
		return ValueResolver.resolveValues(valueResolver, values);
	}
	

	
	/**
	 * Parses the resource part to a URI using 
	 * {@link NamespaceService#parseURI(String)}
	 * 
	 * @param resourcePart
	 * @param model
	 * @return
	 */
	private URI parseResource(String resourcePart, IWikiModel model) {
		String resource = parseTrim(resourcePart, model);
		return EndpointImpl.api().getNamespaceService().parseURI(resource);
	}
	
	/**
	 * Parses the resourcePart to a URI using the following semantics:
	 * 
	 * a) if resourcePart starts with '?', a URI with the substring 
	 *    after '?' is created in the default namespace
	 * b) if the resourcePart is a special standard URI as defined
	 *    by the {@link NamespaceService} (e.g. label=rdfs:label),
	 *    return the actual URI
	 * c) otherwise: use {@link #parseResource(String, IWikiModel)}
	 * 
	 * @param resourcePart
	 * @param model
	 * @return
	 */
	private URI parseProperty(String resourcePart, IWikiModel model) {
		String resource = resourcePart.trim();
		if (resource.startsWith("?"))
			return EndpointImpl.api().getNamespaceService().createURIInDefaultNS(resource.substring(1));
		URI specialMatch = EndpointImpl.api().getNamespaceService().matchStandardURI(resource);
		if (specialMatch!=null)
			return specialMatch;
		return parseResource(resource, model);
	}
	
	/**
	 * Returns true if the resource part specifies an incoming property.
	 * An incoming property is notated using the ^-symbol, borrowed 
	 * from http://www.w3.org/TR/sparql11-property-paths/
	 * 
	 * @param resourcePart
	 * @return
	 */
	private boolean isIncomingProperty(String resourcePart) {
		resourcePart = resourcePart.trim();
		return resourcePart.startsWith("^");		
	}
	
	/**
	 * Determine the {@link ValueResolver} name of the optional
	 * valueResolver specified in options. If there is no
	 * valueResolver specified, {@link ValueResolver#DEFAULT}
	 * is used. If an undefined {@link ValueResolver} is used,
	 * an {@link IllegalArgumentException} is thrown to indicate
	 * the user error.
	 * 
	 * For user convenience, this method removes enclosing ticks.
	 * 
	 * @param options
	 * @return
	 */
	private String getValueResolverName(Map<String, String> options) {
		String vr = options.get(Property.VALUE_RESOLVER.literal);
		if (vr==null)
			vr = ValueResolver.DEFAULT;
		vr = OperatorUtil.removeEnclosingTicks(vr);
		if (!ValueResolverRegistry.getInstance().hasValueResolver(vr))
			throw new IllegalArgumentException("No such value resolver: " + vr);
		return vr;
	}
	
	@Override
	public void setPageContext(PageContext pc) {
		this.pc = pc;		
	}

	@Override
	public String getFunctionName() {
		return "#show";
	}	
}
