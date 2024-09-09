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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.sejda.io.SeekableSources.inMemorySeekableSourceFrom;

import java.io.IOException;

import org.junit.After;
import org.junit.Test;
import org.sejda.commons.util.IOUtils;
import org.sejda.io.SeekableSource;
import org.sejda.io.SeekableSources;
import org.sejda.sambox.cos.COSObjectKey;

/**
 * @author Andrea Vacondio
 *
 */
public class SourceReaderTest
{
    private SourceReader victim;

    @After
    public void tearDown() throws Exception
    {
        IOUtils.close(victim);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullArgument()
    {
        new SourceReader(null);
    }

    @Test
    public void position() throws IOException
    {
        victim = new SourceReader(
                inMemorySeekableSourceFrom(new byte[] { '1', ' ', '2', '2', '1' }));
        assertEquals(0, victim.position());
        assertEquals(1, victim.readInt());
        assertEquals(1, victim.position());
        victim.position(3);
        assertEquals(3, victim.position());
    }

    @Test
    public void length()
    {
        victim = new SourceReader(
                inMemorySeekableSourceFrom(new byte[] { '1', ' ', '2', '2', '1' }));
        assertEquals(5, victim.length());
    }

    @Test
    public void skipExpected() throws IOException
    {
        victim = new SourceReader(inMemorySeekableSourceFrom("Chuck Norris".getBytes()));
        victim.skipExpected("Chuck");
        assertEquals(5, victim.position());
    }

    @Test(expected = IOException.class)
    public void failingSkipExpected() throws IOException
    {
        victim = new SourceReader(inMemorySeekableSourceFrom("Chuck Norris".getBytes()));
        victim.skipExpected("Norris");
    }

    @Test
    public void skipExpectedChar() throws IOException
    {
        victim = new SourceReader(inMemorySeekableSourceFrom("Chuck Norris".getBytes()));
        victim.skipExpected('C');
        assertEquals(1, victim.position());
    }

    @Test(expected = IOException.class)
    public void failingSkipExpectedChar() throws IOException
    {
        victim = new SourceReader(inMemorySeekableSourceFrom("Chuck Norris".getBytes()));
        victim.skipExpected('c');
    }

    @Test
    public void skipToken() throws IOException
    {
        victim = new SourceReader(inMemorySeekableSourceFrom("Chuck Norris".getBytes()));
        assertTrue(victim.skipTokenIfValue("Chuck"));
        assertEquals(5, victim.position());
    }

    @Test
    public void offset() throws IOException
    {
        victim = new SourceReader(inMemorySeekableSourceFrom("XXXChuck Norris".getBytes()));
        victim.offset(3);
        assertTrue(victim.skipTokenIfValue("Chuck"));
        assertEquals(5, victim.position());
    }

    @Test
    public void skipTokenFailing() throws IOException
    {
        victim = new SourceReader(inMemorySeekableSourceFrom("Chuck Norris".getBytes()));
        victim.position(5);
        assertFalse(victim.skipTokenIfValue("Segal"));
        assertEquals(5, victim.position());
    }

    @Test
    public void skipIndirectObjectDefinition() throws IOException
    {
        victim = new SourceReader(inMemorySeekableSourceFrom("10 0 obj <</key value>>".getBytes()));
        victim.skipIndirectObjectDefinition();
        assertEquals(8, victim.position());
    }

    @Test
    public void skipIndirectObjectDefinitionSpaceAfterObjectNumber() throws IOException
    {
        victim = new SourceReader(
                inMemorySeekableSourceFrom("84        0 obj <</key value>>".getBytes()));
        victim.skipIndirectObjectDefinition();
        assertEquals(15, victim.position());
    }

    @Test
    public void skipIndirectObjectDefinitionSpaceAfterGenerationNumber() throws IOException
    {
        victim = new SourceReader(
                inMemorySeekableSourceFrom("84 0        obj <</key value>>".getBytes()));
        victim.skipIndirectObjectDefinition();
        assertEquals(15, victim.position());
    }

    @Test(expected = IOException.class)
    public void skipFailingIndirectObjectDefinition() throws IOException
    {
        victim = new SourceReader(inMemorySeekableSourceFrom("<</key value>>".getBytes()));
        victim.skipIndirectObjectDefinition();
    }

    @Test
    public void skipExpectedIndirectObjectDefinition() throws IOException
    {
        victim = new SourceReader(inMemorySeekableSourceFrom("10 0 obj <</key value>>".getBytes()));
        victim.skipExpectedIndirectObjectDefinition(new COSObjectKey(10, 0));
        assertEquals(8, victim.position());
    }

    @Test
    public void skipExpectedIndirectObjectDefinitionSpaceAfterObjectNumber() throws IOException
    {
        victim = new SourceReader(
                inMemorySeekableSourceFrom("84        0 obj <</key value>>".getBytes()));
        victim.skipExpectedIndirectObjectDefinition(new COSObjectKey(84, 0));
        assertEquals(15, victim.position());
    }

    @Test
    public void skipExpectedIndirectObjectDefinitionSpaceAfterGenerationNumber() throws IOException
    {
        victim = new SourceReader(
                inMemorySeekableSourceFrom("84 0        obj <</key value>>".getBytes()));
        victim.skipExpectedIndirectObjectDefinition(new COSObjectKey(84, 0));
        assertEquals(15, victim.position());
    }

    @Test(expected = IOException.class)
    public void skipExpectedIndirectObjectDefinitionFailingObjNumber() throws IOException
    {
        victim = new SourceReader(inMemorySeekableSourceFrom("10 0 obj <</key value>>".getBytes()));
        victim.skipExpectedIndirectObjectDefinition(new COSObjectKey(11, 0));
    }

    @Test(expected = IOException.class)
    public void skipExpectedIndirectObjectDefinitionFailingGenNumber() throws IOException
    {
        victim = new SourceReader(inMemorySeekableSourceFrom("10 0 obj <</key value>>".getBytes()));
        victim.skipExpectedIndirectObjectDefinition(new COSObjectKey(10, 1));
    }

    @Test
    public void readToken() throws IOException
    {
        victim = new SourceReader(inMemorySeekableSourceFrom("10 0 obj <</key value>>".getBytes()));
        victim.position(16);
        assertEquals("value", victim.readToken());
    }

    @Test
    public void readTokenSkipSpaces() throws IOException
    {
        victim = new SourceReader(inMemorySeekableSourceFrom("10 0 obj <</key value>>".getBytes()));
        victim.position(15);
        assertEquals("value", victim.readToken());
    }

    @Test
    public void readTokenEOF() throws IOException
    {
        victim = new SourceReader(inMemorySeekableSourceFrom("10 0 obj value".getBytes()));
        victim.position(9);
        assertEquals("value", victim.readToken());
    }

    @Test
    public void isNextToken() throws IOException
    {
        victim = new SourceReader(inMemorySeekableSourceFrom("10 0 obj <</key value>>".getBytes()));
        victim.position(15);
        assertTrue(victim.isNextToken("steven", "value"));
        assertEquals(15, victim.position());
    }

    @Test
    public void isNextTokenFailing() throws IOException
    {
        victim = new SourceReader(inMemorySeekableSourceFrom("10 0 obj <</key value>>".getBytes()));
        victim.position(16);
        assertFalse(victim.isNextToken("steven", "value2"));
    }

    @Test
    public void readObjectNumber() throws IOException
    {
        victim = new SourceReader(inMemorySeekableSourceFrom("10 0 obj <</key value>>".getBytes()));
        assertEquals(10, victim.readObjectNumber());
    }

    @Test
    public void readGenerationNumber() throws IOException
    {
        victim = new SourceReader(inMemorySeekableSourceFrom("10 2 obj <</key value>>".getBytes()));
        victim.position(2);
        assertEquals(2, victim.readGenerationNumber());
    }

    @Test(expected = IOException.class)
    public void readObjectNumberFailing() throws IOException
    {
        victim = new SourceReader(
                inMemorySeekableSourceFrom("12345678912 0 obj <</key value>>".getBytes()));
        victim.readObjectNumber();
    }

    @Test(expected = IOException.class)
    public void readGenerationNumberFailing() throws IOException
    {
        victim = new SourceReader(
                inMemorySeekableSourceFrom("123456 2 obj <</key value>>".getBytes()));
        victim.readGenerationNumber();
    }

    @Test(expected = IOException.class)
    public void readObjectNumberNegative() throws IOException
    {
        victim = new SourceReader(
                inMemorySeekableSourceFrom("-10 0 obj <</key value>>".getBytes()));
        victim.readObjectNumber();
    }

    @Test(expected = IOException.class)
    public void readGenerationNumberNegative() throws IOException
    {
        victim = new SourceReader(
                inMemorySeekableSourceFrom("10 -2 obj <</key value>>".getBytes()));
        victim.position(2);
        victim.readGenerationNumber();
    }

    @Test
    public void readLong() throws IOException
    {
        String data = Long.MAX_VALUE + " 0 obj <</key value>>";
        SeekableSource source = SeekableSources.inMemorySeekableSourceFrom(data.getBytes());
        victim = new SourceReader(source);
        assertEquals(Long.MAX_VALUE, victim.readLong());
    }

    @Test(expected = IOException.class)
    public void readLongFailing() throws IOException
    {
        victim = new SourceReader(inMemorySeekableSourceFrom("10 0 obj <</key value>>".getBytes()));
        victim.position(16);
        victim.readLong();
    }

    @Test
    public void readInt() throws IOException
    {
        String data = Integer.MAX_VALUE + " 0 obj <</key value>>";
        SeekableSource source = SeekableSources.inMemorySeekableSourceFrom(data.getBytes());
        victim = new SourceReader(source);
        assertEquals(Integer.MAX_VALUE, victim.readInt());
    }

    @Test(expected = IOException.class)
    public void readIntFailing() throws IOException
    {
        victim = new SourceReader(inMemorySeekableSourceFrom("10 0 obj <</key value>>".getBytes()));
        victim.position(16);
        victim.readInt();
    }

    @Test
    public void readIntegerNumber() throws IOException
    {
        victim = new SourceReader(inMemorySeekableSourceFrom("53 0 obj <</key value>>".getBytes()));
        assertEquals("53", victim.readIntegerNumber());
    }

    @Test
    public void readSignedIntegerNumber() throws IOException
    {
        victim = new SourceReader(
                inMemorySeekableSourceFrom("-53 0 obj <</key value>>".getBytes()));
        assertEquals("-53", victim.readIntegerNumber());
    }

    @Test
    public void readIntegerNumberFailing() throws IOException
    {
        victim = new SourceReader(inMemorySeekableSourceFrom("ChuckNorris".getBytes()));
        assertEquals("", victim.readIntegerNumber());
    }

    @Test
    public void readNumber() throws IOException
    {
        victim = new SourceReader(inMemorySeekableSourceFrom("53.34".getBytes()));
        assertEquals("53.34", victim.readNumber());
    }

    @Test
    public void readNumberPoint() throws IOException
    {
        victim = new SourceReader(inMemorySeekableSourceFrom(".002".getBytes()));
        assertEquals(".002", victim.readNumber());
    }

    @Test
    public void readNumberAttached() throws IOException
    {
        victim = new SourceReader(inMemorySeekableSourceFrom("53.34ABCDE".getBytes()));
        assertEquals("53.34", victim.readNumber());
    }

    @Test
    public void readSignedNumber() throws IOException
    {
        victim = new SourceReader(inMemorySeekableSourceFrom("-.002".getBytes()));
        assertEquals("-.002", victim.readNumber());
    }

    @Test
    public void readPostScriptNumber() throws IOException
    {
        victim = new SourceReader(inMemorySeekableSourceFrom("6.02E23".getBytes()));
        assertEquals("6.02E23", victim.readNumber());
    }

    @Test
    public void readPostScriptNumberNegativeExponent() throws IOException
    {
        victim = new SourceReader(inMemorySeekableSourceFrom("6.10351563e-003".getBytes()));
        assertEquals("6.10351563e-003", victim.readNumber());
    }

    @Test
    public void readPostScriptNumberPositiveExponent() throws IOException
    {
        victim = new SourceReader(inMemorySeekableSourceFrom("6.10351563e+003".getBytes()));
        assertEquals("6.10351563e+003", victim.readNumber());
    }

    @Test
    public void readNumberEmpty() throws IOException
    {
        victim = new SourceReader(inMemorySeekableSourceFrom(new byte[0]));
        assertEquals("", victim.readNumber());
    }

    @Test
    public void readNumberFailing() throws IOException
    {
        victim = new SourceReader(inMemorySeekableSourceFrom("ChuckNorris".getBytes()));
        assertEquals("", victim.readNumber());
    }

    @Test
    public void readLineToEOF() throws IOException
    {
        victim = new SourceReader(inMemorySeekableSourceFrom("ChuckNorris".getBytes()));
        assertEquals("ChuckNorris", victim.readLine());
    }

    @Test
    public void readLineCR() throws IOException
    {
        victim = new SourceReader(
                inMemorySeekableSourceFrom("ChuckNorris\rStevenSegal".getBytes()));
        assertEquals("ChuckNorris", victim.readLine());
        assertEquals(12, victim.position());
    }

    @Test
    public void readLineCRLF() throws IOException
    {
        victim = new SourceReader(
                inMemorySeekableSourceFrom("ChuckNorris\r\nStevenSegal".getBytes()));
        assertEquals("ChuckNorris", victim.readLine());
        assertEquals(13, victim.position());
    }

    @Test(expected = IOException.class)
    public void readLineFailing() throws IOException
    {
        victim = new SourceReader(inMemorySeekableSourceFrom(new byte[0]));
        victim.readLine();
    }

    @Test
    public void readNameVariousCharsEOF() throws IOException
    {
        victim = new SourceReader(
                inMemorySeekableSourceFrom("/A;Name_With-Various***Characters?".getBytes()));
        assertEquals("A;Name_With-Various***Characters?", victim.readName());
    }

    @Test
    public void readNameNumberSign() throws IOException
    {
        victim = new SourceReader(inMemorySeekableSourceFrom("/Lime#20Green theValue".getBytes()));
        assertEquals("Lime Green", victim.readName());
    }

    @Test
    public void readNameNumberSignNumberSign() throws IOException
    {
        victim = new SourceReader(inMemorySeekableSourceFrom("/The_Key_of_F#23_Minor".getBytes()));
        assertEquals("The_Key_of_F#_Minor", victim.readName());
    }

    @Test(expected = IOException.class)
    public void readNameNumberSignUnexpectedEOF() throws IOException
    {
        victim = new SourceReader(inMemorySeekableSourceFrom("/The_Key_of_F#2".getBytes()));
        victim.readName();
    }

    @Test
    public void readNameNumberSignForStandardChar() throws IOException
    {
        victim = new SourceReader(inMemorySeekableSourceFrom("/A#42".getBytes()));
        assertEquals("AB", victim.readName());
    }

    @Test(expected = IOException.class)
    public void readNameFailingNoData() throws IOException
    {
        victim = new SourceReader(inMemorySeekableSourceFrom(new byte[0]));
        victim.readName();
    }

    @Test(expected = IOException.class)
    public void readNameFailingNotName() throws IOException
    {
        victim = new SourceReader(inMemorySeekableSourceFrom("ChuckNorris".getBytes()));
        victim.readName();
    }

    @Test
    public void readNameWorkaroundForNumberSigned() throws IOException
    {
        victim = new SourceReader(inMemorySeekableSourceFrom("/ChuckNorris#3H".getBytes()));
        assertEquals("ChuckNorris#3H", victim.readName());
    }

    @Test
    public void readHexString() throws IOException
    {
        victim = new SourceReader(
                inMemorySeekableSourceFrom("<4E6F762073686D6F7A206B6120706F702E>".getBytes()));
        assertEquals("4E6F762073686D6F7A206B6120706F702E", victim.readHexString());
    }

    @Test(expected = IOException.class)
    public void readHexStringEOF() throws IOException
    {
        victim = new SourceReader(
                inMemorySeekableSourceFrom("<4E6F762073686D6F7A206B61".getBytes()));
        victim.readHexString();
    }

    @Test
    public void readHexStringWorkaround() throws IOException
    {
        victim = new SourceReader(inMemorySeekableSourceFrom("<4EABZ2>".getBytes()));
        assertEquals("4EAB02", victim.readHexString());
    }

    @Test
    public void readHexStringWithWhitespaces() throws IOException
    {
        victim = new SourceReader(
                inMemorySeekableSourceFrom("<4E6F7620736 86D6F7A\n206B6120706F\r702E>".getBytes()));
        assertEquals("4E6F762073686D6F7A206B6120706F702E", victim.readHexString());
    }

    @Test
    public void readLiteral() throws IOException
    {
        victim = new SourceReader(inMemorySeekableSourceFrom("(Chuck Norris)".getBytes()));
        assertEquals("Chuck Norris", victim.readLiteralString());
    }

    @Test
    public void readLiteralEOF() throws IOException
    {
        victim = new SourceReader(inMemorySeekableSourceFrom("(Chuck Norris".getBytes()));
        assertEquals("Chuck Norris", victim.readLiteralString());
    }

    @Test
    public void readLiteralMultipleBraces() throws IOException
    {
        victim = new SourceReader(
                inMemorySeekableSourceFrom("(Steven (Frederic) Seagal)".getBytes()));
        assertEquals("Steven (Frederic) Seagal", victim.readLiteralString());
    }

    @Test
    public void readLiteralSpecialChar() throws IOException
    {
        victim = new SourceReader(
                inMemorySeekableSourceFrom("(Chuck\\r\\nNo\\tr\\bri\\fs)".getBytes()));
        assertEquals("Chuck\r\nNo\tr\bri\fs", victim.readLiteralString());
    }

    @Test
    public void readLiteralSpecialChar2() throws IOException
    {
        victim = new SourceReader(
                inMemorySeekableSourceFrom("(Steven \\(Frederic\\) Seagal)".getBytes()));
        assertEquals("Steven (Frederic) Seagal", victim.readLiteralString());
    }

    @Test
    public void readLiteralOctal() throws IOException
    {
        victim = new SourceReader(inMemorySeekableSourceFrom(
                "(This string contains \\245two octal characters\\307.)".getBytes()));
        assertEquals("This string contains ¥two octal charactersÇ.", victim.readLiteralString());
    }

    @Test
    public void readLiteralDropUnusedSolidus() throws IOException
    {
        victim = new SourceReader(inMemorySeekableSourceFrom(
                "(This string contains \\935invalid octal.)".getBytes()));
        assertEquals("This string contains 935invalid octal.", victim.readLiteralString());
    }

    @Test
    public void readLiteralOneDigitOctal() throws IOException
    {
        victim = new SourceReader(
                inMemorySeekableSourceFrom("(This string contains \\293short octal.)".getBytes()));
        assertEquals("This string contains \u000293short octal.", victim.readLiteralString());
    }

    @Test
    public void readLiteralTwoDigitsOctal() throws IOException
    {
        victim = new SourceReader(
                inMemorySeekableSourceFrom("(This string contains \\539short octal.)".getBytes()));
        assertEquals("This string contains +9short octal.", victim.readLiteralString());
    }

    @Test
    public void readLiteralSolidusLineBreak() throws IOException
    {
        victim = new SourceReader(
                inMemorySeekableSourceFrom("(These \\\ntwo strings \\\nare the same.)".getBytes()));
        assertEquals("These two strings are the same.", victim.readLiteralString());
    }

    @Test
    public void readLiteralLF() throws IOException
    {
        victim = new SourceReader(inMemorySeekableSourceFrom("(These has \nCR)".getBytes()));
        assertEquals("These has \nCR", victim.readLiteralString());
    }

    @Test
    public void readLiteralCR() throws IOException
    {
        victim = new SourceReader(inMemorySeekableSourceFrom("(These has \rCR)".getBytes()));
        assertEquals("These has \nCR", victim.readLiteralString());
    }

    @Test
    public void readLiteralCRLF() throws IOException
    {
        victim = new SourceReader(inMemorySeekableSourceFrom("(These has \r\nCR)".getBytes()));
        assertEquals("These has \nCR", victim.readLiteralString());
    }

    @Test
    public void skipSpaces() throws IOException
    {
        victim = new SourceReader(
                inMemorySeekableSourceFrom("Lets \u0000\t\f\n\r skip spaces".getBytes()));
        victim.readToken();
        assertEquals(4, victim.position());
        victim.skipSpaces();
        assertEquals(11, victim.position());
    }

    @Test
    public void skipComments() throws IOException
    {
        victim = new SourceReader(
                inMemorySeekableSourceFrom("%this is a comment\nThis is not".getBytes()));
        victim.skipSpaces();
        assertEquals(19, victim.position());
    }

    @Test
    public void unreadIfValid() throws IOException
    {
        victim = new SourceReader(inMemorySeekableSourceFrom("some data".getBytes()));
        victim.position(5);
        int c = victim.source().read();
        victim.position(6);
        victim.unreadIfValid(c);
        assertEquals(5, victim.position());
        victim.position(9);
        c = victim.source().read();
        victim.position(9);
        victim.unreadIfValid(c);
        assertEquals(9, victim.position());
    }

    @Test
    public void unreadSpaces() throws IOException
    {
        victim = new SourceReader(
                inMemorySeekableSourceFrom("Lets \u0000\t\f\n\r skip spaces".getBytes()));
        victim.position(11);
        victim.unreadSpaces();
        assertEquals(4, victim.position());
        victim.position(13);
        victim.unreadSpaces();
        assertEquals(13, victim.position());
    }

    @Test
    public void unreadUntilSpaces() throws IOException
    {
        victim = new SourceReader(
                inMemorySeekableSourceFrom("Lets \u0000\t\f\n\r skip spaces".getBytes()));
        victim.position(11);
        victim.unreadUntilSpaces();
        assertEquals(11, victim.position());
        victim.position(13);
        victim.unreadUntilSpaces();
        assertEquals(11, victim.position());
    }

    @Test
    public void readNumberWithDoubleNegative() throws IOException
    {
        victim = new SourceReader(inMemorySeekableSourceFrom("--5".getBytes()));
        assertEquals("-5", victim.readNumber());
    }

    @Test
    public void readNumberWithNegativeSignInTheMiddle() throws IOException
    {
        victim = new SourceReader(inMemorySeekableSourceFrom("0.00-50".getBytes()));
        assertEquals("0.0050", victim.readNumber());
    }
}
