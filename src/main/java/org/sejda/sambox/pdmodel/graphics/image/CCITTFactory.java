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

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import javax.imageio.stream.MemoryCacheImageOutputStream;

import org.sejda.commons.FastByteArrayOutputStream;
import org.sejda.io.SeekableSource;
import org.sejda.io.SeekableSources;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.filter.Filter;
import org.sejda.sambox.filter.FilterFactory;
import org.sejda.sambox.pdmodel.PDDocument;
import org.sejda.sambox.pdmodel.graphics.color.PDColorSpace;
import org.sejda.sambox.pdmodel.graphics.color.PDDeviceGray;

/**
 * Factory for creating a PDImageXObject containing a CCITT Fax compressed TIFF image.
 * 
 * @author Ben Litchfield
 * @author Paul King
 */
public final class CCITTFactory
{
    private CCITTFactory()
    {
    }

    /**
     * Creates a new CCITT group 4 (T6) compressed image XObject from a b/w BufferedImage. This compression technique
     * usually results in smaller images than those produced by
     * {@link LosslessFactory#createFromImage(PDDocument, BufferedImage) }.
     *
     * @param image the image.
     * @return a new image XObject.
     * @throws IOException if there is an error creating the image.
     * @throws IllegalArgumentException if the BufferedImage is not a b/w image.
     */
    public static PDImageXObject createFromImage(BufferedImage image) throws IOException
    {
        if (image.getType() != BufferedImage.TYPE_BYTE_BINARY
                && image.getColorModel().getPixelSize() != 1)
        {
            throw new IllegalArgumentException("Only 1-bit b/w images supported");
        }

        int height = image.getHeight();
        int width = image.getWidth();

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (MemoryCacheImageOutputStream mcios = new MemoryCacheImageOutputStream(bos))
        {

            for (int y = 0; y < height; ++y)
            {
                for (int x = 0; x < width; ++x)
                {
                    // flip bit to avoid having to set /BlackIs1
                    mcios.writeBits(~(image.getRGB(x, y) & 1), 1);
                }
                if (mcios.getBitOffset() != 0)
                {
                    mcios.writeBits(0, 8 - mcios.getBitOffset());
                }
            }
            mcios.flush();
        }

        return prepareImageXObject(bos.toByteArray(), width, height, PDDeviceGray.INSTANCE);
    }

    private static PDImageXObject prepareImageXObject(byte[] byteArray, int width, int height,
            PDColorSpace initColorSpace) throws IOException
    {
        FastByteArrayOutputStream baos = new FastByteArrayOutputStream();

        Filter filter = FilterFactory.INSTANCE.getFilter(COSName.CCITTFAX_DECODE);
        COSDictionary dict = new COSDictionary();
        dict.setInt(COSName.COLUMNS, width);
        dict.setInt(COSName.ROWS, height);
        filter.encode(new ByteArrayInputStream(byteArray), baos, dict);

        ByteArrayInputStream encodedByteStream = new ByteArrayInputStream(baos.toByteArray());
        PDImageXObject image = new PDImageXObject(encodedByteStream, COSName.CCITTFAX_DECODE, width,
                height, 1, initColorSpace);
        dict.setInt(COSName.K, -1);
        image.getCOSObject().setItem(COSName.DECODE_PARMS, dict);
        return image;
    }

    /**
     * Creates a new CCITT Fax compressed Image XObject from the first page of a TIFF file.
     *
     * @param file the TIFF file which contains a suitable CCITT compressed image
     * @return a new Image XObject
     * @throws IOException if there is an error reading the TIFF data.
     */
    public static PDImageXObject createFromFile(File file) throws IOException
    {
        return createFromFile(file, 0);
    }

    /**
     * Creates a new CCITT Fax compressed Image XObject from the first page of a TIFF file.
     *
     * @param file the TIFF file which contains a suitable CCITT compressed image
     * @param number TIFF image number, starting from 0
     * @return a new Image XObject
     * @throws IOException if there is an error reading the TIFF data.
     */
    public static PDImageXObject createFromFile(File file, int number) throws IOException
    {
        try (SeekableSource source = SeekableSources.seekableSourceFrom(file))
        {
            return createFromRandomAccessImpl(source, number);
        }
    }

    public static PDImageXObject createFromSeekableSource(SeekableSource source) throws IOException
    {
        return createFromRandomAccessImpl(source, 0);
    }

    public static PDImageXObject createFromSeekableSource(SeekableSource source, int number)
            throws IOException
    {
        return createFromRandomAccessImpl(source, number);
    }

    /**
     * Creates a new CCITT Fax compressed Image XObject from a TIFF file.
     * 
     * @param source the random access TIFF file which contains a suitable CCITT compressed image
     * @param number TIFF image number, starting from 0
     * @return a new Image XObject, or null if no such page
     * @throws IOException if there is an error reading the TIFF data.
     */
    private static PDImageXObject createFromRandomAccessImpl(SeekableSource source, int number)
            throws IOException
    {
        COSDictionary decodeParms = new COSDictionary();
        SeekableSource tiffView = extractFromTiff(source, decodeParms, number);
        if (tiffView != null)
        {
            PDImageXObject pdImage = new PDImageXObject(tiffView.asInputStream(),
                    COSName.CCITTFAX_DECODE, decodeParms.getInt(COSName.COLUMNS),
                    decodeParms.getInt(COSName.ROWS), 1, PDDeviceGray.INSTANCE);

            COSDictionary dict = pdImage.getCOSObject();
            dict.setItem(COSName.DECODE_PARMS, decodeParms);
            return pdImage;
        }
        return null;
    }

    // extracts the CCITT stream from the TIFF file
    private static SeekableSource extractFromTiff(SeekableSource source, COSDictionary params,
            int number) throws IOException
    {
        // First check the basic tiff header
        source.position(0);
        char endianess = (char) source.read();
        if ((char) source.read() != endianess)
        {
            throw new IOException("Not a valid tiff file");
        }
        // ensure that endianess is either M or I
        if (endianess != 'M' && endianess != 'I')
        {
            throw new IOException("Not a valid tiff file");
        }
        int magicNumber = readshort(endianess, source);
        if (magicNumber != 42)
        {
            throw new IOException("Not a valid tiff file");
        }

        // Relocate to the first set of tags
        int address = readlong(endianess, source);
        source.position(address);

        // If some higher page number is required, skip this page's tags,
        // then read the next page's address
        for (int i = 0; i < number; i++)
        {
            int numtags = readshort(endianess, source);
            if (numtags > 50)
            {
                throw new IOException("Not a valid tiff file");
            }
            source.position(address + 2 + numtags * 12);
            address = readlong(endianess, source);
            if (address == 0)
            {
                return null;
            }
            source.position(address);
        }

        int numtags = readshort(endianess, source);

        // The number 50 is somewhat arbitary, it just stops us load up junk from somewhere
        // and tramping on
        if (numtags > 50)
        {
            throw new IOException("Not a valid tiff file");
        }

        // Loop through the tags, some will convert to items in the parms dictionary
        // Other point us to where to find the data stream
        // The only parm which might change as a result of other options is K, so
        // We'll deal with that as a special;
        int k = -1000; // Default Non CCITT compression
        int dataoffset = 0;
        int datalength = 0;

        for (int i = 0; i < numtags; i++)
        {
            int tag = readshort(endianess, source);
            int type = readshort(endianess, source);
            int count = readlong(endianess, source);
            int val;

            // Note that when the type is shorter than 4 bytes, the rest can be garbage
            // and must be ignored. E.g. short (2 bytes) from "01 00 38 32" (little endian)
            // is 1, not 842530817 (seen in a real-life TIFF image).

            switch (type)
            {
            case 1: // byte value
                val = source.read();
                source.read();
                source.read();
                source.read();
                break;

            case 3: // short value
                val = readshort(endianess, source);
                source.read();
                source.read();
                break;

            default: // long and other types
                val = readlong(endianess, source);
                break;
            }
            switch (tag)
            {
            case 256:
            {
                params.setInt(COSName.COLUMNS, val);
                break;
            }
            case 257:
            {
                params.setInt(COSName.ROWS, val);
                break;
            }
            case 259:
            {
                if (val == 4)
                {
                    k = -1;
                }
                if (val == 3)
                {
                    k = 0;
                }
                break; // T6/T4 Compression
            }
            case 262:
            {
                if (val == 1)
                {
                    params.setBoolean(COSName.BLACK_IS_1, true);
                }
                break;
            }
            case 266:
            {
                if (val != 1)
                {
                    throw new UnsupportedTiffImageException(
                            "FillOrder " + val + " is not supported");
                }
                break;
            }
            case 273:
            {
                if (count == 1)
                {
                    dataoffset = val;
                }
                break;
            }
            case 274:
            {
                // http://www.awaresystems.be/imaging/tiff/tifftags/orientation.html
                if (val != 1)
                {
                    throw new UnsupportedTiffImageException(
                            "Orientation " + val + " is not supported");
                }
                break;
            }
            case 279:
            {
                if (count == 1)
                {
                    datalength = val;
                }
                break;
            }
            case 292:
            {
                if ((val & 1) != 0)
                {
                    k = 50; // T4 2D - arbitary positive K value
                }
                // http://www.awaresystems.be/imaging/tiff/tifftags/t4options.html
                if ((val & 4) != 0)
                {
                    throw new UnsupportedTiffImageException(
                            "CCITT Group 3 'uncompressed mode' is not supported");
                }
                if ((val & 2) != 0)
                {
                    throw new UnsupportedTiffImageException(
                            "CCITT Group 3 'fill bits before EOL' is not supported");
                }
                break;
            }
            case 324:
            {
                if (count == 1)
                {
                    dataoffset = val;
                }
                break;
            }
            case 325:
            {
                if (count == 1)
                {
                    datalength = val;
                }
                break;
            }
            default:
            {
                // do nothing
            }
            }
        }

        if (k == -1000)
        {
            throw new UnsupportedTiffImageException(
                    "First image in tiff is not CCITT T4 or T6 compressed");
        }
        if (dataoffset == 0)
        {
            throw new UnsupportedTiffImageException(
                    "First image in tiff is not a single tile/strip");
        }

        params.setInt(COSName.K, k);

        source.position(dataoffset);
        return source.view(dataoffset, datalength);

    }

    private static int readshort(char endianess, SeekableSource source) throws IOException
    {
        if (endianess == 'I')
        {
            return source.read() | (source.read() << 8);
        }
        return (source.read() << 8) | source.read();
    }

    private static int readlong(char endianess, SeekableSource source) throws IOException
    {
        if (endianess == 'I')
        {
            return source.read() | (source.read() << 8) | (source.read() << 16)
                    | (source.read() << 24);
        }
        return (source.read() << 24) | (source.read() << 16) | (source.read() << 8) | source.read();
    }
}
