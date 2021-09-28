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

import static org.sejda.sambox.util.CharUtils.isCarriageReturn;
import static org.sejda.sambox.util.CharUtils.isLineFeed;
import static org.sejda.sambox.util.CharUtils.isSpace;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sejda.io.SeekableSource;
import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSBoolean;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSInteger;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSNull;
import org.sejda.sambox.cos.COSNumber;
import org.sejda.sambox.cos.COSStream;
import org.sejda.sambox.cos.COSString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base parser for COS objects providing methods to get parsed objects from the given {@link SeekableSource}
 * 
 * @author Andrea Vacondio
 */
abstract class BaseCOSParser extends SourceReader
{
    private static final Logger LOG = LoggerFactory.getLogger(BaseCOSParser.class);

    public static final String ENDOBJ = "endobj";
    public static final String STREAM = "stream";
    public static final String ENDSTREAM = "endstream";

    BaseCOSParser(SeekableSource source)
    {
        super(source);
    }

    /**
     * @return The next parsed basic type object from the stream or null if the next token is not a COSBase. Basic types
     * are defined in Chap 7.3 of PDF 32000-1:2008
     * @throws IOException If there is an error during parsing.
     */
    public abstract COSBase nextParsedToken() throws IOException;

    /**
     * @return The next parsed dictionary object from the stream. Dictionary objects are defined in Chap 7.3.7 of PDF
     * 32000-1:2008
     * @throws IOException If there is an error during parsing.
     */
    public COSDictionary nextDictionary() throws IOException
    {
        skipExpected("<<");
        skipSpaces();
        COSDictionary dictionary = new COSDictionary();
        int c;
        while ((c = source().peek()) != -1 && c != '>')
        {
            if (c != '/')
            {
                LOG.warn("Invalid dictionary key, expected '/' but was '" + (char) c + "' at "
                        + position());
                if (!consumeInvalidDictionaryKey())
                {
                    return dictionary;
                }
            }
            else
            {
                COSName key = nextName();
                COSBase value = nextParsedToken();
                if (value == null)
                {
                    LOG.warn("Bad dictionary declaration for key '{}'", key);
                }
                else
                {
                    dictionary.setItem(key, value);
                }
            }
            skipSpaces();
        }
        skipExpected(">>");
        return dictionary;
    }

    /**
     * Consumes an invalid dictionary key
     * 
     * @return true if the dictionary has been recovered and parsing can go on
     * @throws IOException
     */
    private boolean consumeInvalidDictionaryKey() throws IOException
    {
        int c;
        while ((c = source().peek()) != -1 && c != '>' && c != '/')
        {
            // in addition to stopping when we find / or >, we also want
            // to stop when we find endstream or endobj.
            if (isNextToken(ENDOBJ, ENDSTREAM))
            {
                LOG.warn(
                        "Found unexpected 'endobj or 'endstream' at position {}, assuming end of dictionary",
                        position());
                return false;
            }
            source().read();

        }
        return c != -1;
    }

    /**
     * @return The next parsed array object from the stream. Array objects are defined in Chap 7.3.6 of PDF 32000-1:2008
     * @throws IOException If there is an error during parsing.
     */
    public COSArray nextArray() throws IOException
    {
        skipExpected('[');
        COSArray array = new COSArray();
        skipSpaces();
        int c;
        while (((c = source().peek()) != -1) && c != ']')
        {
            long position = position();
            COSBase item = nextParsedToken();
            if (item != null)
            {
                array.add(item);
            }
            else
            {
                // This could be an "endobj" or "endstream" which means we can assume that
                // the array has ended.
                if (isNextToken(ENDOBJ, ENDSTREAM))
                {
                    LOG.warn(
                            "Found unexpected 'endobj or 'endstream' at position {}, assuming end of array",
                            position);
                    return array;
                }
                // the next token is "obj" and the latest two are two integer. We assume the array wasn't
                // correctly terminated and we read the object definition as part of the array.
                // We have to unread the latest two int and remove them from the array
                if (isNextToken(OBJ) && array.size() >= 2
                        && (array.getObject(array.size() - 1) instanceof COSInteger)
                        && (array.getObject(array.size() - 2) instanceof COSInteger))
                {
                    unreadSpaces();
                    unreadUntilSpaces();
                    unreadSpaces();
                    unreadUntilSpaces();
                    array.removeLast();
                    array.removeLast();
                    LOG.warn(
                            "Found unexpected object definition at position {}, assuming end of array",
                            position);
                    return array;
                }
                LOG.warn("Found invalid token while parsing array at {}", position);
            }
            skipSpaces();
        }
        skipExpected(']');
        return array;
    }

    /**
     * @return The next parsed boolean object from the stream. Boolean objects are defined in Chap 7.3.2 of PDF
     * 32000-1:2008
     * @throws IOException If there is an error during parsing.
     */
    public COSBoolean nextBoolean() throws IOException
    {
        char c = (char) source().peek();
        if (c == 't')
        {
            skipExpected(Boolean.TRUE.toString());
            return COSBoolean.TRUE;
        }
        skipExpected(Boolean.FALSE.toString());
        return COSBoolean.FALSE;
    }

    /**
     * @return The next parsed numeric object from the stream. Numeric objects are defined in Chap 7.3.3 of PDF
     * 32000-1:2008
     * @throws IOException If there is an error during parsing.
     */
    public COSNumber nextNumber() throws IOException
    {
        return COSNumber.get(readNumber());
    }

    /**
     * @return The next parsed null object from the stream. Null object is defined in Chap 7.3.9 of PDF 32000-1:2008
     * @throws IOException If there is an error during parsing.
     */
    public COSNull nextNull() throws IOException
    {
        skipExpected("null");
        return COSNull.NULL;
    }

    /**
     * @return The next parsed name object from the stream. Name objects are defined in Chap 7.3.5 of PDF 32000-1:2008
     * @throws IOException If there is an error during parsing.
     */
    public COSName nextName() throws IOException
    {
        return COSName.getPDFName(readName());
    }

    /**
     * @return The next parsed literal string object from the stream. Literal string objects are defined in Chap 7.3.4.2
     * of PDF 32000-1:2008
     * @throws IOException If there is an error during parsing.
     */
    public COSString nextLiteralString() throws IOException
    {
        return COSString.newInstance(readLiteralString().getBytes(StandardCharsets.ISO_8859_1));
    }

    /**
     * @return The next parsed hexadecimal string object from the stream. Hexadecimal string objects is defined in Chap
     * 7.3.4.3 of PDF 32000-1:2008
     * @throws IOException If there is an error during parsing.
     */
    public COSString nextHexadecimalString() throws IOException
    {
        return COSString.parseHex(readHexString());
    }

    /**
     * @return The next parsed string object from the stream. String objects is defined in Chap 7.3.4 of PDF
     * 32000-1:2008
     * @throws IOException If there is an error during parsing.
     */
    public COSString nextString() throws IOException
    {
        char next = (char) source().peek();
        switch (next)
        {
        case '(':
            return nextLiteralString();
        case '<':
            return nextHexadecimalString();
        default:
            throw new IOException(String.format("Expected '(' or '<' at offset %d but was '%c'",
                    position(), next));
        }
    }

    /**
     * This will read a COSStream from the input stream using length attribute within dictionary. If length attribute is
     * a indirect reference it is first resolved to get the stream length. This means we copy stream data without
     * testing for 'endstream' or 'endobj' and thus it is no problem if these keywords occur within stream. We require
     * 'endstream' to be found after stream data is read.
     * 
     * @param dic dictionary that goes with this stream.
     * 
     * @return parsed pdf stream.
     * 
     * @throws IOException if an error occurred reading the stream, like problems with reading length attribute, stream
     * does not end with 'endstream' after data read, stream too short etc.
     */
    public COSStream nextStream(COSDictionary streamDictionary) throws IOException
    {
        skipSpaces();
        skipExpected(STREAM);
        int c = source().read();
        while (isSpace(c))
        {
            LOG.warn("Found unexpected space character after 'stream' keyword");
            c = source().read();
        }
        if (isCarriageReturn(c))
        {
            c = source().read();
            if (!isLineFeed(c))
            {
                source().back();
                LOG.warn("Couldn't find expected LF following CR after 'stream' keyword at "
                        + position());
            }
        }
        else if (!isLineFeed(c))
        {
            source().back();
        }

        final COSStream stream;
        long length = streamLength(streamDictionary);
        if (length > 0)
        {
            stream = new COSStream(streamDictionary, source(), position(), length);
        }
        else
        {
            stream = new COSStream(streamDictionary);
        }
        source().forward(stream.getFilteredLength());
        if (!skipTokenIfValue(ENDSTREAM))
        {
            if (isNextToken(ENDOBJ))
            {
                LOG.warn("Expected 'endstream' at " + position() + " but was 'endobj'");
            }
        }
        return stream;
    }

    /**
     * Retrieves the stream length. It gets it from the dictionary, if not present there it applies fallback strategy
     * searching for endstream or endobj keywords.
     * 
     * @param streamDictionary
     * @return the length
     * @throws IOException
     */
    private long streamLength(COSDictionary streamDictionary) throws IOException
    {
        long length = streamLengthFrom(streamDictionary);
        if (length <= 0)
        {
            LOG.info(
                    "Using fallback strategy reading until 'endstream' or 'endobj' is found. Starting at offset "
                            + position());
            length = findStreamLength();
        }
        return length;
    }

    /**
     * @param streamDictionary
     * @return the stream length if found in the dictionary. -1 if nothing is found or if the length is incorrect.
     * @throws IOException
     */
    private long streamLengthFrom(COSDictionary streamDictionary) throws IOException
    {
        long start = position();
        COSBase lengthBaseObj = streamDictionary.getItem(COSName.LENGTH);
        try
        {
            return doStreamLengthFrom(lengthBaseObj);
        }
        finally
        {
            position(start);
        }
    }

    private long doStreamLengthFrom(COSBase lengthBaseObj) throws IOException
    {
        long startingOffset = position();
        if (lengthBaseObj == null)
        {
            LOG.warn("Invalid stream length. No length provided");
            return -1;
        }
        COSBase retVal = lengthBaseObj.getCOSObject();
        if (!(retVal instanceof COSNumber))
        {
            LOG.warn("Invalid stream length. Expected number instance but was "
                    + retVal.getClass().getSimpleName());
            return -1;
        }
        long length = ((COSNumber) retVal).longValue();
        long endStreamOffset = startingOffset + length;
        if (endStreamOffset > length())
        {
            LOG.warn("Invalid stream length. Out of range");
            return -1;
        }
        position(endStreamOffset);
        if (!isNextToken(ENDSTREAM))
        {
            LOG.warn("Invalid stream length. Expected '" + ENDSTREAM + "' at " + endStreamOffset);
            return -1;
        }
        return length;
    }

    /**
     * Reads from the current position until it finds the "endstream" meaning we're at the end of this stream object.
     * Some pdf files, however, forget to write some endstream tags and just close off objects with an "endobj" tag so
     * we have to handle this case as well.
     * 
     * @return the length from the current position to the position where "endstream" or "endobj" was found
     * @throws IOException
     */
    private long findStreamLength() throws IOException
    {
        long start = position();
        try
        {
            return doFindStreamLength(start);
        }
        finally
        {
            position(start);
        }
    }

    private long doFindStreamLength(long start) throws IOException
    {
        Pattern pattern = Pattern.compile("endstream|endobj");
        while (true)
        {
            long currentPosition = position();
            String currentLine = readLine();
            Matcher matcher = pattern.matcher(currentLine);
            if (matcher.find())
            {
                position(currentPosition + matcher.start());
                long length = position() - start;
                int prevChar = source().back().peek();
                if (isCarriageReturn(prevChar))
                {
                    return length - 1;
                }
                if (isLineFeed(prevChar))
                {
                    prevChar = source().back().peek();
                    if (isCarriageReturn(prevChar))
                    {
                        return length - 2;
                    }
                    return length - 1;
                }
                return length;
            }
        }
    }
}
