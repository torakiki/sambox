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
package org.sejda.sambox.output;

import static java.util.Optional.ofNullable;
import static org.sejda.sambox.util.CharUtils.ASCII_SPACE;
import static org.sejda.util.RequireUtils.requireNotNullArg;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.DeflaterInputStream;

import org.sejda.io.CountingWritableByteChannel;
import org.sejda.sambox.SAMBox;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSNull;
import org.sejda.sambox.cos.COSStream;
import org.sejda.sambox.cos.DisposableCOSObject;
import org.sejda.sambox.cos.IndirectCOSObjectReference;
import org.sejda.sambox.cos.NonStorableInObjectStreams;
import org.sejda.sambox.xref.CompressedXrefEntry;
import org.sejda.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link AbstractPDFBodyWriter} implementation where objects are written to an Object Stream and later the stream is
 * written as COSStream using the wrapped {@link AbstractPDFBodyWriter}
 * 
 * @author Andrea Vacondio
 *
 */
class ObjectsStreamPDFBodyWriter extends AbstractPDFBodyWriter
{
    private static final Logger LOG = LoggerFactory.getLogger(ObjectsStreamPDFBodyWriter.class);

    private AbstractPDFBodyWriter wrapped;
    private ObjectsStream currentStream;

    public ObjectsStreamPDFBodyWriter(AbstractPDFBodyWriter wrapped)
    {
        super(ofNullable(wrapped).map(AbstractPDFBodyWriter::context)
                .orElseThrow(() -> new IllegalArgumentException("Wrapped writer cannot be null")));
        requireNotNullArg(wrapped, "Wrapped writer cannot be null");
        this.wrapped = wrapped;
        currentStream = new ObjectsStream(context());
        context().createIndirectReferenceFor(currentStream);
    }

    @Override
    void writeObject(IndirectCOSObjectReference ref) throws IOException
    {
        if (ref instanceof NonStorableInObjectStreams
                || ref.getCOSObject().getCOSObject() instanceof COSStream)
        {
            wrapped.writeObject(ref);
        }
        else
        {
            IndirectCOSObjectReference streamRef = context().getIndirectReferenceFor(currentStream);
            context().putWritten(
                    CompressedXrefEntry.compressedEntry(ref.xrefEntry().getObjectNumber(),
                            streamRef.xrefEntry().getObjectNumber(), currentStream.counter));
            currentStream.addItem(ref);
            LOG.trace("Added ref {} to object stream {}", ref, streamRef);
        }
        if (currentStream.isFull())
        {
            doWriteObjectsStream();
            currentStream = new ObjectsStream(context());
            context().createIndirectReferenceFor(currentStream);
        }
    }

    private void doWriteObjectsStream() throws IOException
    {
        IndirectCOSObjectReference ref = context().getIndirectReferenceFor(currentStream);
        LOG.debug("Writing object stream {}", ref);
        currentStream.prepareForWriting();
        IndirectCOSObjectReference length = context()
                .createNonStorableInObjectStreamIndirectReferenceFor(COSNull.NULL);
        currentStream.setItem(COSName.LENGTH, length);
        wrapped.writeObject(ref);
        LOG.trace("Writing object stream length {}", length);
        wrapped.writeObject(length);
    }

    @Override
    void onCompletion() throws IOException
    {
        if (currentStream.hasItems())
        {
            doWriteObjectsStream();
        }
        // complete writing
        wrapped.onCompletion();
    }

    @Override
    public void close() throws IOException
    {
        IOUtils.close(wrapped);
        super.close();
        currentStream = null;
    }

    static class ObjectsStream extends COSStream implements DisposableCOSObject
    {
        private int counter;
        private ByteArrayOutputStream header = new ByteArrayOutputStream();
        private ByteArrayOutputStream data = new ByteArrayOutputStream();
        private DefaultCOSWriter dataWriter;
        private InputStream filtered;

        public ObjectsStream(PDFWriteContext context)
        {
            setName(COSName.TYPE, COSName.OBJ_STM.getName());
            dataWriter = new IndirectReferencesAwareCOSWriter(
                    CountingWritableByteChannel.from(data), context)
            {
                @Override
                public void writeComplexObjectSeparator()
                {
                    // nothing
                }

                @Override
                public void writeDictionaryItemsSeparator()
                {
                    // nothing
                }
            };
        }

        public boolean hasItems()
        {
            return counter > 0;
        }

        void addItem(IndirectCOSObjectReference ref) throws IOException
        {
            this.counter++;
            header.write(Long.toUnsignedString(ref.xrefEntry().getObjectNumber())
                    .getBytes(StandardCharsets.US_ASCII));
            header.write(ASCII_SPACE);
            header.write(Long.toUnsignedString(dataWriter.writer().offset())
                    .getBytes(StandardCharsets.US_ASCII));
            header.write(ASCII_SPACE);
            ref.getCOSObject().accept(dataWriter);
            dataWriter.writer().write(ASCII_SPACE);
            ref.releaseCOSObject();
        }

        boolean isFull()
        {
            return counter >= Integer.getInteger(SAMBox.OBJECTS_STREAM_SIZE_PROPERTY, 100);
        }

        @Override
        public InputStream doGetFilteredStream()
        {
            return this.filtered;
        }

        void prepareForWriting()
        {
            IOUtils.closeQuietly(dataWriter);
            setInt(COSName.N, counter);
            setInt(COSName.FIRST, header.size());
            setItem(COSName.FILTER, COSName.FLATE_DECODE);
            this.filtered = new DeflaterInputStream(
                    new SequenceInputStream(new ByteArrayInputStream(header.toByteArray()),
                            new ByteArrayInputStream(data.toByteArray())));
            this.header = null;
            this.data = null;
        }

        @Override
        public void close()
        {
            IOUtils.closeQuietly(filtered);
            super.close();
        }

        @Override
        public void releaseCOSObject()
        {
            this.filtered = null;
        }
    }
}
