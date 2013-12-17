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

package com.fluidops.iwb.api.operator;

import java.util.List;
import java.util.Set;

import org.openrdf.model.Value;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Operator utility functions.
 * 
 * @author uli
 *
 */
public class OperatorUtil {	

	
	public static String removeEnclosingTicks(String serialized) {
		String token = serialized.startsWith("'") ? serialized.substring(1) : serialized;
		token = token.endsWith("'") ? token.substring(0, token.length()-1) : token;
		return token;
	}
	
	public static boolean isEnclosedByTicks(String serialized) {
		return serialized.startsWith("'") && serialized.endsWith("'") && serialized.length()>1;
	}
	
	/**
	 * Replace special tokens in the input string,
	 * 
	 * {{Pipe}} => |
	 * 
	 * @param input
	 * @return
	 */
	public static String replaceSpecialTokens(String input) {
		input = input.replaceAll("\\{\\{Pipe\\}\\}", "|");

		return input;
	}
	
	
	/**
	 * Tries a conversion of the given value to the expected target type.
	 * 
	 * Currently supports:
	 * String => {@link Value#stringValue()}
	 * 
	 * @param v
	 * @param targetType
	 * @return
	 */
	public static Object toTargetType(Value v, Class<?> targetType) {
		if (v==null)
			return null;
		if (targetType.equals(String.class))
			return v.stringValue();	
		if (targetType.equals(List.class))
			return Lists.newArrayList(v);
		return v;	
	}
	
	private static final Set<Class<?>> primitives = Sets.<Class<?>>newHashSet(String.class, Integer.class, int.class, Boolean.class, boolean.class, Double.class, double.class, Long.class, long.class );
	
	/**
	 * Returns true if the target type represents a primitive constant according
	 * to the operator framework. Primitives are represented as {@link OperatorConstantNode}.
	 * The set of primitives corresponds to Java primitives and their Object representations,
	 * as well as enumerations and Strings.
	 * 
	 * @param targetType
	 * @return
	 */
	public static boolean isPrimitive(Class<?> targetType) {
		return primitives.contains(targetType) || targetType.isEnum();
	}
	
	/**
	 * Returns true if the given operator is an empty {@link OperatorStructNode}
	 * @param op
	 * @return
	 */
	public static boolean isEmptyStructOperator(Operator op) {
		if (op==null)
			return false;
		return op.isStructure() && ((OperatorStructNode)op.getRoot()).keySet().isEmpty();
	}
}
