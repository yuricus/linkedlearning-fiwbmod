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

package com.fluidops.iwb.server;

import info.aduna.lang.FileFormat;
import info.aduna.lang.service.FileFormatServiceRegistry;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.openrdf.OpenRDFException;
import org.openrdf.http.protocol.Protocol;
import org.openrdf.http.server.ClientHTTPException;
import org.openrdf.http.server.ProtocolUtil;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.query.Binding;
import org.openrdf.query.BindingSet;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryResults;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.query.algebra.evaluation.util.ValueComparator;
import org.openrdf.query.resultio.BooleanQueryResultWriter;
import org.openrdf.query.resultio.BooleanQueryResultWriterFactory;
import org.openrdf.query.resultio.BooleanQueryResultWriterRegistry;
import org.openrdf.query.resultio.TupleQueryResultFormat;
import org.openrdf.query.resultio.TupleQueryResultWriter;
import org.openrdf.query.resultio.TupleQueryResultWriterFactory;
import org.openrdf.query.resultio.TupleQueryResultWriterRegistry;
import org.openrdf.query.resultio.sparqljson.SPARQLResultsJSONWriter;
import org.openrdf.query.resultio.sparqlxml.SPARQLBooleanXMLWriter;
import org.openrdf.query.resultio.sparqlxml.SPARQLResultsXMLWriter;
import org.openrdf.query.resultio.text.csv.SPARQLResultsCSVWriter;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.RDFWriterFactory;
import org.openrdf.rio.RDFWriterRegistry;
import org.openrdf.rio.n3.N3Writer;
import org.openrdf.rio.ntriples.NTriplesWriter;
import org.openrdf.rio.rdfxml.RDFXMLWriter;
import org.openrdf.rio.trig.TriGWriter;
import org.openrdf.rio.trix.TriXWriter;
import org.openrdf.rio.turtle.TurtleWriter;

import com.fluidops.ajax.FSession;
import com.fluidops.ajax.components.FPage;
import com.fluidops.iwb.Global;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.ReadDataManager;
import com.fluidops.iwb.api.ReadDataManager.AggregationType;
import com.fluidops.iwb.api.ReadDataManagerImpl;
import com.fluidops.iwb.api.ReadDataManagerImpl.SparqlQueryType;
import com.fluidops.iwb.api.ReadWriteDataManager;
import com.fluidops.iwb.api.ReadWriteDataManagerImpl;
import com.fluidops.iwb.page.PageContext;
import com.fluidops.iwb.server.RedirectService.RedirectType;
import com.fluidops.iwb.ui.templates.ServletPageParameters;
import com.fluidops.iwb.util.Config;
import com.fluidops.security.XssSafeHttpRequest;
import com.fluidops.util.StringUtil;
import com.fluidops.util.TemplateBuilder;
import com.fluidops.util.concurrent.TaskExecutor;
import com.google.common.collect.Lists;




/**
 * New SPARQL Servlet architecture that is standard conform to the SPARQL specification.<p>
 * 
 * This servlet uses the semaphore design pattern to restrict parallel access to the 
 * database. Only {@value #N_PERMITS} accesses in parallel (see {@link #N_PERMITS}.<p>
 *  
 * This servlet can process both GET and POST requests, supporting the query as <i>q</i> or
 * <i>query</i> argument.<p>
 * 
 * <p>The XSS Filter is deactivated for this SERVLET. Any output must be controlled manually</p>
 * 
 * <p>Result presentation depends on http header, default SPARQL/XML for SELECT queries.</p>
 * 
 * <p>Format parameter can be used to customize the format without using headers. Supported values: are the name or the mime type:</p>
 * 
 * a) Tuple Queries (e.g. SELECT queries):
 * 
 * <ul>
 *  <li>SPARQL/XML / application/sparql-results+xml</li>
 *  <li>SPARQL/JSON / application/sparql-results+json</li>
 *  <li>SPARQL/CSV / text/csv</li>
 * </ul>
 * 
 * b) Graph Queries (e.g. Construct queries):
 * 
 * <ul>
 *  <li>RDF/XML / application/rdf+xml</li>
 *  <li>N-Triples / text/plain</li>
 *  <li>Turtle / text/turtle</li>
 *  <li>N3 / text/rdf+n3</li>
 *  <li>TriX / application/trix</li>
 *  <li>TriG / application/x-trig</li>
 *  
 * Note: for both type format=auto is supported and uses the default, i.e redirect to search servlet.
 * 
 * <p>Inference can be controlled by the request parameter <i>infer</i> (true|false), default is false</p>
 * 
 * Special handling:<p> * 
 * 
 * <ul>
 * 	<li><b>1. Parameter "value":</b> the resolve value for the query, i.e. replacement for ??</li>
 *  <li><b>2. Parameter "historic":</b> boolean flag to indicate if the historical repository should be used</li>
 * 	<li><b>3. Parameter "format" (only in Graphqueries):</b> specify the format as HTTP parameter, 
 *  		e.g. RDF/XML (see {@link org.openrdf.rio.RDFFormat} for supported formats</li>
 *  <li><b>4. Parameter "forceDownload":</b>Add the content-disposition header (to force save as)</li>
 *  <li><b>5. Parameter "queryType":</b> if the "queryType" is set to 'context' the value of the parameter "query" is an array of contexts
 *            (comma-separated URIs or 'All' for all context enclosed in '['/']') to export.
 *            In this case the contexts will be exported in the requested format. Example: [http://example.org/myCtx]</li>
 * </ul>
 * 
 * Legacy Support: <p>
 * 
 * <ul>
 *  <li>Tuple queries: Both "input" and "output" are specified (e.g. for charts) => apply aggregation. 
 *  Also handles "aggregation", "datasets"</li>
 *  <li>CSV legacy in tuple queries: deals with "ctype"=csv, no aggregation, deals with "input" and "output"</li>
 * </ul> 
 * 
 * W3c standard SPARQL 1.1 protocol: http://www.w3.org/TR/sparql11-protocol/
 * 
 * @author Andreas Schwarte
 *
 */
public class SparqlServlet extends IWBHttpServlet {

	// TODO
	/*
	 * Open TODOs
	 * 
	 * 1) Handle "default-graph-uri" parameter: http://www.w3.org/TR/rdf-sparql-protocol/#query-bindings-http
	 * 2) Standards conform error handling => xsd error messages instead of plain text: http://www.w3.org/TR/sparql11-protocol/#query-out-message
	 */
	
	protected static final long serialVersionUID = 2627590629243739807L;	
	protected static final Logger log = Logger.getLogger(SparqlServlet.class);
	

	public static final int N_PERMITS = 8;		// number of parallel DB accesses
	
	protected static final AtomicInteger nextRequestId = new AtomicInteger(1);
	protected static final Semaphore semaphore = new Semaphore(N_PERMITS, true);

	static List<String> classesList = Collections.emptyList();
	static List<String> propertiesList = Collections.emptyList();
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		handle(req, resp);
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		handle(req, resp);
	}
	

	
	/**
	 * Retrieve the query from request, do the token based security check and
	 * evaluate the query.
	 * 
	 * @param req
	 * @param resp
	 * @throws IOException 
	 */
	protected void handle(HttpServletRequest req, HttpServletResponse resp) throws IOException{
		
		// for this servlet we do not use XSS Filter, any output must be controlled.
		if (req instanceof XssSafeHttpRequest) {
			req = ((XssSafeHttpRequest)req).getXssUnsafeHttpRequest();
		}
		
		String query = null;
		try {			
			
			ServletOutputStream outputStream = resp.getOutputStream();

			query = getQueryFromRequest(req);
			
			boolean openLocalAccess = Config.getConfig().getOpenSparqlServletForLocalAccess() && (req.getRemoteAddr().equals("127.0.0.1") || req.getRemoteAddr().equals("localhost")); 
			
			if (query==null) {
				if(!openLocalAccess && !EndpointImpl.api().getUserManager().hasQueryRight(SparqlQueryType.SELECT, null)) {
					resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Not enough rights to execute query.");
	            	return;
				}

				// Need to check the type of browser, because the Flint SPARQL editor does not support IE 8.0.
				// Added explicitly as a request parameter because
				// by default IE10 uses compatibility mode when accessing an intranet page
				// and it is recognised as IE 8.0 on the server side
				if(!Boolean.valueOf(req.getParameter("useIE8Fallback")))
					printQueryInterface(req, resp, outputStream, "com/fluidops/iwb/server/sparqlinterface");
				else
					printQueryInterface(req, resp, outputStream, "com/fluidops/iwb/server/sparqlinterface-ie8-fallback");
				return;
			} 

//			query = URLDecoder.decode(query, "UTF-8");

			String securityToken = req.getParameter("st");

	        //////////////////SECURITY CHECK/////////////////////////
			String qType = req.getParameter("queryType");
	        SparqlQueryType queryType = qType!=null ? null : ReadDataManagerImpl.getSparqlQueryType(query, true);
	        if (!openLocalAccess && !EndpointImpl.api().getUserManager().hasQueryPrivileges(query, queryType, securityToken)) {
	            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Not enough rights to execute query.");
	            return;
	        }
			
			// redirect format=auto to search servlet (for SELECT+CONSTRUCT)
			String format = req.getParameter("format");
			if (format!= null && format.equals("auto")) {
				if (queryType == SparqlQueryType.SELECT || queryType == SparqlQueryType.CONSTRUCT) {
					// Apparently, the jetty limit on request header buffer size is 6144, which is filled by (contentLength*2 + something else).
					// Content length 2965 was sufficient for the query to pass.
					// Moved the threshold down in case if additional properties are to be passed (e.g., multiple query targets).
					if(req.getContentLength()<2800) {
						resp.sendRedirect(req.getContextPath() + "/search/?q=" + StringUtil.urlEncode(query) 
								+ "&infer=" + StringUtil.urlEncode(req.getParameter("infer")) + "&queryLanguage=SPARQL&queryTarget=RDF");
						return;
					} else {
						RequestDispatcher dispatcher = req.getRequestDispatcher("/search/?queryLanguage=SPARQL&queryTarget=RDF");
					    try {
							dispatcher.forward(req, resp);
						} catch (ServletException e) {
							resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Could not forward request to search servlet.");
						}
						return;
					}
				}
			}
			
			//check if the query is a context to export	
			if(qType!=null && qType.equals("context"))
				exportContexts(query, req, resp, outputStream);
			else
				handleQuery(query, queryType, req, resp, outputStream);
			
		
		}
		catch (MalformedQueryException e) {
			error(resp, 400, "Malformed query: \n\n" + query + "\n\n" + e.getMessage());
			return;
		} 
		catch (IOException e) {
			log.error("Error: ", e);
			throw e;
		}       
    }
	
	/**
	 * Exports contexts in the requested format
	 * @param contexts
	 * @param req
	 * @param resp
	 * @param outputStream
	 * @throws IOException
	 */
	private void exportContexts(String contexts, HttpServletRequest req,
			HttpServletResponse resp, ServletOutputStream outputStream) throws IOException
	{
		Resource[] contextsArray = buildContextsArray(contexts);
		RepositoryConnection con = null;
		RDFWriter writer;
		try
		{
			writer = selectWriter(req, resp, resp.getOutputStream());		
            if (writer==null)
            	throw new IOException("Unsupported format");     
			con = Global.repository.getConnection();
			con.exportStatements(null, null, null, false, writer, contextsArray );
		}
		catch (Exception e)
		{
			throw new IOException("could not export data for contexts: " + Arrays.toString(contextsArray), e);
		}
		finally
		{		
			ReadWriteDataManagerImpl.closeQuietly(con);
		}
	}

	private Resource[] buildContextsArray(String contexts) throws IOException
	{
		// contexts is a string representing an array of contexts (comma-separated)
		// example: [http://example.org/myCtx]		 

		String[] exportContexts = null;
		try
		{
			contexts =  contexts.substring(1, contexts.lastIndexOf("]"));
			
			exportContexts = contexts.split(",");
			
			//check whitespaces
			for(int i = 0; i < exportContexts.length; i++)
				exportContexts[i] = exportContexts[i].trim();
		
		}
		catch (Exception e)
		{
			throw new IOException("context parameter syntax error");
		}
						
		if (exportContexts.length==1 && exportContexts[0].equals("All"))
			return new Resource[0];
		
		Resource[] contextsArray = new Resource[exportContexts.length];
		for(int i = 0; i < exportContexts.length; i++)
		{
			if(!exportContexts[i].equals("null"))
				contextsArray[i] = ValueFactoryImpl.getInstance().createURI(exportContexts[i]);		
			else //'null' is the default context
				contextsArray[i] = null;		
		}
		
		return contextsArray;
	}

	/**
	 * Returns the query string from the requests, parameter "query" or "q"
	 * Also considers "update" if none of the others is specified.
	 * 
	 * @param req
	 * @return
	 * 			the query string or null if no query is specified
	 */
	protected String getQueryFromRequest(HttpServletRequest req) {		
		String query = req.getParameter("query");
		if (query==null)
			query = req.getParameter("q");
		if (query==null)
			return req.getParameter("update");
		return query;
	}
	
	/**
	 * Print the sparql query interface if not query is provided. Uses 
	 * com.fluidops.iwb.server.sparqlinterface.st as template
	 * 
	 * @param resp
	 * @param out
	 */
	protected void printQueryInterface(HttpServletRequest req, HttpServletResponse resp, ServletOutputStream out, String template) throws IOException {
		
		// register search page
		String uri = req.getRequestURI();
						
		FSession.registerPage(uri, FPage.class);

		FSession.removePage(req);
						
		// get session and page
		FSession session = FSession.getSession(req);
		FPage page;
		try {
			page = (FPage)session.getComponentById(uri, req);
		} catch (Exception e) {
			log.warn("Could not get the page for request: " + uri, e);
		       resp.sendRedirect(RedirectService.getRedirectURL(RedirectType.PAGE_NOT_FOUND, req));
			return;
		}
				
		PageContext pc = new PageContext();
				
		pc.title = "SPARQL Query Interface";
		pc.page = page;
		pc.contextPath = EndpointImpl.api().getRequestMapper().getContextPath();
		pc.setRequest(req);
		pc.httpResponse = resp;
		
		resp.setContentType("text/html");
		resp.setStatus(200);
		
		String initQuery = req.getParameter("initQuery");
		if(initQuery==null)
			initQuery = "";

		ServletPageParameters pageParams = ServletPageParameters.computePageParameter(pc);
		Map<String, Object> templateParams = pageParams.toServletStringBuilderArgs();
		
		templateParams.put("namespaces", getNamespacesAsJSONList());
		templateParams.put("classes", this.getURIListAsJSONList(classesList));
		templateParams.put("properties", this.getURIListAsJSONList(propertiesList));
		templateParams.put("initQuery", StringEscapeUtils.escapeJavaScript(initQuery));
		
		//TODO: check ACL query rights and, if allowed, generate security token
		TemplateBuilder tb = new TemplateBuilder( "tplForClass", template);
		out.print(
				tb.renderTemplate(
						templateParams
				)
				);
	}
	
	protected String getNamespacesAsJSONList() {
		JSONArray res = new JSONArray();
		
		Map<String, String> prefixes = EndpointImpl.api().getNamespaceService().getRegisteredNamespacePrefixes();
		
		JSONObject item;
		for(Entry<String, String> entry : prefixes.entrySet()) {
			try {
				item = new JSONObject();
				item.append("prefix", entry.getKey());
				item.append("uri", entry.getValue());
				res.put(item);
			} catch(JSONException e) {
				log.warn(e.getMessage() + ": "+entry.getKey() + ": " + entry.getValue());
			}
		}
		return res.toString();
	}
	
	protected String getURIListAsJSONList(List<String> uris) {
		JSONArray res = new JSONArray();
		
		for(String sUri : uris)
			res.put(sUri);
		return res.toString();
	}
	
	/**
	 * Handle the query:
	 * 
	 *  -  retrieves parameters <i>historic</i> and <i>value</i>
	 *  -  uses semaphore to acquire lease ({@link #N_PERMITS} parallel threads on DB)
	 *  -  processes the query
	 *  
	 * @param query
	 * @param req
	 * @param resp
	 * @param outputStream
	 */
	protected void handleQuery(String query, SparqlQueryType queryType, HttpServletRequest req, HttpServletResponse resp, final ServletOutputStream outputStream) {
			
		// historical Repository
		String _historic = req.getParameter("historic");
		boolean historic = _historic!=null && Boolean.parseBoolean(_historic);
		
		// value to be used in queries instead of ??
        String _resolveValue = req.getParameter("value");
        Value resolveValue = _resolveValue!=null ? ValueFactoryImpl.getInstance().createURI(_resolveValue) : null;
        		
		ReadDataManager queryDM = historic ? 
				ReadDataManagerImpl.getDataManager(Global.historyRepository) : 
					ReadDataManagerImpl.getDataManager(Global.repository);
         
		if (log.isTraceEnabled())
			log.trace("Processing query: " + query);
				
		int currentRequest = nextRequestId.incrementAndGet();
		
		// acquire the lease (i.e. slot for processing)
		try {
			semaphore.acquire();
		} catch (InterruptedException e1) {
			log.warn("Interrupted while waiting for proccessing lease.", e1);
			error(resp, 500, "Interrupted while processing queries.");
			return;
		}
		
		try {
			processQuery(query, queryType, currentRequest, resolveValue, queryDM, req, resp, outputStream);
		} finally {
			// release the lease (i.e. free the slot for other queries)
			semaphore.release();
		}
		
	}
	
	/**
	 * Send the specified error message to the client (if the connection is
	 * still open).
	 * 
	 * @param resp
	 * @param errorCode
	 * @param message
	 */
	protected void error(HttpServletResponse resp, int errorCode, String message) {
		
		try {
			log.info("Error (" + errorCode + "): " + message);
			if (!resp.isCommitted())
				resp.sendError(errorCode, message);
		} catch (IllegalStateException e) {
			// should never occur
			log.warn("Error message could not be send. Stream is committed: " + message);
		} catch (IOException e) {
			log.error("Error message could not be sent", e);
		}
	}
	
	
	/**
	 * Evaluate the query and write the results to the outputstream
	 * 
	 * -  uses parameter <i>forceDownload</i> to open saveAs dialog
	 * 
	 * @param queryString
	 * @param queryType
	 * @param reqId
	 * @param resolveValue
	 * @param dm
	 * @param req
	 * @param resp
	 * @param out
	 */
	private void processQuery(String queryString, SparqlQueryType queryType, int reqId,
			Value resolveValue, ReadDataManager dm, HttpServletRequest req,
			HttpServletResponse resp, ServletOutputStream out)
	{

        try {	
        	
        	boolean infer = false;
        	
        	if (req.getParameter("infer")!=null)
        		infer = Boolean.parseBoolean(req.getParameter("infer"));        	
	        
            resp.setStatus(HttpServletResponse.SC_OK);
            switch (queryType) {
            case ASK:
            	boolean bRes = dm.sparqlAsk(queryString, true, resolveValue, infer);         
	            handleAskQueryResult(bRes, req, resp);
	            break;
            case SELECT:
	            TupleQueryResult tRes = dm.sparqlSelect(queryString, true, resolveValue, infer);
	            try {
	            	handleTupleQueryResult(tRes, req, resp, dm);
	            } finally {
	            	ReadDataManagerImpl.closeQuietly(tRes);
	            }
	            break;
            case CONSTRUCT:
	            GraphQueryResult gRes = dm.sparqlConstruct(queryString, true, resolveValue, infer);
	            try { 
	            	handleGraphQueryResult(gRes, req, resp, dm);
	            } finally {
	            	ReadDataManagerImpl.closeQuietly(gRes);
	            }
	            break;
            case UPDATE:
            	ReadWriteDataManager wdm = null;
            	try {
            		wdm = ReadWriteDataManagerImpl.openDataManager(dm.getRepository());
            		wdm.sparqlUpdate(queryString, resolveValue, infer, null);
            		resp.getOutputStream().print("Update executed successfully.");
            	} finally {
            		ReadWriteDataManagerImpl.closeQuietly(wdm);
            	}
            	break;
            default:
            	error(resp, 500, "Querytype not suppported");
            }	       

	    } catch (MalformedQueryException e) {
	    	error(resp, 400, "Error occured while processing the query. \n\n" + queryString + "\n\n" + e.getMessage());
		} catch (QueryEvaluationException e) {
			error(resp, 500, "Error occured while processing the query. \n\n" + queryString + "\n\n" + e.getClass().getSimpleName() + ": " + e.getMessage());
		} catch (IOException e) {
			log.warn("I/O Error. \nQuery :" + queryString, e);
			error(resp, 500, "Error occured while processing the query. \n\n" + queryString + "\n\n" + e.getClass().getSimpleName() + ": " + e.getMessage());
		} catch (OpenRDFException e) {
			// e.g. if client connection was closed, must not be an error
			error(resp, 500, "Error occured while processing the query. \n\n" + queryString + "\n\n" + e.getClass().getSimpleName() + ": " + e.getMessage());
		} catch (Exception e) {
			log.error("Unexpected error occured while processing the query. \nQuery :" + queryString, e);
			error(resp, 500, "Error occured while processing the query. \n\n" + queryString + "\n\n" + e.getClass().getSimpleName() + ": " + e.getMessage());
		}     
		
	}
	
	private void handleAskQueryResult(boolean res, HttpServletRequest req, HttpServletResponse resp) throws IOException, Exception {

        BooleanQueryResultWriter qrWriter;
        if (req.getParameter(Protocol.ACCEPT_PARAM_NAME)!=null) {
        	FileFormatServiceRegistry<? extends FileFormat, ?> registry = BooleanQueryResultWriterRegistry.getInstance();
            BooleanQueryResultWriterFactory qrWriterFactory = (BooleanQueryResultWriterFactory)ProtocolUtil.getAcceptableService(req, resp, registry);
            setResponseContentType(resp, qrWriterFactory.getBooleanQueryResultFormat().getDefaultMIMEType(), "utf-8");
            addContentDispositionHeader(req, resp, "result." + qrWriterFactory.getBooleanQueryResultFormat().getDefaultFileExtension());
            qrWriter = qrWriterFactory.getWriter(resp.getOutputStream());
        } else {
        	setResponseContentType(resp, "application/sparql-results+xml", "utf-8");
        	addContentDispositionHeader(req, resp, "result." + TupleQueryResultFormat.SPARQL.getDefaultFileExtension());
        	qrWriter = new SPARQLBooleanXMLWriter(resp.getOutputStream());
        }
                            
        resp.setStatus(HttpServletResponse.SC_OK);
        qrWriter.handleBoolean(res);
	}	
	
	private void handleTupleQueryResult(TupleQueryResult res,
			HttpServletRequest req, HttpServletResponse resp, ReadDataManager dm)
			throws IOException, Exception
	{
        // handle legacy aggregation for charts (conditional)
        if (!handleAggregationLegacy(res, req, dm, resp.getOutputStream()))
        	return; 	// do not continue if the case is handled
        
        // handle legacy CSV (without aggregation) (conditional)
        if (!handleCSVLegacy(res, req, dm, resp.getOutputStream()))
        	return; 	// do not continue if the case is handled
        
        // handle result format for tuple queries
        if (!handleResultFormatTupleQuery(res, req, resp, dm, resp.getOutputStream()))
        	return;
        
    	TupleQueryResultWriter qrWriter = null;
    	
    	String fileExtension = TupleQueryResultFormat.SPARQL.getDefaultFileExtension();
    	
    	if (req.getParameter(Protocol.ACCEPT_PARAM_NAME)!=null || req.getHeader(Protocol.ACCEPT_PARAM_NAME)!=null) {
    		FileFormatServiceRegistry<? extends FileFormat, ?> registry = TupleQueryResultWriterRegistry.getInstance();
    		try {
    			TupleQueryResultWriterFactory qrWriterFactory = (TupleQueryResultWriterFactory)ProtocolUtil.getAcceptableService(req, resp, registry);
    			setResponseContentType(resp, qrWriterFactory.getTupleQueryResultFormat().getDefaultMIMEType(), "utf-8");
    			addContentDispositionHeader(req, resp, "result." + qrWriterFactory.getTupleQueryResultFormat().getDefaultFileExtension());                    
    			qrWriter = qrWriterFactory.getWriter(resp.getOutputStream());
    		} catch(ClientHTTPException e) {
    			
    		}
    	}
    	
    	if(qrWriter == null) {
    		setResponseContentType(resp, "application/sparql-results+xml", "utf-8");
        	addContentDispositionHeader(req, resp, "result." + TupleQueryResultFormat.SPARQL.getDefaultFileExtension());                    
        	qrWriter = new SPARQLResultsXMLWriter(resp.getOutputStream());
    		addContentDispositionHeader(req, resp, "result." + fileExtension);
    	}
    	
        QueryResults.report(res, qrWriter);
	}

	private void handleGraphQueryResult(GraphQueryResult res,
			HttpServletRequest req, HttpServletResponse resp, ReadDataManager dm)
			throws IOException, Exception
	{
		 // handle result format for Graph queries (e.g. CONSTRUCT)
        if (!handleResultFormatGraphQuery(res, req, resp, dm, resp.getOutputStream()))
        	return; 	// do not continue if the case is handled

        RDFWriter qrWriter;
        if (req.getParameter(Protocol.ACCEPT_PARAM_NAME)!=null) {
        	FileFormatServiceRegistry<? extends FileFormat, ?> registry = RDFWriterRegistry.getInstance();
        	RDFWriterFactory qrWriterFactory = (RDFWriterFactory)ProtocolUtil.getAcceptableService(req, resp, registry);
        	setResponseContentType(resp, qrWriterFactory.getRDFFormat().getDefaultMIMEType(), "utf-8");
        	addContentDispositionHeader(req, resp, "result." + qrWriterFactory.getRDFFormat().getDefaultFileExtension());                 
        	qrWriter = qrWriterFactory.getWriter(resp.getOutputStream());
        } else {
        	setResponseContentType(resp, "application/rdf+xml", "utf-8");
        	addContentDispositionHeader(req, resp, "result." + RDFFormat.RDFXML.getDefaultFileExtension());
        	qrWriter = new RDFXMLWriter(resp.getOutputStream());    	            	
        }    	                	       
        
        QueryResults.report(res, qrWriter);
	}
	
	
	/**
	 * Handle legacy aggregation of tuple queries. Prints the aggregated result as CSV!
	 * 
	 * Condition: tuple query + parameter "input" and "output" are set
	 * 
	 * Also deals with (optional) parameter "datasets"
	 * 
	 * This method MUST return true, if SPARQL processing shall continue. If legacy code
	 * is applied, results may be written to the outputstream directly (and false is returned)
	 *  
	 * @param res
	 * @param req
	 * @return
	 * 			true if the standard SPARQL processing should continue, false otherwise
	 * @throws QueryEvaluationException 
	 * @throws IOException 
	 */
	private boolean handleAggregationLegacy(TupleQueryResult res,
			HttpServletRequest req, ReadDataManager queryDM,
			OutputStream outputStream) throws QueryEvaluationException,
			IOException
	{

		String input = req.getParameter("input");
		String output = req.getParameter("output");
		
		// check the condition
		if (StringUtil.isNullOrEmpty(input) || StringUtil.isNullOrEmpty(output))
			return true;

		String datasets = req.getParameter("datasets");	
		String aggregation = req.getParameter("aggregation");
		
		String[] outputs = output.split(",");  		    		
		
		AggregationType aggType = AggregationType.NONE; 	// DEFAULT
		if (aggregation!=null) {
			if(aggregation.equals("COUNT")) 
                aggType = AggregationType.COUNT;
			else if(aggregation.equals("SUM")) 
                aggType = AggregationType.SUM;
			else if(aggregation.equals("NONE")) 
                aggType = AggregationType.NONE;
			else if(aggregation.equals("AVG")) 
                aggType = AggregationType.AVG;
		}
                        
        Map<Value, Vector<Number>> valueMap;
        if (datasets == null)
        {
            valueMap = queryDM.aggregateQueryResult(res,
                            aggType, input, outputs);
        }
        else
        {
            // special handling: we must first group by the values
            // of the datasets parameter before aggregating; this
            // processing scheme supports only a single output variable
            String[] splittedDatasets = datasets.split(",");
            valueMap = queryDM.aggregateQueryResultWrtDatasets(
                    res, aggType, input, outputs[0],
                    splittedDatasets);
        }

        // We need to sort the input again, as the order gets lost when accessing the valueMap
        Set<Value> keySet = valueMap.keySet();
        SortedSet<Value> sortedSet = new TreeSet<Value>(new ValueComparator());
        sortedSet.addAll(keySet);

        // need to write at least one space, as empty results cause errors in charts
        if(sortedSet.isEmpty())
            outputStream.write(" ".getBytes("UTF-8"));

        for(Value val:sortedSet)
        {
            Vector<Number> vals = valueMap.get(val);
            outputStream.write(val.stringValue().getBytes("UTF-8"));
            for(int i=0; i<vals.size(); i++)
            {
                Number n = vals.elementAt(i);
                if (n==null || n.toString()==null)
                    outputStream.write(";".getBytes("UTF-8"));
                else
                    outputStream.write((";" + n.toString()).getBytes("UTF-8"));
            }
            outputStream.write("\n".getBytes("UTF-8"));
        }
        
    	return false;
	}
	
	/**
	 * Handle legacy CSV representation of tuple queries. Prints the aggregated result as CSV!
	 * 
	 * Condition: tuple query + parameter "ctype" is csv
	 * 
	 * Deals with "output" and "input" parameter
	 *     
	 * This method MUST return true, if SPARQL processing shall continue. If legacy code
	 * is applied, results may be written to the outputstream directly (and false is returned)
	 * 
	 * Note: do not close the outputstream in here
	 * 
	 * @param res
	 * @param req
	 * @return
	 * 			true if the standard SPARQL processing should continue, false otherwise
	 * @throws QueryEvaluationException 
	 * @throws IOException 
	 */
	private boolean handleCSVLegacy(TupleQueryResult res,
			HttpServletRequest req, ReadDataManager dm,
			OutputStream outputStream) throws IOException,
			QueryEvaluationException
	{

		String ctype = req.getParameter("ctype");
		if (ctype==null || !ctype.equals("csv"))
			return true;
		
		String input = req.getParameter("input");
        String output = req.getParameter("output");
        
        output = output==null ? "" : output;
        
		// need to write at least one space, as empty results cause errors in charts
        if(!res.hasNext())
            outputStream.write(" ".getBytes());
        while (res.hasNext())
        {
            if(input==null || output.isEmpty())
            {
                BindingSet bindingSet = res.next();
                for(String bindingName:res.getBindingNames())
                {
                    Binding binding = bindingSet.getBinding(bindingName);
                    if(binding==null)
                        outputStream.write(";".getBytes());
                    else
                        outputStream.write((binding.getValue().stringValue()+";").getBytes("UTF-8"));
                }
                outputStream.write("\n".getBytes("UTF-8"));
            }
            else 
            {
                BindingSet bindingSet = res.next();

                String[] outputs = output.split(",");
                for(String bindingName:outputs)
                    outputStream.write((bindingSet.getBinding(bindingName).getValue().stringValue()+";").getBytes("UTF-8"));
                outputStream.write("\n".getBytes("UTF-8"));
            }                     

        }
		
		return false;
	}

	/**
	 * Handle result format in select queries, i.e. the format parameter.
	 * 
	 * Condition: select query + format is set
	 * 
	 * This method MUST return true, if SPARQL processing shall continue. If legacy code
	 * is applied, results may be written to the outputstream directly (and false is returned)
	 * 
	 * For supported values see class documentation.
	 * 
	 * @param res
	 * @param req
	 * 
	 * @return
	 * 			true if the standard SPARQL processing should continue, false otherwise
	 * @throws QueryEvaluationException 
	 * @throws IOException 
	 * @throws TupleQueryResultHandlerException 
	 */
	protected boolean handleResultFormatTupleQuery(TupleQueryResult res,
			HttpServletRequest req, HttpServletResponse resp,
			ReadDataManager dm, OutputStream outputStream) throws IOException,
			QueryEvaluationException, TupleQueryResultHandlerException
	{

        String format = req.getParameter("format");
        if (format==null)
        	return true;
        
        TupleQueryResultFormat contentType = null;
        if (format.equals("auto"))  {
        	contentType = TupleQueryResultFormat.SPARQL;		
        } else {
        	// try to determine the content type:
            // check both name and mime type
            
            for(TupleQueryResultFormat f : TupleQueryResultFormat.values()) {
                if((f.getName()).equals(format)) {
                    contentType=f;
                    break;
                }
                if (f.getDefaultMIMEType().equals(format)) {
                	contentType=f;
                	break;
                }                	
            }
            if (contentType==null)
            	throw new IOException("Illegal format specified: " + format);           
        }        
        
        
        TupleQueryResultWriter qrWriter;
        if (contentType==TupleQueryResultFormat.SPARQL)
        {
        	qrWriter = new SPARQLResultsXMLWriter(outputStream);
        }
        else if(contentType==TupleQueryResultFormat.JSON) {
            qrWriter = new SPARQLResultsJSONWriter(outputStream);
        }
        else if(contentType==TupleQueryResultFormat.CSV)
        {
            qrWriter = new SPARQLResultsCSVWriter(outputStream);
        }
        else
        	throw new IOException("Format " + format + " not supported.");
        
        setResponseContentType(resp, contentType.getDefaultMIMEType(), "utf-8");
        addContentDispositionHeader(req, resp, "result." + contentType.getDefaultFileExtension());
        QueryResults.report(res, qrWriter);      

        return false;
	}
	
	/**
	 * Handle result format in construct queries, i.e. the format parameter.
	 * 
	 * Condition: graph query + format is set
	 * 
	 * This method MUST return true, if SPARQL processing shall continue. If legacy code
	 * is applied, results may be written to the outputstream directly (and false is returned)
	 * 
	 * For supported values see class documentation.
	 * 
	 * @param res
	 * @param req
	 * 
	 * @return
	 * 			true if the standard SPARQL processing should continue, false otherwise
	 * @throws QueryEvaluationException 
	 * @throws IOException 
	 * @throws RDFHandlerException 
	 */
	protected boolean handleResultFormatGraphQuery(GraphQueryResult res,
			HttpServletRequest req, HttpServletResponse resp,
			ReadDataManager dm, OutputStream outputStream) throws IOException,
			QueryEvaluationException, RDFHandlerException
	{

		RDFWriter qrWriter = selectWriter(req, resp, outputStream);
		
		if(qrWriter == null)
			return true;
		
        QueryResults.report(res, qrWriter);      

        return false;
	}
    
	private RDFWriter selectWriter(HttpServletRequest req,
			HttpServletResponse resp, OutputStream outputStream) throws IOException
	{
        String format = req.getParameter("format");
        if (format==null)
        	return null;
        
        RDFFormat contentType = null;
        if (format.equals("auto"))  {
        	contentType = RDFFormat.RDFXML;		
        } else {
        	// try to determine the content type:
            // check mime type first, after that by name
            
        	contentType = RDFFormat.forMIMEType(format);
        	if (contentType==null) {
        		for(RDFFormat f : RDFFormat.values()) {
	                if((f.getName()).equals(format)) {
	                    contentType=f;
	                    break;
	                }
        		}
        	}
            if (contentType==null)
            	throw new IOException("Illegal format specified: " + format);           
        }        
        
        RDFWriter qrWriter;   
        if (contentType==RDFFormat.RDFXML)
        {
        	qrWriter = new RDFXMLWriter(outputStream);
        }
        else if(contentType==RDFFormat.TRIG) {
            qrWriter = new TriGWriter(outputStream);
        }
        else if(contentType==RDFFormat.NTRIPLES)
        {
            qrWriter = new NTriplesWriter(outputStream);
        }
        else if(contentType==RDFFormat.N3)
        {
            qrWriter = new N3Writer(outputStream);
        }
        else if(contentType==RDFFormat.TURTLE)
        {
            qrWriter = new TurtleWriter(outputStream);
        }
        else if(contentType==RDFFormat.TRIX)
        {
            qrWriter = new TriXWriter(outputStream);
        }       
        else
        	throw new IOException("Format " + format + " not supported.");

        
        setResponseContentType(resp, contentType.getDefaultMIMEType(), "utf-8");
        addContentDispositionHeader(req, resp, "result." + contentType.getDefaultFileExtension());
        
		return qrWriter;
	}

	/**
	 * Add the content-disposition header (to force save as) if the parameter
	 * <i>forceDownload</i> is set.
	 *        	
	 * @param resp
	 * @param fileName
	 */
	private void addContentDispositionHeader(HttpServletRequest req, HttpServletResponse resp, String fileName) {
		
		if (req.getParameter("forceDownload")!=null)
    		resp.setHeader("Content-Disposition", "attachment; filename="+fileName );
	}
	
	private static void setResponseContentType(HttpServletResponse resp, String contentType, String charset) {
		
		if(resp==null || StringUtil.isNullOrEmpty(contentType))
			return;
		
		if(StringUtil.isNotNullNorEmpty(charset)) {
			resp.setContentType(contentType + "; charset=" + charset);
		} else {
			resp.setContentType(contentType);
		}
		
	}

	/**
	 * Retrieves from the data repository the lists of classes and properties which will be used as suggestions in the SPARQL interface.
	 * Called only once on startup (in IWBStart).
	 * 
	 * @return Future<?> object whose get() method returns null if the sub-thread has executed successfully, null otherwise (for test purposes only) 
	 */
	public static Future<?> initOntologyTermsSuggestions() {
		// Loading of classes and properties is performed in two stages:
		// First, only the classes and properties explicitly declared as such in the ontology are retrieved.
		// Then, all terms serving as entity types (classes) and statement predicates (properties) are retrieved in a parallel thread.
		// This is done to prevent queries to very big datasets (e.g. DBpedia) from blocking the startup while the results are being retrieved.
		classesList = Lists.newArrayListWithCapacity(1000);
		propertiesList = Lists.newArrayListWithCapacity(1000);
		
		ReadDataManager dm = ReadDataManagerImpl.getDataManager(Global.repository);
		fillUris(dm, "SELECT DISTINCT ?type WHERE { { ?type a rdfs:Class } UNION { ?type a owl:Class } } LIMIT 1000", classesList);
		Collections.sort(classesList);
		fillUris(dm, "SELECT DISTINCT ?property WHERE { { ?property a rdf:Property } UNION { ?property a owl:ObjectProperty } UNION { ?property a owl:DatatypeProperty } } LIMIT 1000", propertiesList);
		Collections.sort(propertiesList);
		
		log.info("Loaded classes and properties declared in the ontology");
		
		final List<String> fullClassesList = new ArrayList<String>(classesList);
		final List<String> fullPropertiesList = new ArrayList<String>(propertiesList);
		Runnable classInfoReader = new Runnable() {

			@Override
			public void run() {
				
				Set<String> classesSet = new HashSet<String>();
				Set<String> propertiesSet = new HashSet<String>();
				ReadDataManager dm = ReadDataManagerImpl.getDataManager(Global.repository);
				fillUris(dm, "SELECT DISTINCT ?type WHERE { ?x rdf:type ?type . } LIMIT 1000", classesSet);
				combineWithAndSort(fullClassesList, classesSet);
				classesList = fullClassesList;
				fillUris(dm, "SELECT DISTINCT ?property WHERE { ?x ?property ?y . } LIMIT 1000", propertiesSet);
				combineWithAndSort(fullPropertiesList, propertiesSet);
				propertiesList = fullPropertiesList;
				log.info("Loaded classes and properties based on usage");
			}
			
			private void combineWithAndSort(List<String> resultList, Set<String> tmpSet) {
				tmpSet.addAll(resultList);
				resultList.clear();
				resultList.addAll(tmpSet);
				Collections.sort(resultList);
			}
			
			
		};
		
		try {
			return TaskExecutor.instance().submit(classInfoReader);
		} catch(Exception e) {
			log.warn(e.getMessage());
			log.debug("Details: ", e);
		}
		
		return null;
	}
	
	private static void fillUris(ReadDataManager dm, String sQuery, Collection<String> collection) {
		try {
			TupleQueryResult tqr = dm.sparqlSelect(sQuery);
			try {
				while(tqr.hasNext()) {
					BindingSet bs = tqr.next();
					if(!bs.getBindingNames().isEmpty()) {
						Binding b = bs.iterator().next();
						if(b.getValue() instanceof URI) {
							String strForm = EndpointImpl.api().getRequestMapper().getReconvertableUri((URI)b.getValue(), true);
							if(!strForm.contains(":"))
								strForm = ":"+strForm;
							collection.add(strForm);
						}
					}
				}
			} finally {
				ReadDataManagerImpl.closeQuietly(tqr);
			}
		} catch (Exception e) {
			log.warn("Could not read the list from the backend");
			log.debug("Details: ", e);
		}
	}
	
	
}
