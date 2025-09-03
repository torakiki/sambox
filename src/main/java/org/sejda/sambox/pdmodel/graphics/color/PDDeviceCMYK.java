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

import static org.sejda.commons.util.RequireUtils.requireNotNullArg;

import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.sejda.sambox.cos.COSName;

/**
 * Allows colors to be specified according to the subtractive CMYK (cyan, magenta, yellow, black)
 * model typical of printers and other paper-based output devices.
 *
 * @author John Hewson
 * @author Ben Litchfield
 */
public class PDDeviceCMYK extends PDDeviceColorSpace
{
    public static PDDeviceCMYK INSTANCE = new PDDeviceCMYK();

    private final PDColor initialColor = new PDColor(new float[] { 0, 0, 0, 1 }, this);

    private PDDeviceCMYK()
    {
    }

    @Override
    public String getName()
    {
        return COSName.DEVICECMYK.getName();
    }

    @Override
    public int getNumberOfComponents()
    {
        return 4;
    }

    @Override
    public float[] getDefaultDecode(int bitsPerComponent)
    {
        return new float[] { 0, 1, 0, 1, 0, 1, 0, 1 };
    }

    @Override
    public PDColor getInitialColor()
    {
        return initialColor;
    }

    @Override
    public float[] toRGB(float[] value)
    {
        return ConversionContextHolder.CONVERSION_CONTEXT.colorSpace.toRGB(value);
    }

    @Override
    public BufferedImage toRawImage(WritableRaster raster)
    {
        // Device CMYK is not specified, as its the colors of whatever device you use.
        // The user should fallback to the RGB image
        return null;
    }

    @Override
    public BufferedImage toRGBImage(WritableRaster raster)
    {
        return toRGBImageAWT(raster, ConversionContextHolder.CONVERSION_CONTEXT.colorSpace);
    }

    @Override
    protected BufferedImage toRGBImageAWT(WritableRaster raster, ColorSpace colorSpace)
    {
        if (Boolean.getBoolean("org.sejda.sambox.rendering.UsePureJavaCMYKConversion"))
        {
            BufferedImage dest = new BufferedImage(raster.getWidth(), raster.getHeight(),
                    BufferedImage.TYPE_INT_RGB);
            ColorSpace destCS = dest.getColorModel().getColorSpace();
            WritableRaster destRaster = dest.getRaster();
            float[] srcValues = new float[4];
            float[] lastValues = new float[] { -1.0f, -1.0f, -1.0f, -1.0f };
            float[] destValues = new float[3];
            int startX = raster.getMinX();
            int startY = raster.getMinY();
            int endX = raster.getWidth() + startX;
            int endY = raster.getHeight() + startY;
            for (int x = startX; x < endX; x++)
            {
                for (int y = startY; y < endY; y++)
                {
                    raster.getPixel(x, y, srcValues);
                    // check if the last value can be reused
                    if (!Arrays.equals(lastValues, srcValues))
                    {
                        lastValues[0] = srcValues[0];
                        srcValues[0] = srcValues[0] / 255f;

                        lastValues[1] = srcValues[1];
                        srcValues[1] = srcValues[1] / 255f;

                        lastValues[2] = srcValues[2];
                        srcValues[2] = srcValues[2] / 255f;

                        lastValues[3] = srcValues[3];
                        srcValues[3] = srcValues[3] / 255f;

                        // use CIEXYZ as intermediate format to optimize the color conversion
                        destValues = destCS.fromCIEXYZ(colorSpace.toCIEXYZ(srcValues));
                        for (int k = 0; k < destValues.length; k++)
                        {
                            destValues[k] = destValues[k] * 255f;
                        }
                    }
                    destRaster.setPixel(x, y, destValues);
                }
            }
            return dest;
        }
        return super.toRGBImageAWT(raster, colorSpace);
    }

    /**
     * Lazy initialization holder class
     *
     * @author Andrea Vacondio
     *
     */
    private static final class ConversionContextHolder
    {

        private ConversionContextHolder()
        {
            // hide constructor
        }

        static final ConversionContext CONVERSION_CONTEXT = new ConversionContext();
    }

    private static class ConversionContext
    {
        private final ICC_ColorSpace colorSpace;

        ConversionContext()
        {
            // Adobe Acrobat uses "U.S. Web Coated (SWOP) v2" as the default
            // CMYK profile, however it is not available under an open license.
            // Instead, the "ISO Coated v2 300% (basICColor)" is used, which
            // is an open alternative to the "ISO Coated v2 300% (ECI)" profile.
            try (var iccProfileStream = PDDeviceCMYK.class.getResourceAsStream(
                    "/org/sejda/sambox/resources/icc/ISOcoated_v2_300_bas.icc"))
            {
                this.colorSpace = new ICC_ColorSpace(ICC_Profile.getInstance(iccProfileStream));
            }
            catch (IOException e)
            {
                throw new IllegalStateException("Failed to initialize PDDeviceCMYK", e);
            }
        }

    }

    /**
     * Creates a PDDeviceCMYK instance using the provided ICC profile stream for color conversion
     * which is eagerly loaded.
     * <p>This allows for custom CMYK color profiles to be used instead of the
     * default one:
     * <pre>
     *     PDDeviceCMYK.INSTANCE = PDDeviceCMYK.eagerInstance(yourIccProfileStream);
     * </pre>
     * </p>
     *
     * @param iccProfileStream stream to the icc profile to be used for color conversion
     * @return the PDDeviceCMYK instance
     * @throws IOException              if there is an error reading the ICC profile stream
     * @throws IllegalArgumentException if the provided stream is not a valid ICC profile
     */
    public static PDDeviceCMYK eagerInstance(InputStream iccProfileStream)
            throws IOException, IllegalArgumentException
    {
        var instance = new PDDeviceCMYK()
        {
            private final ICC_ColorSpace colorSpace;

            {
                requireNotNullArg(iccProfileStream, "Cannot load a null ICC profile");
                this.colorSpace = new ICC_ColorSpace(ICC_Profile.getInstance(iccProfileStream));
            }

            @Override
            public float[] toRGB(float[] value)
            {
                return colorSpace.toRGB(value);
            }

            @Override
            public BufferedImage toRGBImage(WritableRaster raster)
            {
                return toRGBImageAWT(raster, colorSpace);
            }
        };
        return instance;
    }
}
