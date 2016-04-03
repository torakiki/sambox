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
import org.sejda.sambox.input.XrefFullScanner.XrefScanOutcome;
import org.sejda.util.IOUtils;

/**
 * @author Andrea Vacondio
 *
 */
public class XrefFullScannerTest
{

    private XrefFullScanner victim;
    private COSParser parser;

    @After
    public void tearDown() throws IOException
    {
        IOUtils.close(parser);
    }

    @Test
    public void scanMultipleTables() throws IOException
    {
        parser = new COSParser(SeekableSources.inMemorySeekableSourceFrom(
                getClass().getResourceAsStream("/sambox/test_multiple_xref_tables.pdf")));
        victim = new XrefFullScanner(parser);
        assertEquals(XrefScanOutcome.FOUND, victim.scan());
        assertEquals(408, victim.trailer().getInt(COSName.PREV));
        assertEquals(8, victim.trailer().getInt(COSName.SIZE));
        assertNotNull(victim.trailer().getDictionaryObject(COSName.ROOT));
        COSDictionary overriddenObj = (COSDictionary) parser.provider().get(new COSObjectKey(3, 0))
                .getCOSObject();
        assertNotNull(overriddenObj.getDictionaryObject(COSName.ANNOTS));
    }

    @Test
    public void scanStreamAndTable() throws IOException
    {
        parser = new COSParser(SeekableSources.inMemorySeekableSourceFrom(
                getClass().getResourceAsStream("/sambox/test_xref_stream_and_table.pdf")));
        victim = new XrefFullScanner(parser);
        victim.scan();
        assertEquals(562, victim.trailer().getInt(COSName.PREV));
        assertEquals(9, victim.trailer().getInt(COSName.SIZE));
        assertNotNull(victim.trailer().getDictionaryObject(COSName.ROOT));
        COSDictionary overriddenObj = (COSDictionary) parser.provider().get(new COSObjectKey(3, 0))
                .getCOSObject();
        assertNull(overriddenObj.getDictionaryObject(COSName.ANNOTS));
    }

    @Test
    public void missingXref() throws IOException
    {
        parser = new COSParser(SeekableSources.inMemorySeekableSourceFrom(
                getClass().getResourceAsStream("/sambox/test_xref_missing_xref.pdf")));
        victim = new XrefFullScanner(parser);
        assertEquals(XrefScanOutcome.NOT_FOUND, victim.scan());
    }

    @Test
    public void trunkatedXref() throws IOException
    {
        parser = new COSParser(SeekableSources.inMemorySeekableSourceFrom(
                getClass().getResourceAsStream("/sambox/test_trunkated_xref_table.pdf")));
        victim = new XrefFullScanner(parser);
        assertEquals(XrefScanOutcome.WITH_ERRORS, victim.scan());
    }
}
