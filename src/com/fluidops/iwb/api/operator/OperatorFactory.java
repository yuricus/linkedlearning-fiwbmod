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

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.openrdf.model.Value;

import com.fluidops.iwb.model.ParameterConfigDoc;
import com.fluidops.iwb.ui.configuration.ConfigurationFormUtil;

/**
 * Factory for easy access to {@link OperatorNode} for external users.
 * 
 * @author as
 *
 */
public class OperatorFactory {

	/**
	 * Create a new {@link Operator} with the given operator node
	 * as root
	 * 
	 * @param operatorNode
	 * @return
	 */
	public static Operator toOperator(OperatorNode operatorNode) {
		return new Operator(operatorNode);
	}
	
	/**
	 * Try to convert the given object to an Operator.
	 * 
	 * Primitives according to {@link OperatorUtil#isPrimitive(Class)} are 
	 * returned as {@link OperatorConstantNode}. 
	 * 
	 * Lists are returned as {@link OperatorListNode}
	 * 
	 * Other objects are being dealt with as {@link OperatorStructNode},
	 * where only those fields are considered that are annotated with
	 * {@link ParameterConfigDoc}.
	 * 
	 */
	public static Operator toOperator(Object object) {
		
		if (object==null)
			return Operator.createNoop();
		
		Class<?> clazz = object.getClass();
		
		if (OperatorUtil.isPrimitive(clazz)) {
			if (clazz.equals(String.class))
				return toOperator(textInputToOperatorNode(object.toString(), String.class));
			return OperatorParser.toOperator(object);
		}
		
		if (object instanceof Value) {
			return toOperator(valueToOperatorNode((Value) object));
		}
		
		// handle lists
		if (List.class.isAssignableFrom(clazz)) {
			OperatorListNode listNode = new OperatorListNode();
			for (Object o : (List<?>)object) {
				listNode.addChild( toOperator(o).getRoot() );
			}
			return toOperator(listNode);
		}
		
		// collect all non-null field values of fields that are annotated with ParameterConfigDoc
		OperatorStructNode opStruct = new OperatorStructNode();
		for (Field f : ConfigurationFormUtil.getConfigFieldsSorted(object.getClass())) {
			try {
				Object value = f.get(object);
				if (value!=null)
					opStruct.add(f.getName(), toOperator( value).getRoot());
			} catch (Exception e) {
				throw new IllegalStateException("Could not convert field to operator: " + f.getName(), e);
			} 
		}
		return toOperator(opStruct);
	}
	

	/**
	 * Generate an {@link OperatorNode} from the given textual info. Adds
	 * single quotes if necessary (dependent on the targetType)
	 * 
	 * This method replaces some special tokens:
	 *  | => {{Pipe}} 
	 * 
	 * @param value
	 * @param targetType
	 * @return
	 */
	public static OperatorNode textInputToOperatorNode(String value, Class<?> targetType) {
		value = value.replace("|", "{{Pipe}}");
		return OperatorParser.toOperatorNode( addQuotes(value, targetType) );			
	}

	public static OperatorNode valueToOperatorNode(Value value) {
		return OperatorParser.toOperatorNode(value);
	}
	
	/**
	 * Create a dynamic operator from the given string value. The given string
	 * value must denote valid dynamic operator syntax (i.e. enclosed by $).
	 * 
	 * @param value
	 * @return
	 */
	public static OperatorNode textInputToDynamicOperatorNode(String value) {
		if (!value.startsWith("$"))
			throw new IllegalArgumentException("Not a valid dynamic operator, expected $: " + value);
		return OperatorParser.toOperatorNode(value);
	}
	
	/**
	 * Generate a {@link OperatorNode} from the given map. Creates a structure
	 * operator from the map entries.
	 * 
	 * @param map
	 * @return
	 */
	public static OperatorNode mapToOperatorNode(Map<String, OperatorNode> map) {
		
		OperatorStructNode res = new OperatorStructNode();
		for (Entry<String, OperatorNode> e : map.entrySet())
			res.add(e.getKey(), e.getValue());
		return res;
	}
	
	
	/**
	 * Generate a {@link OperatorNode} from the specified list. Adds all items
	 * to a list operator and sets the list generic type.
	 * 
	 * @param list
	 * @param listType
	 * @return
	 */
	public static OperatorNode listToOperatorNode(List<OperatorNode> list, Class<?> listType) {
		
		OperatorListNode res = new OperatorListNode();
		res.setListType(listType);
		for (OperatorNode c : list)
			res.addChild(c);
		return res;
	}
	
	public static Operator operatorNodeToOperator(OperatorNode opNode) {
		return new Operator(opNode);
	}
	
	/**
	 * Convert the given operator to text input
	 * 
	 * This method replaces some special tokens:
	 *   {{Pipe}} => |
	 *   
	 * @param op
	 * @param valueType
	 * @return
	 */
	public static String operatorToText(Operator op, Class<?> valueType) {
		if (op==null)
			return "";
		// TODO think about a more appropriate implementation
		if (op.isList() || op.isStructure())
			return op.toString();
		
		// TODO add proper handling for Values, e.g.:
//		if (op.isValue()) {
//			Value val = op.evaluate(Value.class);
//			if (valueType.equals(String.class))
//				return val.stringValue();
//			else
//				return val.toString();
//		}
		
		String operatorString = op.toString();
		operatorString = operatorString.replace("{{Pipe}}", "|");
		// remove quotes if the operator does not contain the special $ token, compare bug 11095
		operatorString = operatorString.contains("$") ? operatorString : removeQuotes(operatorString, valueType);
		return operatorString;
	}
	
	/**
	 * Returns the operator as a #widget snippet definition and applies
	 * pretty printing rules.
	 */
	public static String toWidgetWikiSnippet(Operator operator, String widgetName) {		
		return toWidgetWikiSnippet(operator.getRoot(), widgetName);
	}
	
	public static String toWidgetWikiSnippet(OperatorNode operator, String widgetName) {
		
		StringBuilder sb = new StringBuilder();
		sb.append("{{#widget: ").append(widgetName);
		if (operator!=null && !(operator instanceof OperatorVoidNode))
			sb.append(" | ");
		appendOperatorNode(operator, sb, 0);
		sb.append("}}");
		return sb.toString();
	}
	
	private static void appendOperatorNode(OperatorNode opNode, StringBuilder sb, int indent) {
		if (opNode==null)
			return;
		if (opNode instanceof OperatorStructNode)
			appendStructOperator((OperatorStructNode)opNode, sb, indent+1);
		else if (opNode instanceof OperatorListNode)
			appendListOperator((OperatorListNode)opNode, sb, indent+1);
		else if (opNode instanceof OperatorConstantNode)
			appendConstantOperator((OperatorConstantNode)opNode, sb, indent);
		else if (opNode instanceof OperatorThisEvalNode)
			appendThisOperator((OperatorThisEvalNode)opNode, sb, indent);
		else if (opNode instanceof OperatorSelectEvalNode)
			appendSelectOperator((OperatorSelectEvalNode)opNode, sb, indent);
		else if (opNode instanceof OperatorVoidNode)
			return;	// do not append anything
		else
			throw new UnsupportedOperationException("Operator not supported for printing: " + opNode.getClass());
	}
	
	private static void appendStructOperator(OperatorStructNode opStruct, StringBuilder sb, int indent) {
				
		appendNewLine(sb);
		
		if (indent>1)
			appendIndent(sb, indent-1).append("  {{");

		boolean firstItem = true;
		for (String childName : opStruct.keySet()) {
			
			if (firstItem) {
				sb.append(" ");
				firstItem=false;
			} else {
				appendIndent(sb, indent);
				sb.append("| ");
			}
			sb.append(childName).append(" = ");
			appendOperatorNode(opStruct.getOperatorNode(childName), sb, indent);
			appendNewLine(sb);
		}
		
		if (indent>1)
			appendIndent(sb, indent-1).append("  }}");
	}
	
	private static void appendListOperator(OperatorListNode opList, StringBuilder sb, int indent) {
		
		sb.append("{{ ");
		Iterator<OperatorNode> iter = opList.getChildren().iterator();
		while (iter.hasNext()) {
			appendOperatorNode(iter.next(), sb, indent);
			if (iter.hasNext())
				sb.append(" | ");
		}			
		sb.append(" }}");
	}
	
	private static StringBuilder appendConstantOperator(OperatorConstantNode opConst, StringBuilder sb, int indent) {
		return sb.append(opConst.serialize());
	}
	
	private static StringBuilder appendThisOperator(OperatorThisEvalNode opThis, StringBuilder sb, int indent) {
		return sb.append(opThis.serialize());
	}
	
	private static StringBuilder appendSelectOperator(OperatorSelectEvalNode opSelect, StringBuilder sb, int indent) {
		return sb.append(opSelect.serialize());
	}
	
	private static StringBuilder appendNewLine(StringBuilder sb) {
		return sb.append("\n");
	}
	
	private static StringBuilder appendIndent(StringBuilder sb, int indent) {
		sb.append(" ");
		for (int i=1; i<indent; i++)
			sb.append("   ");
		return sb;
	}
	
	/**
	 * Add single quotes to the operator if required. Single quotes
	 * are required for Strings that are not already enclosed by
	 * single quotes, as well as for URIs, and Literals. They
	 * are in particular not required for primitive types (as
	 * well as their object correspondences), for enumerations
	 * and for operator values enclosed by $.
	 * 
	 * @param value
	 * @param type a supported type, must not be null
	 * @return
	 */
	static String addQuotes(String value, Class<?> type)
    {
		if (type==null)
			throw new IllegalArgumentException("Provided type must not be null");
		
		// all primitives of the operator framework (Integer, int, Boolean)
		// are serialized as is, i.e. without enclosing ticks
		if (OperatorUtil.isPrimitive(type) && !type.equals(String.class) && !type.isEnum())
			return value;
		else if (isOperatorValue(value))
			return value;
		else if (OperatorUtil.isEnclosedByTicks(value))
    		return value;
    	else
    		return "'"+value+"'";
    }
	
	/**
	 * Remove single quotes to the operator if required
	 * 
	 * Note:
	 * - if opNodeValue equals '', this is considered to be the empty string
	 *   and we do not remove the ''
	 * 
	 * @param value
	 * @param type
	 * @return
	 */
	static String removeQuotes(String opNodeValue, Class<?> type)
    {
    	if((type != null && (type.equals(Integer.class) || type.equals(Double.class) || type.equals(Enum.class) 
    			|| type.equals(Boolean.class)) || isOperatorValue(opNodeValue.toString()) ))
    		return opNodeValue.toString();
    	else if (opNodeValue.equals("''"))
    		return opNodeValue;
    	return OperatorUtil.removeEnclosingTicks(opNodeValue);
    }
	
	/**
     * This check has been added to avoid the values of isNameRule and idLabelRule in the DataInputWidget 
     * being treated as operators: e.g., if idLabelRule=$firstName$.$lastName$. For this, we check whether 
     * the value has additional '$' characters inside. This doesn't cover the case where the rule only contains one variable:
     * e.g., $lastName$. In that case it is required that the user encloses the value in double quotes: "$lastName$" (see DataInputWidget).
     */
    static boolean isOperatorValue(String value) {
    	if(value.startsWith("$") 
    			&& value.endsWith("$") 
    			&& (value.indexOf('$', 1)==value.length()-1))
    			return true;
    	
    	return false;
    }
}
