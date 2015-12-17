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

import static java.util.Comparator.comparing;

import java.util.Objects;

/**
 * Object representing a key to identify a PDF object
 *
 * @author Michael Traut
 */
public final class COSObjectKey implements Comparable<COSObjectKey>
{
    private final long number;
    private final int generation;

    /**
     * @param number The object number.
     * @param generation The object generation number.
     */
    public COSObjectKey(long number, int generation)
    {
        this.number = number;
        this.generation = generation;
    }

    /**
     * @return The objects generation number.
     */
    public int generation()
    {
        return generation;
    }

    /**
     * @return The object number
     */
    public long objectNumber()
    {
        return number;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (!(obj instanceof COSObjectKey))
        {
            return false;
        }
        COSObjectKey other = (COSObjectKey) obj;

        return (number == other.number && generation == other.generation);
    }

    @Override
    public int hashCode()
    {
        return Long.hashCode(number + generation);
    }

    @Override
    public String toString()
    {
        return String.format("on=%d, gn=%d", number, generation);
    }

    @Override
    public int compareTo(COSObjectKey other)
    {
        return Objects.compare(this, other,
                comparing(COSObjectKey::objectNumber).thenComparingInt(k -> k.generation()));
    }
}
