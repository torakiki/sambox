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
package org.apache.pdfbox.xref;

import static org.apache.pdfbox.util.RequireUtils.requireArg;

import org.apache.pdfbox.cos.COSObjectKey;

/**
 * Entry of the xref table or stream
 * 
 * @author Andrea Vacondio
 *
 */
public class XrefEntry
{
    public static final int MAX_GENERATION_NUMBER = 65535;
    public static final long UNKNOWN_OFFSET = -1;

    private XrefType type;
    private COSObjectKey key;
    private long byteOffset;

    XrefEntry(XrefType type, long objectNumber, long byteOffset, int generationNumber)
    {
        requireArg(objectNumber >= 0 && generationNumber >= 0,
                "Object number and generation number cannot be negative");
        this.type = type;
        this.key = new COSObjectKey(objectNumber, generationNumber);
        this.byteOffset = byteOffset;
    }

    public XrefType getType()
    {
        return type;
    }

    public long getByteOffset()
    {
        return byteOffset;
    }

    public void setByteOffset(long byteOffset)
    {
        this.byteOffset = byteOffset;
    }

    public long getObjectNumber()
    {
        return key.getNumber();
    }

    public int getGenerationNumber()
    {
        return key.getGeneration();
    }

    public boolean isUnknownOffset()
    {
        return this.byteOffset <= UNKNOWN_OFFSET;
    }

    public COSObjectKey key()
    {
        return key;
    }

    /**
     * @param entry
     * @return true if the given input entry is part of an object stream and this is the entry representing that object
     * stream.
     */
    public boolean owns(XrefEntry entry)
    {
        return entry != null && entry.getType() == XrefType.COMPRESSED
                && key.getNumber() == ((CompressedXrefEntry) entry).getObjectStreamNumber();
    }

    @Override
    public String toString()
    {
        return String.format("%s offset=%d, %s", type.toString(), byteOffset, key.toString());
    }

    /**
     * Factory method for an in use xref table/stream entry
     * 
     * @param objectNumber
     * @param byteOffset
     * @param generationNumber
     * @return the newly created instance
     */
    public static XrefEntry inUseEntry(long objectNumber, long byteOffset, int generationNumber)
    {
        return new XrefEntry(XrefType.IN_USE, objectNumber, byteOffset, generationNumber);
    }

    /**
     * Factory method for a free xref tabe/stream entry
     * 
     * @param objectNumber
     * @param byteOffset
     * @param generationNumber
     * @return the newly created instance
     */
    public static XrefEntry freeEntry(long objectNumber, long byteOffset, int generationNumber)
    {
        return new XrefEntry(XrefType.FREE, objectNumber, byteOffset, generationNumber);
    }
}
