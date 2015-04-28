/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.pdfbox.cos;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;

import org.apache.pdfbox.util.Charsets;

/**
 * This class represents a floating point number in a PDF document.
 *
 * @author Ben Litchfield
 * 
 */
public class COSFloat extends COSNumber
{
    private BigDecimal value;
    private String valueAsString;

    /**
     * @param aFloat The primitive float object that this object wraps.
     */
    public COSFloat( float aFloat )
    {
        // use a BigDecimal as intermediate state to avoid 
        // a floating point string representation of the float value
        value = new BigDecimal(String.valueOf(aFloat));
        valueAsString = removeNullDigits(value.toPlainString());
    }

    /**
     * @param aFloat The primitive float object that this object wraps.
     * @throws IOException If aFloat is not a float.
     */
    public COSFloat( String aFloat ) throws IOException
    {
        try
        {
            valueAsString = aFloat; 
            value = new BigDecimal( valueAsString );
        }
        catch( NumberFormatException e )
        {
            throw new IOException( "Error expected floating point number actual='" +aFloat + "'", e );
        }
    }

    private static String removeNullDigits(String plainStringValue)
    {
        // remove fraction digit "0" only
        if (plainStringValue.indexOf('.') > -1 && !plainStringValue.endsWith(".0"))
        {
            while (plainStringValue.endsWith("0") && !plainStringValue.endsWith(".0"))
            {
                plainStringValue = plainStringValue.substring(0,plainStringValue.length()-1);
            }
        }
        return plainStringValue;
    }

    @Override
    public float floatValue()
    {
        return value.floatValue();
    }

    @Override
    public double doubleValue()
    {
        return value.doubleValue();
    }

    @Override
    public long longValue()
    {
        return value.longValue();
    }

    @Override
    public int intValue()
    {
        return value.intValue();
    }

    @Override
    public boolean equals( Object o )
    {
        return o instanceof COSFloat && 
                Float.floatToIntBits(((COSFloat)o).value.floatValue()) == Float.floatToIntBits(value.floatValue());
    }

    @Override
    public int hashCode()
    {
        return value.hashCode();
    }

    @Override
    public String toString()
    {
        return "COSFloat{" + valueAsString + "}";
    }

    @Override
    public void accept(COSVisitor visitor) throws IOException
    {
        visitor.visit(this);
    }

    /**
     * Writes the {@link COSFloat} to the given {@link OutputStream}
     *
     * @param output The stream to write to.
     * @throws IOException If there is an error writing to the stream.
     */
    public void writeTo(OutputStream output) throws IOException
    {
        output.write(valueAsString.getBytes(Charsets.ISO_8859_1));
    }
}
