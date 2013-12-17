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

package com.fluidops.iwb.xml;

import java.io.Serializable;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

/**
 * different types of cross plattform JSON flavors - required for cross site restriction workaround
 * 
 * @author aeb
 */
@SuppressWarnings(value="UWF_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD", justification="Fields writte externally")
public class JsonType implements Serializable
{
    private static final long serialVersionUID = -3906413961132720136L;

    /**
     * delicious -> var x = RES
     * object    -> {RES}
     * function  -> f(RES)
     * 
     * @author aeb
     */
    public static enum Type { delicious, object, function };
    public Type type;
    public String fnName;
    public String varName;
    public int numParameters;
    
    public boolean isXdomain()
    {
        if ( type == Type.object )
            return false;
        else
            return true;
    }
    
    public String toString()
    {
        if ( type == Type.delicious )
            return "var: " + varName;
        else if ( type == Type.function )
            return fnName + "("+numParameters+")";
        else
            return "single literal";
    }
}
