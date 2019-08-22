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
package org.sejda.sambox.pdmodel.common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.sejda.commons.util.IOUtils;
import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSArrayList;
import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSNull;
import org.sejda.sambox.cos.COSObjectable;
import org.sejda.sambox.cos.COSStream;
import org.sejda.sambox.pdmodel.common.filespecification.FileSpecifications;
import org.sejda.sambox.pdmodel.common.filespecification.PDFileSpecification;

/**
 * A PDStream represents a stream in a PDF document. Streams are tied to a single PDF document.
 * 
 * @author Ben Litchfield
 */
public class PDStream implements COSObjectable
{
    private COSStream stream;

    /**
     * This will create a new PDStream object.
     */
    public PDStream()
    {
        stream = new COSStream();
    }

    /**
     * Constructor.
     * 
     * @param str The stream parameter.
     */
    public PDStream(COSStream str)
    {
        stream = str;
    }

    /**
     * Constructor. Reads all data from the input stream and embeds it into the document, this will close the
     * InputStream.
     * 
     * @param input The stream parameter.
     * @throws IOException If there is an error creating the stream in the document.
     */
    public PDStream(InputStream input) throws IOException
    {
        this(input, (COSBase) null);
    }

    /**
     * Constructor. Reads all data from the input stream and embeds it into the document with the given filter applied.
     * This method closes the InputStream.
     *
     * @param input The stream parameter.
     * @param filter Filter to apply to the stream.
     * @throws IOException If there is an error creating the stream in the document.
     */
    public PDStream(InputStream input, COSName filter) throws IOException
    {
        this(input, (COSBase) filter);
    }

    /**
     * Constructor. Reads all data from the input stream and embeds it into the document with the given filters applied.
     * This method closes the InputStream.
     *
     * @param input The stream parameter.
     * @param filters Filters to apply to the stream.
     * @throws IOException If there is an error creating the stream in the document.
     */
    public PDStream(InputStream input, COSArray filters) throws IOException
    {
        this(input, (COSBase) filters);
    }

    /**
     * Constructor. Reads all data from the input stream and embeds it into the document, this will close the
     * InputStream.
     * 
     * @param input The stream parameter.
     * @param filter Filter to apply to the stream.
     * @throws IOException If there is an error creating the stream in the document.
     */
    public PDStream(InputStream input, COSBase filter) throws IOException
    {
        stream = new COSStream();
        try (OutputStream output = stream.createFilteredStream(filter))
        {
            IOUtils.copy(input, output);
        }
        finally
        {
            IOUtils.close(input);
        }
    }

    /**
     * Convert this standard java object to a COS object.
     * 
     * @return The cos object that matches this Java object.
     */
    @Override
    public COSStream getCOSObject()
    {
        return stream;
    }

    /**
     * This will get a stream that can be written to.
     * 
     * @return An output stream to write data to.
     * 
     * @throws IOException If an IO error occurs during writing.
     */
    public OutputStream createOutputStream()
    {
        return stream.createUnfilteredStream();
    }

    /**
     * This will get a stream that can be written to, with the given filter.
     *
     * @param filter the filter to be used.
     * @return An output stream to write data to.
     * @throws IOException If an IO error occurs during writing.
     */
    public OutputStream createOutputStream(COSName filter)
    {
        return stream.createFilteredStream(filter);
    }

    /**
     * This will get a stream that can be read from.
     * 
     * @return An input stream that can be read from.
     * 
     * @throws IOException If an IO error occurs during reading.
     */
    public InputStream createInputStream() throws IOException
    {
        return stream.getUnfilteredStream();
    }

    /**
     * This will get the length of the filtered/compressed stream. This is readonly in the PD Model and will be managed
     * by this class.
     * 
     * @return The length of the filtered stream.
     */
    public int getLength()
    {
        return stream.getInt(COSName.LENGTH, 0);
    }

    /**
     * This will get the list of filters that are associated with this stream. Or null if there are none.
     * 
     * @return A list of all encoding filters to apply to this stream.
     */
    public List<COSName> getFilters()
    {
        COSBase filters = stream.getFilters();
        if (filters instanceof COSName)
        {
            COSName name = (COSName) filters;
            return new COSArrayList<>(name, name, stream, COSName.FILTER);
        }
        else if (filters instanceof COSArray)
        {
            return (List<COSName>) ((COSArray) filters).toList();
        }
        return null;
    }

    /**
     * This will set the filters that are part of this stream.
     * 
     * @param filters The filters that are part of this stream.
     */
    public void setFilters(List<COSName> filters)
    {
        COSBase obj = COSArrayList.converterToCOSArray(filters);
        stream.setItem(COSName.FILTER, obj);
    }

    /**
     * Get the list of decode parameters. Each entry in the list will refer to an entry in the filters list.
     * 
     * @return The list of decode parameters.
     * 
     * @throws IOException if there is an error retrieving the parameters.
     */
    public List<Object> getDecodeParms() throws IOException
    {
        List<Object> retval = null;

        COSBase dp = stream.getDictionaryObject(COSName.DECODE_PARMS);
        if (dp == null)
        {
            // See PDF Ref 1.5 implementation note 7, the DP is sometimes used
            // instead.
            dp = stream.getDictionaryObject(COSName.DP);
        }
        if (dp instanceof COSDictionary)
        {
            Map<?, ?> map = COSDictionaryMap.convertBasicTypesToMap((COSDictionary) dp);
            retval = new COSArrayList<>(map, dp, stream, COSName.DECODE_PARMS);
        }
        else if (dp instanceof COSArray)
        {
            COSArray array = (COSArray) dp;
            List<Object> actuals = new ArrayList<>();
            for (int i = 0; i < array.size(); i++)
            {
                actuals.add(COSDictionaryMap
                        .convertBasicTypesToMap((COSDictionary) array.getObject(i)));
            }
            retval = new COSArrayList<>(actuals, array);
        }

        return retval;
    }

    /**
     * This will set the list of decode parameterss.
     * 
     * @param decodeParams The list of decode parameterss.
     */
    public void setDecodeParms(List<?> decodeParams)
    {
        stream.setItem(COSName.DECODE_PARMS, COSArrayList.converterToCOSArray(decodeParams));
    }

    /**
     * This will get the file specification for this stream. This is only required for external files.
     * 
     * @return The file specification.
     */
    public PDFileSpecification getFile()
    {
        return FileSpecifications.fileSpecificationFor(stream.getDictionaryObject(COSName.F));
    }

    /**
     * Set the file specification.
     * 
     * @param f The file specification.
     */
    public void setFile(PDFileSpecification f)
    {
        stream.setItem(COSName.F, f);
    }

    /**
     * This will get the list of filters that are associated with this stream. Or null if there are none.
     * 
     * @return A list of all encoding filters to apply to this stream.
     */
    public List<String> getFileFilters()
    {
        List<String> retval = null;
        COSBase filters = stream.getDictionaryObject(COSName.F_FILTER);
        if (filters instanceof COSName)
        {
            COSName name = (COSName) filters;
            retval = new COSArrayList<>(name.getName(), name, stream, COSName.F_FILTER);
        }
        else if (filters instanceof COSArray)
        {
            retval = COSArrayList.convertCOSNameCOSArrayToList((COSArray) filters);
        }
        return retval;
    }

    /**
     * This will set the filters that are part of this stream.
     * 
     * @param filters The filters that are part of this stream.
     */
    public void setFileFilters(List<String> filters)
    {
        COSBase obj = COSArrayList.convertStringListToCOSNameCOSArray(filters);
        stream.setItem(COSName.F_FILTER, obj);
    }

    /**
     * Get the list of decode parameters. Each entry in the list will refer to an entry in the filters list.
     * 
     * @return The list of decode parameters.
     * 
     * @throws IOException if there is an error retrieving the parameters.
     */
    public List<Object> getFileDecodeParams() throws IOException
    {
        List<Object> retval = null;

        COSBase dp = stream.getDictionaryObject(COSName.F_DECODE_PARMS);
        if (dp instanceof COSDictionary)
        {
            Map<?, ?> map = COSDictionaryMap.convertBasicTypesToMap((COSDictionary) dp);
            retval = new COSArrayList<>(map, dp, stream, COSName.F_DECODE_PARMS);
        }
        else if (dp instanceof COSArray)
        {
            COSArray array = (COSArray) dp;
            List<Object> actuals = new ArrayList<>();
            for (int i = 0; i < array.size(); i++)
            {
                actuals.add(COSDictionaryMap
                        .convertBasicTypesToMap((COSDictionary) array.getObject(i)));
            }
            retval = new COSArrayList<>(actuals, array);
        }

        return retval;
    }

    /**
     * This will set the list of decode params.
     * 
     * @param decodeParams The list of decode params.
     */
    public void setFileDecodeParams(List<?> decodeParams)
    {
        stream.setItem("FDecodeParams", COSArrayList.converterToCOSArray(decodeParams));
    }

    /**
     * This will copy the stream into a byte array.
     * 
     * @return The byte array of the filteredStream
     * @throws IOException When getFilteredStream did not work
     */
    public byte[] toByteArray() throws IOException
    {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (InputStream is = createInputStream())
        {
            IOUtils.copy(is, output);
        }
        return output.toByteArray();
    }

    /**
     * Get the metadata that is part of the document catalog. This will return null if there is no meta data for this
     * object.
     * 
     * @return The metadata for this object.
     * @throws IllegalStateException if the value of the metadata entry is different from a stream or null
     */
    public PDMetadata getMetadata()
    {
        PDMetadata retval = null;
        COSBase mdStream = stream.getDictionaryObject(COSName.METADATA);
        if (mdStream != null)
        {
            if (mdStream instanceof COSStream)
            {
                retval = new PDMetadata((COSStream) mdStream);
            }
            else if (mdStream instanceof COSNull)
            {
                // null is authorized
            }
            else
            {
                throw new IllegalStateException(
                        "Expected a COSStream but was a " + mdStream.getClass().getSimpleName());
            }
        }
        return retval;
    }

    /**
     * Set the metadata for this object. This can be null.
     * 
     * @param meta The meta data for this object.
     */
    public void setMetadata(PDMetadata meta)
    {
        stream.setItem(COSName.METADATA, meta);
    }

    /**
     * Get the decoded stream length.
     * 
     * @since Apache PDFBox 1.1.0
     * @see <a href="https://issues.apache.org/jira/browse/PDFBOX-636">PDFBOX-636</a>
     * @return the decoded stream length
     */
    public int getDecodedStreamLength()
    {
        return this.stream.getInt(COSName.DL);
    }

    /**
     * Set the decoded stream length.
     * 
     * @since Apache PDFBox 1.1.0
     * @see <a href="https://issues.apache.org/jira/browse/PDFBOX-636">PDFBOX-636</a>
     * @param decodedStreamLength the decoded stream length
     */
    public void setDecodedStreamLength(int decodedStreamLength)
    {
        this.stream.setInt(COSName.DL, decodedStreamLength);
    }

}
