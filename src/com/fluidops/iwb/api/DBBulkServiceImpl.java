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

import static com.fluidops.iwb.api.ReadWriteDataManagerImpl.execute;
import static com.fluidops.util.StringUtil.replaceNonIriRefCharacter;
import static java.lang.String.format;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.openrdf.model.Literal;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.sail.memory.MemoryStore;

import com.fluidops.iwb.api.Context.ContextLabel;
import com.fluidops.iwb.api.Context.ContextType;
import com.fluidops.iwb.api.ReadWriteDataManagerImpl.ReadWriteDataManagerCallback;
import com.fluidops.iwb.api.ReadWriteDataManagerImpl.ReadWriteDataManagerVoidCallback;
import com.fluidops.iwb.model.Vocabulary;
import com.fluidops.iwb.model.Vocabulary.SYSTEM_ONTOLOGY;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

public class DBBulkServiceImpl implements DBBulkService
{
    private static final ValueFactory valueFactory = ValueFactoryImpl.getInstance();
    private static final Logger logger = Logger.getLogger(DBBulkServiceImpl.class);
    private final Supplier<Repository> repository;

    private static final Pattern VERSION_IRI_PATTERN = Pattern.compile(".*(\\d+)");


    public DBBulkServiceImpl(final Repository repository)
    {
        this(Suppliers.ofInstance(repository));
    }
    
    /**
	 * @param repository
	 *            A {@link Supplier} for the {@link Repository}. This allows for
	 *            lazy initialization, if the {@link DBBulkService} needs to be
	 *            constructed before the {@link Repository} is actually
	 *            available.
	 */
    public DBBulkServiceImpl(Supplier<Repository> repository) {
		this.repository = repository;
	}

	@Override
    public void updateOntology(final File ontologyFile)
    {
        execute(repository.get(), new ReadWriteDataManagerVoidCallback()
        {
            @Override
            public void doWithDataManager(ReadWriteDataManager dataManager)
            {
            	assert dataManager.getRepository() != null;

            	/*
            	 * Check the versioning mechanisms using, in order:
            	 * 1) Version IRI
            	 * 2) Version INFO
            	 */
            	URI [] versionPricateURIs = new URI[] { Vocabulary.OWL.VERSION_IRI, Vocabulary.OWL.VERSION_INFO };
            	Literal dbVersion = versionInDb(dataManager, filenameToOntologyUri(ontologyFile), versionPricateURIs);

            	boolean updateOntology = false;
            	if (dbVersion == null) {
            		/*
            		 * The DB does not contain versioning info.
            		 * Let's accept whatever is being thrown at it.
            		 */
            		updateOntology = true;
            	} else {
            		/*
            		 * We have versioning info in the DB; we will update only if
            		 * the version of the ontology on file is "newer".
            		 */
            		Literal fileVersion = versionInFile(ontologyFile, versionPricateURIs);

            		if (fileVersion == null) {
            			/*
            			 * The file contains no version information.
            			 * We cannot risk to corrupt the database.
            			 */
            			updateOntology = false;
    	                logger.error("Ignoring ontology '" + ontologyFile + " because it lacks versioning information and the database instead has it");
            		} else {
            			/*
            			 * Here be Dragons! We are potentially mixing two different
            			 * versioning schemes, namely Version IRI and Version INFO.
            			 * 
            			 * Version INFO is a progressive Integer scheme.
            			 * Version IRI is, well, an IRI in the form:
            			 * 
            			 *   &fluidops;ontologies/ecloud/[yyyymmdd]
            			 * 
            			 * That is, the last segment of the IRI is an integer that
            			 * encodes a date. To keep the comparison consistent,
            			 * we are transforming the IRI into the integer that
            			 * encodes the date, and compare it with the other integers.
            			 * 
            			 * (It is anyways unlikely that an incremental versioning
            			 * scheme like Version INFO has gone beyond 20.0120.000 iterations,
            			 * so when mixing the newer Version IRI with the others, the
            			 * former should take precedence.)
            			 */
            			
            			try {
            				/*
            				 * First, try to compare them as numbers; this
            				 * is the case when both use the VERSION_INFO scheme.
            				 */
            				updateOntology = dbVersion.intValue() < fileVersion.intValue();
            			} catch (NumberFormatException e) {
            				/*
            				 * Try comparison assuming both use Version IRI
            				 */
            				int dbVersionNumber = extractIntegerVersionNumber(dbVersion),
            					fileVersionNumber = extractIntegerVersionNumber(fileVersion);
            				
                			if (fileVersionNumber > dbVersionNumber) {
                				/*
                				 * Update only if the file version is strictly higher
                				 * than the DB one.
                				 */
                				updateOntology = true;
                			}
            			}
            			
            			if (!updateOntology) {
            				logger.debug("Ignoring ontology '" + ontologyFile + " because its version is not newer than the DB's");
            			}
            		}
            	}

            	if (updateOntology) {
	                logger.info("Trying to load/update ontology '" + ontologyFile + "' into DB...");
	                dataManager.updateDataForSrc(filenameToContextUri(ontologyFile), null, ContextType.SYSTEM,
	                        ContextLabel.ONTOLOGY_IMPORT, RDFFormat.RDFXML, ontologyFile, null);
            	}
            }

			private int extractIntegerVersionNumber(Literal version) {
				try {
					/*
					 * Let's try the happy case (the literal
					 * is actually a number) first
					 */
					return version.intValue();
				} catch (NumberFormatException e) {
					/*
					 * OK, this must be an IRI then.
					 */
					String uri = version.stringValue();
					/*
					 * Find the sequence of digits at the end
					 * of the string and return it parsed to
					 * an int.
					 */
					Matcher m = VERSION_IRI_PATTERN.matcher(uri);

					if (!m.matches()) {
						throw new IllegalArgumentException("The version literal is neither a number, nor an IRI that ends with a [yyyymmdd] pattern");
					}

					return Integer.parseInt(m.group(1));
				}
			}
        });
        // shouldnt we update the keyword index as well?
    }

    @Override
    public void bootstrapDB(final File bootstrapFile)
    {
        logger.info("Trying to load/update file '" + bootstrapFile + "' into DB...");
        final URI sourceURI = valueFactory.createURI("urn:bootstrap-" + bootstrapFile.getName());

        try {
	        execute(repository.get(), new ReadWriteDataManagerCallback<Context>()
	        {
	            @Override
	            public Context callWithDataManager(ReadWriteDataManager dataManager)
	            {
	                Context newContext = dataManager.updateDataForSrc(sourceURI, null, Context.ContextType.SYSTEM,
	                        ContextLabel.RDF_IMPORT, null, bootstrapFile, null);
	                dataManager.calculateVoIDStatistics(newContext.getURI());
	                return newContext;
	            }
	        });
        } catch (RuntimeException e) {
        	// wrap exception in a more helpful text, bug 9577
        	throw new RuntimeException("Error loading RDF file " + bootstrapFile + ": " + e.getMessage(), e);
        }
    }
    
    @Override
    public void bootstrapDBAndRemove(File dbFile)
    {
        bootstrapDB(dbFile);
        if (!dbFile.delete()) {
            logger.info(format("Cannot delete '%s' after successful import."
                    + " Remove manually or it will be imported again.", dbFile));
        }
    }
    
    private Literal versionInFile(File ontologyFile, URI ... versionPredicateURIs)
    {
        Repository tmpRepository = null;
        RepositoryConnection tmpConnnection = null;
        RepositoryResult<Statement> results = null;
        try
        {
            tmpRepository = newMemoryRepository();
            tmpConnnection = tmpRepository.getConnection();
            tmpConnnection.add(ontologyFile, null, RDFFormat.RDFXML);

            for (URI versionPredicateURI : versionPredicateURIs) {
            	try {
            		results = tmpConnnection.getStatements(
            				filenameToOntologyUri(ontologyFile), versionPredicateURI, null, false);
            		if (results.hasNext()) {
            			/*
            			 * We have a match!
            			 */
            			return (Literal) results.next().getObject();
            		}
            	} finally {
                    closeQuietly(results);
            	}
            }

            /*
             * None of the version predicates has been found
             */
            return null;
        }
        catch (Exception ex)
        {
        	throw new RuntimeException(ex);
        }
        finally
        {
            ReadWriteDataManagerImpl.closeQuietly(tmpConnnection);
            shutdownQuietly(tmpRepository);
        }
    }

    private void closeQuietly(RepositoryResult<Statement> statements)
    {
        if(statements != null) {
            try
            {
                statements.close();
            }
            catch (RepositoryException e)
            {
                // ignore
            }
        }
    }

    private Repository newMemoryRepository() throws RepositoryException
    {
        Repository tmpRepository = new SailRepository(new MemoryStore());
        tmpRepository.initialize();
        return tmpRepository;
    }

    private void shutdownQuietly(Repository tmp)
    {
        try
        {
            if(tmp!= null) tmp.shutDown();
        }
        catch (RepositoryException e)
        {
        }
    }

    private Literal versionInDb(ReadWriteDataManager dataManager, URI ontologyUri, URI ... versionPredicateURIs)
    {
    	Literal version = null;
    	for (URI versionPredicateURI : versionPredicateURIs) {
    		Statement versionStmt = dataManager.searchOne(ontologyUri, versionPredicateURI, null);
    		if (versionStmt == null) {
    			/*
    			 * There is no predicate matching this URI
    			 */
    			continue;
    		}

    		/*
    		 * Version found
    		 */
    		version = (Literal) versionStmt.getObject();
    	}

    	return version;
    }

    private URI filenameToOntologyUri(File ontologyFile)
    {
        return valueFactory.createURI(replaceNonIriRefCharacter(SYSTEM_ONTOLOGY.ONTOLOGY_NAME_PREFIX + ontologyFile.getName(), '_'));
    }

    private URI filenameToContextUri(File ontologyFile)
    {
        return valueFactory.createURI(replaceNonIriRefCharacter(SYSTEM_ONTOLOGY.ONTOLOGY_CONTEXT_PREFIX + ontologyFile.getName(), '_'));
    }

    @Override
    public void bootstrapDBAllFrom(File dir)
    {
        bootstrapDBAllFrom(dir, false);
    }

    @Override
    public void bootstrapDBAllFromAndRemove(File dir)
    {
        bootstrapDBAllFrom(dir, true);
    }
    
    private void bootstrapDBAllFrom(File dir, boolean removeAfterImport)
    {
        if(!dir.exists()) return;
        File[] filesToBootstrap = dir.listFiles();
        if(filesToBootstrap == null) throw new IllegalStateException(dir + " is not a readable directory"); 
        for (File dbFile : filesToBootstrap)
        {
            if(dbFile.isFile()) {
                if(removeAfterImport) bootstrapDBAndRemove(dbFile); else bootstrapDB(dbFile);
            }
        }
    }
}

