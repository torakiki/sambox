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

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import org.apache.pdfbox.util.IOUtils;

/**
 * Bridge between {@link SeekableSources} and {@link InputStream}
 * 
 * @author Andrea Vacondio
 *
 */
class SeekableSourceInputStream extends InputStream
{
    private SeekableSource wrapped;

    SeekableSourceInputStream(SeekableSource wrapped)
    {
        requireNonNull(wrapped);
        this.wrapped = wrapped;
    }

    @Override
    public int read() throws IOException
    {
        return getSource().read();
    }

    @Override
    public int read(byte[] b) throws IOException
    {
        return getSource().read(ByteBuffer.wrap(b, 0, Math.min(b.length, available())));
    }

    @Override
    public int read(byte[] b, int offset, int length) throws IOException
    {
        return getSource().read(
                ByteBuffer.wrap(b, Math.min(b.length, offset),
                        Math.min(length, Math.min(b.length - offset, available()))));

    }

    @Override
    public int available() throws IOException
    {
        SeekableSource source = getSource();
        return (int) (source.size() - source.position());
    }

    @Override
    public long skip(long offset) throws IOException
    {
        SeekableSource source = getSource();
        long start = source.position();
        return source.forward(Math.min(offset, available())).position() - start;
    }

    @Override
    public void close() throws IOException
    {
        IOUtils.close(wrapped);
    }

    private SeekableSource getSource()
    {
        if (wrapped.isOpen())
        {
            return wrapped;
        }
        throw new IllegalStateException("The SeekableSource has been closed");
    }
}
