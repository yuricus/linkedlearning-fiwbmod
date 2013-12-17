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

package com.fluidops.iwb.tools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.openrdf.model.URI;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.Update;
import org.openrdf.query.UpdateExecutionException;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;

import com.fluidops.iwb.api.ReadDataManagerImpl;
import com.fluidops.iwb.api.ReadWriteDataManagerImpl;
import com.fluidops.iwb.api.solution.SolutionService;
import com.fluidops.iwb.util.Config;
import com.fluidops.iwb.util.IWBFileUtil;
import com.fluidops.util.GenUtil;
import com.fluidops.util.logging.Log4JHandler;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;

/**
 * Repository tool providing various operations on the repository level.
 * 
 * Synopsis:
 * 
 * <source>
    usage: repotool [OPTIONS] [SOURCE [TARGET]]

	Tool for different repository level operations (analysis, repair,
	cleanup). The first argument (if given) corresponds to the source path of
	the triple store (default is taken from configuration), the second
	(optional) argument refers to the target destination. If no target is
	specified the repository operation is done in place, with a backup of the
	source being created to %source%.bak.
	Options:
	 -a,--analyze         analyze the repository and print results
	 -c,--cleanup         cleanup of the repository (assumes a non-corrupt
	                      repository). This operation potentially reduces the
	                      size of the repository.
	 -f,--analyzeAndFix   analyze the repository, print results and try to
	                      recover (if problems are detected)
	 -g,--garbageCheck    check for garbage data due to inconsistencies in the
	                      triple store
     -i,--indices <nativeStoreIndices>   the assumed native store indices,
         				  e.g. 'spoc,psoc'
	 -h,--help            print this message
	Examples
	repotool -a -g
	repotool -a data/dbmodel-broken
	repotool -f data/dbmodel-broken
	repotool -f -g data/dbmodel-broken data/dbmodel-fixed
	repotool -a -i cspo,cpso,cops data/historymodel
	
 * </source>
 * 
 * @author as
 *
 */
public class RepositoryTool {
	
	
	private static Logger logger = Logger.getLogger(SolutionService.INSTALL_LOGGER_NAME);
	
	
	/**
	 * Analyzes the given repository at {@link #dbModelFolder} without checking for garbage
	 * 
	 * 1) check if repository is broken with {@link #defaultNativeStoreIndices}
	 * 2) check all individual indices of {@link #getDefaultNativeStoreIndices()}
	 * 3) print analysis results (information about repository state)
	 * 
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static void analyze(File dbModelFolder) throws FileNotFoundException, IOException {
		analyze(dbModelFolder, false, getDefaultNativeStoreIndices());
	}
	
	/**
	 * Analyzes the given repository at {@link #dbModelFolder}. Depending on checkGhostContext
	 * the ghost contexts are analyzed.
	 * 
	 * 1) check if repository is broken with provided nativeStoreIndices
	 * 2) check all individual indices of provided nativeStoreIndices
	 * 3) print analysis results (information about repository state)
	 * 
	 * @param dbModelFolder
	 * @param checkGhostContext
	 * @param nativeStoreIndices the nativeStoreIndices to use for access, e.g. "spoc,psoc"
	 * 
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static void analyze(File dbModelFolder, boolean checkGhostContext, String nativeStoreIndices) throws FileNotFoundException, IOException {
		analyzeAndFixInternal(dbModelFolder, dbModelFolder, false, checkGhostContext, nativeStoreIndices);
	}

	
	/**
	 * Analyzes the given repository at {@link #dbModelFolder} and tries to
	 * recover in case of corruptness. Does not check for ghost context.
	 * 
	 * 1) check if repository is broken with {@link #defaultNativeStoreIndices}
	 * 2) check all individual indices of {@link #getDefaultNativeStoreIndices()}
	 * 3) print analysis results (information about repository state)
	 * 4) if there is a problem, try to recover from the non-broken index
	 *    using {@link #rebuildRepository(File, String)}
	 *    	 	 
	 * @param dbModelFolder the location of the native store
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static void analyzeAndFix(File dbModelFolder) throws FileNotFoundException, IOException {				
		analyzeAndFix(dbModelFolder, false);		
	}
	
	/**
	 * Analyzes the given repository at {@link #dbModelFolder} and tries to
	 * recover in case of corruptness. Depending on checkGhostContext
	 * the ghost contexts are analyzed.
	 * 
	 * 1) check if repository is broken with {@link #getDefaultNativeStoreIndices()}
	 * 2) check all individual indices of {@link #getDefaultNativeStoreIndices()}
	 * 3) print analysis results (information about repository state)
	 * 4) if there is a problem, try to recover from the non-broken index
	 *    using {@link #rebuildRepository(File, String)}
	 * 
	 * @param dbModelFolder the location of the native store
	 * @param checkGhostContext flag indicating whether ghost contexts are checked
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static void analyzeAndFix(File dbModelFolder, boolean checkGhostContext) throws FileNotFoundException, IOException {				
		analyzeAndFix(dbModelFolder, dbModelFolder, checkGhostContext, getDefaultNativeStoreIndices());		
	}
	
	/**
	 * Analyzes the given repository at {@link #dbModelFolder} and tries to
	 * recover in case of corruptness. Depending on checkGhostContext
	 * the ghost contexts are analyzed.
	 * 
	 * 1) check if repository is broken with provided nativeStoreIndices
	 * 2) check all individual indices of provided nativeStoreIndices
	 * 3) print analysis results (information about repository state)
	 * 4) if there is a problem, try to recover from the non-broken index
	 *    using {@link #rebuildRepository(File, String)}
	 * 
	 * @param dbModelFolder the location of the native store
	 * @param checkGhostContext flag indicating whether ghost contexts are checked
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static void analyzeAndFix(File dbModelFolder, File targetFolder, boolean checkGhostContext, String nativeStoreIndices) throws FileNotFoundException, IOException {				
		analyzeAndFixInternal(dbModelFolder, targetFolder, true, checkGhostContext, nativeStoreIndices);		
	}
	
	/**
	 * Rebuilds/cleans-up the repository by performing the following steps:
	 * 
	 * 1) Open the original repository from dbModelFolder
	 * 2) Open a fresh target repository at dbModelFolder.tmp using nativeStoreIndices
	 * 3) Copy the content from source repository to target repository
	 * 4) Move original repository to dbModelFolder.bak
	 * 5) Move fresh target repository to dbModelFolder
	 * 
	 * In case of an error during a physical I/O operation (i.e. moving the original folder, deleting
	 * the temporary repository) an IOException is thrown. The original repository remains
	 * untouched, the target repository may have created files in dbModelFolder.tmp (which
	 * need to be deleted manually)
	 * 
	 * @param dbModelFolder the location of the native store
	 * @throws IOException
	 */
	public static void cleanupRepository(File dbModelFolder) throws IOException {
		
		cleanupRepository(dbModelFolder, dbModelFolder);
	}
	
	/**
	 * Rebuilds/cleans-up the repository by performing the following steps:
	 * 
	 * 1) Open the original repository from dbModelFolder
	 * 2) Open a fresh target repository at dbModelFolder.tmp using {@link #getDefaultNativeStoreIndices()}
	 * 3) Copy the content from source repository to target repository
	 * 4) Leaves original repository intact
	 * 5) Move fresh target repository to target
	 * 
	 * In case of an error during a physical I/O operation (i.e. moving the original folder, deleting
	 * the temporary repository) an IOException is thrown. The original repository remains
	 * untouched, the target repository may have created files in dbModelFolder.tmp (which
	 * need to be deleted manually)
	 * 
	 * @param dbModelFolder the location of the native store
	 * @throws IOException
	 */
	public static void cleanupRepository(File dbModelFolder, File target) throws IOException {		
		
		cleanupRepository(dbModelFolder, target, getDefaultNativeStoreIndices());
	}
	
	/**
	 * Rebuilds/cleans-up the repository by performing the following steps:
	 * 
	 * 1) Open the original repository from dbModelFolder
	 * 2) Open a fresh target repository at dbModelFolder.tmp using the provided native store indices
	 * 3) Copy the content from source repository to target repository
	 * 4) Leaves original repository intact
	 * 5) Move fresh target repository to target
	 * 
	 * In case of an error during a physical I/O operation (i.e. moving the original folder, deleting
	 * the temporary repository) an IOException is thrown. The original repository remains
	 * untouched, the target repository may have created files in dbModelFolder.tmp (which
	 * need to be deleted manually)
	 * 
	 * @param dbModelFolder the location of the native store
	 * @throws IOException
	 */
	public static void cleanupRepository(File dbModelFolder, File target, String nativeStoreIndices) throws IOException {
		
		print("Cleaning up repository at " + dbModelFolder.getPath());
		
		// precondition: check whether the repository is accessible
		// if not: the appropriate exception is thrown
		checkRepositoryAccessible(dbModelFolder, nativeStoreIndices);
		
		rebuildRepository(dbModelFolder, target, nativeStoreIndices, nativeStoreIndices);
	}
	
	private static void analyzeAndFixInternal(File dbModelFolder, File targetFolder, boolean fixMode, boolean checkGhostContexts, String nativeStoreIndices) throws FileNotFoundException, IOException {
		
		print("Analyzing repository at " + dbModelFolder.getPath());
		
		// precondition: check whether the repository is accessible
		// if not: the appropriate exception is thrown
		checkRepositoryAccessible(dbModelFolder, nativeStoreIndices);
		
		RepositoryState stateDefault;
		
		// Step 1: check if repository is broken with default indices
		resetIndexConfiguration(dbModelFolder);
		print("1) Checking repository state with default indices (" + nativeStoreIndices + ")");
		stateDefault = checkRepositoryState(dbModelFolder, nativeStoreIndices, checkGhostContexts);
		
		// individual indices
		String[] indices = nativeStoreIndices.split(",");
		List<RepositoryState> indexStates = Lists.newArrayList();
		
		// Step 2: check with all individual indices
		print("2) Checking repository state with individual indices");
		for (String index : indices) {
			resetIndexConfiguration(dbModelFolder);
			print(" Checking " + index + " index");
			RepositoryState s = checkRepositoryState(dbModelFolder, index, stateDefault.isCorrupt() && checkGhostContexts);
			indexStates.add(s);
		}
		
		// delete the index configuration, in case the repository is reused late on
		resetIndexConfiguration(dbModelFolder);
		
		// Step 3: print analysis results
		print("3) Analysis result:");
		boolean repositoryCorrupt = false;
		boolean allIndicesCorrupt = false;
		if (stateDefault.isCorrupt()) {
			repositoryCorrupt = true;
			print(" * Repository indices are corrupt: " + stateDefault.getCorruptMessage());
			allIndicesCorrupt = true;
			for (RepositoryState indexState : indexStates) {
				if (!indexState.isCorrupt()) {
					allIndicesCorrupt = false;
					continue;
				}
				print(" * " + indexState.getIndices() + " index is corrupt: " + indexState.getCorruptMessage());
			}			
			if (allIndicesCorrupt)
				print(" * All available indices are corrupt, repository cannot be repaired.");			
		}
						
		// check the size of the individual index configurations
		long size = stateDefault.getSize();		
		for (RepositoryState indexState : indexStates) {
			if (size!=indexState.getSize()) {
				repositoryCorrupt = true;
				print(" * Repository indices are out of synch: size of at least one index configuration differs");
				break;
			}
			size = indexState.getSize();
		}
		
		// check for ghost contexts
		boolean isAffectedByGhosts = false;
		for (RepositoryState indexState : Lists.asList(stateDefault, indexStates.toArray(new RepositoryState[0]))) {
			if (indexState.isAffectedByGhosts()){
				repositoryCorrupt = true;
				isAffectedByGhosts = true;
				print(" * Repository is affected by inconsistent index states: " + stateDefault.getNumberOfGhosts() + " garbage contexts");
				print("   See Sesame bug SES-1867 for further details");
				break;
			}
		}
		
		// try to find a non-corrupt index, take the one with largest size
		String goodIndex = null;
		long goodIndexSize = -1;
		for (RepositoryState indexState : indexStates) {
			if (!indexState.isCorrupt() && indexState.size>goodIndexSize) {
				goodIndex = indexState.getIndices();
				goodIndexSize = indexState.getSize();
			}
		}
		
		if (!repositoryCorrupt) {
			if (checkGhostContexts)
				print(String.format(" * Repository is healthy (No garbage contexts detected, size: %s)", stateDefault.getSize()));
			else
				print(String.format(" * Repository is healthy (No checks for garbage contexts, size: %s)", stateDefault.getSize()));
			return;
		} else if (!allIndicesCorrupt){
			print(" * Index " + goodIndex + " can be used for recovery process.");
		}
		
		// Step 5: if desired automatically fix
		if (!fixMode)
			return;
		
		System.out.println("4) Trying to repair database");
		if (allIndicesCorrupt) {
			print(" * Cannot recover database, all indices are corrupt.");
			throw new IllegalStateException("Database cannot be automatically recovered, all indices are corrupt.");
		}		
				
		print("Trying to recover database using " + goodIndex + " index");
		if (isAffectedByGhosts) 
			print("(Note: garbage contexts arising through invalid data states will be removed in this process)");
		
		try {
			rebuildRepository(dbModelFolder, targetFolder, goodIndex, nativeStoreIndices);
		} catch (IllegalStateException e) {
			// try to find another good context, if available (and retry)
			String secondIndex = null;
			for (RepositoryState indexState : indexStates) {
				if (!indexState.isCorrupt() && goodIndex.equals(indexState.getIndices())) {
					secondIndex = indexState.getIndices();
				}
			}
			if (secondIndex==null) 
				throw e;			
			
			// retry with secondIndex (delete temp files first)
			print("Retrying with second good index: " + secondIndex);
			File targetRepoTmpFolder = new File(dbModelFolder.getParentFile(), dbModelFolder.getName() + ".tmp");
			FileUtils.deleteDirectory(targetRepoTmpFolder);
			rebuildRepository(dbModelFolder, targetFolder, secondIndex, nativeStoreIndices);
		}
		
		// if the repository was affected by ghosts: remove them
		if (checkGhostContexts && isAffectedByGhosts)
			removeGhostContexts(targetFolder, nativeStoreIndices);
	}
	
	private static void print(String msg) {
		System.out.println(msg);
		logger.info(msg);
	}
	
	private static void checkRepositoryAccessible(File dbModelFolder, String nativeStoreIndices) throws FileNotFoundException, IOException {
		if (!dbModelFolder.exists()) {
			throw new FileNotFoundException("dbModel not found: " + dbModelFolder.getAbsolutePath());
		}
		resetIndexConfiguration(dbModelFolder);
		Repository repo = null;
		RepositoryConnection conn = null;
		try {
			repo = getRepository(dbModelFolder, nativeStoreIndices);
			repo.initialize();
			conn = repo.getConnection();
			conn.size();
		} catch (Exception e) {
			if (e.getMessage().contains("locked")) {
				print("Repository is locked. Please make sure that the repository is not accessed by any other process." + e.getMessage());
				throw new IllegalStateException(e);
			}
		} finally {
			ReadWriteDataManagerImpl.closeQuietly(conn);
			ReadWriteDataManagerImpl.shutdownQuietly(repo);
		}
	}
	
	private static RepositoryState checkRepositoryState(File dbModelFolder, String indices, boolean checkGhostContexts) {
		
		RepositoryState state = new RepositoryState(indices);
		
		Repository repo = null;
		RepositoryConnection conn = null;
		try {
			repo = getRepository(dbModelFolder, indices);
			repo.initialize();
			conn = repo.getConnection();
			long size1 = conn.size();			
			long size2 = sizeQuery(conn);
			if (size1!=size2) 
				throw new IllegalStateException("Size of repository via API and SELECT COUNT(*) differs: " + size1 + " vs. " + size2);

			print("  Size: " + size1);
			state.reportSize(size1);
			if (checkGhostContexts) {
				List<GhostContext> ghosts = checkGhostContexts(conn);
				state.reportGhosts(ghosts);
			}
		} catch (Exception e) {
			print("  Error: " + e.getMessage());
			state.reportCorruptState(e);
		} finally {	
			ReadWriteDataManagerImpl.closeQuietly(conn);
			ReadWriteDataManagerImpl.shutdownQuietly(repo);
		}
		
		return state;
	}
	
	private static List<GhostContext> checkGhostContexts(RepositoryConnection conn) throws Exception {
		
		List<GhostContext> ghosts = Lists.newArrayList();
		TupleQueryResult res = null;		
		try {
			res = conn.prepareTupleQuery(QueryLanguage.SPARQL, 		
				"SELECT ?context (COUNT(?s) AS ?cnt) WHERE { " +
				"  GRAPH ?context { ?s ?p ?o } " +
				"  FILTER (?context NOT IN(<http://www.fluidops.com/MetaContext>, <http://www.fluidops.com/VoIDContext>)) " +
				"  FILTER (NOT EXISTS { ?context rdf:type <http://www.fluidops.com/Context> } ) " +
				"} GROUP BY ?context order by ?context").evaluate();
			while (res.hasNext()) {
				BindingSet b = res.next();
				if (!b.hasBinding("context")) {
					continue;
				}
				ghosts.add(new GhostContext((URI) b.getValue("context"), Integer.parseInt((b.getValue("cnt").stringValue()) ) ) );
			}			
		} finally {
			ReadWriteDataManagerImpl.closeQuietly(res);
		}
		
		return ghosts;
	}
	
	/**
	 * Rebuilds the repository by performing the following steps:
	 * 
	 * 1) Open the original repository from dbModelFolder
	 * 2) Open a fresh target repository at dbModelFolder.tmp using nativeStoreIndices
	 * 3) Copy the content from source repository to target repository
	 * 4) Move original repository to dbModelFolder.bak
	 * 5) Move fresh target repository to dbModelFolder
	 * 
	 * In case of an error during a physical I/O operation (i.e. moving the original folder, deleting
	 * the temporary repository) an IOException is thrown. The original repository remains
	 * untouched, the target repository may have created files in dbModelFolder.tmp (which
	 * need to be deleted manually)
	 * 
	 * @param dbModelFolder
	 * @param targetFolder
	 * @param nativeStoreIndicesSource the native store indices to be used for the source, e.g. "spoc"
	 * @param nativeStoreIndicesTarget the native store indices to be used for the target, e.g. "spoc,psoc"
	 * @throws IllegalStateException if the repository could not be repaired
	 * @throws IOException
	 */
	private static void rebuildRepository(File dbModelFolder, File targetFolder, String nativeStoreIndicesSource, String nativeStoreIndicesTarget) throws IOException {		

		resetIndexConfiguration(dbModelFolder);
		File targetRepoTmpFolder = new File(dbModelFolder.getParentFile(), dbModelFolder.getName() + ".tmp");
		Repository sourceRepo = null;
		RepositoryConnection conn = null;		
		
		try {				
			
			sourceRepo = getRepository(dbModelFolder, nativeStoreIndicesSource);
			sourceRepo.initialize();
			conn = sourceRepo.getConnection();
			long sizeOld = conn.size();
			print("Original repository size: " + sizeOld);
			
			if (targetRepoTmpFolder.exists()) {
				throw new IOException("Temporary folder for repository from incomplete recovery exists at: " + targetRepoTmpFolder.getPath() + ": Remove it manually.");
			}
			
			Repository targetRepo = null;
			RepositoryConnection targetConn = null;
			try {
				targetRepo = getRepository(targetRepoTmpFolder, nativeStoreIndicesTarget);
				targetRepo.initialize();
				targetConn = targetRepo.getConnection();
				print("Copying contents of repository to new store ..."); 
				print("(Note: Depending on the triple store size this might take several minutes)");
				copyRepositoryContent(conn, targetConn);
				long sizeNew = targetConn.size();
				print("New repository size: " + sizeNew);
				if (sizeOld!=sizeNew)
					print("WARNING: repository size of old and new repository differ, please validate manually.");
			} finally {
				ReadWriteDataManagerImpl.closeQuietly(targetConn);
				ReadWriteDataManagerImpl.shutdownQuietly(targetRepo);
			}				
			
		} catch (Exception e) {
			print("Error while rebuilding repository: " + e.getMessage());
			print("The original repository is still in place at " + dbModelFolder.getPath() + ". Temporary files need to be removed manually from " + targetRepoTmpFolder.getPath()); 
			throw new IllegalStateException("Error while rebuilding repository: " + e.getMessage());
		} finally {	
			ReadWriteDataManagerImpl.closeQuietly(conn);
			ReadWriteDataManagerImpl.shutdownQuietly(sourceRepo);
			resetIndexConfiguration(dbModelFolder);
		}
		
		// Success: change the locations of the new and backup repository
		// 1) if targetFolder and dbModelFolder are the same, backup original repository to %dbModelFolder.bak%
		// 2) move new repository to %targetFolder%
		
		if (targetFolder.equals(dbModelFolder)) {
			File dbModelFolderBak = new File(dbModelFolder.getParentFile(), dbModelFolder.getName() + ".bak");
			FileUtils.moveDirectory(dbModelFolder, dbModelFolderBak);
			print("Note: the backup of the original repository is available at " + dbModelFolderBak.getPath());
		}
		FileUtils.moveDirectory(targetRepoTmpFolder, targetFolder);
					
		print("Repository successfully rebuild to " + targetFolder.getPath());
		
	}
	
	/**
	 * remove the ghost contexts in the given dbmodel using a SPARQL DELETE query
	 */
	private static void removeGhostContexts(File dbModelFolder, String nativeStoreIndices) {
		
		Repository sourceRepo = null;
		RepositoryConnection conn = null;		
		try {				
			sourceRepo = getRepository(dbModelFolder, nativeStoreIndices);
			sourceRepo.initialize();
			conn = sourceRepo.getConnection();
			
			print("Deleting garbage contents from repository ..."); 
			print("(Note: Depending on the triple store size this might take several minutes)");
			
			String deleteQuery = "DELETE {  GRAPH ?context { ?s ?p ?o . } } WHERE {   GRAPH ?context { ?s ?p ?o }   FILTER (?context NOT IN(<http://www.fluidops.com/MetaContext>,<http://www.fluidops.com/VoIDContext>))  FILTER (NOT EXISTS { ?context rdf:type <http://www.fluidops.com/Context> } ) }";
			Update delete = conn.prepareUpdate(QueryLanguage.SPARQL, deleteQuery);
			delete.execute();			
			
			print("Repository size after cleanup: " + conn.size());
		} catch (RepositoryException e) {
			print("Error while removing garbage contexts: " + e.getMessage());
			throw new IllegalStateException("Error while removing garbage contexts: " + e.getMessage());
		} catch (MalformedQueryException e) {
			throw Throwables.propagate(e);
		} catch (UpdateExecutionException e) {
			print("Error while removing garbage contexts: " + e.getMessage());
			throw new IllegalStateException("Error while removing garbage contexts: " + e.getMessage());
		} finally {
			ReadWriteDataManagerImpl.closeQuietly(conn);
			ReadWriteDataManagerImpl.shutdownQuietly(sourceRepo);
		}
	}

	private static Repository getRepository(File repository, String indices) {
		return new SailRepository(
				ReadDataManagerImpl.getNativeStore(repository, indices));
	}
	
	private static void resetIndexConfiguration(File dbModelFolder) throws IOException {		
		GenUtil.delete(new File(dbModelFolder, "triples.prop"));
	}
	
	private static long sizeQuery(RepositoryConnection conn) throws Exception {
		TupleQueryResult res = null;
		try {
			res = conn.prepareTupleQuery(QueryLanguage.SPARQL, "SELECT (COUNT(*) AS ?cnt) WHERE { ?s ?p ?o }").evaluate();
			return Long.parseLong(res.next().getValue("cnt").stringValue());
		} finally {
			ReadWriteDataManagerImpl.closeQuietly(res);
		}
	}
	
	private static void copyRepositoryContent(RepositoryConnection source, RepositoryConnection target) {		
		try {
			target.add(source.getStatements(null, null, null, false));				
		} catch (RepositoryException e) {
			print("Error copying data from source repository.");
		}
	}

	private static String getDefaultNativeStoreIndices() {
		return Config.getConfig().getNativeStoreIndices();
	}
	
	
	
	
	public static void main(String[] args) throws IOException {
		
		// Configure logging
		Log4JHandler.initLogging();
		Config.getConfigWithOverride();

		try {
			
			CommandLineParser parser = new BasicParser();
			Options options = buildOptions();
			
		    // parse the command line arguments
		    CommandLine line = parser.parse( options, args );

		    // print the help
		    if( line.getOptions().length==0 || line.hasOption( "help" ) ) {
		    	printHelp(options);
		    	return;
		    }
		    
		    @SuppressWarnings("unchecked")
			List<String> lArgs = line.getArgList();
		    File source=null, target=null;
		    if (lArgs.size()==0) {
		    	source = IWBFileUtil.getFileInDataFolder(Config.getConfig().getRepositoryName());
		    	target = source;
		    } else if (lArgs.size()==1) {
		    	source = new File(lArgs.get(0));
		    	target = source;
		    } else if (lArgs.size()==2) {
		    	source = new File(lArgs.get(0));
		    	target = new File(lArgs.get(1));
		    } else {
		    	System.out.println("Unrecognized arguments.");
		    	printHelp(options);
		    	System.exit(1);
		    }  	
		    
		    // handle different options
		    boolean checkGhosts = line.hasOption("g");
		    String indices = line.hasOption("i") ? line.getOptionValue("i") : getDefaultNativeStoreIndices();
		    if (line.hasOption("a")) {
	    		analyze(source, checkGhosts, indices);
		    	System.exit(0);
		    }
		    
		    if (line.hasOption("f")) {
	    		analyzeAndFix(source, target, checkGhosts, indices);
		    	System.exit(0);
		    }
		    
		    if (line.hasOption("c")) {
		    	cleanupRepository(source, target);
		    	System.exit(0);
		    }
		    
		} catch(Exception exp) {
		    System.out.println( "Unexpected error: " + exp.getMessage() );
		    System.exit(1);
		}
		
	}
	
	
	@SuppressWarnings("static-access")
	private static Options buildOptions() {
		
		Options o = new Options();
		o.addOption("a", "analyze", false, "analyze the repository and print results");
		o.addOption("f", "analyzeAndFix", false, "analyze the repository, print results and try to recover (if problems are detected)");
		o.addOption("g", "garbageCheck", false, "check for garbage data due to inconsistencies in the triple store");
		o.addOption("c", "cleanup", false, "cleanup of the repository (assumes a non-corrupt repository). This operation potentially reduces the size of the repository.");
		o.addOption("h", "help", false, "print this message");
		o.addOption(OptionBuilder
				.withArgName("nativeStoreIndices")
				.withLongOpt("indices")
				.hasArg()
				.withDescription("the assumed native store indices, e.g. 'spoc,psoc'")
				.create("i"));
		return o;	
	}
	
	private static void printHelp(Options options) {
		HelpFormatter formatter = new HelpFormatter();
    	formatter.printHelp( "repotool [OPTIONS] [SOURCE [TARGET]]", 
    			"\nTool for different repository level operations (analysis, repair, cleanup). The first argument (if given) " +
    			"corresponds to the source path of the triple store (default is taken from configuration), the second (optional) " +
    			"argument refers to the target destination. If no target is specified the repository operation is done in place, " +
    			"with a backup of the source being created to %source%.bak.\n\nOptions:", 
    			options, 
    			"Examples\n" +
    			"repotool -a -g\n" +
    			"repotool -a data/dbmodel-broken\n" +
    			"repotool -f data/dbmodel-broken\n" +
    			"repotool -f -g data/dbmodel-broken data/dbmodel-fixed\n" + 
    			"repotool -a -i cspo,cpso,cops data/historymodel", false );	   	
	}
	
	
	protected static class RepositoryState {
		private final String indices;
		private final List<GhostContext> ghosts = Lists.newArrayList();
		private long size = -1;
		private Exception corruptException;		
		private RepositoryState(String indices) {
			this.indices = indices;
		}
		public void reportGhost(URI contextUri, int nTriples) {
			ghosts.add(new GhostContext(contextUri, nTriples));
		}
		public void reportGhosts(List<GhostContext> ghosts) {
			this.ghosts.addAll(ghosts);
		}
		public void reportSize(long size) {
			this.size = size;
		}
		public void reportCorruptState(Exception corruptException) {
			this.corruptException = corruptException;
		}
		public boolean isAffectedByGhosts() {
			return ghosts.size()>0;
		}
		public boolean isCorrupt() {
			return corruptException!=null;
		}
		public String getCorruptMessage() {
			if (!isCorrupt())
				return "";
			return corruptException.getMessage();
		}
		public long getSize() {
			return size;
		}
		public int getNumberOfGhosts() {
			return ghosts.size();
		}
		public void printGhosts() {
			for (GhostContext gc : ghosts)
				System.out.println(" * " + gc.contextUri.stringValue() + " (Triples: " + gc.nTriples + ")");
		}
		public String getIndices() {
			return indices;
		}
	}
	
	protected static class GhostContext {
		public final URI contextUri;
		public final int nTriples;
		private GhostContext(URI contextUri, int nTriples) {
			this.contextUri = contextUri;
			this.nTriples = nTriples;
		}		
	}
}
