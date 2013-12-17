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

package com.fluidops.iwb.wiki.parserfunction;

import info.bliki.wiki.filter.MagicWord;
import info.bliki.wiki.model.IWikiModel;

import java.util.Set;

import org.openrdf.model.URI;
import org.openrdf.model.Value;

import com.fluidops.base.Version;
import com.fluidops.config.Config;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.wiki.FluidWikiModel;
import com.google.common.collect.Sets;

/**
 * This class defines a set of special {@link MagicWord}s interpreted 
 * by the wiki engine. The magic words defined in this class have
 * precedence over build-in words.
 * 
 * These magic words are evaluated in 
 * {@link FluidWikiModel#getRawWikiContent(String, String, java.util.Map)}
 * 
 * The special magic word {{this}} (case insensitive) is an alias
 * for FULLPAGENAME and returns the URI to be used within queries.
 * 
 * All magic words that return a {@link Value} are rendered using 
 * {@link ParserFunctionUtil#valueToString(Value)}, i.e. URIs 
 * can directly be used within queries.
 * 
 * @author as
 * @see MagicWord
 */
public class IWBMagicWords {
	
	
	public static final String MAGIC_USERNAME = "USERNAME";
	
	public static final String MAGIC_USERURI = "USERURI";
	
	public static final String MAGIC_FULL_PAGE_NAME = "FULLPAGENAME";
	
	public static final String MAGIC_PRODUCT = "PRODUCT";
	
	public static final String MAGIC_STARTPAGE = "STARTPAGE";
	
	
	private static final Set<String> MAGICWORDS = Sets.newHashSet(MAGIC_USERNAME, MAGIC_USERURI, MAGIC_FULL_PAGE_NAME, 
													MAGIC_PRODUCT, MAGIC_STARTPAGE);
	
	public static boolean isMagicWord(String name) {
		if (name.equalsIgnoreCase("this"))
			return true;
		return MAGICWORDS.contains(name);
	}
	
	
	public static String processMagicWord(String name, String parameter, IWikiModel model, URI page) {
		
		if (name.equalsIgnoreCase("this"))
			return "<" + page.stringValue() + ">";
		
		if ( name.equals(MAGIC_USERNAME))
			return getUserName();
		
		if ( name.equals(MAGIC_USERURI))
			return ParserFunctionUtil.uriToString(getUserURI());
		
		if ( name.equals(MAGIC_FULL_PAGE_NAME))
			return ParserFunctionUtil.uriToString(page);
		
		if ( name.equals(MAGIC_PRODUCT))
			return Version.getVersionInfo().getProductName();
		
		if ( name.equals(MAGIC_STARTPAGE))
			return Config.getConfig().startPage();
		
		throw new UnsupportedOperationException("Magic word not yet implemented: " + name);
	}
	
	private static String getUserName() {
		try {
			return EndpointImpl.api().getUserName();
		} catch (Exception e) {
			throw new IllegalStateException("Could not fetch user name: " + e.getMessage());
		}
	}
	
	private static URI getUserURI() {
		try {
			return EndpointImpl.api().getUserURI();
		} catch (Exception e) {
			throw new IllegalStateException("Could not fetch user URI: " + e.getMessage());
		}
	}
	
	
}
