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

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.sejda.commons.util.RequireUtils.requireNotNullArg;

import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.ImagingOpException;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;

import org.sejda.commons.util.IOUtils;
import org.sejda.io.SeekableSource;
import org.sejda.io.SeekableSources;
import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSStream;
import org.sejda.sambox.filter.DecodeResult;
import org.sejda.sambox.pdmodel.PDResources;
import org.sejda.sambox.pdmodel.common.PDMetadata;
import org.sejda.sambox.pdmodel.common.PDStream;
import org.sejda.sambox.pdmodel.documentinterchange.markedcontent.PDPropertyList;
import org.sejda.sambox.pdmodel.graphics.PDXObject;
import org.sejda.sambox.pdmodel.graphics.color.PDColorSpace;
import org.sejda.sambox.pdmodel.graphics.color.PDDeviceGray;
import org.sejda.sambox.pdmodel.graphics.color.PDDeviceRGB;
import org.sejda.sambox.pdmodel.graphics.color.PDJPXColorSpace;
import org.sejda.sambox.util.filetypedetector.FileType;
import org.sejda.sambox.util.filetypedetector.FileTypeDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

/**
 * An Image XObject.
 *
 * @author John Hewson
 * @author Ben Litchfield
 */
public final class PDImageXObject extends PDXObject implements PDImage
{

    private static final Logger LOG = LoggerFactory.getLogger(PDImageXObject.class);

    private SoftReference<BufferedImage> cachedImage;
    private PDColorSpace colorSpace;
    private PDResources resources; // current resource dictionary (has color spaces)

    /**
     * Creates a thumbnail Image XObject from the given COSBase and name.
     *
     * @param cosStream the COS stream
     * @return an XObject
     * @throws IOException if there is an error creating the XObject.
     */
    public static PDImageXObject createThumbnail(COSStream cosStream) throws IOException
    {
        // thumbnails are special, any non-null subtype is treated as being "Image"
        PDStream pdStream = new PDStream(cosStream);
        return new PDImageXObject(pdStream, null);
    }

    /**
     * Creates an Image XObject in the given document.
     *
     * @throws java.io.IOException if there is an error creating the XObject.
     */
    public PDImageXObject() throws IOException
    {
        this(new PDStream(), null);
    }

    /**
     * Creates an Image XObject in the given document using the given filtered stream.
     *
     * @param encodedStream    a filtered stream of image data
     * @param cosFilter        the filter or a COSArray of filters
     * @param width            the image width
     * @param height           the image height
     * @param bitsPerComponent the bits per component
     * @param initColorSpace   the color space
     * @throws IOException if there is an error creating the XObject.
     */
    public PDImageXObject(InputStream encodedStream, COSBase cosFilter, int width, int height,
            int bitsPerComponent, PDColorSpace initColorSpace) throws IOException
    {
        super(createRawStream(encodedStream), COSName.IMAGE);
        getCOSObject().setItem(COSName.FILTER, cosFilter);
        resources = null;
        colorSpace = null;
        setBitsPerComponent(bitsPerComponent);
        setWidth(width);
        setHeight(height);
        setColorSpace(initColorSpace);
    }

    /**
     * Creates a COS stream from raw (encoded) data.
     */
    private static COSStream createRawStream(InputStream rawInput) throws IOException
    {
        COSStream stream = new COSStream();
        try (OutputStream output = stream.createFilteredStream())
        {
            IOUtils.copy(rawInput, output);
        }
        return stream;
    }

    /**
     * Creates an Image XObject with the given stream as its contents and current color spaces.
     *
     * @param stream    the XObject stream to read
     * @param resources the current resources
     * @throws java.io.IOException if there is an error creating the XObject.
     */
    public PDImageXObject(PDStream stream, PDResources resources) throws IOException
    {
        super(stream, COSName.IMAGE);
        this.resources = resources;
        List<COSName> filters = stream.getFilters();
        if (filters != null && !filters.isEmpty() && COSName.JPX_DECODE.equals(
                filters.get(filters.size() - 1)))
        {
            DecodeResult decodeResult = stream.getCOSObject().getDecodeResult();
            stream.getCOSObject().addAll(decodeResult.getParameters());
            this.colorSpace = decodeResult.getJPXColorSpace();
        }
    }

    public static PDImageXObject createFromFile(String imagePath) throws IOException
    {
        return createFromFile(new File(imagePath));
    }

    public static PDImageXObject createFromFile(File file) throws IOException
    {
        requireNotNullArg(file, "Cannot create image from a null file");
        try (SeekableSource source = SeekableSources.seekableSourceFrom(file))
        {
            return createFromSeekableSource(source, file.getName());
        }
    }

    public static PDImageXObject createFromSeekableSource(SeekableSource source, String filename)
            throws IOException
    {
        return createFromSeekableSource(source, filename, 0);
    }

    // same as ImageIO.read() but allows reading a specific image from a multi-page image (eg: tiff)
    public static BufferedImage read(InputStream inputStream, int number) throws IOException
    {
        if (number == 0)
        {
            return ImageIO.read(inputStream);
        }

        BufferedImage image = null;
        ImageInputStream iis = ImageIO.createImageInputStream(inputStream);
        Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);

        if (readers.hasNext())
        {
            ImageReader reader = readers.next();
            reader.setInput(iis);

            try
            {
                image = reader.read(number);
            }
            finally
            {
                reader.dispose();
                iis.close();
            }
        }

        return image;
    }

    public static PDImageXObject createFromSeekableSource(SeekableSource source, String filename,
            int number) throws IOException
    {
        // we first try to match the first bytes to some known pattern, so we don't rely on the extension first
        FileType fileType = FileTypeDetector.detectFileType(source);

        if (fileType.equals(FileType.JPEG))
        {
            return JPEGFactory.createFromSeekableSource(source);
        }
        if (fileType.equals(FileType.TIFF))
        {
            try
            {
                return CCITTFactory.createFromSeekableSource(source, number);
            }
            catch (IOException ex)
            {
                LOG.warn("Reading as TIFF failed using CCITTFactory, falling back to ImageIO: {}",
                        ex.getMessage());
                // Plan B: try reading with ImageIO
                // common exception:
                // First image in tiff is not CCITT T4 or T6 compressed
            }
        }
        // last resort, let's see if ImageIO can read it
        BufferedImage image;
        try
        {
            image = read(source.asNewInputStream(), number);
        }
        catch (Exception e)
        {
            LOG.warn(String.format("An error occurred while reading image: %s type: %s", filename,
                    fileType), e);
            throw new UnsupportedImageFormatException(fileType, filename, e);
        }

        if (image == null)
        {
            LOG.warn(String.format("Could not read image format: %s type: %s", filename, fileType));
            throw new UnsupportedImageFormatException(fileType, filename, null);
        }
        return LosslessFactory.createFromImage(image);
    }

    /**
     * Returns the metadata associated with this XObject, or null if there is none.
     *
     * @return the metadata associated with this object.
     */
    public PDMetadata getMetadata()
    {
        COSStream cosStream = getCOSObject().getDictionaryObject(COSName.METADATA, COSStream.class);
        if (cosStream != null)
        {
            return new PDMetadata(cosStream);
        }
        return null;
    }

    /**
     * Sets the metadata associated with this XObject, or null if there is none.
     *
     * @param meta the metadata associated with this object
     */
    public void setMetadata(PDMetadata meta)
    {
        getCOSObject().setItem(COSName.METADATA, meta);
    }

    /**
     * Returns the key of this XObject in the structural parent tree.
     *
     * @return this object's key the structural parent tree
     */
    public int getStructParent()
    {
        return getCOSObject().getInt(COSName.STRUCT_PARENT);
    }

    /**
     * Sets the key of this XObject in the structural parent tree.
     *
     * @param key the new key for this XObject
     */
    public void setStructParent(int key)
    {
        getCOSObject().setInt(COSName.STRUCT_PARENT, key);
    }

    /**
     * {@inheritDoc} The returned images are cached for the lifetime of this XObject.
     */
    @Override
    public BufferedImage getImage() throws IOException
    {
        if (cachedImage != null)
        {
            BufferedImage cached = cachedImage.get();
            if (cached != null)
            {
                return cached;
            }
        }

        // get RGB image w/o reference because applyMask might modify it, take long time and a lot of memory.
        final BufferedImage image;
        final PDImageXObject softMask = getSoftMask();
        final PDImageXObject mask = getMask();
        // soft mask (overrides explicit mask)
        if (softMask != null)
        {
            image = applyMask(SampledImageReader.getRGBImage(this, getColorKeyMask()),
                    softMask.getOpaqueImage(), softMask.getInterpolate(), true,
                    extractMatte(softMask));
        }
        // explicit mask - to be applied only if /ImageMask true
        else if (mask != null && mask.isStencil())
        {
            image = applyMask(SampledImageReader.getRGBImage(this, getColorKeyMask()),
                    mask.getOpaqueImage(), mask.getInterpolate(), false, null);
        }
        else
        {
            image = SampledImageReader.getRGBImage(this, getColorKeyMask());
        }

        cachedImage = new SoftReference<>(image);
        return image;
    }

    @Override
    public BufferedImage getRawImage() throws IOException
    {
        return getColorSpace().toRawImage(getRawRaster());
    }

    @Override
    public WritableRaster getRawRaster() throws IOException
    {
        return SampledImageReader.getRawRaster(this);
    }

    private float[] extractMatte(PDImageXObject softMask) throws IOException
    {
        float[] matte = ofNullable(
                softMask.getCOSObject().getDictionaryObject(COSName.MATTE, COSArray.class)).map(
                COSArray::toFloatArray).orElse(null);
        if (nonNull(matte))
        {
            // PDFBOX-4267: process /Matte
            // convert to RGB
            PDColorSpace colorSpace = getColorSpace();
            if (colorSpace instanceof PDJPXColorSpace)
            {
                // #125 JPX color spaces don't support drawing
                colorSpace = PDDeviceRGB.INSTANCE;
            }
            if (matte.length < colorSpace.getNumberOfComponents())
            {
                LOG.error("Image /Matte entry not long enough for colorspace, skipped");
                return null;
            }
            return colorSpace.toRGB(matte);
        }
        return null;
    }

    /**
     * @return the image without mask applied. The image is not cached
     * @throws IOException
     */
    public BufferedImage getImageWithoutMasks() throws IOException
    {
        return SampledImageReader.getRGBImage(this, getColorKeyMask());
    }

    /**
     * {@inheritDoc} The returned images are not cached.
     */
    @Override
    public BufferedImage getStencilImage(Paint paint) throws IOException
    {
        if (!isStencil())
        {
            throw new IllegalStateException("Image is not a stencil");
        }
        return SampledImageReader.getStencilImage(this, paint);
    }

    /**
     * Returns an RGB buffered image containing the opaque image stream without any masks applied.
     * If this Image XObject is a mask then the buffered image will contain the raw mask.
     *
     * @return the image without any masks applied
     * @throws IOException if the image cannot be read
     */
    public BufferedImage getOpaqueImage() throws IOException
    {
        return SampledImageReader.getRGBImage(this, null);
    }

    /**
     * @param image           The image to apply the mask to as alpha channel.
     * @param mask            A mask image in 8 bit Gray. Even for a stencil mask image due to
     *                        {@link #getOpaqueImage()} and {@link SampledImageReader}'s
     *                        {@code from1Bit()} special handling of DeviceGray.
     * @param interpolateMask interpolation flag of the mask image.
     * @param isSoft          {@code true} if a soft mask. If not stencil mask, then alpha will be
     *                        inverted by this method.
     * @param matte           an optional RGB matte if a soft mask.
     * @return an ARGB image (can be the altered original image)
     */
    private BufferedImage applyMask(BufferedImage image, BufferedImage mask,
            boolean interpolateMask, boolean isSoft, float[] matte)
    {
        if (mask == null)
        {
            return image;
        }

        final int width = Math.max(image.getWidth(), mask.getWidth());
        final int height = Math.max(image.getHeight(), mask.getHeight());

        // scale mask to fit image, or image to fit mask, whichever is larger.
        // also make sure that mask is 8 bit gray and image is ARGB as this
        // is what needs to be returned.
        if (mask.getWidth() < width || mask.getHeight() < height)
        {
            mask = scaleImage(mask, width, height, BufferedImage.TYPE_BYTE_GRAY, interpolateMask);
        }
        else if (mask.getType() != BufferedImage.TYPE_BYTE_GRAY)
        {
            mask = scaleImage(mask, width, height, BufferedImage.TYPE_BYTE_GRAY, false);
        }

        if (image.getWidth() < width || image.getHeight() < height)
        {
            image = scaleImage(image, width, height, BufferedImage.TYPE_INT_ARGB, getInterpolate());
        }
        else if (image.getType() != BufferedImage.TYPE_INT_ARGB)
        {
            image = scaleImage(image, width, height, BufferedImage.TYPE_INT_ARGB, false);
        }

        // compose alpha into ARGB image, either:
        // - very fast by direct bit combination if not a soft mask and a 8 bit alpha source.
        // - fast by letting the sample model do a bulk band operation if no matte is set.
        // - slow and complex by matte calculations on individual pixel components.
        final WritableRaster raster = image.getRaster();
        final WritableRaster alpha = mask.getRaster();
        if (!isSoft && raster.getDataBuffer().getSize() == alpha.getDataBuffer().getSize())
        {
            final DataBuffer dst = raster.getDataBuffer();
            final DataBuffer src = alpha.getDataBuffer();
            for (int i = 0, c = dst.getSize(); c > 0; i++, c--)
            {
                dst.setElem(i, dst.getElem(i) & 0xffffff | ~src.getElem(i) << 24);
            }
        }
        else if (matte == null)
        {
            final int[] samples = new int[width];
            for (int y = 0; y < height; y++)
            {
                alpha.getSamples(0, y, width, 1, 0, samples);
                if (!isSoft)
                {
                    for (int x = 0; x < width; x++)
                    {
                        samples[x] ^= -1;
                    }
                }
                raster.setSamples(0, y, width, 1, 3, samples);
            }
        }
        else
        {
            final int[] alphas = new int[width];
            final int[] pixels = new int[4 * width];
            // Original code is to clamp component and alpha to [0f, 1f] as matte is,
            // and later expand to [0; 255] again (with rounding).
            // component = 255f * ((component / 255f - matte) / (alpha / 255f) + matte)
            //           = (255 * component - 255 * 255f * matte) / alpha + 255f * matte
            // There is a clearly visible factor 255 for most components in above formula,
            // i.e. max value is 255 * 255: 16 bits + sign.
            // Let's use faster fixed point integer arithmetics with Q16.15,
            // introducing neglible errors (0.001%).
            // Note: For "correct" rounding we increase the final matte value (m0h, m1h, m2h) by
            // a half an integer.
            final int fraction = 15;
            final int factor = 255 << fraction;
            final int m0 = Math.round(factor * matte[0]) * 255;
            final int m1 = Math.round(factor * matte[1]) * 255;
            final int m2 = Math.round(factor * matte[2]) * 255;
            final int m0h = m0 / 255 + (1 << fraction - 1);
            final int m1h = m1 / 255 + (1 << fraction - 1);
            final int m2h = m2 / 255 + (1 << fraction - 1);
            for (int y = 0; y < height; y++)
            {
                raster.getPixels(0, y, width, 1, pixels);
                alpha.getSamples(0, y, width, 1, 0, alphas);
                int offset = 0;
                for (int x = 0; x < width; x++)
                {
                    int a = alphas[x];
                    if (a == 0)
                    {
                        offset += 3;
                    }
                    else
                    {
                        pixels[offset] = clampColor(
                                ((pixels[offset++] * factor - m0) / a + m0h) >> fraction);
                        pixels[offset] = clampColor(
                                ((pixels[offset++] * factor - m1) / a + m1h) >> fraction);
                        pixels[offset] = clampColor(
                                ((pixels[offset++] * factor - m2) / a + m2h) >> fraction);
                    }
                    pixels[offset++] = a;
                }
                raster.setPixels(0, y, width, 1, pixels);
            }
        }

        return image;
    }

    private int clampColor(float color)
    {
        return color < 0 ? 0 : (color > 255 ? 255 : Math.round(color));
    }

    /**
     * High-quality image scaling.
     */
    private static BufferedImage scaleImage(BufferedImage image, int width, int height, int type,
            boolean interpolate)
    {
        final int imgWidth = image.getWidth();
        final int imgHeight = image.getHeight();
        // largeScale switch is arbitrarily chosen as to where bicubic becomes very slow
        boolean largeScale =
                width * height > 3000 * 3000 * (type == BufferedImage.TYPE_BYTE_GRAY ? 3 : 1);
        interpolate &= imgWidth != width || imgHeight != height;

        BufferedImage image2 = new BufferedImage(width, height, type);
        if (interpolate)
        {
            AffineTransform af = AffineTransform.getScaleInstance((double) width / imgWidth,
                    (double) height / imgHeight);
            AffineTransformOp afo = new AffineTransformOp(af,
                    largeScale ? AffineTransformOp.TYPE_BILINEAR : AffineTransformOp.TYPE_BICUBIC);
            try
            {
                afo.filter(image, image2);
                return image2;
            }
            catch (ImagingOpException e)
            {
                LOG.warn(e.getMessage(), e);
            }
        }
        Graphics2D g = image2.createGraphics();
        if (interpolate)
        {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    largeScale ? RenderingHints.VALUE_INTERPOLATION_BILINEAR : RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING,
                    largeScale ? RenderingHints.VALUE_RENDER_DEFAULT : RenderingHints.VALUE_RENDER_QUALITY);
        }
        g.drawImage(image, 0, 0, width, height, 0, 0, imgWidth, imgHeight, null);
        g.dispose();
        return image2;
    }

    public boolean hasMask()
    {
        return nonNull(getCOSObject().getDictionaryObject(COSName.MASK, COSStream.class));
    }

    /**
     * Returns the Mask Image XObject associated with this image, or null if there is none.
     *
     * @return Mask Image XObject
     */
    public PDImageXObject getMask() throws IOException
    {
        COSStream cosStream = getCOSObject().getDictionaryObject(COSName.MASK, COSStream.class);
        if (cosStream != null)
        {
            // always DeviceGray
            return new PDImageXObject(new PDStream(cosStream), null);
        }
        return null;
    }

    /**
     * Returns the color key mask array associated with this image, or null if there is none.
     *
     * @return Mask Image XObject
     */
    public COSArray getColorKeyMask()
    {
        return getCOSObject().getDictionaryObject(COSName.MASK, COSArray.class);
    }

    public boolean hasSoftMask()
    {
        return nonNull(getCOSObject().getDictionaryObject(COSName.SMASK, COSStream.class));
    }

    /**
     * Returns the Soft Mask Image XObject associated with this image, or null if there is none.
     *
     * @return the SMask Image XObject, or null.
     */
    public PDImageXObject getSoftMask() throws IOException
    {
        COSStream cosStream = getCOSObject().getDictionaryObject(COSName.SMASK, COSStream.class);
        if (cosStream != null)
        {
            // always DeviceGray
            return new PDImageXObject(new PDStream(cosStream), null);
        }
        return null;
    }

    @Override
    public int getBitsPerComponent()
    {
        if (isStencil())
        {
            return 1;
        }
        return getCOSObject().getInt(COSName.BITS_PER_COMPONENT, COSName.BPC);
    }

    @Override
    public void setBitsPerComponent(int bpc)
    {
        getCOSObject().setInt(COSName.BITS_PER_COMPONENT, bpc);
    }

    @Override
    public PDColorSpace getColorSpace() throws IOException
    {
        if (colorSpace == null)
        {
            COSBase cosBase = getCOSObject().getDictionaryObject(COSName.COLORSPACE, COSName.CS);
            if (cosBase != null)
            {
                colorSpace = PDColorSpace.create(cosBase, resources);
            }
            else if (isStencil())
            {
                // stencil mask color space must be gray, it is often missing
                return PDDeviceGray.INSTANCE;
            }
            else
            {
                // an image without a color space is always broken
                throw new IOException("could not determine color space");
            }
        }
        return colorSpace;
    }

    @Override
    public InputStream createInputStream() throws IOException
    {
        return getStream().createInputStream();
    }

    @Override
    public ByteBuffer asByteBuffer() throws IOException
    {
        return getStream().getCOSObject().getUnfilteredByteBuffer();
    }

    @Override
    public boolean isEmpty() throws IOException
    {
        return getStream().getCOSObject().isEmpty();
    }

    @Override
    public void setColorSpace(PDColorSpace cs)
    {
        getCOSObject().setItem(COSName.COLORSPACE, cs != null ? cs.getCOSObject() : null);
        colorSpace = null;
        cachedImage = null;
    }

    @Override
    public int getHeight()
    {
        return getCOSObject().getInt(COSName.HEIGHT);
    }

    @Override
    public void setHeight(int h)
    {
        getCOSObject().setInt(COSName.HEIGHT, h);
    }

    @Override
    public int getWidth()
    {
        return getCOSObject().getInt(COSName.WIDTH);
    }

    @Override
    public void setWidth(int w)
    {
        getCOSObject().setInt(COSName.WIDTH, w);
    }

    @Override
    public boolean getInterpolate()
    {
        return getCOSObject().getBoolean(COSName.INTERPOLATE, false);
    }

    @Override
    public void setInterpolate(boolean value)
    {
        getCOSObject().setBoolean(COSName.INTERPOLATE, value);
    }

    @Override
    public void setDecode(COSArray decode)
    {
        getCOSObject().setItem(COSName.DECODE, decode);
    }

    @Override
    public COSArray getDecode()
    {
        COSBase decode = getCOSObject().getDictionaryObject(COSName.DECODE);
        if (decode instanceof COSArray)
        {
            return (COSArray) decode;
        }
        return null;
    }

    @Override
    public boolean isStencil()
    {
        return getCOSObject().getBoolean(COSName.IMAGE_MASK, false);
    }

    @Override
    public void setStencil(boolean isStencil)
    {
        getCOSObject().setBoolean(COSName.IMAGE_MASK, isStencil);
    }

    /**
     * @return The optional content group or optional content membership dictionary or null if there
     * is none.
     */
    public PDPropertyList getOptionalContent()
    {
        return ofNullable(getCOSObject().getDictionaryObject(COSName.OC, COSDictionary.class)).map(
                PDPropertyList::create).orElse(null);
    }

    /**
     * Sets the optional content group or optional content membership dictionary.
     */
    public void setOptionalContent(PDPropertyList oc)
    {
        getCOSObject().setItem(COSName.OC, oc);
    }
}
