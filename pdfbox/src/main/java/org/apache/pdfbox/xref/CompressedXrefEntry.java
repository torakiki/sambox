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

    private CompressedXrefEntry(XrefType type, long objectNumber, long byteOffset,
            int generationNumber, long objectStreamNumber)
    {
        super(type, objectNumber, byteOffset, generationNumber);
        requireArg(objectStreamNumber >= 0, "Containing object stream number cannot be negative");
        this.objectStreamNumber = objectStreamNumber;
    }

    /**
     * @return The object number of the object stream in which this object is stored.
     */
    public long getObjectStreamNumber()
    {
        return objectStreamNumber;
    }

    @Override
    public String toString()
    {
        return String.format("%s offset=%d objectStreamNumber=%d %s", getType().toString(),
                getByteOffset(), objectStreamNumber, key().toString());
    }
    /**
     * Factory method for an entry in the xref stream representing a compressed object in an object stream
     * 
     * @param objectNumber
     * @param objectStreamNumber The object number of the object stream in which this object is stored.
     * @return the newly created instance
     */
    public static CompressedXrefEntry compressedEntry(long objectNumber, long objectStreamNumber)
    {
        return new CompressedXrefEntry(XrefType.COMPRESSED, objectNumber, UNKNOWN_OFFSET, 0,
                objectStreamNumber);
    }
}
