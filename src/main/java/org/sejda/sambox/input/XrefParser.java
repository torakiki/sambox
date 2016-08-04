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

import static org.sejda.sambox.input.AbstractXrefTableParser.TRAILER;
import static org.sejda.sambox.input.AbstractXrefTableParser.XREF;
import static org.sejda.util.RequireUtils.requireIOCondition;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.input.XrefFullScanner.XrefScanOutcome;
import org.sejda.sambox.xref.XrefEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Component responsible for finding and parsing the xref chain (either tables and streams). In case of errors while
 * parsing the xref chain (Ex. invalid offset, bad dictionaries etc) it has a fallback mechanism performing a document
 * full scan searching for xrefs. When parsing the document, xref info are passed to the {@link COSParser} which will
 * use them to retrieve COS objects on demand.
 * 
 * @author Andrea Vacondio
 * @see XrefFullScanner
 */
class XrefParser
{
    private static final Logger LOG = LoggerFactory.getLogger(XrefParser.class);
    /**
     * How many trailing bytes to read for EOF marker.
     */
    private static final int DEFAULT_TRAIL_BYTECOUNT = 2048;
    private static final String STARTXREF = "startxref";

    private COSDictionary trailer = new COSDictionary();
    private AbstractXrefStreamParser xrefStreamParser;
    private AbstractXrefTableParser xrefTableParser;
    private COSParser parser;

    public XrefParser(COSParser parser)
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
            XrefScanOutcome xrefScanStatus = fallbackFullScanner.scan();
            if (xrefScanStatus != XrefScanOutcome.NOT_FOUND)
            {
                // something was found so we keep the trailer
                this.trailer = fallbackFullScanner.trailer();
            }
            if (xrefScanStatus != XrefScanOutcome.FOUND)
            {
                // there were errors in the found xrefs so we perform objects full scan
                LOG.warn(
                        "Xref full scan encountered some errors, now performing objects full scan");
                ObjectsFullScanner objectsFullScanner = new ObjectsFullScanner(parser)
                {
                    @Override
                    protected void onNonObjectDefinitionLine(long offset, String line)
                            throws IOException
                    {
                        if (line != null && line.startsWith(TRAILER))
                        {
                            LOG.debug("Parsing trailer at " + offset);
                            parser.position(offset);
                            parser.skipExpected(TRAILER);
                            parser.skipSpaces();
                            trailer.merge(parser.nextDictionary());
                            parser.skipSpaces();
                        }
                    }
                };
                // and we consider it scan more reliable compared to what was found in the somehow broken xrefs
                objectsFullScanner.entries().values().stream().forEach(parser.provider()::addEntry);
            }
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
        int relativeIndex = new String(buffer, StandardCharsets.ISO_8859_1).lastIndexOf(STARTXREF);
        if (relativeIndex < 0)
        {
            LOG.warn("Unable to find 'startxref' keyword");
            return -1;
        }
        try
        {
            parser.position(startPosition + relativeIndex + STARTXREF.length());
            parser.skipSpaces();
            long xrefOffset = parser.readLong();
            LOG.debug("Found xref offset at " + xrefOffset);
            return xrefOffset;
        }
        catch (IOException e)
        {
            LOG.warn("An error occurred while parsing the xref offset", e);
            return -1;
        }
    }

    private boolean parseXref(long xrefOffset)
    {
        try
        {
            return doParseXref(xrefOffset);
        }
        catch (IOException e)
        {
            LOG.warn("An error occurred while parsing the xref, applying fallback strategy", e);
            return false;
        }
    }

    private boolean doParseXref(long xrefOffset) throws IOException
    {

        requireIOCondition(isValidXrefOffset(xrefOffset),
                "Offset '" + xrefOffset + "' doesn't point to an xref table or stream");

        Set<Long> parsedOffsets = new HashSet<>();
        while (xrefOffset > -1)
        {
            requireIOCondition(!parsedOffsets.contains(xrefOffset), "/Prev loop detected");
            requireIOCondition(isValidXrefOffset(xrefOffset),
                    "Offset '" + xrefOffset + "' doesn't point to an xref table or stream");

            parser.position(xrefOffset);
            parser.skipSpaces();

            if (parser.isNextToken(XREF))
            {
                COSDictionary trailer = xrefTableParser.parse(xrefOffset);
                parsedOffsets.add(xrefOffset);
                long streamOffset = trailer.getLong(COSName.XREF_STM);
                if (streamOffset > 0)
                {
                    requireIOCondition(isValidXrefStreamOffset(xrefOffset),
                            "Offset '" + streamOffset + "' doesn't point to an xref stream");

                    trailer.setLong(COSName.XREF_STM, streamOffset);
                    xrefStreamParser.parse(streamOffset);
                }
                xrefOffset = trailer.getLong(COSName.PREV);
            }
            else
            {
                COSDictionary streamDictionary = xrefStreamParser.parse(xrefOffset);
                parsedOffsets.add(xrefOffset);
                xrefOffset = streamDictionary.getLong(COSName.PREV);
            }
        }
        return true;
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
            parser.skipSpaces();
            COSDictionary xrefStreamDictionary = parser.nextDictionary();
            parser.position(xrefStreamOffset);
            return COSName.XREF.equals(xrefStreamDictionary.getCOSName(COSName.TYPE));
        }
        catch (IOException exception)
        {
            return false;
        }
    }

    public COSDictionary trailer()
    {
        return this.trailer;
    }

}
