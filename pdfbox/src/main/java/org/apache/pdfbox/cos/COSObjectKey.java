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

/**
 * Object representing the physical reference to an indirect pdf object.
 *
 * @author Michael Traut
 * @version $Revision: 1.5 $
 */
public class COSObjectKey implements Comparable<COSObjectKey>
{
    private final long number;
    private final int generation;

    /**
     * @param num The object number.
     * @param gen The object generation number.
     */
    public COSObjectKey(long num, int gen)
    {
        number = num;
        generation = gen;
    }

    /**
     * @return The objects generation number.
     */
    public int getGeneration()
    {
        return generation;
    }

    /**
     * @return The object's id.
     */
    public long getNumber()
    {
        return number;
    }

    @Override
    public boolean equals(Object obj)
    {
        return (obj instanceof COSObjectKey) && ((COSObjectKey) obj).getNumber() == getNumber()
                && ((COSObjectKey) obj).getGeneration() == getGeneration();
    }

    @Override
    public int hashCode()
    {
        return Long.valueOf(number + generation).hashCode();
    }

    @Override
    public String toString()
    {
        return Long.toString(number) + " " + Integer.toString(generation) + " R";
    }

    @Override
    public int compareTo(COSObjectKey other)
    {
        int numCompare = Long.compare(number, other.getNumber());
        if (numCompare == 0)
        {
            return Integer.compare(generation, other.getGeneration());
        }
        return numCompare;

    }

}
