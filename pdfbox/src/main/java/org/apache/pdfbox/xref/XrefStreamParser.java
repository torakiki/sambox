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

import static java.util.stream.LongStream.concat;
import static java.util.stream.LongStream.empty;
import static java.util.stream.LongStream.rangeClosed;

import java.io.IOException;
import java.io.InputStream;
import java.util.PrimitiveIterator.OfLong;
import java.util.stream.LongStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSNumber;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.input.BaseCOSParser;

/**
 * @author Andrea Vacondio
 *
 */
class XrefStreamParser
{
    private static final Log LOG = LogFactory.getLog(XrefStreamParser.class);

    private TrailerMerger trailerMerger;
    private BaseCOSParser parser;

    XrefStreamParser(BaseCOSParser parser, TrailerMerger trailerMerger)
    {
        this.parser = parser;
        this.trailerMerger = trailerMerger;
    }

    /**
     * Parse the xref object stream.
     * 
     * @param streamObjectOffset xref stream object offset
     * @return the stream dictionary
     * @throws IOException
     */
    public COSDictionary parse(long streamObjectOffset) throws IOException
    {
        LOG.debug("Parsing xref stream at offset " + streamObjectOffset);
        parser.offset(streamObjectOffset);
        parser.skipIndirectObjectDefinition();
        parser.skipSpaces();

        COSDictionary dictionary = parser.nextDictionary();
        try (COSStream xrefStream = parser.nextStream(dictionary))
        {
            trailerMerger.mergeTrailerWithoutOverwriting(streamObjectOffset, dictionary);
            parseStream(xrefStream);
        }
        LOG.debug("Done parsing xref stream");
        return dictionary;
    }

    private void parseStream(COSStream xrefStream) throws IOException
    {
        LongStream objectNumbers = empty();
        COSArray index = (COSArray) xrefStream.getDictionaryObject(COSName.INDEX);

        if (index == null)
        {
            LOG.debug("No index found for xref stream, using default values");
            objectNumbers = rangeClosed(0, xrefStream.getInt(COSName.SIZE));
        }
        else
        {
            LOG.debug("Index found, now retrieving expected object numbers");
            for (int i = 0; i < index.size(); i += 2)
            {
                long start = ((COSNumber) index.get(i)).longValue();
                long end = start + ((COSNumber) index.get(i + 1)).longValue();
                LOG.trace(String.format("Adding expected range from %d to %d", start, end));
                objectNumbers = concat(objectNumbers, rangeClosed(start, end));
            }

        }
        COSArray xrefFormat = (COSArray) xrefStream.getDictionaryObject(COSName.W);
        int w0 = xrefFormat.getInt(0);
        int w1 = xrefFormat.getInt(1);
        int w2 = xrefFormat.getInt(2);
        int lineSize = w0 + w1 + w2;

        try (InputStream stream = xrefStream.getUnfilteredStream())
        {
            OfLong objectIds = objectNumbers.iterator();
            while (stream.available() > 0 && objectIds.hasNext())
            {

                Long objectId = objectIds.next();
                byte[] currLine = new byte[lineSize];
                stream.read(currLine);

                int type = 0;
                int i = 0;
                /*
                 * Grabs the number of bytes specified for the first column in the W array and stores it.
                 */
                for (i = 0; i < w0; i++)
                {
                    type += (currLine[i] & 0x00ff) << ((w0 - i - 1) * 8);
                }
                int field1 = 0;
                for (i = 0; i < w1; i++)
                {
                    field1 += (currLine[i + w0] & 0x00ff) << ((w1 - i - 1) * 8);
                }
                int field2 = 0;
                for (i = 0; i < w2; i++)
                {
                    field2 += (currLine[i + w0 + w1] & 0x00ff) << ((w2 - i - 1) * 8);
                }
                switch (type)
                {
                case 0:
                    // xref.add(XrefEntry.freeEntry(objectId, field1, field2));
                    break;
                case 1:
                    parser.provider().addEntry(XrefEntry.inUseEntry(objectId, field1, field2));
                    break;
                case 2:
                    parser.provider().addEntry(
                            CompressedXrefEntry.compressedEntry(objectId, field1));
                    break;
                default:
                    break;
                }
            }

        }
    }
}
