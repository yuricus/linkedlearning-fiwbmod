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

package com.fluidops.iwb.widget.system;


import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.rio.RDFFormat;

import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FHTML;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.ReadDataManager;
import com.fluidops.iwb.model.ParameterConfigDoc;
import com.fluidops.iwb.widget.AbstractWidget;
import com.fluidops.iwb.widget.WidgetEmbeddingError;
import com.fluidops.iwb.widget.WidgetEmbeddingError.ErrorType;
import com.fluidops.util.StringUtil;

/**
 * Widget to export ontology / provide link for external editors (such as Protege)
 * 
 * @author pha
 *
 */
public class ExportOntologyWidget extends AbstractWidget<ExportOntologyWidget.Config> {

	public static class Config {
		
		@ParameterConfigDoc(desc="The URI of the ontology", required=true)
		public URI ontologyURI;
		
		@ParameterConfigDoc(desc="The display label for the link", required=true)
		public String label;
	}


	@Override
	protected FComponent getComponent(String id) {

		Config c = get();
		if (c==null)
			return WidgetEmbeddingError.getErrorLabel(id, ErrorType.INVALID_WIDGET_CONFIGURATION, "configuration parameters missing");

        ReadDataManager dm = EndpointImpl.api().getDataManager();
        
        Statement ontologyDeclaration = dm.searchOne(c.ontologyURI, RDF.TYPE, OWL.ONTOLOGY);
        if(ontologyDeclaration==null || ontologyDeclaration.getContext()==null)
        {
        	return WidgetEmbeddingError.getErrorLabel(id, ErrorType.EXCEPTION, "Ontology can not be found");
       	
        }
        
		String location = EndpointImpl.api().getRequestMapper().getContextPath()+"/sparql" +
				"?query=["+StringUtil.urlEncode(ontologyDeclaration.getContext().stringValue())+"]&queryType=context"+"&format="+RDFFormat.RDFXML.getName()+"&infer=false";
		
		String link = "<a href=\"" + location + "\">" + c.label + "</a>";

		return new FHTML(id, link);
		
	}
	
	
	@Override
	public String getTitle() {
		return "Export ontology widget";
	}

	@Override
	public Class<?> getConfigClass() {
		return Config.class;
	}
	
}
