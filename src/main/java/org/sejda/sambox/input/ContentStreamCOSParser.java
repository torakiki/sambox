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

import java.io.IOException;

import org.sejda.io.SeekableSource;
import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSBoolean;
import org.sejda.sambox.cos.COSNull;

/**
 * A component capable of parsing COS objects in a content stream as defined in in Chap 7.8.2 of PDF 32000-1:2008, where
 * "Indirect objects and object references shall not be permitted at all".
 * 
 * @author Andrea Vacondio
 */
class ContentStreamCOSParser extends BaseCOSParser
{

    ContentStreamCOSParser(SeekableSource source)
    {
        super(source);
    }

    /**
     * @return The next parsed basic type object from the stream or null if the next token is not a COSBase. Basic types
     * are defined in Chap 7.3 of PDF 32000-1:2008 except for Chap 7.3.10. This method doesn't parse indirect objects
     * definition because they are not permitted in contest stream.
     * @throws IOException If there is an error during parsing.
     */
    @Override
    public COSBase nextParsedToken() throws IOException
    {
        String token;
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
            token = readToken();
            if ("null".equals(token))
            {
                return COSNull.NULL;
            }
            return null;
        case 't':
        case 'f':
            token = readToken();
            if ("true".equals(token))
            {
                return COSBoolean.TRUE;
            }
            if ("false".equals(token))
            {
                return COSBoolean.FALSE;
            }
            return null;
        case '.':
        case '-':
        case '+':
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
            return nextNumber();
        case (char) -1:
            return null;
        default:
            String badString = readToken();
            if (badString.length() <= 0)
            {
                // we are at a zero length end of token, we try to skip it and hopefully recover
                source().read();
            }
            return null;
        }
    }
}
