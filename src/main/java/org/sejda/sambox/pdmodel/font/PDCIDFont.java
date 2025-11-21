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

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.sejda.commons.util.IOUtils;
import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSNumber;
import org.sejda.sambox.cos.COSObjectable;
import org.sejda.sambox.cos.COSStream;
import org.sejda.sambox.pdmodel.common.PDDictionaryWrapper;
import org.sejda.sambox.util.Vector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A CIDFont. A CIDFont is a PDF object that contains information about a CIDFont program. Although
 * its Type value is Font, a CIDFont is not actually a font.
 *
 * <p>
 * It is not usually necessary to use this class directly, prefer {@link PDType0Font}.
 *
 * @author Ben Litchfield
 */
public abstract class PDCIDFont extends PDDictionaryWrapper
        implements COSObjectable, PDFontLike, PDVectorFont
{
    private static final Logger LOG = LoggerFactory.getLogger(PDCIDFont.class);

    protected final PDType0Font parent;

    private Map<Integer, Float> widths;
    private float defaultWidth = -1F;
    private float averageWidth;

    private final Map<Integer, Float> verticalDisplacementY = new HashMap<>(); // w1y
    private final Map<Integer, Vector> positionVectors = new HashMap<>(); // v
    private float[] dw2 = new float[] { 880, -1000 };

    /**
     * Constructor.
     *
     * @param fontDictionary The font dictionary according to the PDF specification.
     */
    PDCIDFont(COSDictionary fontDictionary, PDType0Font parent)
    {
        super(fontDictionary);
        this.parent = parent;
        readWidths();
        readVerticalDisplacements();
    }

    private void readWidths()
    {
        // see 9.7.4.3, "Glyph Metrics in CIDFonts"
        widths = new HashMap<>();
        COSArray wArray = getCOSObject().getDictionaryObject(COSName.W, COSArray.class);
        if (nonNull(wArray))
        {
            int size = wArray.size();
            int counter = 0;
            while (counter < size - 1)
            {
                COSBase firstCodeBase = wArray.getObject(counter++);
                if (!(firstCodeBase instanceof COSNumber firstCode))
                {
                    LOG.warn("Expected a number array member, got {}", firstCodeBase);
                    continue;
                }
                COSBase next = wArray.getObject(counter++);
                if (next instanceof COSArray array)
                {
                    int startRange = firstCode.intValue();
                    int arraySize = array.size();
                    for (int i = 0; i < arraySize; i++)
                    {
                        COSNumber width = array.getObject(i, COSNumber.class);
                        if (nonNull(width))
                        {
                            widths.put(startRange + i, width.floatValue());
                        }
                    }
                }
                else
                {
                    if (counter >= size)
                    {
                        LOG.warn("premature end of widths array");
                        break;
                    }
                    COSBase rangeWidthBase = wArray.getObject(counter++);
                    if (!(next instanceof COSNumber secondCode)
                            || !(rangeWidthBase instanceof COSNumber rangeWidth))
                    {
                        LOG.warn("Expected two numbers, got {} and {}", next, rangeWidthBase);
                        continue;
                    }
                    int startRange = firstCode.intValue();
                    int endRange = secondCode.intValue();
                    float width = rangeWidth.floatValue();
                    for (int i = startRange; i <= endRange; i++)
                    {
                        widths.put(i, width);
                    }
                }
            }
        }

    }

    private void readVerticalDisplacements()
    {
        // default position vector and vertical displacement vector
        COSArray dw2Array = getCOSObject().getDictionaryObject(COSName.DW2, COSArray.class);
        if (nonNull(dw2Array))
        {
            dw2 = new float[2];
            COSBase base0 = dw2Array.getObject(0);
            COSBase base1 = dw2Array.getObject(1);
            if (base0 instanceof COSNumber && base1 instanceof COSNumber)
            {
                dw2[0] = ((COSNumber) base0).floatValue();
                dw2[1] = ((COSNumber) base1).floatValue();
            }
        }

        // vertical metrics for individual CIDs.
        COSArray w2Array = getCOSObject().getDictionaryObject(COSName.W2, COSArray.class);
        if (nonNull(w2Array))
        {
            for (int i = 0; i < w2Array.size(); i++)
            {
                COSNumber c = (COSNumber) w2Array.getObject(i);
                COSBase next = w2Array.getObject(++i);
                if (next instanceof COSArray array)
                {
                    for (int j = 0; j < array.size(); j++)
                    {
                        int cid = c.intValue() + j / 3;
                        COSNumber w1y = (COSNumber) array.getObject(j);
                        COSNumber v1x = (COSNumber) array.getObject(++j);
                        COSNumber v1y = (COSNumber) array.getObject(++j);
                        verticalDisplacementY.put(cid, w1y.floatValue());
                        positionVectors.put(cid, new Vector(v1x.floatValue(), v1y.floatValue()));
                    }
                }
                else
                {
                    int first = c.intValue();
                    int last = ((COSNumber) next).intValue();
                    COSNumber w1y = (COSNumber) w2Array.getObject(++i);
                    COSNumber v1x = (COSNumber) w2Array.getObject(++i);
                    COSNumber v1y = (COSNumber) w2Array.getObject(++i);
                    for (int cid = first; cid <= last; cid++)
                    {
                        verticalDisplacementY.put(cid, w1y.floatValue());
                        positionVectors.put(cid, new Vector(v1x.floatValue(), v1y.floatValue()));
                    }
                }
            }
        }
    }

    /**
     * The PostScript name of the font.
     *
     * @return The postscript name of the font.
     */
    public String getBaseFont()
    {
        return getCOSObject().getNameAsString(COSName.BASE_FONT);
    }

    @Override
    public String getName()
    {
        return getBaseFont();
    }

    @Override
    public PDFontDescriptor getFontDescriptor()
    {
        return ofNullable(
                getCOSObject().getDictionaryObject(COSName.FONT_DESC, COSDictionary.class)).map(
                PDFontDescriptor::new).orElse(null);
    }

    /**
     * @return parent Type 0 font
     */
    public final PDType0Font getParent()
    {
        return parent;
    }

    /**
     * @return The default width for the glyphs in this font or 1000 if nothing is set.
     */
    public float getDefaultWidth()
    {
        if (defaultWidth == -1)
        {
            defaultWidth = ofNullable(
                    getCOSObject().getDictionaryObject(COSName.DW, COSNumber.class)).map(
                    COSNumber::floatValue).orElse(1000F);
        }
        return defaultWidth;
    }

    /**
     * @param cid CID
     * @return the default position vector (v).
     */
    private Vector getDefaultPositionVector(int cid)
    {
        return new Vector(getWidthForCID(cid) / 2, dw2[0]);
    }

    private float getWidthForCID(int cid)
    {
        return ofNullable(widths.get(cid)).orElseGet(this::getDefaultWidth);
    }

    @Override
    public Float getExplicitWidth(int code)
    {
        return widths.get(codeToCID(code));
    }

    @Override
    public boolean hasExplicitWidth(int code)
    {
        return nonNull(getExplicitWidth(code));
    }

    @Override
    public Vector getPositionVector(int code)
    {
        int cid = codeToCID(code);
        Vector v = positionVectors.get(cid);
        if (v == null)
        {
            v = getDefaultPositionVector(cid);
        }
        return v;
    }

    /**
     * Returns the y-component of the vertical displacement vector (w1).
     *
     * @param code character code
     * @return w1y
     */
    public float getVerticalDisplacementVectorY(int code)
    {
        int cid = codeToCID(code);
        Float w1y = verticalDisplacementY.get(cid);
        if (w1y == null)
        {
            w1y = dw2[1];
        }
        return w1y;
    }

    @Override
    public float getWidth(int code) throws IOException
    {
        // these widths are supposed to be consistent with the actual widths given in the CIDFont
        // program, but PDFBOX-563 shows that when they are not, Acrobat overrides the embedded
        // font widths with the widths given in the font dictionary
        return getWidthForCID(codeToCID(code));
    }

    @Override
    // todo: this method is highly suspicious, the average glyph width is not usually a good metric
    public float getAverageFontWidth()
    {
        if (averageWidth == 0)
        {
            float totalWidths = 0.0f;
            int characterCount = 0;
            if (widths != null)
            {
                for (Float width : widths.values())
                {
                    if (width > 0)
                    {
                        totalWidths += width;
                        ++characterCount;
                    }
                }
            }
            if (characterCount != 0)
            {
                averageWidth = totalWidths / characterCount;
            }
            if (averageWidth <= 0 || Float.isNaN(averageWidth))
            {
                averageWidth = getDefaultWidth();
            }
        }
        return averageWidth;
    }

    /**
     * @return the CIDSystemInfo, or null if it is missing (which isn't allowed but could happen).
     */
    public PDCIDSystemInfo getCIDSystemInfo()
    {
        return ofNullable(
                getCOSObject().getDictionaryObject(COSName.CIDSYSTEMINFO, COSDictionary.class)).map(
                PDCIDSystemInfo::new).orElse(null);
    }

    /**
     * Returns the CID for the given character code. If not found then CID 0 is returned.
     *
     * @param code character code
     * @return CID
     */
    public abstract int codeToCID(int code);

    /**
     * Returns the GID for the given character code.
     *
     * @param code character code
     * @return GID
     */
    public abstract int codeToGID(int code) throws IOException;

    /**
     * Encodes the given Unicode code point for use in a PDF content stream. Content streams use a
     * multi-byte encoding with 1 to 4 bytes.
     *
     * <p>
     * This method is called when embedding text in PDFs and when filling in fields.
     *
     * @param unicode Unicode code point.
     * @return Array of 1 to 4 PDF content stream bytes.
     * @throws IOException If the text could not be encoded.
     */
    protected abstract byte[] encode(int unicode) throws IOException;

    final int[] readCIDToGIDMap() throws IOException
    {
        int[] cid2gid = null;
        COSBase map = getCOSObject().getDictionaryObject(COSName.CID_TO_GID_MAP);
        if (map instanceof COSStream stream)
        {

            InputStream is = stream.getUnfilteredStream();
            byte[] mapAsBytes = IOUtils.toByteArray(is);
            IOUtils.closeQuietly(is);
            int numberOfInts = mapAsBytes.length / 2;
            cid2gid = new int[numberOfInts];
            int offset = 0;
            for (int index = 0; index < numberOfInts; index++)
            {
                int gid = (mapAsBytes[offset] & 0xff) << 8 | mapAsBytes[offset + 1] & 0xff;
                cid2gid[index] = gid;
                offset += 2;
            }
        }
        return cid2gid;
    }
}
