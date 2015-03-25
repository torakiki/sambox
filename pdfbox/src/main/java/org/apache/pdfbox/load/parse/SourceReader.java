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
package org.apache.pdfbox.load.parse;

import static org.apache.pdfbox.load.parse.ParseUtils.isCarriageReturn;
import static org.apache.pdfbox.load.parse.ParseUtils.isDigit;
import static org.apache.pdfbox.load.parse.ParseUtils.isEOL;
import static org.apache.pdfbox.load.parse.ParseUtils.isEndOfName;
import static org.apache.pdfbox.load.parse.ParseUtils.isHexDigit;
import static org.apache.pdfbox.load.parse.ParseUtils.isLineFeed;
import static org.apache.pdfbox.load.parse.ParseUtils.isWhitespace;

import java.io.IOException;

import org.apache.pdfbox.io.PushBackInputStream;

/**
 * @author Andrea Vacondio
 *
 */
public class SourceReader
{

    private static final long OBJECT_NUMBER_THRESHOLD = 10000000000L;
    private static final long GENERATION_NUMBER_THRESHOLD = 65535;
    protected static final String ISO_8859_1 = "ISO-8859-1";

    // TODO maybe use a pool of buffers if we want to support concurrent reads and async indirect objs resolve
    private StringBuilder buffer = new StringBuilder();
    private PushBackInputStream source;

    public SourceReader(PushBackInputStream source)
    {
        this.source = source;
    }

    /**
     * @return a buffer to be used during read and parsing.
     */
    protected StringBuilder buffer()
    {
        buffer.setLength(0);
        return buffer;
    }

    /**
     * @return the source for this reader
     */
    protected PushBackInputStream source()
    {
        return source;
    }

    /**
     * @return The next string that was read from the stream.
     *
     * @throws IOException If there is an error reading from the stream.
     */
    protected String readString() throws IOException
    {
        skipSpaces();
        StringBuilder builder = buffer();
        char c;
        while (((c = (char) source.read()) != -1) && !isEndOfName(c))
        {
            builder.append((char) c);
            c = (char) source.read();
        }
        unreadIfValid(c);
        return builder.toString();
    }

    /**
     * Skips the given String
     *
     * @param expectedString the String value that is expected.
     * @throws IOException if the String char is not the expected value or if an I/O error occurs.
     */
    protected final void skipExpected(String expected) throws IOException
    {
        for (char c : expected.toCharArray())
        {
            skipExpected(c);
        }
    }

    /**
     * Skips one char and throws an exception if it is not the expected value.
     *
     * @param ec the char value that is expected.
     * @throws IOException if the read char is not the expected value or if an I/O error occurs.
     */
    protected void skipExpected(char ec) throws IOException
    {
        char c = (char) source.read();
        if (c != ec)
        {
            throw new IOException("expected='" + ec + "' actual='" + c + "' at offset "
                    + source.getOffset());
        }
    }

    /**
     * Reads bytes until the first end of line marker occurs. NOTE: The EOL marker may consists of 1 (CR or LF) or 2 (CR
     * and CL) bytes which is an important detail if one wants to unread the line.
     *
     * @return The characters between the current position and the end of the line.
     * @throws IOException If there is an error reading from the stream.
     */
    protected String readLine() throws IOException
    {
        if (source.isEOF())
        {
            throw new IOException("Expected line but was end of file");
        }

        StringBuilder builder = buffer();

        int c;
        while ((c = source.read()) != -1 && !isEOL(c))
        {
            builder.append((char) c);
        }
        if (isCarriageReturn(c) && isLineFeed(source.peek()))
        {
            source.read();
        }
        return builder.toString();
    }

    /**
     * Reads a long and throws an {@link IOException} if the long value is negative or has more than 10 digits (i.e. :
     * bigger than {@link #OBJECT_NUMBER_THRESHOLD})
     *
     * @return the object number being read.
     * @throws IOException if an I/O error occurs
     */
    protected long readObjectNumber() throws IOException
    {
        long retval = readLong();
        if (retval < 0 || retval >= OBJECT_NUMBER_THRESHOLD)
        {
            throw new IOException("Object Number '" + retval
                    + "' has more than 10 digits or is negative");
        }
        return retval;
    }

    /**
     * reads an integer and throws an {@link IOException} if the integer value has more than the maximum object revision
     * (i.e. : bigger than {@link #GENERATION_NUMBER_THRESHOLD})
     * 
     * @return the generation number being read.
     * @throws IOException if an I/O error occurs
     */
    protected int readGenerationNumber() throws IOException
    {
        int retval = readInt();
        if (retval < 0 || retval > GENERATION_NUMBER_THRESHOLD)
        {
            throw new IOException("Generation Number '" + retval + "' has more than 5 digits");
        }
        return retval;
    }

    /**
     * Reads a token conforming with PDF Name Objects chap 7.3.5 PDF 32000-1:2008.
     * 
     * @return the generation number being read.
     * @throws IOException if an I/O error occurs
     */
    protected String readName() throws IOException
    {
        skipExpected('/');
        StringBuilder builder = buffer();
        char c;
        while (((c = (char) source.read()) != -1) && !isEndOfName(c))
        {
            if (c == '#')
            {
                int ch1 = source.read();
                int ch2 = source.read();

                // Prior to PDF v1.2, the # was not a special character. Also,
                // it has been observed that various PDF tools do not follow the
                // spec with respect to the # escape, even though they report
                // PDF versions of 1.2 or later. The solution here is that we
                // interpret the # as an escape only when it is followed by two
                // valid hex digits.
                //
                if (isHexDigit(ch1) && isHexDigit(ch2))
                {
                    String hex = "" + ch1 + ch2;
                    try
                    {
                        c = (char) Integer.parseInt(hex, 16);
                    }
                    catch (NumberFormatException e)
                    {
                        source.unread(ch1);
                        source.unread(ch2);
                        throw new IOException(String.format(
                                "Expected an Hex number at offset %d but was '%s'",
                                source.getOffset(), hex), e);
                    }
                }
                else
                {
                    source.unread(ch2);
                    c = (char) ch1;
                }
            }
            builder.append(c);
        }
        unreadIfValid(c);
        return builder.toString();
    }

    /**
     * @return The integer that was read from the stream.
     *
     * @throws IOException If there is an error reading from the stream.
     */
    protected int readInt() throws IOException
    {
        skipSpaces();
        String intBuffer = readIntegerNumber();
        try
        {
            return Integer.parseInt(intBuffer);
        }
        catch (NumberFormatException e)
        {
            source.unread(intBuffer.getBytes(ISO_8859_1));
            throw new IOException(String.format(
                    "Expected an integer type at offset %d but was '%s'", source.getOffset(),
                    intBuffer), e);
        }
    }

    /**
     * @return The long that was read from the stream.
     *
     * @throws IOException If there is an error reading from the stream.
     */
    protected long readLong() throws IOException
    {
        skipSpaces();
        String longBuffer = readIntegerNumber();
        try
        {
            return Long.parseLong(longBuffer);
        }
        catch (NumberFormatException e)
        {
            source.unread(longBuffer.getBytes(ISO_8859_1));
            throw new IOException(String.format("Expected a long type at offset %d but was '%s'",
                    source.getOffset(), longBuffer), e);
        }
    }

    /**
     * Reads a token by the {@linkplain #readInt()} method and the {@linkplain #readLong()} method.
     *
     * @return the token to parse as integer or long by the calling method.
     * @throws IOException If there is an error reading from the stream.
     */
    private final String readIntegerNumber() throws IOException
    {
        StringBuilder builder = buffer();
        int c = source.read();
        if (c != -1 && (isDigit(c) || c == '+' || c == '-'))
        {
            builder.append((char) c);
            while ((c = source.read()) != -1 && isDigit(c))
            {
                builder.append((char) c);
            }
        }
        unreadIfValid(c);
        return builder.toString();
    }

    /**
     * Reads a token conforming with PDF Numeric Objects chap 7.3.3 PDF 32000-1:2008.
     *
     * @return the token to parse as integer or long by the calling method.
     * @throws IOException If there is an error reading from the stream.
     */
    protected final String readNumber() throws IOException
    {
        StringBuilder builder = buffer();
        int c = source.read();
        if (c != -1 && (isDigit(c) || c == '+' || c == '-' || c == '.'))
        {
            builder.append((char) c);
            while ((c = source.read()) != -1 && (isDigit(c) || c == 'E' || c == 'e'))
            {
                builder.append((char) c);
            }
        }
        unreadIfValid(c);
        return builder.toString();
    }

    /**
     * Skips all spaces and comments that are present.
     *
     * @throws IOException If there is an error reading from the stream.
     */
    protected void skipSpaces() throws IOException
    {
        int c = source.read();
        // 37 is the % character, a comment
        while (isWhitespace(c) || c == 37)
        {
            if (c == 37)
            {
                // skip past the comment section
                c = source.read();
                while (!isEOL(c) && c != -1)
                {
                    c = source.read();
                }
            }
            else
            {
                c = source.read();
            }
        }
        unreadIfValid(c);
    }

    /**
     * Unreads the given character if it's not -1
     * 
     * @param c
     * @throws IOException
     */
    protected void unreadIfValid(int c) throws IOException
    {
        if (c != -1)
        {
            source.unread(c);
        }
    }
}
