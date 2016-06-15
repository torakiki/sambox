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

import static org.sejda.sambox.util.CharUtils.isDigit;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.sejda.io.SeekableSource;
import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parser for COS objects capable of handling indirect references.
 * 
 * @author Andrea Vacondio
 */
class COSParser extends BaseCOSParser
{
    private static final Logger LOG = LoggerFactory.getLogger(COSParser.class);

    private IndirectObjectsProvider provider;

    COSParser(SeekableSource source)
    {
        super(source);
        this.provider = new LazyIndirectObjectsProvider().initializeWith(this);
    }

    COSParser(SeekableSource source, IndirectObjectsProvider provider)
    {
        super(source);
        this.provider = provider;
    }

    @Override
    public COSBase nextParsedToken() throws IOException
    {
        skipSpaces();
        char c = (char) source().peek();
        switch (c)
        {
        case '<':
        {
            source().read();
            c = (char) source().peek();
            source().back();
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
            if (ENDOBJ.equals(badString) || ENDSTREAM.equals(badString) || OBJ.equals(badString))
            {
                source().back(badString.getBytes(StandardCharsets.ISO_8859_1).length);
            }
            else
            {
                LOG.warn(String.format("Unknown token with value '%s' ending at offset %d",
                        badString, position()));
                if (badString.length() <= 0)
                {
                    // we are at a zero length end of token, we try to skip it and hopefully recover
                    source().read();
                }
            }
        }
        }
        return null;
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
        long offset = position();
        skipSpaces();
        if (isDigit(source().peek()))
        {
            String second = readIntegerNumber();
            skipSpaces();
            if ('R' == source().read())
            {
                try
                {
                    return new ExistingIndirectCOSObject(Long.parseLong(first),
                            Integer.parseInt(second), provider);
                }
                catch (NumberFormatException nfe)
                {
                    throw new IOException(String.format(
                            "Unable to parse an object indirect reference with object number '%s' and generation number '%s'",
                            first, second), nfe);
                }
            }
        }
        position(offset);
        return COSNumber.get(first);
    }

    public IndirectObjectsProvider provider()
    {
        return provider;
    }

    /**
     * Closes the parser but not the associated provider
     */
    @Override
    public void close() throws IOException
    {
        super.close();
    }
}
