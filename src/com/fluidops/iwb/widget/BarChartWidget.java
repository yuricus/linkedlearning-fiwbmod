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

import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import org.openrdf.model.Value;

import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FJSChart;
import com.fluidops.ajax.models.ChartDataModel;
import com.fluidops.ajax.models.ChartDataModel.FChartType;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.ReadDataManager;
import com.fluidops.iwb.api.RequestMapper;
import com.fluidops.iwb.model.ParameterConfigDoc;
import com.fluidops.iwb.model.ParameterConfigDoc.Type;
import com.fluidops.iwb.model.TypeConfigDoc;
import com.fluidops.iwb.widget.ChartWidgetUtil.MultiDimChartDataSeries;
import com.fluidops.iwb.widget.WidgetEmbeddingError.ErrorType;
import com.fluidops.iwb.widget.config.WidgetChartConfig;
import com.fluidops.iwb.widget.config.WidgetSerialChartConfig;
import com.fluidops.util.StringUtil;

/**
 * Show the results of a query in form of a bar chart. 
 * The chart is rendered using amchart-JavaScript unless the browser doesn't support SVG. 
 * In this case an flash-based amchart-chart is rendered.
 * 
 * "Simple configuration example: <br />" +
 *               "{{ #widget: BarChart <br />" +
 *               "| query = <br />'" +
 *               "SELECT DISTINCT ?country ?population <br />" +
 *               "WHERE {?country a dbpedia:country . <br />" +
 *               "?country :hasPopulation ?population}'<br />" +
 *               "| type = 'BAR_VERTICAL'<br />" +
 *               "| input = 'country'<br />" +
 *               "| output = {{'population'}}<br />" +
 *       "}}"
 * 
 * @author ango
 */
@TypeConfigDoc("The bar chart widget displays the result of a query in a bar chart with either vertical, horizontal or clustered bars")
public class BarChartWidget extends AbstractChartWidget<BarChartWidget.Config>
{
    /**
     * default height (in px) for resulting chart
     */
	protected static final String CHART_HEIGHT_DEFAULT = "500";

    public static class Config extends WidgetSerialChartConfig
    {
    	
        //Legacy chart types: {"BAR_HORIZONTAL","BAR_VERTICAL","BAR_CLUSTERED"} 
    	//for backwards compatibility. 
    	//They will be mapped to amcharts conform types : BAR and COLUMN.
    	//The number of the output variables (datasets) defines if the chart is clustered or not
    	@Deprecated
        public LegacyType type; 
        
        @ParameterConfigDoc(
                desc = "The chart type to display the results: bar, column", 
                defaultValue="VERTICAL", 
                type=Type.DROPDOWN,
                selectValues={"bar","column"}) 
        public FChartType barType = FChartType.COLUMN; 

        @ParameterConfigDoc(
                desc = "The colors of the bars, if the bar color is not set. " +
                        "If there are more bar graphs than colors in this array, the chart picks random color. " +
                        "E.g. '#FF0F00'",
                        type=Type.LIST)
        public List<String> colors;

        @ParameterConfigDoc(
                desc = "Balloon text appearing when hovering over a bar. The following tags can be used: [[value]], [[title]], [[percents]], [[description]].",
                defaultValue = "[[description]]: [[value]]")
        public String balloonText = "[[description]]: [[value]]";
    }


	// Legacy Chart Types. Should be mapped to the amcharts conform chart types: column and bar
	public enum LegacyType { BAR_HORIZONTAL, BAR_VERTICAL, BAR_CLUSTERED};

    @Override
    public Class<Config> getConfigClass()
    {
        return Config.class;
    }

    @Override
    public String getTitle()
    {
        Config c = get();
        return c.title == null? "Chart" : c.title;
    }

	/**
	 * @see com.fluidops.iwb.widget.AbstractChartWidget#getChart(String, WidgetChartConfig, ReadDataManager, ReadDataManager, Vector, Vector)
	 */
	@Override
	protected FComponent getChart(String id, BarChartWidget.Config config,
			ReadDataManager globalDm, ReadDataManager queryDM,
			Vector<Vector<Number>> values, Vector<Value> labels) {
        FComponent widgetEmbeddingError = config.checkObligatoryFields(id, config, pc.value, CHART_HEIGHT_DEFAULT);
	      if(widgetEmbeddingError!=null)
	            return widgetEmbeddingError;
	      
	      	
	      	// legacy handling
	        if(config.type != null && config.barType == FChartType.COLUMN)
	        {
	        	switch(config.type)
	        	{
		        	case BAR_HORIZONTAL: 	        		
		        		config.barType = FChartType.BAR; break;
		        	case BAR_VERTICAL:
		        	case BAR_CLUSTERED:
		        		 config.barType = FChartType.COLUMN;
	        	}
	        }
	                
	        ChartDataModel pm = ChartWidgetUtil.createChartModel(config.barType, config.height,
	                config.width, config.title);

	        List<String> outputs = config.output;

	        try
	        {
	        	// now we have the query result available in the values datastructure, together
	        	// with the associated labels; in the next step, we build the chart data model
	        	MultiDimChartDataSeries chartDataSeries = 
	        			ChartWidgetUtil.generateMultiDimChartDataSeries(
	        					globalDm, values, labels, outputs);

	        	RequestMapper rm = EndpointImpl.api().getRequestMapper();
	        	for (int i = 0; i < chartDataSeries.labelsArray.length; i++)
	        	{
	        		pm.addDataRow(chartDataSeries.labelsArray[i],
	        				rm.getRequestStringFromValue(labels.elementAt(i)),
	        				chartDataSeries.valuesArray[i]);
	        	}
	        	
	            widgetEmbeddingError = config.setOutputLabels(id, outputs, pm);
	    	      if(widgetEmbeddingError!=null)
	  	            return widgetEmbeddingError;
	        } 
	        catch (Exception e)
	        {
	        	logger.error(e.getMessage());
	        	return ChartWidgetUtil.chartExceptionToFComponent(e,id,config.query);
	        }
	        ///////////////////////////////////////////////////// CREATE CHART
	        // create basic chart...
	        FJSChart chart = new FJSChart(id, pm);     

	        if(config.settings != null)
	        {
	            String settingsTemplate = ChartWidgetUtil.getSettingsTemplate(config.settings);
	            
	            if (settingsTemplate == null)
	                return WidgetEmbeddingError.getErrorLabel(id,
	                        ErrorType.SETTINGS_FILE_NOT_FOUND, config.settings);
	                
	            chart.setSettingsTemplate(settingsTemplate);
	        }


	        // construct map of parameters to be replaced in the settings file template;
	        // this option currently is supported only by the amChart engine, in other
	        // cases we have to pass null to avoid an exception
	        
	        // override param map
	        chart.setParams("", config.title, config.output, config.customMappings, 
	                config.balloonText, config.width, config.height,
	        		EndpointImpl.api().getRequestMapper().getContextPath());  
	        
			//set line specific parameters
			if(config.colors!=null)
				chart.setParam("colors", StringUtil.listToString(config.colors, ","));
			else
			{
				pm.setColors(Arrays.asList(ChartWidgetUtil.fillDefaultColors(config.output.size())));
			}

	        return chart;
    }

}

