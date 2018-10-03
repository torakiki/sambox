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

import static org.sejda.sambox.pdmodel.font.FontUtils.getTag;
import static org.sejda.sambox.pdmodel.font.FontUtils.isEmbeddingPermitted;
import static org.sejda.sambox.pdmodel.font.FontUtils.isSubsettingPermitted;

import java.awt.geom.GeneralPath;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.fontbox.ttf.CmapLookup;
import org.apache.fontbox.ttf.CmapSubtable;
import org.apache.fontbox.ttf.HeaderTable;
import org.apache.fontbox.ttf.HorizontalHeaderTable;
import org.apache.fontbox.ttf.OS2WindowsMetricsTable;
import org.apache.fontbox.ttf.PostScriptTable;
import org.apache.fontbox.ttf.TTFParser;
import org.apache.fontbox.ttf.TTFSubsetter;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.sejda.commons.util.IOUtils;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.pdmodel.common.PDRectangle;
import org.sejda.sambox.pdmodel.common.PDStream;
/**
 * Common functionality for embedding TrueType fonts.
 *
 * @author Ben Litchfield
 * @author John Hewson
 */
abstract class TrueTypeEmbedder implements Subsetter
{
    private static final int ITALIC = 1;
    private static final int OBLIQUE = 512;

    protected TrueTypeFont ttf;
    protected PDFontDescriptor fontDescriptor;
    @Deprecated
    protected final CmapSubtable cmap;

    protected final CmapLookup cmapLookup;
    private final Set<Integer> subsetCodePoints = new HashSet<>();
    private final boolean embedSubset;

    /**
     * Creates a new TrueType font for embedding.
     */
    TrueTypeEmbedder(COSDictionary dict, TrueTypeFont ttf, boolean embedSubset) throws IOException
    {
        this.embedSubset = embedSubset;
        this.ttf = ttf;
        fontDescriptor = createFontDescriptor(ttf);

        if (!isEmbeddingPermitted(ttf))
        {
            throw new IOException("This font does not permit embedding");
        }

        if (!embedSubset)
        {
            // full embedding
            PDStream stream = new PDStream(ttf.getOriginalData(), COSName.FLATE_DECODE);
            stream.getCOSObject().setLong(COSName.LENGTH1, ttf.getOriginalDataSize());
            fontDescriptor.setFontFile2(stream);
        }

        dict.setName(COSName.BASE_FONT, ttf.getName());

        // choose a Unicode "cmap"
        cmap = ttf.getUnicodeCmap();
        cmapLookup = ttf.getUnicodeCmapLookup();
    }

    public void buildFontFile2(InputStream ttfStream) throws IOException
    {
        PDStream stream = new PDStream(ttfStream, COSName.FLATE_DECODE);

        // as the stream was closed within the PDStream constructor, we have to recreate it
        InputStream input = null;
        try
        {
            input = stream.createInputStream();
            ttf = new TTFParser().parseEmbedded(input);
            if (!isEmbeddingPermitted(ttf))
            {
                throw new IOException("This font does not permit embedding");
            }
            if (fontDescriptor == null)
            {
                fontDescriptor = createFontDescriptor(ttf);
            }
        }
        finally
        {
            IOUtils.closeQuietly(input);
        }
        stream.getCOSObject().setLong(COSName.LENGTH1, ttf.getOriginalDataSize());
        fontDescriptor.setFontFile2(stream);
    }


    /**
     * Creates a new font descriptor dictionary for the given TTF.
     */
    private PDFontDescriptor createFontDescriptor(TrueTypeFont ttf) throws IOException
    {
        PDFontDescriptor fd = new PDFontDescriptor();
        fd.setFontName(ttf.getName());

        OS2WindowsMetricsTable os2 = ttf.getOS2Windows();
        PostScriptTable post = ttf.getPostScript();

        // Flags
        fd.setFixedPitch(
                post.getIsFixedPitch() > 0 || ttf.getHorizontalHeader().getNumberOfHMetrics() == 1);

        int fsSelection = os2.getFsSelection();
        fd.setItalic(((fsSelection & (ITALIC | OBLIQUE)) != 0));

        switch (os2.getFamilyClass())
        {
        case OS2WindowsMetricsTable.FAMILY_CLASS_CLAREDON_SERIFS:
        case OS2WindowsMetricsTable.FAMILY_CLASS_FREEFORM_SERIFS:
        case OS2WindowsMetricsTable.FAMILY_CLASS_MODERN_SERIFS:
        case OS2WindowsMetricsTable.FAMILY_CLASS_OLDSTYLE_SERIFS:
        case OS2WindowsMetricsTable.FAMILY_CLASS_SLAB_SERIFS:
            fd.setSerif(true);
            break;
        case OS2WindowsMetricsTable.FAMILY_CLASS_SCRIPTS:
            fd.setScript(true);
            break;
        default:
            break;
        }

        fd.setFontWeight(os2.getWeightClass());

        fd.setSymbolic(true);
        fd.setNonSymbolic(false);

        // ItalicAngle
        fd.setItalicAngle(post.getItalicAngle());

        // FontBBox
        HeaderTable header = ttf.getHeader();
        PDRectangle rect = new PDRectangle();
        float scaling = 1000f / header.getUnitsPerEm();
        rect.setLowerLeftX(header.getXMin() * scaling);
        rect.setLowerLeftY(header.getYMin() * scaling);
        rect.setUpperRightX(header.getXMax() * scaling);
        rect.setUpperRightY(header.getYMax() * scaling);
        fd.setFontBoundingBox(rect);

        // Ascent, Descent
        HorizontalHeaderTable hHeader = ttf.getHorizontalHeader();
        fd.setAscent(hHeader.getAscender() * scaling);
        fd.setDescent(hHeader.getDescender() * scaling);

        // CapHeight, XHeight
        if (os2.getVersion() >= 1.2)
        {
            fd.setCapHeight(os2.getCapHeight() * scaling);
            fd.setXHeight(os2.getHeight() * scaling);
        }
        else
        {
            GeneralPath capHPath = ttf.getPath("H");
            if (capHPath != null)
            {
                fd.setCapHeight(Math.round(capHPath.getBounds2D().getMaxY()) * scaling);
            }
            else
            {
                // estimate by summing the typographical +ve ascender and -ve descender
                fd.setCapHeight((os2.getTypoAscender() + os2.getTypoDescender()) * scaling);
            }
            GeneralPath xPath = ttf.getPath("x");
            if (xPath != null)
            {
                fd.setXHeight(Math.round(xPath.getBounds2D().getMaxY()) * scaling);
            }
            else
            {
                // estimate by halving the typographical ascender
                fd.setXHeight(os2.getTypoAscender() / 2.0f * scaling);
            }
        }

        // StemV - there's no true TTF equivalent of this, so we estimate it
        fd.setStemV(fd.getFontBoundingBox().getWidth() * .13f);

        return fd;
    }

    /**
     * Returns the FontBox font.
     */
    @Deprecated
    public TrueTypeFont getTrueTypeFont()
    {
        return ttf;
    }

    /**
     * Returns the font descriptor.
     */
    public PDFontDescriptor getFontDescriptor()
    {
        return fontDescriptor;
    }

    @Override
    public void addToSubset(int codePoint)
    {
        subsetCodePoints.add(codePoint);
    }

    @Override
    public void subset() throws IOException
    {
        if (!isSubsettingPermitted(ttf))
        {
            throw new IOException("This font does not permit subsetting");
        }

        if (!embedSubset)
        {
            throw new IllegalStateException("Subsetting is disabled");
        }

        // PDF spec required tables (if present), all others will be removed
        // set the GIDs to subset
        TTFSubsetter subsetter = new TTFSubsetter(ttf, Arrays.asList("head", "hhea", "loca", "maxp",
                "cvt", "prep", "glyf", "hmtx", "fpgm", "gasp"));
        subsetter.addAll(subsetCodePoints);

        // calculate deterministic tag based on the chosen subset
        Map<Integer, Integer> gidToCid = subsetter.getGIDMap();
        String tag = getTag(gidToCid);
        subsetter.setPrefix(tag);

        // save the subset font
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        subsetter.writeToStream(out);

        // re-build the embedded font
        buildSubset(new ByteArrayInputStream(out.toByteArray()), tag, gidToCid);
        ttf.close();
    }

    /**
     * @return true if the font needs to be subset.
     */
    public boolean needsSubset()
    {
        return embedSubset;
    }

    /**
     * @return a font subset.
     */
    protected abstract void buildSubset(InputStream ttfSubset, String tag,
            Map<Integer, Integer> gidToCid) throws IOException;


}
