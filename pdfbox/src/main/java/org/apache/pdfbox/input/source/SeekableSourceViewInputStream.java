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

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Optional;

import org.apache.pdfbox.io.IOUtils;

/**
 * @author Andrea Vacondio
 *
 */
class SeekableSourceViewInputStream extends InputStream
{
    private long startingPosition;
    private long length;
    private long currentPosition;
    private SeekableSource wrapped;

    SeekableSourceViewInputStream(SeekableSource wrapped, long startingPosition, long length)
    {
        this.startingPosition = startingPosition;
        this.currentPosition = 0;
        this.length = length;
        this.wrapped = wrapped;
    }

    @Override
    public int read() throws IOException
    {
        if (available() > 0)
        {
            getSource().position(startingPosition + currentPosition);
            currentPosition++;
            return getSource().read();
        }
        return -1;
    }

    @Override
    public int read(byte[] b) throws IOException
    {
        if (available() > 0)
        {
            getSource().position(startingPosition + currentPosition);
            int read = getSource().read(ByteBuffer.wrap(b, 0, Math.min(b.length, available())));
            if (read > 0)
            {
                currentPosition += read;
                return read;
            }
        }
        return -1;
    }

    @Override
    public int available()
    {
        return (int) (length - currentPosition);
    }

    @Override
    public long skip(long n)
    {
        long skipped = Math.min(n, available());
        currentPosition += skipped;
        return skipped;
    }

    public void seek(long newOffset) throws IOException
    {
        getSource().position(startingPosition + newOffset);
    }

    public long getLength()
    {
        return length;
    }

    @Override
    public void close() throws IOException
    {
        IOUtils.close(wrapped);
        this.currentPosition = 0;
    }

    private SeekableSource getSource()
    {
        return Optional
                .ofNullable(this.wrapped)
                .filter(SeekableSource::isOpen)
                .orElseThrow(() -> new IllegalStateException("The SeekableSource has been closed."));
    }
}
