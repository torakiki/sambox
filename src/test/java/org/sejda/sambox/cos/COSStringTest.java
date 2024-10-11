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
package org.sejda.sambox.cos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import org.junit.Test;
import org.sejda.sambox.TestUtils;

/**
 * @author Andrea Vacondio
 *
 */
public class COSStringTest
{
    @Test
    public void equals()
    {
        TestUtils.testEqualsAndHashCodes(COSString.parseLiteral("Chuck"),
                COSString.parseLiteral("Chuck"), COSString.parseLiteral("Chuck"),
                COSString.parseLiteral("Norris"));
    }

    @Test
    public void fromHex() throws IOException
    {
        String expected = "Quick and simple test";
        String hexForm = createHex(expected);
        COSString victim = COSString.parseHex(hexForm);
        assertTrue(victim.isForceHexForm());
        assertEquals("Quick and simple test", new String(victim.getBytes()));
    }

    @Test(expected = IOException.class)
    public void failingParseHex() throws IOException
    {
        String hexForm = createHex("Quick and simple test");
        COSString.parseHex(hexForm + "xx");
    }

    @Test
    public void compareFromHexString() throws IOException
    {
        COSString test1 = COSString.parseHex("000000FF000000");
        COSString test2 = COSString.parseHex("000000FF00FFFF");
        assertNotEquals(test1.toHexString(), test2.toHexString());
    }

    @Test
    public void hexOddDigits() throws IOException
    {
        COSString test1 = COSString.parseHex("000000FF000000");
        COSString test2 = COSString.parseHex("000000FF00000");
        assertEquals(test1.toHexString(), test2.toHexString());
    }

    @Test
    public void unicodeParseLiteral()
    {
        String theString = "\u4e16";
        assertTrue(COSString.parseLiteral(theString).getString().equals(theString));
    }

    @Test
    public void asciiParseLiteral() throws UnsupportedEncodingException
    {
        String textAscii = "This is some regular text. It should all be expressable in ASCII";
        String stringAscii = COSString.parseLiteral(textAscii).getString();
        assertTrue(stringAscii.equals(textAscii));
        // should be stored as ISO-8859-1 because they only contain chars in the range 0..255
        assertEquals(textAscii, new String(stringAscii.getBytes(), StandardCharsets.ISO_8859_1));
    }

    @Test
    public void unitocde8BitsParseLiteral() throws UnsupportedEncodingException
    {
        /** En français où les choses sont accentués. En español, así */
        String text8Bit = "En fran\u00e7ais o\u00f9 les choses sont accentu\u00e9s. En espa\u00f1ol, as\u00ed";
        COSString string8Bit = COSString.parseLiteral(text8Bit);
        assertTrue(string8Bit.getString().equals(text8Bit));
        assertEquals(text8Bit, new String(string8Bit.getBytes(), StandardCharsets.ISO_8859_1));
    }

    @Test
    public void unitocdeHighBitsParseLiteral() throws UnsupportedEncodingException
    {
        // をクリックしてく
        String textHighBits = "\u3092\u30af\u30ea\u30c3\u30af\u3057\u3066\u304f";
        COSString stringHighBits = COSString.parseLiteral(textHighBits);
        assertTrue(stringHighBits.getString().equals(textHighBits));
        // The japanese text contains high bits so must be stored as big endian UTF-16
        assertEquals(textHighBits, new String(stringHighBits.getBytes(), "UnicodeBig"));
    }

    @Test
    public void bytes()
    {
        byte[] bytes = "TheString".getBytes();
        COSString other = COSString.parseLiteral("A string");
        other.setValue(bytes);
        assertEquals(COSString.newInstance(bytes), other);
    }

    @Test
    public void nullSafe()
    {
        assertNull(COSString.parseLiteral(null));
    }

    @Test
    public void negativeForceHex()
    {
        String value = "A string without Line feed";
        assertFalse(new COSString(value.getBytes()).isForceHexForm());
    }

    @Test
    public void forceHexIfNonASCII()
    {
        String value = "A string with \n Line feed";
        assertTrue(new COSString(value.getBytes()).isForceHexForm());
    }

    @Test
    public void forceHexIfNonASCIIAgain()
    {
        assertTrue(new COSString(new byte[] { -1, 3, 4, 5 }).isForceHexForm());
    }

    private static String createHex(String str)
    {
        StringBuilder sb = new StringBuilder();
        for (char c : str.toCharArray())
        {
            sb.append(Integer.toString(c, 16));
        }
        return sb.toString().toUpperCase();
    }

    /**
     * PDFBOX-3881: Test that if String has only the BOM, that it be an empty string.
     * 
     * @throws IOException
     */
    @Test
    public void testEmptyStringWithBOM() throws IOException
    {
        assertTrue(COSString.parseHex("FEFF").getString().isEmpty());
        assertTrue(COSString.parseHex("FFFE").getString().isEmpty());
    }
}
