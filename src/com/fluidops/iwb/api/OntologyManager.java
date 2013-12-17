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

import org.openrdf.model.URI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

/**
 * Ontology management API
 * The API provides methods for loading, storing and removing ontologies to/from the RDF database
 * Ontologies are accessed via the OWLAPI
 * 
 * @author pha
 *
 */
public interface OntologyManager {
	/**
	 * @param ontologyURI
	 * @return
	 * @throws OWLOntologyCreationException
	 */
	OWLOntology loadOntology(URI ontologyURI)
			throws OWLOntologyCreationException;
	
	
	/**
	 * @param ontology, ontologyURI, overwrite
	 * @return 
	 * @throws OWLOntologyStorageException 
	 * @throws OWLOntologyCreationException 
	 */
	boolean storeOntology(OWLOntology ontology, URI ontologyURI, boolean overwrite) throws OWLOntologyStorageException, OWLOntologyCreationException;
	
	/**
	 * @param ontologyURI
	 * @return 
	 * @throws OWLOntologyStorageException 
	 */
	boolean removeOntology(URI ontologyURI) throws OWLOntologyStorageException;


}
