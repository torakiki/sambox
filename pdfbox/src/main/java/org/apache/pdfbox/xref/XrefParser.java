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

import static org.apache.pdfbox.util.RequireUtils.requireIOCondition;
import static org.apache.pdfbox.xref.XrefTableParser.XREF;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.input.BaseCOSParser;
import org.apache.pdfbox.util.Charsets;

/**
 * @author Andrea Vacondio
 *
 */
public class XrefParser
{
    private static final Log LOG = LogFactory.getLog(XrefParser.class);
    /**
     * How many trailing bytes to read for EOF marker.
     */
    private static final int DEFAULT_TRAIL_BYTECOUNT = 2048;
    private static final String STARTXREF = "startxref";

    private TrailerMerger trailerMerger = new TrailerMerger();
    private XrefStreamParser xrefStreamParser;
    private XrefTableParser xrefTableParser;
    private BaseCOSParser parser;

    public XrefParser(BaseCOSParser parser)
    {
        this.parser = parser;
        this.xrefStreamParser = new XrefStreamParser(parser, trailerMerger);
        this.xrefTableParser = new XrefTableParser(parser, trailerMerger);
    }

    /**
     * @return parse the xref returning its offset
     * @throws IOException
     */
    public long parse() throws IOException
    {
        long xrefOffset = findXrefOffset();
        if (xrefOffset > 0)
        {
            LOG.debug("Found xref offset at " + xrefOffset);
            parseXref(xrefOffset);
            return xrefOffset;
        }
        else
        {
            rebuildTrailer();
            // return the offset found
            return -1;
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
        parser.offset(startPosition);
        byte[] buffer = parser.source().readFully(chunkSize);
        int relativeIndex = new String(buffer, Charsets.ISO_8859_1).lastIndexOf(STARTXREF);
        if (relativeIndex < 0)
        {
            LOG.warn("Unable to find 'startxref' keyword");
            return -1;
        }
        parser.offset(startPosition + relativeIndex + STARTXREF.length());
        parser.skipSpaces();
        return parser.readLong();
    }

    private void parseXref(long xrefOffset) throws IOException
    {
        if (!isValidXrefOffset(xrefOffset))
        {
            LOG.warn("Offset '" + xrefOffset
                    + "' doesn't point to an xref table or stream, applying fallback strategy");
            // fallback strategy
            // TODO set xrefOffset to amended offset

        }
        requireIOCondition(xrefOffset > 0, "Unable to find correct xref table or stream offset");

        while (xrefOffset > -1)
        {
            parser.offset(xrefOffset);
            parser.skipSpaces();

            if (parser.isNextToken(XREF))
            {
                COSDictionary trailer = xrefTableParser.parse(xrefOffset);
                long streamOffset = trailer.getLong(COSName.XREF_STM);
                if (streamOffset > 0)
                {
                    if (!isValidXrefStreamOffset(streamOffset))
                    {
                        LOG.warn("Offset '" + streamOffset
                                + "' doesn't point to an xref stream, applying fallback strategy");
                        // fallback strategy
                        // TODO set streamOffset to amended offset
                        // TODO log if the fallback offset is invalid

                    }
                    if (streamOffset > 0)
                    {
                        trailer.setLong(COSName.XREF_STM, streamOffset);
                        xrefStreamParser.parse(streamOffset);
                    }

                }
                xrefOffset = amendPrevIfInvalid(trailer);
            }
            else
            {
                COSDictionary streamDictionary = xrefStreamParser.parse(xrefOffset);
                xrefOffset = amendPrevIfInvalid(streamDictionary);
            }
        }
        // TODO
        // check the offsets of all referenced objects
        // checkXrefOffsets();
    }

    /**
     * Validates the PREV entry in the given dictionary and if not valid it applies a fallback strategy
     * 
     * @param dictionary
     * @return the original PREV value if valid, the fallback amended one otherwise.
     * @throws IOException
     */
    private long amendPrevIfInvalid(COSDictionary dictionary) throws IOException
    {
        long prevOffset = dictionary.getLong(COSName.PREV);
        if (prevOffset > 0)
        {
            if (!isValidXrefOffset(prevOffset))
            {
                LOG.warn("Offset '" + prevOffset
                        + "' doesn't point to an xref table or stream, applying fallback strategy");
                // fallback strategy
                // TODO set the amended offset
                dictionary.setLong(COSName.PREV, prevOffset);
            }
        }
        return prevOffset;
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
        parser.offset(xrefOffset);
        return parser.isNextToken(XREF);
    }

    /**
     * @param xrefStreamOffset
     * @return true if the given offset points to a valid xref stream
     * @throws IOException
     */
    private boolean isValidXrefStreamOffset(long xrefStreamOffset) throws IOException
    {
        parser.offset(xrefStreamOffset);
        try
        {
            parser.skipIndirectObjectDefinition();
        }
        catch (IOException exception)
        {
            return false;
        }
        parser.offset(xrefStreamOffset);
        return true;
    }

    /**
     * Rebuild the trailer dictionary if startxref can't be found.
     * 
     * @return the rebuild trailer dictionary
     * 
     * @throws IOException if something went wrong
     */
    protected final COSDictionary rebuildTrailer()
    {

        // TODO rebuild strategy
        return null;
    }

    public COSDictionary getTrailer()
    {
        return this.trailerMerger.getTrailer();
    }

}
