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
import java.lang.ref.WeakReference;
import java.util.Optional;

/**
 * @author Andrea Vacondio
 *
 */
public class SeekableSourceViewInputStream extends InputStream
{
    private long startingPosition;
    private long length;
    private long currentPosition;
    private WeakReference<SeekableSource> wrapped;

    /**
     * @param startingPosition
     * @param length
     * @param wrapped
     */
    public SeekableSourceViewInputStream(SeekableSource wrapped, long startingPosition, long length)
    {
        this.startingPosition = startingPosition;
        this.currentPosition = 0;
        this.length = length;
        this.wrapped = new WeakReference<>(wrapped);
    }

    @Override
    public int read() throws IOException
    {
        if (currentPosition < length)
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
        return this.read(b, 0, b.length);
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

    /**
     * Doesn't close the wrapped {@link SeekableSource}
     */
    @Override
    public void close()
    {
        this.currentPosition = 0;
    }

    private SeekableSource getSource()
    {
        return Optional
                .ofNullable(this.wrapped.get())
                .filter(SeekableSource::isOpen)
                .orElseThrow(
                        () -> new IllegalStateException(
                                "The original SeekableSource has been closed."));
    }
}
