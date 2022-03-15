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

import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSObjectable;
import org.sejda.sambox.pdmodel.graphics.color.PDColorSpace;

import java.awt.Paint;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * An image in a PDF document.
 *
 * @author John Hewson
 */
public interface PDImage extends COSObjectable
{
    /**
     * Returns the content of this image as an AWT buffered image with an (A)RGB color space. The
     * size of the returned image is the larger of the size of the image itself or its mask.
     *
     * @return content of this image as a buffered image.
     * @throws IOException
     */
    BufferedImage getImage() throws IOException;

    /**
     * Return the image data as WritableRaster. You should consult the PDColorSpace returned by
     * {@link #getColorSpace()} to know how to interpret the data in this WritableRaster.
     * <p>
     * Use this if e.g. want access to the raw color information of a {@link
     * org.sejda.sambox.pdmodel.graphics.color.PDDeviceN} image.
     *
     * @return the raw writable raster for this image
     * @throws IOException
     */
    WritableRaster getRawRaster() throws IOException;

    /**
     * Try to get the raw image as AWT buffered image with it's original colorspace. No color
     * conversion is performed.
     * <p>
     * You could use the returned BufferedImage for draw operations. But this would be very slow as
     * the color conversion would happen on demand. You rather should use {@link #getImage()} for
     * that.
     * <p>
     * This method returns null if it is not possible to map the underlying colorspace into a
     * java.awt.ColorSpace.
     * <p>
     * Use this method if you want to extract the image without loosing any color information, as no
     * color conversion will be performed.
     * <p>
     * You can alwoys use {@link #getRawRaster()}, if you want to access the raw data even if no
     * matching java.awt.ColorSpace exists
     *
     * @return the raw image with a java.awt.ColorSpace or null
     * @throws IOException
     */
    BufferedImage getRawImage() throws IOException;

    /**
     * Returns an ARGB image filled with the given paint and using this image as a mask.
     *
     * @param paint the paint to fill the visible portions of the image with
     * @return a masked image filled with the given paint
     * @throws IOException           if the image cannot be read
     * @throws IllegalStateException if the image is not a stencil.
     */
    BufferedImage getStencilImage(Paint paint) throws IOException;

    /**
     * Returns an InputStream containing the image data, irrespective of whether this is an inline
     * image or an image XObject.
     *
     * @return Decoded stream
     * @throws IOException if the data could not be read.
     */
    InputStream createInputStream() throws IOException;

    /**
     * @return image data in the form of a {@link ByteBuffer}
     */
    ByteBuffer asByteBuffer() throws IOException;

    /**
     * Returns true if the image has no data.
     *
     * @throws IOException
     */
    boolean isEmpty() throws IOException;

    /**
     * Returns true if the image is a stencil mask.
     */
    boolean isStencil();

    /**
     * Sets whether or not the image is a stencil. This corresponds to the {@code ImageMask} entry
     * in the image stream's dictionary.
     *
     * @param isStencil True to make the image a stencil.
     */
    void setStencil(boolean isStencil);

    /**
     * Returns bits per component of this image, or -1 if one has not been set.
     */
    int getBitsPerComponent();

    /**
     * Set the number of bits per component.
     *
     * @param bitsPerComponent The number of bits per component.
     */
    void setBitsPerComponent(int bitsPerComponent);

    /**
     * Returns the image's color space.
     *
     * @throws IOException If there is an error getting the color space.
     */
    PDColorSpace getColorSpace() throws IOException;

    /**
     * Sets the color space for this image.
     *
     * @param colorSpace The color space for this image.
     */
    void setColorSpace(PDColorSpace colorSpace);

    /**
     * Returns height of this image, or -1 if one has not been set.
     */
    int getHeight();

    /**
     * Sets the height of the image.
     *
     * @param height The height of the image.
     */
    void setHeight(int height);

    /**
     * Returns the width of this image, or -1 if one has not been set.
     */
    int getWidth();

    /**
     * Sets the width of the image.
     *
     * @param width The width of the image.
     */
    void setWidth(int width);

    /**
     * Sets the decode array.
     *
     * @param decode the new decode array.
     */
    void setDecode(COSArray decode);

    /**
     * Returns the decode array.
     */
    COSArray getDecode();

    /**
     * Returns true if the image should be interpolated when rendered.
     */
    boolean getInterpolate();

    /**
     * Sets the Interpolate flag, true for high-quality image scaling.
     */
    void setInterpolate(boolean value);
}
