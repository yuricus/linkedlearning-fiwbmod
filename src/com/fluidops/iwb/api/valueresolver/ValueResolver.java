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

package com.fluidops.iwb.api.valueresolver;

import static com.fluidops.iwb.api.valueresolver.ValueResolver.CurrencyUSDValueResolver.resolveNumber2Places;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.ServiceLoader;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.openrdf.model.Value;
import org.openrdf.model.impl.ValueFactoryImpl;

import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.ImageResolver;
import com.fluidops.iwb.util.Config;
import com.fluidops.iwb.util.DateTimeUtil;
import com.fluidops.iwb.util.HTMLSanitizer;
import com.fluidops.iwb.util.IWBFileUtil;
import com.fluidops.security.XssSafeHttpRequest;
import com.fluidops.util.ExtensibleEnumValue;
import com.fluidops.util.StringUtil;


/**
 * <p>The base class for all {@link ValueResolver}s. A {@link ValueResolver} is
 * a light-weight module which converts a {@link Value} into some
 * String representation.</p>
 * 
 * <p>{@link ValueResolver}s are registered to the {@link ValueResolverRegistry}.
 * There exist various built-in system value resolvers as defined in this
 * class which are automatically initialized. Despite that it is possible
 * to provide custom value resolvers as extensions using the Java SPI
 * mechanisms.</p>
 * 
 * <p>{@link ValueResolver}s must have a system-wide unique name used for
 * identification in the {@link ValueResolverRegistry}. Note that the 
 * name must adhere to the naming rules of Java fields.</p>
 * 
 * <p><b>Adding custom value resolvers</b></p>
 * 
 * <p>The value resolver framework is designed with extensibility in mind,
 * i.e., it is easily possible to register custom value resolvers either
 * by code or using the Java ServiceLoader mechanism. As already mentioned,
 * all value resolvers are registered to the singleton {@link ValueResolverRegistry}
 * instance. This instance is initialized on startup with the built-in 
 * value resolvers as well as with extensions using the {@link ServiceLoader}
 * mechanism.</p>
 * 
 * <p>A custom value resolver can be created as follows:</p>
 * 
 * <ol>
 *   <li>Create a new value resolver by extending the abstract {@link ValueResolver}
 *       class. It is recommended to also override the {@link #description()} method.</li>
 *   <li>Implement a {@link ValueResolverFactory} which creates the {@link ValueResolver}
 *       instance.</li>
 *   <li>Add the entry for the {@link ValueResolverFactory} to a the classpath
 *    in META-INF such that it can be picked up by the {@link ServiceLoader}.
 *    In practice this means that you create (or extend) the file
 *    "META-INF/services/com.fluidops.iwb.api.valueresolver.ValueResolverFactory" 
 *    in which the fully qualified classnames of the {@link ValueResolverFactory}
 *    are specified, one entry per line, e.g.
 *    com.fluidops.iwb.api.valueresolver.MyValueResolverFactory</li>
 * </ol>
 * 
 * @author as
 * @see ValueResolverRegistry
 * @see ValueResolverFactory
 * @see ValueResolverUtil
 *
 */
public abstract class ValueResolver extends ExtensibleEnumValue {

	private static Logger logger = Logger.getLogger(ValueResolver.class);
	
	/**
	 * The representation of an undefined value, used e.g., for {@link #DEFAULT}
	 */
	public static final String UNDEFINED = "<i>(undefined)</i>";
	
	/**
	 * Instantiate the {@link ValueResolver}. The given name must adhere
	 * to the naming rules of Java fields, otherwise an {@link IllegalArgumentException}
	 * is thrown.
	 * 
	 * @param name
	 */
	public ValueResolver(String name) {
		super(name);
	}

	/**
	 * This method determines how the non-null {@link Value} is
	 * converted to a String representation. This method must not
	 * return <null>
	 * 
	 * @param value a non-null {@link Value} to convert
	 * @return a non-null String representation
	 */
	public abstract String handle(Value value);

	/**
	 * This method determines how a List of value is
	 * converted to a string representation. Actual
	 * value resolvers may override the default functionality,
	 * which takes the first element of the list. If the
	 * list is empty of null, the {@link #handle(Value)} is
	 * invoked for an empty string.
	 * 
	 * @param values
	 * @return
	 */
	public String handle(List<Value> values) {
		if (values == null || values.isEmpty())
			return handle(ValueFactoryImpl.getInstance().createLiteral(""));
		return handle(values.get(0));
	}
	
	/**
	 * Returns a textual description of the respective {@link ValueResolver}.
	 * By default this method returns its name.
	 * 
	 * @return
	 */
	public String description() {
		return name();
	}

	/**
	 * Resolve the given {@link Value} with the specified valueResolver.
	 * The valueResolver is looked up in the {@link ValueResolverRegistry}.
	 * For robustness this method resolves the value with the {@link #DEFAULT}
	 * one if 
	 * 
	 * a) the provided valueResolver is <null>
	 * b) the provided valueResolver is not known to the registry (warning
	 *    is printed to the log)
	 * c) an error occurred in the requested {@link ValueResolver} (e.g. a 
	 *    conversion error). Information is printed to debug log.
	 * 
	 * If a <null> value is provided, the empty string is assumed as literal.
	 * The default resolver then show {@link DefaultValueResolver#UNDEFINED}.
	 * 
	 * @param valueResolver a valueResolver name known to the {@link ValueResolverRegistry}
	 * @param value a value (may be null>
	 * @return
	 */
	public static String resolveValue(String valueResolver, Value value) {
		if (StringUtil.isNullOrEmpty(valueResolver))
			valueResolver = DEFAULT;
		if (value==null)
			value=ValueFactoryImpl.getInstance().createLiteral("");
		try {
			return getValueResolverWithDefault(valueResolver).handle(value);
		} catch (Exception e) {
			logger.debug("Error using value resolver " + valueResolver + ". Fallback to DEFAULT. Details: " + e.getMessage());
			return getValueResolver(DEFAULT).handle(value);
		}
	}

	/**
	 * Resolve the given list of {@link Value}s with the specified valueResolver.
	 * The valueResolver is looked up in the {@link ValueResolverRegistry}.
	 * For robustness this method resolves the values with the {@link #DEFAULT}
	 * one if 
	 * 
	 * a) the provided valueResolver is <null>
	 * b) the provided valueResolver is not known to the registry (warning
	 *    is printed to the log)
	 * c) an error occurred in the requested {@link ValueResolver} (e.g. a 
	 *    conversion error). Information is printed to debug log.
	 *    
	 * Note that only some value resolvers actually override the default
	 * functionality of {@link ValueResolver#handle(List)} in order deal
	 * with multiple values.
	 * 
	 * @param valueResolver a valueResolver name known to the {@link ValueResolverRegistry}
	 * @param value a list of values
	 * @return
	 */
	public static String resolveValues(String valueResolver, List<Value> values) {
		if (StringUtil.isNullOrEmpty(valueResolver))
			valueResolver = DEFAULT;
		try {
			return getValueResolverWithDefault(valueResolver).handle(values);
		} catch (Exception e) {
			logger.debug("Error using value resolver " + valueResolver + ". Fallback to DEFAULT. Details: " + e.getMessage());
			return getValueResolver(DEFAULT).handle(values);
		}
	}

	/**
	 * Looks up the given valueResolver in the {@link ValueResolverRegistry}
	 * and returns the found result. If the valueResolver is not known to
	 * the system, this method returns <null>
	 * 
	 * @param valueResolver a valueResolver name known to the {@link ValueResolverRegistry}
	 * @return a {@link ValueResolver} or <null>
	 */
	public static ValueResolver getValueResolver(String valueResolver) {
		ValueResolverRegistry reg = ValueResolverRegistry.getInstance();
		if (reg.hasValueResolver(valueResolver))
			return reg.valueOf(valueResolver);
		return null;
	}
	
	
	/**
	 * Return the {@link ValueResolver} for the given name. If there is no such,
	 * this method returns the {@link #DEFAULT} value resolver and prints a
	 * warning to the log
	 * 
	 * @param valueResolver
	 * @return
	 */
	private static ValueResolver getValueResolverWithDefault(String valueResolver) {
		ValueResolver res = getValueResolver(valueResolver);
		if (res!=null) {			
			return res;
		}
		logger.warn("Requested value resolver not found: " + valueResolver + ". Fallback to DEFAULT");
		return ValueResolverRegistry.getInstance().valueOf(DEFAULT);
	}

	/* default implementations */

	public static final String DEFAULT = "DEFAULT";
	public static final String DEFAULT_NOERROR = "DEFAULT_NOERROR";
	
	/**
	 * converts a system date like '2011-03-31T19:54:33' to user-readable date
	 */
	public static final String SYSDATE = "SYSDATE";
	
	/**
	 * converts a timestamp from milliseconds (long) like '1370876136065' (e.g. unix timestamp) to user-readable date 'Mon, 10 Jun 2013 16:55:36'
	 */
	public static final String MS_TIMESTAMP2DATE = "MS_TIMESTAMP2DATE";
	
	/**
	 * TODO make SDC solution resolver
	 * converts a timestamp from milliseconds (long) like '1370876136065' (e.g. unix timestamp) to user-readable date including the timezone from the sever 'Mon, 10 Jun 2013 16:55:36 (CEST)'
	 */
	public static final String MS_TIMESTAMP2DATETZ = "MS_TIMESTAMP2DATETZ";
	
	/**
	 * converts a timestamp from seconds like '1370876136' (e.g. unix timestamp) to user-readable date including the timezone from the sever 'Mon, 10 Jun 2013 16:55:36 (CEST)'
	 */
	public static final String S_TIMESTAMP2DATE = "S_TIMESTAMP2DATE";
	
	/**
	 * converts xsd:date to a human-readable format (MMM dd, yyyy)
	 */
	public static final String DATE = "DATE";
	
	/**
	 * converts xsd:time to a human-readable format (HH:mm:ss)
	 */
	public static final String TIME = "TIME";
	
	/**
	 * Converts xsd:dateTime to a human-readable format. The pattern is
	 * retrieved from the config.prop setting {@link Config#getDefaultDatePattern()},
	 * default: MMM dd, yyyy HH:mm:ss
	 */
    public static final String DATETIME = "DATETIME";
    
    /**
     * Resolve the given value to an image
     */
    public static final String IMAGE = "IMAGE";

    /**
     * thumbnail, height=20px
     */
    public static final String THUMBNAIL = "THUMBNAIL";
    
    /**
     * big thumbnail, height=50px
     */
    public static final String BIGTHUMBNAIL = "BIGTHUMBNAIL";
    
    /**
     * decimal value from double
     */
    public static final String DOUBLE2INT = "DOUBLE2INT";
    
    /**
	 * Converts a Value into a nice name which can be displayed on the UI. The
	 * ValueResolver uses the rdfs:label or the localname for URIs and the label
	 * for Literals.
	 */
    public static final String LABEL = "LABEL";
    
    /**
     * Converts a string URL to a HTML link with the attribute target='_blank'
     */
    public static final String URL = "URL";

    /**
     * Render as sanitized HTML using {@link HTMLSanitizer}
     */
    public static final String HTML = "HTML";
    
    /**
     * Link for URL, labeled "Login"
     */
    public static final String LOGINLINK = "LOGINLINK";

    /**
     * Byte to KB
     */
    public static final String BYTE2KB = "BYTE2KB";
    
    /**
     * Byte to MB
     */
    public static final String BYTE2MB = "BYTE2MB";
    
    /**
     * Byte to GB
     */
    public static final String BYTE2GB = "BYTE2GB";
    
    /**
     * Byte to TB
     */
    public static final String BYTE2TB = "BYTE2TB";
    
    /**
     * KByte to MB
     */
    public static final String KBYTE2MB = "KBYTE2MB";
    
    /**
     * KByte to GB
     */
    public static final String KBYTE2GB = "KBYTE2GB";
    
    /**
     * KByte to TB
     */
    public static final String KBYTE2TB = "KBYTE2TB";
    
    /**
     * Converts a decimal number like '0.71356' to a percent value and appends a percent sign '71.36%'.
     * This is useful as decimal numbers indicating, for example, the load factor of machine are often counterintuitive to the end-user.
     */
    public static final String PERCENT = "PERCENT";

    /**
     * Rounds a number to two decimal places after comma and appends a percent sign. For example, it converts '71.356' to '71.36%'.
     * This is useful if someone wants to display percent values labeled with the percent sign, for instance, in a QueryResultTable or BarChart.
     */
    public static final String PERCENT_NOCONVERT = "PERCENT_NOCONVERT";
    
    /**
     * Round a number to two decimal places after comma
     */
    public static final String ROUND_DOUBLE = "ROUND_DOUBLE";
    
    /**
     * Double to currency USD
     */
    public static final String CURRENCY_USD = "CURRENCY_USD";
    
    /**
     * Double to currency EUR
     */
    public static final String CURRENCY_EUR = "CURRENCY_EUR";

    /**
     * Double to currency CNY
     */
    public static final String CURRENCY_CNY = "CURRENCY_CNY";

    /**
     * comma separated representation (uses DEFAULT for each value)
     */
    public static final String COMMA_SEPARATED = "COMMA_SEPARATED";

            
    /**
     * {@link ValueResolver#DEFAULT}
     */
	static class DefaultValueResolver extends ValueResolver {


		public DefaultValueResolver() {
			super(DEFAULT);
		}

		@Override
		public String handle(Value value) {
			return ValueResolverUtil.resolveDefault(value, UNDEFINED);
		}

		
	}
	
	/**
     * {@link ValueResolver#DEFAULT_NOERROR}
     */
	static class DefaultNoErrorValueResolver extends ValueResolver {

		public DefaultNoErrorValueResolver() {
			super(DEFAULT_NOERROR);
		}

		@Override
		public String handle(Value value) {
			return ValueResolverUtil.resolveDefault(value, "");
		}
	}

	/**
     * {@link ValueResolver#SYSDATE}
     */
	static class SysdateValueResolver extends ValueResolver {
		
		public SysdateValueResolver() {
			super(SYSDATE);
		}

		@Override
		public String handle(Value value) {
			return ValueResolverUtil.resolveSystemDate(value.stringValue());
		}
	}
	
	
	/**
     * {@link ValueResolver#MS_TIMESTAMP2DATE}
     */
	static class MS_Timestamp2DateValueResolver extends ValueResolver {

		public MS_Timestamp2DateValueResolver() {
			super(MS_TIMESTAMP2DATE);
		}

		@Override
		public String handle(Value value) {
			Long t = Long.valueOf(value.stringValue());
	        GregorianCalendar c = new GregorianCalendar();
	        c.setTimeInMillis(t);
	            
	        DateFormat df = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss");
	        return df.format(c.getTime());
		}
	}
	
	
	/**
     * {@link ValueResolver#S_TIMESTAMP2DATE}
     */
	static class S_Timestamp2DateValueResolver extends ValueResolver {

		public S_Timestamp2DateValueResolver() {
			super(S_TIMESTAMP2DATE);
		}

		@Override
		public String handle(Value value) {
			Long t = Long.valueOf(value.stringValue())*1000;
	        GregorianCalendar c = new GregorianCalendar();
	        c.setTimeInMillis(t);
	            
	        DateFormat df = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss");
	        return df.format(c.getTime());
		}
	}
	
	
	/**
     * {@link ValueResolver#MS_TIMESTAMP2DATETZ}
     * TODO move to SDC solution
     */
	static class MS_Timestamp2DateTZValueResolver extends ValueResolver {

		public MS_Timestamp2DateTZValueResolver() {
			super(MS_TIMESTAMP2DATETZ);
		}

		@Override
		public String handle(Value value) {
			Long t = Long.valueOf(value.stringValue());
	        GregorianCalendar c = new GregorianCalendar();
	        c.setTimeInMillis(t);
	            
	        DateFormat df = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss (z)");
	        return df.format(c.getTime());
		}
	}
	
		
	/**
     * {@link ValueResolver#DATE}
     */
	static class DateValueResolver extends ValueResolver {

		public DateValueResolver() {
			super(DATE);
		}
		
		@Override
		public String handle(Value value) {
			Calendar calendar = DatatypeConverter.parseDate(value.stringValue());
			return DateTimeUtil.getDate(calendar, "MMM dd, yyyy");
		}
	}
	
	
	/**
     * {@link ValueResolver#TIME}
     */
	static class TimeValueResolver extends ValueResolver {

		public TimeValueResolver() {
			super(TIME);
		}
		
		@Override
		public String handle(Value value) {
			Calendar calendar = DatatypeConverter.parseTime(value.stringValue());
			return DateTimeUtil.getDate(calendar, "HH:mm:ss");
		}
	}
	
	
	/**
     * {@link ValueResolver#DATETIME}
     */
	static class DatetimeValueResolver extends ValueResolver {

		public DatetimeValueResolver() {
			super(DATETIME);
		}
		
		@Override
		public String handle(Value value) {
			Calendar calendar = DatatypeConverter.parseDateTime(value.stringValue());
			return DateTimeUtil.getDate(calendar);
		}
	}
	
	
	/**
     * {@link ValueResolver#IMAGE}
     */
	static class ImageValueResolver extends ValueResolver {

		public ImageValueResolver() {
			super(IMAGE);
		}
		
		@Override
		public String handle(Value value) {
			String link = getDefaultImageResolver().resolveImage(value);
	    	return ImageResolver.imageString(link);
		}
	}
	
	/**
     * {@link ValueResolver#THUMBNAIL}
     */
	static class ThumbnailValueResolver extends ValueResolver {

		public ThumbnailValueResolver() {
			super(THUMBNAIL);
		}
		
		@Override
		public String handle(Value value) {
			return resolveThumbnail(value,"20px");
		}
		
		/**
	     * Converts a string to a a thumbnail link, if it represents an image
	     * (which is determined by file ending).
	     * 
	     * @param The image link as a String
	     * @return Returns the correct image link
	     */
	    private static String resolveThumbnail(Value value, String height) {
	    	String link = getDefaultImageResolver().resolveImage(value);
	        return ImageResolver.thumbnailString(link,height, "");
	    }
	}
	
	/**
     * {@link ValueResolver#BIGTHUMBNAIL}
     */
	static class BigThumbnailValueResolver extends ValueResolver {

		public BigThumbnailValueResolver() {
			super(BIGTHUMBNAIL);
		}
		
		@Override
		public String handle(Value value) {
			return ThumbnailValueResolver.resolveThumbnail(value, "50px");
		}
	}
	
	/**
     * {@link ValueResolver#DOUBLE2INT}
     */
	static class Double2IntValueResolver extends ValueResolver {

		public Double2IntValueResolver() {
			super(DOUBLE2INT);
		}
		
		@Override
		public String handle(Value value) {
			Double d = Double.valueOf(value.stringValue());
			DecimalFormat numberFormatter = new DecimalFormat("0");
	        String out = numberFormatter.format(d);	        
	        return out;
		}
	}
	
	/**
     * {@link ValueResolver#LABEL}
     */
	static class LabelValueResolver extends ValueResolver {

		public LabelValueResolver() {
			super(LABEL);
		}
		
		@Override
		public String handle(Value value) {
			return EndpointImpl.api().getDataManager().getLabelHTMLEncoded(value);
		}
	}
	
	/**
     * {@link ValueResolver#URL}
     */
	static class URLValueResolver extends ValueResolver {

		public URLValueResolver() {
			super(URL);
		}
		
		@Override
		public String handle(Value value) {
			String valueStr = value.stringValue();
			
	        valueStr = XssSafeHttpRequest.cleanXSS(valueStr);
	        String linkName = StringEscapeUtils.escapeHtml(valueStr);
			return "<a href='" + valueStr + "' target='_blank'>" + linkName + "</a>";
		}
	}
	
	/**
     * {@link ValueResolver#HTML}
     */
	static class HTMLValueResolver extends ValueResolver {

		public HTMLValueResolver() {
			super(HTML);
		}
		
		@Override
		public String handle(Value value) {
			return HTMLSanitizer.sanitize(value.stringValue());
		}
	}
	
	/**
     * {@link ValueResolver#LOGINLINK}
     */
	static class LoginLinkValueResolver extends ValueResolver {

		public LoginLinkValueResolver() {
			super(LOGINLINK);
		}
		
		@Override
		public String handle(Value value) {
			String valueStr = value.stringValue();
			
	        valueStr = XssSafeHttpRequest.cleanXSS(valueStr);
	        String linkName = "Login";
			return "<a href='" + valueStr + "' target='_blank'>" + linkName + "</a>";
		}
	}
	
	/**
	 * Base class for {@link Byte2KBValueResolver}, {@link Byte2MBValueResolver},
	 * {@link Byte2GBValueResolver} and {@link Byte2TBValueResolver}.
	 *
	 */
	abstract static class AbstractByteValueResolver extends ValueResolver {

		private final String unit;
		public AbstractByteValueResolver(String name, String unit) {
			super(name);
			this.unit = unit;
		}
		
		@Override
		public String handle(Value value) {
			return resolveByte2X(value.stringValue(), unit);
		}
		
		/**
	     * A String is converted into a long and than calculated to the set unit (KB, MB, GB, TB).
	     * 
	     * @param value The String to convert to long
	     * @param unit The Unit in which the value is converted
	     * @return Returns String representation of the changed long value with unit
	     */
	    private String resolveByte2X(String value, String unit) {
	        Double byteSize = Double.valueOf(value);
	        DecimalFormat df = new DecimalFormat("0.00");

	        if (unit.equals("KB"))
	            return String.valueOf(df.format(byteSize / 1024.0)) + " " + unit;
	        else if (unit.equals("MB"))
	            return String.valueOf(df.format(byteSize / 1048576.0)) + " " + unit;
	        else if (unit.equals("GB"))
	            return String.valueOf(df.format(byteSize / 1073741824.0)) + " " + unit;
	        else if (unit.equals("TB"))
	            return String.valueOf(df.format((byteSize / 1073741824.0) / 1024.0)) + " " + unit;
	        else
	            return "<em>" + StringEscapeUtils.escapeHtml(value)
	                    + " could not be converted to "
	                    + StringEscapeUtils.escapeHtml(unit) + "!</em>";
	     }
	}
	
	/**
     * {@link ValueResolver#BYTE2KB}
     */
	static class Byte2KBValueResolver extends AbstractByteValueResolver {

		public Byte2KBValueResolver() {
			super(BYTE2KB, "KB");
		}
	}
	
	/**
     * {@link ValueResolver#BYTE2MB}
     */
	static class Byte2MBValueResolver extends AbstractByteValueResolver {

		public Byte2MBValueResolver() {
			super(BYTE2MB, "MB");
		}
	}
	
	/**
     * {@link ValueResolver#BYTE2GB}
     */
	static class Byte2GBValueResolver extends AbstractByteValueResolver {

		public Byte2GBValueResolver() {
			super(BYTE2GB, "GB");
		}
	}
	
	/**
     * {@link ValueResolver#BYTE2TB}
     */
	static class Byte2TBValueResolver extends AbstractByteValueResolver {

		public Byte2TBValueResolver() {
			super(BYTE2TB, "TB");
		}
	}
	
	/**
	 * Base class for {@link KByte2MBValueResolver}, {@link KByte2GBValueResolver}
	 * and {@link KByte2TBValueResolver}
	 */
	abstract static class AbstractKByteValueResolver extends ValueResolver {

		private final String unit;
		public AbstractKByteValueResolver(String name, String unit) {
			super(name);
			this.unit = unit;
		}
		
		@Override
		public String handle(Value value) {
			return resolveKByte2X(value.stringValue(), unit);
		}
		
		/**
	     * A String is converted into a long and than calculated to the set unit (MB, GB, TB).
	     * 
	     * @param value The String to convert to long
	     * @param unit The Unit in which the value is converted
	     * @return Returns String representation of the changed long value with unit
	     */
	    private static String resolveKByte2X(String value, String unit)
	    {
	        Double kByteSize = Double.valueOf(value);
	        DecimalFormat df = new DecimalFormat("0.00");

	        if (unit.equals("MB"))
	            return String.valueOf(df.format(kByteSize / 1024.0)) + " " + unit;
	        else if (unit.equals("GB"))
	            return String.valueOf(df.format(kByteSize / 1048576.0)) + " " + unit;
	        else if (unit.equals("TB"))
	            return String.valueOf(df.format(kByteSize / 1073741824.0)) + " " + unit;
	        else
	            return "<em>" + StringEscapeUtils.escapeHtml(value)
	                    + " could not converted to "
	                    + StringEscapeUtils.escapeHtml(unit) + "!</em>";
	     }
	}
	
	/**
     * {@link ValueResolver#KBYTE2MB}
     */
	static class KByte2MBValueResolver extends AbstractKByteValueResolver {

		public KByte2MBValueResolver() {
			super(KBYTE2MB, "MB");
		}
	}
	
	/**
     * {@link ValueResolver#KBYTE2GB}
     */
	static class KByte2GBValueResolver extends AbstractKByteValueResolver {

		public KByte2GBValueResolver() {
			super(KBYTE2GB, "GB");
		}
	}
	
	/**
     * {@link ValueResolver#KBYTE2TB}
     */
	static class KByte2TBValueResolver extends AbstractKByteValueResolver {

		public KByte2TBValueResolver() {
			super(KBYTE2TB, "TB");
		}
	}
	
	/**
     * {@link ValueResolver#PERCENT}
     */
	static class PercentValueResolver extends ValueResolver {

		public PercentValueResolver() {
			super(PERCENT);
		}
		
		@Override
		public String handle(Value value) {
			Double d = Double.valueOf(value.stringValue()); // throws exception if conversion fails
	        double dPerc = d*100;
	        
	        DecimalFormat df = new DecimalFormat("0.00");
	        return String.valueOf(df.format(dPerc)) + "%";
		}
	}
	
	/**
     * {@link ValueResolver#PERCENT_NOCONVERT}
     */
	static class PercentNoConvertValueResolver extends ValueResolver {

		public PercentNoConvertValueResolver() {
			super(PERCENT_NOCONVERT);
		}
		
		@Override
		public String handle(Value value) {
			Double d = Double.valueOf(value.stringValue()); // throws exception if conversion fails
	        
	        DecimalFormat df = new DecimalFormat("0.00");
	        return String.valueOf(df.format(d)) + "%";
		}
	}
	
	/**
     * {@link ValueResolver#ROUND_DOUBLE}
     */
	static class RoundDoubleValueResolver extends ValueResolver {

		public RoundDoubleValueResolver() {
			super(ROUND_DOUBLE);
		}
		
		@Override
		public String handle(Value value) {
			Double number = Double.valueOf(value.stringValue());
	        DecimalFormat df = new DecimalFormat("0.00");
	        return df.format(number);
		}
	}
	
	
	/**
     * {@link ValueResolver#CURRENCY_USD}
     */
	static class CurrencyUSDValueResolver extends ValueResolver {

		public CurrencyUSDValueResolver() {
			super(CURRENCY_USD);
		}
		
		@Override
		public String handle(Value value) {
			return "&#36;" + resolveNumber2Places(value.stringValue());
		}
		
		/**
	     * A number is converted to only have 2 places after the comma.
	     * 
	     * @param value The number to convert to
	     * @return Returns the number with only 2 places after the comma as String
	     */
	    static String resolveNumber2Places(String value)
	    {
	        Double number = Double.valueOf(value);
	        DecimalFormat df = new DecimalFormat("0.00");
	        return df.format(number);
	    }
	}
	
	/**
     * {@link ValueResolver#CURRENCY_EUR}
     */
	static class CurrencyEURValueResolver extends ValueResolver {

		public CurrencyEURValueResolver() {
			super(CURRENCY_EUR);
		}
		
		@Override
		public String handle(Value value) {
			return resolveNumber2Places(value.stringValue()) + "&#8364;";
		}
	}
	
	/**
     * {@link ValueResolver#CURRENCY_CNY}
     */
	static class CurrencyCNYValueResolver extends ValueResolver {

		public CurrencyCNYValueResolver() {
			super(CURRENCY_CNY);
		}
		
		@Override
		public String handle(Value value) {
			return "&yen;" + resolveNumber2Places(value.stringValue());
		}
	}
	
	
	/**
     * {@link ValueResolver#COMMA_SEPARATED}
     */
	static class CommaSeparatedValueResolver extends ValueResolver {

		public CommaSeparatedValueResolver() {
			super(COMMA_SEPARATED);
		}
		
		@Override
		public String handle(Value value) {
			return "&yen;" + resolveNumber2Places(value.stringValue());
		}

		@Override
		public String handle(List<Value> values) {
			if (values.isEmpty())
				return "";
			StringBuilder sb = new StringBuilder();
	    	for (Value v : values) {
	    		sb.append(ValueResolverUtil.resolveDefault(v, UNDEFINED)).append(", "); 
	    	}
	    	return sb.substring(0, sb.length()-2 );		// remove the last comma and space
		}		
	}
	
	/**
     * Get default image resolver.
     * 
	 * @return image resolver
	 */
	private static ImageResolver getDefaultImageResolver() {
		return new ImageResolver(IWBFileUtil.getFileInConfigFolder("images.prop").getPath(), false);
	}
}