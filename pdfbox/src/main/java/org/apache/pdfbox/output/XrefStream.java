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
package org.apache.pdfbox.output;

import static org.apache.pdfbox.cos.DirectCOSObject.asDirectObject;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Optional;
import java.util.TreeMap;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSInteger;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.xref.XrefEntry;

/**
 * @author Andrea Vacondio
 *
 */
class XrefStream extends COSStream
{

    /**
     * Assumes the last entry is the one with highest offset
     * 
     * @param dictionary
     * @param entries
     * @throws IOException
     */
    XrefStream(COSDictionary dictionary, TreeMap<Long, XrefEntry> entries) throws IOException
    {
        super(dictionary);
        removeItem(COSName.PREV);
        removeItem(COSName.XREF_STM);
        removeItem(COSName.DOC_CHECKSUM);
        removeItem(COSName.DECODE_PARMS);
        removeItem(COSName.F_DECODE_PARMS);
        removeItem(COSName.F_FILTER);
        removeItem(COSName.F);
        setName(COSName.TYPE, COSName.XREF.getName());
        setLong(COSName.SIZE, entries.lastKey() + 1);
        setItem(COSName.INDEX, asDirectObject(new COSArray(COSInteger.get(entries.firstKey()),
                COSInteger.get(entries.lastKey() - entries.firstKey() + 1))));
        int secondFieldLength = sizeOf(entries.lastEntry().getValue().getByteOffset());
        setItem(COSName.W,
                asDirectObject(new COSArray(COSInteger.get(1), COSInteger.get(secondFieldLength),
                        COSInteger.get(2))));
        try (OutputStream out = createUnfilteredStream())
        {
            for (long key = entries.firstKey(); key <= entries.lastKey(); key++)
            {
                out.write(Optional.ofNullable(entries.get(key))
                        .orElse(XrefEntry.DEFAULT_FREE_ENTRY)
                        .toXrefStreamEntry(secondFieldLength, 2));
            }
        }
        setLong(COSName.DL, getUnfilteredLength());
        setFilters(COSName.FLATE_DECODE);
    }

    private static int sizeOf(long number)
    {
        int size = 0;
        while (number > 0)
        {
            size++;
            number >>= 8;
        }
        return size;
    }

}
