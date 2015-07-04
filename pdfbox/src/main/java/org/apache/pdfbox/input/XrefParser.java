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
package org.apache.pdfbox.input;

import static org.apache.pdfbox.input.AbstractXrefTableParser.XREF;
import static org.sejda.util.RequireUtils.requireIOCondition;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.util.Charsets;
import org.apache.pdfbox.xref.XrefEntry;
/**
 * @author Andrea Vacondio
 *
 */
class XrefParser
{
    private static final Log LOG = LogFactory.getLog(XrefParser.class);
    /**
     * How many trailing bytes to read for EOF marker.
     */
    private static final int DEFAULT_TRAIL_BYTECOUNT = 2048;
    private static final String STARTXREF = "startxref";

    private COSDictionary trailer = new COSDictionary();
    private AbstractXrefStreamParser xrefStreamParser;
    private AbstractXrefTableParser xrefTableParser;
    private BaseCOSParser parser;

    public XrefParser(BaseCOSParser parser)
    {
        this.parser = parser;
        this.xrefStreamParser = new AbstractXrefStreamParser(parser)
        {
            @Override
            void onTrailerFound(COSDictionary found)
            {
                trailer.mergeWithoutOverwriting(found);
            }

            @Override
            void onEntryFound(XrefEntry entry)
            {
                parser().provider().addEntryIfAbsent(entry);
            }
        };

        this.xrefTableParser = new AbstractXrefTableParser(parser)
        {
            @Override
            void onTrailerFound(COSDictionary found)
            {
                trailer.mergeWithoutOverwriting(found);
            }

            @Override
            void onEntryFound(XrefEntry entry)
            {
                parser().provider().addEntryIfAbsent(entry);
            }
        };
    }

    /**
     * parse the xref using the given parser.
     * 
     * @throws IOException
     */
    public void parse() throws IOException
    {
        long xrefOffset = findXrefOffset();
        if (xrefOffset <= 0 || !parseXref(xrefOffset))
        {
            XrefFullScanner fallbackFullScanner = new XrefFullScanner(parser);
            fallbackFullScanner.scan();
            this.trailer = fallbackFullScanner.trailer();
        }
    }

    /**
     * Looks for the startxref keyword within the latest {@link #DEFAULT_TRAIL_BYTECOUNT} bytes of the source. If found
     * it returns the Long read after the keyword, if not it returns -1.
     * 
     * @return the xref offset or -1 if the startxref keyword is not found
     * @throws IOException If something went wrong.
     */
    private final long findXrefOffset() throws IOException
    {
        int chunkSize = (int) Math.min(parser.length(), DEFAULT_TRAIL_BYTECOUNT);
        long startPosition = parser.length() - chunkSize;
        parser.position(startPosition);
        byte[] buffer = new byte[chunkSize];
        parser.source().read(ByteBuffer.wrap(buffer));
        int relativeIndex = new String(buffer, Charsets.ISO_8859_1).lastIndexOf(STARTXREF);
        if (relativeIndex < 0)
        {
            LOG.warn("Unable to find 'startxref' keyword");
            return -1;
        }
        parser.position(startPosition + relativeIndex + STARTXREF.length());
        parser.skipSpaces();
        long xrefOffset = parser.readLong();
        LOG.debug("Found xref offset at " + xrefOffset);
        return xrefOffset;
    }

    private boolean parseXref(long xrefOffset) throws IOException
    {
        if (isValidXrefOffset(xrefOffset))
        {
            requireIOCondition(xrefOffset > 0, "Unable to find correct xref table or stream offset");
            while (xrefOffset > -1)
            {
                if (!isValidXrefOffset(xrefOffset))
                {
                    LOG.warn("Offset '"
                            + xrefOffset
                            + "' doesn't point to an xref table or stream, applying fallback strategy");
                    return false;
                }
                parser.position(xrefOffset);
                parser.skipSpaces();

                if (parser.isNextToken(XREF))
                {
                    COSDictionary trailer = xrefTableParser.parse(xrefOffset);
                    long streamOffset = trailer.getLong(COSName.XREF_STM);
                    if (streamOffset > 0)
                    {
                        if (!isValidXrefStreamOffset(streamOffset))
                        {
                            LOG.warn("Offset '"
                                    + streamOffset
                                    + "' doesn't point to an xref stream, applying fallback strategy");
                            return false;

                        }
                        if (streamOffset > 0)
                        {
                            trailer.setLong(COSName.XREF_STM, streamOffset);
                            xrefStreamParser.parse(streamOffset);
                        }
                    }
                    xrefOffset = trailer.getLong(COSName.PREV);
                }
                else
                {
                    COSDictionary streamDictionary = xrefStreamParser.parse(xrefOffset);
                    xrefOffset = streamDictionary.getLong(COSName.PREV);
                }
            }
            return true;
        }
        LOG.warn("Offset '" + xrefOffset
                + "' doesn't point to an xref table or stream, applying fallback strategy");
        return false;
    }

    /**
     * @param xrefOffset
     * @return true if the given offset points to an xref table or and xref stream
     * @throws IOException
     */
    private boolean isValidXrefOffset(long xrefOffset) throws IOException
    {
        if (isValidXrefStreamOffset(xrefOffset))
        {
            return true;
        }
        parser.position(xrefOffset);
        return parser.isNextToken(XREF);
    }

    /**
     * @param xrefStreamOffset
     * @return true if the given offset points to a valid xref stream
     * @throws IOException
     */
    private boolean isValidXrefStreamOffset(long xrefStreamOffset) throws IOException
    {
        parser.position(xrefStreamOffset);
        try
        {
            parser.skipIndirectObjectDefinition();
        }
        catch (IOException exception)
        {
            return false;
        }
        parser.position(xrefStreamOffset);
        return true;
    }

    public COSDictionary trailer()
    {
        return this.trailer;
    }

}
