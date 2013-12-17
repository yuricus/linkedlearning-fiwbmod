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

package com.fluidops.iwb.widget;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;

import com.fluidops.ajax.XMLBuilder.Attribute;
import com.fluidops.ajax.components.FAsynchContainer;
import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FForm.Validation;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.operator.Operator;
import com.fluidops.iwb.api.operator.OperatorException;
import com.fluidops.iwb.model.ParameterConfigDoc;
import com.fluidops.iwb.page.PageContext;
import com.fluidops.iwb.user.UserManager;
import com.fluidops.iwb.widget.WidgetEmbeddingError.ErrorType;
import com.fluidops.iwb.widget.WidgetEmbeddingError.NotificationType;
import com.fluidops.iwb.widget.config.WidgetVoidConfig;
import com.fluidops.iwb.widget.config.WidgetBaseConfig;
import com.fluidops.security.acl.ACL;
import com.fluidops.security.acl.ACLPermission;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;

/**
 * Base implementation class of IWB widgets.
 * 
 * Has default implementations for various housekeeping and ACL permission handling.
 * 
 * The generic parameter indicates the class of the configuration. It is recommended
 * to use a configuration class inheriting from {@link WidgetBaseConfig} in order
 * to automatically inhibit existing functionality (e.g. asynch loading). In case
 * no configuration is required, it is recommended to use {@link WidgetVoidConfig}.
 * Configuration parameters in the configuration class must be annotated with
 * {@link ParameterConfigDoc} in order to be accessible from the configuration forms.
 * It should be absolutely avoided to use any primitive class (such as String)
 * for this configuration.
 * 
 * @author uli
 *
 * @param <T> the generic configuration. Recommendation: extend {@link WidgetBaseConfig}. 
 * 				See class notes for further details.
 */
public abstract class AbstractWidget<T> implements Widget<T>
{
	private static final Logger logger = Logger.getLogger(AbstractWidget.class.getName());

	private static UserManager userManager = EndpointImpl.api().getUserManager();
	
    /**
     * field required
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    public @interface Required{}

    /**
     * validator for the field
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    public @interface TextInputValidation{
        public Validation getValidation();
    }

    /**
     * text area for input required
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    public @interface TextAreaReq{}
    
    /**
     * dropdown list with valid values
     */    
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    public @interface SelectBoxReq
    {
        public String values();
    }
    /**
     * page context
     */
    protected PageContext pc;
    Operator mapping;
    T value;
    boolean wasEvaluated;
    
    public Operator getMapping() {
    	return mapping;
    }

    @Override
    public void setMapping(Operator mapping)
    {
        wasEvaluated = false;
        this.mapping = mapping;
    }

    @Override
    public void setPageContext(PageContext pc)
    {
        this.pc = pc;
    }

    /**
     * Retrieve the evaluated {@link #mapping} using the 
     * {@link Operator} framework. The evaluated result
     * is cached and maintained.
     * 
     * Note: this method returns null for primitive
     * {@link #getConfigClass()} (e.g. String) as well as for 
     * {@link Void}.
     * 
     * If no mapping is provided a {@link Operator#createNoop()}
     * mapping is assumed.
     * 
     * In case of any error this method propagates the error
     * as runtime exceptions.
     */
    @SuppressWarnings("unchecked")
	public T get() 
    {
        try
        {
            if ( ! wasEvaluated )
            {
            	if (mapping==null)
            		mapping = Operator.createNoop();
            	
            	if (mapping.isNoop()) {
            		value = (T)Operator.newInstance(getConfigClass());
            	} else
                	value = (T)mapping.evaluate(getConfigClass(), pc.value);
                wasEvaluated = true;
            }
            return value;
        } catch (OperatorException e) {
        	throw new RuntimeException(e);
        } catch (Exception e) {
        	// unexpected error
        	throw Throwables.propagate(e);
        }
    }

    public boolean isListType()
    {
        return false;
    }

    /**
     * Returns the additional Java Scripts required by this widget. Actual
     * widget implementations may override this methods to provide their
     * required scripts.
     *  
     * Example (taken from charts):
     * 
     * <source>
     	String cp = EndpointImpl.api().getRequestMapper().getContextPath();
        return Lists.newArrayList(cp + "/amchart/js/amcharts.js", cp + "/amchart/js/amfallback.js");
     * </source>
     * 
     * Important: must not return null
     * 
     * @return
     */
    public List<String> jsURLs() {
        return Collections.emptyList();
    }
    
    @Override
    public FComponent getComponentUAE(final String id)
    {
        // in case the user has no access rights to the widget, forbid access
    	// TODO: Having no access is actually not an error, it is intended behaviour to not show the widget
    	// In fact, it is rather irritating for a user to see an error message
        if (!userManager.hasWidgetAccess(this.getClass(),null))
            return WidgetEmbeddingError.getNotificationLabel(id,NotificationType.ACCESS_FORBIDDEN);
        
        // Check if the widget is requesting extended access control permissions
        if ( this instanceof WidgetAccessControl )
        {
        	ACL additionalACL = ((WidgetAccessControl)this).getAdditionalACL();
        	if ( additionalACL!=null )
        	{
        		boolean permit = true;
        		try
        		{
        			for ( ACLPermission aclPerm : additionalACL.getPermissions() )
            		{
            			String eType = aclPerm.entityType();
            			String eId = aclPerm.entityId();
            			
        				// "And" in the permission flag
        				permit &= userManager.hasComponentAccess( eType, eId, null);
            		}
        			// No permission
        			if ( !permit )
        				return WidgetEmbeddingError.getNotificationLabel(id,NotificationType.ACCESS_FORBIDDEN);
        		}
        		catch (Exception e)
        		{
        			logger.warn("Error while constructing component widget", e);
        			// ignore: widget is invalid anyway
        		}
        	}
        }
        
        try
        {
        	// allow for asynchronous loading if specified
        	// in the widget configuration
        	FComponent component;
        	if (isAsynchLoad()) {
    			component = new FAsynchContainer(id, "<div class=\"statusLoading\" />") {
    				@Override
    				public FComponent getComponentAsynch() {
    					FComponent res = AbstractWidget.this.getComponent(id+"_a");
    					/*
    					 * Make sure that the widget attribute is set on the
    					 * div that is filled out with asynch loading.
    					 */
    					res.addAttribute(new Attribute(Widget.WIDGET_ATTRIBUTE, AbstractWidget.this.getClass().getName()));
    					return res;
    				}        		
    			};
    		} else {
    			component = getComponent(id);
    			component.addAttribute(new Attribute(Widget.WIDGET_ATTRIBUTE, getClass().getName()));
    		}   
        	
			return component;
        }
        catch (Exception e)
        {
        	// catch exceptions which are not yet caught
        	logger.warn("Widget construction failed on page " + pc.value + ":" +  e.getMessage());
        	logger.debug("Widget construction exception", e);
            return WidgetEmbeddingError.getErrorLabel(id,ErrorType.INVALID_WIDGET_CONFIGURATION,e.getMessage());
        }
    }
    
    /**
     * Determine if the widget is loaded asynchronously from the widget's configuration
     * (if configuration is a WidgetBaseConfig). Defaults to false.
     * 
     * Certain widgets that cannot deal with asynchronous handling (e.g. due to java
     * scripts) can override this method and return false, see e.g. TimelineWidget.
     * 
     * @return
     */
    protected boolean isAsynchLoad() {
    	T config = get();
    	boolean asynch=false;
    	if (config instanceof WidgetBaseConfig) {
    		WidgetBaseConfig _c = (WidgetBaseConfig)config;
    		asynch = _c.asynch==null ? false : _c.asynch;
    	}
    	return asynch;
    }
    
    protected abstract FComponent getComponent(String id);
    
    /**
     * Set the widget config explicitly
     * 
     * @param value
     */
    public void setConfig(T value)
    {
    	this.value=value;
    	wasEvaluated=true;
    }
}
