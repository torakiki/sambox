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
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;
import java.util.List;

import javax.imageio.ImageIO;

import org.sejda.commons.util.IOUtils;
import org.sejda.io.SeekableSource;
import org.sejda.io.SeekableSources;
import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSStream;
import org.sejda.sambox.filter.DecodeResult;
import org.sejda.sambox.pdmodel.PDResources;
import org.sejda.sambox.pdmodel.common.PDMetadata;
import org.sejda.sambox.pdmodel.common.PDStream;
import org.sejda.sambox.pdmodel.graphics.PDXObject;
import org.sejda.sambox.pdmodel.graphics.color.PDColorSpace;
import org.sejda.sambox.pdmodel.graphics.color.PDDeviceGray;
import org.sejda.sambox.pdmodel.graphics.color.PDDeviceRGB;
import org.sejda.sambox.pdmodel.graphics.color.PDJPXColorSpace;
import org.sejda.sambox.util.filetypedetector.FileType;
import org.sejda.sambox.util.filetypedetector.FileTypeDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
     * @param encodedStream a filtered stream of image data
     * @param cosFilter the filter or a COSArray of filters
     * @param width the image width
     * @param height the image height
     * @param bitsPerComponent the bits per component
     * @param initColorSpace the color space
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
     * @param stream the XObject stream to read
     * @param resources the current resources
     * @throws java.io.IOException if there is an error creating the XObject.
     */
    public PDImageXObject(PDStream stream, PDResources resources) throws IOException
    {
        super(stream, COSName.IMAGE);
        this.resources = resources;
        List<COSName> filters = stream.getFilters();
        if (filters != null && !filters.isEmpty()
                && COSName.JPX_DECODE.equals(filters.get(filters.size() - 1)))
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
                return CCITTFactory.createFromSeekableSource(source);
            }
            catch (IOException ex)
            {
                LOG.warn("Reading as TIFF failed, setting fileType to PNG", ex);
                // Plan B: try reading with ImageIO
                // common exception:
                // First image in tiff is not CCITT T4 or T6 compressed
            }
        }
        // last resort, let's see if ImageIO can read it
        BufferedImage image;
        try
        {
            image = ImageIO.read(source.asNewInputStream());
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

        // get image as RGB
        BufferedImage image = SampledImageReader.getRGBImage(this, getColorKeyMask());

        // soft mask (overrides explicit mask)
        PDImageXObject softMask = getSoftMask();
        if (softMask != null)
        {
            float[] matte = extractMatte(softMask);
            image = applyMask(image, softMask.getOpaqueImage(), true, matte);
        }
        else
        {
            // explicit mask - to be applied only if /ImageMask true
            PDImageXObject mask = getMask();
            if (mask != null && mask.isStencil())
            {
                image = applyMask(image, mask.getOpaqueImage(), false, null);
            }
        }

        cachedImage = new SoftReference<>(image);
        return image;
    }

    private float[] extractMatte(PDImageXObject softMask) throws IOException
    {
        float[] matte = ofNullable(
                softMask.getCOSObject().getDictionaryObject(COSName.MATTE, COSArray.class))
                        .map(COSArray::toFloatArray).orElse(null);
        if (nonNull(matte))
        {
            // PDFBOX-4267: process /Matte
            // convert to RGB
            PDColorSpace colorSpace = getColorSpace();
            if(colorSpace instanceof PDJPXColorSpace) {
                // #125 JPX color spaces don't support drawing
                colorSpace = PDDeviceRGB.INSTANCE; 
            }
            return colorSpace.toRGB(matte);
        }
        return null;
    }

    /**
     * 
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
     * Returns an RGB buffered image containing the opaque image stream without any masks applied. If this Image XObject
     * is a mask then the buffered image will contain the raw mask.
     * 
     * @return the image without any masks applied
     * @throws IOException if the image cannot be read
     */
    public BufferedImage getOpaqueImage() throws IOException
    {
        return SampledImageReader.getRGBImage(this, null);
    }

    // explicit mask: RGB + Binary -> ARGB
    // soft mask: RGB + Gray -> ARGB
    private BufferedImage applyMask(BufferedImage image, BufferedImage mask, boolean isSoft,
            float[] matte)
    {
        if (mask == null)
        {
            return image;
        }

        int width = image.getWidth();
        int height = image.getHeight();

        // scale mask to fit image, or image to fit mask, whichever is larger
        if (mask.getWidth() < width || mask.getHeight() < height)
        {
            mask = scaleImage(mask, width, height);
        }
        else if (mask.getWidth() > width || mask.getHeight() > height)
        {
            width = mask.getWidth();
            height = mask.getHeight();
            image = scaleImage(image, width, height);
        }
        else if (image.getRaster().getPixel(0, 0, (int[]) null).length < 3)
        {
            // PDFBOX-4470 bitonal image has only one element => copy into RGB
            image = scaleImage(image, width, height);
        }

        // compose to ARGB
        BufferedImage masked = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        WritableRaster src = image.getRaster();
        WritableRaster dest = masked.getRaster();
        WritableRaster alpha = mask.getRaster();

        float[] rgb = new float[4];
        float[] rgba = new float[4];
        float[] alphaPixel = null;
        for (int y = 0; y < height; y++)
        {
            for (int x = 0; x < width; x++)
            {
                src.getPixel(x, y, rgb);

                rgba[0] = rgb[0];
                rgba[1] = rgb[1];
                rgba[2] = rgb[2];

                alphaPixel = alpha.getPixel(x, y, alphaPixel);
                if (isSoft)
                {
                    rgba[3] = alphaPixel[0];
                    if (matte != null && Float.compare(alphaPixel[0], 0) != 0)
                    {
                        rgba[0] = clampColor(
                                ((rgba[0] / 255 - matte[0]) / (alphaPixel[0] / 255) + matte[0])
                                        * 255);
                        rgba[1] = clampColor(
                                ((rgba[1] / 255 - matte[1]) / (alphaPixel[0] / 255) + matte[1])
                                        * 255);
                        rgba[2] = clampColor(
                                ((rgba[2] / 255 - matte[2]) / (alphaPixel[0] / 255) + matte[2])
                                        * 255);
                    }
                }
                else
                {
                    rgba[3] = 255 - alphaPixel[0];
                }

                dest.setPixel(x, y, rgba);
            }
        }

        return masked;
    }

    private static float clampColor(float color)
    {
        return color < 0 ? 0 : (color > 255 ? 255 : color);
    }

    /**
     * High-quality image scaling.
     */
    private BufferedImage scaleImage(BufferedImage image, int width, int height)
    {
        BufferedImage image2 = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image2.createGraphics();
        if (getInterpolate())
        {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        }
        g.drawImage(image, 0, 0, width, height, 0, 0, image.getWidth(), image.getHeight(), null);
        g.dispose();
        return image2;
    }
    
    public boolean hasMask()
    {
        COSStream cosStream = getCOSObject().getDictionaryObject(COSName.MASK, COSStream.class);
        return cosStream != null;
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
        COSStream cosStream = getCOSObject().getDictionaryObject(COSName.SMASK, COSStream.class);
        return cosStream != null;
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

}
