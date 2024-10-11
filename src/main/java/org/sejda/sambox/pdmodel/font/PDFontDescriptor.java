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

import static java.util.Optional.ofNullable;

import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSObjectable;
import org.sejda.sambox.cos.COSStream;
import org.sejda.sambox.cos.COSString;
import org.sejda.sambox.pdmodel.common.PDDictionaryWrapper;
import org.sejda.sambox.pdmodel.common.PDRectangle;
import org.sejda.sambox.pdmodel.common.PDStream;

/**
 * @author Ben Litchfield
 */
public final class PDFontDescriptor extends PDDictionaryWrapper implements COSObjectable
{
    private static final int FLAG_FIXED_PITCH = 1;
    private static final int FLAG_SERIF = 2;
    private static final int FLAG_SYMBOLIC = 4;
    private static final int FLAG_SCRIPT = 8;
    private static final int FLAG_NON_SYMBOLIC = 32;
    private static final int FLAG_ITALIC = 64;
    private static final int FLAG_ALL_CAP = 65536;
    private static final int FLAG_SMALL_CAP = 131072;
    private static final int FLAG_FORCE_BOLD = 262144;

    private float xHeight = Float.NEGATIVE_INFINITY;
    private float capHeight = Float.NEGATIVE_INFINITY;
    private int flags = -1;

    /**
     * Package-private constructor, for embedding.
     */
    PDFontDescriptor()
    {
        super(COSDictionary.of(COSName.TYPE, COSName.FONT_DESC));
    }

    /**
     * @param dictionary The wrapped COS Dictionary.
     */
    public PDFontDescriptor(COSDictionary dictionary)
    {
        super(dictionary);
    }

    /**
     * A convenience method that checks the flag bit.
     *
     * @return The flag value.
     */
    public boolean isFixedPitch()
    {
        return isFlagBitOn(FLAG_FIXED_PITCH);
    }

    /**
     * A convenience method that sets the flag bit.
     *
     * @param flag The flag value.
     */
    public void setFixedPitch(boolean flag)
    {
        setFlagBit(FLAG_FIXED_PITCH, flag);
    }

    /**
     * A convenience method that checks the flag bit.
     *
     * @return The flag value.
     */
    public boolean isSerif()
    {
        return isFlagBitOn(FLAG_SERIF);
    }

    /**
     * A convenience method that sets the flag bit.
     *
     * @param flag The flag value.
     */
    public void setSerif(boolean flag)
    {
        setFlagBit(FLAG_SERIF, flag);
    }

    /**
     * A convenience method that checks the flag bit.
     *
     * @return The flag value.
     */
    public boolean isSymbolic()
    {
        return isFlagBitOn(FLAG_SYMBOLIC);
    }

    /**
     * A convenience method that sets the flag bit.
     *
     * @param flag The flag value.
     */
    public void setSymbolic(boolean flag)
    {
        setFlagBit(FLAG_SYMBOLIC, flag);
    }

    /**
     * A convenience method that checks the flag bit.
     *
     * @return The flag value.
     */
    public boolean isScript()
    {
        return isFlagBitOn(FLAG_SCRIPT);
    }

    /**
     * A convenience method that sets the flag bit.
     *
     * @param flag The flag value.
     */
    public void setScript(boolean flag)
    {
        setFlagBit(FLAG_SCRIPT, flag);
    }

    /**
     * A convenience method that checks the flag bit.
     *
     * @return The flag value.
     */
    public boolean isNonSymbolic()
    {
        return isFlagBitOn(FLAG_NON_SYMBOLIC);
    }

    /**
     * A convenience method that sets the flag bit.
     *
     * @param flag The flag value.
     */
    public void setNonSymbolic(boolean flag)
    {
        setFlagBit(FLAG_NON_SYMBOLIC, flag);
    }

    /**
     * A convenience method that checks the flag bit.
     *
     * @return The flag value.
     */
    public boolean isItalic()
    {
        return isFlagBitOn(FLAG_ITALIC);
    }

    /**
     * A convenience method that sets the flag bit.
     *
     * @param flag The flag value.
     */
    public void setItalic(boolean flag)
    {
        setFlagBit(FLAG_ITALIC, flag);
    }

    /**
     * A convenience method that checks the flag bit.
     *
     * @return The flag value.
     */
    public boolean isAllCap()
    {
        return isFlagBitOn(FLAG_ALL_CAP);
    }

    /**
     * A convenience method that sets the flag bit.
     *
     * @param flag The flag value.
     */
    public void setAllCap(boolean flag)
    {
        setFlagBit(FLAG_ALL_CAP, flag);
    }

    /**
     * A convenience method that checks the flag bit.
     *
     * @return The flag value.
     */
    public boolean isSmallCap()
    {
        return isFlagBitOn(FLAG_SMALL_CAP);
    }

    /**
     * A convenience method that sets the flag bit.
     *
     * @param flag The flag value.
     */
    public void setSmallCap(boolean flag)
    {
        setFlagBit(FLAG_SMALL_CAP, flag);
    }

    /**
     * A convenience method that checks the flag bit.
     *
     * @return The flag value.
     */
    public boolean isForceBold()
    {
        return isFlagBitOn(FLAG_FORCE_BOLD);
    }

    /**
     * A convenience method that sets the flag bit.
     *
     * @param flag The flag value.
     */
    public void setForceBold(boolean flag)
    {
        setFlagBit(FLAG_FORCE_BOLD, flag);
    }

    private boolean isFlagBitOn(int bit)
    {
        return (getFlags() & bit) != 0;
    }

    private void setFlagBit(int bit, boolean value)
    {
        int flags = getFlags();
        if (value)
        {
            flags = flags | bit;
        }
        else
        {
            flags = flags & (~bit);
        }
        setFlags(flags);
    }


    public String getFontName()
    {
        return ofNullable(getCOSObject().getDictionaryObject(COSName.FONT_NAME, COSName.class)).map(
                COSName::getName).orElse(null);
    }

    public void setFontName(String fontName)
    {
        getCOSObject().setItem(COSName.FONT_NAME, COSName.getPDFName(fontName));
    }

    /**
     * @return A string representing the preferred font family.
     */
    public String getFontFamily()
    {
        return ofNullable(
                getCOSObject().getDictionaryObject(COSName.FONT_FAMILY, COSString.class)).map(
                COSString::getString).orElse(null);
    }

    public void setFontFamily(String fontFamily)
    {
        getCOSObject().setItem(COSName.FONT_FAMILY, COSString.parseLiteral(fontFamily));
    }

    /**
     * The weight of the font. According to the PDF spec "possible values are 100, 200, 300, 400,
     * 500, 600, 700, 800 or 900" Where a higher number is more weight and appears to be more bold.
     *
     * @return The font weight.
     */
    public float getFontWeight()
    {
        return getCOSObject().getFloat(COSName.FONT_WEIGHT, 0);
    }

    public void setFontWeight(float fontWeight)
    {
        getCOSObject().setFloat(COSName.FONT_WEIGHT, fontWeight);
    }

    /**
     * A string representing the preferred font stretch. According to the PDF Spec: The font stretch
     * value; it must be one of the following (ordered from narrowest to widest): UltraCondensed,
     * ExtraCondensed, Condensed, SemiCondensed, Normal, SemiExpanded, Expanded, ExtraExpanded or
     * UltraExpanded.
     *
     * @return The stretch of the font.
     */
    public String getFontStretch()
    {
        return ofNullable(
                getCOSObject().getDictionaryObject(COSName.FONT_STRETCH, COSName.class)).map(
                COSName::getName).orElse(null);
    }

    public void setFontStretch(String fontStretch)
    {
        getCOSObject().setItem(COSName.FONT_STRETCH, COSName.getPDFName(fontStretch));
    }

    /**
     * @return The font flags.
     */
    public int getFlags()
    {
        if (flags == -1)
        {
            flags = getCOSObject().getInt(COSName.FLAGS, 0);
        }
        return flags;
    }

    /**
     * @param flags The new font flags.
     */
    public void setFlags(int flags)
    {
        getCOSObject().setInt(COSName.FLAGS, flags);
        this.flags = flags;
    }

    public PDRectangle getFontBoundingBox()
    {
        return ofNullable(
                getCOSObject().getDictionaryObject(COSName.FONT_BBOX, COSArray.class)).map(
                PDRectangle::new).orElse(null);
    }

    public void setFontBoundingBox(PDRectangle rect)
    {
        getCOSObject().setItem(COSName.FONT_BBOX,
                ofNullable(rect).map(PDRectangle::getCOSObject).orElse(null));
    }

    public float getItalicAngle()
    {
        return getCOSObject().getFloat(COSName.ITALIC_ANGLE, 0);
    }

    public void setItalicAngle(float angle)
    {
        getCOSObject().setFloat(COSName.ITALIC_ANGLE, angle);
    }

    public float getAscent()
    {
        return getCOSObject().getFloat(COSName.ASCENT, 0);
    }

    public void setAscent(float ascent)
    {
        getCOSObject().setFloat(COSName.ASCENT, ascent);
    }

    public float getDescent()
    {
        return getCOSObject().getFloat(COSName.DESCENT, 0);
    }

    public void setDescent(float descent)
    {
        getCOSObject().setFloat(COSName.DESCENT, descent);
    }

    public float getLeading()
    {
        return getCOSObject().getFloat(COSName.LEADING, 0);
    }

    public void setLeading(float leading)
    {
        getCOSObject().setFloat(COSName.LEADING, leading);
    }

    public float getCapHeight()
    {
        if (capHeight == Float.NEGATIVE_INFINITY)
        {
            /*
             * We observed a negative value being returned with the Scheherazade font. PDFBOX-429 was logged for this.
             * We are not sure if returning the absolute value is the correct fix, but it seems to work.
             */
            capHeight = Math.abs(getCOSObject().getFloat(COSName.CAP_HEIGHT, 0));
        }
        return capHeight;
    }

    public void setCapHeight(float capHeight)
    {
        getCOSObject().setFloat(COSName.CAP_HEIGHT, capHeight);
        this.capHeight = capHeight;
    }

    public float getXHeight()
    {
        if (xHeight == Float.NEGATIVE_INFINITY)
        {
            /*
             * We observed a negative value being returned with the Scheherazade font. PDFBOX-429 was logged for this.
             * We are not sure if returning the absolute value is the correct fix, but it seems to work.
             */
            xHeight = Math.abs(getCOSObject().getFloat(COSName.XHEIGHT, 0));
        }
        return xHeight;
    }

    public void setXHeight(float xHeight)
    {
        getCOSObject().setFloat(COSName.XHEIGHT, xHeight);
        this.xHeight = xHeight;
    }

    public float getStemV()
    {
        return getCOSObject().getFloat(COSName.STEM_V, 0);
    }

    public void setStemV(float stemV)
    {
        getCOSObject().setFloat(COSName.STEM_V, stemV);
    }

    public float getStemH()
    {
        return getCOSObject().getFloat(COSName.STEM_H, 0);
    }

    public void setStemH(float stemH)
    {
        getCOSObject().setFloat(COSName.STEM_H, stemH);
    }

    public float getAverageWidth()
    {
        return getCOSObject().getFloat(COSName.AVG_WIDTH, 0);
    }

    public void setAverageWidth(float averageWidth)
    {
        getCOSObject().setFloat(COSName.AVG_WIDTH, averageWidth);
    }

    public float getMaxWidth()
    {
        return getCOSObject().getFloat(COSName.MAX_WIDTH, 0);
    }

    public void setMaxWidth(float maxWidth)
    {
        getCOSObject().setFloat(COSName.MAX_WIDTH, maxWidth);
    }

    /**
     * @return true if widths are present in the font descriptor.
     */
    public boolean hasWidths()
    {
        return getCOSObject().containsKey(COSName.WIDTHS) || getCOSObject().containsKey(
                COSName.MISSING_WIDTH);
    }

    /**
     * @return true if the missing widths entry is present in the font descriptor.
     */
    public boolean hasMissingWidth()
    {
        return getCOSObject().containsKey(COSName.MISSING_WIDTH);
    }

    /**
     * @return The missing width value, or 0 if there is no such dictionary entry.
     */
    public float getMissingWidth()
    {
        return getCOSObject().getFloat(COSName.MISSING_WIDTH, 0);
    }

    public void setMissingWidth(float missingWidth)
    {
        getCOSObject().setFloat(COSName.MISSING_WIDTH, missingWidth);
    }

    public String getCharSet()
    {
        return ofNullable(
                getCOSObject().getDictionaryObject(COSName.CHAR_SET, COSString.class)).map(
                COSString::getString).orElse(null);
    }

    public void setCharacterSet(String charSet)
    {
        getCOSObject().setItem(COSName.CHAR_SET, COSString.parseLiteral(charSet));
    }

    /**
     * @return A stream containing a Type 1 font program.
     */
    public PDStream getFontFile()
    {
        return ofNullable(
                getCOSObject().getDictionaryObject(COSName.FONT_FILE, COSStream.class)).map(
                PDStream::new).orElse(null);
    }

    /**
     * Set the type 1 font program.
     *
     * @param type1Stream The type 1 stream.
     */
    public void setFontFile(PDStream type1Stream)
    {
        getCOSObject().setItem(COSName.FONT_FILE, type1Stream);
    }

    /**
     * @return A stream containing a true type font program.
     */
    public PDStream getFontFile2()
    {
        return ofNullable(
                getCOSObject().getDictionaryObject(COSName.FONT_FILE2, COSStream.class)).map(
                PDStream::new).orElse(null);
    }

    /**
     * Set the true type font program.
     *
     * @param ttfStream The true type stream.
     */
    public void setFontFile2(PDStream ttfStream)
    {
        getCOSObject().setItem(COSName.FONT_FILE2, ttfStream);
    }

    /**
     * @return A stream containing a font program that is not true type or type 1.
     */
    public PDStream getFontFile3()
    {
        return ofNullable(
                getCOSObject().getDictionaryObject(COSName.FONT_FILE3, COSStream.class)).map(
                PDStream::new).orElse(null);
    }

    /**
     * Set a stream containing a font program that is not true type or type 1.
     *
     * @param stream The font program stream.
     */
    public void setFontFile3(PDStream stream)
    {
        getCOSObject().setItem(COSName.FONT_FILE3, stream);
    }

    /**
     * @return A stream containing a CIDSet.
     */
    public PDStream getCIDSet()
    {
        return ofNullable(getCOSObject().getDictionaryObject(COSName.CID_SET, COSStream.class)).map(
                PDStream::new).orElse(null);
    }

    /**
     * @param stream a stream containing a CIDSet.
     */
    public void setCIDSet(PDStream stream)
    {
        getCOSObject().setItem(COSName.CID_SET, stream);
    }

    /**
     * @return the Panose entry of the Style dictionary, if any.
     */
    public PDPanose getPanose()
    {
        return ofNullable(
                getCOSObject().getDictionaryObject(COSName.STYLE, COSDictionary.class)).map(
                        s -> s.getDictionaryObject(COSName.PANOSE, COSString.class))
                .map(COSString::getBytes).map(bytes -> {
                    if (bytes.length >= PDPanose.LENGTH)
                    {
                        return new PDPanose(bytes);
                    }
                    return null;
                }).orElse(null);

    }
}
