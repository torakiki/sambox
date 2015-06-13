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
package org.apache.pdfbox.output;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.TreeMap;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSInteger;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.input.source.SeekableSource;
import org.apache.pdfbox.xref.CompressedXrefEntry;
import org.apache.pdfbox.xref.XrefEntry;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Andrea Vacondio
 *
 */
public class XrefStreamTest
{
    private TreeMap<Long, XrefEntry> elements = new TreeMap<>();

    @Before
    public void setUp()
    {
        elements.put(2L, CompressedXrefEntry.compressedEntry(2, 4, 1));
        elements.put(4L, XrefEntry.inUseEntry(4, 256, 0));
    }

    @Test
    public void someKeysAreRemoved() throws IOException
    {
        COSDictionary existingTrailer = new COSDictionary();
        existingTrailer.setName(COSName.PREV, "value");
        existingTrailer.setName(COSName.XREF_STM, "value");
        existingTrailer.setName(COSName.DOC_CHECKSUM, "value");
        existingTrailer.setName(COSName.DECODE_PARMS, "value");
        existingTrailer.setName(COSName.F_DECODE_PARMS, "value");
        existingTrailer.setName(COSName.F_FILTER, "value");
        existingTrailer.setName(COSName.F, "value");
        // TODO remove this test once encryption is implemented
        existingTrailer.setName(COSName.ENCRYPT, "value");

        try (XrefStream victim = new XrefStream(existingTrailer, elements))
        {
            assertFalse(victim.containsKey(COSName.PREV));
            assertFalse(victim.containsKey(COSName.XREF_STM));
            assertFalse(victim.containsKey(COSName.DOC_CHECKSUM));
            assertFalse(victim.containsKey(COSName.DECODE_PARMS));
            assertFalse(victim.containsKey(COSName.F_DECODE_PARMS));
            assertFalse(victim.containsKey(COSName.F_FILTER));
            assertFalse(victim.containsKey(COSName.F));
            assertFalse(victim.containsKey(COSName.ENCRYPT));
        }
    }

    @Test
    public void keysArePopulated() throws IOException
    {
        try (XrefStream victim = new XrefStream(new COSDictionary(), elements))
        {
            assertEquals(COSName.XREF, victim.getCOSName(COSName.TYPE));
            assertEquals(5, victim.getLong(COSName.SIZE));
            COSArray index = (COSArray) victim.getItem(COSName.INDEX);
            assertNotNull(index);
            assertEquals(2, ((COSInteger) index.get(0)).intValue());
            assertEquals(3, ((COSInteger) index.get(1)).intValue());
            COSArray w = (COSArray) victim.getItem(COSName.W);
            assertEquals(1, ((COSInteger) w.get(0)).intValue());
            assertEquals(2, ((COSInteger) w.get(1)).intValue());
            assertEquals(2, ((COSInteger) w.get(2)).intValue());
            assertEquals(victim.getLong(COSName.DL), victim.getUnfilteredLength());
        }
    }

    @Test
    public void streamIsPopulated() throws IOException
    {
        try (XrefStream victim = new XrefStream(new COSDictionary(), elements))
        {
            SeekableSource source = victim.getUnfilteredSource();
            assertEquals(0b00000010, source.read());
            assertEquals(0b00000000, source.read());
            assertEquals(0b00000100, source.read());
            assertEquals(0b00000000, source.read());
            assertEquals(0b00000001, source.read());

            assertEquals(0b00000000, source.read());
            assertEquals(0b00000000, source.read());
            assertEquals(0b00000011, source.read());
            assertEquals(0b00000000, source.read());
            assertEquals(0b00000000, source.read());

            assertEquals(0b00000001, source.read());
            assertEquals(0b00000001, source.read());
            assertEquals(0b00000000, source.read());
            assertEquals(0b00000000, source.read());
            assertEquals(0b00000000, source.read());
        }
    }
}
