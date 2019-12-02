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

import static java.util.Arrays.asList;
import static org.sejda.commons.util.RequireUtils.requireIOCondition;
import static org.sejda.commons.util.RequireUtils.requireNotNullArg;
import static org.sejda.sambox.util.CharUtils.ASCII_BACKSPACE;
import static org.sejda.sambox.util.CharUtils.ASCII_CARRIAGE_RETURN;
import static org.sejda.sambox.util.CharUtils.ASCII_FORM_FEED;
import static org.sejda.sambox.util.CharUtils.ASCII_HORIZONTAL_TAB;
import static org.sejda.sambox.util.CharUtils.ASCII_LINE_FEED;
import static org.sejda.sambox.util.CharUtils.isCarriageReturn;
import static org.sejda.sambox.util.CharUtils.isDigit;
import static org.sejda.sambox.util.CharUtils.isEOL;
import static org.sejda.sambox.util.CharUtils.isEndOfName;
import static org.sejda.sambox.util.CharUtils.isHexDigit;
import static org.sejda.sambox.util.CharUtils.isLineFeed;
import static org.sejda.sambox.util.CharUtils.isOctalDigit;
import static org.sejda.sambox.util.CharUtils.isWhitespace;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.sejda.commons.FastByteArrayOutputStream;
import org.sejda.commons.Pool;
import org.sejda.commons.util.IOUtils;
import org.sejda.io.SeekableSource;
import org.sejda.sambox.SAMBox;
import org.sejda.sambox.cos.COSObjectKey;
import org.sejda.sambox.util.CharUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Component responsible for reading a {@link SeekableSource}. Methods to read expected kind of tokens are available as
 * well as methods to skip them. This implementation uses a pool of {@link StringBuilder}s to minimize garbage
 * collection.
 * 
 * @author Andrea Vacondio
 */
class SourceReader implements Closeable
{

    private static final Logger LOG = LoggerFactory.getLogger(SourceReader.class);

    private static final long OBJECT_NUMBER_THRESHOLD = 10000000000L;
    private static final int GENERATION_NUMBER_THRESHOLD = 65535;
    public static final String OBJ = "obj";

    private Pool<StringBuilder> pool = new Pool<>(StringBuilder::new,
            Integer.getInteger(SAMBox.BUFFERS_POOL_SIZE_PROPERTY, 10)).onGive(b -> {
                b.setLength(0);
                b.trimToSize();
            });
    private SeekableSource source;

    public SourceReader(SeekableSource source)
    {
        requireNotNullArg(source, "Cannot read a null source");
        this.source = source;
    }

    /**
     * @return the source for this reader
     */
    public SeekableSource source()
    {
        return source;
    }

    /**
     * @return the current position
     * @throws IOException
     * @see {@link SeekableSource#position()}
     */
    public long position() throws IOException
    {
        return source.position();
    }

    /**
     * seeks to the given offset
     * 
     * @param offset the new offset
     * @throws IOException
     * @see {@link SeekableSource#position(long)}
     */
    public void position(long offset) throws IOException
    {
        source.position(offset);
    }

    /**
     * @return the source length
     * @see {@link SeekableSource#size()}
     */
    public long length()
    {
        return source.size();
    }

    /**
     * Skips the expected given String
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
            throw new IOException(
                    "expected='" + ec + "' actual='" + c + "' at offset " + (position() - 1));
        }
    }

    /**
     * Skips the next token if it's value is one of the given ones
     *
     * @param values the values to skip
     * @return true if the token is found and skipped, false otherwise.
     * @throws IOException if there is an error reading from the stream
     */
    public boolean skipTokenIfValue(String... values) throws IOException
    {
        long pos = position();
        String token = readToken();
        if (!asList(values).contains(token))
        {
            source.position(pos);
            return false;
        }
        return true;
    }

    /**
     * Skips an indirect object definition open tag (Ex. "12 0 obj") as defined in the chap 7.3.10 PDF 32000-1:2008.
     * 
     * @throws IOException if we are reading a not valid indirect object definition open tag
     */
    public void skipIndirectObjectDefinition() throws IOException
    {
        readObjectNumber();
        readGenerationNumber();
        skipSpaces();
        skipExpected(OBJ);
    }

    /**
     * Skips an indirect object definition open tag (Ex. "12 0 obj") as defined in the chap 7.3.10 PDF 32000-1:2008.
     * 
     * @param expected object we are expecting to find
     * @throws IOException if we are reading a not valid indirect object definition open tag or the object number or
     * generation number don't match the expected object
     */
    public void skipExpectedIndirectObjectDefinition(COSObjectKey expected) throws IOException
    {
        long objNumOffset = position();
        long number = readObjectNumber();
        if (number != expected.objectNumber())
        {
            throw new IOException(
                    String.format("Expected '%d' object number at offset %d but was '%d'",
                            expected.objectNumber(), objNumOffset, number));
        }
        long genNumOffset = position();
        long generation = readGenerationNumber();
        if (generation != expected.generation())
        {
            throw new IOException(
                    String.format("Expected '%d' generation number at offset %d but was '%d'",
                            expected.generation(), genNumOffset, number));
        }
        skipSpaces();
        skipExpected(OBJ);
    }

    /**
     * @return The next token that was read from the stream.
     *
     * @throws IOException If there is an error reading from the stream.
     * @see CharUtils#isEndOfName(int)
     */
    public String readToken() throws IOException
    {
        skipSpaces();
        StringBuilder builder = pool.borrow();
        try
        {
            int c;
            while (((c = source.read()) != -1) && !isEndOfName(c))
            {
                builder.append((char) c);
            }
            unreadIfValid(c);
            return builder.toString();
        }
        finally
        {
            pool.give(builder);
        }
    }

    /**
     * Unreads white spaces
     * 
     * @throws IOException
     */
    public void unreadSpaces() throws IOException
    {
        int c;
        while ((c = source.peekBack()) != -1 && isWhitespace(c))
        {
            source.back();
        }
    }

    /**
     * Unreads characters until it finds a white space
     * 
     * @throws IOException
     */
    public void unreadUntilSpaces() throws IOException
    {
        int c;
        while ((c = source.peekBack()) != -1 && !isWhitespace(c))
        {
            source.back();
        }
    }

    /**
     * @param valid values for the next token.
     * @return true if the next token is one of the given values. false otherwise.
     * @throws IOException if there is an error reading from the stream
     */
    public boolean isNextToken(String... values) throws IOException
    {
        long pos = position();
        String token = readToken();
        position(pos);
        return asList(values).contains(token);
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
        requireIOCondition(source.peek() != -1, "Expected line but was end of file");

        StringBuilder builder = pool.borrow();
        try
        {
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
        finally
        {
            pool.give(builder);
        }
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
            throw new IOException(
                    "Object Number '" + retval + "' has more than 10 digits or is negative");
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
    public int readGenerationNumber() throws IOException
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
    public String readName() throws IOException
    {
        skipExpected('/');
        FastByteArrayOutputStream buffer = new FastByteArrayOutputStream();
        int i;
        while (((i = source.read()) != -1) && !isEndOfName(i))
        {
            if (i == '#')
            {
                int ch1 = source.read();
                int ch2 = source.read();
                requireIOCondition(ch2 != -1 && ch1 != -1,
                        "Expected 2-digit hexadecimal code but was end of file");

                // Prior to PDF v1.2, the # was not a special character. Also,
                // it has been observed that various PDF tools do not follow the
                // spec with respect to the # escape, even though they report
                // PDF versions of 1.2 or later. The solution here is that we
                // interpret the # as an escape only when it is followed by two
                // valid hex digits.
                //
                if (isHexDigit((char) ch1) && isHexDigit((char) ch2))
                {
                    String hex = Character.toString((char) ch1) + (char) ch2;
                    i = Integer.parseInt(hex, 16);
                }
                else
                {
                    source.back(2);
                    LOG.warn(
                            "Found NUMBER SIGN (#) not used as escaping char while reading name at "
                                    + position());
                }
            }
            buffer.write(i);
        }
        unreadIfValid(i);
        byte[] bytes = buffer.toByteArray();
        try
        {
            StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes));
        }
        catch (CharacterCodingException e)
        {
            return new String(bytes, Charset.forName("Windows-1252"));
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * @return The integer that was read from the stream.
     * @throws IOException If there is an error reading from the stream.
     */
    public int readInt() throws IOException
    {
        String intBuffer = readIntegerNumber();
        try
        {
            return Integer.parseInt(intBuffer);
        }
        catch (NumberFormatException e)
        {
            source.back(intBuffer.getBytes(StandardCharsets.ISO_8859_1).length);
            throw new IOException(
                    String.format("Expected an integer type at offset %d but was '%s'", position(),
                            intBuffer),
                    e);
        }
    }

    /**
     * @return The long that was read from the stream.
     * @throws IOException If there is an error reading from the stream.
     */
    public long readLong() throws IOException
    {
        String longBuffer = readIntegerNumber();
        try
        {
            return Long.parseLong(longBuffer);
        }
        catch (NumberFormatException e)
        {
            source.back(longBuffer.getBytes(StandardCharsets.ISO_8859_1).length);
            throw new IOException(String.format("Expected a long type at offset %d but was '%s'",
                    position(), longBuffer), e);
        }
    }

    /**
     * Reads a a token conforming with a PDF Integer object defined in Numeric Objects chap 7.3.3 PDF 32000-1:2008.
     *
     * @return the token to parse as {@link Integer} or {@link Long}.
     * @throws IOException If there is an error reading from the stream.
     */
    public final String readIntegerNumber() throws IOException
    {
        skipSpaces();
        StringBuilder builder = pool.borrow();
        try
        {
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
        finally
        {
            pool.give(builder);
        }
    }

    /**
     * Reads a token conforming with PDF Numeric Objects chap 7.3.3 PDF 32000-1:2008.
     *
     * @return the token to parse as integer or real number.
     * @throws IOException If there is an error reading from the stream.
     */
    public final String readNumber() throws IOException
    {
        StringBuilder builder = pool.borrow();
        int lastAppended = -1;
        try
        {
            int c = source.read();
            if (c != -1 && (isDigit(c) || c == '+' || c == '-' || c == '.'))
            {
                builder.append((char) c);
                lastAppended = c;

                // Ignore double negative (this is consistent with Adobe Reader)
                if (c == '-' && source.peek() == c)
                {
                    source.read();
                }

                while ((c = source.read()) != -1
                        && (isDigit(c) || c == '.' || c == 'E' || c == 'e' || c == '+' || c == '-'))
                {
                    if (c == '-' && !(lastAppended == 'e' || lastAppended == 'E'))
                    {
                        // PDFBOX-4064: ignore "-" in the middle of a number
                        // but not if its a negative exponent 1e-23
                    }
                    else
                    {
                        builder.append((char) c);
                        lastAppended = c;
                    }
                }
            }
            unreadIfValid(c);
            return builder.toString();
        }
        finally
        {
            pool.give(builder);
        }
    }

    /**
     * Reads a token conforming with PDF Hexadecimal Strings chap 7.3.4.3 PDF 32000-1:2008. Any non hexadecimal char
     * found while parsing the token is replace with the default '0' hex char.
     *
     * @return the token to parse as an hexadecimal string
     * @throws IOException If there is an error reading from the stream.
     */
    public final String readHexString() throws IOException
    {
        skipExpected('<');
        StringBuilder builder = pool.borrow();
        try
        {
            int c;
            while (((c = source.read()) != -1) && c != '>')
            {
                if (isHexDigit(c))
                {
                    builder.append((char) c);
                }
                else if (isWhitespace(c))
                {
                    continue;
                }
                else
                {
                    // this differs from original PDFBox implementation. It replaces the wrong char with a default value
                    // and goes on.
                    LOG.warn(String.format(
                            "Expected an hexadecimal char at offset %d but was '%c'. Replaced with default 0.",
                            position() - 1, c));
                    builder.append('0');
                }
            }
            requireIOCondition(c != -1,
                    "Unexpected EOF. Missing closing bracket for hexadecimal string.");
            return builder.toString();
        }
        finally
        {
            pool.give(builder);
        }
    }

    /**
     * Reads a token conforming with PDF Literal Strings chap 7.3.4.2 PDF 32000-1:2008.
     *
     * @return the token to parse as a literal string
     * @throws IOException If there is an error during parsing.
     */
    public String readLiteralString() throws IOException
    {
        skipExpected('(');
        int bracesCounter = 1;
        StringBuilder builder = pool.borrow();
        try
        {

            int i;
            while ((i = source.read()) != -1 && bracesCounter > 0)
            {
                char c = (char) i;
                switch (c)
                {
                case '(':
                    bracesCounter++;
                    builder.append(c);
                    break;
                case ')':
                    bracesCounter--;
                    // TODO PDFBox 276
                    // this differs from the PDFBox 2.0.0 impl.
                    // consider if we want to take care of this. Maybe investigate Acrobat to see how they do it
                    if (bracesCounter > 0)
                    {
                        builder.append(c);
                    }
                    break;
                case '\\':
                {
                    char next = (char) source.read();
                    switch (next)
                    {
                    case 'n':
                        builder.append((char) ASCII_LINE_FEED);
                        break;
                    case 'r':
                        builder.append((char) ASCII_CARRIAGE_RETURN);
                        break;
                    case 't':
                        builder.append((char) ASCII_HORIZONTAL_TAB);
                        break;
                    case 'b':
                        builder.append((char) ASCII_BACKSPACE);
                        break;
                    case 'f':
                        builder.append((char) ASCII_FORM_FEED);
                        break;
                    case ')':
                        // TODO PDFBox 276
                        // this differs from the PDFBox 2.0.0 impl.
                        // consider if we want to take care of this. Maybe investigate Acrobat to see how they do it
                    case '(':
                    case '\\':
                        builder.append(next);
                        break;
                    case '0':
                    case '1':
                    case '2':
                    case '3':
                    case '4':
                    case '5':
                    case '6':
                    case '7':
                    {
                        StringBuilder octal = pool.borrow();
                        try
                        {

                            octal.append(next);
                            next = (char) source.read();
                            if (isOctalDigit(next))
                            {
                                octal.append(next);
                                next = (char) source.read();
                                if (isOctalDigit(next))
                                {
                                    octal.append(next);
                                }
                                else
                                {
                                    unreadIfValid(next);
                                }
                            }
                            else
                            {
                                unreadIfValid(next);
                            }
                            builder.append((char) Integer.parseInt(octal.toString(), 8));
                        }
                        finally
                        {
                            pool.give(octal);
                        }
                        break;
                    }
                    case ASCII_LINE_FEED:
                    case ASCII_CARRIAGE_RETURN:
                    {
                        // this is a break in the line so ignore it and the newline and continue
                        while ((c = (char) source.read()) != -1 && isEOL(c))
                        {
                            // NOOP
                        }
                        unreadIfValid(c);
                        break;
                    }
                    default:
                        // dropping the backslash
                        unreadIfValid(c);
                    }
                    break;
                }
                case ASCII_LINE_FEED:
                    builder.append((char) ASCII_LINE_FEED);
                    break;
                case ASCII_CARRIAGE_RETURN:
                {
                    builder.append((char) ASCII_LINE_FEED);
                    if (!CharUtils.isLineFeed(source.read()))
                    {
                        unreadIfValid(c);
                    }
                    break;
                }
                default:
                    builder.append(c);
                }
            }
            unreadIfValid(i);
            return builder.toString();
        }
        finally
        {
            pool.give(builder);
        }
    }

    /**
     * Skips all spaces and comments that are present.
     *
     * @throws IOException If there is an error reading from the stream.
     */
    public void skipSpaces() throws IOException
    {
        int c = source.read();
        // 37 is the % character, a comment
        while (isWhitespace(c) || c == 37)
        {
            if (c == 37)
            {
                // skip past the comment section
                while ((c = source.read()) != -1 && !isEOL(c))
                {
                    // NOOP
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
    public void unreadIfValid(int c) throws IOException
    {
        if (c != -1)
        {
            source.back();
        }
    }

    /**
     * Closes the {@link SeekableSource} this reader was created from.
     */
    @Override
    public void close() throws IOException
    {
        IOUtils.close(source);
    }
}
