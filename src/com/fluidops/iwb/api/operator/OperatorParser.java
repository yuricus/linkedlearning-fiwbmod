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

package com.fluidops.iwb.api.operator;

import static com.fluidops.util.StringUtil.isNullOrEmpty;
import info.bliki.wiki.filter.TemplateParser;
import info.bliki.wiki.template.AbstractTemplateFunction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.query.MalformedQueryException;

import com.fluidops.ajax.components.FComponent;
import com.fluidops.iwb.api.ReadDataManagerImpl;
import com.fluidops.iwb.api.ReadDataManagerImpl.SparqlQueryType;
import com.fluidops.iwb.wiki.FluidTemplateResolver;
import com.fluidops.iwb.wiki.FluidWikiModel;
import com.fluidops.iwb.wiki.FluidWikiModel.TemplateResolver;
import com.fluidops.util.ObjectHolder;
import com.fluidops.util.StringUtil;

/**
 * Parser for operator syntax. Uses the bliki engine {@link TemplateParser}
 * together with a custom {@link TemplateResolver} implementation to 
 * parse wiki text into the corresponding {@link Operator}.
 * 
 * @author aeb, as
 *
 */
class OperatorParser  {			
	
	static Pattern whitespacePattern = Pattern.compile("\\s+");
	static Pattern bracketsPattern = Pattern.compile("\\s*\\{\\{\\s*");
	static Pattern equalsPattern = Pattern.compile("\\s*=\\s*");
	static Pattern structPairPattern = Pattern.compile("\\s*[A-Za-z]+\\s*=.*");
	static Pattern structPattern = Pattern.compile("\\{\\{\\s*[A-Za-z]+\\s*=.*");
	
	static Operator parse(String serialized, URI pageURI) {			
		Object root = parseToObject(serialized, pageURI);			
		OperatorNode opNode = toOperatorNode(root);
		return new Operator(opNode);
	}
	
	static Operator parseStruct(Map<String, String> parameters, URI pageURI) {
		OperatorStructNode opStruct = new OperatorStructNode();
		for (Entry<String, String> keyEntry : parameters.entrySet()) {
			if(isUnnamedParameter(keyEntry)) continue;
			Object parsed = parseToObject(keyEntry.getValue(), pageURI);
			opStruct.add(keyEntry.getKey(), toOperatorNode(parsed));
		}
		return new Operator(opStruct);
	}
	
	static Operator toOperator(Object o) {
		OperatorNode opNode = toOperatorNode(o);
		return new Operator(opNode);
	}

	private static boolean isUnnamedParameter(Entry<String, String> keyEntry) {
		return keyEntry.getKey().equals("1");
	}
			
	@SuppressWarnings("unchecked")
	static OperatorNode toOperatorNode(Object o) {
		if (o ==null) {
			throw new IllegalArgumentException("Object must not be null");
		}
		if (o instanceof Map) {
			return toOperatorStructNode((Map<String,Object>)o);
		}
		if (o instanceof List) {
			return toOperatorListNode((List<Object>)o);
		}
		if (o instanceof String) {
			String s = (String)o;
			if (s.startsWith("$"))
				return toOperatorEvalNode(s);
			return toOperatorConstantNode(s);
		}
		if (o instanceof Literal) {
			Literal v = (Literal)o;
			return toOperatorConstantNode("'" + v.toString() + "'");			
		}
		if (o instanceof URI) {
			URI u = (URI)o;
			return toOperatorConstantNode("'<" + u.stringValue() + ">'");			
		}
		// TODO additional constants that are supported
		if (o instanceof Boolean || o instanceof Integer) {
			return toOperatorConstantNode(o.toString());
		}
		
		if (o.getClass().isEnum()) {
			return toOperatorConstantNode("'" + o.toString() + "'");
		}
		throw new IllegalArgumentException("Object type not supported: " + o.getClass().getName() + " => " + o.toString());
	}
	
	private static OperatorStructNode toOperatorStructNode(Map<String, Object> map) {
		OperatorStructNode res = new OperatorStructNode();
		for (Entry<String, Object> keyEntry : map.entrySet())
			res.add(keyEntry.getKey(), toOperatorNode(keyEntry.getValue()));
		return res;
	}
	
	private static OperatorListNode toOperatorListNode(List<Object> list) {
		OperatorListNode res = new OperatorListNode();
		for (Object o : list)
			res.addChild(toOperatorNode(o));
		return res;
	}
	
	private static Pattern evalPattern = Pattern.compile("$.*$");
	private static OperatorNode toOperatorEvalNode(String s) {
		if (evalPattern.matcher(s).matches())
			throw new IllegalArgumentException("Illegal evaluation pattern specified: " + s);
		if (s.startsWith("$this"))
			return new OperatorThisEvalNode(s);
		try	{
			String queryString = s.substring(1, s.length()-1);			
			SparqlQueryType qt = ReadDataManagerImpl.getSparqlQueryType(OperatorUtil.replaceSpecialTokens(queryString), true);
			if (qt == SparqlQueryType.SELECT)
				return new OperatorSelectEvalNode(s);
		}
		catch (MalformedQueryException e) {
			throw new IllegalArgumentException("Invalid SPARQL SELECT query specified: " + e.getMessage());
		}
		throw new IllegalArgumentException("Illegal evaluation pattern specified: " + s);
	}
	
	private static OperatorConstantNode toOperatorConstantNode(String s) {
		return new OperatorConstantNode(s);
	}
	
	public static final String OPEN = "{{";
	public static final String SINGLE = "'";
	public static final String DOUBLE = "\"";
	
	/**
	 * parser for nested template parameters / widget config
	 * 
	 * parse nested wiki config, borrowed from TP class (originally implemented by aeb)
	 * 
	 * @param s		a config such as {{ a=b | c={{1|2|3}} }} or simple strings with or without quotes
	 * @return		a mix of list, map, string, bool, int, double representing the value tree
	 * @author 		aeb
	 */
	private static Object parseToObject(String s, final URI pageUri)
	{
		if ( s == null )
			return null;
		
		final ObjectHolder<Object> holder = new ObjectHolder<Object>();
		
		final FluidWikiModel wikiModel = new FluidWikiModel(pageUri, null) {

			@Override
			public void substituteTemplateCall(String templateName,
					Map<String, String> parameterMap, Appendable writer)
					throws IOException {
				String plainContent = getRawWikiContent(getTemplateNamespace(), templateName, parameterMap);
				
				if (StringUtil.isNullOrEmpty(plainContent)) {
					// return the original template value
					StringBuilder sb=new StringBuilder();
					sb.append("{{").append(templateName);
					for (Entry<String, String> e : parameterMap.entrySet())
						if (isListParameterMap(parameterMap))
							sb.append(" | ").append(e.getValue());
						else
							sb.append(" | ").append(e.toString());
					sb.append("}}");
					writer.append(sb.toString());
				} else {
					writer.append(AbstractTemplateFunction.parseTrim(plainContent, this));
				}
			}       	
        };
        wikiModel.setReplaceIWBMagicWords(false);
        
        wikiModel.addTemplateResolver( new TemplateResolver() 
        {
			@Override
			public String resolveTemplate(String namespace,
					String templateName,
					Map<String, String> pars, URI page,
					FComponent parent)
			{
				// array if key 1 exists
				if ( isListParameterMap(pars) )
				{
					// array
					List<Object> list = new ArrayList<Object>();
					for ( String s : pars.values() ) {
						Object parsed = parseToObject(s, pageUri);
						if(nonEmpty(parsed)) list.add( parsed );
					}
					holder.value = list;
				}
				else
				{
					// struct
					Map<String, Object> map = new HashMap<String, Object>();
					for (Entry<String, String> parameter : pars.entrySet()) {
						if(!isUnnamedParameter(parameter)) 
							map.put(parameter.getKey(), parseToObject(parameter.getValue(), pageUri));
					}
					holder.value = map;
				}
				
				return null;
			}

		} );
        FluidTemplateResolver tplResolver = new FluidTemplateResolver(wikiModel, null) {

			@Override
			public String resolveTemplate(String namespace,	String templateName,
					Map<String, String> templateParameters, URI page, FComponent parent) {
				// if the templateName does not contain a ':' ignore it. The issue is that
				// we need to support some legacy syntax like "items = {{ item | item1 }}"
				// (i.e. without the ''). If "item" exists as template wiki page, it would
				// be included by the template resolver. The test is now that only those
				// items (without ') will be considered by this template resolver, which
				// contain a : (see WikimediaTest#testListParsingInSpecialWidget)
				// details are tracked in bug 10550
				if (!templateName.contains(":"))
					return null;				
				return super.resolveTemplate(namespace, templateName, templateParameters, page,
						parent);
			}        	
        };
        tplResolver.setIgnoreErrors(true);
        wikiModel.addTemplateResolver(tplResolver);  
        wikiModel.setUp();
        
        s = AbstractTemplateFunction.parseTrim(s, wikiModel);

        s = s.trim();
		String nestedInput = null;
		if (!s.startsWith( OPEN ) )
			return s;
		
		nestedInput = "{{ dummy |" + s.substring( 2 );
        wikiModel.render( nestedInput );
        return holder.value;
	}

	
	private static boolean isListParameterMap(Map<String, String> pars) {
		return pars.containsKey("1") && !isNullOrEmpty(pars.get("1").trim());
	}
	
	private static boolean nonEmpty(Object parsed) {
		return parsed != null && (!(parsed instanceof String) || !isNullOrEmpty((String)parsed));
	}
}