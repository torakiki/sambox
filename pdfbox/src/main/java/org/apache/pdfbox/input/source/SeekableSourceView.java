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

import org.apache.pdfbox.util.IOUtils;

/**
 * {@link SeekableSource} representing a view over a portion of a {@link SeekableSource}. A view becomes invalid if the
 * parent {@link SeekableSource} is closed and the parent view is closed when the view is closed. The parent position is
 * modified when a read method is called on the view.
 * 
 * @author Andrea Vacondio
 *
 */
public class SeekableSourceView extends BaseSeekableSource
{
    private long startingPosition;
    private long length;
    private long currentPosition;
    private SeekableSource wrapped;

    public SeekableSourceView(SeekableSource wrapped, long startingPosition, long length)
    {
        super(ofNullable(wrapped).map(SeekableSource::id).orElseThrow(() -> {
            return new IllegalArgumentException("Input decorated SeekableSource cannot be null");
        }));
        requireArg(startingPosition >= 0 && startingPosition < wrapped.size(),
                "Starting position cannot be negative");
        requireArg(length > 0, "View length must be positive");
        this.startingPosition = startingPosition;
        this.currentPosition = 0;
        this.length = Math.min(length, wrapped.size() - startingPosition);
        this.wrapped = wrapped;
    }

    @Override
    public long position()
    {
        return currentPosition;
    }

    @Override
    public SeekableSource position(long newPosition) throws IOException
    {
        requireArg(newPosition >= 0, "Cannot set position to a negative value");
        this.currentPosition = Math.min(length, newPosition);
        wrapped.position(startingPosition + currentPosition);
        return this;
    }

    @Override
    public long size()
    {
        return length;
    }

    @Override
    public int read(ByteBuffer dst) throws IOException
    {
        requireOpen();
        if (available())
        {
            wrapped.position(startingPosition + currentPosition);
            int read = wrapped.read(dst);
            if (read > 0)
            {
                currentPosition += read;
                return read;
            }
        }
        return -1;
    }

    @Override
    public int read() throws IOException
    {
        requireOpen();
        if (available())
        {
            wrapped.position(startingPosition + currentPosition);
            currentPosition++;
            return wrapped.read();
        }
        return -1;
    }

    private boolean available()
    {
        return currentPosition < length;
    }

    @Override
    public void close() throws IOException
    {
        super.close();
        IOUtils.close(wrapped);
        this.currentPosition = 0;
    }

    @Override
    protected void requireOpen()
    {
        super.requireOpen();
        if (!wrapped.isOpen())
        {
            throw new IllegalStateException("The original SeekableSource has been closed");
        }
    }

    /**
     * Cannot create a view of a view. This method throws {@link RuntimeException}.
     * 
     * @throws RuntimeException
     */
    @Override
    public SeekableSource view(long startingPosition, long length)
    {
        throw new RuntimeException("Cannot create a view of a view");
    }

}
