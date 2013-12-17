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

import java.io.IOException;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.fluidops.iwb.ui.configuration.ConfigurationFormBase;
import com.fluidops.iwb.ui.configuration.SparqlEditorConfigurationFormElement;




/**
 * An extension of the SparqlServlet for simple query editing purposes used in {@link ConfigurationFormBase} 
 * to render {@link SparqlEditorConfigurationFormElement} (no query submission)
 * 
 * @author ango
 *
 */
public class SparqlEditorServlet extends SparqlServlet {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -2979553128594455010L;


	protected static final Logger log = Logger.getLogger(SparqlEditorServlet.class);



	
	/**
	 * Print simple sparql editor (no query submission)
	 * 
	 * @param req
	 * @param resp
	 * @throws IOException 
	 */
	@Override
	protected void handle(HttpServletRequest req, HttpServletResponse resp) throws IOException{
		
		try {			
			
			ServletOutputStream outputStream = resp.getOutputStream();
			printQueryInterface(req, resp, outputStream, "com/fluidops/iwb/server/sparqleditor");
			return;	
		
		}
		catch (IOException e) {
			log.error("Error: ", e);
			throw e;
		}       
    }
	
}
