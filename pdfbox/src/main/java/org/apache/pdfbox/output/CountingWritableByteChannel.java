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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

/**
 * A {@link WritableByteChannel} that keeps track of the number of written bytes
 * 
 * @author Andrea Vacondio
 */
class CountingWritableByteChannel implements WritableByteChannel
{

    private long written = 0;
    private WritableByteChannel wrapped;

    private CountingWritableByteChannel(WritableByteChannel wrapped)
    {
        requireNonNull(wrapped);
        this.wrapped = wrapped;
    }

    /**
     * @return the number of written bytes
     */
    public long count()
    {
        return written;
    }

    @Override
    public int write(ByteBuffer src) throws IOException
    {
        int count = wrapped.write(src);
        written += count;
        return count;
    }

    @Override
    public boolean isOpen()
    {
        return wrapped.isOpen();
    }

    @Override
    public void close() throws IOException
    {
        wrapped.close();
    }

    static CountingWritableByteChannel from(WritableByteChannel channel)
    {
        return new CountingWritableByteChannel(channel);
    }

    static CountingWritableByteChannel from(OutputStream stream)
    {
        return new CountingWritableByteChannel(Channels.newChannel(stream));
    }

    static CountingWritableByteChannel from(File file) throws FileNotFoundException
    {
        return new CountingWritableByteChannel(new RandomAccessFile(file, "rw").getChannel());
    }

    static CountingWritableByteChannel from(String file) throws FileNotFoundException
    {
        return new CountingWritableByteChannel(new RandomAccessFile(file, "rw").getChannel());
    }
}
