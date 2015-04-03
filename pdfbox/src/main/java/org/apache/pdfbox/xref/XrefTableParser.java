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

import static org.apache.pdfbox.load.ParseUtils.isDigit;
import static org.apache.pdfbox.load.ParseUtils.isEOF;
import static org.apache.pdfbox.load.ParseUtils.isEndOfName;
import static org.apache.pdfbox.xref.XrefEntry.inUseEntry;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.io.PushBackInputStream;
import org.apache.pdfbox.load.BaseCOSParser;
import org.apache.pdfbox.load.IndirectObjectsProvider;

/**
 * @author Andrea Vacondio
 *
 */
class XrefTableParser extends BaseCOSParser
{
    private static final Log LOG = LogFactory.getLog(XrefTableParser.class);
    private static final String TRAILER = "trailer";
    static final String XREF = "xref";

    private Xref xref;
    private TrailerMerger trailerMerger;

    XrefTableParser(PushBackInputStream source, IndirectObjectsProvider provider, Xref xref,
            TrailerMerger trailerMerger)
    {
        super(source, provider);
        this.xref = xref;
        this.trailerMerger = trailerMerger;
    }

    /**
     * Parse the xref table
     * 
     * @param tableOffset xref stream object offset
     * @return the trailer for this table
     * @throws IOException
     */
    public COSDictionary parse(long tableOffset) throws IOException
    {
        parseXrefTable(tableOffset);
        skipSpaces();
        // PDFBOX-1739 skip extra xref entries in RegisSTAR documents
        while (source().peek() != 't')
        {
            LOG.warn("Expected trailer object at position " + offset() + ", skipping line.");
            readLine();
        }
        return parseTrailer();
    }

    private void parseXrefTable(long startByteOffset) throws IOException
    {
        skipExpected(XREF);
        if (isNextToken(TRAILER))
        {
            LOG.warn("Skipping empty xref table at offset " + startByteOffset);
            return;
        }
        while (true)
        {
            long currentObjectNumber = readObjectNumber();
            long numberOfEntries = readLong();
            skipSpaces();

            for (int i = 0; i < numberOfEntries; i++)
            {
                int next = source().peek();
                if (next == 't' || isEndOfName(next) || isEOF(next))
                {
                    break;
                }

                String currentLine = readLine();
                String[] splitString = currentLine.split("\\s");
                if (splitString.length < 3)
                {
                    LOG.warn("Invalid xref line: " + currentLine);
                    break;
                }
                // TODO add a unit test instead of comment?
                // This supports the corrupt table as reported in PDFBOX-474 (XXXX XXX XX n)
                String entryType = splitString[splitString.length - 1];
                if ("n".equals(entryType))
                {
                    try
                    {
                        xref.add(inUseEntry(currentObjectNumber, Long.parseLong(splitString[0]),
                                Integer.parseInt(splitString[1])));
                    }
                    catch (NumberFormatException e)
                    {
                        throw new IOException(e);
                    }
                }
                else if (!"f".equals(entryType))
                {
                    throw new IOException("Corrupted xref table entry. Expected 'f' but was "
                            + entryType);
                }
                currentObjectNumber++;
                skipSpaces();
            }
            skipSpaces();
            if (!isDigit(source().peek()))
            {
                break;
            }
        }

    }

    private COSDictionary parseTrailer() throws IOException
    {
        long offset = offset();
        LOG.debug("Parsing trailer at " + offset);
        skipExpected(TRAILER);
        skipSpaces();
        COSDictionary dictionary = nextDictionary();
        trailerMerger.mergeTrailerWithoutOverwriting(offset, dictionary);
        skipSpaces();
        return dictionary;
    }
}
