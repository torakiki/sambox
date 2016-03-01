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
package org.sejda.sambox.input;

import static java.util.stream.LongStream.empty;
import static java.util.stream.LongStream.rangeClosed;

import java.io.IOException;
import java.io.InputStream;
import java.util.PrimitiveIterator.OfLong;
import java.util.stream.LongStream;
import java.util.stream.LongStream.Builder;

import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSNumber;
import org.sejda.sambox.cos.COSStream;
import org.sejda.sambox.xref.CompressedXrefEntry;
import org.sejda.sambox.xref.XrefEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for an xref stream parser. Implementors will decide what to do when the parser finds a new trailer or a
 * new entries.
 * 
 * @author Andrea Vacondio
 * @see AbstractXrefTableParser
 */
abstract class AbstractXrefStreamParser
{
    private static final Logger LOG = LoggerFactory.getLogger(AbstractXrefStreamParser.class);

    private COSParser parser;

    AbstractXrefStreamParser(COSParser parser)
    {
        this.parser = parser;
    }

    /**
     * Action to perform when a trailer is found
     * 
     * @param trailer
     */
    abstract void onTrailerFound(COSDictionary trailer);

    /**
     * Action to perform when an {@link XrefEntry} is found
     * 
     * @param entry
     */
    abstract void onEntryFound(XrefEntry entry);

    /**
     * Parse the xref object stream.
     * 
     * @param streamObjectOffset xref stream object offset
     * @return the stream dictionary
     * @throws IOException
     */
    COSDictionary parse(long streamObjectOffset) throws IOException
    {
        LOG.debug("Parsing xref stream at offset " + streamObjectOffset);
        parser.position(streamObjectOffset);
        parser.skipIndirectObjectDefinition();
        parser.skipSpaces();

        COSDictionary dictionary = parser.nextDictionary();
        try (COSStream xrefStream = parser.nextStream(dictionary))
        {
            onTrailerFound(dictionary);
            parseStream(xrefStream);
        }
        LOG.debug("Done parsing xref stream");
        return dictionary;
    }

    void parseStream(COSStream xrefStream) throws IOException
    {
        LongStream objectNumbers = empty();
        COSArray index = xrefStream.getDictionaryObject(COSName.INDEX, COSArray.class);

        if (index == null)
        {
            LOG.debug("No index found for xref stream, using default values");
            objectNumbers = rangeClosed(0, xrefStream.getInt(COSName.SIZE));
        }
        else
        {
            LOG.debug("Index found, now retrieving expected object numbers");
            Builder builder = LongStream.builder();
            for (int i = 0; i < index.size(); i += 2)
            {
                long start = ((COSNumber) index.get(i)).longValue();
                long end = start + Math.max(((COSNumber) index.get(i + 1)).longValue() - 1, 0);
                LOG.trace(String.format("Adding expected range from %d to %d", start, end));
                rangeClosed(start, end).forEach(builder::add);
            }
            objectNumbers = builder.build();

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
                int type = (w0 == 0) ? 1 : 0;
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
                    onEntryFound(XrefEntry.freeEntry(objectId, field2));
                    break;
                case 1:
                    onEntryFound(XrefEntry.inUseEntry(objectId, field1, field2));
                    break;
                case 2:
                    onEntryFound(CompressedXrefEntry.compressedEntry(objectId, field1, field2));
                    break;
                default:
                    LOG.warn("Unknown xref entry type " + type);
                    break;
                }
            }
        }
    }

    COSParser parser()
    {
        return parser;
    }
}
