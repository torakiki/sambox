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

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNullElse;
import static java.util.Optional.ofNullable;

import java.awt.geom.GeneralPath;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.fontbox.FontBoxFont;
import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSArrayList;
import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSNumber;
import org.sejda.sambox.pdmodel.font.encoding.DictionaryEncoding;
import org.sejda.sambox.pdmodel.font.encoding.Encoding;
import org.sejda.sambox.pdmodel.font.encoding.GlyphList;
import org.sejda.sambox.pdmodel.font.encoding.MacRomanEncoding;
import org.sejda.sambox.pdmodel.font.encoding.StandardEncoding;
import org.sejda.sambox.pdmodel.font.encoding.WinAnsiEncoding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple font. Simple fonts use a PostScript encoding vector.
 *
 * @author John Hewson
 */
public abstract class PDSimpleFont extends PDFont
{
    private static final Logger LOG = LoggerFactory.getLogger(PDSimpleFont.class);

    protected Encoding encoding;
    protected GlyphList glyphList;
    private Boolean isSymbolic;
    private List<Float> widths;
    private final Map<Integer, Float> codeToWidth = new HashMap<>();
    private final Set<Integer> noUnicode = new HashSet<>(); // for logging

    /**
     * Constructor for embedding.
     */
    PDSimpleFont()
    {
    }

    /**
     * Constructor for Standard 14.
     */
    PDSimpleFont(String baseFont)
    {
        super(baseFont);
        assignGlyphList(baseFont);
    }

    /**
     * @param fontDictionary Font dictionary.
     */
    PDSimpleFont(COSDictionary fontDictionary) throws IOException
    {
        super(fontDictionary);
    }

    /**
     * Reads the Encoding from the Font dictionary or the embedded or substituted font file. Must be
     * called at the end of any subclass constructors.
     *
     * @throws IOException if the font file could not be read
     */
    protected void readEncoding() throws IOException
    {
        COSBase encodingBase = getCOSObject().getDictionaryObject(COSName.ENCODING);
        if (encodingBase instanceof COSName encodingName)
        {
            this.encoding = Encoding.getInstance(encodingName);
            if (this.encoding == null)
            {
                LOG.warn("Unknown encoding: " + encodingName.getName());
                this.encoding = readEncodingFromFont(); // fallback
            }
        }
        else if (encodingBase instanceof COSDictionary encodingDict)
        {
            Encoding builtIn = null;
            Boolean symbolic = getSymbolicFlag();

            COSName baseEncoding = encodingDict.getCOSName(COSName.BASE_ENCODING);

            boolean hasValidBaseEncoding =
                    baseEncoding != null && Encoding.getInstance(baseEncoding) != null;

            if (!hasValidBaseEncoding && Boolean.TRUE.equals(symbolic))
            {
                builtIn = readEncodingFromFont();
            }

            if (symbolic == null)
            {
                symbolic = false;
            }
            this.encoding = new DictionaryEncoding(encodingDict, !symbolic, builtIn);
        }
        else
        {
            this.encoding = readEncodingFromFont();
        }

        // normalise the standard 14 name, e.g "Symbol,Italic" -> "Symbol"
        String standard14Name = Standard14Fonts.getMappedFontName(getName());
        assignGlyphList(standard14Name);
    }

    /**
     * Called by readEncoding() if the encoding needs to be extracted from the font file.
     *
     * @throws IOException if the font file could not be read.
     */
    protected abstract Encoding readEncodingFromFont() throws IOException;

    /**
     * Returns the Encoding vector.
     */
    public Encoding getEncoding()
    {
        return encoding;
    }

    /**
     * Returns the Encoding vector.
     */
    public GlyphList getGlyphList()
    {
        return glyphList;
    }

    /**
     * Returns true the font is a symbolic (that is, it does not use the Adobe Standard Roman
     * character set).
     */
    public final boolean isSymbolic()
    {
        if (isSymbolic == null)
        {
            // unless we can prove that the font is non-symbolic, we assume that it is symbolic
            isSymbolic = requireNonNullElse(isFontSymbolic(), true);
        }
        return isSymbolic;
    }

    /**
     * Internal implementation of isSymbolic, allowing for the fact that the result may be
     * indeterminate.
     */
    protected Boolean isFontSymbolic()
    {
        Boolean result = getSymbolicFlag();
        if (result != null)
        {
            return result;
        }
        if (isStandard14())
        {
            String mappedName = Standard14Fonts.getMappedFontName(getName());
            return mappedName.equals("Symbol") || mappedName.equals("ZapfDingbats");
        }
        if (encoding == null)
        {
            // sanity check, should never happen
            if (!(this instanceof PDTrueTypeFont))
            {
                throw new IllegalStateException("Encoding should not be null!");
            }

            // TTF without its non-symbolic flag set must be symbolic
            return true;
        }
        if (encoding instanceof WinAnsiEncoding || encoding instanceof MacRomanEncoding
                || encoding instanceof StandardEncoding)
        {
            return false;
        }
        if (encoding instanceof DictionaryEncoding)
        {
            // each name in Differences array must also be in the latin character set
            for (String name : ((DictionaryEncoding) encoding).getDifferences().values())
            {
                if (".notdef".equals(name))
                {
                    // skip
                }
                else if (!(WinAnsiEncoding.INSTANCE.contains(name)
                        && MacRomanEncoding.INSTANCE.contains(name)
                        && StandardEncoding.INSTANCE.contains(name)))
                {
                    return true;
                }

            }
            return false;
        }
        // we don't know
        return null;
    }

    /**
     * @return the value of the symbolic flag, allowing for the fact that the result may be
     * indeterminate.
     */
    protected final Boolean getSymbolicFlag()
    {
        // fixme: isSymbolic() defaults to false if the flag is missing so we can't trust this
        return ofNullable(getFontDescriptor()).map(PDFontDescriptor::isSymbolic).orElse(null);
    }

    @Override
    public String toUnicode(int code) throws IOException
    {
        return toUnicode(code, GlyphList.getAdobeGlyphList());
    }

    @Override
    public String toUnicode(int code, GlyphList customGlyphList) throws IOException
    {
        // allow the glyph list to be overridden for the purpose of extracting Unicode
        // we only do this when the font's glyph list is the AGL, to avoid breaking Zapf Dingbats
        GlyphList unicodeGlyphList;
        if (this.glyphList == GlyphList.getAdobeGlyphList())
        {
            unicodeGlyphList = customGlyphList;
        }
        else
        {
            unicodeGlyphList = this.glyphList;
        }

        // first try to use a ToUnicode CMap
        String unicode = super.toUnicode(code);
        if (unicode != null)
        {
            return unicode;
        }

        // if the font is a "simple font" and uses MacRoman/MacExpert/WinAnsi[Encoding]
        // or has Differences with names from only Adobe Standard and/or Symbol, then:
        //
        // a) Map the character codes to names
        // b) Look up the name in the Adobe Glyph List to obtain the Unicode value

        String name = null;
        if (encoding != null)
        {
            name = encoding.getName(code);
            unicode = unicodeGlyphList.toUnicode(name);
            if (unicode != null)
            {
                return unicode;
            }
        }

        // if no value has been produced, there is no way to obtain Unicode for the character.
        if (LOG.isWarnEnabled() && !noUnicode.contains(code))
        {
            // we keep track of which warnings have been issued, so we don't log multiple times
            noUnicode.add(code);
            if (name != null)
            {
                LOG.warn("No Unicode mapping for " + name + " (" + code + ") in font " + getName());
            }
            else
            {
                LOG.warn("No Unicode mapping for character code " + code + " in font " + getName());
            }
        }

        return null;
    }

    @Override
    public boolean isVertical()
    {
        return false;
    }

    @Override
    protected final float getStandard14Width(int code)
    {
        if (getStandard14AFM() != null)
        {
            String nameInAFM = getEncoding().getName(code);

            // the Adobe AFMs don't include .notdef, but Acrobat uses 250, test with PDFBOX-2334
            if (".notdef".equals(nameInAFM))
            {
                return 250f;
            }

            if ("nbspace".equals(nameInAFM))
            {
                // PDFBOX-4944: nbspace is missing in AFM files,
                // but PDF specification tells "it shall be typographically the same as SPACE"
                nameInAFM = "space";
            }
            else if ("sfthyphen".equals(nameInAFM))
            {
                // PDFBOX-5115: sfthyphen is missing in AFM files,
                // but PDF specification tells "it shall be typographically the same as hyphen"
                nameInAFM = "hyphen";
            }

            return getStandard14AFM().getCharacterWidth(nameInAFM);
        }
        throw new IllegalStateException("No AFM");
    }

    @Override
    public boolean isStandard14()
    {
        // this logic is based on Acrobat's behaviour, see see PDFBOX-2372
        // the Encoding entry cannot have Differences if we want "standard 14" font handling
        if (getEncoding() instanceof DictionaryEncoding dictionary)
        {
            if (!dictionary.getDifferences().isEmpty())
            {
                // we also require that the differences are actually different, see PDFBOX-1900 with
                // the file from PDFBOX-2192 on Windows
                Encoding baseEncoding = dictionary.getBaseEncoding();
                if (isNull(baseEncoding))
                {
                    return false;
                }
                for (Map.Entry<Integer, String> entry : dictionary.getDifferences().entrySet())
                {
                    if (!entry.getValue().equals(baseEncoding.getName(entry.getKey())))
                    {
                        return false;
                    }
                }
            }
        }
        return super.isStandard14();
    }

    /**
     * Returns the path for the character with the given name. For some fonts, GIDs may be used
     * instead of names when calling this method.
     *
     * @return glyph path
     * @throws IOException if the path could not be read
     */
    public abstract GeneralPath getPath(String name) throws IOException;

    /**
     * Returns true if the font contains the character with the given name.
     *
     * @throws IOException if the path could not be read
     */
    public abstract boolean hasGlyph(String name) throws IOException;

    /**
     * Returns the embedded or system font used for rendering. This is never null.
     */
    public abstract FontBoxFont getFontBoxFont();

    @Override
    public void addToSubset(int codePoint)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void subset() throws IOException
    {
        // only TTF subsetting via PDType0Font is currently supported
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean willBeSubset()
    {
        return false;
    }

    @Override
    public float getWidth(int code) throws IOException
    {
        var width = codeToWidth.computeIfAbsent(code, c -> {
            // Acrobat overrides the widths in the font program on the conforming reader's system with
            // the widths specified in the font dictionary." (Adobe Supplement to the ISO 32000)
            //
            // Note: The Adobe Supplement says that the override happens "If the font program is not
            // embedded", however PDFBOX-427 shows that it also applies to embedded fonts.

            initWidths();

            // Type1, Type1C, Type3
            var explicitWith = getExplicitWidth(code);
            if (nonNull(explicitWith))
            {
                return explicitWith;
            }

            //we use MissingWidth of the descriptor only if present, we fallback to width from font otherwise
            var missingWidth = ofNullable(getFontDescriptor()).map(PDFontDescriptor::getCOSObject)
                    .map(d -> d.getDictionaryObject(COSName.MISSING_WIDTH, COSNumber.class))
                    .orElse(null);
            if (nonNull(missingWidth))
            {
                return missingWidth.floatValue();
            }

            // standard 14 font widths are specified by an AFM
            if (isStandard14())
            {
                return getStandard14Width(code);
            }
            return null;
        });

        if (isNull(width))
        {
            // if there's nothing to override with, then obviously we fall back to the font
            width = getWidthFromFont(code);
            codeToWidth.put(code, width);
        }
        return width;
    }

    @Override
    public Float getExplicitWidth(int code)
    {
        initWidths();
        if (!widths.isEmpty())
        {
            int firstChar = getCOSObject().getInt(COSName.FIRST_CHAR, 0);
            int index = code - firstChar;
            if (code >= firstChar && index < widths.size())
            {
                return widths.get(index);
            }
        }
        return null;
    }

    @Override
    public boolean hasExplicitWidth(int code)
    {
        return nonNull(getExplicitWidth(code));
    }

    /**
     * initialize the internal widths property with The widths of the characters. This will be null
     * for the standard 14 fonts.
     */
    private void initWidths()
    {
        if (isNull(widths))
        {
            this.widths = ofNullable(
                    getCOSObject().getDictionaryObject(COSName.WIDTHS, COSArray.class)).map(
                    COSArrayList::convertFloatCOSArrayToList).orElseGet(Collections::emptyList);
        }
    }

    private void assignGlyphList(String baseFont)
    {
        // assign the glyph list based on the font
        if ("ZapfDingbats".equals(baseFont))
        {
            glyphList = GlyphList.getZapfDingbats();
        }
        else
        {
            glyphList = GlyphList.getAdobeGlyphList();
        }
    }
}
