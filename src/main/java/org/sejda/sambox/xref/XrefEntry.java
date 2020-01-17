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
package org.sejda.sambox.xref;

import java.util.Locale;

import org.sejda.sambox.cos.COSObjectKey;

/**
 * Entry of the xref table or stream
 * 
 * @author Andrea Vacondio
 */
public class XrefEntry
{
    private static final String XREFTABLE_ENTRY_FORMAT = "%010d %05d %c\r\n";

    public static final XrefEntry DEFAULT_FREE_ENTRY = freeEntry(0, 65535);
    public static final long UNKNOWN_OFFSET = -1;

    private XrefType type;
    private COSObjectKey key;
    private long byteOffset;

    XrefEntry(XrefType type, long objectNumber, long byteOffset, int generationNumber)
    {
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
        return key.objectNumber();
    }

    public int getGenerationNumber()
    {
        return key.generation();
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
                && key.objectNumber() == ((CompressedXrefEntry) entry).getObjectStreamNumber();
    }

    @Override
    public String toString()
    {
        return String.format("%s offset=%d, %s", type.toString(), byteOffset, key.toString());
    }

    /**
     * @return a xref table line corresponding to this entry
     * @throws IllegalArgumentException if the entry is a compressed one
     */
    public String toXrefTableEntry()
    {
        switch (type)
        {
        case IN_USE:
            return String.format(Locale.US, XREFTABLE_ENTRY_FORMAT, getByteOffset(),
                    getGenerationNumber(), 'n');
        case FREE:
            return String.format(Locale.US, XREFTABLE_ENTRY_FORMAT, getObjectNumber(),
                    getGenerationNumber(), 'f');
        default:
            throw new IllegalArgumentException(
                    "Only in_use and free entries can be written to an xref table");
        }
    }

    /**
     * Creates Cross-reference stream data for this entry as defined in Chap 7.5.8.3 of PDF32000-1:2008, table 18.
     * 
     * @param secondFieldLength length of the second field
     * @param thirdFieldLength length of the second field
     * @return an entry corresponding to this xref entry to be used in the xref stream.
     */
    public byte[] toXrefStreamEntry(int secondFieldLength, int thirdFieldLength)
    {
        byte[] retVal = new byte[1 + secondFieldLength + thirdFieldLength];
        if (type == XrefType.FREE)
        {
            retVal[0] = 0b00000000;
            copyBytesTo(key.objectNumber(), secondFieldLength, retVal, 1);
            copyBytesTo(key.generation(), thirdFieldLength, retVal, 1 + secondFieldLength);
            return retVal;
        }
        retVal[0] = 0b00000001;
        copyBytesTo(byteOffset, secondFieldLength, retVal, 1);
        copyBytesTo(key.generation(), thirdFieldLength, retVal, 1 + secondFieldLength);
        return retVal;
    }

    protected void copyBytesTo(long data, int length, byte[] destination, int destinationIndex)
    {
        for (int i = 0; i < length; i++)
        {
            destination[length + destinationIndex - i - 1] = (byte) (data & 0xFF);
            data >>= 8;
        }
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
     * Factory method for an in use xref table/stream entry with unknown offset
     * 
     * @param objectNumber
     * @param generationNumber
     * @return the newly created instance
     */
    public static XrefEntry unknownOffsetEntry(long objectNumber, int generationNumber)
    {
        return new XrefEntry(XrefType.IN_USE, objectNumber, UNKNOWN_OFFSET, generationNumber);
    }

    /**
     * Factory method for a free xref tabe/stream entry
     * 
     * @param objectNumber
     * @param generationNumber
     * @return the newly created instance
     */
    public static XrefEntry freeEntry(long objectNumber, int generationNumber)
    {
        return new XrefEntry(XrefType.FREE, objectNumber, UNKNOWN_OFFSET, generationNumber);
    }
}
