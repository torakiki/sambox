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

import static java.util.Objects.nonNull;
import static java.util.stream.LongStream.empty;
import static java.util.stream.LongStream.rangeClosed;
import static org.sejda.commons.util.RequireUtils.requireIOCondition;

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
 * Base class for an xref stream parser. Implementors will decide what to do when the parser finds a
 * new trailer or a new entries.
 *
 * @author Andrea Vacondio
 * @see AbstractXrefTableParser
 */
abstract class AbstractXrefStreamParser
{
    private static final Logger LOG = LoggerFactory.getLogger(AbstractXrefStreamParser.class);
    // Upper bound on objects in a single xref stream to prevent OOM via crafted /Index or /Size entries
    private static final int MAX_XREF_ENTRIES = 24_000_000;
    // PDF 32000-1 §7.5.4: maximum generation number is 65535.
    private static final long MAX_GENERATION = 65_535L;

    private final COSParser parser;

    AbstractXrefStreamParser(COSParser parser)
    {
        this.parser = parser;
    }

    /**
     * Action to perform when a trailer is found
     */
    abstract void onTrailerFound(COSDictionary trailer);

    /**
     * Action to perform when an {@link XrefEntry} is found
     */
    abstract void onEntryFound(XrefEntry entry);

    /**
     * Parse the xref object stream.
     *
     * @param streamObjectOffset xref stream object offset
     * @return the stream dictionary
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
            int size = xrefStream.getInt(COSName.SIZE);
            requireIOCondition(size > 0 && size < MAX_XREF_ENTRIES,
                    "Invalid xref /Size value " + size);
            objectNumbers = rangeClosed(0, size);
        }
        else
        {
            LOG.debug("Index found, now retrieving expected object numbers");
            Builder builder = LongStream.builder();
            for (int i = 0; i < index.size(); i += 2)
            {
                long start = index.getObject(i, COSNumber.class).longValue();
                long count = index.getObject(i + 1, COSNumber.class).longValue();
                requireIOCondition(count > 0 && count < MAX_XREF_ENTRIES,
                        "Invalid xref /Index count " + count);
                long end = start + Math.max(count - 1, 0);
                LOG.trace(String.format("Adding expected range from %d to %d", start, end));
                rangeClosed(start, end).forEach(builder::add);
            }
            objectNumbers = builder.build();
        }
        COSArray xrefFormat = xrefStream.getDictionaryObject(COSName.W, COSArray.class);
        requireIOCondition(nonNull(xrefFormat), "Invalid type for /W xref stream entry");
        requireIOCondition(xrefFormat.size() == 3, "Invalid /W array size");
        int w0 = xrefFormat.getInt(0);
        int w1 = xrefFormat.getInt(1);
        int w2 = xrefFormat.getInt(2);
        // we cap at 8 bytes, the bytes needed to decode a long
        requireIOCondition(w0 >= 0 && w0 <= 8 && w1 >= 1 && w1 <= 8 && w2 >= 0 && w2 <= 8,
                "Invalid /W widths [" + w0 + " " + w1 + " " + w2
                        + "], expected w0 in [0,8], w1 in [1,8], w2 in [0,8]");
        int lineSize = w0 + w1 + w2;
        try (InputStream stream = xrefStream.getUnfilteredStream())
        {
            OfLong objectIds = objectNumbers.iterator();
            byte[] currLine = new byte[lineSize];
            while (objectIds.hasNext())
            {
                long objectId = objectIds.nextLong();
                int bytesRead = stream.readNBytes(currLine, 0, lineSize);
                if (bytesRead < lineSize)
                {
                    LOG.warn("Xref stream ended prematurely at object {}", objectId);
                    break;
                }
                // PDF 32000-1 Table 17: when /W[0] is 0, the type field is omitted and defaults to 1 (in-use).
                long type = (w0 == 0) ? 1 : decodeField(currLine, 0, w0);
                long field1 = decodeField(currLine, w0, w1);
                long field2 = decodeField(currLine, w0 + w1, w2);
                if (type == 0 && field2 <= MAX_GENERATION)
                {
                    onEntryFound(XrefEntry.freeEntry(objectId, (int) field2));
                }
                else if (type == 1 && field2 <= MAX_GENERATION)
                {
                    onEntryFound(XrefEntry.inUseEntry(objectId, field1, (int) field2));
                }
                else if (type == 2)
                {
                    onEntryFound(CompressedXrefEntry.compressedEntry(objectId, field1, field2));
                }
                else
                {
                    LOG.warn("Discarding xref entry at object {}: type={}, field1={}, field2={}",
                            objectId, type, field1, field2);
                }
            }
        }
    }

    private static long decodeField(byte[] data, int start, int length)
    {
        long value = 0L;
        for (int i = 0; i < length; i++)
        {
            value |= ((long) data[start + i] & 0xff) << ((length - i - 1) * 8);
        }
        return value;
    }

    COSParser parser()
    {
        return parser;
    }
}
