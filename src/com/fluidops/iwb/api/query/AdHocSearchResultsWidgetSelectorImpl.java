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

package com.fluidops.iwb.api.query;

import static com.fluidops.iwb.api.query.QueryFieldProfile.FieldType.DATE;
import static com.fluidops.iwb.api.query.QueryFieldProfile.FieldType.NOMINAL;
import static com.fluidops.iwb.api.query.QueryFieldProfile.FieldType.NUMERIC;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.fluidops.iwb.api.AbstractWidgetSelector;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.operator.Operator;
import com.fluidops.iwb.api.query.QueryFieldProfile.FieldType;
import com.fluidops.iwb.model.AbstractMutableTupleQueryResult;
import com.fluidops.iwb.page.PageContext;
import com.fluidops.iwb.page.SearchPageContext;
import com.fluidops.iwb.util.Config;
import com.fluidops.iwb.widget.BarChartWidget;
import com.fluidops.iwb.widget.GMapWidget;
import com.fluidops.iwb.widget.LineChartWidget;
import com.fluidops.iwb.widget.PieChartWidget;
import com.fluidops.iwb.widget.PivotWidget;
import com.fluidops.iwb.widget.TableResultWidget;
import com.fluidops.iwb.widget.TimelineWidget;
import com.fluidops.iwb.widget.Widget;

/**
 * An implementation of a widget selector for the hybrid search page. 
 * Based on the submitted query and returned query results it tries 
 * to guess the datatypes of returned variables. Based on these datatypes, 
 * the widgets which can provide suitable visualization are selected.
 * 
 * @author andriy.nikolov
 *
 */
public class AdHocSearchResultsWidgetSelectorImpl extends AbstractWidgetSelector {

	private String query;
	
	// Flag determining whether the query results should also be used for datatype guessing
	private boolean useQueryResultsToDetermineDatatypes = true;
	
	private boolean infer = false;
	private String queryType;
	private String queryLanguage;
	AbstractMutableTupleQueryResult mutableQueryResult = null;
	
	@Override
	public void selectWidgets(PageContext pc) throws RemoteException {
		
		// Should only be applied from the search results page
		if(!(pc instanceof SearchPageContext)) {
			throw new IllegalStateException("SearchPageContext expected, was " + pc.getClass());
		}
		
		SearchPageContext spc = (SearchPageContext)pc;
		initializeWithPageContext(spc);
		
		Map<String, QueryFieldProfile> mapFieldProfiles;
		Map<QueryFieldProfile.FieldType, Set<String>> mapVariablesByType;
		
		if (queryType.equals("SELECT")) {
			
			TupleQueryResultDatatypeEstimator estimator = new TupleQueryResultDatatypeEstimator(
					query, mutableQueryResult);

			mapFieldProfiles = estimator
					.getPossibleDataTypes(useQueryResultsToDetermineDatatypes);

			mapVariablesByType = buildMapVariablesByType(mapFieldProfiles);


			Widget<?> widget;
			for (Class<? extends Widget<?>> widgetClass : EndpointImpl
					.api().getWidgetService().getWidgets()) {
				if (canBeMapped(widgetClass, mapFieldProfiles, mapVariablesByType)) {
					widget = guessInitialConfiguration(
							widgetClass,
							mapFieldProfiles, 
							mapVariablesByType);
					if (widget != null)
						pc.widgets.add(widget);
				}
			} 

		} else if (Config.getConfig().getPivotActive())
		{
			PivotWidget piv = new PivotWidget();
			guessInitialPivotConfiguration(piv);
			pc.widgets.add(piv);
		}

	}
	
	void initializeWithPageContext(SearchPageContext pc) {
		this.query = pc.query;
		this.infer = pc.infer;
		this.queryLanguage = pc.queryLanguage;
		this.queryType = pc.queryType;
		if(pc.queryResult instanceof AbstractMutableTupleQueryResult) {
			mutableQueryResult = (AbstractMutableTupleQueryResult) pc.queryResult;
		}
	}
	
	/**
	 * Checks whether the widget class can be configured for the actual set of query variables.
	 * Implementation note: mapVariablesByType is fully initialized for all {@link FieldType}s
	 * 
	 * @param widgetClass
	 * @return
	 */
	boolean canBeMapped(Class<? extends Widget<?>> widgetClass, Map<String, QueryFieldProfile> mapFieldProfiles, Map<FieldType, Set<String>> mapVariablesByType) {
		if(widgetClass.equals(BarChartWidget.class)
				 || widgetClass.equals(PieChartWidget.class )
				 || widgetClass.equals(LineChartWidget.class)) {
			if(mapVariablesByType.get(NUMERIC).size()>0
					&& mapVariablesByType.get(NOMINAL).size()>0)
				return true;
		} else if(widgetClass.equals(TimelineWidget.class)) {
			if(mapVariablesByType.get(DATE).size()>0
					&& mapVariablesByType.get(NOMINAL).size()>0) {
				return true;
			}
		} else if(widgetClass.equals(GMapWidget.class)) {
			return canBeMappedToGMap(mapFieldProfiles);
		} else if(widgetClass.equals(TableResultWidget.class)) {
			return (this.queryLanguage.equals("SPARQL") && this.queryType.equals("SELECT"));
		} else if(widgetClass.equals(PivotWidget.class)) {
			return Config.getConfig().getPivotActive() && this.canBeMappedToPivot(mapFieldProfiles);
		} 
		
		return false;
		
	}
	
	Widget<?> guessInitialConfiguration(Class<? extends Widget<?>> widgetClass, Map<String, QueryFieldProfile> mapFieldProfiles, Map<FieldType, Set<String>> mapVariablesByType) {
		
		Widget<?> widget;
		try {
			widget = widgetClass.newInstance();
		} catch (Exception e) {
			return null;
		} 
		if(widgetClass.equals(BarChartWidget.class)
				||widgetClass.equals(PieChartWidget.class)
				||widgetClass.equals(LineChartWidget.class)) {
			guessInitialChartConfiguration(widget, mapVariablesByType);
		} else if(widgetClass.equals(TimelineWidget.class)) {
			guessInitialTimelineConfiguration((TimelineWidget)widget, mapFieldProfiles, mapVariablesByType);
		} else if(widgetClass.equals(GMapWidget.class)) {
			guessInitialGMapConfiguration((GMapWidget)widget, mapFieldProfiles, mapVariablesByType);
		} else if(widgetClass.equals(TableResultWidget.class)) {
			guessInitialTableConfiguration((TableResultWidget)widget);
		} else if(widgetClass.equals(PivotWidget.class)) {
			guessInitialPivotConfiguration((PivotWidget)widget);
		}
		
		return widget;

	}
	
	private void guessInitialTableConfiguration(TableResultWidget widget) {
		StringBuilder mappingBuilder = new StringBuilder("{{");
		if(infer) {
			mappingBuilder.append(" infer = 'true' |");
		}
		mappingBuilder.append(" query = '");
		mappingBuilder.append(query);
		mappingBuilder.append("' }}");
		
		widget.setMapping(Operator.parse(mappingBuilder.toString()));
	}
	
	private void guessInitialChartConfiguration(Widget<?> widget, Map<QueryFieldProfile.FieldType, Set<String>> mapVariablesByType) {
		
		Set<String> numericVariables = mapVariablesByType.get(NUMERIC);
		if (numericVariables==null || numericVariables.isEmpty())
			throw new AssertionError("Must have at least one numeric variable");
		
		Set<String> nominalVariables = mapVariablesByType.get(NOMINAL);
		if (nominalVariables==null || nominalVariables.isEmpty())
			throw new AssertionError("Must have at least one nominal variable");
		
		// Select y - the first numeric variable in the pool
		String y = numericVariables.iterator().next();
		
		// Select x - the first nominal value (non-numeric and non-date string or a resource)
		String x = nominalVariables.iterator().next();
		
		StringBuilder mappingBuilder = new StringBuilder("{{");
		
		if(infer) {
			mappingBuilder.append(" infer = 'true' |");
		}
		mappingBuilder.append(" input = '");
		mappingBuilder.append(x);
		mappingBuilder.append("' | output = {{ '");
		mappingBuilder.append(y);
		mappingBuilder.append("' }} | query = '");
		mappingBuilder.append(query);
		mappingBuilder.append("' }}");
		
		widget.setMapping(Operator.parse(mappingBuilder.toString()));
	}
	
	
	private void guessInitialTimelineConfiguration(TimelineWidget widget, Map<String, QueryFieldProfile> mapFieldProfiles, Map<QueryFieldProfile.FieldType, Set<String>> mapVariablesByType) {
		
		Set<String> dateVariables = mapVariablesByType.get(DATE);
		if (dateVariables==null || dateVariables.isEmpty())
			throw new AssertionError("Must have at least one date variable");
		
		Set<String> nominalVariables = mapVariablesByType.get(NOMINAL);
		if (nominalVariables==null || nominalVariables.isEmpty())
			throw new AssertionError("Must have at least one nominal variable");
		
		// Select start - the first date/time variable
		String start = dateVariables.iterator().next();
		
		// For the initial version, only time points as events
		String end = start;
		
		// Something to serve as a label and desc
		String label = nominalVariables.iterator().next();
		
		QueryFieldProfile startDateProfile = mapFieldProfiles.get(start);
		
		long diff = 0;
		
		if((startDateProfile.maxDate!=null)&&(startDateProfile.minDate!=null)) {
			diff = startDateProfile.maxDate.getTime()-startDateProfile.minDate.getTime();
		}
		
		String interval = determineTimeInterval(diff).toString();
			
		StringBuilder mappingBuilder = new StringBuilder("{{");
		
		if(infer) {
			mappingBuilder.append(" infer = 'true' |");
		}
		mappingBuilder.append(" start = '");
		mappingBuilder.append(start);
		mappingBuilder.append("' | end = '");
		mappingBuilder.append(end);
		mappingBuilder.append("' | label = '");
		mappingBuilder.append(label);
		mappingBuilder.append("' | interval = '");
		mappingBuilder.append(interval);
		mappingBuilder.append("' | desc = '");
		mappingBuilder.append(label);
		mappingBuilder.append("' | query = '");
		mappingBuilder.append(query);
		mappingBuilder.append("' }}");
			
		widget.setMapping(Operator.parse(mappingBuilder.toString()));
		
	}
	
	private boolean canBeMappedToPivot(Map<String, QueryFieldProfile> mapFieldProfiles) {
		if(mutableQueryResult!=null) {
			if(!mutableQueryResult.getBindingNames().isEmpty()) {
				if(mapFieldProfiles.get(mutableQueryResult.getBindingNames().iterator().next()).getPossibleFieldTypes().contains(FieldType.RESOURCE))
					return true;
			}
		}
		return false;
	}
	
	private boolean canBeMappedToGMap(Map<String, QueryFieldProfile> mapFieldProfiles) {
		
		// Check whether the search results contain location as a string
		QueryFieldProfile tmpProfile = mapFieldProfiles.get("location"); 
		if(tmpProfile != null && tmpProfile.getPossibleFieldTypes().contains(FieldType.NOMINAL)) 
			return true;
		
		// If not, check if the results contain geo coordinates as "lng" and "lat" variables
		tmpProfile = mapFieldProfiles.get("lng");
		QueryFieldProfile tmpProfile2 = mapFieldProfiles.get("lat");
		
		if(tmpProfile!=null 
				&& tmpProfile2!=null 
				&& tmpProfile.getPossibleFieldTypes().contains(FieldType.GEO_LONGITUDE) 
				&& tmpProfile2.getPossibleFieldTypes().contains(FieldType.GEO_LATITUDE))
			return true;
			
		return false;
	}
	
	private void guessInitialGMapConfiguration(GMapWidget widget, Map<String, QueryFieldProfile> mapFieldProfiles, Map<QueryFieldProfile.FieldType, Set<String>> mapVariablesByType) {
		
		QueryFieldProfile location = mapFieldProfiles.get("location");
		QueryFieldProfile lng = mapFieldProfiles.get("lng");
		QueryFieldProfile lat = mapFieldProfiles.get("lat");
		
		if(location==null && (lng==null || lat==null))
			throw new AssertionError("The query must have either the 'location' variable or a combination of 'lng' and 'lat' variables specified");
		
		StringBuilder mappingBuilder = new StringBuilder("{{");
		mappingBuilder.append(" markers = $");
		mappingBuilder.append(query);
		mappingBuilder.append("$ }}");
				
		widget.setMapping(Operator.parse(mappingBuilder.toString()));
		
	}

	private void guessInitialPivotConfiguration(PivotWidget piv) {
		PivotWidget.Config cfg = new PivotWidget.Config();
		cfg.query = query;
		piv.setConfig(cfg);
	}
	
	/**
	 * Implementation note: mapVariablesByType is fully initialized for all {@link FieldType}s
	 * 
	 * @param mapFieldProfiles
	 * @return
	 */
	Map<QueryFieldProfile.FieldType, Set<String>> buildMapVariablesByType(Map<String, QueryFieldProfile> mapFieldProfiles) {
		
		Map<FieldType, Set<String>> mapVariablesByType = new HashMap<FieldType, Set<String>>();
		
		for(FieldType fieldType : FieldType.values()) {
			mapVariablesByType.put(fieldType, new HashSet<String>());
		}
		
		for(Entry<String, QueryFieldProfile> varEntry : mapFieldProfiles.entrySet()) {
			for(QueryFieldProfile.FieldType fieldType : varEntry.getValue().getPossibleFieldTypes()) {
				mapVariablesByType.get(fieldType).add(varEntry.getKey());
			}
		}
		
		return mapVariablesByType;
	}
	
	private TimelineWidget.TimeInterval determineTimeInterval(long millis) {
		
		long millisInDay = TimeUnit.DAYS.toMillis(1l);
		
		if(millis==0) {
			return TimelineWidget.TimeInterval.YEAR;
		} else if(millis < TimeUnit.SECONDS.toMillis(1l)) {
			return TimelineWidget.TimeInterval.MILLISECOND;
		} else if(millis < TimeUnit.MINUTES.toMillis(1l)) {
			return TimelineWidget.TimeInterval.SECOND;
		} else if(millis < TimeUnit.HOURS.toMillis(1l)) {
			return TimelineWidget.TimeInterval.MINUTE;
		} else if(millis < TimeUnit.DAYS.toMillis(1l)) {
			return TimelineWidget.TimeInterval.HOUR;
		} else if(millis < 7l * millisInDay) {
			return TimelineWidget.TimeInterval.DAY;
		} else if(millis < 30l * millisInDay) {
			return TimelineWidget.TimeInterval.WEEK;
		} else if(millis < 365l * millisInDay) {
			return TimelineWidget.TimeInterval.MONTH;
		} else if(millis < 3650l * millisInDay) {
			return TimelineWidget.TimeInterval.YEAR;
		} else if(millis < 36500l * millisInDay) {
			return TimelineWidget.TimeInterval.DECADE;
		} else if(millis < 365000l * millisInDay) {
			return TimelineWidget.TimeInterval.CENTURY;
		} else {
			return TimelineWidget.TimeInterval.MILLENIUM;
		}
		
	}

}
