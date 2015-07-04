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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;

import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSObjectKey;
import org.junit.After;
import org.junit.Test;
import org.sejda.io.SeekableSources;
import org.sejda.util.IOUtils;

/**
 * @author Andrea Vacondio
 *
 */
public class XrefFullScannerTest
{

    private XrefFullScanner victim;
    private BaseCOSParser parser;

    @After
    public void tearDown() throws IOException
    {
        IOUtils.close(parser);
    }

    @Test
    public void scanMultipleTables() throws IOException
    {
        parser = new BaseCOSParser(SeekableSources.inMemorySeekableSourceFrom(getClass()
                .getResourceAsStream("/input/test_multiple_xref_tables.pdf")));
        victim = new XrefFullScanner(parser);
        victim.scan();
        assertEquals(406, victim.trailer().getInt(COSName.PREV));
        assertEquals(8, victim.trailer().getInt(COSName.SIZE));
        assertNotNull(victim.trailer().getDictionaryObject(COSName.ROOT));
        COSDictionary overriddenObj = (COSDictionary) parser.provider().get(new COSObjectKey(3, 0))
                .getCOSObject();
        assertNotNull(overriddenObj.getDictionaryObject(COSName.ANNOTS));
    }

    @Test
    public void scanStreamAndTable() throws IOException
    {
        parser = new BaseCOSParser(SeekableSources.inMemorySeekableSourceFrom(getClass()
                .getResourceAsStream("/input/test_xref_stream_and_table.pdf")));
        victim = new XrefFullScanner(parser);
        victim.scan();
        assertEquals(562, victim.trailer().getInt(COSName.PREV));
        assertEquals(8, victim.trailer().getInt(COSName.SIZE));
        assertNotNull(victim.trailer().getDictionaryObject(COSName.ROOT));
        COSDictionary overriddenObj = (COSDictionary) parser.provider().get(new COSObjectKey(3, 0))
                .getCOSObject();
        assertNull(overriddenObj.getDictionaryObject(COSName.ANNOTS));
    }
}
