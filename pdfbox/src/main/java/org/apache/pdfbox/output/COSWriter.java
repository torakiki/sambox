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
package org.apache.pdfbox.output;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBoolean;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSFloat;
import org.apache.pdfbox.cos.COSInteger;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSNull;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.cos.COSVisitor;
import org.apache.pdfbox.cos.IndirectCOSObjectReference;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.util.Charsets;

/**
 * @author Andrea Vacondio
 *
 */
class COSWriter implements COSVisitor
{
    private static final Log LOG = LogFactory.getLog(COSWriter.class);
    private static final String OUTPUT_PAGE_SIZE = "org.pdfbox.output.page.size";

    private CountingWritableByteChannel channel;
    private ByteBuffer buffer = ByteBuffer.allocate(Integer.getInteger(OUTPUT_PAGE_SIZE, 4096));

    public COSWriter(CountingWritableByteChannel channel)
    {
        requireNonNull(channel);
        this.channel = channel;
    }

    @Override
    public void visit(COSArray value)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void visit(COSBoolean value) throws IOException
    {
        write(value.toString());
    }

    @Override
    public void visit(COSDictionary value)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void visit(COSFloat value)
    {
    }

    @Override
    public void visit(COSInteger value) throws IOException
    {
        write(Long.toString(value.longValue()));

    }

    @Override
    public void visit(COSName value)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void visit(COSNull value) throws IOException
    {
        write("null".getBytes(Charsets.ISO_8859_1));

    }

    @Override
    public void visit(COSStream value)
    {
        // alla fine
        // value.setItem(COSName.LENGTH, asDirect(lengthObject));

    }

    @Override
    public void visit(COSString value) throws IOException
    {
    }

    @Override
    public void visit(IndirectCOSObjectReference value) throws IOException
    {
        value.xrefEntry().setByteOffset(offset());
        write(value.xrefEntry().key().toString());
    }
    /**
     * @return the current offset in the output
     */
    public long offset()
    {
        return channel.count() + buffer.position();
    }

    @Override
    public void close() throws IOException
    {
        if (buffer.position() != 0)
        {
            flushBuffer();
        }
        IOUtils.close(channel);
    }

    /**
     * writes the given string in {@link Charsets#ISO_8859_1}
     * 
     * @param value
     * @throws IOException
     */
    void write(String value) throws IOException
    {
        write(value.getBytes(Charsets.ISO_8859_1));
    }

    void write(byte[] bytes) throws IOException
    {
        for (int i = 0; i < bytes.length; i++)
        {
            buffer.put(bytes[i]);
            if (!buffer.hasRemaining())
            {
                flushBuffer();
            }
        }
    }

    private void flushBuffer() throws IOException
    {
        buffer.flip();
        channel.write(buffer);
        buffer.clear();
    }

}
