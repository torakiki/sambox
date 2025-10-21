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
import static java.util.Optional.ofNullable;

import java.awt.geom.GeneralPath;
import java.io.IOException;
import java.io.InputStream;

import org.apache.fontbox.FontBoxFont;
import org.apache.fontbox.util.BoundingBox;
import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSNumber;
import org.sejda.sambox.cos.COSStream;
import org.sejda.sambox.pdmodel.PDResources;
import org.sejda.sambox.pdmodel.ResourceCache;
import org.sejda.sambox.pdmodel.common.PDRectangle;
import org.sejda.sambox.pdmodel.font.encoding.DictionaryEncoding;
import org.sejda.sambox.pdmodel.font.encoding.Encoding;
import org.sejda.sambox.pdmodel.font.encoding.GlyphList;
import org.sejda.sambox.util.Matrix;
import org.sejda.sambox.util.Vector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A PostScript Type 3 Font.
 *
 * @author Ben Litchfield
 */
public class PDType3Font extends PDSimpleFont
{
    private static final Logger LOG = LoggerFactory.getLogger(PDType3Font.class);

    private PDResources resources;
    private Matrix fontMatrix;
    private BoundingBox fontBBox;
    private final ResourceCache resourceCache;

    public PDType3Font(COSDictionary fontDictionary) throws IOException
    {
        this(fontDictionary, null);
    }

    public PDType3Font(COSDictionary fontDictionary, ResourceCache resourceCache) throws IOException
    {
        super(fontDictionary);
        this.resourceCache = resourceCache;
        readEncoding();
    }

    @Override
    public String getName()
    {
        return getCOSObject().getNameAsString(COSName.NAME);
    }

    @Override
    protected final void readEncoding()
    {
        COSBase encodingBase = getCOSObject().getDictionaryObject(COSName.ENCODING);
        if (encodingBase instanceof COSName encodingName)
        {
            encoding = Encoding.getInstance(encodingName);
            if (encoding == null)
            {
                LOG.warn("Unknown encoding: {}", encodingName.getName());
            }
        }
        else if (encodingBase instanceof COSDictionary)
        {
            encoding = new DictionaryEncoding((COSDictionary) encodingBase);
        }
        glyphList = GlyphList.getAdobeGlyphList();
    }

    @Override
    protected Encoding readEncodingFromFont()
    {
        // Type 3 fonts do not have a built-in encoding
        throw new UnsupportedOperationException("not supported for Type 3 fonts");
    }

    @Override
    protected Boolean isFontSymbolic()
    {
        return false;
    }

    @Override
    public GeneralPath getPath(String name)
    {
        // Type 3 fonts do not use vector paths
        throw new UnsupportedOperationException("not supported for Type 3 fonts");
    }

    @Override
    public boolean hasGlyph(String name)
    {
        return ofNullable(getCharProcs()).map(
                        d -> nonNull(d.getDictionaryObject(COSName.getPDFName(name), COSStream.class)))
                .orElse(false);
    }

    @Override
    public FontBoxFont getFontBoxFont()
    {
        // Type 3 fonts do not use FontBox fonts
        throw new UnsupportedOperationException("not supported for Type 3 fonts");
    }

    @Override
    public Vector getDisplacement(int code) throws IOException
    {
        return getFontMatrix().transform(new Vector(getWidth(code), 0));
    }

    @Override
    public float getWidthFromFont(int code) throws IOException
    {
        PDType3CharProc charProc = getCharProc(code);
        if (charProc == null || charProc.getContentStream().getLength() == 0)
        {
            return 0;
        }
        return charProc.getWidth();
    }

    @Override
    public boolean isEmbedded()
    {
        return true;
    }

    @Override
    public float getHeight(int code)
    {
        PDFontDescriptor desc = getFontDescriptor();
        if (desc != null)
        {
            // the following values are all more or less accurate at least all are average
            // values. Maybe we'll find another way to get those value for every single glyph
            // in the future if needed
            PDRectangle bbox = desc.getFontBoundingBox();
            float retval = 0;
            if (bbox != null)
            {
                retval = bbox.getHeight() / 2;
            }
            if (retval == 0)
            {
                retval = desc.getCapHeight();
            }
            if (retval == 0)
            {
                retval = desc.getAscent();
            }
            if (retval == 0)
            {
                retval = desc.getXHeight();
                if (retval > 0)
                {
                    retval -= desc.getDescent();
                }
            }
            return retval;
        }
        return 0;
    }

    @Override
    protected byte[] encode(int unicode)
    {
        throw new UnsupportedOperationException("Not implemented: Type3");
    }

    @Override
    public int readCode(InputStream in) throws IOException
    {
        return in.read();
    }

    @Override
    public Matrix getFontMatrix()
    {
        if (fontMatrix == null)
        {
            COSArray matrix = getCOSObject().getDictionaryObject(COSName.FONT_MATRIX,
                    COSArray.class);
            fontMatrix = checkFontMatrixValues(matrix) ? Matrix.createMatrix(
                    matrix) : super.getFontMatrix();
        }
        return fontMatrix;
    }

    private boolean checkFontMatrixValues(COSArray matrix)
    {
        if (matrix == null || matrix.size() != 6)
        {
            return false;
        }
        for (COSBase element : matrix.toList())
        {
            if (!(element instanceof COSNumber))
                return false;
        }
        return true;
    }

    @Override
    public boolean isDamaged()
    {
        // there's no font file to load
        return false;
    }

    @Override
    public boolean isStandard14()
    {
        return false;
    }

    /**
     * @return the optional resources bound to be used when parsing the type3 stream
     */
    public PDResources getResources()
    {
        if (resources == null)
        {
            var resourcesDict = getCOSObject().getDictionaryObject(COSName.RESOURCES,
                    COSDictionary.class);
            if (resourcesDict != null)
            {
                this.resources = new PDResources(resourcesDict, resourceCache);
            }
        }
        return resources;
    }

    /**
     * @return The fonts bounding box.
     */
    public PDRectangle getFontBBox()
    {
        return ofNullable(
                getCOSObject().getDictionaryObject(COSName.FONT_BBOX, COSArray.class)).map(
                PDRectangle::new).orElse(null);
    }

    @Override
    public BoundingBox getBoundingBox()
    {
        if (fontBBox == null)
        {
            fontBBox = generateBoundingBox();
        }
        return fontBBox;
    }

    private BoundingBox generateBoundingBox()
    {
        PDRectangle rect = getFontBBox();
        if (nonNull(rect))
        {
            if (rect.getLowerLeftX() == 0 && rect.getLowerLeftY() == 0 && rect.getUpperRightX() == 0
                    && rect.getUpperRightY() == 0)
            {
                // Plan B: get the max bounding box of the glyphs
                COSDictionary cp = getCharProcs();
                if (nonNull(cp))
                {
                    for (COSName name : cp.keySet())
                    {
                        COSStream stream = cp.getDictionaryObject(name, COSStream.class);
                        if (nonNull(stream))
                        {
                            PDType3CharProc charProc = new PDType3CharProc(this, stream);
                            PDRectangle glyphBBox = charProc.getGlyphBBox();
                            if (nonNull(glyphBBox))
                            {
                                rect.setLowerLeftX(
                                        Math.min(rect.getLowerLeftX(), glyphBBox.getLowerLeftX()));
                                rect.setLowerLeftY(
                                        Math.min(rect.getLowerLeftY(), glyphBBox.getLowerLeftY()));
                                rect.setUpperRightX(Math.max(rect.getUpperRightX(),
                                        glyphBBox.getUpperRightX()));
                                rect.setUpperRightY(Math.max(rect.getUpperRightY(),
                                        glyphBBox.getUpperRightY()));
                            }
                        }
                    }
                }
            }
            return new BoundingBox(rect.getLowerLeftX(), rect.getLowerLeftY(),
                    rect.getUpperRightX(), rect.getUpperRightY());
        }
        LOG.warn("FontBBox missing, returning empty rectangle");
        return new BoundingBox();
    }

    /**
     * @return the dictionary containing all streams to be used to render the glyphs.
     */
    public COSDictionary getCharProcs()
    {
        return getCOSObject().getDictionaryObject(COSName.CHAR_PROCS, COSDictionary.class);
    }

    /**
     * Returns the stream of the glyph for the given character code
     *
     * @param code character code
     * @return the stream to be used to render the glyph
     */
    public PDType3CharProc getCharProc(int code)
    {
        Encoding encoding = getEncoding();
        if (nonNull(encoding))
        {
            String name = encoding.getName(code);
            return ofNullable(getCharProcs()).map(
                            d -> d.getDictionaryObject(COSName.getPDFName(name), COSStream.class))
                    .map(s -> new PDType3CharProc(this, s)).orElse(null);
        }
        return null;
    }

    @Override
    public boolean isOriginalEmbeddedMissing()
    {
        return false;
    }

    @Override
    public boolean isMappingFallbackUsed()
    {
        return false;
    }
}
