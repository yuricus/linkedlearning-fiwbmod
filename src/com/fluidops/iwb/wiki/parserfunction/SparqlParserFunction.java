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

import org.apache.log4j.Logger;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryException;

import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.ReadDataManager;
import com.fluidops.iwb.api.ReadDataManagerImpl;
import com.fluidops.iwb.api.ReadDataManagerImpl.SparqlQueryType;
import com.fluidops.iwb.page.PageContext;
import com.fluidops.iwb.util.QueryResultUtil;
import com.fluidops.util.StringUtil;


/**
 * The #sparql parser function is a useful construct to access the RDF database 
 * from within wiki pages. It can be used to render the result of SPARQL SELECT 
 * queries using some wiki template for each tuple, or in conjunction with the 
 * #ifexpr to allow for conditional wiki content. 
 * 
 * Examples
 * 
 * a) Rendering an unordered list of all class instances
 * 
 * <source
 * {{#sparql: SELECT ?class WHERE { ?class rdf:type owl:Class } 
	 | format=template
	 | template=Template:ClassTemplate
	}}
	
	Content of the Template:ClassTemplate wiki page:

 	* {{{class}}
 * </source>
 * 
 * b) Using no options
 * 
 * When using no arguments for the #sparql function, the query types SELECT, CONSTRUCT
 * and ASK can be evaluated. The result of this function call is the size of the result
 * set (SELECT and CONSTRUCT) or 0|1 corresponding to false|true (ASK). This method
 * becomes handy together with the #ifexpr.
 * 
 * <source>
 * a) ASK query resulting in "0" (=false): {{#sparql: ASK { ?? :notExist ?o } }}
 * b) ASK query resulting in "1" (=true): {{#sparql: ASK { ?s ?p ?o } }}
 * c) SELECT query resulting in 10 (=size of result set with limit): {{#sparql: SELECT * WHERE { ?s ?p ?o } LIMIT 10 }}
 * </source>
 * 
 * @author as
 *
 */
public class SparqlParserFunction extends AbstractTemplateFunction implements PageContextAwareParserFunction {

	private static Logger logger = Logger.getLogger(SparqlParserFunction.class);
	
	private PageContext pc;

	@Override
	public String parseFunction(List<String> parts, IWikiModel model,
			char[] src, int beginIndex, int endIndex, boolean isSubst)
			throws IOException {

		if (parts.size()==0)
			return null;
		
		String query = parts.get(0).trim();
		Map<String, String> options = ParserFunctionUtil.getTemplateParameters(parts);
		
		try {
			// option format is specified
			if (options.containsKey("format")) {
				return parseTrim(
						parseFormat(query, options),
						model);
			}
		
			// no option: return the size of query result set
			return parseNoOptions(query);
			
		} catch (MalformedQueryException e) {
			return "Malformed query: " + e.getMessage();
		} catch (RepositoryException e) {
			logger.debug("Repository exception while evaluating #sparql parser function:", e);
			throw new IOException(e);
		} catch (QueryEvaluationException e) {
			logger.debug("Query evaluation error in #sparql parser function:", e);
			return "Error during query evaluation: " + e.getMessage();
		} catch (IllegalAccessException e) {
			// e.g. for UPDATE queries, message is prepared
			return e.getMessage();
		}
	}
	
	
	/**
	 * Evaluates the query (ASK, SELECT, CONSTRUCT) and returns the size of the
	 * result set. Note that 0 returns to false for ASK queries.
	 * 
	 * @param query
	 * @return
	 * @throws IllegalAccesstException if the query is an UPDATE query, or if it is malformed
	 * @throws IOException if there was an error during query evaluation
	 * @throws MalformedQueryException 
	 * @throws QueryEvaluationException 
	 * @throws RepositoryException 
	 */
	private String parseNoOptions(String query) throws IllegalAccessException, MalformedQueryException, RepositoryException, QueryEvaluationException {
		
		// TODO think about using repository from PageContext
		ReadDataManager dm = EndpointImpl.api().getDataManager();
		

		SparqlQueryType type = ReadDataManagerImpl.getSparqlQueryType(query, true);
		if (type==SparqlQueryType.ASK) {
			return dm.sparqlAsk(query, true, pc.value, false) ? "1" : "0";				
		}
		
		if (type==SparqlQueryType.SELECT) {
			return Integer.toString(
					QueryResultUtil.tupleQueryResultAsList(
							dm.sparqlSelect(query, true, pc.value, false)
						).size());
		}
		
		if (type==SparqlQueryType.CONSTRUCT) {
			return Integer.toString(
					QueryResultUtil.graphQueryResultAsList(
							dm.sparqlConstruct(query, true, pc.value, false)
						).size());
		}
		
		throw new IllegalAccessException("Unsupported query type: " + type);
		
	}
	
	/**
	 * Parses the option "format=FORMAT". 
	 * 
	 * Currently supported:
	 *  format=template
	 * 
	 * @param query
	 * @param options
	 * @return
	 * @throws QueryEvaluationException 
	 * @throws IllegalAccessException 
	 * @throws MalformedQueryException 
	 */
	private String parseFormat(String query, Map<String, String> options) throws MalformedQueryException, IllegalAccessException, QueryEvaluationException {
		String format = options.get("format");
		
		if ("template".equals(format))
			return parseFormatTemplate(query, options);
		
		throw new UnsupportedOperationException("Format '" + format + "' not yet supported.");
	}
	
	/**
	 * Parsers the option format=template: includes the template
	 * as specified by the additional template=TEMPLATE option.
	 * 
	 * Projection variables from the query can be referenced from
	 * the template using the {{{varName}}} notation.
	 * 
	 * Only SELECT queries are supported here, otherwise ERROR is
	 * printed.
	 * 
	 * @param query
	 * @param options
	 * @return
	 * @throws MalformedQueryException 
	 * @throws IllegalAccessException if the query is not of type SELECT
	 * @throws QueryEvaluationException 
	 */
	private String parseFormatTemplate(String query, Map<String, String> options) throws MalformedQueryException, IllegalAccessException, QueryEvaluationException {
		
		String template = options.get("template");
		if (template==null || StringUtil.isNullOrEmpty(template))
			throw new IllegalAccessException("Option template=TEMPLATE required for format=template.");
		template = template.trim();
				
		// TODO think about using repository from PageContext
		ReadDataManager dm = EndpointImpl.api().getDataManager();		

		SparqlQueryType type = ReadDataManagerImpl.getSparqlQueryType(query, true);
		if (type!=SparqlQueryType.SELECT)
			throw new IllegalAccessException("Only SELECT queries supported for format=template.");
		
		TupleQueryResult qRes = null;
		StringBuilder sb = new StringBuilder();
		try {
			qRes = dm.sparqlSelect(query, true, pc.value, false);
			while (qRes.hasNext()) {
				sb.append( buildTemplateCall(template, qRes.next()) ).append("\n");				
			}
		} finally {
			ReadDataManagerImpl.closeQuietly(qRes);
		}
		
		return sb.toString();
	}
	
	/**
	 * Creates the template inclusion command with named parameters for
	 * each value in the binding set. Literals are passed using their
	 * string value, while URIs are given as full URI (i.e. http:..)
	 * 
	 * Implementation notes: 
	 *  -it is not possible to return the URI as <http://...> since
	 *   the un-encoded < character breaks the wiki engine
	 * 
	 * Example:
	 * <source>
	 * {{Template:MyTemplate|binding1=literal value|binding2=http://example.org/full}}
	 * </source>
	 * 
	 * The template may then reference the values using {{{binding1}}} and {{{binding2}}}
	 * 
	 * @param template
	 * @param b
	 * @return
	 */
	private String buildTemplateCall(String template, BindingSet bs) {
		StringBuilder sb = new StringBuilder();
		sb.append("{{").append(template);
		for (String bName : bs.getBindingNames()) {
			sb.append("|");
			sb.append(bName).append("=");
			sb.append(ParserFunctionUtil.valueToString(bs.getValue(bName)));
		}
		
		sb.append("}}");
		return sb.toString();
	}
	
	
	
	@Override
	public void setPageContext(PageContext pc) {
		this.pc = pc;		
	}

	@Override
	public String getFunctionName() {
		return "#sparql";
	}	
}
