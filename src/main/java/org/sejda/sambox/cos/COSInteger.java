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

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class represents an integer number in a PDF document.
 *
 * @author Ben Litchfield
 */
public final class COSInteger extends COSNumber
{

    private static final Map<Long, COSInteger> CACHE = new ConcurrentHashMap<>();

    public static final COSInteger ZERO = get(0);
    public static final COSInteger ONE = get(1);
    public static final COSInteger TWO = get(2);
    public static final COSInteger THREE = get(3);

    /**
     * Factory method for a COSInteger instance with the given value.
     *
     * @param val integer value
     * @return COSInteger instance
     */
    public static COSInteger get(long key)
    {
        COSInteger value = CACHE.get(key);
        if (value == null)
        {
            final COSInteger newVal = new COSInteger(key);
            value = CACHE.putIfAbsent(key, newVal);
            if (value == null)
            {
                value = newVal;
            }
        }
        return value;
    }

    private final long value;

    /**
     * creates a COSInteger that is not cached
     */
    public COSInteger(long value)
    {
        this.value = value;
    }

    @Override
    public boolean equals(Object o)
    {
        return o instanceof COSInteger && ((COSInteger) o).intValue() == intValue();
    }

    @Override
    public int hashCode()
    {
        return Long.hashCode(value);
    }

    @Override
    public String toString()
    {
        return Long.toString(value);
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
        return (int) value;
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
