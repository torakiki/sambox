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

package org.sejda.sambox.pdmodel.font;

import static java.util.Objects.nonNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.sejda.sambox.contentstream.PDContentStream;
import org.sejda.sambox.contentstream.operator.Operator;
import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSNumber;
import org.sejda.sambox.cos.COSObjectable;
import org.sejda.sambox.cos.COSStream;
import org.sejda.sambox.input.ContentStreamParser;
import org.sejda.sambox.pdmodel.PDResources;
import org.sejda.sambox.pdmodel.common.PDRectangle;
import org.sejda.sambox.pdmodel.common.PDStream;
import org.sejda.sambox.util.Matrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Type 3 character procedure. This is a standalone PDF content stream.
 *
 * @author John Hewson
 */
public final class PDType3CharProc implements COSObjectable, PDContentStream
{
    private static final Logger LOG = LoggerFactory.getLogger(PDType3CharProc.class);

    private final PDType3Font font;
    private final COSStream charStream;

    public PDType3CharProc(PDType3Font font, COSStream charStream)
    {
        this.font = font;
        this.charStream = charStream;
    }

    @Override
    public COSStream getCOSObject()
    {
        return charStream;
    }

    public PDType3Font getFont()
    {
        return font;
    }

    public PDStream getContentStream()
    {
        return new PDStream(charStream);
    }

    @Override
    public InputStream getContents() throws IOException
    {
        return charStream.getUnfilteredStream();
    }

    @Override
    public PDResources getResources()
    {
        COSDictionary resources = charStream.getDictionaryObject(COSName.RESOURCES,
                COSDictionary.class);
        if (nonNull(resources))
        {
            // PDFBOX-5294
            LOG.warn("Using resources dictionary found in charproc entry");
            LOG.warn("This should have been in the font or in the page dictionary");
            return new PDResources(resources);
        }
        return font.getResources();
    }

    @Override
    public PDRectangle getBBox()
    {
        return font.getFontBBox();
    }

    /**
     * Calculate the bounding box of this glyph. This will work only if the first operator in the
     * stream is d1.
     *
     * @return the bounding box of this glyph, or null if the first operator is not d1.
     */
    public PDRectangle getGlyphBBox()
    {
        List<COSBase> arguments = new ArrayList<>();
        try (var parser = new ContentStreamParser(this))
        {
            Object token = null;
            while ((token = parser.nextParsedToken()) != null)
            {
                if (token instanceof Operator operator)
                {
                    if ("d1".equals(operator.getName()) && arguments.size() == 6)
                    {
                        for (int i = 0; i < 6; ++i)
                        {
                            if (!(arguments.get(i) instanceof COSNumber))
                            {
                                return null;
                            }
                        }
                        float x = ((COSNumber) arguments.get(2)).floatValue();
                        float y = ((COSNumber) arguments.get(3)).floatValue();
                        return new PDRectangle(x, y,
                                ((COSNumber) arguments.get(4)).floatValue() - x,
                                ((COSNumber) arguments.get(5)).floatValue() - y);
                    }
                    return null;
                }
                arguments.add((COSBase) token);
            }
        }
        catch (IOException e)
        {
            LOG.warn("An error occurred while calculating the glyph bbox", e);
        }
        return null;
    }

    @Override
    public Matrix getMatrix()
    {
        return font.getFontMatrix();
    }

    /**
     * Get the width from a type3 charproc stream.
     *
     * @return the glyph width.
     * @throws IOException if the stream could not be read, or did not have d0 or d1 as first
     *                     operator, or if their first argument was not a number.
     */
    public float getWidth() throws IOException
    {
        List<COSBase> arguments = new ArrayList<>();
        try (var parser = new ContentStreamParser(this))
        {
            Object token = null;
            while ((token = parser.nextParsedToken()) != null)
            {
                if (token instanceof Operator operator)
                {
                    return parseWidth(operator, arguments);
                }
                if (token instanceof COSBase argument)
                {
                    arguments.add(argument.getCOSObject());
                }
            }
        }
        throw new IOException("Unexpected end of stream");
    }

    private float parseWidth(Operator operator, List<COSBase> arguments) throws IOException
    {
        if ("d0".equals(operator.getName()) || "d1".equals(operator.getName()))
        {
            COSBase fist = arguments.getFirst();
            if (fist instanceof COSNumber number)
            {
                return number.floatValue();
            }
            throw new IOException("Expected COSNumber but was: " + fist);
        }
        throw new IOException("First operator must be d0 or d1");
    }
}
