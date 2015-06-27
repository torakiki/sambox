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

import static org.apache.pdfbox.util.RequireUtils.requireNotNullArg;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import org.apache.pdfbox.SAMBox;
import org.apache.pdfbox.util.Charsets;
import org.apache.pdfbox.util.IOUtils;

/**
 * Component providing methods to write to a {@link CountingWritableByteChannel}. This implementation is buffered and
 * bytes are flushed to the {@link CountingWritableByteChannel} only when the buffer is full. The buffer size is
 * configurable using the {@link SAMBox#OUTPUT_BUFFER_SIZE_PROPERTY} property.
 * 
 * @author Andrea Vacondio
 *
 */
class BufferedDestinationWriter implements Closeable
{
    private static final byte EOL = '\n';

    private CountingWritableByteChannel channel;
    private ByteBuffer buffer = ByteBuffer.allocate(Integer.getInteger(
            SAMBox.OUTPUT_BUFFER_SIZE_PROPERTY, 4096));
    private boolean onNewLine = false;

    public BufferedDestinationWriter(CountingWritableByteChannel channel)
    {
        requireNotNullArg(channel, "Cannot write to a null channell");
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

    public void writeEOL() throws IOException
    {
        if (!onNewLine)
        {
            write(EOL);
            onNewLine = true;
        }
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
            write(bytes[i]);
        }
    }

    /**
     * Writes the single byte to the destination
     * 
     * @param myByte
     * @throws IOException
     */
    public void write(byte myByte) throws IOException
    {
        onNewLine = false;
        buffer.put(myByte);
        if (!buffer.hasRemaining())
        {
            flushBuffer();
        }
    }

    /**
     * Writes everything that is read from the {@link InputStream} to the destination
     * 
     * @param stream
     * @throws IOException
     */
    public void write(InputStream stream) throws IOException
    {
        onNewLine = false;
        try (ReadableByteChannel readable = Channels.newChannel(stream))
        {
            flushBuffer();
            while (readable.read(buffer) != -1)
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
