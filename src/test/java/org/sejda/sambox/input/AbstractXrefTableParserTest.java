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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.sejda.io.SeekableSources.inMemorySeekableSourceFrom;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.xref.XrefEntry;

/**
 * @author Andrea Vacondio
 *
 */
public class AbstractXrefTableParserTest
{

    @Test
    public void parse() throws IOException
    {
        Set<XrefEntry> found = new HashSet<>();
        AbstractXrefTableParser victim = new AbstractXrefTableParser(
                new COSParser(inMemorySeekableSourceFrom(
                        getClass().getResourceAsStream("/sambox/xref_table.txt"))))
        {
            @Override
            void onTrailerFound(COSDictionary trailer)
            {
                assertNotNull(trailer);
            }

            @Override
            void onEntryFound(XrefEntry entry)
            {
                assertNotNull(entry);
                found.add(entry);
            }
        };
        victim.parse(15);
        assertEquals(3, found.size());
    }

    @Test(expected = IOException.class)
    public void parseCorrupted() throws IOException
    {
        AbstractXrefTableParser victim = new AbstractXrefTableParser(
                new COSParser(inMemorySeekableSourceFrom(
                        getClass().getResourceAsStream("/sambox/xref_table_corrupted.txt"))))
        {
            @Override
            void onTrailerFound(COSDictionary trailer)
            {
                // nothing
            }

            @Override
            void onEntryFound(XrefEntry entry)
            {
                // nothing
            }
        };
        victim.parse(15);
    }

    @Test(expected = IOException.class)
    public void parseCorruptedObjectNumber() throws IOException
    {
        AbstractXrefTableParser victim = new AbstractXrefTableParser(
                new COSParser(inMemorySeekableSourceFrom(
                        getClass().getResourceAsStream("/sambox/xref_table_corrupted_on.txt"))))
        {
            @Override
            void onTrailerFound(COSDictionary trailer)
            {
                // nothing
            }

            @Override
            void onEntryFound(XrefEntry entry)
            {
                // nothing
            }
        };
        victim.parse(15);
    }

    @Test(expected = IOException.class)
    public void parseCorruptedGenerationNumber() throws IOException
    {
        AbstractXrefTableParser victim = new AbstractXrefTableParser(
                new COSParser(inMemorySeekableSourceFrom(
                        getClass().getResourceAsStream("/sambox/xref_table_corrupted_gn.txt"))))
        {
            @Override
            void onTrailerFound(COSDictionary trailer)
            {
                // nothing
            }

            @Override
            void onEntryFound(XrefEntry entry)
            {
                // nothing
            }
        };
        victim.parse(15);
    }

    @Test
    public void parseEmptyTable() throws IOException
    {
        AbstractXrefTableParser victim = new AbstractXrefTableParser(
                new COSParser(inMemorySeekableSourceFrom(
                        getClass().getResourceAsStream("/sambox/xref_empty_table.txt"))))
        {
            @Override
            void onTrailerFound(COSDictionary trailer)
            {
                // nothing
            }

            @Override
            void onEntryFound(XrefEntry entry)
            {
                // nothing
            }
        };
        victim.parse(15);
    }

    @Test(expected = IOException.class)
    public void parseInvalidRow() throws IOException
    {
        AbstractXrefTableParser victim = new AbstractXrefTableParser(
                new COSParser(inMemorySeekableSourceFrom(
                        getClass().getResourceAsStream("/sambox/xref_table_invalid_row.txt"))))
        {
            @Override
            void onTrailerFound(COSDictionary trailer)
            {
                assertNotNull(trailer);
            }

            @Override
            void onEntryFound(XrefEntry entry)
            {
                assertNotNull(entry);
            }
        };
        victim.parse(15);
    }

    /**
     * PDFBOX-1739
     * 
     * @throws IOException
     */
    @Test
    public void parseSkipsUnexpectedRows() throws IOException
    {
        Set<XrefEntry> found = new HashSet<>();
        AbstractXrefTableParser victim = new AbstractXrefTableParser(
                new COSParser(inMemorySeekableSourceFrom(
                        getClass().getResourceAsStream("/sambox/xref_table_wrong_size.txt"))))
        {
            @Override
            void onTrailerFound(COSDictionary trailer)
            {
                assertNotNull(trailer);
            }

            @Override
            void onEntryFound(XrefEntry entry)
            {
                assertNotNull(entry);
                found.add(entry);
            }
        };
        victim.parse(15);
        assertEquals(6, found.size());
    }

    /**
     * PDFBOX-474
     * 
     * @throws IOException
     */
    @Test
    public void parseInvalidTokensNumber() throws IOException
    {
        Set<XrefEntry> found = new HashSet<>();
        AbstractXrefTableParser victim = new AbstractXrefTableParser(
                new COSParser(inMemorySeekableSourceFrom(getClass()
                        .getResourceAsStream("/sambox/xref_table_invalid_tokens_number.txt"))))
        {
            @Override
            void onTrailerFound(COSDictionary trailer)
            {
                assertNotNull(trailer);
            }

            @Override
            void onEntryFound(XrefEntry entry)
            {
                assertNotNull(entry);
                found.add(entry);
            }
        };
        victim.parse(15);
        assertEquals(3, found.size());
    }

    @Test
    public void parseNegativeGeneration() throws IOException
    {
        AbstractXrefTableParser victim = new AbstractXrefTableParser(
                new COSParser(inMemorySeekableSourceFrom(
                        getClass().getResourceAsStream("/sambox/xref_table_negative.txt"))))
        {
            @Override
            void onTrailerFound(COSDictionary trailer)
            {
                assertNotNull(trailer);
            }

            @Override
            void onEntryFound(XrefEntry entry)
            {
                assertNotNull(entry);
            }
        };
        victim.parse(15);
    }
}
