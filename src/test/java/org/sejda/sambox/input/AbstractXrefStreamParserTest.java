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
import static org.junit.Assert.assertTrue;
import static org.sejda.io.SeekableSources.inMemorySeekableSourceFrom;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.xref.CompressedXrefEntry;
import org.sejda.sambox.xref.XrefEntry;
import org.sejda.sambox.xref.XrefType;

/**
 * 
 * @author Andrea Vacondio
 */
public class AbstractXrefStreamParserTest
{

    @Test
    public void parse() throws IOException
    {
        Set<XrefEntry> found = new HashSet<>();
        AbstractXrefStreamParser victim = new AbstractXrefStreamParser(
                new COSParser(inMemorySeekableSourceFrom(
                        getClass().getResourceAsStream("/sambox/xref_stream.txt"))))
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
        victim.parse(17);
        assertEquals(10, found.size());
        for (XrefEntry entry : found)
        {
            if (entry.getType() == XrefType.COMPRESSED)
            {
                assertEquals(2L, ((CompressedXrefEntry) entry).getObjectStreamNumber());
            }
        }
    }

    @Test
    public void parseDefaultW0() throws IOException
    {
        Set<XrefEntry> found = new HashSet<>();
        AbstractXrefStreamParser victim = new AbstractXrefStreamParser(
                new COSParser(inMemorySeekableSourceFrom(
                        getClass().getResourceAsStream("/sambox/xref_stream_no_w0.txt"))))
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
                assertEquals(XrefType.IN_USE, entry.getType());
                found.add(entry);
            }
        };
        victim.parse(17);
        assertEquals(10, found.size());
    }

    @Test
    public void parseNoIdex() throws IOException
    {
        Set<XrefEntry> found = new HashSet<>();
        AbstractXrefStreamParser victim = new AbstractXrefStreamParser(
                new COSParser(inMemorySeekableSourceFrom(
                        getClass().getResourceAsStream("/sambox/xref_stream_no_index.txt"))))
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
        victim.parse(17);
        assertEquals(10, found.size());
    }

    @Test
    public void parseRanges() throws IOException
    {
        Set<XrefEntry> found = new HashSet<>();
        AbstractXrefStreamParser victim = new AbstractXrefStreamParser(
                new COSParser(inMemorySeekableSourceFrom(
                        getClass().getResourceAsStream("/sambox/xref_stream_multiple_ranges.txt"))))
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
        victim.parse(17);
        assertEquals(10, found.size());
        for (XrefEntry entry : found)
        {
            if (entry.getType() != XrefType.COMPRESSED)
            {
                assertTrue(entry.getObjectNumber() == 501 || entry.getObjectNumber() == 2);
            }
        }
    }
}
