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
import static org.sejda.commons.util.RequireUtils.requireIOCondition;
import static org.sejda.commons.util.RequireUtils.requireState;

import java.awt.geom.GeneralPath;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.fontbox.cmap.CMap;
import org.apache.fontbox.ttf.CmapLookup;
import org.apache.fontbox.ttf.TTFParser;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.apache.fontbox.util.BoundingBox;
import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.pdmodel.PDDocument;
import org.sejda.sambox.util.Matrix;
import org.sejda.sambox.util.Vector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Composite (Type 0) font.
 *
 * @author Ben Litchfield
 */
public class PDType0Font extends PDFont implements PDVectorFont
{
    private static final Logger LOG = LoggerFactory.getLogger(PDType0Font.class);

    private final PDCIDFont descendantFont;
    private CMap cMap, cMapUCS2;
    private boolean isCMapPredefined;
    private boolean isDescendantCJK;
    private PDCIDFontType2Embedder embedder;
    private final Set<Integer> noUnicode = new HashSet<>();
    private TrueTypeFont ttf;

    /**
     * Loads a TTF to be embedded and subset into a document as a Type 0 font. If you are loading a
     * font for AcroForm, then use the 3-parameter constructor instead.
     *
     * @param doc  The PDF document that will hold the embedded font.
     * @param file A TrueType font.
     * @return A Type0 font with a CIDFontType2 descendant.
     * @throws IOException If there is an error reading the font file.
     */
    public static PDType0Font load(PDDocument doc, File file) throws IOException
    {
        TrueTypeFont ttf = new TTFParser().parse(file);
        boolean embedSubset = FontUtils.isSubsettingPermitted(ttf);
        return new PDType0Font(doc, ttf, embedSubset, true, false);
    }

    /**
     * Loads a TTF to be embedded and subset into a document as a Type 0 font. If you are loading a
     * font for AcroForm, then use the 3-parameter constructor instead.
     *
     * @param doc   The PDF document that will hold the embedded font.
     * @param input An input stream of a TrueType font. It will be closed before returning.
     * @return A Type0 font with a CIDFontType2 descendant.
     * @throws IOException If there is an error reading the font stream.
     */
    public static PDType0Font load(PDDocument doc, InputStream input) throws IOException
    {
        TrueTypeFont ttf = new TTFParser().parse(input);
        boolean embedSubset = FontUtils.isSubsettingPermitted(ttf);
        return new PDType0Font(doc, ttf, embedSubset, true, false);
    }

    /**
     * Loads a TTF to be embedded into a document as a Type 0 font.
     *
     * @param doc         The PDF document that will hold the embedded font.
     * @param input       A TrueType font.
     * @param embedSubset True if the font will be subset before embedding
     * @return A Type0 font with a CIDFontType2 descendant.
     * @throws IOException If there is an error reading the font stream.
     */
    public static PDType0Font load(PDDocument doc, InputStream input, boolean embedSubset)
            throws IOException
    {
        return new PDType0Font(doc, new TTFParser().parse(input), embedSubset, true, false);
    }

    /**
     * Loads a TTF to be embedded into a document as a Type 0 font.
     *
     * @param doc         The PDF document that will hold the embedded font.
     * @param ttf         A TrueType font.
     * @param embedSubset True if the font will be subset before embedding
     * @return A Type0 font with a CIDFontType2 descendant.
     * @throws IOException If there is an error reading the font stream.
     */
    public static PDType0Font load(PDDocument doc, TrueTypeFont ttf, boolean embedSubset)
            throws IOException
    {
        return new PDType0Font(doc, ttf, embedSubset, false, false);
    }

    /**
     * Loads a TTF to be embedded into a document as a vertical Type 0 font.
     *
     * @param doc  The PDF document that will hold the embedded font.
     * @param file A TrueType font.
     * @return A Type0 font with a CIDFontType2 descendant.
     * @throws IOException If there is an error reading the font file.
     */
    public static PDType0Font loadVertical(PDDocument doc, File file) throws IOException
    {
        return new PDType0Font(doc, new TTFParser().parse(file), true, true, true);
    }

    /**
     * Loads a TTF to be embedded into a document as a vertical Type 0 font.
     *
     * @param doc   The PDF document that will hold the embedded font.
     * @param input A TrueType font.
     * @return A Type0 font with a CIDFontType2 descendant.
     * @throws IOException If there is an error reading the font stream.
     */
    public static PDType0Font loadVertical(PDDocument doc, InputStream input) throws IOException
    {
        return new PDType0Font(doc, new TTFParser().parse(input), true, true, true);
    }

    /**
     * Loads a TTF to be embedded into a document as a vertical Type 0 font.
     *
     * @param doc         The PDF document that will hold the embedded font.
     * @param input       A TrueType font.
     * @param embedSubset True if the font will be subset before embedding
     * @return A Type0 font with a CIDFontType2 descendant.
     * @throws IOException If there is an error reading the font stream.
     */
    public static PDType0Font loadVertical(PDDocument doc, InputStream input, boolean embedSubset)
            throws IOException
    {
        return new PDType0Font(doc, new TTFParser().parse(input), embedSubset, true, true);
    }

    /**
     * Loads a TTF to be embedded into a document as a vertical Type 0 font.
     *
     * @param doc         The PDF document that will hold the embedded font.
     * @param ttf         A TrueType font.
     * @param embedSubset True if the font will be subset before embedding
     * @return A Type0 font with a CIDFontType2 descendant.
     * @throws IOException If there is an error reading the font stream.
     */
    public static PDType0Font loadVertical(PDDocument doc, TrueTypeFont ttf, boolean embedSubset)
            throws IOException
    {
        return new PDType0Font(doc, ttf, embedSubset, false, true);
    }

    /**
     * Constructor for reading a Type0 font from a PDF file.
     *
     * @param fontDictionary The font dictionary according to the PDF specification.
     */
    public PDType0Font(COSDictionary fontDictionary) throws IOException
    {
        super(fontDictionary);
        var descendantFonts = getCOSObject().getDictionaryObject(COSName.DESCENDANT_FONTS,
                COSArray.class);
        requireIOCondition(nonNull(descendantFonts), "Missing descendant font array");
        requireIOCondition(!descendantFonts.isEmpty(), "Descendant font array is empty");

        var descendantFontDict = descendantFonts.getObject(0, COSDictionary.class);
        requireIOCondition(nonNull(descendantFontDict), "Missing descendant font dictionary");
        requireIOCondition(COSName.FONT.equals(
                        descendantFontDict.getDictionaryObject(COSName.TYPE, COSName.FONT, COSName.class)),
                "Missing or wrong type in descendant font dictionary");

        descendantFont = PDFontFactory.createDescendantFont(descendantFontDict, this);
        readEncoding();
        fetchCMapUCS2();
    }

    /**
     * @param document
     * @param ttf
     * @param embedSubset
     * @param closeTTF    whether to close the ttf parameter after embedding. Must be true when the
     *                    ttf parameter was created in the load() method, false when the ttf
     *                    parameter was passed to the load() method.
     * @param vertical    whether to enable vertical substitutions.
     * @throws IOException
     */
    private PDType0Font(PDDocument document, TrueTypeFont ttf, boolean embedSubset,
            boolean closeTTF, boolean vertical) throws IOException
    {
        if (vertical)
        {
            ttf.enableVerticalSubstitutions();
        }
        embedder = new PDCIDFontType2Embedder(document, getCOSObject(), ttf, embedSubset, this,
                vertical);
        descendantFont = embedder.getCIDFont();
        readEncoding();
        fetchCMapUCS2();
        if (closeTTF)
        {
            if (embedSubset)
            {
                this.ttf = ttf;
                document.registerTrueTypeFontForClosing(ttf);
            }
            else
            {
                // the TTF is fully loaded and it is safe to close the underlying data source
                ttf.close();
            }
        }
    }

    @Override
    public void addToSubset(int codePoint)
    {
        requireState(willBeSubset(), "This font was created with subsetting disabled");
        embedder.addToSubset(codePoint);
    }

    @Override
    public void subset() throws IOException
    {
        requireState(willBeSubset(), "This font was created with subsetting disabled");
        embedder.subset();
        if (nonNull(ttf))
        {
            ttf.close();
            ttf = null;
        }
    }

    @Override
    public boolean willBeSubset()
    {
        return embedder != null && embedder.needsSubset();
    }

    /**
     * Reads the font's Encoding entry, which should be a CMap name/stream.
     */
    private void readEncoding() throws IOException
    {
        COSBase encoding = getCOSObject().getDictionaryObject(COSName.ENCODING);
        if (encoding instanceof COSName encodingName)
        {
            // predefined CMap
            cMap = CMapManager.getPredefinedCMap(encodingName.getName());
            isCMapPredefined = true;
        }
        else if (encoding != null)
        {
            cMap = readCMap(encoding);
            requireIOCondition(nonNull(cMap), "Missing required CMap");
            if (!cMap.hasCIDMappings())
            {
                LOG.warn("Invalid Encoding CMap in font " + getName());
            }
        }

        // check if the descendant font is CJK
        PDCIDSystemInfo ros = descendantFont.getCIDSystemInfo();
        if (ros != null)
        {
            String ordering = ros.getOrdering();
            isDescendantCJK =
                    "Adobe".equals(ros.getRegistry()) && ("GB1".equals(ordering) || "CNS1".equals(
                            ordering) || "Japan1".equals(ordering) || "Korea1".equals(ordering));
        }
    }

    /**
     * Fetches the corresponding UCS2 CMap if the font's CMap is predefined.
     */
    private void fetchCMapUCS2() throws IOException
    {
        // if the font is composite and uses a predefined cmap (excluding Identity-H/V)
        // or whose descendant CIDFont uses the Adobe-GB1, Adobe-CNS1, Adobe-Japan1, or
        // Adobe-Korea1 character collection:
        COSName name = getCOSObject().getCOSName(COSName.ENCODING);
        if (isCMapPredefined && !(name == COSName.IDENTITY_H || name == COSName.IDENTITY_V)
                || isDescendantCJK)
        {
            // a) Map the character code to a CID using the font's CMap
            // b) Obtain the ROS from the font's CIDSystemInfo
            // c) Construct a second CMap name by concatenating the ROS in the format "R-O-UCS2"
            // d) Obtain the CMap with the constructed name
            // e) Map the CID according to the CMap from step d), producing a Unicode value

            // todo: not sure how to interpret the PDF spec here, do we always override? or only when Identity-H/V?
            String strName = null;
            if (isDescendantCJK)
            {
                PDCIDSystemInfo cidSystemInfo = descendantFont.getCIDSystemInfo();
                if (cidSystemInfo != null)
                {
                    strName = cidSystemInfo.getRegistry() + "-" + cidSystemInfo.getOrdering() + "-"
                            + cidSystemInfo.getSupplement();
                }
            }
            else if (name != null)
            {
                strName = name.getName();
            }

            // try to find the corresponding Unicode (UC2) CMap
            if (strName != null)
            {
                try
                {
                    CMap prdCMap = CMapManager.getPredefinedCMap(strName);
                    String ucs2Name = prdCMap.getRegistry() + "-" + prdCMap.getOrdering() + "-UCS2";
                    cMapUCS2 = CMapManager.getPredefinedCMap(ucs2Name);
                }
                catch (IOException ex)
                {
                    LOG.warn("Could not get {} UC2 map for font {}", strName, getName(), ex);
                }
            }
        }
    }

    /**
     * Returns the PostScript name of the font.
     */
    public String getBaseFont()
    {
        return getCOSObject().getNameAsString(COSName.BASE_FONT);
    }

    /**
     * Returns the descendant font.
     */
    public PDCIDFont getDescendantFont()
    {
        return descendantFont;
    }

    /**
     * Returns the font's CMap.
     */
    public CMap getCMap()
    {
        return cMap;
    }

    /**
     * Returns the font's UCS2 CMap, only present this font uses a predefined CMap.
     */
    public CMap getCMapUCS2()
    {
        return cMapUCS2;
    }

    @Override
    public PDFontDescriptor getFontDescriptor()
    {
        return descendantFont.getFontDescriptor();
    }

    @Override
    public Matrix getFontMatrix()
    {
        return descendantFont.getFontMatrix();
    }

    @Override
    public boolean isVertical()
    {
        return cMap != null && cMap.getWMode() == 1;
    }

    @Override
    public float getHeight(int code) throws IOException
    {
        return descendantFont.getHeight(code);
    }

    @Override
    protected byte[] encode(int unicode) throws IOException
    {
        return descendantFont.encode(unicode);
    }

    @Override
    public boolean hasExplicitWidth(int code)
    {
        return descendantFont.hasExplicitWidth(code);
    }

    @Override
    public float getAverageFontWidth()
    {
        return descendantFont.getAverageFontWidth();
    }

    @Override
    public Vector getPositionVector(int code)
    {
        // units are always 1/1000 text space, font matrix is not used, see FOP-2252
        return descendantFont.getPositionVector(code).scale(-1 / 1000f);
    }

    @Override
    public Vector getDisplacement(int code) throws IOException
    {
        if (isVertical())
        {
            return new Vector(0, descendantFont.getVerticalDisplacementVectorY(code) / 1000f);
        }
        return super.getDisplacement(code);
    }

    @Override
    public float getWidth(int code) throws IOException
    {
        return descendantFont.getWidth(code);
    }

    @Override
    protected float getStandard14Width(int code)
    {
        throw new UnsupportedOperationException("not supported");
    }

    @Override
    public float getWidthFromFont(int code) throws IOException
    {
        return descendantFont.getWidthFromFont(code);
    }

    @Override
    public boolean isEmbedded()
    {
        return descendantFont.isEmbedded();
    }

    @Override
    public String toUnicode(int code) throws IOException
    {
        // try to use a ToUnicode CMap
        String unicode = super.toUnicode(code);
        if (unicode != null)
        {
            return unicode;
        }

        if ((isCMapPredefined || isDescendantCJK) && cMapUCS2 != null)
        {
            // if the font is composite and uses a predefined cmap (excluding Identity-H/V) then
            // or if its descendant font uses Adobe-GB1/CNS1/Japan1/Korea1

            // a) Map the character code to a character identifier (CID) according to the font?s CMap
            int cid = codeToCID(code);

            // e) Map the CID according to the CMap from step d), producing a Unicode value
            return cMapUCS2.toUnicode(cid);
        }

        // PDFBOX-5324: try to get unicode from font cmap
        if (descendantFont instanceof PDCIDFontType2)
        {
            TrueTypeFont font = ((PDCIDFontType2) descendantFont).getTrueTypeFont();
            if (font != null)
            {
                try
                {
                    CmapLookup cmap = font.getUnicodeCmapLookup(false);
                    if (cmap != null)
                    {
                        int gid;
                        if (descendantFont.isEmbedded())
                        {
                            // original PDFBOX-5324 supported only embedded fonts
                            gid = descendantFont.codeToGID(code);
                        }
                        else
                        {
                            // PDFBOX-5331: this bypasses the fallback attempt in
                            // PDCIDFontType2.codeToGID() which would bring a stackoverflow
                            gid = descendantFont.codeToCID(code);
                        }
                        List<Integer> codes = cmap.getCharCodes(gid);
                        if (codes != null && !codes.isEmpty())
                        {
                            return Character.toString((char) (int) codes.get(0));
                        }
                    }
                }
                catch (IOException | NullPointerException e)
                {
                    LOG.warn("get unicode from font cmap fail", e);
                }
            }
        }

        if (LOG.isWarnEnabled() && !noUnicode.contains(code))
        {
            // if no value has been produced, there is no way to obtain Unicode for the character.
            String cid = "CID+" + codeToCID(code);
            LOG.warn("No Unicode mapping for " + cid + " (" + code + ") in font " + getName());
            // we keep track of which warnings have been issued, so we don't log multiple times
            noUnicode.add(code);
        }
        return null;
    }

    @Override
    public String getName()
    {
        return getBaseFont();
    }

    @Override
    public BoundingBox getBoundingBox() throws IOException
    {
        // Will be cached by underlying font
        return descendantFont.getBoundingBox();
    }

    @Override
    public int readCode(InputStream in) throws IOException
    {
        requireIOCondition(nonNull(cMap), "required cmap is null");
        return cMap.readCode(in);
    }

    /**
     * Returns the CID for the given character code. If not found then CID 0 is returned.
     *
     * @param code character code
     * @return CID
     */
    public int codeToCID(int code)
    {
        return descendantFont.codeToCID(code);
    }

    /**
     * Returns the GID for the given character code.
     *
     * @param code character code
     * @return GID
     */
    public int codeToGID(int code) throws IOException
    {
        return descendantFont.codeToGID(code);
    }

    @Override
    public boolean isStandard14()
    {
        return false;
    }

    @Override
    public boolean isDamaged()
    {
        return descendantFont.isDamaged();
    }

    @Override
    public boolean isOriginalEmbeddedMissing()
    {
        return descendantFont.isOriginalEmbeddedMissing();
    }

    @Override
    public boolean isMappingFallbackUsed()
    {
        return descendantFont.isMappingFallbackUsed();
    }

    @Override
    public String toString()
    {
        String descendant = null;
        if (getDescendantFont() != null)
        {
            descendant = getDescendantFont().getClass().getSimpleName();
        }
        return getClass().getSimpleName() + "/" + descendant + ", PostScript name: "
                + getBaseFont();
    }

    @Override
    public GeneralPath getPath(int code) throws IOException
    {
        return descendantFont.getPath(code);
    }

    @Override
    public boolean hasGlyph(int code) throws IOException
    {
        return descendantFont.hasGlyph(code);
    }

}
