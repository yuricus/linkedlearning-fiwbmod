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

package com.fluidops.iwb.model;

import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;

import com.fluidops.iwb.api.EndpointImpl;

/*
 * We here define some standard vocabularies to be reused across domains
 * The idea is to adopt well established ontologies for this purpose, to
 * avoid unnecessary heterogeneity on the schema level 
 */
public class Vocabulary 
{
    // Convenience variables used for readability
    private static final ValueFactory VF = ValueFactoryImpl.getInstance();
    private static final String SYSTEM_NAMESPACE = EndpointImpl.api().getNamespaceService().systemNamespace();
    
    public static final Literal TRUE =  VF.createLiteral(true);
	// Convenience access to values 
    public static final Literal FALSE =  VF.createLiteral(false);
    
    
    
    ///////////////////////////////////////////////////////////////////////////
    /////////////////////////// System Vocabulary /////////////////////////////
    ///////////////////////////////////////////////////////////////////////////
    //    !!! NOTE: be particularly careful making some changes here  !!!    //
    ///////////////////////////////////////////////////////////////////////////
    // general system-specific vocabulary
    public static class SYSTEM
    {
	    // file handling
	    public static final URI ATTACHEDFILE = VF.createURI(SYSTEM_NAMESPACE,"attachedFile");
	
	    // user handling
	    public static final URI USER = VF.createURI(SYSTEM_NAMESPACE + "User");
	    public static final URI UNKNOWNUSER = VF.createURI(SYSTEM_NAMESPACE + "user/unknown");
	    
	    // wiki pages
    	public static final URI WIKI = VF.createURI(SYSTEM_NAMESPACE, "wiki");
    	
    	// Placeholder for the context value to be used as the resource of the search results page.
    	// FIXME: A meaningful identifier of the actual query eventually should be eventually used for this purpose.
    	public static final URI SEARCH_VALUE_CONTEXT = VF.createURI(SYSTEM_NAMESPACE, "SearchValueContext");
    }

    // vocabulary for context management
    public static class SYSTEM_CONTEXT
    {
		public static final URI ISEDITABLE = VF.createURI(SYSTEM_NAMESPACE, "editable");
		public static final URI CONTEXT = VF.createURI(SYSTEM_NAMESPACE + "Context");
	    public static final URI EMPTYCONTEXT = VF.createURI(SYSTEM_NAMESPACE + "EmptyContext");
	    public static final URI METACONTEXT = VF.createURI(SYSTEM_NAMESPACE + "MetaContext");
	    public static final URI VOIDCONTEXT = VF.createURI(SYSTEM_NAMESPACE + "VoIDContext");
	    public static final URI ALLCONTEXT = VF.createURI(SYSTEM_NAMESPACE  + "AllContext");
	    public static final URI INPUTPARAMETER = VF.createURI(SYSTEM_NAMESPACE + "inputParameter");
	    public static final URI CONTEXTSRC = VF.createURI(SYSTEM_NAMESPACE + "contextSrc");
	    public static final URI CONTEXTGROUP = VF.createURI(SYSTEM_NAMESPACE + "contextGroup");
	    public static final URI CONTEXTSTATE = VF.createURI(SYSTEM_NAMESPACE + "contextState");
	    public static final URI CONTEXTTYPE = VF.createURI(SYSTEM_NAMESPACE + "contextType");
    }
    
    // Vocabulary for ontology bootstrap mechanism
    public static class SYSTEM_ONTOLOGY
    {
	    private static final String ONTOLOGY_PREFIX = "http://www.fluidops.com/";
	    
	    public static final String ONTOLOGY_NAME_PREFIX = ONTOLOGY_PREFIX + "name/";
	    public static final String ONTOLOGY_CONTEXT_PREFIX = ONTOLOGY_PREFIX + "ontologyContext/";
    }
    
    public static class OWL
    {
    	public static final String NAMESPACE = "http://www.w3.org/2002/07/owl#";
    	
    	public static final URI VERSION_INFO = VF.createURI(NAMESPACE, "versionInfo");

    	public static final URI VERSION_IRI = VF.createURI(NAMESPACE, "versionIRI");
    }
    
    
    /**
     * BBC Ontologies
     */
    public static class BBC
    {
	    public static final String NAMESPACE = "http://www.bbc.co.uk/ontologies/";
	    
	    public static final URI ORIGIN = VF.createURI(NAMESPACE, "origin");
	    public static final URI OWNER = VF.createURI(NAMESPACE, "owner");
    }

    /**
     * DBpedia ontology
     */
    public static class DBPEDIA_ONT
    {
    	public static final String NAMESPACE = "http://dbpedia.org/ontology/";
	    
        public static final URI THUMBNAIL = VF.createURI(NAMESPACE, "thumbnail");
	    public static final URI TYPE_ALBUM = VF.createURI(NAMESPACE, "Album");
	    public static final URI TYPE_MUSICALWORK = VF.createURI(NAMESPACE, "MusicalWork");
	    public static final URI ALBUM = VF.createURI(NAMESPACE, "album");    
	    public static final URI ARTIST = VF.createURI(NAMESPACE, "artist");
	    public static final URI YEAR = VF.createURI(NAMESPACE, "year");
	    public static final URI GENRE = VF.createURI(NAMESPACE, "genre");
	    public static final URI TRACK =  VF.createURI(NAMESPACE, "trackNumber");
	    public static final URI COORDINATES = VF.createURI(NAMESPACE, "coordinates");
	    public static final URI COMPANY = VF.createURI(NAMESPACE, "Company");
	    public static final URI FORMATION_DATE = VF.createURI(NAMESPACE, "formationDate");
	    public static final URI NUMBER_OF_STAFF = VF.createURI(NAMESPACE, "numberOfStaff");
	    public static final URI INDUSTRY = VF.createURI(NAMESPACE, "industry");
	    
	    
    }
    
    /**
     * Drugbank vocabulary
     */
    public static class DRUGBANK
    {
    	public static final String NAMESPACE = "http://www4.wiwiss.fu-berlin.de/drugbank/resource/drugbank/";
    	
	    public static final URI DRUGS = VF.createURI(NAMESPACE + "drugs");
	    public static final URI SMILES_CANONICAL = VF.createURI(NAMESPACE + "drugbank/smilesStringCanonical");
    }

    /**
     * Dublin Core vocabulary
     */
    public static class DC
    {
    	public static final String NAMESPACE = "http://purl.org/dc/elements/1.1/";
	    
	    public static final URI TITLE = VF.createURI(NAMESPACE, "title");
	    public static final URI DATE = VF.createURI(NAMESPACE, "date");
	    public static final URI CREATOR = VF.createURI(NAMESPACE, "creator");
	    public static final URI FORMAT = VF.createURI(NAMESPACE, "format");
	    public static final URI DESCRIPTION = VF.createURI(NAMESPACE, "description");
	    public static final URI LICENSE = VF.createURI(NAMESPACE, "license");
	    public static final URI HAS_VERSION = VF.createURI(NAMESPACE, "hasVersion");
    }
    
    
    /**
     * DCAT vocabulary
     */
    public static class DCAT
    {
    	public static final String NAMESPACE = "http://www.w3.org/ns/dcat#";
        
        public static final URI DATASET = VF.createURI(NAMESPACE, "Dataset");
        public static final URI DATASETDISTRIBUTION = VF.createURI(NAMESPACE, "distribution");
        public static final URI ACCESSURL = VF.createURI(NAMESPACE, "accessURL");
        public static final URI DISTRIBUTION = VF.createURI(NAMESPACE, "Distribution");
        public static final URI HAS_DISTRIBUTION = VF.createURI(NAMESPACE, "distribution");
        public static final URI DOWNLOAD = VF.createURI(NAMESPACE, "Download");
        public static final URI CATALOG = VF.createURI(NAMESPACE, "Catalog");
        public static final URI HAS_DATASET = VF.createURI(NAMESPACE, "dataset");
        public static final URI CATALOGRECORD = VF.createURI(NAMESPACE, "CatalogRecord");
        public static final URI RECORD = VF.createURI(NAMESPACE, "record");
        public static final URI KEYWORD = VF.createURI(NAMESPACE, "keyword");
        public static final URI LINKEDFROM = VF.createURI(NAMESPACE, "linkedFrom");
        public static final URI RATING = VF.createURI(NAMESPACE, "rating");
        public static final URI THEME = VF.createURI(NAMESPACE, "theme");
        public static final URI TRIPLES = VF.createURI(NAMESPACE, "triples");
        public static final URI LINKSTO = VF.createURI(NAMESPACE, "linksTo");
    }
    
    /**
     * Dublin Core terms vocabulary
     */
    public static class DCTERMS
    {
    	public static final String NAMESPACE = "http://purl.org/dc/terms/";
	    
	    public static final URI TITLE = VF.createURI(NAMESPACE, "title");
	    public static final URI CREATED = VF.createURI(NAMESPACE, "created");
	    public static final URI MODIFIED = VF.createURI(NAMESPACE, "modified");
	    public static final URI FORMAT = VF.createURI(NAMESPACE, "format");	    
	    public static final URI CONTRIBUTOR = VF.createURI(NAMESPACE, "contributor");
	    public static final URI RIGHTS = VF.createURI(NAMESPACE, "rights");
	    public static final URI CREATOR = VF.createURI(NAMESPACE, "creator");
	    public static final URI LICENSE = VF.createURI(NAMESPACE, "license");
	    public static final URI RELATION = VF.createURI(NAMESPACE, "relation");
    }
    
    /**
     * FOAF vocabulary
     */
    public static class FOAF
    {
        public static final String NAMESPACE = "http://xmlns.com/foaf/0.1/";
        
        public static final URI PERSON = VF.createURI(NAMESPACE, "Person");
        public static final URI GROUP = VF.createURI(NAMESPACE, "Group");        
        public static final URI ORGANIZATION = VF.createURI(NAMESPACE, "Organization");
        public static final URI PROJECT = VF.createURI(NAMESPACE, "Project");
        public static final URI DOCUMENT = VF.createURI(NAMESPACE, "Document");
        public static final URI IMAGE = VF.createURI(NAMESPACE, "Image");
        public static final URI THUMBNAIL = VF.createURI(NAMESPACE, "thumbnail");
        public static final URI TITLE = VF.createURI(NAMESPACE, "title");
        public static final URI LASTNAME = VF.createURI(NAMESPACE, "lastName");
        public static final URI FIRSTNAME = VF.createURI(NAMESPACE, "firstName");
        public static final URI MBOX = VF.createURI(NAMESPACE, "mbox");
        public static final URI MEMBER = VF.createURI(NAMESPACE, "member");
        public static final URI PHONE = VF.createURI(NAMESPACE, "phone");
        public static final URI LOGO = VF.createURI(NAMESPACE, "logo");
        public static final URI PAGE = VF.createURI(NAMESPACE, "page");
        public static final URI HOMEPAGE = VF.createURI(NAMESPACE, "homepage");
        public static final URI BASED_NEAR = VF.createURI(NAMESPACE, "based_near");
        public static final URI ONLINE_ACCOUNT = VF.createURI(NAMESPACE, "onlineAccount");
        public static final URI COMPANY = VF.createURI(NAMESPACE, "Company");
        public static final URI IMG = VF.createURI(NAMESPACE, "img");
        public static final URI DEPICTION = VF.createURI(NAMESPACE, "depiction");
        public static final URI NAME = VF.createURI(NAMESPACE, "name");
    }
    
    /**
     * Geo ontology
     */
    public static class GEO
    {
    	public static final String NAMESPACE = "http://www.w3.org/2003/01/geo/wgs84_pos#";
    			
        public static final URI LAT = VF.createURI(NAMESPACE, "lat");
        public static final URI LONG = VF.createURI(NAMESPACE, "long");
        public static final URI LAT_LONG = VF.createURI(NAMESPACE, "lat_long");
    }
    
    /**
     * PURL changeset ontology
     */
    public static class PURL_CHANGESET
    {
	    private static final String NAMESPACE = "http://purl.org/vocab/changeset/schema#";
	    
	    public static final URI TRIPLES_ADDED = VF.createURI(NAMESPACE + "triplesAdded");
	    public static final URI TRIPLES_REMOVED = VF.createURI(NAMESPACE + "triplesRemoved");
    }
    
    /**
     * fluidOps' Relational Schema Ontology
     */
    public static class RSO
    {
		public static final String NAMESPACE = "http://www.fluidops.com/ontologies/RelationalSchemaOntology/";
		public static final String NAMESPACE_TABLETYPE = NAMESPACE + "TableType/";
		public static final String NAMESPACE_INDEXTYPE = NAMESPACE + "IndexType/";
		public static final String NAMESPACE_CONNECTION_INFO = NAMESPACE + "ConnectionInfo/";
		public static final String NAMESPACE_DATABASE_INFO = NAMESPACE + "DatabaseInfo/";
		public static final String NAMESPACE_TEST_ENVIRONMENT = "http://www.optique-project.eu/ontologies/TestingEnvironment/";
		
		// TYPES
		public static final URI TYPE_SCHEMA = VF.createURI(NAMESPACE + "Schema");
	    public static final URI TYPE_TABLE = VF.createURI(NAMESPACE + "Table");
	    public static final URI TYPE_COLUMN = VF.createURI(NAMESPACE + "ColumnInfo");
	    public static final URI TYPE_KEY = VF.createURI(NAMESPACE + "Key");
	    public static final URI TYPE_INDEX = VF.createURI(NAMESPACE + "Index");
	    public static final URI TYPE_PRIMARY_KEY = VF.createURI(NAMESPACE + "PrimaryKey");
		public static final URI TYPE_FOREIGNKEY = VF.createURI(NAMESPACE + "ForeignKey");
		public static final URI TYPE_CONSTRAINT = VF.createURI(NAMESPACE + "Constraint");
		public static final URI TYPE_COLUMN_DATA_TYPE = VF.createURI(NAMESPACE + "ColumnDataType");

		public static final URI TYPE_COLUMN_INFO = VF.createURI(NAMESPACE + "Column");
		public static final URI TYPE_CONSTRAINT_COLUMN_INFO = VF.createURI(NAMESPACE + "ConstraintColumn");
		public static final URI TYPE_INDEX_COLUMN_INFO = VF.createURI(NAMESPACE + "IndexColumn");
		public static final URI TYPE_TABLE_COLUMN_INFO = VF.createURI(NAMESPACE + "TableColumn");
		
		
		
		// PROPERTIES
		public static final URI PROP_TABLE_NAME = VF.createURI(NAMESPACE + "tableName");
		public static final URI PROP_TABLE_FULL_NAME = VF.createURI(NAMESPACE + "tableFullName");
		public static final URI PROP_TABLE_TYPE = VF.createURI(NAMESPACE + "tableType");
	    public static final URI PROP_TABLE = VF.createURI(NAMESPACE + "schemaTable");
		public static final URI PROP_TABLE_COLUMN = VF.createURI(NAMESPACE + "tableColumn");
		public static final URI PROP_TABLE_CONSTRAINT = VF.createURI(NAMESPACE + "tableConstraint");
		
		public static final URI PROP_SCHEMA_NAME = VF.createURI(NAMESPACE + "schemaName");
		public static final URI PROP_SCHEMA_FULL_NAME = VF.createURI(NAMESPACE + "schemaFullName");
		
		public static final URI PROP_COLUMN_NAME = VF.createURI(NAMESPACE + "columnInfoName");
		public static final URI PROP_COLUMN_SHORT_NAME = VF.createURI(NAMESPACE + "columnInfoShortName");
		public static final URI PROP_COLUMN_FULL_NAME = VF.createURI(NAMESPACE + "columnInfoFullName");
		
		public static final URI PROP_CONSTRAINT_COLUMN = VF.createURI(NAMESPACE + "constraintColumn");
		public static final URI PROP_CONSTRAINT_NAME = VF.createURI(NAMESPACE + "constraintName");
		public static final URI PROP_CONSTRAINT_FULL_NAME = VF.createURI(NAMESPACE + "constraintFullName");
		public static final URI PROP_CONSTRAINT_SHORT_NAME = VF.createURI(NAMESPACE + "constraintShortName");
		
		public static final URI PROP_CONSTITUENT = VF.createURI(NAMESPACE + "columnInfo");

		public static final URI PROP_DATATYPE = VF.createURI(NAMESPACE + "columnInfoDatatype");
		public static final URI PROP_COLUMN_DATATYPE_NAME = VF.createURI(NAMESPACE + "columnDataTypeName");
		public static final URI PROP_COLUMN_DATATYPE_TYPE_NAME = VF.createURI(NAMESPACE + "columnDataTypeTypeName");
		public static final URI PROP_COLUMN_DATATYPE_FULL_NAME = VF.createURI(NAMESPACE + "columnDatatypeFullName");
		
		public static final URI PROP_TABLE_INDEX = VF.createURI(NAMESPACE + "tableIndex");
		
		public static final URI PROP_INDEX_COLUMN = VF.createURI(NAMESPACE + "indexColumn");
		public static final URI PROP_INDEX_NAME = VF.createURI(NAMESPACE + "indexName");
		public static final URI PROP_INDEX_FULL_NAME = VF.createURI(NAMESPACE + "indexFullName");
		public static final URI PROP_INDEX_SHORT_NAME = VF.createURI(NAMESPACE + "indexShortName");
		
		public static final URI PROP_POSITION = VF.createURI(NAMESPACE + "columnPosition");
		public static final URI PROP_REFERENCES_KEY = VF.createURI(NAMESPACE + "referencesKey");
		
		
		// INDIVIDUALS
		public static final URI INDIVIDUAL_TABLE_TYPE_ALIAS = VF.createURI(NAMESPACE_TABLETYPE + "ALIAS");
		public static final URI INDIVIDUAL_TABLE_TYPE_GLOBAL_TEMPORARY = VF.createURI(NAMESPACE_TABLETYPE + "GLOBAL_TEMPORARY");
		public static final URI INDIVIDUAL_TABLE_TYPE_LOCAL_TEMPORARY = VF.createURI(NAMESPACE_TABLETYPE + "LOCAL_TEMPORARY");
		public static final URI INDIVIDUAL_TABLE_TYPE_SYNONYM = VF.createURI(NAMESPACE_TABLETYPE + "SYNONYM");
		public static final URI INDIVIDUAL_TABLE_TYPE_TABLE = VF.createURI(NAMESPACE_TABLETYPE + "TABLE");
		public static final URI INDIVIDUAL_TABLE_TYPE_VIEW = VF.createURI(NAMESPACE_TABLETYPE + "VIEW");
		public static final URI INDIVIDUAL_TABLE_TYPE_UNKOWN =  VF.createURI(NAMESPACE_TABLETYPE + "UNKNOWN");
		
		public static final URI INDIVIDUAL_INDEX_TYPE_CLUSTERED = VF.createURI(NAMESPACE + "IndexType/CLUSTERED");
		public static final URI INDIVIDUAL_INDEX_TYPE_HASHED = VF.createURI(NAMESPACE + "IndexType/HASHED");
		public static final URI INDIVIDUAL_INDEX_TYPE_OTHER = VF.createURI(NAMESPACE + "IndexType/OTHER");
		public static final URI INDIVIDUAL_INDEX_TYPE_STATISTIC =  VF.createURI(NAMESPACE + "IndexType/STATISTIC");
		public static final URI INDIVIDUAL_INDEX_TYPE_UNKNOWN =  VF.createURI(NAMESPACE + "IndexType/UNKNOWN");
		
		public static final URI TYPE_RELATIONAL_DATA_ENDPOINT = VF.createURI(NAMESPACE_TEST_ENVIRONMENT + "RelationalDataEndpoint");
		public static final URI PROP_SCHEMA = VF.createURI(NAMESPACE_TEST_ENVIRONMENT + "schema");
		public static final URI TYPE_DATABASE_META_INFORMATION = VF.createURI(NAMESPACE_TEST_ENVIRONMENT + "DatabaseMetaInformation");
		public static final URI PROP_DATABASE_META_INFORMATION = VF.createURI(NAMESPACE_TEST_ENVIRONMENT + "databaseMetaInformation");
		public static final URI TYPE_DATABASE_CONNECTION_INFORMATION = VF.createURI(NAMESPACE_TEST_ENVIRONMENT + "DatabaseConnectionInformation");
		public static final URI PROP_DATABASE_CONNECTION_INFORMATION = VF.createURI(NAMESPACE_TEST_ENVIRONMENT + "databaseConnectionInformation");
		public static final URI PROP_DATABASE_INFO = VF.createURI(NAMESPACE_DATABASE_INFO + "databaseInfo");
		public static final URI PROP_PRODUCT_NAME = VF.createURI(NAMESPACE_DATABASE_INFO + "productName");
		public static final URI PROP_PRODUCT_VERSION = VF.createURI(NAMESPACE_DATABASE_INFO + "productVersion");
		public static final URI PROP_CONN_SCHEMA_NAME = VF.createURI(NAMESPACE_CONNECTION_INFO + "schemaName");
		public static final URI PROP_CONN_DRIVER_CLASS = VF.createURI(NAMESPACE_CONNECTION_INFO + "driverClass");
		public static final URI PROP_CONN_CONNECTION_STRING = VF.createURI(NAMESPACE_CONNECTION_INFO + "connectionString");
		public static final URI PROP_CONN_USER = VF.createURI(NAMESPACE_CONNECTION_INFO + "connectionUser");
		public static final URI PROP_CONN_HOST = VF.createURI(NAMESPACE_CONNECTION_INFO + "connectionHost"); // for pwd lookup
		
		
		public static final URI PROP_INDEX_TYPE = VF.createURI(NAMESPACE + "indexType");
	 }
    
    public static class VCARD
    {
    	public static final String NAMESPACE = "http://www.w3.org/2006/vcard/ns-2006.html#";
    	public static final URI ADDRESS = VF.createURI(NAMESPACE,"Address");
    	public static final URI COUNTRY_NAME = VF.createURI(NAMESPACE, "country-name");
    	public static final URI LOCALITY = VF.createURI(NAMESPACE, "locality");
    	public static final URI POSTAL_CODE= VF.createURI(NAMESPACE, "postal-code");
    	public static final URI REGION = VF.createURI(NAMESPACE, "region");
    	public static final URI STREET_ADDRESS = VF.createURI(NAMESPACE, "street-address");
    }
    
    public static class VCARD_V4
    {
    	public static final String NAMESPACE = "http://www.w3.org/2006/vcard/ns#";
    	public static final URI ADDRESS = VF.createURI(NAMESPACE,"Address");
    	public static final URI COUNTRY = VF.createURI(NAMESPACE, "country");
    	public static final URI LOCALITY = VF.createURI(NAMESPACE, "locality");
    	public static final URI POSTAL_CODE= VF.createURI(NAMESPACE, "postalCode");
    	public static final URI REGION = VF.createURI(NAMESPACE, "region");
    	public static final URI STREET_ADDRESS = VF.createURI(NAMESPACE, "streetAddress");    	
    	
    	public static final URI HAS_ADDRESS = VF.createURI(NAMESPACE, "hasAddress");
    }
    
    /**
     * VoID (See http://vocab.deri.ie/void for reference)
     */
    public static class VOID
    {
    	public static final String NAMESPACE = "http://rdfs.org/ns/void#";
	    
	    public static final URI DATASET = VF.createURI(NAMESPACE, "Dataset");
	    public static final URI CLASSPARTITION = VF.createURI(NAMESPACE, "classPartition");
	    public static final URI CLASS = VF.createURI(NAMESPACE, "class");
	    public static final URI ENTITIES = VF.createURI(NAMESPACE, "entities");
	    public static final URI PROPERTYPARTITION = VF.createURI(NAMESPACE, "propertyPartition");
	    public static final URI PROPERTY = VF.createURI(NAMESPACE, "property");
	    public static final URI CLASSES = VF.createURI(NAMESPACE, "classes");
	    public static final URI PROPERTIES = VF.createURI(NAMESPACE, "properties");
	    public static final URI TRIPLES = VF.createURI(NAMESPACE, "triples");
	    public static final URI DISTINCTSUBJECTS = VF.createURI(NAMESPACE, "distinctSubjects");
	    public static final URI DISTINCTOBJECTS = VF.createURI(NAMESPACE, "distinctObjects");
	    public static final URI URISPACE = VF.createURI(NAMESPACE, "uriSpace");
    }
}
