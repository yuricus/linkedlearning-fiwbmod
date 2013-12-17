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

package com.fluidops.iwb.api.query;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.query.algebra.Avg;
import org.openrdf.query.algebra.Count;
import org.openrdf.query.algebra.ExtensionElem;
import org.openrdf.query.algebra.FunctionCall;
import org.openrdf.query.algebra.GroupConcat;
import org.openrdf.query.algebra.Projection;
import org.openrdf.query.algebra.ProjectionElem;
import org.openrdf.query.algebra.StatementPattern;
import org.openrdf.query.algebra.Sum;
import org.openrdf.query.algebra.ValueConstant;
import org.openrdf.query.algebra.Var;
import org.openrdf.query.algebra.helpers.QueryModelVisitorBase;
import org.openrdf.query.parser.ParsedOperation;
import org.openrdf.query.parser.ParsedTupleQuery;

import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.ReadDataManagerImpl;
import com.fluidops.iwb.cache.PropertyCache.PropertyInfo;

/**
 * A wrapper for a SPARQL tuple query which tries to guess the 
 * datatypes of the returned query variables.
 * 
 * @author andriy.nikolov
 *
 */
public class QueryAnalyzeUtil {
	
	private static final Logger logger = Logger.getLogger(QueryAnalyzeUtil.class);
	
	/**
	 * No actual instantiation is expected.
	 */
	private QueryAnalyzeUtil() {
		
	};
	
	/**
	 * Parses the query and tries to guess the datatypes of variables in the query.
	 * Checks for explicit casts and domains/ranges defined in the ontology.
	 */
	public static Map<String, QueryFieldProfile> guessVariableTypesFromQueryString(String queryString) {
		DatatypeGuessingTupleQueryVisitor queryVisitor = new DatatypeGuessingTupleQueryVisitor();
		
		try {
			
			ParsedOperation parsedOperation = ReadDataManagerImpl.parseQuery(queryString, true);
			
			if(parsedOperation instanceof ParsedTupleQuery) {
				((ParsedTupleQuery) parsedOperation).getTupleExpr().visit(queryVisitor);
				
				return guessVariableTypes(queryVisitor);
			} else {
				logger.warn("Cannot determine variable datatypes: possibly wrong query type");
			}
			
		} catch(Exception e) {
			logger.error("Query processing error: "+e.getMessage()+" : \n"+queryString);
		}
		
		return new HashMap<String, QueryFieldProfile>();
		
	}
	
	
	/**
	 * Tries to guess the datatypes of variables in the query.
	 * Checks for explicit casts and domains/ranges defined in the ontology.
	 */
	private static Map<String, QueryFieldProfile> guessVariableTypes(DatatypeGuessingTupleQueryVisitor queryVisitor) {
		
		Map<String, QueryFieldProfile> mapQueryFieldProfiles = new HashMap<String, QueryFieldProfile>();
		
		QueryFieldProfile profile;
		
		for(String var : queryVisitor.projectedVariables) {
			
			profile = new QueryFieldProfile(var);
			
			mapQueryFieldProfiles.put(var, profile);
			
			guessVariableType(profile, var, queryVisitor);
		}
		
		return mapQueryFieldProfiles;
	}
	
	/**
	 * The method tries to guess the datatype of the variable, for which the profile is provided.
	 * 
	 * @param profile The profile of the variable, for which the datatype should be found. 
	 * @param aliasVar Alias name of the variable, for which the datatype should be found (either the variable name itself, or the variable bound to it)
	 * @param queryVisitor 
	 */
	private static void guessVariableType(QueryFieldProfile profile, String aliasVar, DatatypeGuessingTupleQueryVisitor queryVisitor) {
		if(queryVisitor.mapCastDatatypes.containsKey(aliasVar)) {
			profile.setExplicitDatatype(queryVisitor.mapCastDatatypes.get(aliasVar));
		} else if(queryVisitor.mapDomains.containsKey(aliasVar)) {
			// A variable which appears in the subject position in any triple pattern 
			// would normally represent a resource 
			profile.containsOnlyResources = true;
		} else if(queryVisitor.mapRanges.containsKey(aliasVar)) {
			findVariablePropertyRange(profile, aliasVar, queryVisitor);
		} else if(queryVisitor.mappedVariables.containsKey(aliasVar)) {
			guessVariableType(profile, queryVisitor.mappedVariables.get(aliasVar), queryVisitor);
		}
		
	}
	
	private static void findVariablePropertyRange(QueryFieldProfile profile, String aliasVar, DatatypeGuessingTupleQueryVisitor queryVisitor) {
		
		try {
			ReadDataManagerImpl dm = EndpointImpl.api().getDataManager();
			
			Set<URI> predicates = queryVisitor.mapRanges.get(aliasVar);
			List<URI> ranges;
			
			PropertyInfo predicateInfo;
			if(predicates!=null) {
				for(URI uri : predicates) {
					predicateInfo = dm.getPropertyInfo(uri);
					
					if(predicateInfo.isKnownDatatypeProperty()) {
						ranges = predicateInfo.getRan();
						
						for(URI datatypeRange : ranges) {
							profile.setExplicitDatatype(datatypeRange);
						}
						
					}
					
				}
			}
			
		} catch(Exception e) {
			logger.error("Error accessing the data manager: "+e.getMessage());
			logger.debug("Details: ", e);
		}
		
	}
	
	/**
	 * Visitor class responsible for parsing the tuple query, determining the list of returned variables, 
	 * datatypes to which they are explicitly cast, as 
	 * well as RDF properties which have them as subjects or objects. This information is then used to determine 
	 * the list of widgets which can visualise the query results (in AdHocSearchResultsWidgetSelector).
	 * 
	 * @author andriy.nikolov
	 *
	 */
	private static class DatatypeGuessingTupleQueryVisitor extends QueryModelVisitorBase<RuntimeException> {

		public Set<String> projectedVariables = new HashSet<String>();
		public Map<String, String> mappedVariables = new HashMap<String, String>();
		public Map<String, Set<URI>> mapDomains = new HashMap<String, Set<URI>>();
		public Map<String, Set<URI>> mapRanges = new HashMap<String, Set<URI>>();
		public Map<String, URI> mapCastDatatypes = new HashMap<String, URI>();
		
		private ValueFactory vf = ValueFactoryImpl.getInstance();
		private boolean topLevelProjectionProcessed = false;  

		@Override
		public void meet(StatementPattern node) throws RuntimeException {
			
			Var predVar = node.getPredicateVar();

			if(predVar.getValue() instanceof URI) {
				Var objVar = node.getObjectVar();
				if(!objVar.hasValue()) {
					addPredicateToMap(objVar.getName(), (URI)predVar.getValue(), mapRanges);
				}
				
				Var subjVar = node.getSubjectVar();
				if(!subjVar.hasValue()) {
					addPredicateToMap(subjVar.getName(), (URI)predVar.getValue(), mapDomains);
				}
			}
			
			super.meet(node);
		}
		
		
		
		/* (non-Javadoc)
		 * @see org.openrdf.query.algebra.helpers.QueryModelVisitorBase#meet(org.openrdf.query.algebra.Projection)
		 */
		@Override
		public void meet(Projection node) throws RuntimeException {
			node.getProjectionElemList().visit(this);
			topLevelProjectionProcessed = true;
			node.getArg().visit(this);
		}



		@Override
		public void meet(ProjectionElem node) throws RuntimeException {
			if(!topLevelProjectionProcessed)
				projectedVariables.add(node.getTargetName());
			
			super.meet(node);
		}

		@Override
		public void meet(ExtensionElem node) throws RuntimeException {
			if(node.getExpr() instanceof Var) {
				Var var = (Var)node.getExpr();
				if(var.hasValue()) {
					addCastDatatypeFromValue(node.getName(), var.getValue());
				} else if(!node.getName().equals(var.getName())) {
					this.mappedVariables.put(node.getName(), var.getName());
				}
			} else if(node.getExpr() instanceof ValueConstant) {
				ValueConstant c = (ValueConstant)node.getExpr();
				addCastDatatypeFromValue(node.getName(), c.getValue());
			} else if(node.getExpr() instanceof FunctionCall) {
				FunctionCall callNode = ((FunctionCall)node.getExpr());
				if(callNode.getURI().startsWith(XMLSchema.NAMESPACE)) {
					this.mapCastDatatypes.put(
							node.getName(), 
							vf.createURI(
									callNode.getURI()));
				} 
			} else if(node.getExpr() instanceof Count) {
				this.mapCastDatatypes.put(
						node.getName(), 
						XMLSchema.INTEGER);
			} else if(node.getExpr() instanceof Avg) {
				// Assuming that no integer average is possible
				this.mapCastDatatypes.put(
						node.getName(), 
						XMLSchema.DOUBLE);
			} else if(node.getExpr() instanceof Sum) {
				// Even if the variable is an integer, applying SUM indicates that 
				// it is intended to represent quantity rather than serve as an id
				this.mapCastDatatypes.put(
						node.getName(), 
						XMLSchema.DOUBLE);
			} else if(node.getExpr() instanceof GroupConcat) {
				this.mapCastDatatypes.put(
						node.getName(), 
						XMLSchema.STRING);
			}
			
			super.meet(node);
		}

		private void addPredicateToMap(String varName, URI predicate, Map<String, Set<URI>> map) {
			
			Set<URI> set;
			
			if(map.containsKey(varName)) {
				set = map.get(varName);
			} else {
				set = new HashSet<URI>();
				map.put(varName, set);
			}
			set.add(predicate);
		}
		
		private void addCastDatatypeFromValue(String varName, Value val) {
			if(val instanceof URI) {
				this.mapCastDatatypes.put(
						varName, 
						RDFS.RESOURCE);
			} else if(val instanceof Literal) {
				Literal lit = (Literal)val;
				this.mapCastDatatypes.put(varName, 
						(lit.getDatatype()==null) ? RDFS.LITERAL : lit.getDatatype());
			}
		}

	}
	

}
