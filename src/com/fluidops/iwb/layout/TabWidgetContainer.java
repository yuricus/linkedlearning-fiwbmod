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

package com.fluidops.iwb.layout;

import java.util.List;
import java.util.Map;

import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FContainer;
import com.fluidops.ajax.components.FTabPane2Lazy;
import com.fluidops.ajax.components.FLabel.ElementType;
import com.fluidops.ajax.components.FTabPane2Lazy.ComponentHolder;
import com.fluidops.iwb.page.PageContext;
import com.fluidops.iwb.widget.AbstractWidget;
import com.fluidops.iwb.widget.GraphWidget;
import com.fluidops.iwb.widget.PivotWidget;
import com.fluidops.iwb.widget.SemWikiWidget;
import com.fluidops.iwb.widget.TripleEditorWidget;
import com.fluidops.iwb.widget.Widget;
import com.fluidops.util.Rand;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import ru.ifmo.ailab.OntoViewerWidget;

/**
 * The standard container for the platforms UI. This container shows the four
 * standard views (wiki, table, graph, pivot) in a {@link FTabPane2Lazy}
 * instance, more particularly in {@link ViewTabPane}.
 * 
 * Note in particular that this container does not show other widgets than
 * just the four standard ones.
 * 
 * @author aeb, as
 */
public class TabWidgetContainer implements WidgetContainer
{
	
	/* labels for the view tabs */
	public static final String TAB_WIKI_VIEW = "Wiki View";
	public static final String TAB_TABLE_VIEW = "Table View";
	public static final String TAB_GRAPH_VIEW = "Graph View";
	public static final String TAB_PIVOT_VIEW = "Pivot View";	
		
	
	protected ViewTabPane tabPane;
	protected Map<Class<?>, AbstractWidget<?>> tabWidgets = Maps.newHashMap();
	protected PageContext pc;	
	
	public TabWidgetContainer() {		
		initialize();
	}
	
	protected void initialize() {
		
		tabPane = new ViewTabPane( "tabPaneVert" + Rand.getIncrementalFluidUUID() )
		{
		    public void onTabChange()
		    {
		        pc.session.setSessionState("activeLabel", this.getActiveLabel().returnValues());
		    }
		};
		tabPane.enableClientSideTabCaching = true;
		tabPane.drawAdvHeader(true);
		tabPane.drawHeader(false);
		tabPane.addFlTabHeaderClass("flTabWikiPage");
		tabPane.setTabContentClazz("flTabContentWikiPage");
		tabPane.setTabControlClazz("viewTabPaneControl");
		tabPane.setTabsTemplate("com/fluidops/iwb/layout/TabLayoutSimple");
		tabPane.setTabLabelType(ElementType.DIV);

	}
	
	@Override
	public void postRegistration(PageContext pc )
	{   
		if(tabWidgets.containsKey(SemWikiWidget.class))
			// SemWikiWidget cannot be wrapped by LazyWidgetComponentHolder as it does not implement
			// jsURLs correctly. So the jsURLs can only be computed by the real component SemWiki.
			tabPane.addView(TAB_WIKI_VIEW, tabWidgets.get(SemWikiWidget.class).getComponentUAE(Rand.getIncrementalFluidUUID()), "nav_wiki");
		if(tabWidgets.containsKey(TripleEditorWidget.class))
			tabPane.addLazyView(TAB_TABLE_VIEW, tabWidgets.get(TripleEditorWidget.class), "nav_table");
		if(tabWidgets.containsKey(GraphWidget.class))
			tabPane.addLazyView(TAB_GRAPH_VIEW, tabWidgets.get(GraphWidget.class), "nav_graph");
        if(tabWidgets.containsKey(OntoViewerWidget.class))
                    tabPane.addLazyView(TAB_GRAPH_VIEW, tabWidgets.get(OntoViewerWidget.class), "nav_graph");
		if(tabWidgets.containsKey(PivotWidget.class))
			tabPane.addLazyView(TAB_PIVOT_VIEW, tabWidgets.get(PivotWidget.class), "nav_pivot");
		
		// hide the tabpane control, if there is only a single tab.
		if (tabPane.getNumberOfTabs()==1)
			tabPane.appendClazz("hideTabPaneControl");
		
	    this.pc = pc;
	    // for JUnit-test only, since we cannot set the pagecontext-session artificially
	    if (pc.session != null)
	    {
	    	Object state = pc.session.getSessionState("activeLabel");
	    	if (state!=null)
	    		tabPane.setActiveLabelWithoutRender((String)state);
	    }
	}
	
	
	@Override
	public void add(AbstractWidget<?> widget, String id)
	{		
		/*
		 * We add a conditional check here since we allow only a single
		 * widget configuration for each view (e.g. one configuration for the
		 * graph view). The current implementation of the WidgetSelector
		 * is such that the least specific configuration (i.e. in most
		 * cases the fallback to the rdfs:Resource configuration) is
		 * added last if there are more than one. Hence, with this check
		 * we automatically take the most appropriate.
		 */
		if (!tabWidgets.containsKey(widget.getClass()))
			tabWidgets.put(widget.getClass(), widget);	
	}

	
	@Override
	public FContainer getContainer()
	{		
		return tabPane;
	}	

	@Override
	public List<String> jsUrls() {

		List<String> res = Lists.newArrayList();
		for (AbstractWidget<?> widget : tabWidgets.values()) {
			res.addAll(widget.jsURLs());
		}
		return res;
	}
	
	/**
	 * A {@link ComponentHolder} that constructs the {@link FComponent} of a {@link Widget} on the first call
	 * to {@link #getComponent()}.
	 */
	protected static class LazyWidgetComponentHolder implements ComponentHolder {
		private Widget<?> widget;
		private FComponent cached;
		private static final String[] NO_CSS_URLS = new String[0];

		public LazyWidgetComponentHolder(Widget<?> widget) {
			this.widget = widget;
		}

		@Override
		public FComponent getComponent() {
			if(cached == null) cached = widget.getComponentUAE(Rand.getIncrementalFluidUUID());
			return cached;
		}

		@Override
		public String[] jsURLs() {
			return ((AbstractWidget<?>)widget).jsURLs().toArray(new String[0]);
		}

		@Override
		public String[] cssURLs() {
			return NO_CSS_URLS;
		}		
	}
	
	
	/**
	 * A special {@link FTabPane2Lazy} which provides convenience functionality
	 * to add further views.
	 * 
	 * @author as
	 *
	 */
	public static class ViewTabPane extends FTabPane2Lazy {

		/**
		 * Name of the common CSS class for each tab item
		 */
		final static String viewTabItem = "viewTabItem";
		
		public ViewTabPane(String id) {
			super(id);
		}		
		
		public void addView(String tabTitle, FComponent component, String cssClass) {
			addTab(tabTitle, null, component, cssClass + " "+viewTabItem);
		}
		
		public void addLazyView(String tabTitle, AbstractWidget<?> widget, String cssClass) {
			addLazyView(tabTitle, new LazyWidgetComponentHolder(widget), cssClass);
		}
		
		public void addLazyView(String tabTitle, ComponentHolder componentHolder, String cssClass) {
			addTab(tabTitle, null, componentHolder, cssClass + " " + viewTabItem);
		}
	}
}
