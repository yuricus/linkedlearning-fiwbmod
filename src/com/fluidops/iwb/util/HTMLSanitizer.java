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

package com.fluidops.iwb.util;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;

/**
 * A convenience class for HTML text that provides sanitizing 
 * functionality based on OWASP
 * 
 * @author as
 *
 */
public class HTMLSanitizer {

	/**
	 * A {@link PolicyFactory} which allows HTML links. In contrast to
	 * {@link Sanitizers#LINKS} this version does not require the
	 * rel=nofollow attribute, and allows the target attribute
	 */
	private static final PolicyFactory HTML_LINKS = new HtmlPolicyBuilder()
		.allowElements("a")									            
		.allowStandardUrlProtocols() 
		.allowAttributes("href", "target").onElements("a").toFactory();

	
	private static final PolicyFactory defaultHtmlSanitizer = Sanitizers.FORMATTING
			.and(Sanitizers.BLOCKS)
			.and(Sanitizers.STYLES)
			.and(Sanitizers.IMAGES)
			.and(HTML_LINKS);
	
	private static final PolicyFactory linkHtmlSanitizer = Sanitizers.FORMATTING
			.and(HTML_LINKS);
	
	/**
	 * Returns an HTML sanitizer that allows certain elements,
	 * however, cleans up unsafe attributes. In addition to basic
	 * formatting elements, this sanitizer allows blocks, style attributes,
	 * images as well as links.
	 * 
	 * @return
	 */
	public static PolicyFactory defaultSanitizer() {
		return defaultHtmlSanitizer;
	}

	/**
	 * Returns an HTML sanitizer that sanitizes links. Note: 
	 * this sanitizer allows basic formatting elements.
	 * 
	 * @return
	 */
	public static PolicyFactory linkSanitizer() {
		return linkHtmlSanitizer;
	}
	
	/**
	 * Sanitizes the given String using {@link #defaultSanitizer()}.
	 * 
	 * @param text
	 * @return
	 */
	public static String sanitize(String text) {
		return defaultSanitizer().sanitize(text);
	}

	/**
	 * Sanitizes the given String using {@link #linkSanitizer()}.
	 * Note: this sanitizer allows basic formatting elements.
	 * 
	 * @param text
	 * @return
	 */
	public static String sanitizeLinks(String text) {
		return linkSanitizer().sanitize(text);
	}
}
