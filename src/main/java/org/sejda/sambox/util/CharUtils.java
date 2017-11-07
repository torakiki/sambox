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
package org.sejda.sambox.util;

/**
 * Utility class providing chars related helper methods
 * 
 * @author Andrea Vacondio
 */
public final class CharUtils
{

    public static final byte ASCII_LINE_FEED = 10;
    public static final byte ASCII_FORM_FEED = 12;
    public static final byte ASCII_CARRIAGE_RETURN = 13;
    public static final byte ASCII_BACKSPACE = 8;
    public static final byte ASCII_HORIZONTAL_TAB = 9;

    private static final byte ASCII_ZERO = 48;
    private static final byte ASCII_SEVEN = 55;
    private static final byte ASCII_NINE = 57;

    public static final byte ASCII_NULL = 0;
    public static final byte ASCII_SPACE = 32;

    private static final byte ASCII_UPPERCASE_A = 65;
    private static final byte ASCII_UPPERCASE_Z = 90;
    private static final byte ASCII_LOWERCASE_A = 97;
    private static final byte ASCII_LOWERCASE_Z = 122;

    private CharUtils()
    {
        // no instance
    }

    /**
     * @param ch The character
     * @return <code>true</code> if the character terminates a PDF name, otherwise <code>false</code>.
     */
    public static boolean isEndOfName(int ch)
    {
        return isWhitespace(ch) || ch == '>' || ch == '<' || ch == '[' || ch == ']' || ch == '/'
                || ch == ')' || ch == '(' || ch == '%';
    }

    /**
     * @param c
     * @return true if the char is end of file
     */
    public static boolean isEOF(int c)
    {
        return c == -1;
    }

    /**
     * @param c The character to check against end of line
     * @return true if the character is a line feed or a carriage return
     */
    public static boolean isEOL(int c)
    {
        return isCarriageReturn(c) || isLineFeed(c);
    }

    public static boolean isLineFeed(int c)
    {
        return ASCII_LINE_FEED == c;
    }

    public static boolean isCarriageReturn(int c)
    {
        return ASCII_CARRIAGE_RETURN == c;
    }

    /**
     * This will tell if a character is whitespace or not. These values are specified in table 1 (page 12) of ISO
     * 32000-1:2008.
     * 
     * @param c The character to check against whitespace
     * @return true if the character is a whitespace character.
     */
    public static boolean isWhitespace(int c)
    {
        return c == ASCII_NULL || c == ASCII_HORIZONTAL_TAB || c == ASCII_FORM_FEED || isEOL(c)
                || isSpace(c);
    }

    public static boolean isNul(int c)
    {
        return c == ASCII_NULL;
    }

    /**
     * @param c The character to check against space
     * @return true if the character is a space character.
     */
    public static boolean isSpace(int c)
    {
        return ASCII_SPACE == c;
    }

    /**
     * @param c The character to be checked
     * @return true if the character is a digit.
     */
    public static boolean isDigit(int c)
    {
        return c >= ASCII_ZERO && c <= ASCII_NINE;
    }

    /**
     * @param c The character to be checked
     * @return true if the character is a letter (case unsensitive).
     */
    public static boolean isLetter(int c)
    {
        return (c >= ASCII_UPPERCASE_A && c <= ASCII_UPPERCASE_Z)
                || (c >= ASCII_LOWERCASE_A && c <= ASCII_LOWERCASE_Z);
    }

    /**
     * @param c The character to be checked
     * @return true if the character is an octal digit.
     */
    public static boolean isOctalDigit(int c)
    {
        return c >= ASCII_ZERO && c <= ASCII_SEVEN;
    }

    /**
     * @param c The character to be checked
     * @return true if the character is an hex digit.
     */
    public static boolean isHexDigit(int c)
    {
        return isDigit(c) || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    public static boolean isBlank(final CharSequence cs)
    {
        int strLen;
        if (cs == null || (strLen = cs.length()) == 0)
        {
            return true;
        }
        for (int i = 0; i < strLen; i++)
        {
            if (Character.isWhitespace(cs.charAt(i)) == false)
            {
                return false;
            }
        }
        return true;
    }
}
