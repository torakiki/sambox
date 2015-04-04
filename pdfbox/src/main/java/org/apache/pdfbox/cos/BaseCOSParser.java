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
package org.apache.pdfbox.cos;

import static org.apache.pdfbox.cos.ParseUtils.isCarriageReturn;
import static org.apache.pdfbox.cos.ParseUtils.isDigit;
import static org.apache.pdfbox.cos.ParseUtils.isLineFeed;
import static org.apache.pdfbox.cos.ParseUtils.isSpace;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.io.PushBackInputStream;
import org.apache.pdfbox.pdfparser.EndstreamOutputStream;
import org.apache.pdfbox.util.Charsets;

/**
 * @author Andrea Vacondio
 *
 */
public final class BaseCOSParser extends SourceReader
{

    private static final Log LOG = LogFactory.getLog(BaseCOSParser.class);

    public static final String ENDOBJ = "endobj";
    private static final byte[] ENDOBJ_BYTES = ENDOBJ.getBytes(Charsets.ISO_8859_1);
    public static final String STREAM = "stream";
    public static final String ENDSTREAM = "endstream";
    private static final byte[] ENDSTREAM_BYTES = ENDSTREAM.getBytes(Charsets.ISO_8859_1);
    private static final String DEF = "def";

    private IndirectObjectsProvider provider;

    public BaseCOSParser(PushBackInputStream source)
    {
        super(source);
        this.provider = new LazyIndirectObjectsProvider(this);
    }

    public BaseCOSParser(PushBackInputStream source, IndirectObjectsProvider provider)
    {
        super(source);
        this.provider = provider;
    }

    /**
     * @return The next parsed basic type object from the stream. Basic types are defined in Chap 7.3 of PDF
     * 32000-1:2008
     * @throws IOException If there is an error during parsing.
     */
    public COSBase nextParsedToken() throws IOException
    {
        skipSpaces();
        char c = (char) source().peek();
        switch (c)
        {
        case '<':
        {
            int leftBracket = source().read();
            c = (char) source().peek();
            source().unread(leftBracket);
            if (c == '<')
            {
                return nextDictionary();
            }
            return nextHexadecimalString();
        }
        case '[':
            return nextArray();
        case '(':
            return nextLiteralString();
        case '/':
            return nextName();
        case 'n':
            return nextNull();
        case 't':
        case 'f':
            return nextBoolean();
        case '.':
        case '-':
        case '+':
            return nextNumber();
        case '0':
        case '1':
        case '2':
        case '3':
        case '4':
        case '5':
        case '6':
        case '7':
        case '8':
        case '9':
            return nextNumberOrIndirectReference();
        case (char) -1:
            return null;
        default:
        {
            String badString = readToken();
            // if it's an endstream/endobj, we want to put it back so the caller will see it
            if (ENDOBJ.equals(badString) || ENDSTREAM.equals(badString))
            {
                source().unread(badString.getBytes(Charsets.ISO_8859_1));
            }
            else
            {
                LOG.warn(String.format("Unknown token with value '%s' ending at offset %d",
                        badString, offset()));
            }
        }
        }
        return null;
    }

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
        char c;
        while (((c = (char) source().peek()) != -1) && c != '>')
        {
            if (c != '/')
            {
                // an invalid dictionary, we are expecting the key, read until we can recover
                LOG.warn("Invalid dictionary, expected '/' but was '" + c + "'");
                while (((c = (char) source().peek()) != -1) && c != '>' && c != '/')
                {
                    // in addition to stopping when we find / or >, we also want
                    // to stop when we find endstream or endobj.
                    if (skipTokenIfValue(ENDOBJ, ENDSTREAM))
                    {
                        return dictionary;
                    }
                    else
                    {
                        source().read();
                    }

                }
                if (c == -1)
                {
                    return dictionary;
                }
            }
            else
            {
                COSName key = nextName();
                COSBase value = nextParsedToken();
                skipSpaces();
                if (source().peek() == 'd')
                {
                    // if the next string is 'def' then we are parsing a cmap stream
                    // and want to ignore it, otherwise throw an exception.
                    skipTokenIfValue(DEF);
                }

                if (value == null)
                {
                    LOG.warn(String.format("Bad dictionary declaration for key '%s'", key));
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
                    return array;
                }
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
     * @return The next parsed numeric object from the stream or the next indirect object in case the number is an
     * object number . Numeric objects are defined in Chap 7.3.3 of PDF 32000-1:2008. Indirect objects are defined in
     * Chap 7.3.10 of PDF 32000-1:2008
     * @throws IOException If there is an error during parsing.
     * @see #nextNumber()
     */
    public COSBase nextNumberOrIndirectReference() throws IOException
    {
        String first = readNumber();
        long offset = offset();
        skipSpaces();
        if (isDigit(source().peek()))
        {
            String second = readIntegerNumber();
            skipSpaces();
            if ('R' == source().read())
            {
                try
                {
                    return new IndirectCOSObject(new COSObjectKey(Long.parseLong(first),
                            Integer.parseInt(second)), provider);
                }
                catch (NumberFormatException nfe)
                {
                    throw new IOException(
                            String.format(
                                    "Unable to parse an object indirect reference with object number '%s' and generation number '%s'",
                                    first, second), nfe);
                }
            }
        }
        offset(offset);
        return COSNumber.get(first);
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
        return new COSString(readLiteralString());
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
                    offset(), next));
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
                source().unread(c);
                LOG.warn("Couldn't find expected LF following CR after 'stream' keyword at "
                        + offset());
            }
        }
        else if (!isLineFeed(c))
        {
            source().unread(c);
        }

        final COSStream stream = new COSStream(streamDictionary);
        long length = streamLength(streamDictionary);

        try (OutputStream out = stream.createFilteredStream())
        {
            if (length > 0)
            {
                IOUtils.copy(source(), out, length);
            }
            else
            {
                LOG.info("Using fallback strategy reading until 'endstream' or 'endobj' is found. Starting at offset "
                        + offset());
                // TODO
                copyUntilEndStreamTo(new EndstreamOutputStream(out));
            }
            if (!isNextToken(ENDSTREAM))
            {
                if (isNextToken(ENDOBJ))
                {
                    LOG.warn("Expected 'endstream' at " + offset() + " but was 'endobj'");
                }
                else
                {
                    skipExpected(ENDSTREAM);
                }
            }
        }

        return stream;
    }

    /**
     * @param streamDictionary
     * @return the stream length if found in the dictionary. -1 if if nothing is found or if the length is incorrect.
     * @throws IOException
     */
    private long streamLength(COSDictionary streamDictionary) throws IOException
    {
        COSBase lengthBaseObj = streamDictionary.getItem(COSName.LENGTH);
        if (lengthBaseObj == null)
        {
            LOG.warn("Invalid stream length. No length provided");
            return -1;
        }
        COSBase retVal = lengthBaseObj.getCOSObject();
        if (!(retVal instanceof COSNumber))
        {
            throw new IOException("Invalid stream length. Expected number instance but was "
                    + retVal.getClass().getSimpleName());
        }
        long length = ((COSNumber) retVal).longValue();
        long startingOffset = offset();
        long endStreamOffset = startingOffset + length;
        if (endStreamOffset > length())
        {
            LOG.warn("Invalid stream length. Out of range");
            return -1;
        }
        offset(endStreamOffset);
        if (!isNextToken(ENDSTREAM))
        {
            LOG.warn("Invalid stream length. Expected '" + ENDSTREAM + "' at " + endStreamOffset);
            return -1;
        }
        offset(startingOffset);
        return length;
    }

    /**
     * This method will read through the current stream object until we find the keyword "endstream" meaning we're at
     * the end of this object. Some pdf files, however, forget to write some endstream tags and just close off objects
     * with an "endobj" tag so we have to handle this case as well.
     * 
     * This method is optimized using buffered IO and reduced number of byte compare operations.
     * 
     * @param out stream we write out to.
     * 
     * @throws IOException if something went wrong
     */
    // TODO
    private void copyUntilEndStreamTo(final OutputStream out) throws IOException
    {

        byte[] buffer = new byte[2048];
        int bufSize;
        int charMatchCount = 0;
        byte[] keyw = ENDSTREAM_BYTES;

        // last character position of shortest keyword ('endobj')
        final int quickTestOffset = 5;

        // read next chunk into buffer; already matched chars are added to beginning of buffer
        while ((bufSize = source().read(buffer, charMatchCount, 2048 - charMatchCount)) > 0)
        {
            bufSize += charMatchCount;

            int bIdx = charMatchCount;
            int quickTestIdx;

            // iterate over buffer, trying to find keyword match
            for (int maxQuicktestIdx = bufSize - quickTestOffset; bIdx < bufSize; bIdx++)
            {
                // reduce compare operations by first test last character we would have to
                // match if current one matches; if it is not a character from keywords
                // we can move behind the test character;
                // this shortcut is inspired by the Boyer-Moore string search algorithm
                // and can reduce parsing time by approx. 20%
                if ((charMatchCount == 0)
                        && ((quickTestIdx = bIdx + quickTestOffset) < maxQuicktestIdx))
                {

                    final byte ch = buffer[quickTestIdx];
                    if ((ch > 't') || (ch < 'a'))
                    {
                        // last character we would have to match if current character would match
                        // is not a character from keywords -> jump behind and start over
                        bIdx = quickTestIdx;
                        continue;
                    }
                }

                // could be negative - but we only compare to ASCII
                final byte ch = buffer[bIdx];

                if (ch == keyw[charMatchCount])
                {
                    if (++charMatchCount == keyw.length)
                    {
                        // match found
                        bIdx++;
                        break;
                    }
                }
                else
                {
                    if ((charMatchCount == 3) && (ch == ENDOBJ_BYTES[charMatchCount]))
                    {
                        // maybe ENDSTREAM is missing but we could have ENDOBJ
                        keyw = ENDOBJ_BYTES;
                        charMatchCount++;
                    }
                    else
                    {
                        // no match; incrementing match start by 1 would be dumb since we already know matched chars
                        // depending on current char read we may already have beginning of a new match:
                        // 'e': first char matched;
                        // 'n': if we are at match position idx 7 we already read 'e' thus 2 chars matched
                        // for each other char we have to start matching first keyword char beginning with next
                        // read position
                        charMatchCount = (ch == 'e') ? 1 : ((ch == 'n') && (charMatchCount == 7)) ? 2 : 0;
                        // search again for 'endstream'
                        keyw = ENDSTREAM_BYTES;
                    }
                }
            } // for

            int contentBytes = Math.max(0, bIdx - charMatchCount);

            // write buffer content until first matched char to output stream
            if (contentBytes > 0)
            {
                out.write(buffer, 0, contentBytes);
            }
            if (charMatchCount == keyw.length)
            {
                // keyword matched; unread matched keyword (endstream/endobj) and following buffered content
                source().unread(buffer, contentBytes, bufSize - contentBytes);
                break;
            }
            else
            {
                // copy matched chars at start of buffer
                System.arraycopy(keyw, 0, buffer, 0, charMatchCount);
            }

        }
        // this writes a lonely CR or drops trailing CR LF and LF
        out.flush();
    }

    public IndirectObjectsProvider provider()
    {
        return provider;
    }

}
