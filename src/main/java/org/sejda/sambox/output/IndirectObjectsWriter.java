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

import static org.sejda.sambox.output.DefaultCOSWriter.SPACE;
import static org.sejda.util.RequireUtils.requireNotNullArg;

import java.io.Closeable;
import java.io.IOException;
import java.util.TreeMap;

import org.sejda.io.BufferedCountingChannelWriter;
import org.sejda.sambox.cos.IndirectCOSObjectReference;
import org.sejda.sambox.util.Charsets;
import org.sejda.sambox.xref.XrefEntry;
import org.sejda.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Component that writes pdf objects and keeps track of what has been written to avoid writing an object twice.
 * 
 * @author Andrea Vacondio
 */
class IndirectObjectsWriter implements Closeable
{
    private static final byte[] OBJ = "obj".getBytes(Charsets.US_ASCII);
    private static final byte[] ENDOBJ = "endobj".getBytes(Charsets.US_ASCII);

    private static final Logger LOG = LoggerFactory.getLogger(DefaultPDFWriter.class);

    private TreeMap<Long, XrefEntry> written = new TreeMap<>();
    private COSWriter writer;

    public IndirectObjectsWriter(COSWriter writer)
    {
        requireNotNullArg(writer, "Cannot write to a null COSWriter");
        this.writer = writer;
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
        if (!written.containsKey(object.xrefEntry().getObjectNumber()))
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
        doWriteObject(object);
        written.put(object.xrefEntry().getObjectNumber(), object.xrefEntry());
        object.releaseCOSObject();
        LOG.trace("Released " + object);
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
        LOG.trace("Written object " + object.xrefEntry());
    }

    /**
     * @return number of written objects so far.
     */
    public int size()
    {
        return written.size();
    }

    /**
     * Adds an entry to the list of the written entries
     * 
     * @param entry
     * @return the previous value if an entry with the same object number has been already written, null otherwise.
     */
    public XrefEntry put(XrefEntry entry)
    {
        return written.put(entry.getObjectNumber(), entry);
    }

    /**
     * @return the written entry with the highest object number
     */
    public XrefEntry highest()
    {
        return written.lastEntry().getValue();
    }

    /**
     * @return the written entry with the lowest object number
     */
    public XrefEntry lowest()
    {
        return written.firstEntry().getValue();
    }

    /**
     * 
     * @param objectNumber
     * @return the written entry with the given object number if any, null otherwise.
     */
    public XrefEntry get(Long objectNumber)
    {
        return written.get(objectNumber);
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
        written.clear();
    }
}
