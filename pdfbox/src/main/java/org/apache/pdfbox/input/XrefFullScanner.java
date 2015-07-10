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

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.xref.XrefEntry;

/**
 * Component scanning for xref tables/streams. It scans top to bottom parsing any xref table/stream found with the
 * assumption that xrefs found later in the file are more recent.
 * 
 * @author Andrea Vacondio
 */
class XrefFullScanner
{
    private static final Log LOG = LogFactory.getLog(XrefFullScanner.class);

    private COSDictionary trailer = new COSDictionary();
    private AbstractXrefStreamParser xrefStreamParser;
    private AbstractXrefTableParser xrefTableParser;
    private BaseCOSParser parser;
    private Pattern objectDefPatter = Pattern.compile("^(\\d+)[\\s](\\d+)[\\s]obj");

    XrefFullScanner(BaseCOSParser parser)
    {
        this.parser = parser;
        this.xrefStreamParser = new AbstractXrefStreamParser(parser)
        {
            @Override
            void onTrailerFound(COSDictionary found)
            {
                trailer.merge(found);
            }

            @Override
            void onEntryFound(XrefEntry entry)
            {
                parser.provider().addEntry(entry);
            }
        };
        this.xrefTableParser = new AbstractXrefTableParser(parser)
        {

            @Override
            void onTrailerFound(COSDictionary found)
            {
                trailer.merge(found);
            }

            @Override
            void onEntryFound(XrefEntry entry)
            {
                parser.provider().addEntry(entry);
            }
        };
    }

    void scan() throws IOException
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
                parseFoundXrefTable(offset);
            }
            Matcher matcher = objectDefPatter.matcher(line);
            if (matcher.find())
            {
                parseFoundObject(offset);
            }
            parser.skipSpaces();
        }
        parser.position(savedPos);
    }

    private void parseFoundXrefTable(long offset) throws IOException
    {
        LOG.debug("Found xref table at " + offset);
        xrefTableParser.parse(offset);
    }

    COSDictionary trailer()
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
            LOG.debug("Found xref stream at " + offset);
            parseFoundXrefStream((COSDictionary) found);
        }
    }

    private void parseFoundXrefStream(COSDictionary trailer) throws IOException
    {
        try (COSStream xrefStream = parser.nextStream(trailer))
        {
            xrefStreamParser.onTrailerFound(trailer);
            xrefStreamParser.parseStream(xrefStream);
        }
        LOG.debug("Done parsing xref stream");
    }

}
