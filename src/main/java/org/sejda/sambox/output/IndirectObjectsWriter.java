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
import static org.sejda.sambox.output.DefaultCOSWriter.SPACE;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.sejda.commons.util.IOUtils;
import org.sejda.io.BufferedCountingChannelWriter;
import org.sejda.io.CountingWritableByteChannel;
import org.sejda.sambox.cos.IndirectCOSObjectReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Component that writes pdf objects and keeps track of what has been written to avoid writing an object twice.
 * 
 * @author Andrea Vacondio
 */
class IndirectObjectsWriter implements Closeable
{
    private static final byte[] OBJ = "obj".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] ENDOBJ = "endobj".getBytes(StandardCharsets.US_ASCII);

    private static final Logger LOG = LoggerFactory.getLogger(IndirectObjectsWriter.class);

    private COSWriter writer;
    private PDFWriteContext context;

    IndirectObjectsWriter(CountingWritableByteChannel channel, PDFWriteContext context)
    {
        this(new BufferedCountingChannelWriter(channel), context);
    }

    IndirectObjectsWriter(BufferedCountingChannelWriter writer, PDFWriteContext context)
    {
        requireNotNullArg(writer, "Writer cannot be null");
        requireNotNullArg(context, "Write context cannot be null");
        this.writer = new EncryptingIndirectReferencesAwareCOSWriter(writer, context);
        this.context = context;
    }

    PDFWriteContext context()
    {
        return context;
    }

    /**
     * Writes the given {@link IndirectCOSObjectReference} updating its offset and releasing it once written. The object
     * is written only if not previously already written.
     * 
     * @param object
     * @throws IOException
     */
    public void writeObjectIfNotWritten(IndirectCOSObjectReference object) throws IOException
    {
        if (!context.hasWritten(object.xrefEntry()))
        {
            writeObject(object);
        }
    }

    /**
     * Writes the given {@link IndirectCOSObjectReference} updating its offset and releasing it once written.
     * 
     * @param object
     * @throws IOException
     */
    public void writeObject(IndirectCOSObjectReference object) throws IOException
    {
        context.writing(object.xrefEntry().key());
        doWriteObject(object);
        context.addWritten(object.xrefEntry());
        onWritten(object);
    }

    /**
     * Called when the input indirect references has just been written
     * 
     * @param object
     */
    protected void onWritten(IndirectCOSObjectReference ref)
    {
        ref.releaseCOSObject();
        LOG.trace("Released {}", ref);
    }

    private void doWriteObject(IndirectCOSObjectReference object) throws IOException
    {
        object.xrefEntry().setByteOffset(writer.writer().offset());
        writer.writer().write(Long.toString(object.xrefEntry().getObjectNumber()));
        writer.writer().write(SPACE);
        writer.writer().write(Integer.toString(object.xrefEntry().getGenerationNumber()));
        writer.writer().write(SPACE);
        writer.writer().write(OBJ);
        writer.writer().writeEOL();
        object.getCOSObject().accept(writer);
        writer.writer().writeEOL();
        writer.writer().write(ENDOBJ);
        writer.writer().writeEOL();
        LOG.trace("Written object {}", object.xrefEntry());
    }

    /**
     * @return the underlying {@link COSWriter}
     */
    COSWriter writer()
    {
        return writer;
    }

    /**
     * @see BufferedCountingChannelWriter#writeEOL()
     * @throws IOException
     */
    public void writeEOL() throws IOException
    {
        writer.writer().writeEOL();
    }

    /**
     * @see BufferedCountingChannelWriter#write(byte[])
     * @param bytes
     * @throws IOException
     */
    public void write(byte[] bytes) throws IOException
    {
        writer.writer().write(bytes);
    }

    /**
     * @see BufferedCountingChannelWriter#write(String)
     * @param string
     * @throws IOException
     */
    public void write(String string) throws IOException
    {
        writer.writer().write(string);
    }

    /**
     * @see BufferedCountingChannelWriter#write(byte)
     * @param b
     * @throws IOException
     */
    public void write(byte b) throws IOException
    {
        writer.writer().write(b);
    }

    /**
     * @see BufferedCountingChannelWriter#write(InputStream)
     * @param stream
     * @throws IOException
     */
    public void write(InputStream stream) throws IOException
    {
        writer.writer().write(stream);
    }

    /**
     * @see BufferedCountingChannelWriter#offset()
     * @return the current offset
     */
    public long offset()
    {
        return writer.writer().offset();
    }

    @Override
    public void close() throws IOException
    {
        IOUtils.close(writer);
        context = null;
    }
}
