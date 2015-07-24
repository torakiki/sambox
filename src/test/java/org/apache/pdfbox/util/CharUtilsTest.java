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
package org.apache.pdfbox.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * @author Andrea Vacondio
 *
 */
public class CharUtilsTest
{

    @Test
    public void isEndOfName()
    {
        assertFalse(CharUtils.isEndOfName('b'));
        assertTrue(CharUtils.isEndOfName('('));
        assertTrue(CharUtils.isEndOfName(')'));
        assertTrue(CharUtils.isEndOfName('/'));
        assertTrue(CharUtils.isEndOfName('['));
        assertTrue(CharUtils.isEndOfName('<'));
        assertTrue(CharUtils.isEndOfName('>'));
        assertTrue(CharUtils.isEndOfName(']'));
        assertTrue(CharUtils.isEndOfName(CharUtils.ASCII_HORIZONTAL_TAB));
        assertTrue(CharUtils.isEndOfName(CharUtils.ASCII_CARRIAGE_RETURN));
        assertTrue(CharUtils.isEndOfName(CharUtils.ASCII_LINE_FEED));
        assertTrue(CharUtils.isEndOfName((byte) 32));
    }

    @Test
    public void isEOF()
    {
        assertFalse(CharUtils.isEOF('b'));
        assertTrue(CharUtils.isEOF(-1));
    }

    @Test
    public void isEOL()
    {
        assertFalse(CharUtils.isEOL('b'));
        assertTrue(CharUtils.isEOL(CharUtils.ASCII_CARRIAGE_RETURN));
        assertTrue(CharUtils.isEOL(CharUtils.ASCII_LINE_FEED));
    }

    @Test
    public void isLineFeed()
    {
        assertFalse(CharUtils.isLineFeed(CharUtils.ASCII_CARRIAGE_RETURN));
        assertTrue(CharUtils.isLineFeed(CharUtils.ASCII_LINE_FEED));
    }

    @Test
    public void isCarriageReturn()
    {
        assertTrue(CharUtils.isCarriageReturn(CharUtils.ASCII_CARRIAGE_RETURN));
        assertFalse(CharUtils.isCarriageReturn(CharUtils.ASCII_LINE_FEED));
    }

    @Test
    public void isWhitespace()
    {
        assertFalse(CharUtils.isWhitespace('b'));
        assertTrue(CharUtils.isWhitespace(CharUtils.ASCII_CARRIAGE_RETURN));
        assertTrue(CharUtils.isWhitespace(CharUtils.ASCII_LINE_FEED));
        assertTrue(CharUtils.isWhitespace(CharUtils.ASCII_LINE_FEED));
        assertTrue(CharUtils.isWhitespace(0));
        assertTrue(CharUtils.isWhitespace(CharUtils.ASCII_HORIZONTAL_TAB));
        assertTrue(CharUtils.isWhitespace(CharUtils.ASCII_FORM_FEED));
        assertTrue(CharUtils.isWhitespace((byte) 32));
    }

    @Test
    public void isSpace()
    {
        assertFalse(CharUtils.isSpace('b'));
        assertTrue(CharUtils.isSpace((byte) 32));
    }

    @Test
    public void isDigit()
    {
        assertFalse(CharUtils.isDigit('b'));
        assertFalse(CharUtils.isDigit(CharUtils.ASCII_CARRIAGE_RETURN));
        assertTrue(CharUtils.isDigit('0'));
        assertTrue(CharUtils.isDigit('1'));
        assertTrue(CharUtils.isDigit('2'));
        assertTrue(CharUtils.isDigit('3'));
        assertTrue(CharUtils.isDigit('4'));
        assertTrue(CharUtils.isDigit('5'));
        assertTrue(CharUtils.isDigit('6'));
        assertTrue(CharUtils.isDigit('7'));
        assertTrue(CharUtils.isDigit('8'));
        assertTrue(CharUtils.isDigit('9'));
    }

    @Test
    public void isLetter()
    {
        int ASCII_UPPERCASE_A = 65;
        int ASCII_UPPERCASE_Z = 90;
        int ASCII_LOWERCASE_A = 97;
        int ASCII_LOWERCASE_Z = 122;
        for (int i = ASCII_UPPERCASE_A; i <= ASCII_UPPERCASE_Z; i++)
        {
            assertTrue(CharUtils.isLetter(i));
        }
        for (int i = ASCII_LOWERCASE_A; i <= ASCII_LOWERCASE_Z; i++)
        {
            assertTrue(CharUtils.isLetter(i));
        }
        assertFalse(CharUtils.isLetter(CharUtils.ASCII_CARRIAGE_RETURN));
        assertFalse(CharUtils.isLetter('['));
        assertFalse(CharUtils.isLetter('{'));
    }

    @Test
    public void isOctalDigit()
    {
        assertFalse(CharUtils.isOctalDigit('b'));
        assertFalse(CharUtils.isOctalDigit(CharUtils.ASCII_CARRIAGE_RETURN));
        assertTrue(CharUtils.isOctalDigit('0'));
        assertTrue(CharUtils.isOctalDigit('1'));
        assertTrue(CharUtils.isOctalDigit('2'));
        assertTrue(CharUtils.isOctalDigit('3'));
        assertTrue(CharUtils.isOctalDigit('4'));
        assertTrue(CharUtils.isOctalDigit('5'));
        assertTrue(CharUtils.isOctalDigit('6'));
        assertTrue(CharUtils.isOctalDigit('7'));
        assertFalse(CharUtils.isOctalDigit('8'));
    }

    @Test
    public void isHexDigit()
    {
        assertFalse(CharUtils.isHexDigit('g'));
        assertFalse(CharUtils.isHexDigit(CharUtils.ASCII_CARRIAGE_RETURN));
        assertTrue(CharUtils.isHexDigit('0'));
        assertTrue(CharUtils.isHexDigit('1'));
        assertTrue(CharUtils.isHexDigit('2'));
        assertTrue(CharUtils.isHexDigit('3'));
        assertTrue(CharUtils.isHexDigit('4'));
        assertTrue(CharUtils.isHexDigit('5'));
        assertTrue(CharUtils.isHexDigit('6'));
        assertTrue(CharUtils.isHexDigit('7'));
        assertTrue(CharUtils.isHexDigit('8'));
        assertTrue(CharUtils.isHexDigit('9'));
        assertTrue(CharUtils.isHexDigit('a'));
        assertTrue(CharUtils.isHexDigit('A'));
        assertTrue(CharUtils.isHexDigit('b'));
        assertTrue(CharUtils.isHexDigit('B'));
        assertTrue(CharUtils.isHexDigit('c'));
        assertTrue(CharUtils.isHexDigit('C'));
        assertTrue(CharUtils.isHexDigit('d'));
        assertTrue(CharUtils.isHexDigit('D'));
        assertTrue(CharUtils.isHexDigit('e'));
        assertTrue(CharUtils.isHexDigit('E'));
        assertTrue(CharUtils.isHexDigit('f'));
        assertTrue(CharUtils.isHexDigit('F'));
    }
}
