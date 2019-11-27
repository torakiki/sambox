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

import static org.sejda.sambox.util.CharUtils.ASCII_SPACE;
import static org.sejda.sambox.util.CharUtils.isEOF;
import static org.sejda.sambox.util.CharUtils.isEOL;
import static org.sejda.sambox.util.CharUtils.isNul;
import static org.sejda.sambox.util.CharUtils.isSpace;
import static org.sejda.sambox.util.CharUtils.isWhitespace;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.sejda.commons.FastByteArrayOutputStream;
import org.sejda.commons.util.IOUtils;
import org.sejda.io.SeekableSource;
import org.sejda.io.SeekableSources;
import org.sejda.sambox.contentstream.PDContentStream;
import org.sejda.sambox.contentstream.operator.Operator;
import org.sejda.sambox.contentstream.operator.OperatorName;
import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;

/**
 * Component responsible for parsing a a content stream to extract operands and such.
 * 
 * @author Andrea Vacondio
 */
public class ContentStreamParser extends SourceReader
{
    private ContentStreamCOSParser cosParser;
    private List<Object> tokens = new ArrayList<>();

    public ContentStreamParser(PDContentStream stream) throws IOException
    {
        this(SeekableSources.inMemorySeekableSourceFrom(stream.getContents()));
    }

    public ContentStreamParser(SeekableSource source)
    {
        super(source);
        this.cosParser = new ContentStreamCOSParser(source());
    }

    /**
     * @return a list of tokens retrieved parsing the source this parser was created from.
     * @throws IOException
     */
    public List<Object> tokens() throws IOException
    {
        tokens.clear();
        Object token;
        while ((token = nextParsedToken()) != null)
        {
            tokens.add(token);
        }
        return Collections.unmodifiableList(tokens);
    }

    /**
     * @return the next token parsed from the content stream
     * @throws IOException
     */
    public Object nextParsedToken() throws IOException
    {
        skipSpaces();
        long pos = position();
        COSBase token = cosParser.nextParsedToken();
        if (token != null)
        {
            return token;
        }
        position(pos);
        return nextOperator();
    }

    private Object nextOperator() throws IOException
    {
        if ('B' == (char) source().peek())
        {
            Operator operator = Operator.getOperator(readToken());
            if (OperatorName.BEGIN_INLINE_IMAGE.equals(operator.getName()))
            {
                nextInlineImage(operator);
            }
            return operator;
        }
        return Optional.ofNullable(readToken()).filter(s -> s.length() > 0)
                .map(Operator::getOperator).orElse(null);

    }

    private void nextInlineImage(Operator operator) throws IOException
    {
        COSDictionary imageParams = new COSDictionary();
        operator.setImageParameters(imageParams);
        COSBase nextToken = null;
        long position = position();
        while ((nextToken = cosParser.nextParsedToken()) instanceof COSName)
        {
            imageParams.setItem((COSName) nextToken, cosParser.nextParsedToken());
            position = position();
        }
        position(position);
        operator.setImageData(nextImageData());
    }

    /**
     * Reads data until it finds an "EI" operator followed by a whitespace.
     * 
     * @return the image data
     * @throws IOException
     */
    private byte[] nextImageData() throws IOException
    {
        skipSpaces();
        skipExpected(OperatorName.BEGIN_INLINE_IMAGE_DATA);
        if (!isWhitespace(source().read()))
        {
            source().back();
        }
        try (FastByteArrayOutputStream imageData = new FastByteArrayOutputStream())
        {
            int current;

            while ((current = source().read()) != -1)
            {
                long position = source().position();
                if ((current == 'E' && isEndOfImageFrom(position - 1))
                        || (isWhitespace(current) && isEndOfImageFrom(position)))
                {
                    break;
                }
                imageData.write(current);
            }
            return imageData.toByteArray();
        }
    }

    private boolean isEndOfImageFrom(long position) throws IOException
    {
        long currentPosition = source().position();
        source().position(position);
        int current = source().read();
        if (current == 'E')
        {
            current = source().read();
            // if not a EI we restore the position and go on
            if (current == 'I' && (isEndOfImage() || isEOF(source().peek())))
            {
                return true;
            }
        }
        source().position(currentPosition);
        return false;
    }

    private boolean isEndOfImage() throws IOException
    {
        long currentPosition = source().position();
        try
        {
            int current = source().read();
            // we do what PDF.js does
            if (isSpace(current) || isEOL(current))
            {
                // from PDF.js: Let's check the next ten bytes are ASCII... just be sure.
                for (int i = 0; i < 10; i++)
                {
                    current = source().read();
                    if (isNul(current) && !isNul(source().peek()))
                    {

                        // from PDF.js: NUL bytes are not supposed to occur *outside* of inline
                        // images, but some PDF generators violate that assumption,
                        // thus breaking the EI detection heuristics used below.
                        //
                        // However, we can't unconditionally treat NUL bytes as "ASCII",
                        // since that *could* result in inline images being truncated.
                        //
                        // To attempt to address this, we'll still treat any *sequence*
                        // of NUL bytes as non-ASCII, but for a *single* NUL byte we'll
                        // continue checking the `followingBytes` (fixes issue8823.pdf).
                        continue;
                    }
                    if (!isEOF(current) && !isEOL(current)
                            && (current < ASCII_SPACE || current > 0x7F))
                    {
                        // from PDF.js: Not a LF, CR, SPACE or any visible ASCII character, i.e. it's binary stuff.
                        return false;
                    }
                }
                return true;
            }
            return false;
        }
        finally
        {
            source().position(currentPosition);
        }
    }

    @Override
    public void close() throws IOException
    {
        super.close();
        IOUtils.closeQuietly(cosParser);
    }
}
