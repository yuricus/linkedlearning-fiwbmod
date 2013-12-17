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
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.color.CMMException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;

import com.fluidops.iwb.util.IWBFileUtil;

public class DeepZoomCollection
{
    private static Map<String,BufferedImage> cachedImages = Collections.synchronizedMap(new HashMap<String,BufferedImage>());
    
    private static BufferedImage resize(BufferedImage image, int width, int height) {
        if(width<1) width=1;  //may happen due to rounding
        if(height<1) height=1;
        int imageType;
         
        int size=width;
        if(height>size)
            size=height;

        if(image==null) //empty image 
        {    
            imageType = BufferedImage.TYPE_INT_ARGB_PRE;


            BufferedImage resizedImage = new BufferedImage(size, size,
                    imageType);

            Graphics2D gout = resizedImage.createGraphics();
            //gout.setComposite(AlphaComposite.Xor);
            gout.setBackground(new Color(238,241,245));


            gout.clearRect(0, 0, size-1, size-1);
            gout.dispose();
            return resizedImage;
        }        

        
        if(image.getType()!=BufferedImage.TYPE_CUSTOM)
        {
            if(width<1) width=1;  //may happen due to rounding
            if(height<1) height=1;

            
            BufferedImage resizedImage = new BufferedImage(size, size,
                    BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = resizedImage.createGraphics();
            g.setBackground(new Color(238,241,245));
            g.clearRect(0, 0, size-1, size-1);
            g.drawImage(image, (size-width)/2, (size-height)/2, width, height, null);
            g.dispose();
            return resizedImage;
        }

        else 
        {    
            imageType = BufferedImage.TYPE_INT_ARGB_PRE;


            BufferedImage resizedImage = new BufferedImage(size, size,
                    imageType);

            Graphics2D gout = resizedImage.createGraphics();
            //gout.setComposite(AlphaComposite.Xor);
            gout.setBackground(new Color(238,241,245));


            gout.clearRect(0, 0, size-1, size-1);
            gout.drawImage(image, (size-width)/2, (size-height)/2, width, height, null);
            gout.dispose();
            return resizedImage;
        }        


    } 
    
    public static void handleTileRequest(String fname, int zoomLevel, HttpServletResponse resp)
    {

    	int resizeFactor = (int)java.lang.Math.pow ( 2, 6-zoomLevel);

    	// standard size of DZC tiles
    	int width = 200;
    	int height = 200;

    	BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

    	Graphics2D ig2 = bi.createGraphics();
    	ig2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    	ig2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

    	BufferedImage img;

    	File file = IWBFileUtil.getFileInDataFolder(
    			"pivotCache/imageCache/"+fname);
    	try
    	{
    		img = ImageIO.read( file);
    	}
    	catch(CMMException e)  // some images cause ImageIO to choke
    	{
    		img = new BufferedImage(1,1,BufferedImage.TYPE_INT_RGB);
    	}
    	catch(NullPointerException e)  // some images cause ImageIO to choke with NPE, seems to be bug in ImageIO
    	{
    		img = new BufferedImage(1,1,BufferedImage.TYPE_INT_RGB);
    	}
    	catch(IIOException e)  // timing problem, image requested but not yet available
    	{
    		img = new BufferedImage(1,1,BufferedImage.TYPE_INT_RGB);
    		// do not cache image, as we may succeed next time
    	}
    	catch (Exception e) // any other problem
    	{
    		img = new BufferedImage(1,1,BufferedImage.TYPE_INT_RGB);
    		// do not cache image, as we may succeed next time
    	}


    	Dimension d = getImageDimensions(img);

    	if (d.width > d.height)
    		ig2.drawImage( resize(img, (Math.max(d.width, 200))/resizeFactor, d.height*200/d.width/resizeFactor), 0, 0, null );
    	else
    		ig2.drawImage( resize(img, d.width*200/d.height/resizeFactor, (Math.max(d.height, 200))/resizeFactor), 0, 0, null );

    	try
    	{
    		ImageIO.write(bi, "JPG", resp.getOutputStream());
    	}
    	catch (IOException e)
    	{
    		// Sometimes we get an exception if the remote HTTP connection was closed
    		// This does not seem to be a problem though
    		System.out.println("Remote connection was closed, Tile was not delivered");
    	}

    }

    
    private static Dimension getImageDimensions(BufferedImage img)
	{

            if(img==null) return new Dimension(200,200);  // this image is not available
            int maxLength = img.getWidth();
            if(img.getHeight()>maxLength) maxLength=img.getHeight();
            
            double scale = 1;
            if(maxLength>200)
                scale=(double)200/maxLength;
                       
            return new Dimension((int)(img.getWidth()*scale),(int)(img.getHeight()*scale));

	}


	public static void handleTileRequest(Vector<String> imageVector, int zoomLevel, int x_Offset, int y_Offset, HttpServletResponse resp)
    {
        
        int resizeFactor = (int)java.lang.Math.pow ( 2, 8-zoomLevel);

        //System.out.println("Zoom Level: "+zoomLevel);

        
        // standard size of DZC tiles
        int width = 256;
        int height = 256;
				

        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        Graphics2D ig2 = bi.createGraphics();
        ig2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        ig2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        BufferedImage img;
        for(int x=0;x<resizeFactor;x++)
        {
            for(int y=0;y<resizeFactor;y++)
            {
            	String image ="";
            	
            	if(imageVector != null )
            	{
            		int imgNumber = interleave(resizeFactor*y_Offset+y,resizeFactor*x_Offset+x);

            		if(imgNumber>=imageVector.size()) 
            			continue;

            		image = imageVector.elementAt(imgNumber);
            	}

                if(cachedImages.containsKey(image) && imageVector != null) {
                    img = cachedImages.get(image);
                    //if(img==null) continue;  // this image is not available
                }
                else
                {
                	File file = null;

                    String filename = ImageLoader.filename(image);
                    file = IWBFileUtil.getFileInDataFolder(
                            "pivotCache/imageCache/"+ImageLoader.subdir(filename)+"/"+filename);

                    try
                    {
                        img = ImageIO.read( file);
                        cachedImages.put(image, img);
                    }
                    catch(CMMException e)  // some images cause ImageIO to choke
                    {
                        System.out.println(image + ": "+e.toString());
                        img = new BufferedImage(1,1,BufferedImage.TYPE_INT_RGB);
                        cachedImages.put(image, img);
                    }
                    catch(NullPointerException e)  // some images cause ImageIO to choke with NPE, seems to be bug in ImageIO
                    {
                        System.out.println(image + ": "+e.toString());
                        img = new BufferedImage(1,1,BufferedImage.TYPE_INT_RGB);
                        cachedImages.put(image, img);
                    }
                    catch(IIOException e)  // timing problem, image requested but not yet available
                    {
                        System.out.println(image + ": "+e.toString());
                        img = new BufferedImage(1,1,BufferedImage.TYPE_INT_RGB);
                        // do not cache image, as we may succeed next time
                    }
                    catch (Exception e) // any other problem
                    {
                        System.out.println(image + ": "+e.toString());
                        img = new BufferedImage(1,1,BufferedImage.TYPE_INT_RGB);
                        // do not cache image, as we may succeed next time
                    }

                }

                Dimension d = new Dimension(200,200);
                
                if(cachedImages.containsKey(image)) {
                    BufferedImage bimg = cachedImages.get(image);
                    d = getImageDimensions(bimg);
                }

                if (d.width > d.height)
                    ig2.drawImage( resize(img, (Math.max(d.width, 200))/resizeFactor, d.height*200/d.width/resizeFactor), (256*x)/resizeFactor, (256*y)/resizeFactor, null );
                else
                    ig2.drawImage( resize(img, d.width*200/d.height/resizeFactor, (Math.max(d.height, 200))/resizeFactor), (256*x)/resizeFactor, (256*y)/resizeFactor, null );

            }

        }
        try
        {
            ImageIO.write(bi, "JPG", resp.getOutputStream());
        }
        catch (IOException e)
        {
            // Sometimes we get an exception if the remote HTTP connection was closed
            // This does not seem to be a problem though
            System.out.println("Remote connection was closed, Tile was not delivered");
        }
             
    }
    // implementation of Morton code
    // input: x and y are 2 dimensional coordinates
    // return value is the Morton code
    static  int interleave(int x, int y)
    {
        int result = 0;
        int position = 0;
        int bit = 1;
     
        while(bit <= x || bit <= y)
        {
            if((bit & x) > 0)
                result |= 1 << (2*position+1);
            if((bit & y) > 0)
                result |= 1 << (2*position);
     
            position += 1;
            bit = 1 << position;
        }
        return result;
    }
}
