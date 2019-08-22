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
package org.sejda.sambox.filter;

import static java.util.Objects.nonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Optional;
import java.util.zip.Deflater;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;

import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A filter for stream data.
 *
 * @author Ben Litchfield
 * @author John Hewson
 */
public abstract class Filter
{
    private static final Logger LOG = LoggerFactory.getLogger(Filter.class);
    /**
     * Compression Level System Property. Set this to a value from 0 to 9 to change the zlib deflate compression level
     * used to compress /Flate streams. The default value is -1 which is {@link Deflater#DEFAULT_COMPRESSION}. To set
     * maximum compression, use {@code System.setProperty(Filter.SYSPROP_DEFLATELEVEL, "9");}
     */
    public static final String SYSPROP_DEFLATELEVEL = "org.sejda.sambox.filter.deflatelevel";

    protected Filter()
    {
    }

    /**
     * Decodes data, producing the original non-encoded data.
     * 
     * @param encoded the encoded byte stream
     * @param decoded the stream where decoded data will be written
     * @param parameters the parameters used for decoding
     * @param index the index to the filter being decoded
     * @return repaired parameters dictionary, or the original parameters dictionary
     * @throws IOException if the stream cannot be decoded
     */
    public abstract DecodeResult decode(InputStream encoded, OutputStream decoded,
            COSDictionary parameters, int index) throws IOException;

    /**
     * Encodes data.
     * 
     * @param input the byte stream to encode
     * @param encoded the stream where encoded data will be written
     * @param parameters the parameters used for encoding
     * @throws IOException if the stream cannot be encoded
     */
    public abstract void encode(InputStream input, OutputStream encoded, COSDictionary parameters)
            throws IOException;

    // gets the decode params for a specific filter index, this is used to
    // normalise the DecodeParams entry so that it is always a dictionary
    protected COSDictionary getDecodeParams(COSDictionary dictionary, int index)
    {
        // AV: PDFBOX-3932 considers valid only the case name,dictionary and array,array but discards dp in the case
        // name,array of 1 where we previously picked the one element as dp
        COSBase filter = dictionary.getDictionaryObject(COSName.FILTER, COSName.F);
        COSBase dp = Optional
                .ofNullable(dictionary.getDictionaryObject(COSName.DECODE_PARMS, COSName.DP))
                .orElseGet(COSDictionary::new);
        if (filter instanceof COSName && dp instanceof COSDictionary)
        {
            // PDFBOX-3932: The PDF specification requires "If there is only one filter and that
            // filter has parameters, DecodeParms shall be set to the filter’s parameter dictionary"
            // but tests show that Adobe means "one filter name object".
            return (COSDictionary) dp;
        }
        if (filter instanceof COSArray && dp instanceof COSArray)
        {
            COSArray array = (COSArray) dp;
            if (index < array.size())
            {
                COSBase params = Optional.ofNullable(array.getObject(index))
                        .orElseGet(COSDictionary::new);
                if (params instanceof COSDictionary)
                {
                    return (COSDictionary) params;
                }
                LOG.error("Ignoring invalid DecodeParams. Expected dictionary but found {}",
                        params.getClass().getName());
                return new COSDictionary();
            }
        }
        if (!(filter instanceof COSArray || dp instanceof COSArray))
        {
            LOG.error("Ignoring invalid DecodeParams. Expected array or dictionary but found {}",
                    dp.getClass().getName());
        }
        return new COSDictionary();
    }

    /**
     * Finds a suitable image reader for a format.
     *
     * @param formatName The format to search for.
     * @param errorCause The probably cause if something goes wrong.
     * @return The image reader for the format.
     * @throws MissingImageReaderException if no image reader is found.
     */
    protected static ImageReader findImageReader(String formatName, String errorCause)
            throws MissingImageReaderException
    {
        Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName(formatName);
        ImageReader reader = null;
        while (readers.hasNext())
        {
            reader = readers.next();
            if (nonNull(reader) && reader.canReadRaster())
            {
                break;
            }
        }
        if (reader == null)
        {
            throw new MissingImageReaderException(
                    "Cannot read " + formatName + " image: " + errorCause);
        }
        return reader;
    }

    /**
     * @return the ZIP compression level configured for PDFBox
     */
    public static int getCompressionLevel()
    {
        int compressionLevel = Deflater.DEFAULT_COMPRESSION;
        try
        {
            compressionLevel = Integer
                    .parseInt(System.getProperty(Filter.SYSPROP_DEFLATELEVEL, "-1"));
        }
        catch (NumberFormatException ex)
        {
            LOG.warn(ex.getMessage(), ex);
        }
        return Math.max(-1, Math.min(Deflater.BEST_COMPRESSION, compressionLevel));
    }
}
