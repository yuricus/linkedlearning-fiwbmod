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

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.openrdf.model.URI;
import org.openrdf.model.Value;

import com.fluidops.ajax.FClientUpdate;
import com.fluidops.ajax.components.FButton;
import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FContainer;
import com.fluidops.ajax.components.FImageButton;
import com.fluidops.ajax.components.FLabel;
import com.fluidops.ajax.components.FLinkButton;
import com.fluidops.ajax.components.FPopupWindow;
import com.fluidops.iwb.annotation.CallableFromWidget;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.model.TypeConfigDoc;
import com.fluidops.iwb.service.CodeExecution;
import com.fluidops.iwb.service.CodeExecution.CodeExecutionContext;
import com.fluidops.iwb.service.CodeExecution.Config;
import com.fluidops.iwb.service.CodeExecution.UserInputConfig;
import com.fluidops.iwb.service.CodeExecution.WidgetCodeConfig;
import com.fluidops.iwb.ui.configuration.CodeExecutionConfigurationForm;
import com.fluidops.iwb.ui.configuration.ConfigurationFormBase;
import com.fluidops.iwb.ui.configuration.ConfigurationFormUtil;
import com.fluidops.iwb.widget.WidgetEmbeddingError.ErrorType;
import com.fluidops.util.Rand;
import com.fluidops.util.StringUtil;
import com.fluidops.util.concurrent.TaskExecutor;

/**
 * Code execution widget for java and groovy code.
 * 
 * 1) leveraged nested parameter parsing
 * 2) supports the new groovy classloading
 * 3) uses regular java / groovy reflection (i.e. any method can be called)
 * 4) parameterize method calls with user input
 * 
 * For a specification and complete usage scenario (e.g. how types can be used),
 * see {@link CodeExecutionWidgetTest}. Additional examples can be found in
 * solutions/testBootstrap => test:CodeExecutionWidget
 * 
 * Component can be styled by using CSS class "CodeExecution".
 * 
 * Examples:
 * 
 * 1. Executing a Java method with signature
 * 
 * @CallableFromWidget
 * public static String testMe(String param, Value a, List<Value> b, List<Value> x)
    
 * <code>
 *	{{#widget: CodeWidget
 *  | label = 'Test Various'
 *  | clazz = 'com.fluidops.iwb.widget.CodeWidget'
 *  | method = 'testMe'
 *  | args = {{ 'Constant' | $this.a$ | $this.b$ | $select ?x where { ?? ?p ?x }$ }}
 *  | confirm = 'Do you really want to execute testMe()'
 *  | onFinish = 'reload'
 *  }}
 * </code> 
 * 
 * <ul>
 * 	<li>Rendered as button with label 'Test Various'. To render as image use type='img:/path/to/img'</li>
 *  <li>Confirmation method is optional</li>
 *  <li>onFinish is optional, in this example reloads page. Alternatives: onFinish=$this.a$, 
 *       onFinish='none', onFinish='http://myuri.com'</li>
 * </ul>
 * 
 * 
 * 2. Executing a groovy script 
 * 
 * <code>
 * {{#widget: CodeExecution 
 *  | label = 'Test Groovy'
 *  | clazz = 'dyn.GroovyTest' 
 *  | method = 'hello' 
 *  | args = {{ 'abc' }}
 *  | passContext = true
 * }}
 * </code>
 * 
 * File: scripts/dyn/GroovyTest.groovy
 * 
 * => an example can be found in solutions/testBootstrap* 
 * 
 * 
 * 3. Executing a method with CodeExecutionContext as first argument
 * 
 * When setting passContext to true, the {@link CodeExecutionContext} is
 * transmitted as the first argument, without having to explicitly add
 * it to the list of arguments. The corresponding signature to the method
 * below looks like
 * 
 * @CallableFromWidget
 * public static void testMe2(CodeExecutionContext ceCtx, Value value)
 * 
 * <code>
 * {{#widget: com.fluidops.iwb.widget.CodeExecutionWidget
 *  | label = 'Test 7'
 *  | clazz = 'com.fluidops.iwb.widget.CodeExecutionWidget'
 *  | method = 'testMe2'
 *  | args = {{ $this.a$ }}
 *  | passContext = true
 *  }}
 * </code>
 * 
 * 4. Letting the code execution appear as a usual link, with a code execution
 *    script that opens a given URL in a new TAB
 *    
 * see also {@link CodeExecution#linkTo(CodeExecutionContext, String, Boolean)}
 * 
 * <code>
	{{#widget: com.fluidops.iwb.widget.CodeExecutionWidget
	| label = 'Click Me'
	| render = 'link'
	| clazz = 'com.fluidops.iwb.service.CodeExecution'
	| method = 'linkTo'
	| args = {{ 'http://www.google.de' | true }}
	| passContext = true
	| onFinish = none
	}}
 * </code>
 * 
 * 5. Parameterize method calls with user input
 * 
 * <code>
	{{#widget: CodeExecution
	 | label = 'Test User Input'
	 | clazz = 'com.fluidops.iwb.widget.CodeExecutionWidget'
	 | method = 'testMe'
	 | args = {{ '%name' | '<http://www.fluidops.com/Constant>' | '%type' }}
	 | userInput = {{ 
	      {{ name = 'name'
	       | componentType = 'SIMPLE'
	       | presetValue = 'Hello, widget'
	      }} | 
	      {{ name = 'type'
	       | componentType = 'DROPDOWN'
	       | selectValues = $select distinct ?type where { ?x rdf:type ?type }$
	      }} }}
	}}
 * </code>
 * 
 * @author (msc), (aeb), as, christian.huetter
 * @see CodeExecutionWidgetTest
 */
@TypeConfigDoc("Widget to invoke pre-defined Java and Groovy methods with paramaters specified in wiki notation or provided by the user.")
public class CodeExecutionWidget extends AbstractWidget<CodeExecution.WidgetCodeConfig>
{
	
	protected static final Logger logger = Logger.getLogger(CodeExecutionWidget.class.getName());
	

	@Override
	protected FComponent getComponent(String id)
	{
		final WidgetCodeConfig config = get();
		
		final FContainer cont = new FContainer(id);
		cont.appendClazz("CodeExecution");
		
		final CodeExecution.Config codeConfig;
		try {	
			CodeExecutionContext ceCtx = new CodeExecutionContext(this.pc, cont);
			codeConfig = CodeExecution.widgetConfigToCodeConfig(config, ceCtx);
		} catch (Exception e) {
			logger.debug("Error while converting widget configuration: " + e.getMessage(), e);
			return WidgetEmbeddingError.getErrorLabel(id, ErrorType.INVALID_WIDGET_CONFIGURATION, e.getMessage());
		}
		
		validate(config);
		
		final FLabel msg = new FLabel("codelbl");
		msg.setHidden(true);
		
        FComponent comp;
        if (config.render.equals("btn"))
        {
        	comp = new FButton( id, config.label )
    		{
    			@Override
    			public void onClick()
    			{
    				handleOnClick(config, codeConfig, msg, this);
    			}
    		};
        } 
        else if (config.render.startsWith("img"))
        {
        	if (!config.render.contains(":")) 
    			throw new IllegalArgumentException("Invalid specification of image location: " + config.render);
    		
    		String imgUrl = config.render.substring(config.render.indexOf(":")+1);    	    		
        	String imgTooltip = config.label;
        	
        	comp = new FImageButton("ce"+Rand.getIncrementalFluidUUID(), imgUrl, imgTooltip) {
        		@Override
				public void onClick() {
    				handleOnClick(config, codeConfig, msg, this);
				}
        	};  
        }
        else if (config.render.equals("link")) 
        {
        	comp = new FLinkButton( id, config.label )
    		{
    			@Override
    			public void onClick()
    			{
    				handleOnClick(config, codeConfig, msg, this);
    			}
    		};
        }
        else if (config.render.equals("inline")) 
        {
    		if (config.userInput != null && !config.userInput.isEmpty())
    		{
    			comp = constructConfigurationForm(config, codeConfig, msg, null);
    		}
    		else
    			return WidgetEmbeddingError.getErrorLabel(id,
    					ErrorType.INVALID_WIDGET_CONFIGURATION, "Inline render mode requires user input to be configured.");
        }
        else
			return WidgetEmbeddingError.getErrorLabel(id,
					ErrorType.INVALID_WIDGET_CONFIGURATION, "Config.render "
							+ config.render + " not supported");

		// set confirmation question
        // for user input, the configuration form will show the confirmation question
		if (!StringUtil.isNullOrEmpty(config.confirm)
				&& (config.userInput == null || config.userInput.isEmpty())
				&& comp instanceof FButton)
		{
			((FButton) comp).setConfirmationQuestion(config.confirm);
		}
		
        cont.add(comp);
        cont.add(msg);
        
        return cont;
	}

	/**
	 *  Validate user input configuration
	 */
	private void validate(final WidgetCodeConfig config) throws IllegalArgumentException
	{
		if (config.userInput != null)
		{
			for (UserInputConfig userInput : config.userInput)
			{
				if (StringUtil.isNullOrEmpty(userInput.name))
					throw new IllegalArgumentException("Name of userInput must be neither null nor empty.");
				
				if (userInput.componentType == null)
					throw new IllegalArgumentException("componentType of userInput '" + userInput.name + "' must not be null.");
				
				// check if there is at least one argument placeholder for the current userInput
				boolean found = false;
				for (Object arg : config.args)
				{
					String argName = extractPlaceholderName(arg);
					if (argName != null && argName.equalsIgnoreCase(userInput.name))
					{
						found = true;
						break;
					}
				}
				
				if (!found)
					throw new IllegalArgumentException("No argument placeholder defined for userInput '" + userInput.name + "'.");
			}
		}
	}

	/**
	 * Extract the name of the given argument placeholder.
	 * @return placeholder name or null if argument is not a placeholder
	 */
	public static String extractPlaceholderName(Object arg)
	{
		if (arg == null)
			return null;
		
		// is the argument a placeholder?
		String argString = arg.toString();
		if (argString != null && argString.startsWith("%"))
		{
			return argString.substring(1);
		}
		
		return null;
	}

	/**
	 * Decide whether to construct a configuration form or to execute the method directly.
	 */
	private final void handleOnClick(WidgetCodeConfig widgetConfig, Config codeConfig,
			FLabel msg, FButton fButton)
	{
		if (widgetConfig.userInput != null && !widgetConfig.userInput.isEmpty())
		{
			// create configuration form
			ConfigurationFormBase configForm = constructConfigurationForm(widgetConfig, codeConfig, msg, fButton);
			
			// show configuration form in a popup
			FPopupWindow popup = fButton.getPage().getPopupWindowInstance();
			ConfigurationFormUtil.showConfigurationFormInPopup(popup, widgetConfig.label, configForm);
		}
		else
		{
			execute(widgetConfig, codeConfig, msg, fButton);
		}
	}

	/**
	 * Create a configuration form to input data from the user.
	 */
	protected ConfigurationFormBase constructConfigurationForm(WidgetCodeConfig widgetConfig,
			Config codeConfig, FLabel msg, FButton fButton)
	{
		return new CodeExecutionConfigurationForm("cf"+Rand.getIncrementalFluidUUID(),
				widgetConfig, codeConfig, msg, fButton, this);
	}

	@Override
	public Class<?> getConfigClass()
	{
		return WidgetCodeConfig.class;
	}

	@Override
	public String getTitle()
	{
		return "Call Java or Groovy";
	}
	
	/**
	 * script execution happens here
	 * @param codeConfig
	 * @return
	 * @throws Exception
	 */
	protected Object executeScript(CodeExecution.Config codeConfig) throws Exception
	{
		return new CodeExecution().run(codeConfig);
	}
	
	
	/**
	 * Execute the {@link CodeExecution.Config} using the {@link CodeExecution} service. Handles
	 * config.onFinish option as documented (e.g. page reload etc)
	 * 
	 * @param config
	 * @param codeConfig
	 * @param msg
	 */
	public void execute(final WidgetCodeConfig config, final CodeExecution.Config codeConfig, final FLabel msg, final FComponent comp) {
		try {

			msg.setClazz("statusLoading");
			msg.setText("");
			msg.hide(false);
			msg.populateView();
			comp.setEnabled(false);
			
			final Future<Object> f = TaskExecutor.instance().submit(new Callable<Object>() {
				@Override
				public Object call() throws Exception {
					return executeScript(codeConfig);
				}
			});		
			
			Object res = null;
			try {
				res = f.get(500, TimeUnit.MILLISECONDS);
			} catch (TimeoutException e) {
				// enable polling
				msg.startPolling(2000L, null, new Runnable() {					
					@Override
					public void run() {						
						if (f.isDone()) {
							msg.stopPolling();
							try {
								executionDone(config, msg, f.get(), comp);
							} catch (ExecutionException e) {
								executionError(codeConfig, msg, e.getCause(), comp);  
							} catch (Exception e) {
								executionError(codeConfig, msg, e, comp);								
							}
						}
					}
				});
			}			
			
			if (f.isDone()) {
				executionDone(config, msg, res, comp);
			} else {
				logger.debug("Continuing code execution of " +codeConfig.method + " asynchronously");
			}
			
		} 
		catch (ExecutionException e) {
			executionError(codeConfig, msg, e.getCause(), comp);  
		} catch (Exception e) {
			executionError(codeConfig, msg, e, comp);								
		}
	}

	
	void executionDone(WidgetCodeConfig config, FLabel msg, Object res, FComponent comp) {
		msg.removeClazz("statusLoading");
		comp.setEnabled(true);
		
		String msgTxt = (res == null) ? "Finished" : StringEscapeUtils.escapeHtml(res.toString());
		if (StringUtil.isNullOrEmpty(config.onFinish))
		{
            msg.setText(msgTxt);
            msg.hide(false);
            msg.populateView();
		}
		else if (config.onFinish.equals("popup")) // show result in popup
        {
			final FPopupWindow p = msg.getPage().getPopupWindowInstance(msgTxt);
			p.setTitle("Code Execution");
			p.addCloseButton("Close");
			p.populateAndShow();
			msg.hide(false);
        }
		else if (config.onFinish.equals("reload")) // reload page
        {
        	// TODO: reload swallows all errors which is not nice
            msg.addClientUpdate(FClientUpdate.reload());
        }
		else if (config.onFinish.equals("none"))
		{
			msg.hide(false);
		}
        else                    	 
        {
        	// forward to URI
            URI u = EndpointImpl.api().getNamespaceService().guessURI(config.onFinish);
            String redirect = EndpointImpl.api().getRequestMapper().getRequestStringFromValue(u);
            msg.addClientUpdate(new FClientUpdate("document.location='" + redirect + "';"));
        }
	}

	void executionError(CodeExecution.Config codeConfig, FLabel msg, Throwable e, FComponent comp) {
		logger.warn("Error executing method " + codeConfig.method, e);
		msg.removeClazz("statusLoading");
		comp.setEnabled(true);
		msg.setText("Error: " + (e.getMessage()!=null ? StringEscapeUtils.escapeHtml(e.getMessage()) : "unknown") );
        msg.hide(false);
        msg.populateView(); 
	}
	
	@CallableFromWidget
	public Object test( Object o )
	{
		System.out.println( o );
		return o;
	}
	
    /**
     * This is an example static method that could be called,
     * the widget configuration for calling this method is as follows.
     * 
     * <code>
	 *	{{#widget: CodeWidget
	 *  | label = 'Test Various'
	 *  | clazz = 'com.fluidops.iwb.widget.CodeExecutionWidget'
	 *  | method = 'testMe'
	 *  | args = {{ 'Constant' | $this.a$ | $select ?x where { ?? ?p ?x }$ }}
	 *  | confirm = 'Do you really want to execute testMe()'
	 *  | onFinish = 'reload'
	 *  }}
	 * </code> 
     */
    @CallableFromWidget
    public static String testMe(String param, Value a, Value x)
    {
        System.out.println("Param: " + param);
        System.out.println("a: " + a);
        System.out.println("x: " + x);
        
        return "Executed method successfully!";
    }
    
    /**
     * This is an example static method that could be called,
     * the widget configuration for calling this method is as follows.
     * 
     * <code>
	 *	{{#widget: CodeExecution
	 * | label = 'Test 7'
	 * | clazz = 'com.fluidops.iwb.widget.CodeExecutionWidget'
	 * | method = 'testMe2'
	 * | args = {{ $this.a$ }}
	 * | passContext = true
	 * }}
	 * </code> 
     */
    @CallableFromWidget
    public static void testMe2(CodeExecutionContext ceCtx, String value)
    {
		ceCtx.parentComponent.doCallback("alert('Clicked on "
				+ StringEscapeUtils.escapeHtml(value == null ? "(undefined)" : value.toString()
						) + "');");
	}
    
    @CallableFromWidget
    public static void testMe3(String value)
    {
    	System.out.println("Clicked on " + StringEscapeUtils.escapeHtml(value) );
    }
}
