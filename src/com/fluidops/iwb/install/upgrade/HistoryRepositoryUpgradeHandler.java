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

package com.fluidops.iwb.install.upgrade;

import info.aduna.iteration.Iterations;

import java.util.List;

import org.apache.log4j.Logger;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

import com.fluidops.iwb.api.ReadWriteDataManagerImpl;
import com.fluidops.iwb.provider.ProviderUtils;
import com.google.common.collect.Lists;

/**
 * Upgrade procedures for the history repository
 * 
 * 1) Invalid value for property {@link #invalidSnapshotDateProperty} 
 *    => conversion "\"1351164988291\""^^xsd:integer to "1351164988291"^^xsd:integer
 * 
 * @author as
 *
 */
public class HistoryRepositoryUpgradeHandler implements UpgradeHandler {

	private static Logger logger = Logger.getLogger(HistoryRepositoryUpgradeHandler.class);
	
	static final URI invalidSnapshotDateProperty = ValueFactoryImpl.getInstance().createURI("http://www.fluidops.com/history/Snapshot/date");
	static final ValueFactory vf = ValueFactoryImpl.getInstance();
	
	@Override
	public void doUpgrade(UpgradeContext upgradeContext) {
		
		if (upgradeContext.getHistoryRepository()==null)
			return;		// no history repository available
		
		RepositoryConnection conn = null;
		try {
			
			conn = upgradeContext.getHistoryRepository().getConnection();
			
			upgradeInvalidSnapshotDateDatatype(conn);
		} catch (Exception e) {
			logger.warn("Error while upgrading historical repository. Problems need to be resolved manually. Details: "  + e.getMessage());
			logger.debug("Details", e);
		} finally {
			ReadWriteDataManagerImpl.closeQuietly(conn);
		}
	}
	
	/**
	 * Upgrade erroneous datatypes in particular triple patterns which were added
	 * in old versions of the platform.
	 * 
	 * Statements that are repaired here had a wrong data item in the object (note the " symbols)
	 * 
	 * <source>
	 * Subject <http://www.fluidops.com/history/Snapshot/date> "\"1351164988291\""^^xsd:integer
	 * </source>
	 * 
	 * All statements of this pattern are replaced with the corrected version
	 * 
	 * @param conn
	 * @throws QueryEvaluationException
	 * @throws RepositoryException
	 * @throws MalformedQueryException
	 */
	private void upgradeInvalidSnapshotDateDatatype(RepositoryConnection conn) throws QueryEvaluationException, RepositoryException, MalformedQueryException  {
		
		// handler for bug 10893
		
		logger.debug("Repairing invalid statements with property " + invalidSnapshotDateProperty);
		String query = "CONSTRUCT { ?s " + ProviderUtils.uriToQueryString(invalidSnapshotDateProperty) + " ?o } WHERE " +
				"{ ?s " + ProviderUtils.uriToQueryString(invalidSnapshotDateProperty) + " ?o . FILTER ( (isLiteral(?o)) && (datatype(?o)=xsd:integer) && (regex(str(?o), '^\".*') ) )   }";
	
		GraphQueryResult qRes = conn.prepareGraphQuery(QueryLanguage.SPARQL, query).evaluate();
		
		List<Statement> invalidStatements = Iterations.asList(qRes);
		if (invalidStatements.size()==0) {
			logger.debug("No invalid statements with property " + invalidSnapshotDateProperty + " found.");
			return;
		}
			
		logger.info("Found " + invalidStatements.size() + " invalid statements. Trying to repair." );
		
		List<Statement> repairedStatements = Lists.newArrayList();
		for (Statement st : invalidStatements) {
			String objStr = st.getObject().stringValue();
			objStr = removeEnclosingTicks(objStr);
			Statement repaired = vf.createStatement(st.getSubject(), st.getPredicate(),vf.createLiteral(objStr, XMLSchema.INTEGER), st.getContext());
			repairedStatements.add(repaired);
		}
		
		try {
			conn.begin();
			conn.add(repairedStatements);
			conn.remove(invalidStatements);
			conn.commit();
		} catch (Exception e) {
			conn.rollback();
		}
	}
	
	
	private static String removeEnclosingTicks(String serialized) {
		String token = serialized.startsWith("\"") ? serialized.substring(1) : serialized;
		token = token.endsWith("\"") ? token.substring(0, token.length()-1) : token;
		return token;
	}

}
