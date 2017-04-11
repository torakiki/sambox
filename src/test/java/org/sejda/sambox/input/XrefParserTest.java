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
import static org.junit.Assert.assertNull;

import java.io.IOException;

import org.junit.After;
import org.junit.Test;
import org.sejda.io.SeekableSources;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSObjectKey;
import org.sejda.util.IOUtils;

/**
 * @author Andrea Vacondio
 *
 */
public class XrefParserTest
{

    private XrefParser victim;
    private COSParser parser;

    @After
    public void tearDown() throws IOException
    {
        IOUtils.close(parser);
    }
    @Test
    public void scanStream() throws IOException
    {
        parser = new COSParser(SeekableSources.inMemorySeekableSourceFrom(
                getClass().getResourceAsStream("/sambox/simple_test.pdf")));
        victim = new XrefParser(parser);
        victim.parse();
        assertEquals(9, victim.trailer().getCOSObject().getInt(COSName.SIZE));
        assertNotNull(victim.trailer().getCOSObject().getDictionaryObject(COSName.ROOT));
        assertEquals(564, victim.trailer().xrefOffset());
    }

    @Test
    public void scanMultipleTables() throws IOException
    {
        parser = new COSParser(SeekableSources.inMemorySeekableSourceFrom(
                getClass().getResourceAsStream("/sambox/test_multiple_xref_tables.pdf")));
        victim = new XrefParser(parser);
        victim.parse();
        assertEquals(408, victim.trailer().getCOSObject().getInt(COSName.PREV));
        assertEquals(8, victim.trailer().getCOSObject().getInt(COSName.SIZE));
        assertNotNull(victim.trailer().getCOSObject().getDictionaryObject(COSName.ROOT));
        COSDictionary overriddenObj = (COSDictionary) parser.provider().get(new COSObjectKey(3, 0))
                .getCOSObject();
        assertEquals(839, victim.trailer().xrefOffset());
        assertNotNull(overriddenObj.getDictionaryObject(COSName.ANNOTS));
    }

    @Test
    public void scanStreamAndTable() throws IOException
    {
        parser = new COSParser(SeekableSources.inMemorySeekableSourceFrom(
                getClass().getResourceAsStream("/sambox/test_xref_stream_and_table.pdf")));
        victim = new XrefParser(parser);
        victim.parse();
        assertEquals(562, victim.trailer().getCOSObject().getInt(COSName.PREV));
        assertEquals(9, victim.trailer().getCOSObject().getInt(COSName.SIZE));
        assertNotNull(victim.trailer().getCOSObject().getDictionaryObject(COSName.ROOT));
        COSDictionary overriddenObj = (COSDictionary) parser.provider().get(new COSObjectKey(3, 0))
                .getCOSObject();
        assertEquals(919, victim.trailer().xrefOffset());
        assertNull(overriddenObj.getDictionaryObject(COSName.ANNOTS));
    }

    @Test
    public void scanCorruptedStreamAndTable() throws IOException
    {
        parser = new COSParser(SeekableSources.inMemorySeekableSourceFrom(getClass()
                .getResourceAsStream("/sambox/test_xref_corrupted_stream_and_table.pdf")));
        victim = new XrefParser(parser);
        victim.parse();
        assertEquals(562, victim.trailer().getCOSObject().getInt(COSName.PREV));
        assertEquals(9, victim.trailer().getCOSObject().getInt(COSName.SIZE));
        assertNotNull(victim.trailer().getCOSObject().getDictionaryObject(COSName.ROOT));
        COSDictionary overriddenObj = (COSDictionary) parser.provider().get(new COSObjectKey(3, 0))
                .getCOSObject();
        assertEquals(919, victim.trailer().xrefOffset());
        assertNull(overriddenObj.getDictionaryObject(COSName.ANNOTS));
    }

    @Test
    public void scanOnTableLoop() throws IOException
    {
        parser = new COSParser(SeekableSources.inMemorySeekableSourceFrom(
                getClass().getResourceAsStream("/sambox/test_xref_table_loop.pdf")));
        victim = new XrefParser(parser);
        victim.parse();
        assertEquals(919, victim.trailer().getCOSObject().getInt(COSName.PREV));
        assertEquals(9, victim.trailer().getCOSObject().getInt(COSName.SIZE));
        assertNotNull(victim.trailer().getCOSObject().getDictionaryObject(COSName.ROOT));
        COSDictionary overriddenObj = (COSDictionary) parser.provider().get(new COSObjectKey(3, 0))
                .getCOSObject();
        assertEquals(919, victim.trailer().xrefOffset());
        assertNull(overriddenObj.getDictionaryObject(COSName.ANNOTS));
    }

    @Test
    public void scanMultipleTablesFallsbackOnFullScannerOnWrongObjectOffset() throws IOException
    {
        parser = new COSParser(SeekableSources.inMemorySeekableSourceFrom(getClass()
                .getResourceAsStream("/sambox/test_multiple_xref_tables_wrong_object_offset.pdf")));
        victim = new XrefParser(parser);
        victim.parse();
        assertEquals(108, victim.trailer().getCOSObject().getInt(COSName.PREV));
        assertEquals(8, victim.trailer().getCOSObject().getInt(COSName.SIZE));
        assertNotNull(victim.trailer().getCOSObject().getDictionaryObject(COSName.ROOT));
        COSDictionary overriddenObj = (COSDictionary) parser.provider().get(new COSObjectKey(3, 0))
                .getCOSObject();
        assertEquals(839, victim.trailer().xrefOffset());
        assertNotNull(overriddenObj.getDictionaryObject(COSName.ANNOTS));
    }

    @Test
    public void scanMultipleTablesFallsbackOnFullScannerOnNegativeOffset() throws IOException
    {
        parser = new COSParser(SeekableSources.inMemorySeekableSourceFrom(getClass()
                .getResourceAsStream("/sambox/test_multiple_xref_tables_negative_offset.pdf")));
        victim = new XrefParser(parser);
        victim.parse();
        assertEquals(408, victim.trailer().getCOSObject().getInt(COSName.PREV));
        assertEquals(8, victim.trailer().getCOSObject().getInt(COSName.SIZE));
        assertNotNull(victim.trailer().getCOSObject().getDictionaryObject(COSName.ROOT));
        COSDictionary overriddenObj = (COSDictionary) parser.provider().get(new COSObjectKey(3, 0))
                .getCOSObject();
        assertEquals(839, victim.trailer().xrefOffset());
        assertNotNull(overriddenObj.getDictionaryObject(COSName.ANNOTS));
    }

    @Test
    public void scanMultipleTablesFallsbackOnFullScannerOnWrongOffset() throws IOException
    {
        parser = new COSParser(SeekableSources.inMemorySeekableSourceFrom(getClass()
                .getResourceAsStream("/sambox/test_multiple_xref_tables_wrong_offset.pdf")));
        victim = new XrefParser(parser);
        victim.parse();
        assertEquals(108, victim.trailer().getCOSObject().getInt(COSName.PREV));
        assertEquals(8, victim.trailer().getCOSObject().getInt(COSName.SIZE));
        assertNotNull(victim.trailer().getCOSObject().getDictionaryObject(COSName.ROOT));
        COSDictionary overriddenObj = (COSDictionary) parser.provider().get(new COSObjectKey(3, 0))
                .getCOSObject();
        assertEquals(839, victim.trailer().xrefOffset());
        assertNotNull(overriddenObj.getDictionaryObject(COSName.ANNOTS));
    }

    @Test
    public void scanMissingStartxrefKeyword() throws IOException
    {
        parser = new COSParser(SeekableSources.inMemorySeekableSourceFrom(
                getClass().getResourceAsStream("/sambox/simple_test_missing_startxref.pdf")));
        victim = new XrefParser(parser);
        victim.parse();
        assertEquals(9, victim.trailer().getCOSObject().getInt(COSName.SIZE));
        assertNotNull(victim.trailer().getCOSObject().getDictionaryObject(COSName.ROOT));
        COSDictionary overriddenObj = (COSDictionary) parser.provider().get(new COSObjectKey(3, 0))
                .getCOSObject();
        assertEquals(564, victim.trailer().xrefOffset());
        assertNotNull(overriddenObj.getDictionaryObject(COSName.ANNOTS));
    }

    @Test
    public void scanBogusDictionary() throws IOException
    {
        parser = new COSParser(SeekableSources.inMemorySeekableSourceFrom(getClass()
                .getResourceAsStream("/sambox/test_multiple_xref_tables_bogus_trailer.pdf")));
        victim = new XrefParser(parser);
        victim.parse();
        assertEquals(8, victim.trailer().getCOSObject().getInt(COSName.SIZE));
        assertNotNull(victim.trailer().getCOSObject().getDictionaryObject(COSName.ROOT));
        COSDictionary overriddenObj = (COSDictionary) parser.provider().get(new COSObjectKey(3, 0))
                .getCOSObject();
        assertEquals(839, victim.trailer().xrefOffset());
        assertNotNull(overriddenObj.getDictionaryObject(COSName.ANNOTS));
    }

    @Test
    public void scanBogusPrev() throws IOException
    {
        parser = new COSParser(SeekableSources.inMemorySeekableSourceFrom(getClass()
                .getResourceAsStream("/sambox/test_multiple_xref_tables_bogus_prev.pdf")));
        victim = new XrefParser(parser);
        victim.parse();
        assertEquals(8, victim.trailer().getCOSObject().getInt(COSName.SIZE));
        assertNotNull(victim.trailer().getCOSObject().getDictionaryObject(COSName.ROOT));
        COSDictionary overriddenObj = (COSDictionary) parser.provider().get(new COSObjectKey(3, 0))
                .getCOSObject();
        assertEquals(839, victim.trailer().xrefOffset());
        assertNotNull(overriddenObj.getDictionaryObject(COSName.ANNOTS));
    }

    @Test
    public void scanXRefStm() throws IOException
    {
        parser = new COSParser(SeekableSources.inMemorySeekableSourceFrom(
                getClass().getResourceAsStream("/sambox/test_xref_table_and_XRefStm.pdf")));
        victim = new XrefParser(parser);
        victim.parse();
        assertEquals(9, victim.trailer().getCOSObject().getInt(COSName.SIZE));
        assertNotNull(victim.trailer().getCOSObject().getDictionaryObject(COSName.ROOT));
        COSDictionary overriddenObj = (COSDictionary) parser.provider().get(new COSObjectKey(3, 0))
                .getCOSObject();
        assertEquals(1110, victim.trailer().xrefOffset());
        assertNull(overriddenObj.getDictionaryObject(COSName.ANNOTS));
    }

    @Test
    public void scanInvalidXRefStm() throws IOException
    {
        parser = new COSParser(SeekableSources.inMemorySeekableSourceFrom(
                getClass().getResourceAsStream("/sambox/test_xref_table_and_invalid_XRefStm.pdf")));
        victim = new XrefParser(parser);
        victim.parse();
        assertEquals(9, victim.trailer().getCOSObject().getInt(COSName.SIZE));
        assertNotNull(victim.trailer().getCOSObject().getDictionaryObject(COSName.ROOT));
        COSDictionary overriddenObj = (COSDictionary) parser.provider().get(new COSObjectKey(3, 0))
                .getCOSObject();
        assertEquals(1110, victim.trailer().xrefOffset());
        assertNull(overriddenObj.getDictionaryObject(COSName.ANNOTS));
    }

    @Test
    public void scanMissingOffset() throws IOException
    {
        parser = new COSParser(SeekableSources.inMemorySeekableSourceFrom(
                getClass().getResourceAsStream("/sambox/test_missing_xref_offset.pdf")));
        victim = new XrefParser(parser);
        victim.parse();
        assertEquals(408, victim.trailer().getCOSObject().getInt(COSName.PREV));
        assertEquals(8, victim.trailer().getCOSObject().getInt(COSName.SIZE));
        assertNotNull(victim.trailer().getCOSObject().getDictionaryObject(COSName.ROOT));
        COSDictionary overriddenObj = (COSDictionary) parser.provider().get(new COSObjectKey(3, 0))
                .getCOSObject();
        assertEquals(839, victim.trailer().xrefOffset());
        assertNotNull(overriddenObj.getDictionaryObject(COSName.ANNOTS));
    }

    @Test
    public void scanWrongStartxrefAndMissingXref() throws IOException
    {
        parser = new COSParser(SeekableSources.inMemorySeekableSourceFrom(
                getClass().getResourceAsStream("/sambox/test_xref_issue23.pdf")));
        victim = new XrefParser(parser);
        victim.parse();
        assertEquals(10, victim.trailer().getCOSObject().getInt(COSName.SIZE));
        assertNotNull(victim.trailer().getCOSObject().getDictionaryObject(COSName.ROOT));
        assertEquals(-1, victim.trailer().xrefOffset());
    }

    @Test
    public void scanMissingTrailer() throws IOException
    {
        parser = new COSParser(SeekableSources.inMemorySeekableSourceFrom(
                getClass().getResourceAsStream("/sambox/test_xref_missing_trailer.pdf")));
        victim = new XrefParser(parser);
        victim.parse();
        assertNotNull(victim.trailer().getCOSObject().getDictionaryObject(COSName.ROOT));
        assertEquals(-1, victim.trailer().xrefOffset());
    }
}
