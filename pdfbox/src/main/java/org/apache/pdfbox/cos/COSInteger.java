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

/**
 * This class represents an integer number in a PDF document.
 *
 * @author Ben Litchfield
 */
public final class COSInteger extends COSNumber
{

    /**
     * The lowest integer to be kept in the {@link #STATIC} array.
     */
    private static final int LOW = -100;

    /**
     * The highest integer to be kept in the {@link #STATIC} array.
     */
    private static final int HIGH = 256;

    /**
     * Static instances of all COSIntegers in the range from {@link #LOW}
     * to {@link #HIGH}.
     */
    private static final COSInteger[] STATIC = new COSInteger[HIGH - LOW + 1];

    /**
     * Constant for the number zero.
     * @since Apache PDFBox 1.1.0
     */
    public static final COSInteger ZERO = get(0); 

    /**
     * Constant for the number one.
     * @since Apache PDFBox 1.1.0
     */
    public static final COSInteger ONE = get(1); 

    /**
     * Constant for the number two.
     * @since Apache PDFBox 1.1.0
     */
    public static final COSInteger TWO = get(2); 

    /**
     * Constant for the number three.
     * @since Apache PDFBox 1.1.0
     */
    public static final COSInteger THREE = get(3); 

    /**
     * Returns a COSInteger instance with the given value.
     *
     * @param val integer value
     * @return COSInteger instance
     */
    public static COSInteger get(long val)
    {
        if (LOW <= val && val <= HIGH)
        {
            int index = (int) val - LOW;
            // no synchronization needed
            if (STATIC[index] == null)
            {
                STATIC[index] = new COSInteger(val);
            }
            return STATIC[index];
        }
        return new COSInteger(val);
    }

    private final long value;

    /**
     * @param val The integer value of this object.
     */
    private COSInteger( long val )
    {
        value = val;
    }

    @Override
    public boolean equals(Object o)
    {
        return o instanceof COSInteger && ((COSInteger)o).intValue() == intValue();
    }

    @Override
    public int hashCode()
    {
        return Long.hashCode(value);
    }

    @Override
    public String toString()
    {
        return "COSInt{" + value + "}";
    }

    @Override
    public float floatValue()
    {
        return value;
    }

    @Override
    public double doubleValue()
    {
        return value;
    }

    @Override
    public int intValue()
    {
        return (int)value;
    }

    @Override
    public long longValue()
    {
        return value;
    }

    @Override
    public void accept(COSVisitor visitor) throws IOException
    {
        visitor.visit(this);
    }
}
