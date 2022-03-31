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
package org.sejda.sambox.pdmodel.graphics.image;

import org.sejda.commons.FastByteArrayOutputStream;
import org.sejda.io.SeekableSource;
import org.sejda.io.SeekableSources;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.pdmodel.graphics.color.PDColorSpace;
import org.sejda.sambox.pdmodel.graphics.color.PDDeviceCMYK;
import org.sejda.sambox.pdmodel.graphics.color.PDDeviceGray;
import org.sejda.sambox.pdmodel.graphics.color.PDDeviceRGB;
import org.w3c.dom.Element;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.WritableRaster;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import static java.util.Objects.nonNull;
import static org.sejda.commons.util.RequireUtils.requireIOCondition;

/**
 * Factory for creating a PDImageXObject containing a JPEG compressed image.
 *
 * @author John Hewson
 */
public final class JPEGFactory
{
    private JPEGFactory()
    {
    }

    /**
     * Creates a new JPEG Image XObject from an input stream containing JPEG data.
     * <p>
     * The input stream data will be preserved and embedded in the PDF file without modification.
     *
     * @param file a JPEG file
     * @return a new Image XObject
     * @throws IOException if the input stream cannot be read
     */
    public static PDImageXObject createFromFile(File file) throws IOException
    {
        try (SeekableSource source = SeekableSources.seekableSourceFrom(file))
        {
            return createFromSeekableSource(source);
        }
    }

    public static PDImageXObject createFromSeekableSource(SeekableSource source) throws IOException
    {
        // read image
        BufferedImage awtImage = readJpeg(source.asNewInputStream());

        // create Image XObject from stream
        PDImageXObject pdImage = new PDImageXObject(
                new BufferedInputStream(source.asNewInputStream()), COSName.DCT_DECODE,
                awtImage.getWidth(), awtImage.getHeight(),
                awtImage.getColorModel().getComponentSize(0), getColorSpaceFromAWT(awtImage));

        // no alpha
        if (awtImage.getColorModel().hasAlpha())
        {
            throw new UnsupportedOperationException("alpha channel not implemented");
        }

        return pdImage;
    }

    public static BufferedImage readJpegFile(File file) throws IOException
    {
        return readJpeg(file);
    }

    private static BufferedImage readJpeg(Object fileOrStream) throws IOException
    {
        Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("JPEG");
        ImageReader reader = null;
        while (readers.hasNext())
        {
            reader = readers.next();
            if (reader.canReadRaster())
            {
                break;
            }
        }
        requireIOCondition(nonNull(reader), "Cannot find an ImageIO reader for JPEG image");

        try (ImageInputStream iis = ImageIO.createImageInputStream(fileOrStream))
        {
            reader.setInput(iis);
            ImageIO.setUseCache(false);
            return reader.read(0);
        }
        finally
        {
            reader.dispose();
        }
    }

    /**
     * Creates a new JPEG Image XObject from a Buffered Image.
     *
     * @param image the buffered image to embed
     * @return a new Image XObject
     * @throws IOException if the JPEG data cannot be written
     */
    public static PDImageXObject createFromImage(BufferedImage image) throws IOException
    {
        return createFromImage(image, 0.75f);
    }

    /**
     * Creates a new JPEG Image XObject from a Buffered Image and a given quality. The image will be
     * created at 72 DPI.
     *
     * @param image   the buffered image to embed
     * @param quality the desired JPEG compression quality
     * @return a new Image XObject
     * @throws IOException if the JPEG data cannot be written
     */
    public static PDImageXObject createFromImage(BufferedImage image, float quality)
            throws IOException
    {
        return createFromImage(image, quality, 72);
    }

    /**
     * Creates a new JPEG Image XObject from a Buffered Image, a given quality and DPI.
     *
     * @param image   the buffered image to embed
     * @param quality the desired JPEG compression quality
     * @param dpi     the desired DPI (resolution) of the JPEG
     * @return a new Image XObject
     * @throws IOException if the JPEG data cannot be written
     */
    public static PDImageXObject createFromImage(BufferedImage image, float quality, int dpi)
            throws IOException
    {
        return createJPEG(image, quality, dpi);
    }

    // returns the alpha channel of an image
    private static BufferedImage getAlphaImage(BufferedImage image)
    {
        if (!image.getColorModel().hasAlpha())
        {
            return null;
        }
        if (image.getTransparency() == Transparency.BITMASK)
        {
            throw new UnsupportedOperationException("BITMASK Transparency JPEG compression is not"
                    + " useful, use LosslessImageFactory instead");
        }
        WritableRaster alphaRaster = image.getAlphaRaster();
        if (alphaRaster == null)
        {
            // happens sometimes (PDFBOX-2654) despite colormodel claiming to have alpha
            return null;
        }
        BufferedImage alphaImage = new BufferedImage(image.getWidth(), image.getHeight(),
                BufferedImage.TYPE_BYTE_GRAY);
        alphaImage.setData(alphaRaster);
        return alphaImage;
    }

    // Creates an Image XObject from a Buffered Image using JAI Image I/O
    private static PDImageXObject createJPEG(BufferedImage image, float quality, int dpi)
            throws IOException
    {
        // extract alpha channel (if any)
        BufferedImage awtColorImage = getColorImage(image);

        // create XObject
        ByteArrayInputStream byteStream = new ByteArrayInputStream(
                encodeImageToJPEGStream(awtColorImage, quality, dpi));

        PDImageXObject pdImage = new PDImageXObject(byteStream, COSName.DCT_DECODE,
                awtColorImage.getWidth(), awtColorImage.getHeight(), 8,
                getColorSpaceFromAWT(awtColorImage));

        // alpha -> soft mask
        BufferedImage awtAlphaImage = getAlphaImage(image);
        if (awtAlphaImage != null)
        {
            PDImage xAlpha = JPEGFactory.createFromImage(awtAlphaImage, quality);
            pdImage.getCOSObject().setItem(COSName.SMASK, xAlpha);
        }

        return pdImage;
    }

    private static byte[] encodeImageToJPEGStream(BufferedImage image, float quality, int dpi)
            throws IOException
    {
        ImageWriter imageWriter = ImageIO.getImageWritersBySuffix("jpeg").next(); // find JAI writer
        FastByteArrayOutputStream out = new FastByteArrayOutputStream();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(out))
        {
            imageWriter.setOutput(ios);

            // add compression
            ImageWriteParam jpegParam = imageWriter.getDefaultWriteParam();
            jpegParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            jpegParam.setCompressionQuality(quality);

            // add metadata
            ImageTypeSpecifier imageTypeSpecifier = new ImageTypeSpecifier(image);
            IIOMetadata metadata = imageWriter.getDefaultImageMetadata(imageTypeSpecifier,
                    jpegParam);
            Element tree = (Element) metadata.getAsTree("javax_imageio_jpeg_image_1.0");
            Element jfif = (Element) tree.getElementsByTagName("app0JFIF").item(0);
            String dpiString = Integer.toString(dpi);
            jfif.setAttribute("Xdensity", dpiString);
            jfif.setAttribute("Ydensity", dpiString);
            jfif.setAttribute("resUnits", "1"); // 1 = dots/inch

            // write
            imageWriter.write(metadata, new IIOImage(image, null, null), jpegParam);
        }
        finally
        {
            // clean up
            if (imageWriter != null)
            {
                imageWriter.dispose();
            }
        }
        return out.toByteArray();
    }

    // returns a PDColorSpace for a given BufferedImage
    public static PDColorSpace getColorSpaceFromAWT(BufferedImage awtImage)
    {
        if (awtImage.getColorModel().getNumComponents() == 1)
        {
            // 256 color (gray) JPEG
            return PDDeviceGray.INSTANCE;
        }

        ColorSpace awtColorSpace = awtImage.getColorModel().getColorSpace();
        if (awtColorSpace instanceof ICC_ColorSpace && !awtColorSpace.isCS_sRGB())
        {
            throw new UnsupportedOperationException("ICC color spaces not implemented");
        }

        switch (awtColorSpace.getType())
        {
        case ColorSpace.TYPE_RGB:
            return PDDeviceRGB.INSTANCE;
        case ColorSpace.TYPE_GRAY:
            return PDDeviceGray.INSTANCE;
        case ColorSpace.TYPE_CMYK:
            return PDDeviceCMYK.INSTANCE;
        default:
            throw new UnsupportedOperationException(
                    "color space not implemented: " + awtColorSpace.getType());
        }
    }

    // returns the color channels of an image
    private static BufferedImage getColorImage(BufferedImage image)
    {
        if (!image.getColorModel().hasAlpha())
        {
            return image;
        }

        if (image.getColorModel().getColorSpace().getType() != ColorSpace.TYPE_RGB)
        {
            throw new UnsupportedOperationException("only RGB color spaces are implemented");
        }

        // create an RGB image without alpha
        // BEWARE: the previous solution in the history
        // g.setComposite(AlphaComposite.Src) and g.drawImage()
        // didn't work properly for TYPE_4BYTE_ABGR.
        // alpha values of 0 result in a black dest pixel!!!
        BufferedImage rgbImage = new BufferedImage(image.getWidth(), image.getHeight(),
                BufferedImage.TYPE_3BYTE_BGR);
        return new ColorConvertOp(null).filter(image, rgbImage);
    }
}
