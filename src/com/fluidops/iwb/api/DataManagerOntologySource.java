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

import info.aduna.iteration.CloseableIteration;

import java.io.InputStream;
import java.io.Reader;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.openrdf.OpenRDFException;
import org.openrdf.model.Namespace;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.semanticweb.owlapi.formats.OWLOntologyFormatFactory;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLRuntimeException;


public class DataManagerOntologySource implements OWLOntologyDocumentSource
{
    
    private final Map<String, String> namespaces = new LinkedHashMap<String, String>();
    private final Iterator<Statement> statementIterator;
    private IRI documentIRI;
    
    public DataManagerOntologySource(final Iterator<Statement> statements, URI uri)
    {
        this.documentIRI = IRI.create(uri.toString());
        this.statementIterator = statements;
        
    }
    
    /**
     * Creates an OWLOntologyDocumentSource using a closeable iteration. 
     */
    public DataManagerOntologySource(final CloseableIteration<Statement, ? extends OpenRDFException> statements, URI uri)
    {
        this.documentIRI = IRI.create(uri.toString());
        this.statementIterator = new Iterator<Statement>()
            {
                
                @Override
                public void remove()
                {
                    throw new UnsupportedOperationException("Cannot remove statements using this iterator");
                }
                
                @Override
                public Statement next()
                {
                    Statement nextStatement = null;
                    try
                    {
                        nextStatement = statements.next();
                        
                        if(nextStatement != null)
                        {
                            return nextStatement;
                        }
                        else
                        {
                            throw new NoSuchElementException("No more statements in this iterator");
                        }
                    }
                    catch(OpenRDFException e)
                    {
                        throw new OWLRuntimeException("Found exception while iterating", e);
                    }
                    finally
                    {
                        if(nextStatement == null)
                        {
                            try
                            {
                                statements.close();
                            }
                            catch(OpenRDFException e)
                            {
                                e.printStackTrace();
                            }
                        }
                    }
                }
                
                @Override
                public boolean hasNext()
                {
                    boolean result = false;
                    
                    try
                    {
                        result = statements.hasNext();
                        
                        return result;
                    }
                    catch(OpenRDFException e)
                    {
                        throw new OWLRuntimeException("Found exception while iterating", e);
                    }
                    finally
                    {
                        if(!result)
                        {
                            try
                            {
                                statements.close();
                            }
                            catch(OpenRDFException e)
                            {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            };
    }
    
    /**
     * Creates an OWLOntologyDocumentSource using a closeable iteration. 
     */
    public DataManagerOntologySource(final CloseableIteration<Statement, ? extends OpenRDFException> statements,
            final Map<String, String> namespaces, URI uri)
    {
        this(statements, uri);
        
        this.namespaces.putAll(namespaces);
        
    }
    
    @Override
    public boolean isReaderAvailable()
    {
        return false;
    }
    
    @Override
    public Reader getReader()
    {
        return null;
    }
    
    @Override
    public boolean isInputStreamAvailable()
    {
        return false;
    }
    
    @Override
    public InputStream getInputStream()
    {
        return null;
    }
    
    @Override
    public IRI getDocumentIRI()
    {
        return this.documentIRI;
    }
    
    @Override
    public boolean isFormatKnown()
    {
        return false;
    }
    
    @Override
    public OWLOntologyFormatFactory getFormatFactory()
    {
        return null;
    }
    
    public Map<String, String> getNamespaces()
    {
        return this.namespaces;
    }
    
    public Iterator<Statement> getStatementIterator()
    {
        return this.statementIterator;
    }
    
    public void setNamespaces(Map<String, String> nextNamespaces)
    {
        this.namespaces.clear();
        this.namespaces.putAll(nextNamespaces);
    }
    
    public void setNamespaces(RepositoryResult<Namespace> namespaces) throws RepositoryException
    {
        this.namespaces.clear();
        try
        {
            while(namespaces.hasNext())
            {
                Namespace nextNamespace = namespaces.next();
                this.namespaces.put(nextNamespace.getPrefix(), nextNamespace.getName());
            }
        }
        finally
        {
            namespaces.close();
        }
    }
}