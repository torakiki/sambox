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

import static org.sejda.io.SeekableSources.seekableSourceFrom;

import java.io.File;
import java.io.IOException;

import org.sejda.io.SeekableSource;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
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
     * Creates a new CCITT Fax compressed Image XObject from the first page of a TIFF file.
     *
     * @param file the TIFF file which contains a suitable CCITT compressed image
     * @return a new Image XObject
     * @throws IOException if there is an error reading the TIFF data.
     */
    public static PDImageXObject createFromFile(File file) throws IOException
    {
        return createFromRandomAccessImpl(seekableSourceFrom(file), 0);
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
        return createFromRandomAccessImpl(seekableSourceFrom(file), number);
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
            int val = readlong(endianess, source); // See note

            // Note, we treated that value as a long. The value always occupies 4 bytes
            // But it might only use the first byte or two. Depending on endianess we might
            // need to correct.
            // Note we ignore all other types, they are of little interest for PDFs/CCITT Fax
            if (endianess == 'M')
            {
                switch (type)
                {
                case 1:
                {
                    val = val >> 24;
                    break; // byte value
                }
                case 3:
                {
                    val = val >> 16;
                    break; // short value
                }
                case 4:
                {
                    break; // long value
                }
                default:
                {
                    // do nothing
                }
                }
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
                    throw new IOException("FillOrder " + val + " is not supported");
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
                    throw new IOException("CCITT Group 3 'uncompressed mode' is not supported");
                }
                if ((val & 2) != 0)
                {
                    throw new IOException("CCITT Group 3 'fill bits before EOL' is not supported");
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
            throw new IOException("First image in tiff is not CCITT T4 or T6 compressed");
        }
        if (dataoffset == 0)
        {
            throw new IOException("First image in tiff is not a single tile/strip");
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
