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

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sejda.commons.util.IOUtils;
import org.sejda.io.SeekableSources;
import org.sejda.sambox.TestUtils;
import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSNull;
import org.sejda.sambox.cos.COSObjectKey;
import org.sejda.sambox.cos.COSStream;
import org.sejda.sambox.xref.CompressedXrefEntry;
import org.sejda.sambox.xref.XrefEntry;

/**
 * @author Andrea Vacondio
 */
public class LazyIndirectObjectsProviderTest
{
    private LazyIndirectObjectsProvider victim;
    private COSParser parser;

    @Before
    public void setUp() throws IOException
    {
        victim = new LazyIndirectObjectsProvider();
    }

    private void initialize(String name) throws IOException
    {
        parser = new COSParser(
                SeekableSources.inMemorySeekableSourceFrom(getClass().getResourceAsStream(name)),
                victim);
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
    public void get() throws IOException
    {
        initialize("/sambox/simple_test.pdf");
        COSBase dictionary = victim.get(new COSObjectKey(4, 0));
        assertThat(dictionary, is(instanceOf(COSDictionary.class)));
        assertNotNull(((COSDictionary) dictionary).getItem(COSName.ANNOTS));
    }

    @Test
    public void getNotExisting() throws IOException
    {
        initialize("/sambox/simple_test.pdf");
        assertNull(victim.get(new COSObjectKey(30, 0)));
    }

    @Test
    public void getCompressed() throws IOException
    {
        initialize("/sambox/simple_test_objstm.pdf");
        assertNotNull(victim.get(new COSObjectKey(6, 0)));
    }

    @Test
    public void getCompressedWrongOwningStream() throws IOException
    {
        initialize("/sambox/simple_test_objstm.pdf");
        victim.addEntry(CompressedXrefEntry.compressedEntry(6, 8, 1));
        assertNull(victim.get(new COSObjectKey(6, 0)));
    }

    @Test
    public void getCompressedWrongOwningStreamCompressed() throws IOException
    {
        initialize("/sambox/simple_test_objstm.pdf");
        victim.addEntry(CompressedXrefEntry.compressedEntry(6, 4, 1));
        assertNull(victim.get(new COSObjectKey(6, 0)));
    }

    @Test
    public void selfIndirectRef() throws IOException
    {
        initialize("/sambox/self_indirect_ref.pdf");
        assertEquals(COSNull.NULL, victim.get(new COSObjectKey(9, 0)).getCOSObject());
    }

    @Test
    public void getCompressedNullContainingStream() throws IOException
    {
        initialize("/sambox/simple_test_objstm.pdf");
        victim.addEntry(CompressedXrefEntry.compressedEntry(6, 1404, 1));
        assertNull(victim.get(new COSObjectKey(6, 0)));
    }

    @Test
    public void fallbackDoesntWorkForObjStm() throws IOException
    {
        initialize("/sambox/bad_objstm.pdf");
        assertNull(victim.get(new COSObjectKey(6, 0)));
    }

    @Test
    public void fallbackKicksIn() throws IOException
    {
        initialize("/sambox/test_multiple_xref_tables_wrong_obj_offset.pdf");
        assertNotNull(victim.get(new COSObjectKey(1, 0)));
    }

    @Test
    public void release() throws IOException
    {
        initialize("/sambox/simple_test.pdf");
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

    @Test
    public void close() throws IOException
    {
        Map<COSObjectKey, COSBase> store = new ConcurrentHashMap<>();
        COSStream item = mock(COSStream.class);
        COSObjectKey key = new COSObjectKey(1, 0);
        store.put(key, item);
        TestUtils.setProperty(victim, "store", store);
        victim.close();
        verify(item).close();
    }

    @Test
    public void emptyObj() throws IOException
    {
        parser = new COSParser(SeekableSources.inMemorySeekableSourceFrom(
                getClass().getResourceAsStream("/sambox/empty_obj.txt")), victim);
        victim.initializeWith(parser);
        victim.addEntry(XrefEntry.inUseEntry(10, 3, 0));
        assertEquals(COSNull.NULL, victim.get(new COSObjectKey(10, 0)));
    }

    @Test
    public void selfPointingStreamLength() throws IOException
    {
        parser = new COSParser(SeekableSources.inMemorySeekableSourceFrom(
                getClass().getResourceAsStream("/sambox/self_poiting_stream_length.txt")), victim);
        victim.initializeWith(parser);
        victim.addEntry(XrefEntry.inUseEntry(3, 0, 0));
        COSBase stream = victim.get(new COSObjectKey(3, 0));
        assertThat(stream, is(instanceOf(COSStream.class)));
        assertEquals(5, ((COSStream) stream).getLong(COSName.LENGTH));
    }
}
