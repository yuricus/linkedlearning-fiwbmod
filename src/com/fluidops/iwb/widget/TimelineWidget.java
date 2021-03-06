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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResult;

import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FTimeline;
import com.fluidops.ajax.components.FTimeline.Event;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.ReadDataManager;
import com.fluidops.iwb.api.ReadDataManagerImpl;
import com.fluidops.iwb.model.ParameterConfigDoc;
import com.fluidops.iwb.model.ParameterConfigDoc.Type;
import com.fluidops.iwb.model.TypeConfigDoc;
import com.fluidops.iwb.widget.WidgetEmbeddingError.ErrorType;
import com.fluidops.iwb.widget.WidgetEmbeddingError.NotificationType;
import com.fluidops.iwb.widget.config.WidgetQueryConfig;
import com.fluidops.util.StringUtil;
import com.google.common.collect.Lists;
/**
 * timeline widget (using simile-timeline widget from http://www.simile-widgets.org)
 * shows the events (results of the given query) on a timeline, given temporal information of each event.
 * One can adjust the time intervals of the timeline by defining the 'interval' parameter.
 * (the possible intervals are MILLISECOND, SECOND, MINUTE, HOUR, DAY, WEEK, MONTH, YEAR, DECADE, CENTURY, MILLENNIUM)
 * The obligatory input parameters are 'query', 'start', 'end', 'label'. 'start' and 'end' should contain temporal data of the event to display.
 * It is also possible to give some description to be displayed in the info window by defining 'desc' as a column query result,
 * possible further leading links in the column 'link' will be set behind the label, if column 'image' is defined, it will be displayed in the info window.
 * 
 *  An example of the configuration:
 *  
 *  {{#widget:Timeline
 * |query ='SELECT   ?time ?label ?image ?event
 * WHERE { ?event dbpedia:time ?time .
 *              ?event dbpedia:thumbnail ?image .
 * 	            ??  dbpedia:event ?event .
 *              ?event rdfs:label  ?label .}'
 * | start='time'
 * | end='time'	
 * | desc='label'
 * | label='label'
 * | link='event'
 * | image='image'
 * | interval='month'
 }}
 * @author ango
 */

@TypeConfigDoc("Shows a timeline using the Simile Widgets framework")
public class TimelineWidget extends AbstractWidget<TimelineWidget.Config>
{
	private static final Logger logger = Logger.getLogger(TimelineWidget.class.getName());
	
    public static class Config extends WidgetQueryConfig {
        
    	@ParameterConfigDoc(
    			desc = "The interval unit of the timeline",
    			defaultValue = "DAY",
    			type = Type.DROPDOWN)
        public TimeInterval interval = TimeInterval.DAY;

    	@ParameterConfigDoc(desc = "The variable to display")
        public String desc;
        
    	@ParameterConfigDoc(
    			desc = "The variable containing the start date",
    			required = true)
        public String start;
        
    	@ParameterConfigDoc(
    			desc = "The variable containing the end date",
    			required = true)
        public String end;
        
    	@ParameterConfigDoc(
    			desc = "The variable containing the label",
    			required = true)
        public String label;
        
    	@ParameterConfigDoc(desc = "Link to an entity") 
        public String link;  
        
        @ParameterConfigDoc( desc = "The variable containing the image to be displayed in the info window" ) 
        public String image;  
        
    	@ParameterConfigDoc(desc = "The timezone", defaultValue="0")
        public String timezone = "0";
        
    	@ParameterConfigDoc(desc = "Per-column width in pixels")
        public String colwidth;
    }
    
    public static enum TimeInterval{
    	
    	MILLISECOND,
    	SECOND,
    	MINUTE,
    	HOUR,
    	DAY,
    	WEEK,
		MONTH,
		YEAR,
		DECADE,
		CENTURY,
		MILLENIUM,
		EPOCH,
		ERA
		
    }
     
    /**
     * Supported date formats
     */
    private static DateFormat[] formatters = new DateFormat[] 
    {
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"),
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ"),
        new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z"),
        new SimpleDateFormat("yyyy-MM-dd"),
        new SimpleDateFormat("yyyy")
    };


    @Override
    public FComponent getComponent(final String id)
    {
    	
    	final Config c =get();
    	
    	if(c.query==null||c.start==null||c.end==null||c.label==null)
    		return WidgetEmbeddingError.getErrorLabel(id, ErrorType.MISSING_INPUT_VARIABLE);
    	
        FTimeline timeline;
        List<Event> events = new ArrayList<Event>();
        
        ReadDataManager dm = null;
        
        try {
           
        	dm = ReadDataManagerImpl.getDataManager(pc.repository);
        	
            TupleQueryResult res;
            
            res = dm.sparqlSelect(c.query, true, pc.value, c.infer);

            if (!res.hasNext()) {
            	/*
            	 * Empty set of results, show noDataMessage instead
            	 */
            	return WidgetEmbeddingError.getNotificationLabel(id, NotificationType.NO_DATA, c.noDataMessage);
            }

            // Gather TimelineEvents
            timeline = new FTimeline(id);
            while (res.hasNext())
            {
            	
                BindingSet bindingSet = res.next();

                Date start = parseDate(bindingSet.getValue(c.start).stringValue());
                Date end = parseDate(bindingSet.getValue(c.end).stringValue());        
                String desc = "";
                if(c.desc!=null&&bindingSet.getValue(c.desc)!=null)
                {
                desc = StringEscapeUtils.escapeJavaScript(bindingSet.getValue(c.desc).stringValue());
                }
                String image ="";                
                if(c.image!=null&&bindingSet.getValue(c.image)!=null)
                {
                    image = bindingSet.getValue(c.image).stringValue(); 
                }
                String label = StringEscapeUtils.escapeJavaScript(bindingSet.getValue(c.label).stringValue().replace("\"", ""));
                String link = "";
                if(c.link!=null)
                {
                    link = EndpointImpl.api().getRequestMapper().getRequestStringFromValue(bindingSet.getValue(c.link));
                }
                events.add(new FTimeline.Event( start, end, label, desc, link, image) );
            }
        }
        catch(QueryEvaluationException e)
        {
            logger.warn(e.toString());
            return WidgetEmbeddingError.getErrorLabel(id, ErrorType.QUERY_EVALUATION, e.toString());
        }
        catch(MalformedQueryException e)
        {
            logger.warn(e.toString());
            return WidgetEmbeddingError.getErrorLabel(id, ErrorType.QUERY_ENCODING_ERROR, e.toString());
        }

        timeline.setInterval(c.interval.toString());
        timeline.setTimezone(c.timezone);
        timeline.setEvents(events);
        timeline.setColWidth(c.colwidth);
        
        
        if(StringUtil.isNotNullNorEmpty(c.width))
        	timeline.setWidth(c.width);
        if(StringUtil.isNotNullNorEmpty(c.height))
        	timeline.setHeight(c.height);
        
        return timeline; 
    }

    @Override
    public String getTitle()
    {
        return "Timeline";
    }

    @Override
    public Class<?> getConfigClass()
    {
        return Config.class;
    }
    
    
    @Override
	protected boolean isAsynchLoad() {
    	/* deactivate asynch handling for this particular widget */
		return false;
	}

	private static Date parseDate(String dateOriginalFormat)
    {   
        Date date = null;
        
        // Loop over supported date formats
        for (DateFormat formatter : formatters)
        {
            try
            {
            	formatter.setTimeZone(TimeZone.getTimeZone("GMT-0"));
                date = (Date) formatter.parse(dateOriginalFormat);
                break;
            }
            // If date format is not supported..
            catch (ParseException e)
            {
            	//ignore, try next formatter
            }
        }
        if(date==null) 
        {
            logger.error("Date format not supported: "
                    + dateOriginalFormat + ". Using today instead.");
            date = new Date();
        }
        return date;
    }
	
    @Override
	public List<String> jsURLs()
	{
		String cp = EndpointImpl.api().getRequestMapper().getContextPath();
        return Lists.newArrayList(cp + "/timeline_2.3.0/timeline_js/timeline-api.js" );
	}

}
