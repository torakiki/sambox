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
package org.sejda.sambox.output;

import static org.sejda.sambox.xref.XrefEntry.freeEntry;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Optional;

import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSInteger;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSStream;

/**
 * A {@link COSStream} that represent and xref stream as defined in Chap 7.5.8 of PDF 32000-1:2008
 * 
 * @author Andrea Vacondio
 */
class XrefStream extends COSStream
{

    /**
     * Creates an xref stream from the given dictionary. The stream will contain all the entries that have been written
     * using the given context
     * 
     * @param dictionary
     * @param context
     * @throws IOException
     */
    XrefStream(COSDictionary dictionary, PDFWriteContext context) throws IOException
    {
        super(dictionary);
        removeItem(COSName.PREV);
        removeItem(COSName.XREF_STM);
        removeItem(COSName.DOC_CHECKSUM);
        removeItem(COSName.DECODE_PARMS);
        removeItem(COSName.FILTER);
        removeItem(COSName.F_DECODE_PARMS);
        removeItem(COSName.F_FILTER);
        removeItem(COSName.F);
        removeItem(COSName.LENGTH);
        setName(COSName.TYPE, COSName.XREF.getName());
        setLong(COSName.SIZE, context.highestWritten().getObjectNumber() + 1);
        setItem(COSName.INDEX,
                new COSArray(COSInteger.get(context.lowestWritten().getObjectNumber()), COSInteger
                        .get(context.highestWritten().getObjectNumber()
                                - context.lowestWritten().getObjectNumber() + 1)));
        int secondFieldLength = sizeOf(context.highestWritten().getByteOffset());
        setItem(COSName.W, new COSArray(COSInteger.get(1), COSInteger.get(secondFieldLength),
                COSInteger.get(2)));
        try (OutputStream out = createUnfilteredStream())
        {
            for (long key = context.lowestWritten().getObjectNumber(); key <= context
                    .highestWritten().getObjectNumber(); key++)
            {
                out.write(Optional.ofNullable(context.getWritten(key)).orElse(freeEntry(key, 0))
                        .toXrefStreamEntry(secondFieldLength, 2));
            }
        }
        setLong(COSName.DL, getUnfilteredLength());
        addCompression();
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
