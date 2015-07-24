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
package org.apache.pdfbox.input;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.sejda.io.SeekableSources.inMemorySeekableSourceFrom;

import java.io.IOException;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSBoolean;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSInteger;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSNull;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.util.Charsets;
import org.junit.After;
import org.junit.Test;
import org.sejda.util.IOUtils;

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
    public void nextParsedTokenN() throws IOException
    {
        victim = new ContentStreamCOSParser(
                inMemorySeekableSourceFrom("q\nq\n27.27 823.89 540.738 -783.57 re\nW\nn\n540.73824 0 0 783.569764 27.268676 40.32 cm\n/a0 gs\n/x5 Do\nQ\nQ"
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
        assertEquals(10, victim.position());
    }

    @Test
    public void nextParsedToken() throws IOException
    {
        victim = new ContentStreamCOSParser(
                inMemorySeekableSourceFrom("   3 true false (Chuck Norris) <436875636B204E6f72726973> [10 (A String)] null <</R 10>> +2 /R"
                        .getBytes()));
        assertEquals(COSInteger.THREE, victim.nextParsedToken());
        assertEquals(COSBoolean.TRUE, victim.nextParsedToken());
        assertEquals(COSBoolean.FALSE, victim.nextParsedToken());
        COSString expected = COSString.newInstance("Chuck Norris".getBytes(Charsets.ISO_8859_1));
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
}
