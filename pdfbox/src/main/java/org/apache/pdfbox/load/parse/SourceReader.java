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
import static org.apache.pdfbox.load.parse.ParseUtils.isEOL;
import static org.apache.pdfbox.load.parse.ParseUtils.isEndOfName;
import static org.apache.pdfbox.load.parse.ParseUtils.isLineFeed;
import static org.apache.pdfbox.load.parse.ParseUtils.isSpace;
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
    private static final String ISO_8859_1 = "ISO-8859-1";

    private StringBuilder buffer = new StringBuilder();
    protected PushBackInputStream source;

    public SourceReader(PushBackInputStream source)
    {
        this.source = source;
    }

    private void clearBuffer()
    {
        buffer.setLength(0);
    }


    /**
     * @return The next string that was read from the stream.
     *
     * @throws IOException If there is an error reading from the stream.
     */
    public String readString() throws IOException
    {
        skipSpaces();
        clearBuffer();
        char c = (char) source.read();
        while (!isEndOfName(c) && c != -1)
        {
            buffer.append((char) c);
            c = (char) source.read();
        }
        if (c != -1)
        {
            source.unread(c);
        }
        return buffer.toString();
    }

    /**
     * Skips the given String
     *
     * @param expectedString the String value that is expected.
     * @throws IOException if the String char is not the expected value or if an I/O error occurs.
     */
    public final void skipExpected(String expected) throws IOException
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
    public void skipExpected(char ec) throws IOException
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
    public String readLine() throws IOException
    {
        if (source.isEOF())
        {
            throw new IOException("Expected line but was end of file");
        }

        clearBuffer();

        int c;
        while ((c = source.read()) != -1 && !isEOL(c))
        {
            buffer.append((char) c);
        }
        if (isCarriageReturn(c) && isLineFeed(source.peek()))
        {
            source.read();
        }
        return buffer.toString();
    }

    /**
     * Reads a long and throws an {@link IOException} if the long value is negative or has more than 10 digits (i.e. :
     * bigger than {@link #OBJECT_NUMBER_THRESHOLD})
     *
     * @return the object number being read.
     * @throws IOException if an I/O error occurs
     */
    public long readObjectNumber() throws IOException
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
     * This will read an integer from the stream.
     *
     * @return The integer that was read from the stream.
     *
     * @throws IOException If there is an error reading from the stream.
     */
    protected int readInt() throws IOException
    {
        skipSpaces();
        int retval = 0;

        String intBuffer = readStringNumber();

        try
        {
            retval = Integer.parseInt(intBuffer);
        }
        catch (NumberFormatException e)
        {
            source.unread(intBuffer.getBytes(ISO_8859_1));
            throw new IOException(
                    "Error: Expected an integer type at offset " + source.getOffset(), e);
        }
        return retval;
    }

    /**
     * This will read an long from the stream.
     *
     * @return The long that was read from the stream.
     *
     * @throws IOException If there is an error reading from the stream.
     */
    protected long readLong() throws IOException
    {
        skipSpaces();
        long retval = 0;

        String longBuffer = readStringNumber();

        try
        {
            retval = Long.parseLong(longBuffer);
        }
        catch (NumberFormatException e)
        {
            source.unread(longBuffer.getBytes(ISO_8859_1));
            throw new IOException("Error: Expected a long type at offset " + source.getOffset()
                    + ", instead got '" + longBuffer + "'", e);
        }
        return retval;
    }

    /**
     * Reads a token by the {@linkplain #readInt()} method and the {@linkplain #readLong()} method.
     *
     * @return the token to parse as integer or long by the calling method.
     * @throws IOException throws by the {@link #source} methods.
     */
    private final String readStringNumber() throws IOException
    {
        int lastByte = 0;
        clearBuffer();
        while ((lastByte = source.read()) != -1 && !isSpace(lastByte) && !isEOL(lastByte)
                && lastByte != 60 && // see sourceforge bug 1714707
                lastByte != '[' && // PDFBOX-1845
                lastByte != '(' && // PDFBOX-2579
                lastByte != 0 // See sourceforge bug 853328
        )
        {
            buffer.append((char) lastByte);
        }
        if (lastByte != -1)
        {
            source.unread(lastByte);
        }
        return buffer.toString();
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
        if (c != -1)
        {
            source.unread(c);
        }
    }
}
