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
package org.sejda.sambox.cos;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.sejda.io.SeekableSources.inMemorySeekableSourceFrom;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import org.sejda.commons.FastByteArrayOutputStream;
import org.sejda.commons.util.IOUtils;
import org.sejda.io.SeekableSource;
import org.sejda.io.SeekableSourceSupplier;
import org.sejda.sambox.filter.DecodeResult;
import org.sejda.sambox.filter.Filter;
import org.sejda.sambox.filter.FilterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class represents a stream object in a PDF document.
 *
 * @author Ben Litchfield
 */
public class COSStream extends COSDictionary implements Closeable, Encryptable
{
    private static final List<COSName> CAN_COMPRESS = Arrays.asList(COSName.ASCII_HEX_DECODE,
            COSName.ASCII_HEX_DECODE_ABBREVIATION, COSName.ASCII85_DECODE,
            COSName.ASCII85_DECODE_ABBREVIATION);

    private static final Logger LOG = LoggerFactory.getLogger(COSStream.class);

    private LazySeekableSourceViewHolder existing;
    private byte[] filtered;
    private byte[] unfiltered;
    private DecodeResult decodeResult;
    // an encryption function that returns an encrypted view of the filtered stream
    private Function<InputStream, InputStream> encryptor;
    private boolean encryptable = true;
    // if the writer should write the stream length as indirect object
    private boolean indirectLength = false;

    public COSStream()
    {
    }

    /**
     * @param dictionary The dictionary that is associated with this stream.
     */
    public COSStream(COSDictionary dictionary)
    {
        super(dictionary);
    }

    /**
     * Creates a stream with the given dictionary and where filtered data is a view of the given {@link SeekableSource}.
     * 
     * @param dictionary The dictionary that is associated with this stream.
     * @param seekableSource the source where filtered data is read from
     * @param startingPosition starting position of the stream data in the {@link SeekableSource}
     * @param length the length of the stream data
     */
    public COSStream(COSDictionary dictionary, SeekableSource seekableSource, long startingPosition,
            long length)
    {
        super(dictionary);
        this.existing = new LazySeekableSourceViewHolder(seekableSource, startingPosition, length);
    }

    /**
     * @return the (encoded) stream with all of the filters applied.
     * @throws IOException when encoding/decoding causes an exception
     */
    public final InputStream getFilteredStream() throws IOException
    {
        if (nonNull(encryptor))
        {
            return encryptor.apply(doGetFilteredStream());
        }
        return doGetFilteredStream();
    }

    protected InputStream doGetFilteredStream() throws IOException
    {
        if (nonNull(existing))
        {
            return existing.get().asInputStream();
        }
        encodeIfRequired();
        if (nonNull(filtered))
        {
            return new MyByteArrayInputStream(filtered);
        }
        return new MyByteArrayInputStream(unfiltered);
    }

    /**
     * @return the (encoded) {@link SeekableSource} with all of the filters applied.
     * @throws IOException when encoding/decoding causes an exception
     */
    public SeekableSource getFilteredSource() throws IOException
    {
        if (existing != null)
        {
            return existing.get();
        }
        return inMemorySeekableSourceFrom(getFilteredStream());
    }

    /**
     * @return the length of the encoded stream as long
     * @throws IOException
     */
    public long getFilteredLength() throws IOException
    {
        if (existing != null)
        {
            return existing.length;
        }
        encodeIfRequired();
        if (nonNull(filtered))
        {
            return filtered.length;
        }
        return ofNullable(unfiltered).map(f -> f.length).orElse(0);
    }

    private void encodeIfRequired() throws IOException
    {
        if (getFilters() != null)
        {
            if (filtered == null && unfiltered != null)
            {
                doEncode();
            }
        }
    }

    /**
     * @return the (decoded) stream with all of the filters applied.
     * @throws IOException when encoding/decoding causes an exception
     */
    public InputStream getUnfilteredStream() throws IOException
    {
        decodeIfRequired();
        if (unfiltered != null)
        {
            return new MyByteArrayInputStream(unfiltered);
        }
        return getStreamToDecode();
    }

    /**
     * @return the (decoded) {@link SeekableSource} with all of the filters applied.
     * @throws IOException when encoding/decoding causes an exception
     */
    public SeekableSource getUnfilteredSource() throws IOException
    {
        decodeIfRequired();
        if (unfiltered != null)
        {
            return inMemorySeekableSourceFrom(unfiltered);
        }
        if (existing != null)
        {
            return existing.get();
        }
        return inMemorySeekableSourceFrom(filtered);
    }

    /**
     * @return the (decoded) stream in the form of a read only {@link ByteBuffer} with all of the filters applied.
     * @throws IOException when encoding/decoding causes an exception
     */
    public ByteBuffer getUnfilteredByteBuffer() throws IOException
    {
        decodeIfRequired();
        if (unfiltered != null)
        {
            return ByteBuffer.wrap(unfiltered).asReadOnlyBuffer();
        }
        if (existing != null)
        {
            return ByteBuffer.wrap(IOUtils.toByteArray(existing.get().asInputStream()));
        }
        return ByteBuffer.wrap(filtered).asReadOnlyBuffer();
    }

    /**
     * @return the length of the decoded stream as long
     * @throws IOException
     */
    public long getUnfilteredLength() throws IOException
    {
        decodeIfRequired();
        if (nonNull(unfiltered))
        {
            return unfiltered.length;
        }
        if (nonNull(existing))
        {
            return existing.length;
        }
        return ofNullable(filtered).map(f -> f.length).orElse(0);
    }

    private void decodeIfRequired() throws IOException
    {
        if (nonNull(getFilters()) && isNull(unfiltered))
        {
            doDecode();
        }
    }

    /**
     * @return the repaired stream parameters dictionary
     * @throws IOException when encoding/decoding causes an exception
     */
    public DecodeResult getDecodeResult() throws IOException
    {
        decodeIfRequired();

        return Optional.ofNullable(decodeResult).orElse(DecodeResult.DEFAULT);
    }

    @Override
    public void accept(COSVisitor visitor) throws IOException
    {
        visitor.visit(this);
    }

    /**
     * This will decode the physical byte stream applying all of the filters to the stream.
     *
     * @throws IOException If there is an error applying a filter to the stream.
     */
    private void doDecode() throws IOException
    {
        COSBase filters = getFilters();
        if (nonNull(filters))
        {
            if (filters instanceof COSName)
            {
                unfiltered = decode((COSName) filters, 0, getStreamToDecode());
            }
            else if (filters instanceof COSArray)
            {
                unfiltered = decodeChain((COSArray) filters, getStreamToDecode());
            }
            else
            {
                LOG.warn("Unknown filter type:" + filters);
            }
        }
    }

    private InputStream getStreamToDecode() throws IOException
    {
        if (existing != null)
        {
            return existing.get().asInputStream();
        }
        return new MyByteArrayInputStream(filtered);
    }

    private byte[] decodeChain(COSArray filters, InputStream startingFrom) throws IOException
    {
        if (filters.size() > 0)
        {
            byte[] tmpResult = new byte[0];
            InputStream input = startingFrom;
            for (int i = 0; i < filters.size(); i++)
            {
                COSName filterName = (COSName) filters.getObject(i);
                tmpResult = decode(filterName, i, input);
                input = new MyByteArrayInputStream(tmpResult);
            }
            return tmpResult;
        }
        return null;
    }

    private byte[] decode(COSName filterName, int filterIndex, InputStream toDecode)
            throws IOException
    {
        if (toDecode.available() > 0)
        {
            Filter filter = FilterFactory.INSTANCE.getFilter(filterName);
            try (MyByteArrayOutputStream out = new MyByteArrayOutputStream())
            {
                decodeResult = filter.decode(toDecode, out, this, filterIndex);
                return out.toByteArray();
            }
        }
        return new byte[0];

    }

    /**
     * This will encode the logical byte stream applying all of the filters to the stream.
     *
     * @throws IOException If there is an error applying a filter to the stream.
     */
    private void doEncode() throws IOException
    {
        COSBase filters = getFilters();
        if (filters instanceof COSName)
        {
            filtered = encode((COSName) filters, new MyByteArrayInputStream(unfiltered));
        }
        else if (filters instanceof COSArray)
        {
            filtered = encodeChain((COSArray) filters, new MyByteArrayInputStream(unfiltered));
        }
    }

    private byte[] encode(COSName filterName, InputStream toEncode) throws IOException
    {
        Filter filter = FilterFactory.INSTANCE.getFilter(filterName);
        try (MyByteArrayOutputStream encoded = new MyByteArrayOutputStream())
        {
            filter.encode(toEncode, encoded, this);
            return encoded.toByteArray();
        }
    }

    private byte[] encodeChain(COSArray filters, InputStream startingFrom) throws IOException
    {
        if (filters.size() > 0)
        {
            byte[] tmpResult = new byte[0];
            InputStream input = startingFrom;
            for (int i = filters.size() - 1; i >= 0; i--)
            {
                COSName filterName = (COSName) filters.getObject(i);
                tmpResult = encode(filterName, input);
                input = new MyByteArrayInputStream(tmpResult);
            }
            return tmpResult;
        }
        return null;
    }

    /**
     * This will return the filters to apply to the byte stream. The method will return - null if no filters are to be
     * applied - a COSName if one filter is to be applied - a COSArray containing COSNames if multiple filters are to be
     * applied
     *
     * @return the COSBase object representing the filters
     */
    public COSBase getFilters()
    {
        return getDictionaryObject(COSName.FILTER);
    }

    /**
     * @param filter
     * @return true if the stream has the given filter in the filters list
     */
    public boolean hasFilter(COSName filter)
    {
        COSBase filters = getFilters();
        if (filters instanceof COSName)
        {
            return filters.equals(filter);
        }
        else if (filters instanceof COSArray)
        {
            return ((COSArray) filters).contains(filter);
        }
        return false;
    }

    /**
     * Sets the function to be used to encrypt this stream.
     * 
     * @param encryptor
     */
    public void setEncryptor(Function<InputStream, InputStream> encryptor)
    {
        this.encryptor = encryptor;
    }

    /**
     * Creates a new stream for which filtered byte should be written to. You probably don't want this but want to use
     * the createUnfilteredStream, which is used to write raw bytes to.
     *
     * @return A stream that can be written to.
     */
    public OutputStream createFilteredStream()
    {
        IOUtils.closeQuietly(existing);
        unfiltered = null;
        existing = null;
        filtered = null;
        return new MyByteArrayOutputStream(bytes -> {
            this.filtered = bytes;
        });
    }

    /**
     * Returns a new OutputStream for writing stream data, using and the given filters.
     * 
     * @param filters COSArray or COSName of filters to be used.
     * @return OutputStream for un-encoded stream data.
     * @throws IOException If the output stream could not be created.
     */
    public OutputStream createFilteredStream(COSBase filters)
    {
        if (filters != null)
        {
            setItem(COSName.FILTER, filters);
        }
        return createUnfilteredStream();
    }

    /**
     * set the filters to be applied to the stream.
     *
     * @param filters The filters to set on this stream.
     * @throws IOException If there is an error clearing the old filters.
     */
    public void setFilters(COSBase filters) throws IOException
    {
        if (unfiltered == null)
        {
            try (InputStream in = getUnfilteredStream())
            {
                try (MyByteArrayOutputStream out = new MyByteArrayOutputStream(bytes -> {
                    this.unfiltered = bytes;
                }))
                {
                    IOUtils.copy(in, out);
                }

            }

        }
        setItem(COSName.FILTER, filters);
        IOUtils.closeQuietly(existing);
        existing = null;
        filtered = null;
    }

    /**
     * Adds Flate decode filter to the current filters list if possible
     * 
     * @true if the FlateDecode filter has been added
     */
    public boolean addCompression()
    {
        if (canCompress())
        {
            try
            {
                COSArray newFilters = new COSArray(COSName.FLATE_DECODE);
                COSBase filters = getFilters();
                if (filters instanceof COSName)
                {
                    newFilters.add(filters);
                    setFilters(newFilters);
                }
                else if (filters instanceof COSArray)
                {
                    newFilters.addAll((COSArray) filters);
                    setFilters(newFilters);
                }
                else
                {
                    setFilters(COSName.FLATE_DECODE);
                }
                return true;
            }
            catch (IOException e)
            {
                LOG.warn("Unable to add FlateDecode filter to the stream", e);
            }
        }
        return false;
    }

    /**
     * @return true if we can add compression to the current filters
     */
    private boolean canCompress()
    {
        if (getDictionaryObject(COSName.DECODE_PARMS, COSName.DP) != null)
        {
            // we currently don't compress if there's a filter with params.
            return false;
        }
        COSBase filters = getFilters();
        if (filters instanceof COSName)
        {
            return CAN_COMPRESS.contains(filters);
        }
        if (filters instanceof COSArray)
        {
            return ((COSArray) filters).stream().allMatch(CAN_COMPRESS::contains);

        }
        return true;
    }

    @Override
    public boolean encryptable()
    {
        return encryptable;
    }

    @Override
    public void encryptable(boolean encryptable)
    {
        this.encryptable = encryptable;
    }

    /**
     * This will create an output stream that can be written to.
     *
     * @return An output stream which raw data bytes should be written to.
     *
     * @throws IOException If there is an error creating the stream.
     */
    public OutputStream createUnfilteredStream()
    {
        filtered = null;
        IOUtils.closeQuietly(existing);
        existing = null;
        unfiltered = null;
        return new MyByteArrayOutputStream(bytes -> {
            this.unfiltered = bytes;
        });
    }

    public boolean isEmpty() throws IOException
    {
        if (nonNull(existing))
        {
            return existing.get().size() <= 0;
        }
        return ofNullable(filtered).map(f -> (f.length <= 0)).orElseGet(() -> {
            return ofNullable(unfiltered).map(u -> (u.length <= 0)).orElse(true);
        });
    }

    /**
     * @return the contents of the stream as a text string. Text string as defined in Chap 7.9 of PDF 32000-1:2008.
     */
    public String asTextString()
    {
        try
        {
            return COSString.newInstance(IOUtils.toByteArray(getUnfilteredStream())).getString();
        }
        catch (IOException e)
        {
            LOG.warn("Unable to convert the COSStream to a text string", e);
            return "";
        }
    }

    @Override
    public void close() throws IOException
    {
        IOUtils.closeQuietly(existing);
        existing = null;
        unfiltered = null;
        filtered = null;
    }

    /**
     * offload decoded/unfiltered data leaving the COSStrem in its filtered state and reducing memory footprint
     */
    public void unDecode()
    {
        if (nonNull(existing))
        {
            unfiltered = null;
            filtered = null;
        }
        if (nonNull(filtered))
        {
            unfiltered = null;
        }
    }

    /**
     * @return true if the writer should write this stream length as indirect
     */
    public boolean indirectLength()
    {
        return indirectLength;
    }

    /**
     * @param indirectLength if the writer should write this stream length as indirect
     */
    public void indirectLength(boolean indirectLength)
    {
        this.indirectLength = indirectLength;
    }

    static class MyByteArrayOutputStream extends FastByteArrayOutputStream
    {
        private Optional<Consumer<byte[]>> onClose;

        MyByteArrayOutputStream()
        {
            this(null);
        }

        MyByteArrayOutputStream(Consumer<byte[]> onClose)
        {
            super();
            this.onClose = Optional.ofNullable(onClose);
        }

        @Override
        public void close()
        {
            super.close();
            onClose.ifPresent(c -> c.accept(toByteArray()));
        }
    }

    static class MyByteArrayInputStream extends ByteArrayInputStream
    {
        MyByteArrayInputStream(byte[] bytes)
        {
            super(Optional.ofNullable(bytes).orElse(new byte[0]));
        }
    }

    /**
     * Holder for a view of a portion of the given {@link SeekableSource}
     * 
     * @author Andrea Vacondio
     */
    private static class LazySeekableSourceViewHolder implements Closeable
    {
        private WeakReference<SeekableSource> sourceRef;
        private long length;
        private SeekableSourceSupplier<SeekableSource> supplier;
        private SeekableSource view;

        public LazySeekableSourceViewHolder(SeekableSource source, long startingPosition,
                long length)
        {
            this.supplier = () -> {
                return Optional.ofNullable(this.sourceRef.get()).filter(SeekableSource::isOpen)
                        .orElseThrow(() -> new IllegalStateException(
                                "The original SeekableSource has been closed"))
                        .view(startingPosition, length);
            };
            this.sourceRef = new WeakReference<>(source);
            this.length = length;
        }

        SeekableSource get() throws IOException
        {
            if (view == null)
            {
                view = supplier.get();
            }
            view.position(0);
            return view;
        }

        @Override
        public void close() throws IOException
        {
            IOUtils.close(view);
            view = null;
        }
    }
}
