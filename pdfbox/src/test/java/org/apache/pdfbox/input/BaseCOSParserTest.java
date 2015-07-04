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
import static org.sejda.io.SeekableSources.inMemorySeekableSourceFrom;

import java.io.IOException;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBoolean;
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
public class BaseCOSParserTest
{

    private BaseCOSParser victim;

    @After
    public void tearDown() throws Exception
    {
        IOUtils.close(victim);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullArgument()
    {
        new BaseCOSParser(null);
    }

    @Test
    public void nextNull() throws IOException
    {
        victim = new BaseCOSParser(inMemorySeekableSourceFrom("53 0 obj <</key null>>".getBytes()));
        victim.position(16);
        assertEquals(COSNull.NULL, victim.nextNull());
    }

    @Test(expected = IOException.class)
    public void nextNullFailing() throws IOException
    {
        victim = new BaseCOSParser(inMemorySeekableSourceFrom("53 0 obj <</key value>>".getBytes()));
        victim.position(16);
        victim.nextNull();
    }

    @Test
    public void nextBooleanTrue() throws IOException
    {
        victim = new BaseCOSParser(inMemorySeekableSourceFrom("53 0 obj <</key true>>".getBytes()));
        victim.position(16);
        assertEquals(COSBoolean.TRUE, victim.nextBoolean());
    }

    @Test
    public void nextBooleanFalse() throws IOException
    {
        victim = new BaseCOSParser(inMemorySeekableSourceFrom("53 0 obj <</key false>>".getBytes()));
        victim.position(16);
        assertEquals(COSBoolean.FALSE, victim.nextBoolean());
    }

    @Test(expected = IOException.class)
    public void nextBooleanFailing() throws IOException
    {
        victim = new BaseCOSParser(inMemorySeekableSourceFrom("53 0 obj <</key value>>".getBytes()));
        victim.position(16);
        victim.nextBoolean();
    }

    @Test
    public void nextName() throws IOException
    {
        victim = new BaseCOSParser(
                inMemorySeekableSourceFrom("53 0 obj <</Creator me>>".getBytes()));
        victim.position(11);
        assertEquals(COSName.CREATOR, victim.nextName());
    }

    @Test
    public void nextCustomName() throws IOException
    {
        victim = new BaseCOSParser(inMemorySeekableSourceFrom("53 0 obj <</Chuck me>>".getBytes()));
        victim.position(11);
        assertEquals(COSName.getPDFName("Chuck"), victim.nextName());
    }

    @Test
    public void nextLiteral() throws IOException
    {
        victim = new BaseCOSParser(inMemorySeekableSourceFrom("(Chuck Norris)".getBytes()));
        assertEquals(COSString.newInstance("Chuck Norris".getBytes(Charsets.ISO_8859_1)),
                victim.nextLiteralString());
    }

    @Test
    public void nextHexString() throws IOException
    {
        victim = new BaseCOSParser(
                inMemorySeekableSourceFrom("<436875636B204E6f72726973>".getBytes()));
        COSString expected = COSString.newInstance("Chuck Norris".getBytes(Charsets.ISO_8859_1));
        expected.setForceHexForm(true);
        assertEquals(expected, victim.nextHexadecimalString());
    }

    @Test
    public void nextString() throws IOException
    {
        victim = new BaseCOSParser(
                inMemorySeekableSourceFrom("(Chuck Norris) <436875636B204E6f72726973>".getBytes()));
        assertEquals(COSString.newInstance("Chuck Norris".getBytes(Charsets.ISO_8859_1)),
                victim.nextString());
        victim.skipSpaces();
        COSString expected = COSString.newInstance("Chuck Norris".getBytes(Charsets.ISO_8859_1));
        expected.setForceHexForm(true);
        assertEquals(expected, victim.nextString());
    }

    @Test(expected = IOException.class)
    public void nextHexStringFailing() throws IOException
    {
        victim = new BaseCOSParser(inMemorySeekableSourceFrom("53 0 obj <</Chuck me>>".getBytes()));
        victim.nextString();
    }

    @Test
    public void nextArray() throws IOException
    {
        victim = new BaseCOSParser(inMemorySeekableSourceFrom("[10 (A String)]".getBytes()));
        COSArray result = victim.nextArray();
        assertEquals(10, result.getInt(0));
        assertEquals(COSString.newInstance("A String".getBytes(Charsets.ISO_8859_1)), result.get(1));
    }

}
