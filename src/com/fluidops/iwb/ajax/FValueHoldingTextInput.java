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

package com.fluidops.iwb.ajax;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.RDFS;

import com.fluidops.ajax.FClientUpdate;
import com.fluidops.ajax.FClientUpdate.Prio;
import com.fluidops.ajax.FEvent;
import com.fluidops.ajax.components.FAbstractTextInputWithAutosuggestion;
import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FHTML;
import com.fluidops.ajax.helper.FHelpers;
import com.fluidops.config.Config;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.api.ReadDataManagerImpl;
import com.fluidops.iwb.autocompletion.AutoCompleteFactory;
import com.fluidops.iwb.autocompletion.AutoCompletionUtil;
import com.fluidops.iwb.autocompletion.AutoSuggester;
import com.fluidops.util.Rand;
import com.fluidops.util.StringUtil;
import com.google.common.collect.Lists;

/**
 * This class realises the text input with autosuggestion, which contains an RDF value, rather than plain text. 
 * In case if the values are literals, there are no substantial behavioural changes from FTextInput2. If the values are URIs, 
 * the autocompletion menu allows the user to type in either the URI itself or its label and reacts on both. 
 * By default, instance label is displayed instead of the URI itself.  
 * 
 * @author andriy.nikolov
 *
 */
public abstract class FValueHoldingTextInput extends FAbstractTextInputWithAutosuggestion<Value> {

	protected AutoSuggester suggester = AutoCompleteFactory.createFixedListAutoSuggester(Collections.<Value>emptyList());
	protected boolean activated = false;
	
	private HashMap<String, OptionStruct> optionsMap = new HashMap<String, OptionStruct>();
	 
	private HashMap<String, OptionStruct> optionsMapByDisplayName = new HashMap<String, OptionStruct>();
	
	private HashSet<String> duplicateDisplayNames = new HashSet<String>();
	
	private AutocompletionOption<Value> chosenOption;
	
	// Now the options are loaded every time when INPUT_CHANGED is fired. 
	// The first time options are loaded, we need to check 
	// if the currently set value appears in the autosuggestion list.
	// If yes, we display its displayName, otherwise, the value itself.
	private boolean optionsLoaded = false;
	
	private boolean previousInputInterpretedAsAURILabel = false;
	
    
    /**
     * Implementation of the AutocompletionOption for RDF values.
     * @author andriy.nikolov
     *
     */
	private class OptionStruct implements AutocompletionOption<Value> {
		
		private Value value = null;
		private FComponent display = null;
		private String displayLastGeneratedForInput = "";
		private String displayName = null;
		private String displayUri = null;
		
		public OptionStruct(Value value) {
			this.value = value;
			if(value instanceof URI) {
				this.displayUri = AutoCompletionUtil.toDisplayValue(value, false);
				optionsMapByDisplayName.put(displayUri, this);
			}
		}
		
		@Override
		public String getDisplayName() {
			if(displayName!=null) {
				return displayName;
			}
			displayName = EndpointImpl.api().getDataManager().getLabel(value);
			
			if(!duplicateDisplayNames.contains(displayName)) {

				if(optionsMapByDisplayName.put(displayName, this)!=null) {
					optionsMapByDisplayName.remove(displayName);
					duplicateDisplayNames.add(displayName);
				}
			}
			
			return displayName;
		}
		
		@Override
		public FComponent getDisplay(String input) {
			if(display!=null && input!=null && input.equals(displayLastGeneratedForInput)) {
				return display;
			}
			
			ReadDataManagerImpl dm = EndpointImpl.api().getDataManager();

			String displayString = dm.getLabelHTMLEncoded(value);
			String sDisplayUri = (displayUri!=null) ? FHelpers.HTMLify(displayUri) : null;
			
			// The following procedure handles the case where the display URI is given as <fullURI>. In this case, the bevel function should only check 
			// the URI itself. Otherwise, e.g., typing "t" would cause it to highlight the "t" character in "&lt;" and "&gt;".
			// So, we need to remove the brackets before calling bevel.			
			if(value instanceof URI && !displayString.equals(sDisplayUri)) {
				String bevelDisplayUri;
				if(sDisplayUri.startsWith("&lt;")&&sDisplayUri.endsWith("&gt;")) {
					bevelDisplayUri = "&lt;"+bevel(sDisplayUri.substring(4, sDisplayUri.length()-4), input)+"&gt;";
				} else {
					bevelDisplayUri = bevel(displayUri, input);
				}
				display = new FHTML(Rand.getIncrementalFluidUUID(), "<i>" + bevel(displayString, input) + "</i> (" + bevelDisplayUri + ")");
			} else {
				display = new FHTML(Rand.getIncrementalFluidUUID(), bevel(displayString, input));
			}
			
			displayLastGeneratedForInput = input;
			
			return display;
			
		}
		
		@Override
		public boolean matchesInput(String in, ComparisonType comparisonType) {
			boolean ok = false;
			
			switch (comparisonType) {
			case StartsWith:
				ok = ( getDisplayName().toLowerCase().startsWith(in.toLowerCase())
						|| (value instanceof URI 
								&& displayUri.toLowerCase().startsWith(in.toLowerCase())) );
				break;
			case Contains:
				ok = ( getDisplayName().toLowerCase().contains(in.toLowerCase())
						|| (value instanceof URI 
								&& displayUri.toLowerCase().contains(in.toLowerCase())) );
				break;
			case EndsWith:
				ok = ( getDisplayName().toLowerCase().endsWith(in.toLowerCase())
						|| (value instanceof URI 
								&& displayUri.toLowerCase().endsWith(in.toLowerCase())) );
				break;
			default:
				ok = false;
			}
			
			return ok;
		}
		
		public boolean matchesExactly(String in) {
			return value.stringValue().equals(in)
					||getDisplayName().equals(in)
					||(value instanceof URI
							&& displayUri.equals(in));
		}

		@Override
		public Value getValue() {
			return value;
		}

		@Override
		public String getValueId() {
			return value.stringValue();
		}

		/**
		 * @return the displayUri (only for URIs, otherwise null)
		 */
		private String getDisplayUri() {
			return displayUri;
		}
		
	}
	
	
	
	/**
	 * @param id
	 */
	public FValueHoldingTextInput(String id) {
		this(id, "");
	}

	/**
	 * @param id
	 * @param value
	 */
	public FValueHoldingTextInput(String id, String value) {
		this(id, value, "");
	}

	/**
	 * @param id
	 * @param value
	 * @param label
	 */
	public FValueHoldingTextInput(String id, String value, String label) {
		this(id, value, label, true);
	}

	/**
	 * @param id
	 * @param value
	 * @param label
	 * @param enablesuggestion
	 */
	public FValueHoldingTextInput(String id, String value, String label,
			boolean enablesuggestion) {
		super(id, value, label, enablesuggestion);
	}

	/**
	 * @param id
	 * @param enablesuggestion
	 */
	public FValueHoldingTextInput(String id, boolean enablesuggestion) {
		this(id, "", "", enablesuggestion);
	}

	/**
	 * @param id
	 * @param value
	 * @param enablesuggestion
	 */
	public FValueHoldingTextInput(String id, String value,
			boolean enablesuggestion) {
		super(id, value, enablesuggestion);
	}
	
	@Override
	public void handleClientSideEvent(FEvent event) {
		switch (event.getType()) {
		case MOUSE_LEFT: {
			super.handleClientSideEvent(event);
			populateInterpretation();
			break;
		}
		case INPUT_CHANGED: {
			activated = true;
            super.handleClientSideEvent(event);
            
            if(!this.value.equals(event.getArgument())) {
            	populateView();
            }
            populateInterpretation();            
            break;
		}
		case BLUR: // set the value for the dropdown, UP/DOWN executes this
		{
			super.handleClientSideEvent(event);
			populateInterpretation();
			break;
		}
		default: {
			super.handleClientSideEvent(event);
		}

		}

	}
	
	/**
	 * @return the suggester
	 */
	public AutoSuggester getSuggester() {
		return suggester;
	}

	/**
	 * @param suggester the suggester to set
	 */
	public void setSuggester(AutoSuggester suggester) {
		this.suggester = suggester;
	}

	/* (non-Javadoc)
	 * @see com.fluidops.ajax.components.FAbstractTextInputWithAutosuggestion#getSelectedOptionDisplayName(java.lang.String)
	 */
	@Override
	protected String getSelectedOptionDisplayName(String inputValue) {
		chosenOption = optionsMap.get(inputValue);
		return chosenOption.getDisplayName();
	}

	/* (non-Javadoc)
	 * @see com.fluidops.ajax.components.FAbstractTextInputWithAutosuggestion#getSelectedOptionDisplayName(int)
	 */
	@Override
	protected String getSelectedOptionDisplayName(int selectedIndex) {
		chosenOption = _choices.get(selectedIndex);
		return chosenOption.getDisplayName();
	}

	/**
	 * Whenever the input changes, checks whether it matches some option from the autosuggestion list and, 
	 * if yes, sets the option as the chosen one. 
	 */
	@Override
	public void onChange() {
		if(StringUtil.isNullOrEmpty(this.value)) {
			this.chosenOption = null;
		} else {
			if(chosenOption==null 
					|| !((OptionStruct)chosenOption).matchesExactly(this.value) ) { 
					
				this.chosenOption = optionsMapByDisplayName.get(this.value);
			}
		}
	}

	@Override
	protected synchronized List<AutocompletionOption<Value>> getAndSetChoices() {
			if (!activated) {
				return this._choices;
			}
			
			List<Value> suggestions = suggester.suggest(value);
			
			_choices = Lists.newArrayList();
			optionsMap.clear();
			optionsMapByDisplayName.clear();
			
			OptionStruct option;
			
			for (Value suggestion : suggestions) {

				option = new OptionStruct(
						suggestion);
				_choices.add(option);
				optionsMap.put(suggestion.stringValue(), option);
				optionsMapByDisplayName.put(suggestion.stringValue(), option);
			}
			
			// Now the options are loaded every time when INPUT_CHANGED is fired. 
			// The first time options are loaded, we need to check 
			// if the current value appears in the autosuggestion list.
			// If yes, we display its displayName, otherwise, the value itself.
			// TODO: This has to be done only once, so probably the onFocus event handler would suit better.
			// Need to check why the FOCUS event never gets fired for the component.
			if(!optionsLoaded && this.chosenOption!=null ) {
				if(optionsMap.containsKey(this.chosenOption.getValueId())) {
					this.chosenOption = optionsMap.get(this.chosenOption.getValueId());
					this.value = this.chosenOption.getDisplayName();
				}
			}
			
			optionsLoaded = true;
				
			return this._choices;
	}

	/**
	 * @return the chosenOption
	 */
	public Value getChosenRDFValue() {
		return (chosenOption!=null) ? chosenOption.getValue() : null;
	}
	
	public void setRDFValue(Value value) {
		if(value instanceof URI) {
			if(optionsMap.containsKey(value.stringValue())) {
				this.chosenOption = optionsMap.get(value.stringValue());
			} else {
				this.chosenOption = new OptionStruct(value);
			}
		}
	}

	
	/**
	 * Returns an RDF value for the current input. First, checks whether the value has been chosen from the autosuggestion list and, if not, tries to guess it according to the current type.
	 * 
	 * @param currentType
	 * @param languageTag
	 * @return
	 */
	public Value getOrCreateRDFValue(URI currentType, String languageTag) {

		Value val = this.getChosenRDFValue();

		if(valueSatisfiesDatatype(val, currentType))			
			return val;

		ValueFactoryImpl vf = ValueFactoryImpl.getInstance();

		// produce RDF value from original value, input and current type
		if (currentType.equals(RDFS.RESOURCE))
			return EndpointImpl.api().getNamespaceService()
					.guessURI(this.value);
		else if (currentType.equals(RDFS.LITERAL)) {
			// the generic literal
			if (languageTag!=null) {// need to
									// copy over
									// language
									// tag
				return vf.createLiteral(this.value,
						languageTag);
			} else {
				// no language tag to copy
				return vf.createLiteral(this.value);
			}

		} else {
			// explicit XML type; can have no language tag (cf. Sesame
			// Implementation of Literal)
			return vf.createLiteral(this.value, currentType);
		}
	}
	
	/**
	 * 
	 * @return true if the current input represents a display name of a value rather than the value itself.
	 */
	private boolean isCurrentInputAURILabel() {
		if(this.chosenOption!=null 
				&& chosenOption.getValue() instanceof URI
				&& this.chosenOption.getDisplayName().equals(this.value) 
				&& !((OptionStruct)this.chosenOption).getDisplayUri().equals(this.value)
				&& !this.chosenOption.getValueId().equals(this.value)) {
			
			return true;
		}
		
		return false;
	}
	
	/**
	 * Sends a client update indicating whether the input is interpreted as a label or as the value itself. 
	 * For labels, sets the text style to italic. Probably, should be merged with populateValidity()
	 */
	private void populateInterpretation() {
		boolean isURILabel = isCurrentInputAURILabel();

		if(isURILabel!=previousInputInterpretedAsAURILabel) {
			// check the element's existence before trying to change the className
			String checkExistanceInDOM = "if (getDomElementById('" + getComponentid() + "')) ";
			if (Config.getConfig().isAjaxDebug())
				// if ajax debug is enabled --> create output if action fails
				checkExistanceInDOM = "";
			String style = isURILabel ? "italic" : "normal";
			addComponentStyle("font-style", style);
			
			addClientUpdate(new FClientUpdate(Prio.VERYEND, checkExistanceInDOM
					+ "getDomElementById('" + getComponentid() + "').style.fontStyle='" + style + "';"));
		}
		
		previousInputInterpretedAsAURILabel = isURILabel;
	}
	
	private boolean valueSatisfiesDatatype(Value val, URI datatype) {
		if(val==null) return false;
		
		if(val instanceof URI) {
			// If the value is a URI,then it only satisfies rdfs:Resource.
			if(datatype.equals(RDFS.RESOURCE))
				return true;
		} else if(val instanceof Literal) {
			// If the value is an untyped literal, then it only satisfies rdfs:Literal,
			// otherwise the value datatype must be equal to the input datatype.
			Literal lit = (Literal)val;
			if(lit.getDatatype()==null) {
				if(datatype.equals(RDFS.LITERAL))
					return true;
			} else {
				if(lit.getDatatype().equals(datatype))
					return true;
			}
		}
		
		return false;
	}

}