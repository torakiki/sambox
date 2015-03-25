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

import static org.apache.pdfbox.load.parse.ParseUtils.isDigit;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSBoolean;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSInteger;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSNull;
import org.apache.pdfbox.cos.COSNumber;
import org.apache.pdfbox.cos.COSObjectKey;
import org.apache.pdfbox.io.PushBackInputStream;

/**
 * @author Andrea Vacondio
 *
 */
public class BaseCOSParser extends SourceReader
{

    private static final Log LOG = LogFactory.getLog(BaseCOSParser.class);

    protected static final String ENDOBJ_STRING = "endobj";
    protected static final String ENDSTREAM_STRING = "endstream";

    public BaseCOSParser(PushBackInputStream source)
    {
        super(source);
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
            // pull off first left bracket
            int leftBracket = source().read();
            // check for second left bracket
            c = (char) source().peek();
            source().unread(leftBracket);
            if (c == '<')
            {

                return parseCOSDictionary();
                // TODO skipSpaces();
            }
            return parseCOSString();
            break;
        }
        case '[':
            return parseCOSArray();
        case '(':
            return parseCOSString();
        case '/':
            return nextName();
        case 'n':
            return nextNull();
        case 't':
        case 'f':
            return nextBoolean();
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
        case '.':
        case '-':
        case '+':
        case 'e':
        case 'E':
            return nextNumber();
        case (char) -1:
            return null;
        default:
        {
            // This is not suppose to happen, but we will allow for it
            // so we are more compatible with POS writers that don't
            // follow the spec
            String badString = readString();
            if (badString == null || badString.length() == 0)
            {
                int peek = source().peek();
                // we can end up in an infinite loop otherwise
                throw new IOException("Unknown dir object c='" + c + "' cInt=" + (int) c
                        + " peek='" + (char) peek + "' peekInt=" + peek + " "
                        + source().getOffset());
            }

            // if it's an endstream/endobj, we want to put it back so the caller will see it
            if (ENDOBJ_STRING.equals(badString) || ENDSTREAM_STRING.equals(badString))
            {
                source().unread(badString.getBytes(ISO_8859_1));
            }
        }
        }
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
     * @return The next parsed null object from the stream. Null object is defined in Chap 7.3.9 of PDF 32000-1:2008
     * @throws IOException If there is an error during parsing.
     */
    protected COSNull nextNull() throws IOException
    {
        skipExpected("null");
        return COSNull.NULL;
    }

    /**
     * @return The next parsed name object from the stream. Name objects is defined in Chap 7.3.5 of PDF 32000-1:2008
     * @throws IOException If there is an error during parsing.
     */
    protected COSName nextName() throws IOException
    {
        return COSName.getPDFName(readName());
    }

    /**
     * @return The parsed Dictionary object.
     *
     * @throws IOException If there is an error parsing the dictionary object.
     */
    private COSBase parseDictionaryValue() throws IOException
    {
        COSBase retval = null;
        long numOffset = source().getOffset();
        COSBase number = parseDirObject();
        skipSpaces();
        char next = (char) source().peek();
        if (isDigit(next))
        {
            long genOffset = pdfSource.getOffset();
            COSBase generationNumber = parseDirObject();
            skipSpaces();
            readExpectedChar('R');
            if (!(number instanceof COSInteger))
            {
                throw new IOException("expected number, actual=" + number + " at offset "
                        + numOffset);
            }
            if (!(generationNumber instanceof COSInteger))
            {
                throw new IOException("expected number, actual=" + number + " at offset "
                        + genOffset);
            }
            COSObjectKey key = new COSObjectKey(((COSInteger) number).longValue(),
                    ((COSInteger) generationNumber).intValue());
            retval = document.getObjectFromPool(key);
        }
        else
        {
            retval = number;
        }
        return retval;
    }

    /**
     * This will parse a PDF dictionary.
     *
     * @return The parsed dictionary.
     *
     * @throws IOException IF there is an error reading the stream.
     */
    protected COSDictionary parseCOSDictionary() throws IOException
    {
        readExpectedChar('<');
        readExpectedChar('<');
        skipSpaces();
        COSDictionary obj = new COSDictionary();
        boolean done = false;
        while (!done)
        {
            skipSpaces();
            char c = (char) pdfSource.peek();
            if (c == '>')
            {
                done = true;
            }
            else if (c != '/')
            {
                // an invalid dictionary, we are expecting
                // the key, read until we can recover
                LOG.warn("Invalid dictionary, found: '" + c + "' but expected: '/'");
                int read = pdfSource.read();
                while (read != -1 && read != '/' && read != '>')
                {
                    // in addition to stopping when we find / or >, we also want
                    // to stop when we find endstream or endobj.
                    if (read == E)
                    {
                        read = pdfSource.read();
                        if (read == N)
                        {
                            read = pdfSource.read();
                            if (read == D)
                            {
                                read = pdfSource.read();
                                boolean isStream = read == S && pdfSource.read() == T
                                        && pdfSource.read() == R && pdfSource.read() == E
                                        && pdfSource.read() == A && pdfSource.read() == M;

                                boolean isObj = !isStream && read == O && pdfSource.read() == B
                                        && pdfSource.read() == J;
                                if (isStream || isObj)
                                {
                                    return obj; // we're done reading this object!
                                }
                            }
                        }
                    }
                    read = pdfSource.read();
                }
                if (read != -1)
                {
                    pdfSource.unread(read);
                }
                else
                {
                    return obj;
                }
            }
            else
            {
                COSName key = parseCOSName();
                COSBase value = parseCOSDictionaryValue();
                skipSpaces();
                if (((char) pdfSource.peek()) == 'd')
                {
                    // if the next string is 'def' then we are parsing a cmap stream
                    // and want to ignore it, otherwise throw an exception.
                    String potentialDEF = readString();
                    if (!potentialDEF.equals(DEF))
                    {
                        pdfSource.unread(potentialDEF.getBytes(ISO_8859_1));
                    }
                    else
                    {
                        skipSpaces();
                    }
                }

                if (value == null)
                {
                    LOG.warn("Bad Dictionary Declaration " + pdfSource);
                }
                else
                {
                    value.setDirect(true);
                    obj.setItem(key, value);
                }
            }
        }
        readExpectedChar('>');
        readExpectedChar('>');
        return obj;
    }
}
