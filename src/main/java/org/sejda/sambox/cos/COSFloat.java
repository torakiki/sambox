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
package org.sejda.sambox.cos;

import static org.sejda.commons.util.RequireUtils.requireIOCondition;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * This class represents a floating point number in a PDF document.
 *
 * @author Ben Litchfield
 * 
 */
public class COSFloat extends COSNumber
{
    private float value;
    private static final Pattern DOTS = Pattern.compile("\\.");
    private static final Pattern EXP_END = Pattern.compile("[e|E]$");
    private static final Pattern NUM1 = Pattern.compile("^(-)([-|+]+)\\d+\\.\\d+");
    private static final Pattern NUM2 = Pattern.compile("^(-)([\\-|+]+)");
    private static final Pattern NUM3 = Pattern.compile("^0\\.0*-\\d+");
    private static final Pattern ZERO = Pattern.compile("^0-(\\.|\\d+)*");
    private static final Pattern MINUS = Pattern.compile("-");

    public COSFloat(float value)
    {
        this.value = value;
    }

    /**
     * @param aFloat The primitive float object that this object wraps.
     * @throws IOException If aFloat is not a float.
     */
    public COSFloat(String aFloat) throws IOException
    {
        try
        {
            // no pre-processing! speed optimized for the vanilla scenario where we parse good floats
            // if we encounter a problem, then we start handling edge cases, not before
            value = Float.parseFloat(aFloat);
        }
        catch (NumberFormatException e)
        {
            try
            {
                int dot = aFloat.indexOf('.');
                if (dot != aFloat.lastIndexOf('.'))
                {
                    // 415.75.795 we replace additional dots with 0
                    aFloat = aFloat.substring(0, dot + 1) + DOTS.matcher(aFloat.substring(dot + 1))
                            .replaceAll("0");

                }
                aFloat = EXP_END.matcher(aFloat).replaceAll("");
                value = Float.parseFloat(aFloat);
            }
            catch (NumberFormatException nfex)
            {
                try
                {
                    if (NUM1.matcher(aFloat).matches())
                    {
                        // PDFBOX-3589 --242.0
                        value = Float.parseFloat(NUM2.matcher(aFloat).replaceFirst("-"));
                    }
                    else if (ZERO.matcher(aFloat).matches())
                    {
                        // SAMBox 75
                        value = 0f;
                    }
                    else
                    {
                        // PDFBOX-2990 has 0.00000-33917698
                        // PDFBOX-3369 has 0.00-35095424
                        // PDFBOX-3500 has 0.-262
                        requireIOCondition(NUM3.matcher(aFloat).matches(),
                                "Expected floating point number but found '" + aFloat + "'");
                        value = Float.parseFloat("-" + MINUS.matcher(aFloat).replaceFirst(""));
                    }
                }
                catch (NumberFormatException e2)
                {
                    throw new IOException(
                            "Error expected floating point number actual='" + aFloat + "'", e2);
                }
            }
        }
        value = coerce(value);
    }

    private float coerce(float floatValue)
    {
        if (floatValue == Float.POSITIVE_INFINITY)
        {
            return Float.MAX_VALUE;
        }
        if (floatValue == Float.NEGATIVE_INFINITY)
        {
            return -Float.MAX_VALUE;
        }
        if (Math.abs(floatValue) < Float.MIN_NORMAL)
        {
            // values smaller than the smallest possible float value are converted to 0
            // see PDF spec, chapter 2 of Appendix C Implementation Limits
            return 0f;
        }
        return floatValue;
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
    public long longValue()
    {
        return (long) value;
    }

    @Override
    public int intValue()
    {
        return (int) value;
    }

    @Override
    public boolean equals(Object o)
    {
        return o instanceof COSFloat
                && Float.floatToIntBits(((COSFloat) o).value) == Float.floatToIntBits(value);
    }

    @Override
    public int hashCode()
    {
        return Float.hashCode(value);
    }

    @Override
    public String toString()
    {
        var s = String.valueOf(value);
        if (s.indexOf('E') < 0)
        {
            return s;
        }
        return new BigDecimal(s).stripTrailingZeros().toPlainString();
    }

    @Override
    public void accept(COSVisitor visitor) throws IOException
    {
        visitor.visit(this);
    }
}
