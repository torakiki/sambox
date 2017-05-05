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

import static org.sejda.sambox.cos.DirectCOSObject.asDirectObject;
import static org.sejda.sambox.xref.XrefEntry.freeEntry;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSInteger;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSStream;
import org.sejda.sambox.cos.DirectCOSObject;

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
        setItem(COSName.TYPE, COSName.XREF);
        setItem(COSName.SIZE, asDirect(context.highestObjectNumber() + 1));
        COSArray index = new COSArray();
        for (List<Long> continuos : context.getWrittenContiguousGroups())
        {
            index.add(asDirect(continuos.get(0)));
            index.add(asDirect(continuos.size()));
        }
        setItem(COSName.INDEX, asDirectObject(index));
        int secondFieldLength = sizeOf(context.highestWritten().getByteOffset());
        setItem(COSName.W, asDirectObject(
                new COSArray(asDirect(1), asDirect(secondFieldLength), asDirect(2))));
        try (OutputStream out = createUnfilteredStream())
        {
            for (List<Long> continuos : context.getWrittenContiguousGroups())
            {
                for (long key : continuos)
                {
                    out.write(Optional.ofNullable(context.getWritten(key)).orElse(freeEntry(key, 0))
                            .toXrefStreamEntry(secondFieldLength, 2));
                }
            }
        }
        setItem(COSName.DL, asDirect(getUnfilteredLength()));
        setItem(COSName.FILTER, asDirectObject(COSName.FLATE_DECODE));
    }

    private static DirectCOSObject asDirect(long num)
    {
        return asDirectObject(COSInteger.get(num));
    }

    @Override
    public boolean encryptable()
    {
        return false;
    }

    @Override
    public void encryptable(boolean encryptable)
    {
        // do nothing
    }

    @Override
    public void setEncryptor(Function<InputStream, InputStream> encryptor)
    {
        // do nothing
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
