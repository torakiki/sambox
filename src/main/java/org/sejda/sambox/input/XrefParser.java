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
import static org.sejda.commons.util.RequireUtils.requireIOCondition;
import static org.sejda.sambox.input.AbstractXrefTableParser.TRAILER;
import static org.sejda.sambox.input.AbstractXrefTableParser.XREF;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.input.XrefFullScanner.XrefScanOutcome;
import org.sejda.sambox.xref.FileTrailer;
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

    private FileTrailer trailer = new FileTrailer();
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
                trailer.getCOSObject().mergeWithoutOverwriting(found);
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
                trailer.getCOSObject().mergeWithoutOverwriting(found);
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
                trailer = fallbackFullScanner.trailer();
            }
            if (xrefScanStatus != XrefScanOutcome.FOUND)
            {
                // there were errors in the found xrefs so we perform objects full scan
                LOG.warn(
                        "Xref full scan encountered some errors, now performing objects full scan");
                ObjectsFullScanner objectsFullScanner = new ObjectsFullScanner(parser)
                {
                    private long lastObjectOffset = 0;

                    @Override
                    protected void onNonObjectDefinitionLine(long offset, String line)
                            throws IOException
                    {
                        if (nonNull(line))
                        {
                            if (line.startsWith(TRAILER))
                            {
                                LOG.debug("Parsing trailer at {}", offset);
                                parser.position(offset);
                                parser.skipExpected(TRAILER);
                                parser.skipSpaces();
                                trailer.getCOSObject().merge(parser.nextDictionary());
                                parser.skipSpaces();
                            }
                            else if (line.contains(COSName.CATALOG.getName()))
                            {
                                long position = parser.position();
                                try
                                {
                                    // we do our best to make sure we have a catalog even in corrupted docs
                                    LOG.debug("Parsing potential Catalog at {}", lastObjectOffset);
                                    parser.position(lastObjectOffset);
                                    parser.skipIndirectObjectDefinition();
                                    parser.skipSpaces();
                                    COSDictionary possibleCatalog = parser.nextDictionary();
                                    if (COSName.CATALOG
                                            .equals(possibleCatalog.getCOSName(COSName.TYPE)))
                                    {
                                        trailer.getCOSObject().putIfAbsent(COSName.ROOT,
                                                possibleCatalog);
                                    }
                                    parser.skipSpaces();
                                }
                                catch (IOException e)
                                {
                                    LOG.warn("Unable to parse potential Catalog", e);
                                    parser.position(position);
                                }
                            }
                            else if (line.startsWith(XREF))
                            {
                                LOG.debug("Found xref at {}", offset);
                                trailer.xrefOffset(offset);
                            }
                        }
                    }

                    @Override
                    protected void onObjectDefinitionLine(long offset, String line)
                    {
                        lastObjectOffset = offset;
                    }
                };
                // and we consider it more reliable compared to what was found in the somehow broken xrefs
                objectsFullScanner.entries().values().stream().forEach(parser.provider()::addEntry);
            }
            trailer.setFallbackScanStatus(xrefScanStatus.name());
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
            LOG.debug("Found xref offset at {}", xrefOffset);
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
        long currentOffset = xrefOffset;
        while (currentOffset > -1)
        {
            requireIOCondition(!parsedOffsets.contains(currentOffset), "/Prev loop detected");
            requireIOCondition(isValidXrefOffset(currentOffset),
                    "Offset '" + currentOffset + "' doesn't point to an xref table or stream");

            parser.position(currentOffset);
            parser.skipSpaces();

            if (parser.isNextToken(XREF))
            {
                COSDictionary trailer = xrefTableParser.parse(currentOffset);
                parsedOffsets.add(currentOffset);
                long streamOffset = trailer.getLong(COSName.XREF_STM);
                if (streamOffset > 0)
                {
                    requireIOCondition(isValidXrefStreamOffset(streamOffset),
                            "Offset '" + streamOffset + "' doesn't point to an xref stream");
                    xrefStreamParser.parse(streamOffset);
                }
                currentOffset = trailer.getLong(COSName.PREV);
            }
            else
            {
                COSDictionary streamDictionary = xrefStreamParser.parse(currentOffset);
                parsedOffsets.add(currentOffset);
                currentOffset = streamDictionary.getLong(COSName.PREV);
            }
        }
        trailer.xrefOffset(xrefOffset);
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

    public FileTrailer trailer()
    {
        return this.trailer;
    }

}
