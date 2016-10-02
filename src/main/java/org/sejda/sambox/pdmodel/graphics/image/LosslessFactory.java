/*
 * Copyright 2014 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sejda.sambox.pdmodel.graphics.image;

import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.stream.MemoryCacheImageOutputStream;

import org.sejda.io.FastByteArrayOutputStream;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.filter.Filter;
import org.sejda.sambox.filter.FilterFactory;
import org.sejda.sambox.pdmodel.graphics.color.PDColorSpace;
import org.sejda.sambox.pdmodel.graphics.color.PDDeviceColorSpace;
import org.sejda.sambox.pdmodel.graphics.color.PDDeviceGray;
import org.sejda.sambox.pdmodel.graphics.color.PDDeviceRGB;

/**
 * Factory for creating a PDImageXObject containing a lossless compressed image.
 *
 * @author Tilman Hausherr
 */
public final class LosslessFactory
{
    private LosslessFactory()
    {
    }

    /**
     * Creates a new lossless encoded Image XObject from a Buffered Image.
     *
     * @param image the buffered image to embed
     * @return a new Image XObject
     * @throws IOException if something goes wrong
     */
    public static PDImageXObject createFromImage(BufferedImage image) throws IOException
    {
        int bpc;
        PDDeviceColorSpace deviceColorSpace;

        int height = image.getHeight();
        int width = image.getWidth();
        int[] rgbLineBuffer = new int[width];
        byte[] imageData;

        if ((image.getType() == BufferedImage.TYPE_BYTE_GRAY
                && image.getColorModel().getPixelSize() <= 8)
                || (image.getType() == BufferedImage.TYPE_BYTE_BINARY
                        && image.getColorModel().getPixelSize() == 1))
        {
            // grayscale images need one color per sample
            bpc = image.getColorModel().getPixelSize();
            deviceColorSpace = PDDeviceGray.INSTANCE;

            FastByteArrayOutputStream bos = new FastByteArrayOutputStream(
                    (width * bpc / 8) + (width * bpc % 8 != 0 ? 1 : 0) * height);
            try (MemoryCacheImageOutputStream mcios = new MemoryCacheImageOutputStream(bos))
            {

                for (int y = 0; y < height; ++y)
                {
                    for (int pixel : image.getRGB(0, y, width, 1, rgbLineBuffer, 0, width))
                    {
                        mcios.writeBits(pixel & 0xFF, bpc);
                    }

                    int bitOffset = mcios.getBitOffset();
                    if (bitOffset != 0)
                    {
                        mcios.writeBits(0, 8 - bitOffset);
                    }
                }
                mcios.flush();
            }
            imageData = bos.toByteArray();
        }
        else
        {
            // RGB
            bpc = 8;
            deviceColorSpace = PDDeviceRGB.INSTANCE;
            imageData = new byte[width * height * 3];
            int byteIdx = 0;

            for (int y = 0; y < height; ++y)
            {
                for (int pixel : image.getRGB(0, y, width, 1, rgbLineBuffer, 0, width))
                {
                    imageData[byteIdx++] = (byte) ((pixel >> 16) & 0xFF);
                    imageData[byteIdx++] = (byte) ((pixel >> 8) & 0xFF);
                    imageData[byteIdx++] = (byte) (pixel & 0xFF);
                }
            }
        }

        PDImageXObject pdImage = prepareImageXObject(imageData, image.getWidth(), image.getHeight(),
                bpc, deviceColorSpace);

        // alpha -> soft mask
        PDImage xAlpha = createAlphaFromARGBImage(image);
        if (xAlpha != null)
        {
            pdImage.getCOSObject().setItem(COSName.SMASK, xAlpha);
        }

        return pdImage;
    }

    /**
     * Creates a grayscale Flate encoded PDImageXObject from the alpha channel of an image.
     *
     * @param image an ARGB image.
     *
     * @return the alpha channel of an image as a grayscale image.
     *
     * @throws IOException if something goes wrong
     */
    private static PDImageXObject createAlphaFromARGBImage(BufferedImage image) throws IOException
    {
        // this implementation makes the assumption that the raster uses
        // SinglePixelPackedSampleModel, i.e. the values can be used 1:1 for
        // the stream.
        // Sadly the type of the databuffer is TYPE_INT and not TYPE_BYTE.
        if (!image.getColorModel().hasAlpha())
        {
            return null;
        }

        // extract the alpha information
        WritableRaster alphaRaster = image.getAlphaRaster();
        if (alphaRaster == null)
        {
            // happens sometimes (PDFBOX-2654) despite colormodel claiming to have alpha
            return createAlphaFromARGBImage2(image);
        }

        int[] pixels = alphaRaster.getPixels(0, 0, alphaRaster.getWidth(), alphaRaster.getHeight(),
                (int[]) null);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int bpc;
        if (image.getTransparency() == Transparency.BITMASK)
        {
            bpc = 1;
            MemoryCacheImageOutputStream mcios = new MemoryCacheImageOutputStream(bos);
            int width = alphaRaster.getWidth();
            int p = 0;
            for (int pixel : pixels)
            {
                mcios.writeBit(pixel);
                ++p;
                if (p % width == 0)
                {
                    while (mcios.getBitOffset() != 0)
                    {
                        mcios.writeBit(0);
                    }
                }
            }
            mcios.flush();
            mcios.close();
        }
        else
        {
            bpc = 8;
            for (int pixel : pixels)
            {
                bos.write(pixel);
            }
        }

        PDImageXObject pdImage = prepareImageXObject(bos.toByteArray(), image.getWidth(),
                image.getHeight(), bpc, PDDeviceGray.INSTANCE);

        return pdImage;
    }

    // create alpha image the hard way: get the alpha through getRGB()
    private static PDImageXObject createAlphaFromARGBImage2(BufferedImage bi) throws IOException
    {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int bpc;
        if (bi.getTransparency() == Transparency.BITMASK)
        {
            bpc = 1;
            MemoryCacheImageOutputStream mcios = new MemoryCacheImageOutputStream(bos);
            for (int y = 0, h = bi.getHeight(); y < h; ++y)
            {
                for (int x = 0, w = bi.getWidth(); x < w; ++x)
                {
                    int alpha = bi.getRGB(x, y) >>> 24;
                    mcios.writeBit(alpha);
                }
                while (mcios.getBitOffset() != 0)
                {
                    mcios.writeBit(0);
                }
            }
            mcios.flush();
            mcios.close();
        }
        else
        {
            bpc = 8;
            for (int y = 0, h = bi.getHeight(); y < h; ++y)
            {
                for (int x = 0, w = bi.getWidth(); x < w; ++x)
                {
                    int alpha = bi.getRGB(x, y) >>> 24;
                    bos.write(alpha);
                }
            }
        }

        PDImageXObject pdImage = prepareImageXObject(bos.toByteArray(), bi.getWidth(),
                bi.getHeight(), bpc, PDDeviceGray.INSTANCE);

        return pdImage;
    }

    /**
     * Create a PDImageXObject while making a decision whether not to compress, use Flate filter only, or Flate and LZW
     * filters.
     * 
     * @param byteArray array with data.
     * @param width the image width
     * @param height the image height
     * @param bitsPerComponent the bits per component
     * @param initColorSpace the color space
     * @return the newly created PDImageXObject with the data compressed.
     * @throws IOException
     */
    private static PDImageXObject prepareImageXObject(byte[] byteArray, int width, int height,
            int bitsPerComponent, PDColorSpace initColorSpace) throws IOException
    {
        // pre-size the output stream to half of the input
        FastByteArrayOutputStream baos = new FastByteArrayOutputStream(byteArray.length / 2);

        Filter filter = FilterFactory.INSTANCE.getFilter(COSName.FLATE_DECODE);
        filter.encode(new ByteArrayInputStream(byteArray), baos, new COSDictionary());

        ByteArrayInputStream encodedByteStream = new ByteArrayInputStream(baos.toByteArray());
        return new PDImageXObject(encodedByteStream, COSName.FLATE_DECODE, width, height,
                bitsPerComponent, initColorSpace);
    }

}
