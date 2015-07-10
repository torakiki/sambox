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

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSObjectKey;
import org.apache.pdfbox.xref.CompressedXrefEntry;
import org.apache.pdfbox.xref.XrefEntry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sejda.io.SeekableSources;

/**
 * @author Andrea Vacondio
 */
public class LazyIndirectObjectsProviderTest
{
    private LazyIndirectObjectsProvider victim;
    private BaseCOSParser parser;

    @Before
    public void setUp() throws IOException
    {

        victim = new LazyIndirectObjectsProvider();
        parser = new BaseCOSParser(SeekableSources.inMemorySeekableSourceFrom(getClass()
                .getResourceAsStream("/input/simple_test.pdf")), victim);
        victim.initializeWith(parser);
        XrefParser xrefParser = new XrefParser(parser);
        xrefParser.parse();
    }

    @After
    public void tearDown()
    {
        IOUtils.closeQuietly(parser);
        IOUtils.closeQuietly(victim);
    }

    @Test
    public void get()
    {
        COSBase dictionary = victim.get(new COSObjectKey(3, 0));
        assertThat(dictionary, is(instanceOf(COSDictionary.class)));
        assertNotNull(((COSDictionary) dictionary).getItem(COSName.ANNOTS));
    }

    @Test
    public void getNotExisting()
    {
        assertNull(victim.get(new COSObjectKey(30, 0)));
    }

    @Test
    public void getCompressed() throws IOException
    {
        victim = new LazyIndirectObjectsProvider();
        parser = new BaseCOSParser(SeekableSources.inMemorySeekableSourceFrom(getClass()
                .getResourceAsStream("/input/simple_test_objstm.pdf")), victim);
        victim.initializeWith(parser);
        XrefParser xrefParser = new XrefParser(parser);
        xrefParser.parse();
        assertNotNull(victim.get(new COSObjectKey(6, 0)));
    }

    @Test
    public void getCompressedWrongOwningStream() throws IOException
    {
        victim = new LazyIndirectObjectsProvider();
        parser = new BaseCOSParser(SeekableSources.inMemorySeekableSourceFrom(getClass()
                .getResourceAsStream("/input/simple_test_objstm.pdf")), victim);
        victim.initializeWith(parser);
        XrefParser xrefParser = new XrefParser(parser);
        xrefParser.parse();
        victim.addEntry(CompressedXrefEntry.compressedEntry(6, 8, 1));
        assertNull(victim.get(new COSObjectKey(6, 0)));
    }

    @Test
    public void getCompressedWrongOwningStreamCompressed() throws IOException
    {
        victim = new LazyIndirectObjectsProvider();
        parser = new BaseCOSParser(SeekableSources.inMemorySeekableSourceFrom(getClass()
                .getResourceAsStream("/input/simple_test_objstm.pdf")), victim);
        victim.initializeWith(parser);
        XrefParser xrefParser = new XrefParser(parser);
        xrefParser.parse();
        victim.addEntry(CompressedXrefEntry.compressedEntry(6, 4, 1));
        assertNull(victim.get(new COSObjectKey(6, 0)));
    }

    @Test
    public void getCompressedNullContainingStream() throws IOException
    {
        victim = new LazyIndirectObjectsProvider();
        parser = new BaseCOSParser(SeekableSources.inMemorySeekableSourceFrom(getClass()
                .getResourceAsStream("/input/simple_test_objstm.pdf")), victim);
        victim.initializeWith(parser);
        XrefParser xrefParser = new XrefParser(parser);
        xrefParser.parse();
        victim.addEntry(CompressedXrefEntry.compressedEntry(6, 1404, 1));
        assertNull(victim.get(new COSObjectKey(6, 0)));
    }

    @Test
    public void fallbackDoesntWorkForObjStm() throws IOException
    {
        victim = new LazyIndirectObjectsProvider();
        parser = new BaseCOSParser(SeekableSources.inMemorySeekableSourceFrom(getClass()
                .getResourceAsStream("/input/bad_objstm.pdf")), victim);
        victim.initializeWith(parser);
        XrefParser xrefParser = new XrefParser(parser);
        xrefParser.parse();
        assertNull(victim.get(new COSObjectKey(6, 0)));
    }

    @Test
    public void fallbackKicksIn() throws IOException
    {
        victim = new LazyIndirectObjectsProvider();
        parser = new BaseCOSParser(SeekableSources.inMemorySeekableSourceFrom(getClass()
                .getResourceAsStream("/input/test_multiple_xref_tables_wrong_obj_offset.pdf")),
                victim);
        victim.initializeWith(parser);
        XrefParser xrefParser = new XrefParser(parser);
        xrefParser.parse();
        assertNotNull(victim.get(new COSObjectKey(1, 0)));
    }

    @Test
    public void release()
    {
        COSObjectKey key = new COSObjectKey(1, 0);
        COSBase dictionary = victim.get(key);
        assertEquals(dictionary, victim.get(key));
        victim.release(key);
        assertNotEquals(dictionary, victim.get(key));
    }

    @Test
    public void addIfAbsent()
    {
        XrefEntry notThere = XrefEntry.inUseEntry(50, 1991, 0);
        assertNull(victim.addEntryIfAbsent(notThere));

        XrefEntry notThere2 = XrefEntry.inUseEntry(50, 222, 0);
        XrefEntry alreadyThere = victim.addEntryIfAbsent(notThere2);
        assertNotNull(alreadyThere);
        assertEquals(1991, alreadyThere.getByteOffset());
    }

    @Test
    public void add()
    {
        XrefEntry notThere = XrefEntry.inUseEntry(50, 1991, 0);
        assertNull(victim.addEntry(notThere));

        XrefEntry notThere2 = XrefEntry.inUseEntry(50, 222, 0);
        XrefEntry alreadyThere = victim.addEntry(notThere2);
        assertNotNull(alreadyThere);
        assertEquals(1991, alreadyThere.getByteOffset());
    }
}
