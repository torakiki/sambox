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

import static org.sejda.sambox.util.CharUtils.isDigit;
import static org.sejda.sambox.util.CharUtils.isEOF;
import static org.sejda.sambox.util.CharUtils.isEndOfName;
import static org.sejda.sambox.xref.XrefEntry.inUseEntry;

import java.io.IOException;

import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.xref.XrefEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for an xref table parser. Implementors will decide what to do when the parser finds a new trailer or a new
 * entries.
 * 
 * @author Andrea Vacondio
 * @see AbstractXrefStreamParser
 */
abstract class AbstractXrefTableParser
{
    private static final Logger LOG = LoggerFactory.getLogger(AbstractXrefTableParser.class);
    static final String TRAILER = "trailer";
    static final String XREF = "xref";

    private COSParser parser;

    AbstractXrefTableParser(COSParser parser)
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
     * Parse the xref table
     * 
     * @param tableOffset xref table offset. This is the offset of the "xref" keyword.
     * @return the trailer for this table
     * @throws IOException
     */
    public COSDictionary parse(long tableOffset) throws IOException
    {
        parseXrefTable(tableOffset);
        parser.skipSpaces();
        while (parser.source().peek() != 't')
        {
            LOG.warn("Expected trailer object at position " + parser.position()
                    + ", skipping line.");
            parser.readLine();
        }
        return parseTrailer();
    }

    private void parseXrefTable(long tableOffset) throws IOException
    {
        parser.position(tableOffset);
        parser.skipExpected(XREF);
        if (parser.isNextToken(TRAILER))
        {
            LOG.warn("Skipping empty xref table at offset " + tableOffset);
            return;
        }
        while (true)
        {
            long currentObjectNumber = parser.readObjectNumber();
            long numberOfEntries = parser.readLong();
            parser.skipSpaces();

            for (int i = 0; i < numberOfEntries; i++)
            {
                int next = parser.source().peek();
                if (next == 't' || isEndOfName(next) || isEOF(next))
                {
                    break;
                }

                String currentLine = parser.readLine();
                String[] splitString = currentLine.split("\\s");
                if (splitString.length < 3)
                {
                    throw new IOException(
                            "Corrupted xref table entry. Invalid xref line: " + currentLine);
                }
                String entryType = splitString[splitString.length - 1];
                if ("n".equals(entryType))
                {
                    try
                    {
                        onEntryFound(inUseEntry(currentObjectNumber, Long.parseLong(splitString[0]),
                                Integer.parseInt(splitString[1])));
                    }
                    catch (IllegalArgumentException e)
                    {
                        throw new IOException(
                                "Corrupted xref table entry. Invalid xref line: " + currentLine, e);
                    }

                }
                else if (!"f".equals(entryType))
                {
                    throw new IOException(
                            "Corrupted xref table entry. Expected 'f' but was " + entryType);
                }
                currentObjectNumber++;
                parser.skipSpaces();
            }
            parser.skipSpaces();
            if (!isDigit(parser.source().peek()))
            {
                break;
            }
        }
    }

    private COSDictionary parseTrailer() throws IOException
    {
        long offset = parser.position();
        LOG.debug("Parsing trailer at " + offset);
        parser.skipExpected(TRAILER);
        parser.skipSpaces();
        COSDictionary dictionary = parser.nextDictionary();
        onTrailerFound(dictionary);
        parser.skipSpaces();
        return dictionary;
    }

    COSParser parser()
    {
        return parser;
    }
}
