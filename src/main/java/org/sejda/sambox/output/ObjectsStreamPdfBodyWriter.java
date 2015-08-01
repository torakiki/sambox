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

import static java.util.Objects.requireNonNull;
import static org.sejda.sambox.util.CharUtils.ASCII_SPACE;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.zip.DeflaterInputStream;

import org.sejda.io.CountingWritableByteChannel;
import org.sejda.sambox.SAMBox;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSNull;
import org.sejda.sambox.cos.COSStream;
import org.sejda.sambox.cos.DisposableCOSObject;
import org.sejda.sambox.cos.IndirectCOSObjectReference;
import org.sejda.sambox.util.Charsets;
import org.sejda.sambox.xref.CompressedXrefEntry;
import org.sejda.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link AbstractPdfBodyWriter} implementation where objects are written to an Object Stream and later the stream is
 * written as COSStream using the decorated {@link AbstractPdfBodyWriter}
 * 
 * @author Andrea Vacondio
 *
 */
class ObjectsStreamPdfBodyWriter extends AbstractPdfBodyWriter
{
    private static final Logger LOG = LoggerFactory.getLogger(ObjectsStreamPdfBodyWriter.class);

    private AbstractPdfBodyWriter wrapped;
    private ObjectsStream currentStream = new ObjectsStream();

    public ObjectsStreamPdfBodyWriter(AbstractPdfBodyWriter wrapped)
    {
        requireNonNull(wrapped);
        this.wrapped = wrapped;
    }

    @Override
    void writeObject(IndirectCOSObjectReference ref) throws IOException
    {
        if (ref.getCOSObject().getCOSObject() instanceof COSStream)
        {
            wrapped.writeObject(ref);
        }
        else
        {
            wrapped.writer().put(
                    CompressedXrefEntry.compressedEntry(ref.xrefEntry().getObjectNumber(),
                            currentStream.reference.xrefEntry().getObjectNumber(),
                            currentStream.counter));
            currentStream.addItem(ref);
            LOG.trace("Added ref {} to object stream {}", ref, currentStream.reference);
        }
        if (currentStream.isFull())
        {
            doWriteObjectsStream();
            currentStream = new ObjectsStream();
        }
    }

    private void doWriteObjectsStream() throws IOException
    {
        LOG.trace("Writing object stream {}", currentStream.reference);
        currentStream.prepareForWriting();
        IndirectCOSObjectReference length = nextReferenceFor(COSNull.NULL);
        currentStream.setItem(COSName.LENGTH, length);
        wrapped.writeObject(currentStream.reference);
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
    IndirectObjectsWriter writer()
    {
        return wrapped.writer();
    }

    @Override
    public void close()
    {
        super.close();
        currentStream = null;
    }

    private class ObjectsStream extends COSStream implements DisposableCOSObject
    {
        private int counter;
        private ByteArrayOutputStream header = new ByteArrayOutputStream();
        private ByteArrayOutputStream data = new ByteArrayOutputStream();
        private DefaultCOSWriter dataWriter = new DefaultCOSWriter(
                CountingWritableByteChannel.from(data));
        private InputStream filtered;
        private IndirectCOSObjectReference reference;

        public ObjectsStream()
        {
            setName(COSName.TYPE, COSName.OBJ_STM.getName());
            this.reference = nextReferenceFor(this);
        }

        public boolean hasItems()
        {
            return counter > 0;
        }

        void addItem(IndirectCOSObjectReference ref) throws IOException
        {
            this.counter++;
            header.write(Long.toUnsignedString(ref.xrefEntry().getObjectNumber()).getBytes(
                    Charsets.US_ASCII));
            header.write(ASCII_SPACE);
            header.write(Long.toUnsignedString(dataWriter.writer().offset()).getBytes(
                    Charsets.US_ASCII));
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
        public InputStream getFilteredStream()
        {
            return this.filtered;
        }

        public void prepareForWriting()
        {
            IOUtils.closeQuietly(dataWriter);
            setInt(COSName.N, counter);
            setInt(COSName.FIRST, header.size());
            setItem(COSName.FILTER, COSName.FLATE_DECODE);
            this.filtered = new DeflaterInputStream(new SequenceInputStream(
                    new ByteArrayInputStream(header.toByteArray()), new ByteArrayInputStream(
                            data.toByteArray())));
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
