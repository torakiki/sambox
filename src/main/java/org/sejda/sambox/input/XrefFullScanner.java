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

import static org.sejda.sambox.input.AbstractXrefTableParser.XREF;

import java.io.IOException;
import java.util.regex.Pattern;

import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSStream;
import org.sejda.sambox.xref.FileTrailer;
import org.sejda.sambox.xref.XrefEntry;
import org.sejda.sambox.xref.XrefType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Component scanning for xref tables/streams. It scans top to bottom parsing any xref table/stream found with the
 * assumption that xrefs found later in the file are more recent.
 * 
 * @author Andrea Vacondio
 */
class XrefFullScanner
{
    private static final Logger LOG = LoggerFactory.getLogger(XrefFullScanner.class);

    private FileTrailer trailer = new FileTrailer();
    private AbstractXrefStreamParser xrefStreamParser;
    private AbstractXrefTableParser xrefTableParser;
    private COSParser parser;
    private Pattern objectDefPatter = Pattern.compile("^(\\d+)[\\s](\\d+)[\\s]obj");
    private XrefScanOutcome outcome = XrefScanOutcome.NOT_FOUND;

    XrefFullScanner(COSParser parser)
    {
        this.parser = parser;
        this.xrefStreamParser = new AbstractXrefStreamParser(parser)
        {
            @Override
            void onTrailerFound(COSDictionary found)
            {
                trailer.getCOSObject().merge(found);
            }

            @Override
            void onEntryFound(XrefEntry entry)
            {
                addEntryIfValid(entry);
            }
        };
        this.xrefTableParser = new AbstractXrefTableParser(parser)
        {

            @Override
            void onTrailerFound(COSDictionary found)
            {
                trailer.getCOSObject().merge(found);
            }

            @Override
            void onEntryFound(XrefEntry entry)
            {
                addEntryIfValid(entry);
            }
        };
    }

    private void addEntryIfValid(XrefEntry entry)
    {
        if (isValidEntry(entry))
        {
            parser.provider().addEntry(entry);
        }
        else
        {
            outcome = outcome.moveTo(XrefScanOutcome.WITH_ERRORS);
        }
    }

    /**
     * 
     * @return the state of the scan
     */
    XrefScanOutcome scan()
    {
        try
        {
            doScan();
        }
        catch (Exception e)
        {
            outcome = outcome.moveTo(XrefScanOutcome.WITH_ERRORS);
            LOG.warn("An error occurred while performing full scan looking for xrefs", e);
        }
        return outcome;
    }

    private void doScan() throws IOException
    {
        LOG.info("Performing full scan looking for xrefs");
        long savedPos = parser.position();
        parser.position(0);
        parser.skipSpaces();
        while (parser.source().peek() != -1)
        {
            long offset = parser.position();
            String line = parser.readLine();
            if (line.startsWith(XREF))
            {
                outcome = outcome.moveTo(XrefScanOutcome.FOUND);
                parseFoundXrefTable(offset);
            }
            if (objectDefPatter.matcher(line).find())
            {
                parseFoundObject(offset);
            }
            parser.skipSpaces();
        }
        parser.position(savedPos);
    }

    private void parseFoundXrefTable(long offset) throws IOException
    {
        LOG.debug("Found xref table at {}", offset);
        trailer.xrefOffset(offset);
        xrefTableParser.parse(offset);
    }

    FileTrailer trailer()
    {
        return trailer;
    }

    private void parseFoundObject(long offset) throws IOException
    {
        parser.position(offset);
        parser.skipIndirectObjectDefinition();
        parser.skipSpaces();
        COSBase found = parser.nextParsedToken();
        if (found instanceof COSDictionary
                && COSName.XREF.equals(((COSDictionary) found).getItem(COSName.TYPE)))
        {
            LOG.debug("Found xref stream at {}", offset);
            trailer.xrefOffset(offset);
            parseFoundXrefStream((COSDictionary) found);
        }
    }

    private void parseFoundXrefStream(COSDictionary trailer) throws IOException
    {
        outcome = outcome.moveTo(XrefScanOutcome.FOUND);
        try (COSStream xrefStream = parser.nextStream(trailer))
        {
            xrefStreamParser.onTrailerFound(trailer);
            xrefStreamParser.parseStream(xrefStream);
        }
        LOG.debug("Done parsing xref stream");
    }

    /**
     * @param entry
     * @return true if the given xref entry actually points to a valid object definition
     */
    private boolean isValidEntry(XrefEntry entry)
    {
        if (entry.getType() == XrefType.IN_USE)
        {
            if (entry.getByteOffset() < 0)
            {
                return false;
            }
            try
            {
                long origin = parser.position();
                try
                {
                    parser.position(entry.getByteOffset());
                    parser.skipIndirectObjectDefinition();
                    return true;
                }
                catch (IOException e)
                {
                    LOG.warn("Xref entry points to an invalid object definition {}", entry);
                }
                finally
                {
                    parser.position(origin);
                }
            }
            catch (IOException e)
            {
                LOG.error("Unable to change source position", e);
            }
            return false;
        }
        return true;
    }

    /**
     * possible outcome of the xref full scan
     * 
     * @author Andrea Vacondio
     *
     */
    public static enum XrefScanOutcome
    {
        NOT_FOUND, FOUND, WITH_ERRORS
        {
            @Override
            XrefScanOutcome moveTo(XrefScanOutcome newState)
            {
                return this;
            }
        };

        XrefScanOutcome moveTo(XrefScanOutcome newState)
        {
            if (newState != NOT_FOUND)
            {
                return newState;
            }
            return this;
        }
    }
}
