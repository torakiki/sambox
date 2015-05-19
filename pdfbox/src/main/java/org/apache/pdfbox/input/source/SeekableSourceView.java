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
import java.nio.ByteBuffer;

import org.apache.pdfbox.io.IOUtils;

/**
 * @author Andrea Vacondio
 *
 */
class SeekableSourceView extends BaseSeekableSource
{
    private long startingPosition;
    private long length;
    private long currentPosition;
    private SeekableSource wrapped;

    SeekableSourceView(SeekableSource wrapped, long startingPosition, long length)
    {
        super(wrapped.id());
        this.startingPosition = startingPosition;
        this.currentPosition = 0;
        this.length = length;
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
        this.currentPosition = Math.min(length, newPosition);
        getSource().position(startingPosition + currentPosition);
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
            getSource().position(startingPosition + currentPosition);
            int read = getSource().read(dst);
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
            getSource().position(startingPosition + currentPosition);
            currentPosition++;
            return getSource().read();
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

    private SeekableSource getSource()
    {
        if (wrapped.isOpen())
        {
            return wrapped;
        }
        throw new IllegalStateException("The SeekableSource has been closed");
    }

    @Override
    public SeekableSource view(long startingPosition, long length)
    {
        throw new IllegalStateException("Cannot create a view of a view");
    }

}
