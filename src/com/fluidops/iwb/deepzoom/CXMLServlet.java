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

package com.fluidops.iwb.deepzoom;

import static com.fluidops.iwb.api.EndpointImpl.api;
import info.aduna.io.FileUtil;

import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.json.XML;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.query.BindingSet;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryResult;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.fluidops.ajax.components.PivotControl;
import com.fluidops.iwb.Global;
import com.fluidops.iwb.api.CacheManager;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.ImageResolver;
import com.fluidops.iwb.api.ReadDataManager;
import com.fluidops.iwb.api.ReadDataManagerImpl;
import com.fluidops.iwb.api.ReadDataManagerImpl.SparqlQueryType;
import com.fluidops.iwb.cms.util.IWBCmsUtil;
import com.fluidops.iwb.keywordsearch.KeywordSearchAPI;
import com.fluidops.iwb.model.Vocabulary;
import com.fluidops.iwb.server.IWBHttpServlet;
import com.fluidops.iwb.util.Config;
import com.fluidops.iwb.util.IWBFileUtil;
import com.fluidops.util.GenUtil;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

/**
 * Serve query results as CXML (for Pivot)
 * 
 * @author  pha
 */
@SuppressWarnings(value={"MTIA_SUSPECT_SERVLET_INSTANCE_FIELD","SIC_INNER_SHOULD_BE_STATIC_ANON", "MSF_MUTABLE_SERVLET_FIELD"}, justification="Checked")
public class CXMLServlet extends IWBHttpServlet
{
    private static final Logger logger = Logger.getLogger(CXMLServlet.class.getName());

    private static final long serialVersionUID = 4925086257072637342L;
    
    /**
     * Supported date formats
     */
    private long lastupdate = 0;
    private static DateFormat[] formatters = new DateFormat[]
        {
            new SimpleDateFormat("yyyy-MM-dd"),
            new SimpleDateFormat("yyyy"),
            new SimpleDateFormat()
        };
    
    
    /*
     * This map allows us to re-associate the identifiers in the image collections with the filenames (which we use internally as identifiers)
     * Currently, we do not have a way to control the identifiers when creating the image collections, they are just assigned in file order
     */
    private static Map<String,String> ids = new HashMap<String,String>();

    //contains true for every predicate that has a number-type
    private static Map<URI,URI> predicateTypes = new HashMap<URI,URI>();
    
    static boolean initialized = false;
    
    final static public boolean HASH_IMAGES = false;
    static HashSet<URI> notWantedProps = new HashSet<URI>();
    static HashSet<Value> notWantedObjs = new HashSet<Value>();
    static HashSet<Value> notWantedPics = new HashSet<Value>();
    


    protected static void initialize() 
    {
        if (initialized)
            return; 
        initialized = true;
        notWantedProps.add(RDFS.LABEL);
        notWantedProps.add(Vocabulary.FOAF.DEPICTION);
//      notWantedProps.add(EcmProvider.SAPMGR);
//      notWantedProps.add(EcmProvider.IFS);
        notWantedProps.add(ValueFactoryImpl.getInstance().createURI(EndpointImpl.api().getNamespaceService().defaultNamespace()+"href"));
        
        notWantedObjs.add(ValueFactoryImpl.getInstance().createURI("http://www.w3.org/2002/07/owl#Thing"));
        notWantedPics.add(ValueFactoryImpl.getInstance().createURI("http://upload.wikimedia.org/wikipedia/en/thumb/7/7e/Replace_this_image_male.svg/200px-Replace_this_image_male.svg.png"));
        notWantedPics.add(ValueFactoryImpl.getInstance().createURI("http://upload.wikimedia.org/wikipedia/en/thumb/e/ec/Replace_this_image_female.svg/200px-Replace_this_image_female.svg.png"));
        
        File file = IWBFileUtil.getFileInDataFolder("pivotCache");
        if (!file.exists() || !file.isDirectory()) {
            GenUtil.mkdir(file);
        }
        file = IWBFileUtil.getFileInDataFolder("pivotCache/imageCache");
        if (!file.exists() || !file.isDirectory()) {
        	GenUtil.mkdir(file);
        }
      

    }
    /**
     * We handle different types of requests:
     * 1) queries: these produce results according to the CXML standard, making reference to a collection.xml file 
     * 2) collection.xml files that contains the images for the query results
     * 3) the jpg images themselves
     */
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        String requestURI = req.getRequestURI();
        
        initialize();
        
        // this is the request for the images
        if(requestURI.endsWith("jpg") && requestURI.contains("dzimages")) 
        {   // The format of such a request is as follows: /pivot/dzimages/09/1113490945_files/6/0_0.jpg

            String filestring = requestURI.substring(req.getRequestURI().lastIndexOf("dzimages"));
            String filename = filestring.substring(filestring.indexOf("/")+1, filestring.indexOf("_"));
            String substring = requestURI.substring(req.getRequestURI().lastIndexOf("files"));
            int zoomLevel = Integer.parseInt(substring.substring(substring.indexOf("/")+1, substring.lastIndexOf("/")));

            try {
                DeepZoomCollection.handleTileRequest(filename, zoomLevel, resp);
            }
            catch (Exception e) {
            	logger.trace("Exception while loading images: " + e.getMessage(), e);
                // TODO: for now, problems when loading images are ignored, only the exception on the console is avoided
            }
            return;
        }
        
        // this is the request for the images
        if(requestURI.endsWith("jpg")) 
        {   // The format of such a request is as follows: /wikipedia/collection3_files/8/0_0.jpg
            // wikipedia identifies the name of the global collection (currently we assume there is only one dynamic collection)
            // collection3 identifies the query that has generated a query result, which is a subset of the global collection
            // The 8 is the zoom level (how deep we have zoomed in)
            // 0_0 identified the position of the tile (horizontal and vertical offset)

            int queryNumber = Integer.parseInt(requestURI.substring(requestURI.indexOf("collection")+10, requestURI.indexOf("_")));
            String substring = requestURI.substring(req.getRequestURI().lastIndexOf("files"));
            int zoomLevel = Integer.parseInt(substring.substring(substring.indexOf("/")+1, substring.lastIndexOf("/")));
            int x_Offset = Integer.parseInt(substring.substring(substring.lastIndexOf("/")+1, substring.lastIndexOf("_")));
            int y_Offset = Integer.parseInt(substring.substring(substring.lastIndexOf("_")+1, substring.lastIndexOf(".")));

            Vector<String> imageVector = getImagesFromCacheFile("imageCache", queryNumber);
            //
            try {
            	
                DeepZoomCollection.handleTileRequest(imageVector, zoomLevel, x_Offset, y_Offset, resp);
            }
            catch (Exception e) {
            	logger.trace("Exception while loading images: " + e.getMessage(), e);
                // TODO: for now, problems when loading images are ignored, only the exception on the console is avoided
            }
            return;
        }

        PrintStream  out =  new PrintStream(resp.getOutputStream(), false, "UTF-8");

        if(requestURI.endsWith(".xml")) 
        {
            resp.setContentType("text/xml");
            int collectionNumber = 0;

            collectionNumber = Integer.parseInt(requestURI.substring(requestURI.indexOf("collection")+10, requestURI.indexOf(".xml")));
            getFromCacheFile("collectionCache", collectionNumber, out);

            out.flush();
            out.close();
            return;
        }
        
        String q = PivotControl.decodeQuery(req.getParameter("q"));
        q = StringEscapeUtils.unescapeHtml( q );
        String uriParm = req.getParameter("uri");
        Repository repository = Global.repository;
        URI uri = null;
        if(uriParm!=null)
            uri = ValueFactoryImpl.getInstance().createURI(uriParm);

        if(q != null)
        {
            int maxEntities= 1000;
            try
			{
				maxEntities = Integer.parseInt(req.getParameter("maxEntities"));
			}
			catch (NumberFormatException e)
			{
				logger.debug("wrong number format in parameter 'maxEntities'");
			}
            int maxFacets = 0;
            
            try
			{
				maxFacets = Integer.parseInt(req.getParameter("maxFacets"));
			}
			catch (NumberFormatException e)
			{
				logger.debug("wrong number format in parameter 'maxFacets'");
			}
            
            int hash = hash(q+maxEntities+maxFacets);

            String res = null;
            
            validateCache();
            res = getFromCacheFile("resultCache", hash, out);
            
            if (res != null) {
                logger.trace("Result loaded from cache...");
                
                out.close();
            }
            else {
                handleQuery(q, uri, repository, out, req, maxEntities, maxFacets);
                out.close();
            }
            return;
        }
  
    }

   
    

    private void handleQuery(String q, URI uri, Repository repository, PrintStream out, HttpServletRequest req, int maxEntities, int maxFacets)
    {
		SparqlQueryType qt = null;
		try
		{
			qt = ReadDataManagerImpl.getSparqlQueryType(q, true);
		}
		catch (MalformedQueryException e)
		{
			// ignore: not a valid SPARQL query, treat it as keyword query
		}

        if (qt == SparqlQueryType.SELECT)
        {
            getStructuredSearchResult(q, "pivot", repository, out, req, maxEntities, maxFacets);
        }
        else if (qt == SparqlQueryType.CONSTRUCT)
        {
            getGraphResult(q, uri, "pivot", repository, out, req, maxEntities, maxFacets);
        }
        else 
        {
            try    
            {
                getUnstructuredSearchResult(q, "pivot", out, req, maxEntities, maxFacets);
            }
            catch(Exception e) 
            {
            	logger.error(e.getMessage(), e);
            }
        }
    }
    
    private void getStructuredSearchResult(String q, String collection, Repository repository, PrintStream out, HttpServletRequest req, int maxEntities, int maxFacets) 
    {   
        Vector<URI> predicates = new Vector<URI>();
        Map<URI, Map<URI, Set<Value>>> graph = new HashMap<URI, Map<URI, Set<Value>>>(); 
        
 
        String defaultNS = EndpointImpl.api().getNamespaceService().defaultNamespace();
        try 
        {
            ReadDataManager dm = EndpointImpl.api().getDataManager();
            TupleQueryResult res = dm.sparqlSelect(q, true);

            predicates.add(Vocabulary.DBPEDIA_ONT.THUMBNAIL);
            String key = "uri";

            // If no URI is specified in the query, we simply cluster by the first column

            if(!res.getBindingNames().contains("uri"))
                key = res.getBindingNames().get(0);

            for (String name : res.getBindingNames()) {
                if(name.equals("uri")||name.equals("img")) continue;
                predicates.add(ValueFactoryImpl.getInstance().createURI(defaultNS+name));
            }

            int counter=0;

            while (res.hasNext())
            {
                if(counter>=maxEntities)
                    break;

                BindingSet bindingSet = res.next();

                Value subjectValue = bindingSet.getBinding(key).getValue();
                if(!(subjectValue instanceof URI))
                    continue;
                URI subject = (URI) subjectValue; 

                Map<URI, Set<Value>> facets = null;
                if(graph.containsKey(subject)) 
                    facets = graph.get(subject);
                else
                {
                    facets = new HashMap<URI, Set<Value>>();
                    graph.put(subject, facets);
                }

                for (String name : res.getBindingNames()) {
                    URI predicate;
                    if(name.equals("uri")) continue;


                    //TODO: Perhaps we should get the images separately, without the need of specifying img in the query
                    //      This would of course be more expensive, but also more general
                    if(name.equals("img")) 
                        predicate = Vocabulary.DBPEDIA_ONT.THUMBNAIL;
                    else 
                        predicate = ValueFactoryImpl.getInstance().createURI(defaultNS+name);


                    Value value = bindingSet.getValue(name);

                    if (!notWantedObjs.contains(value)) {
                        Set<Value> facet = null;
                        if(facets.containsKey(predicate)) 
                            facet = facets.get(predicate);
                        else
                        {
                            facet = new HashSet<Value>();
                            facets.put(predicate, facet);
                        }
                        facet.add(value);
                    }
                }
            }
            renderCollection(graph, predicates, null, collection, "Query Result Collection", out, q, req, maxEntities, maxFacets);      

        } catch (RuntimeException e) { 
        	logger.error(e.getMessage(), e);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

    }


    private void getGraphResult(String q, URI uri, String collection, Repository repository, PrintStream out, HttpServletRequest req, int maxEntities, int maxFacets) 
    {        
        Vector<URI> predicates = new Vector<URI>();
        Map<URI, Map<URI, Set<Value>>> graph = new HashMap<URI, Map<URI, Set<Value>>>(); 
        HashMap<URI, Integer> facetCountMap = new HashMap<URI, Integer>();

        try 
        {
            ReadDataManager dm = EndpointImpl.api().getDataManager();
            GraphQueryResult queryRes = dm.sparqlConstruct(q, true);

            HashSet<Value> notWantedSubjs = new HashSet<Value>();

            // graph query result may contain duplicate statements
            // (actually a bug/feature of Sesame), so we filter them out
            // at the same time, we check whether the target URI is at the subject or object position
            // if it is at object position, we create an inverse statement
            //              while(queryRes.hasNext() && count++ < maxTriples)
            //              {
            //                  Statement s = queryRes.next();
            //                  queryResNoDups.add(s);
            //                  /* if(uri==null)
            //                      queryResNoDups.add(s);
            //                  else if(s.getSubject().equals(uri))
            //                      queryResNoDups.add(s);
            //                  else if(s.getObject().equals(uri))
            //                  {
            //                      URI predicate = s.getPredicate();
            //                      URI inverse = f.createURI(predicate.stringValue()+" of");
            //                        queryResNoDups.add(f.createStatement(s.getSubject(), inverse, uri));
            //                  }*/
            //              }
            while (queryRes.hasNext()){

                Statement s = queryRes.next();

                Resource subject = s.getSubject();
                if(!(subject instanceof URI)) 
                    continue;
                
                URI predicate = s.getPredicate();

                if (notWantedProps.contains(predicate)) {
                    continue;
                }

                Map<URI, Set<Value>> facets = null;
                if(graph.containsKey(subject)) 
                    facets = graph.get(subject);
                else
                {
                    facets = new HashMap<URI, Set<Value>>();
                    graph.put((URI)subject, facets);
                }

                if(!predicates.contains(predicate)) {

                    Value range = dm.getProp(predicate, RDFS.RANGE);

                    if(range==null) range = XMLSchema.STRING;
                    if(!(range instanceof URI))
                        range = XMLSchema.STRING;
                    predicateTypes.put(predicate, (URI)range);

                    predicates.add(predicate);
                    facetCountMap.put(predicate, 1);
                }
                else 
                    facetCountMap.put(predicate, facetCountMap.get(predicate)+1);
                
                Value object = s.getObject();

                if (!notWantedObjs.contains(object)) 
                {
                    Set<Value> facet = null;
                    if(facets.containsKey(predicate)) 
                        facet = facets.get(predicate);
                    else
                    {
                        facet = new HashSet<Value>();
                        facets.put(predicate, facet);
                    }
                    facet.add(object);
                }
            }

            for (Value v : notWantedSubjs) 
            {
                if(graph.containsKey(v)) 
                {
                    graph.remove(v);
                }
            }

            renderCollection(graph, predicates, facetCountMap, collection, EndpointImpl.api().getDataManager().getLabel(uri), out, q, req, maxEntities, maxFacets);


        } catch (RuntimeException e) { 
        	logger.error(e.getMessage(), e);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private String cleanLabel(String label)
    {
        StringBuilder sb = new StringBuilder();
        char[] ls = label.toCharArray();

        for (char c : ls)
        {
            // illegal control characters #x00-#x1f, #x7f (delete),
            // #xc280-#xc29f
            if (c < 32 || c == 127 || (c > 49791 && c < 49824))
                continue;

            // note: the following is the old heuristics; it /might/
            // be safer to employ a white list and encode everything, which is
            // not in one of the ranges #x20, #x28-#x3b, #x40-#x7a
            switch (c)
            {
            case '<':
            case '>':
            case '&':
            case '\'':
            case '"':
                sb.append("&#x");
                sb.append(String.format("%x", (int) c));
                sb.append(';');
                break;
            case '[':
            case ']':
                break;
                
            default:
                sb.append(c);
            }
        }

        return sb.toString();
    }

    @SuppressWarnings(value="DM_CONVERT_CASE", justification="checked")
    private void renderCollection(Map<URI, Map<URI, Set<Value>>> graph, Vector<URI> predicates, HashMap<URI, Integer> facetCountMap, String collection, String collectionName, PrintStream httpOut, String q, HttpServletRequest req, int maxEntities, int maxFacets) throws FileNotFoundException, IOException {
       
        PrintStream resultCache = new PrintStream(new FileOutputStream(getCacheFile("resultCache", hash(q))), false, "UTF-8");

        PrintStream out = new PrintStream(new DualOutputStream(httpOut, resultCache), false, "UTF-8");
        
        
        out.println("<?xml  version=\"1.0\"  encoding=\"utf-8\"?>");
        //out.println("<?xml-stylesheet type=\"text/xsl\" href=\"/int/pivot/pivot.xsl\"?>");
        out.println("<Collection Name=\""+XML.escape(collectionName)+"\" SchemaVersion=\"1.0\"");
        out.println(" xmlns=\"http://schemas.microsoft.com/collection/metadata/2009\"");
        out.println(" xmlns:p=\"http://schemas.microsoft.com/livelabs/pivot/collection/2009\"");
        out.println(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        out.println(" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">");

        Dimension d = new Dimension(200,200);   
        
        Writer collectionWriter = null;
        try
        {
            collectionWriter = new OutputStreamWriter( new FileOutputStream(getCacheFile("collectionCache", hash(q))), "UTF-8");
        }
        catch (IOException e)
        {
            logger.warn(e.getMessage());
        }
        

        collectionWriter.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"+
                "<Collection MaxLevel=\"8\" TileSize=\"256\" Format=\"jpg\" NextItemId=\"1001\" ServerFormat=\"Default\" xmlns=\"http://schemas.microsoft.com/deepzoom/2009\">\n"+
        "<Items>\n"); 

        // render facet categories
        out.println("<FacetCategories>");
        List<URI> displayedFacets = new LinkedList<URI>();
        
        // We limit the number of displayed facets (maxPivotFacets)
        // We rank facets by the number of entities that have a value for the given facet
        // The ranking only needs to be performed if maxFacets>0 (0 disables limitation)
        // and the number of facets is actually greater than maxFacets
        
        if(facetCountMap!=null && maxFacets>0 && facetCountMap.size()>maxFacets) {
            Set<Entry<URI, Integer>> s = facetCountMap.entrySet();

            for (int i = 0; i < maxFacets; i++) {
                int max = 0;
                Entry<URI, Integer> maxEntry = null;
                for (Entry<URI, Integer> e : s) {
                    if (e.getValue() > max) {
                        maxEntry = e;
                        max = e.getValue();
                    }
                }
                if (maxEntry == null) break;

                displayedFacets.add(maxEntry.getKey());
                s.remove(maxEntry);
            }
        }
        else 
            displayedFacets = predicates;
        Vector<URI> facetNames = new Vector<URI>();
        Vector<String> facetLabels = new Vector<String>();  
        
        // we may use any sort order here; currently, we sort alphabetically,
        // with the exception that the type facet is always on top
        Collections.sort(predicates, new Comparator<URI>() {
            public int compare(URI o1, URI o2){
                if (o1.equals(RDF.TYPE))
                    return -1;
                else if (o2.equals(RDF.TYPE))
                    return 1;
                else
                    return o1.stringValue().compareTo(o2.stringValue());
             }
          });

        for (URI predicate : predicates) 
        {
            ReadDataManager dm = EndpointImpl.api().getDataManager();
            if(dm.getLabel(predicate).equals("name")||facetNames.contains(predicate)) continue; // labels of names are unfortunately not unique, but must not occur as duplicates
            facetNames.add(predicate);


            if (displayedFacets.contains(predicate)) {
                String name = dm.getLabel(predicate);
                name = escape(name).trim();

                // TODO: only quick fix, has to be handled properly
                // facet labels need to be unique, disregarding capitalization
                if (!facetLabels.contains(name.toLowerCase())) {
                    facetLabels.add(name.toLowerCase());

                    String type;
                    
                    URI range = predicateTypes.get(predicate);
                    if(range==null)
                        range = XMLSchema.STRING;
                    if(range.equals(XMLSchema.DATETIME)||range.equals(XMLSchema.DATE))
                        type = "DateTime";
                    else if(range.equals(XMLSchema.FLOAT)||range.equals(XMLSchema.DOUBLE)||range.equals(XMLSchema.INT)||range.equals(XMLSchema.INTEGER)||range.equals(XMLSchema.LONG))
                        type = "Number";
                    else 
                        type =  "String"; //default
                        
                    
                    out.println("<FacetCategory Name=\""+XML.escape(name)+"\" Type=\""+type+"\" p:IsFilterVisible=\"true\"  p:IsMetaDataVisible=\"false\" />");
                    out.println("<FacetCategory Name=\""+XML.escape(name)+":\" Type=\"Link\" p:IsFilterVisible=\"false\" p:IsMetaDataVisible=\"true\" />");


                }
            }
        }
        out.println("<FacetCategory Name=\"Home\" Type=\"Link\" p:IsFilterVisible=\"false\" p:IsMetaDataVisible=\"true\" />");
        out.println("</FacetCategories>");

        // render items in collection
        String collectionID = "collection"+ hash(q) + ".xml";            
        out.println("<Items ImgBase=\""+collection+"/"+collectionID+"\">");
        
        int counter=0;
        Vector<String> imageVector = new Vector<String>();
//        queryImageVector.put(hash(q), imageVector);
        
        ImageLoader loader = new ImageLoader(api().getRequestMapper().getInternalUrlWithoutContext(req));

        for(Entry<URI, Map<URI, Set<Value>>> entry : graph.entrySet())
        {
        	Resource entity = entry.getKey();
            if(counter>=maxEntities)
                break;

            
            Map<URI, Set<Value>> facets = entry.getValue();
            
            // We assume that all entities are URIs. Perhaps need to re-evaluate this assumption
            URI uri = (URI) entity; 

            String img = null;
            
        	if ( IWBCmsUtil.isUploadedFile(uri) && ImageResolver.isImage(uri.stringValue()))
        		img=IWBCmsUtil.getAccessUrl(uri);
        	else
        	{
        		Set<Value> imgs = new HashSet<Value>();

        		if(facets.get(Vocabulary.DBPEDIA_ONT.THUMBNAIL)!=null)
        		{
        			imgs.addAll(facets.get(Vocabulary.DBPEDIA_ONT.THUMBNAIL));
        		}
        		else if(facets.get(Vocabulary.FOAF.IMG)!=null) 
        		{
        			imgs.addAll(facets.get(Vocabulary.FOAF.IMG));
        		}
        		if(imgs.size()>0)
        			img = imgs.iterator().next().stringValue();
        		else
        			// id: indicates that ID card should be generated
        			img = "id:"+entity.stringValue()+".jpg";
        	}
            String filename = ImageLoader.filename(img);
            if(!imageVector.contains(img)) //only need to load the image once
            {
                loader.createImageThreaded(uri, facets, img, collection);
                imageVector.add(img); 
                int imgID = imageVector.indexOf(img);
                collectionWriter.write("<I Id=\""+imgID+"\" N=\""+imgID+"\" Source=\"dzimages/"+ImageLoader.subdir(filename)+"/"+ ((filename.lastIndexOf(".")>-1)? filename.substring(0, filename.lastIndexOf(".")) : filename)+".xml\">"+
                        "<Size Width=\""+d.width+"\" Height=\""+d.height+"\"/></I>"); 
                
            }

            int imgID = imageVector.indexOf(img);

            ReadDataManager dm = EndpointImpl.api().getDataManager();
            String label = cleanLabel(dm.getLabel(uri));
            if(label.length()==0) continue;
            out.println("<Item Img=\"#"+imgID+"\" Id=\""+counter +"\" Href=\""+ EndpointImpl.api().getRequestMapper().getRequestStringFromValue(uri)+"\" Name=\""+ label+"\">");


            String facetString = "";// temp string as we do not know whether there are any facet values 

            for (Entry<URI,Set<Value>> facetEntry : facets.entrySet()) { 
            	URI predicate = facetEntry.getKey();
                if(predicate.equals(Vocabulary.DBPEDIA_ONT.THUMBNAIL)) continue;

                String name = dm.getLabel(predicate);
                if(name.equals("name")) continue;
                Set<Value> facetValues = facetEntry.getValue();

                String values = "";
                String exploreValues="";

                int facetValueCounter = 0;
                for(Value value:facetValues) {
                    if(value==null) continue;
                    if(facetValueCounter++>1000) break; //TODO: temp hack to prevent scalability problems, improve logic
                    label = cleanLabel(dm.getLabel(value));
                    
                    if (label.equals("")) continue;
                    String href =  EndpointImpl.api().getRequestMapper().getRequestStringFromValue(value);

                    if((value instanceof Literal) && !Config.getConfig().getLinkLiterals()) {
                    	href = "#";
                    }
                    
                    if (label.toLowerCase().startsWith("category:")) {
                        label = label.substring(9);
                    }                    
                    
                    URI range  = predicateTypes.get(predicate);
                    if(range==null)
                        values+="<String Value=\""+label+"\"/>";
                    else if(range.equals(XMLSchema.FLOAT)||range.equals(XMLSchema.DOUBLE)||range.equals(XMLSchema.INT)||range.equals(XMLSchema.INTEGER)||range.equals(XMLSchema.LONG))
                    {

                        try {
                            // Need to be compliant with xsd:decimal
                            values+="<Number Value=\""+(new BigDecimal(Double.parseDouble(label))).toPlainString()+"\"/>";
                        } catch (Exception e) {
                            logger.warn("Parsing problem with: " + label);
                        }
                    }
                    else if(range.equals(XMLSchema.DATETIME)||range.equals(XMLSchema.DATE))
                    {
                        
                        Date date = null;                       
                        // Loop over supported date formats
                        for (DateFormat formatter: formatters)
                        {
                            try
                            {
                                date = (Date) formatter.parse(label);
                                break;
                            }
                            // If date format is not supported..
                            catch (ParseException e)
                            {
                                logger.debug("Date format not supported: " + label + ". Using today instead.");
                            }
                        }
                        if(date!=null)
                        {
                            DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                            values+="<DateTime Value=\""+format.format(date)+"\"/>";
                        }
                    }
                    else 
                    	values+="<String Value=\""+label+"\"/>";                 
                    exploreValues+="<Link Href=\""+href+"\" Name=\""+label+"\"/>";
                    

                }
                name = escape(name).trim();
//                values = values.replace("]", "");
                if(values.length()>0 && displayedFacets.contains(predicate)) facetString+="<Facet Name=\""+XML.escape(name)+"\">"+values+"</Facet>";
                if(exploreValues.length()>0 && displayedFacets.contains(predicate)) facetString+="<Facet Name=\""+XML.escape(name)+":\">"+exploreValues+"</Facet>";

            }

            /*  
            //Query for related elements, currently not used 
            String href = "CONSTRUCT { ?uri <http://dbpedia.org/ontology/thumbnail> ?img . ?uri ?p ?o . } WHERE {  { ?uri ?relationship  <"+uri.stringValue()+"> } UNION {  <"+uri.stringValue()+"> ?relationship  ?uri  }  . ?uri <http://dbpedia.org/ontology/thumbnail> ?img . ?uri ?p ?o }";
            
            try
            {
                href=URLEncoder.encode(href, "UTF-8");
            }
            catch (UnsupportedEncodingException e)
            {
                logger.error(e.getMessage(), e);
            }
            facetString+="<Facet Name=\"Related\"><Link Href=\"query.cxml?q="+href+"\" Name=\"Explore Related\"/></Facet>";
             */
            
            if(!facetString.isEmpty())
                out.println("<Facets>"+facetString+"</Facets>");
            out.println("</Item>");
            counter++;
        }

        if(counter==0)
        {
            // should show some image indicating that nothing has been found, for now we just show a text
            out.println("<Item Img=\"#0\" Id=\"0\" Name=\"Nothing found\">");
            out.println("</Item>");
            
            URI uri = ValueFactoryImpl.getInstance().createURI("http://www.fluidops.com/Nothing Found");
    		// id: indicates that ID card should be generated
			String img = "id:"+uri.stringValue()+".jpg";

			String filename = ImageLoader.filename(img);

            Map<URI, Set<Value>> facets = new HashMap<URI, Set<Value>>();
            
			loader.createImageThreaded(uri, facets, img, collection);
			imageVector.add(img); 
			int imgID = 0;
			collectionWriter.write("<I Id=\""+imgID+"\" N=\""+imgID+"\" Source=\"dzimages/"+ImageLoader.subdir(filename)+"/"+ ((filename.lastIndexOf(".")>-1)? filename.substring(0, filename.lastIndexOf(".")) : filename)+".xml\">"+
                "<Size Width=\""+d.width+"\" Height=\""+d.height+"\"/></I>"); 
        
        }
        


        try {
            writeImageListToFile("imageCache", hash(q), imageVector);
        } catch (IOException e) {
            logger.warn(e.getMessage(), e);
        }
        
        // We wait until all image loading threads have finished, but at most one minute
        long wait=0;
        while(loader.threadCounter>0 && wait<60000)
        {
            try 
            {
                Thread.sleep(10);
                wait+=10;
            } catch (InterruptedException e1) 
            {
                logger.debug(e1.toString());
            }
        }   

        collectionWriter.write("</Items>\n</Collection>");        
        collectionWriter.close();
        
        out.println("</Items>");
        out.println("</Collection>");
        out.close();
          
    }
    
    /**
     * Some facet names in Pivot are reserved.
     * For these we have to slightly modify the name
     */
    private String escape(String name)
    {
        if (name.equalsIgnoreCase("description") || name.equalsIgnoreCase("name") || name.equalsIgnoreCase("home"))
            return name.concat(".");
        
        return name;
    }

    private void getUnstructuredSearchResult(String q, String collection, PrintStream out, HttpServletRequest req, int maxEntities, int maxFacets) {

        Map<URI, Map<URI, Set<Value>>> graph = new HashMap<URI, Map<URI, Set<Value>>>(); 

        HashMap<URI, Integer> facetCountMap = new HashMap<URI, Integer>();
        Vector<URI> predicates = new Vector<URI>();

        Set<String> uris = new HashSet<String>();
        
        if (q == null || q.trim().isEmpty()) {
            // nothing to do
        }
        else
        {
            try {
            	QueryResult<?> res = KeywordSearchAPI.search(q.trim());
            	
                while (res.hasNext()) 
                {
                    BindingSet bindingSet = (BindingSet) res.next();
                    uris.add(bindingSet.getValue("Subject").stringValue());
                }
            }
            catch (RuntimeException e) { 
            	logger.trace(e.getMessage(), e);
            } catch (Exception e) {
                logger.trace(e.getMessage(), e);
            }
        }
        RepositoryConnection con = null;
        
        try
        {
            con = Global.repository.getConnection();
        }
        catch (RepositoryException e)
        {
            logger.warn(e.getMessage(), e);
        }
        
        int counter = 0;
        ValueFactory f = ValueFactoryImpl.getInstance();
        
        for (String uriString : uris ) {
            if(counter>=maxEntities)
                break;


            
            URI uri = f.createURI(uriString);

            URI subject = uri;
            Map<URI, Set<Value>> facets = null;

            facets = new HashMap<URI, Set<Value>>();
            graph.put(subject, facets);

            List<Statement> statements = null;
            try
            {
                statements = con.getStatements(uri, null, null, false).asList();
            }
            catch (RepositoryException e)
            {
                logger.warn(e.getMessage(), e);
                statements = Collections.emptyList();
            }

            for(Statement s: statements){

                if (notWantedProps.contains(s.getPredicate())) {
                    continue;
                }

                URI predicate = s.getPredicate();
                
                Value range = EndpointImpl.api().getDataManager().getProp(predicate, RDFS.RANGE);
                if(range==null) range = XMLSchema.STRING;
                if(!(range instanceof URI))
                    range = XMLSchema.STRING;
                predicateTypes.put(predicate, (URI)range);
                
                if(!predicates.contains(predicate)) {
                    predicates.add(predicate);
                    facetCountMap.put(predicate, 1);
                }
                else {
                    facetCountMap.put(predicate, facetCountMap.get(predicate)+1);
                }
                if (!notWantedObjs.contains(s.getObject())) {
                    Set<Value> facet = null;
                    if(facets.containsKey(predicate)) 
                        facet = facets.get(predicate);
                    else
                    {
                        facet = new HashSet<Value>();
                        facets.put(predicate, facet);
                    }
                    facet.add(s.getObject());
                }
            }

            
            counter++;
        }

        try
        {
            renderCollection(graph, predicates, facetCountMap, collection, q, out, q, req, maxEntities, maxFacets);
        }
        catch (Exception e)
        {
            logger.error(e.getMessage(), e);
        }
//        queryCounter++;
    }


    static void readImageCollectionFile(String collection) {
    	InputStream stream=null;
    	try {
            stream = new FileInputStream(collection);

            Document doc = null;

            doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stream);
            XPathFactory factory = XPathFactory.newInstance();
            XPath xpath = factory.newXPath();
            XPathExpression expr = xpath.compile("//I");
            NodeList result = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
            for(int i=0;i<result.getLength();i++)
            {
                Node node = result.item(i);
                NamedNodeMap map = node.getAttributes();
                String source = map.getNamedItem("Source").getNodeValue();
                String id = map.getNamedItem("Id").getNodeValue();
                
                source=source.substring("dzimages/".length(), source.lastIndexOf(".xml"));
                ids.put(source,id);
            }
        } catch (RuntimeException e) { 
        	logger.error(e.getMessage(), e);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
        	IOUtils.closeQuietly(stream);
        }
    }
    
    private File getCacheFile(String dirString, int cacheHash) throws IOException {
        File dir = IWBFileUtil.getFileInDataFolder("pivotCache");
        if (!dir.exists() || !dir.isDirectory()) {
            GenUtil.mkdir(dir);
        }
        
        dir = IWBFileUtil.getFileInDataFolder("pivotCache/" + dirString);
        if (!dir.exists() || !dir.isDirectory()) {
        	GenUtil.mkdir(dir);
        }
        return new File(dir, String.valueOf(cacheHash));

    }
    
    private String getFromCacheFile(String dirString, int cacheHash, PrintStream out) throws IOException {
        
        logger.trace("trying to get file " + dirString + "/" + cacheHash + " from cache");
        
        File dir = IWBFileUtil.getFileInDataFolder("pivotCache/" + dirString);
        if (!dir.exists() || !dir.isDirectory()) {
            logger.trace("..not cached");
            return null;
        }
        
        File file = new File(dir, String.valueOf(cacheHash));
        
        if (!file.exists()) {
            logger.trace("..not cached");
            return null;
        }
        else {
            logger.trace("file in cache, retrieving...");            
            BufferedReader br = null;
            try {
            	Reader r = new InputStreamReader(new FileInputStream(file), "UTF-8");
            	br = new BufferedReader(r);
            	String line = br.readLine();
                while (line != null) {
                    out.println(line);
                    line = br.readLine();
                }
                return "suc";
            } finally {
            	IOUtils.closeQuietly(br);
            }
            
        }
//      return null;
    }
    
    
    private void writeImageListToFile(String dirString, int cacheHash, Vector<String> imageList) throws IOException {
        
        File dir = IWBFileUtil.getFileInDataFolder("pivotCache");
        if (!dir.exists() || !dir.isDirectory()) {
            GenUtil.mkdir(dir);
        }
        
        dir = IWBFileUtil.getFileInDataFolder("pivotCache/" + dirString);
        if (!dir.exists() || !dir.isDirectory()) {
        	GenUtil.mkdir(dir);
        }
        
        
        Writer fw = null;        
        try {
        	File file = new File(dir, String.valueOf(cacheHash));
        	fw = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
        	for (String s : imageList) {
                fw.write(s + "\n");
            }
        	fw.flush();
        } finally {
        	IOUtils.closeQuietly(fw);
        }
    }
    
    private Vector<String> getImagesFromCacheFile(String dirString, int cacheHash) throws IOException {
        
        File dir = IWBFileUtil.getFileInDataFolder("pivotCache/" + dirString);
        if (!dir.exists() || !dir.isDirectory()) {
            return null;
        }
        
        File file = new File(dir, String.valueOf(cacheHash));
        
        if (!file.exists()) {
            return null;
        }
        else {
            
            BufferedReader br = null;
            try {
            	Reader r = new InputStreamReader(new FileInputStream(file), "UTF-8");
            	br = new BufferedReader(r);
            	Vector<String> res = new Vector<String>();
                String line = br.readLine();
                while (line != null) {
                    res.add(line);
                    line = br.readLine();
                }
                return res;
            } finally {
            	IOUtils.closeQuietly(br);
            }
            
        }
//      return null;
    }
    
    private void validateCache()
    {
        
        if(lastupdate<CacheManager.getLastupdate())
        {
            try
            {
                FileUtil.deleteDir(IWBFileUtil.getFileInDataFolder("pivotCache/resultCache"));
            }
            catch (IOException e)
            {
                logger.debug("Pivot result cache could not be deleted: "+e.getMessage());
            }
            lastupdate=System.currentTimeMillis();
        }
        
    }
    
    
    /* 
     * We use a hash code identifying query results /collections in the (server side) cache.
     * We used to do the hashing only based on the query string, however, the query result may change over time.
     * As Pivot internally also does some caching of image collections, if it sees that it already knows a collection, 
     * it will potentially display outdated images.
     * We thus include a timestamp of the last update in the hashing, i.e. the hash code will change after an update. 
     * 
     */
    int hash(String query)
    {
        return (query+Long.toString(lastupdate)).hashCode();
        
    }

}
