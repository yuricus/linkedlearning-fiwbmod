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

package com.fluidops.iwb.api;

import java.io.ByteArrayOutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.Rio;
import org.openrdf.sail.memory.MemoryStore;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.io.StringDocumentTarget;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import com.fluidops.iwb.Global;
import com.fluidops.iwb.api.Context.ContextLabel;


/**
 * @author pha
 *
 */
public class OntologyManagerImpl implements OntologyManager {

	/**
	 * Logger associated to the {@link OntologyManagerImpl} class.
	 */
    protected static final Logger logger = 
    		Logger.getLogger(OntologyManagerImpl.class.getName());
	
    /**
     * To be used instead of the constructor. 
     */
    public static OntologyManager getOntologyManager()
    {

    	OntologyManager om = new OntologyManagerImpl();
        return om;
    }
	
	@Override
	public OWLOntology loadOntology(URI ontologyURI) throws OWLOntologyCreationException {
		
		OWLOntology o = null;
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

        ReadDataManager dm = EndpointImpl.api().getDataManager();
        
        Statement ontologyDeclaration = dm.searchOne(ontologyURI, RDF.TYPE, OWL.ONTOLOGY);
        if(ontologyDeclaration==null)
        {
        	logger.error("Ontology "+ontologyURI+" does not exist.");
        	return null;
        	
        }
        	
        RepositoryResult<Statement> ontologyStmts = null;
        Repository tmpRepository = null;
        RepositoryConnection tmpConnnection = null;
        
        try {
        	ontologyStmts = dm.getStatements(null, null, null, false, ontologyDeclaration.getContext());

            tmpRepository = new SailRepository(new MemoryStore());
            tmpRepository.initialize();
            tmpConnnection = tmpRepository.getConnection();
            tmpConnnection.add(ontologyStmts);

            ByteArrayOutputStream out = new ByteArrayOutputStream(); 
            RDFWriter writer = Rio.createWriter(RDFFormat.RDFXML, out);           

            tmpConnnection.export(writer);
            
        	o = manager.loadOntologyFromOntologyDocument(new StringDocumentSource(out.toString()));
        	tmpConnnection.close();
        	tmpRepository.shutDown();
           	//manager.loadOntologyFromOntologyDocument(new DataManagerOntologySource(ontologyStmts, ontologyURI));

        } catch (RepositoryException e) {
        	throw new RuntimeException(e);
        }
        catch (RDFHandlerException e) {
        	throw new RuntimeException(e);
        }
        finally {
        	ReadDataManagerImpl.closeQuietly(ontologyStmts);
        	ReadWriteDataManagerImpl.closeQuietly(tmpConnnection);
        }

        return o;
	}

	
	@Override
	public boolean storeOntology(OWLOntology ontology, URI ontologyURI, boolean overwrite) throws OWLOntologyStorageException, OWLOntologyCreationException {

		
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		ReadWriteDataManager dm = null;
		try {
			manager.createOntology(IRI.create(ontologyURI));
			StringDocumentTarget target = new StringDocumentTarget();
			manager.saveOntology(ontology, target);

			dm =  ReadWriteDataManagerImpl.openDataManager(Global.repository);

			dm.importRDFfromInputStream(IOUtils.toInputStream(target.toString()), ontologyURI.toString(), RDFFormat.RDFXML, 
					Context.getFreshPublishedContext(Context.ContextType.USER, ontologyURI, ContextLabel.ONTOLOGY_IMPORT));

		}
		finally {
			ReadWriteDataManagerImpl.closeQuietly(dm);
		}
		
		return true;
	}

	
	@Override
	public boolean removeOntology(URI ontologyURI) {

		ReadWriteDataManager dm = null;
		
		try {

			dm =  ReadWriteDataManagerImpl.openDataManager(Global.repository);

			Statement ontologyDeclaration = dm.searchOne(ontologyURI, RDF.TYPE, OWL.ONTOLOGY);

			if(ontologyDeclaration==null)
			{
				logger.error("Ontology "+ontologyURI+" does not exist.");
				return false;
			}
			dm.deleteContextById(ontologyDeclaration.getContext());
		} 
		finally {
			ReadWriteDataManagerImpl.closeQuietly(dm);
		}
        
        return true;
	}

}
