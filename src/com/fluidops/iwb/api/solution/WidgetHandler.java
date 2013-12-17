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

package com.fluidops.iwb.api.solution;

import static org.apache.log4j.Logger.getLogger;

import java.io.File;
import java.util.List;

import org.apache.log4j.Logger;
import org.openrdf.model.Value;

import com.fluidops.iwb.api.AbstractWidgetSelector;
import com.fluidops.iwb.api.WidgetSelector;
import com.fluidops.iwb.util.WidgetPersistence;
import com.fluidops.iwb.widget.Widget;
import com.fluidops.iwb.widget.WidgetConfig;

public class WidgetHandler extends AbstractFailureHandlingHandler
{
	
	private static final Logger logger = getLogger(SolutionService.INSTALL_LOGGER_NAME);

    static final String WIDGETS_XML_REL_PATH = "config/widgets.xml";
    private final WidgetSelector widgetSelector;

    public WidgetHandler(WidgetSelector widgetSelector)
    {
        this.widgetSelector = widgetSelector;
    }

    @Override boolean installIgnoreExceptions(File solutionDir) throws Exception
    {
        File widgetsXml = new File(solutionDir, WIDGETS_XML_REL_PATH);
        if(!widgetsXml.exists()) return false;
        WidgetPersistence persistence = new WidgetPersistence(widgetsXml.getPath());
        for (WidgetConfig widgetConfig : persistence.load())
        {
            if(widgetConfig.deleted) continue;
            checkIfContainsSimilar(widgetConfig);
            widgetSelector.addWidget(widgetConfig.widget, 
                    widgetConfig.input, 
                    widgetConfig.value, 
                    widgetConfig.applyToInstances,
                    widgetConfig.preCondition);
        }
        return true;
    }
    
    private void checkIfContainsSimilar(WidgetConfig config) throws Exception {
    	List<WidgetConfig> widgets = widgetSelector.getWidgets();
    	
    	for(WidgetConfig existingConfig : widgets) {
    		if(existingConfig.widget.equals(config.widget) && existingConfig.value.equals(config.value) && existingConfig.applyToInstances==config.applyToInstances) {
    			if(!existingConfig.input.toString().equals(config.input.toString()) || AbstractWidgetSelector.hasPreCondition(existingConfig, config.preCondition)) {
    				logger.info("Already contains a different configuration for widget class " + config.widget.toString()+" and resource "+config.value.stringValue()+".");
    				logger.info("Both configurations are applied now.");
    			}
    		}
    	}
    }
    
}
