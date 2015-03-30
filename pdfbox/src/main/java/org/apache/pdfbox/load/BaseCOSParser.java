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
package org.apache.pdfbox.load;

import static org.apache.pdfbox.load.ParseUtils.isDigit;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSBoolean;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSNull;
import org.apache.pdfbox.cos.COSNumber;
import org.apache.pdfbox.cos.COSObjectKey;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.io.PushBackInputStream;
import org.apache.pdfbox.util.Charsets;

/**
 * @author Andrea Vacondio
 *
 */
public class BaseCOSParser extends SourceReader
{

    private static final Log LOG = LogFactory.getLog(BaseCOSParser.class);

    protected static final String ENDOBJ = "endobj";
    protected static final String STREAM = "stream";
    protected static final String ENDSTREAM = "endstream";
    private static final String DEF = "def";

    private IndirectObjectsProvider provider;

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
    protected COSBase nextToken() throws IOException
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
                // TODO skipSpaces();
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
    protected COSDictionary nextDictionary() throws IOException
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
                COSBase value = nextToken();
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
    protected COSArray nextArray() throws IOException
    {
        skipExpected('[');
        COSArray array = new COSArray();
        skipSpaces();
        int c;
        while (((c = source().peek()) != -1) && c != ']')
        {
            COSBase item = nextToken();
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
    protected COSBoolean nextBoolean() throws IOException
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
    protected COSNumber nextNumber() throws IOException
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
    protected COSBase nextNumberOrIndirectReference() throws IOException
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
    protected COSNull nextNull() throws IOException
    {
        skipExpected("null");
        return COSNull.NULL;
    }

    /**
     * @return The next parsed name object from the stream. Name objects are defined in Chap 7.3.5 of PDF 32000-1:2008
     * @throws IOException If there is an error during parsing.
     */
    protected COSName nextName() throws IOException
    {
        return COSName.getPDFName(readName());
    }

    /**
     * @return The next parsed literal string object from the stream. Literal string objects are defined in Chap 7.3.4.2
     * of PDF 32000-1:2008
     * @throws IOException If there is an error during parsing.
     */
    protected COSString nextLiteralString() throws IOException
    {
        return new COSString(readLiteralString());
    }

    /**
     * @return The next parsed hexadecimal string object from the stream. Hexadecimal string objects is defined in Chap
     * 7.3.4.3 of PDF 32000-1:2008
     * @throws IOException If there is an error during parsing.
     */
    protected COSString nextHexadecimalString() throws IOException
    {
        return COSString.parseHex(readHexString());
    }

    /**
     * @return The next parsed string object from the stream. String objects is defined in Chap 7.3.4 of PDF
     * 32000-1:2008
     * @throws IOException If there is an error during parsing.
     */
    protected COSString nextString() throws IOException
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

}
