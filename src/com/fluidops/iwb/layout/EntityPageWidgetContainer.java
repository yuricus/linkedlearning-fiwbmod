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
import java.util.Set;

import com.fluidops.ajax.components.FContainer;
import com.fluidops.iwb.page.PageContext;
import com.fluidops.iwb.widget.AbstractWidget;
import com.fluidops.iwb.widget.GraphWidget;
import com.fluidops.iwb.widget.PivotWidget;
import com.fluidops.iwb.widget.SemWikiWidget;
import com.fluidops.iwb.widget.TripleEditorWidget;
import com.fluidops.util.Rand;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import ru.ifmo.ailab.OntoViewerWidget;


/**
 * A special {@link TabWidgetContainer} which in addition to the views
 * renders all other widgets configured for the resource in a container
 * on the right-hand side of the UI.
 * 
 * @author as
 *
 */
public class EntityPageWidgetContainer extends TabWidgetContainer {
	
	private FContainer mainContainer;
	private FContainer rightContainer;

	protected List<AbstractWidget<?>> allWidgets = Lists.newArrayList();
	
	
	public EntityPageWidgetContainer() {		
		super();	
	}		

	@Override
	protected void initialize() {
		super.initialize();
		
		mainContainer = new FContainer(Rand.getIncrementalFluidUUID());
		
		// TODO: rename this class, also in fiwb styleshee
		tabPane.appendClazz("testContainerClass");
				
		rightContainer = new FContainer(Rand.getIncrementalFluidUUID());
		// TODO: check if this class can be renamed to rightContainerClass
		rightContainer.appendClazz("leftContainerClass");
		
		mainContainer.add(tabPane);
		mainContainer.add(rightContainer);
	}

	@Override
	public void add(final AbstractWidget<?> widget, String id) {
		
		if (isViewTab(widget)) {
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
		} else {
			
			FContainer cont = new FContainer(Rand.getIncrementalFluidUUID()) {				
				@Override
				public String render() {
					return "<center><div class=\"widgetTitle\">" + widget.getTitle() + "</div></center>" +super.render();
				}				
			};
			cont.add(widget.getComponentUAE(id));
			rightContainer.add(cont);
			
		}			
		
		// remember all widgets that are to be rendered
		allWidgets.add(widget);
	}
	

	/**
	 * Returns true if the current widget is rendered as a view in the
	 * tabpane, via {@link #postRegistration(PageContext)}
	 * 
	 * @param widget
	 * @return
	 */
	protected boolean isViewTab(AbstractWidget<?> widget) {
		Class<?> widgetClass = widget.getClass();
		return widgetClass.equals(SemWikiWidget.class) || widgetClass.equals(TripleEditorWidget.class)
				|| widgetClass.equals(GraphWidget.class) || widgetClass.equals(PivotWidget.class) || widgetClass.equals(OntoViewerWidget.class);
	}

	@Override
	public FContainer getContainer() {
		return mainContainer;
	}

	@Override
	public List<String> jsUrls() {

		Set<String> res = Sets.newLinkedHashSet();
		for (AbstractWidget<?> widget : allWidgets) {
			res.addAll(widget.jsURLs());
		}
		
		return Lists.newArrayList(res);
	}
}
