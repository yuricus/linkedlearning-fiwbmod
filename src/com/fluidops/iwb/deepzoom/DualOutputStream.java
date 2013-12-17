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

package com.fluidops.iwb.deepzoom;

import java.io.IOException;
import java.io.OutputStream;

public class DualOutputStream extends OutputStream
{
    
    private OutputStream out1;
    private OutputStream out2;
    
    public DualOutputStream(OutputStream out1, OutputStream out2)
    {
       this.out1 = out1;
       this.out2 = out2;
    }
    
    public void close() throws IOException
    {
        out1.close();
        out2.close();
    }

    public void flush() throws IOException
    {
        out1.flush();
        out2.flush();
    }

    
    public void write(byte[] b, int off, int len) throws IOException
    {
        out1.write(b, off, len);
        out2.write(b, off, len);
    }

    public void write(byte[] b) throws IOException
    {
        out1.write(b);
        out2.write(b);
    }

    
    @Override
    public void write(int b) throws IOException
    {
        out1.write(b);
        out2.write(b);
    }

}
