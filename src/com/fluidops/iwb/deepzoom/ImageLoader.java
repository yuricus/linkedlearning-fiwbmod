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

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;

import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.ReadDataManager;
import com.fluidops.iwb.util.Config;
import com.fluidops.iwb.util.IWBFileUtil;
import com.fluidops.util.GenUtil;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

public class ImageLoader
{
    private static final Logger logger = Logger.getLogger(ImageLoader.class.getName());
    
    int threadCounter=0;

    String serverURL = null;
    
    
    public ImageLoader(String serverURL)
    {
        this.serverURL = serverURL;
    }
    
    /**
     * Gets the image for an entity.
     * First tries to load the image, if an image location is given
     * In case of failure, it creates an ID card.
     * 
     */
    public void getImage(URI uri, Map<URI, Set<Value>> facets, String url, File dir, String filename) 
    {           
        
        if(!dir.exists())
            GenUtil.mkdir(dir);
        File file = new File(dir, filename);
        if(file.exists()) return;

        // image URLs that start with id indicate that an ID card should be generated
        if(url.startsWith("id:"))
        {
            altImage(uri, facets, url, file);   
        }
        else 
        {
            // try to load image, generate ID card in case of failure
            if(!loadImage(url, file))
                altImage(uri, facets, url, file);
        }

    }

    
    /**
     * Selects an alternative strategy to create image
     * 
     */
    public void altImage(URI uri, Map<URI, Set<Value>> facets, String url, File file) 
    {   
        if(Config.getConfig().getPivotGoogleImages())
            {
            if(!loadGoogleImage(uri, facets, file))
                generateIDCard(uri, facets, url, file);
            }
        else
            generateIDCard(uri, facets, url, file);
    }
    
    /**
     * Loads an image from a URL to a local file
     * @param uri - The URL from which to obtain the image
     * @param file - The file to store the image to
     * @return
     */
    
    public boolean loadGoogleImage(URI uri, Map<URI, Set<Value>> facets, File file) 
    {           
        String url;
        URL u;
        InputStream is = null;
        BufferedInputStream dis = null;
        int b;
        try
        {
            URL googleUrl;
            ReadDataManager dm = EndpointImpl.api().getDataManager();
            String label = dm.getLabel(uri);
            // TODO: currently hard coded logic for data sets 
            // should find more flexible logic
            if(uri.stringValue().startsWith("http://www.ckan.net/"))
                label+=" logo";

            googleUrl = new URL(
                    "http://ajax.googleapis.com/ajax/services/search/images?v=1.0&"
                    + "q="+URLEncoder.encode(label, "UTF-8"));

            URLConnection connection = googleUrl.openConnection();
            connection.addRequestProperty("Referer", "http://iwb.fluidops.com/");


            String content = GenUtil.readUrl(connection.getInputStream());

            try 
            {
                JSONObject json = new JSONObject(content);

                JSONObject response = json.getJSONObject("responseData");
                JSONArray results = response.getJSONArray("results");
                JSONObject result = results.getJSONObject(0);
                url = result.getString("unescapedUrl");

            }

            catch(JSONException e) 
            {
                return false;
            }

            u = new URL(url);


            try {
                is = u.openStream();    
            }
            catch (IOException e)
            {
                System.out.println("File could not be found: " + url);

                // quick fix: 200px-Image n/a, try smaller pic (for Wikipedia images)
                url = url.replace("200px", "94px");
                u = new URL(url);
                try {
                    is = u.openStream();
                }
                catch (IOException e2) {
                    System.out.println("also smaller image not available");
                    return false;
                }
            }
            dis = new BufferedInputStream(is);
            BufferedOutputStream out = null;
            try {
	            out = new BufferedOutputStream(new FileOutputStream(file));
	            while ((b = dis.read()) != -1)
	            {
	                out.write(b);
	            }
	            out.flush();
            } finally {
            	IOUtils.closeQuietly(out);
            }
        }
        catch (MalformedURLException mue)
        {
            logger.error(mue.getMessage(), mue);
            return false;

        }
        catch (IOException ioe)
        {
            logger.error(ioe.getMessage(), ioe);
            return false;
        }
        finally
        {
        	IOUtils.closeQuietly(is);
        	IOUtils.closeQuietly(dis);
        }
        return true;
    }

    
    
    /**
     * Loads an image from a URL to a local file
     * @param url - The URL from which to obtain the image
     * @param file - The file to store the image to
     * @return
     */
    
    public boolean loadImage(String url, File file) 
    {           
        URL u;
        InputStream is = null;
        BufferedInputStream dis = null;
        int b;
        try
        {
            try {
                u = new URL(url);
            }
            catch (MalformedURLException e)
            {
                // this is the case if we work with relative URLs (in eCM)
                // just prepend the base URL#
                u = new URL(serverURL+url);
            }

            try {
                is = u.openStream();    
            }
            catch (IOException e)
            {
                System.out.println("File could not be found: " + url);

                // quick fix: 200px-Image n/a, try smaller pic (for Wikipedia images)
                url = url.replace("200px", "94px");
                u = new URL(url);
                try {
                    is = u.openStream();
                }
                catch (IOException e2) {
                    System.out.println("also smaller image not available");
                    return false;
                }
            }
            dis = new BufferedInputStream(is);
            BufferedOutputStream out = null;
            try {
            	out = new BufferedOutputStream(new FileOutputStream(file));
	            while ((b = dis.read()) != -1)
	            {
	                out.write(b);
	            }
	            out.flush();
            } finally {
            	IOUtils.closeQuietly(out);
            }
        }
        catch (MalformedURLException mue)
        {
            logger.error(mue.getMessage(), mue);
            return false;

        }
        catch (IOException ioe)
        {
            logger.error(ioe.getMessage(), ioe);
            return false;
        }
        finally
        {
            IOUtils.closeQuietly(dis);
        }
        return true;
    }
    
    private void generateIDCard(URI uri, Map<URI, Set<Value>> facets, String url, File file)
    {
        int width = 200;
        int height = 200;
        
        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        Graphics2D ig2 = bi.createGraphics();
        ig2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        ig2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        /* Special ID card handling for certain entity types */
        
        /*  TODO: special images based on type
        if(facets.containsKey(RDF.TYPE)) {
            Set<Value> facet = facets.get(RDF.TYPE);

            for(Value v : facet)
            {
                if(v.equals(Vocabulary.DCAT_DATASET))
                {

                    Image img = null;
                    try
                    {
                        img = ImageIO.read( new File( "webapps/ROOT/images/rdf.jpg" ) );
                    }
                    catch (MalformedURLException e)
                    {
                        logger.error(e.getMessage(), e);
                    }
                    catch (IOException e)
                    {
                        logger.error("Could not get image");
                    }

                    ig2.drawImage( img, 0, 0, null );        
                    break;
                }
            }
        } */
        
        
        
        String label = EndpointImpl.api().getDataManager().getLabel(uri);
        Font font = new Font(Font.SANS_SERIF, Font.BOLD, 20);
        ig2.setFont(font);

        FontMetrics fontMetrics = ig2.getFontMetrics();
        int labelwidth = fontMetrics.stringWidth(label);
        if(labelwidth>=width)
        {
            int fontsize = 20*width/labelwidth;
            font = new Font(Font.SANS_SERIF, Font.BOLD, fontsize);
            ig2.setFont(font);
            fontMetrics = ig2.getFontMetrics();
        }
        
        
        
        int x = (width - fontMetrics.stringWidth(label)) / 2;
        int y = (fontMetrics.getAscent() + (height - (fontMetrics.getAscent() + fontMetrics.getDescent())) / 2);
        
        ig2.setPaint(Color.black);
        
        ig2.drawString(label, x, y);


        BufferedOutputStream out;
        try
        {
            out = new BufferedOutputStream(new FileOutputStream(file));
            ImageIO.write(bi, "PNG", out);
            out.flush();
            out.close();
            
        }
        catch (IOException e)
        {
            logger.error(e.getMessage(), e);
        }        
    }

    public void loadAllImages(Repository rep, URI img, String collection)
    {

        List<Statement> images=null;
        /* try
        {
            con = WikiServlet.repository.getConnection();
        
            images = con.getStatements(null, Vocabulary.DEPICTION, null, false).asList();
        }
        catch (RepositoryException e)
        {
            logger.error(e.getMessage(), e);
        }
        for(Statement s:images)
        {*/
        
        
        try {
			RepositoryConnection con = rep.getConnection();
			TupleQuery q = null;
			try {
				q = con.prepareTupleQuery(QueryLanguage.SPARQL,
						"SELECT ?uri ?img { ?uri <http://dbpedia.org/ontology/thumbnail> ?img }");

				TupleQueryResult res = q.evaluate();
				int rowCounter = 0;
				while (res.hasNext()) {

					BindingSet stmt = res.next();

					String imgURL = stmt.getBinding("img").getValue().stringValue();
					URI uri = (URI)stmt.getBinding("uri").getValue();
					if (threadCounter > 50)
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							logger.error(e.getMessage(), e);
						}
					Loader t = new Loader(uri, null, imgURL, collection);
					t.start();
					threadCounter++;
				}

			} finally {
				con.close();
			}
		} catch(RuntimeException e) { 
			logger.error(e.getMessage(), e);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
    }      
    
    
    public void createImageThreaded(URI uri, Map<URI, Set<Value>> facets, String imgURL, String collection)
    {

                Loader t = new Loader(uri, facets, imgURL, collection);
                t.start();
               
    }  
     
    @SuppressWarnings(value="URF_UNREAD_FIELD", justification="Checked")
    public class Loader extends Thread {
        URI uri;
        String img;
        String collection;
        Map<URI, Set<Value>> facets;
        
        public Loader(URI uri, Map<URI, Set<Value>> facets, String img, String collection) {
            this.img=img;
            this.uri=uri;
            this.collection = collection;
            this.facets = facets;
        }
        public void run() {
        	threadCounter++;
            String filename = filename(img);
            File directory = IWBFileUtil.getFileInDataFolder("pivotCache/imageCache/"+subdir(filename));    
            getImage(uri, facets, img, directory, filename);
            threadCounter--;

        }
    }
    
    
    public static String filename(String imagefile) {
        try
        {
            return URLEncoder.encode(Integer.toString(imagefile.hashCode()) /*.substring(imagefile.lastIndexOf("/")+1)*/, "UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    public static String subdir(String imagefile) {
        //return ".";
        if(imagefile.length()>8)
            return imagefile.substring(6,8);
        else return "aa";  //default dir for short names 
    }
    
    public static void main(String[] args)
    {
        URL url;
        try
        {
            url = new URL(
                    "http://ajax.googleapis.com/ajax/services/search/images?v=1.0&"
                    + "q=dbpedia");

            URLConnection connection = url.openConnection();
            connection.addRequestProperty("Referer", "http://iwb.fluidops.com/");

            
            String content = GenUtil.readUrl(connection.getInputStream());

            JSONObject json = new JSONObject(content);
            System.out.println(json);
        }
        catch (RuntimeException e)
        {
            logger.error(e.getMessage(), e);
        }
        catch (Exception e)
        {
            logger.error(e.getMessage(), e);
        }
    
    }
    
}
