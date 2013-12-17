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

package com.fluidops.iwb.ui.configuration;

import java.util.regex.Pattern;

import org.openrdf.model.URI;

import com.fluidops.ajax.components.FComponent;
import com.fluidops.ajax.components.FForm;
import com.fluidops.ajax.components.FForm.Validator;
import com.fluidops.ajax.components.FForm.VoidValidator;
import com.fluidops.iwb.util.validator.ConvertibleToUriValidator;


/**
 * Base class for all {@link ConfigurationFormElement} implementations.
 * 
 * @author as
 *
 * @param <T> the {@link FComponent} to show as an element
 */
public abstract class AbstractConfigurationFormElement<T> implements ConfigurationFormElement<T> {
	
	protected T component;
	protected boolean required = false;
	
	@Override
	public T getComponent(FormElementConfig formElementConfig) {

		if (component==null) {
			component = createComponent(formElementConfig);
			required = formElementConfig.required();
		}
		return component;
	}

	@Override
	public Validator validator(FormElementConfig formElementConfig) {
		
		Validator res = new VoidValidator();
		
		// type dependent validators
		Class<?> targetType = formElementConfig.targetType;
		if (Integer.class.equals(targetType) || int.class.equals(targetType))
			res = new FForm.NumberValidator();
		else if (Boolean.class.equals(targetType) || boolean.class.equals(targetType))
			res = new FForm.BooleanValidator();
		else if (URI.class.isAssignableFrom(targetType))
			res = new ConvertibleToUriValidator();
		
		// also accept some special wiki syntax (e.g. named parameter {{{parameterName}}}) - bug 11149
		res = new AcceptWikiSyntaxOrNestedValidator(res);
		
		if (formElementConfig.required())
			res = new FForm.NotEmptyAndNestedValidator(res);
		else 
			res = new FForm.EmptyOrNestedValidator(res);
			
		return res;
	}
	
	
	/**
	 * Create the component based on the specified {@link FormElementConfig}.
	 * 
	 * @param formElementConfig
	 * @return
	 */
	protected abstract T createComponent(FormElementConfig formElementConfig);
	
	
	/**
	 * Validator to allow special wiki syntax being used in the configuration
	 * form input elements, e.g. named parameters {{{namedParam}}}.
	 * 
	 * This validator successfully validates if either the nested inner
	 * {@link Validator} is true, or valid wiki syntax is provided.
	 * 
	 * @author as
	 *
	 */
	static class AcceptWikiSyntaxOrNestedValidator implements Validator {

		private static final Pattern pattern = Pattern.compile("(\\{\\{\\{\\w*\\}\\}\\})");
		
		private final Validator inner;
		
		public AcceptWikiSyntaxOrNestedValidator(Validator inner) {
			this.inner = inner;
		}
		@Override
		public boolean validate(FComponent c) {
			return isAcceptedWikiSyntax(c) || inner.validate(c);
		}
		
		private boolean isAcceptedWikiSyntax(FComponent c) {
			if (c.returnValues() == null)
				return false;
			String input = c.returnValues().toString();
			return pattern.matcher(input).matches();
		}
	}
}