/*
 * Created on 23/lug/2015
 * Copyright 2015 by Andrea Vacondio (andrea.vacondio@gmail.com).
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * 
 * http://www.apache.org/licenses/LICENSE-2.0 
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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.sejda.io.SeekableSources.inMemorySeekableSourceFrom;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.After;
import org.junit.Test;
import org.sejda.commons.util.IOUtils;
import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSBoolean;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSInteger;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSNull;
import org.sejda.sambox.cos.COSString;

/**
 * @author Andrea Vacondio
 *
 */
public class ContentStreamCOSParserTest
{
    private ContentStreamCOSParser victim;

    @After
    public void tearDown() throws Exception
    {
        IOUtils.close(victim);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullArgument()
    {
        new ContentStreamCOSParser(null);
    }

    @Test
    public void nextParsedTokenNull() throws IOException
    {
        victim = new ContentStreamCOSParser(
                inMemorySeekableSourceFrom("53 0 obj <</key null>>".getBytes()));
        victim.position(16);
        assertEquals(COSNull.NULL, victim.nextParsedToken());
    }

    @Test
    public void nextDictionaryBadValue() throws IOException
    {
        victim = new ContentStreamCOSParser(
                inMemorySeekableSourceFrom("<< /R Chuck >>".getBytes()));
        COSDictionary result = victim.nextDictionary();
        assertNull(result.getItem(COSName.R));
    }

    @Test
    public void nextParsedTokenN() throws IOException
    {
        victim = new ContentStreamCOSParser(inMemorySeekableSourceFrom(
                "q\nq\n27.27 823.89 540.738 -783.57 re\nW\nn\n540.73824 0 0 783.569764 27.268676 40.32 cm\n/a0 gs\n/x5 Do\nQ\nQ"
                        .getBytes()));
        victim.position(38);
        assertNull(victim.nextParsedToken());
    }

    @Test
    public void nextParsedTokenTF() throws IOException
    {
        victim = new ContentStreamCOSParser(inMemorySeekableSourceFrom("something t ".getBytes()));
        victim.position(10);
        assertNull(victim.nextParsedToken());
        assertEquals(11, victim.position());
    }

    @Test
    public void nextParsedToken() throws IOException
    {
        victim = new ContentStreamCOSParser(inMemorySeekableSourceFrom(
                "   3 true false (Chuck Norris) <436875636B204E6f72726973> [10 (A String)] null <</R 10>> +2 /R"
                        .getBytes()));
        assertEquals(COSInteger.THREE, victim.nextParsedToken());
        assertEquals(COSBoolean.TRUE, victim.nextParsedToken());
        assertEquals(COSBoolean.FALSE, victim.nextParsedToken());
        COSString expected = COSString
                .newInstance("Chuck Norris".getBytes(StandardCharsets.ISO_8859_1));
        assertEquals(expected, victim.nextParsedToken());
        expected.setForceHexForm(true);
        assertEquals(expected, victim.nextParsedToken());
        COSBase array = victim.nextParsedToken();
        assertThat(array, is(instanceOf(COSArray.class)));
        assertEquals(COSNull.NULL, victim.nextParsedToken());
        assertThat(victim.nextParsedToken(), is(instanceOf(COSDictionary.class)));
        assertEquals(COSInteger.TWO, victim.nextParsedToken());
        assertEquals(COSName.R, victim.nextParsedToken());
        assertNull(victim.nextParsedToken());
    }

    @Test
    public void nextParsedTokenNumberOrIndirectReferenceObj() throws IOException
    {
        victim = new ContentStreamCOSParser(inMemorySeekableSourceFrom("2 0 R".getBytes()));
        assertEquals(COSInteger.TWO, victim.nextParsedToken());
        assertEquals(COSInteger.ZERO, victim.nextParsedToken());
        assertNull(victim.nextParsedToken());
    }

    @Test
    public void nextParsedTokenInvalidTokenInArray() throws IOException
    {
        victim = new ContentStreamCOSParser(
                inMemorySeekableSourceFrom(new byte[] { 0x5B, 0x28, 0x54, 0x29, 0x20, 0x30, 0x2E,
                        0x30, 0x20, 0x54, 0x63, 0x0A, 0x2D, 0x32, 0x2E, 0x30, 0x20, 0x5D }));
        COSBase array = victim.nextParsedToken();
        assertThat(array, is(instanceOf(COSArray.class)));
        assertEquals(3, ((COSArray) array).size());
    }
}
