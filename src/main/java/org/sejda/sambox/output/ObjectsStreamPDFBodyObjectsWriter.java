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

import static org.sejda.commons.util.RequireUtils.requireNotNullArg;
import static org.sejda.sambox.cos.DirectCOSObject.asDirectObject;
import static org.sejda.sambox.util.CharUtils.ASCII_SPACE;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.DeflaterInputStream;

import org.sejda.commons.FastByteArrayOutputStream;
import org.sejda.commons.util.IOUtils;
import org.sejda.io.CountingWritableByteChannel;
import org.sejda.sambox.SAMBox;
import org.sejda.sambox.cos.COSInteger;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSStream;
import org.sejda.sambox.cos.DisposableCOSObject;
import org.sejda.sambox.cos.IndirectCOSObjectReference;
import org.sejda.sambox.cos.NonStorableInObjectStreams;
import org.sejda.sambox.xref.CompressedXrefEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of a PDFBodyObjectsWriter where objects are written to an ObjectsStream and later the ObjectsStream is
 * written as COSStream using the delegate {@link PDFBodyObjectsWriter}
 * 
 * @author Andrea Vacondio
 */
public class ObjectsStreamPDFBodyObjectsWriter implements PDFBodyObjectsWriter
{
    private static final Logger LOG = LoggerFactory
            .getLogger(ObjectsStreamPDFBodyObjectsWriter.class);

    private PDFWriteContext context;
    private PDFBodyObjectsWriter delegate;
    private ObjectsStream currentStream;

    public ObjectsStreamPDFBodyObjectsWriter(PDFWriteContext context, PDFBodyObjectsWriter delegate)
    {
        requireNotNullArg(context, "Write context cannot be null");
        requireNotNullArg(delegate, "Delegate writer cannot be null");
        this.context = context;
        this.delegate = delegate;
        this.currentStream = new ObjectsStream(context);
    }

    @Override
    public void writeObject(IndirectCOSObjectReference ref) throws IOException
    {
        if (ref instanceof NonStorableInObjectStreams
                || ref.getCOSObject().getCOSObject() instanceof COSStream)
        {
            delegate.writeObject(ref);
        }
        else
        {
            context.addWritten(
                    CompressedXrefEntry.compressedEntry(ref.xrefEntry().getObjectNumber(),
                            currentStream.reference().xrefEntry().getObjectNumber(),
                            currentStream.counter));
            currentStream.addItem(ref);
            LOG.trace("Added ref {} to object stream {}", ref, currentStream.reference());
        }
        if (currentStream.isFull())
        {
            doWriteObjectsStream();
            currentStream = new ObjectsStream(context);
        }

    }

    private void doWriteObjectsStream() throws IOException
    {
        LOG.debug("Writing object stream {}", currentStream.reference());
        currentStream.prepareForWriting();
        IndirectCOSObjectReference length = context
                .createNonStorableInObjectStreamIndirectReference();
        currentStream.setItem(COSName.LENGTH, length);
        delegate.writeObject(currentStream.reference());
        LOG.trace("Writing object stream length {}", length);
        delegate.writeObject(length);
    }

    @Override
    public void onWriteCompletion() throws IOException
    {
        if (currentStream.hasItems())
        {
            doWriteObjectsStream();
        }
        // complete writing
        delegate.onWriteCompletion();
    }

    @Override
    public void close() throws IOException
    {
        IOUtils.close(delegate);
        currentStream = null;
    }

    static class ObjectsStream extends COSStream implements DisposableCOSObject
    {
        private int counter;
        private FastByteArrayOutputStream header = new FastByteArrayOutputStream();
        private FastByteArrayOutputStream data = new FastByteArrayOutputStream();
        private DefaultCOSWriter dataWriter;
        private InputStream filtered;
        private IndirectCOSObjectReference reference;

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
            this.reference = context.createIndirectReferenceFor(this);
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
            setItem(COSName.N, asDirectObject(COSInteger.get(counter)));
            setItem(COSName.FIRST, asDirectObject(COSInteger.get(header.size())));
            setItem(COSName.FILTER, asDirectObject(COSName.FLATE_DECODE));
            this.filtered = new DeflaterInputStream(
                    new SequenceInputStream(new ByteArrayInputStream(header.toByteArray()),
                            new ByteArrayInputStream(data.toByteArray())));
            this.header = null;
            this.data = null;
        }

        IndirectCOSObjectReference reference()
        {
            return this.reference;
        }

        @Override
        public void close() throws IOException
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
