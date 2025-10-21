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
package org.sejda.sambox.pdmodel.graphics.color;

import java.io.IOException;
import java.util.Arrays;

import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A color value, consisting of one or more color components, or for pattern color spaces, a name
 * and optional color components. Color values are not associated with any given color space.
 * <p>
 * Instances of PDColor are immutable.
 *
 * @author John Hewson
 */
public final class PDColor
{
    private static final Logger LOG = LoggerFactory.getLogger(PDColor.class);

    private float[] components;
    private final COSName patternName;
    private final PDColorSpace colorSpace;

    /**
     * Creates a PDColor containing the given color value.
     *
     * @param array      a COS array containing the color value
     * @param colorSpace color space in which the color value is defined
     */
    public PDColor(COSArray array, PDColorSpace colorSpace)
    {
        if (!array.isEmpty() && array.getLast() instanceof COSName)
        {
            // color components (optional)
            components = new float[array.size() - 1];
            initComponents(array);

            // pattern name (required)
            COSBase base = array.get(array.size() - 1);
            if (base instanceof COSName)
            {
                patternName = (COSName) base;
            }
            else
            {
                LOG.warn("pattern name in " + array + " isn't a name, ignored");
                patternName = COSName.getPDFName("Unknown");
            }
        }
        else
        {
            // color components only
            components = new float[array.size()];
            initComponents(array);
            patternName = null;
        }
        this.colorSpace = colorSpace;
    }

    private void initComponents(COSArray array)
    {
        for (int i = 0; i < components.length; i++)
        {
            COSBase base = array.get(i);
            if (base instanceof COSNumber)
            {
                components[i] = ((COSNumber) base).floatValue();
            }
            else
            {
                LOG.warn("color component " + i + " in " + array + " isn't a number, ignored");
                components[i] = 0f;
            }
        }
    }

    /**
     * Creates a PDColor containing the given color component values.
     *
     * @param components array of color component values
     * @param colorSpace color space in which the components are defined
     */
    public PDColor(float[] components, PDColorSpace colorSpace)
    {
        this.components = components.clone();
        this.patternName = null;
        this.colorSpace = colorSpace;
    }

    /**
     * Creates a PDColor containing the given pattern name.
     *
     * @param patternName the name of a pattern in a pattern dictionary
     * @param colorSpace  color space in which the pattern is defined
     */
    public PDColor(COSName patternName, PDColorSpace colorSpace)
    {
        this.components = new float[0];
        this.patternName = patternName;
        this.colorSpace = colorSpace;
    }

    /**
     * Creates a PDColor containing the given color component values and pattern name.
     *
     * @param components  array of color component values
     * @param patternName the name of a pattern in a pattern dictionary
     * @param colorSpace  color space in which the pattern/components are defined
     */
    public PDColor(float[] components, COSName patternName, PDColorSpace colorSpace)
    {
        this.components = components.clone();
        this.patternName = patternName;
        this.colorSpace = colorSpace;
    }

    /**
     * Returns the components of this color value.
     *
     * @return the components of this color value
     */
    public float[] getComponents()
    {
        if (colorSpace instanceof PDPattern || colorSpace == null)
        {
            // colorspace of the pattern color isn't known, so just clone
            // null colorspace can happen with empty annotation color
            // see PDFBOX-3351-538928-p4.pdf
            return components.clone();
        }
        // PDFBOX-4279: copyOf instead of clone in case array is too small
        return Arrays.copyOf(components, colorSpace.getNumberOfComponents());
    }

    /**
     * Returns the pattern name from this color value.
     *
     * @return the pattern name from this color value
     */
    public COSName getPatternName()
    {
        return patternName;
    }

    /**
     * Returns true if this color value is a pattern.
     *
     * @return true if this color value is a pattern
     */
    public boolean isPattern()
    {
        return patternName != null;
    }

    /**
     * Returns the packed RGB value for this color, if any.
     *
     * @return RGB
     * @throws IOException           if the color conversion fails
     * @throws IllegalStateException if this color value is a pattern.
     */
    public int toRGB() throws IOException
    {
        float[] floats = colorSpace.toRGB(components);
        int r = Math.round(floats[0] * 255);
        int g = Math.round(floats[1] * 255);
        int b = Math.round(floats[2] * 255);
        int rgb = r;
        rgb = (rgb << 8) + g;
        rgb = (rgb << 8) + b;
        return rgb;
    }

    /**
     * Returns this color value as a COS array
     *
     * @return the color value as a COS array
     */
    public COSArray toCOSArray()
    {
        var array = COSArray.fromFloats(components);
        if (patternName != null)
        {
            array.add(patternName);
        }
        return array;
    }

    /**
     * @return the color value as a COSarray containing onlye the components values, no pattern
     * name.
     */
    public COSArray toComponentsCOSArray()
    {
        return COSArray.fromFloats(components);
    }

    /**
     * Returns the color space in which this color value is defined.
     */
    public PDColorSpace getColorSpace()
    {
        return colorSpace;
    }

    @Override
    public String toString()
    {
        return "PDColor{components=" + Arrays.toString(components) + ", patternName=" + patternName
                + "}";
    }
}
