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

import static org.sejda.commons.util.RequireUtils.requireArg;

/**
 * An xref entry for a cross reference stream which represent a compressed objects (i.e. an object part of an object
 * stream). See table 18 PDF32000:2008-1
 * 
 * @author Andrea Vacondio
 *
 */
public final class CompressedXrefEntry extends XrefEntry
{
    private long objectStreamNumber;
    private long index;

    private CompressedXrefEntry(XrefType type, long objectNumber, long byteOffset,
            int generationNumber, long objectStreamNumber, long index)
    {
        super(type, objectNumber, byteOffset, generationNumber);
        requireArg(objectStreamNumber >= 0, "Containing object stream number cannot be negative");
        this.objectStreamNumber = objectStreamNumber;
        this.index = index;
    }

    /**
     * @return The object number of the object stream in which this object is stored.
     */
    public long getObjectStreamNumber()
    {
        return objectStreamNumber;
    }

    @Override
    public byte[] toXrefStreamEntry(int secondFieldLength, int thirdFieldLength)
    {
        byte[] retVal = new byte[1 + secondFieldLength + thirdFieldLength];
        retVal[0] = 0b00000010;
        copyBytesTo(getObjectStreamNumber(), secondFieldLength, retVal, 1);
        copyBytesTo(index, thirdFieldLength, retVal, 1 + secondFieldLength);
        return retVal;
    }

    @Override
    public String toString()
    {
        return String.format("%s offset=%d objectStreamNumber=%d, %s", getType().toString(),
                getByteOffset(), objectStreamNumber, key().toString());
    }

    /**
     * Factory method for an entry in the xref stream representing a compressed object in an object stream
     * 
     * @param objectNumber
     * @param objectStreamNumber The object number of the object stream in which this object is stored.
     * @param index The index of this object within the object stream.
     * @return the newly created instance
     */
    public static CompressedXrefEntry compressedEntry(long objectNumber, long objectStreamNumber,
            long index)
    {
        return new CompressedXrefEntry(XrefType.COMPRESSED, objectNumber, UNKNOWN_OFFSET, 0,
                objectStreamNumber, index);
    }
}
