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

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.util.Charsets;

/**
 * @author Andrea Vacondio
 *
 */
class DestinationWriter implements Closeable
{
    private static final String OUTPUT_PAGE_SIZE_PROPERTY = "org.pdfbox.output.page.size";

    private static final byte[] EOL = { '\n' };
    private static final byte[] SPACE = { ' ' };
    private static final byte[] CRLF = { '\r', '\n' };

    private CountingWritableByteChannel channel;
    private ByteBuffer buffer = ByteBuffer.allocate(Integer.getInteger(OUTPUT_PAGE_SIZE_PROPERTY,
            4096));

    public DestinationWriter(CountingWritableByteChannel channel)
    {
        requireNonNull(channel);
        this.channel = channel;
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
     * Writes the given string in {@link Charsets#ISO_8859_1}
     * 
     * @param value
     * @throws IOException
     */
    public void write(String value) throws IOException
    {
        write(value.getBytes(Charsets.ISO_8859_1));
    }

    public void writeEOL() throws IOException
    {
        write(EOL);
    }

    public void writeSpace() throws IOException
    {
        write(SPACE);
    }

    public void writeCRLF() throws IOException
    {
        write(CRLF);
    }

    /**
     * Writes the given bytes to the destination
     * 
     * @param bytes
     * @throws IOException
     */
    public void write(byte[] bytes) throws IOException
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

    /**
     * @return the current offset in the output
     */
    public long offset()
    {
        return channel.count() + buffer.position();
    }

    private void flushBuffer() throws IOException
    {
        buffer.flip();
        channel.write(buffer);
        buffer.clear();
    }
}
