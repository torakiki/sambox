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
package org.apache.pdfbox.input.source;

import static java.util.Optional.ofNullable;
import static org.apache.pdfbox.util.RequireUtils.requireArg;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.pdfbox.SAMBox;
import org.apache.pdfbox.util.IOUtils;

/**
 * {@link SeekableSource} wrapping an existing one and providing buffered read. When a read method is called, a
 * {@link SAMBox#INPUT_BUFFER_SIZE_PROPERTY} long chunk of bytes is read from the underlying source and stored in memory.
 * Subsequent reads are served from the in memory buffer until they fall outside its range, at that point a new buffer
 * is read from the wrapped source.
 * 
 * @author Andrea Vacondio
 */
public class BufferedSeekableSource extends BaseSeekableSource
{

    private ByteBuffer buffer = ByteBuffer.allocate(Integer.getInteger(
            SAMBox.INPUT_BUFFER_SIZE_PROPERTY, 8192));
    private SeekableSource wrapped;
    private long position;
    private long size;

    public BufferedSeekableSource(SeekableSource wrapped)
    {
        super(ofNullable(wrapped).map(SeekableSource::id).orElseThrow(() -> {
            return new IllegalArgumentException("Input decorated SeekableSource cannot be null");
        }));
        this.wrapped = wrapped;
        this.size = wrapped.size();
        this.buffer.limit(0);
    }

    @Override
    public long position()
    {
        return this.position;
    }

    @Override
    public SeekableSource position(long newPosition) throws IOException
    {
        requireArg(newPosition >= 0, "Cannot set position to a negative value");
        if (newPosition != this.position)
        {
            long newBufPosition = newPosition - position + buffer.position();
            if (newBufPosition >= 0 && newBufPosition < buffer.limit())
            {
                buffer.position((int) newBufPosition);
            }
            else
            {
                buffer.limit(0);
                wrapped.position(newPosition);
            }
            this.position = Math.min(newPosition, size);
        }
        return this;
    }

    @Override
    public long size()
    {
        return size;
    }

    @Override
    public void close() throws IOException
    {
        super.close();
        IOUtils.close(wrapped);
        buffer.clear();
        buffer.limit(0);
    }

    @Override
    public int read(ByteBuffer dst) throws IOException
    {
        requireOpen();
        buffer.limit(0);
        wrapped.position(position);
        int read = wrapped.read(dst);
        if (read > 0)
        {
            position += read;
        }
        return read;
    }

    @Override
    public int read() throws IOException
    {
        requireOpen();
        if (ensureBuffer() > 0)
        {
            position++;
            return buffer.get() & 0xff;
        }
        return -1;
    }

    private int ensureBuffer() throws IOException
    {
        if (!buffer.hasRemaining())
        {
            buffer.clear();
            wrapped.read(buffer);
            buffer.flip();
        }
        return buffer.remaining();
    }

    @Override
    public SeekableSource view(long startingPosition, long length) throws IOException
    {
        requireOpen();
        return new BufferedSeekableSource(wrapped.view(startingPosition, length));
    }
}
