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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.WritableByteChannel;

import org.apache.pdfbox.util.IOUtils;

/**
 * A {@link WritableByteChannel} that keeps track of the number of written bytes
 * 
 * @author Andrea Vacondio
 */
public class CountingWritableByteChannel implements WritableByteChannel
{

    private long written = 0;
    private WritableByteChannel wrapped;

    public CountingWritableByteChannel(WritableByteChannel wrapped)
    {
        requireNotNullArg(wrapped, "Cannot decorate a null instance");
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
        if (!isOpen())
        {
            throw new ClosedChannelException();
        }
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
        IOUtils.close(wrapped);
    }

    /**
     * Static factory method to create a {@link CountingWritableByteChannel} from an existing
     * {@link WritableByteChannel}.
     * 
     * @param channel
     * @return the newly created {@link CountingWritableByteChannel}
     */
    public static CountingWritableByteChannel from(WritableByteChannel channel)
    {
        return new CountingWritableByteChannel(channel);
    }

    /**
     * Static factory method to create a {@link CountingWritableByteChannel} from an existing {@link OutputStream}.
     * 
     * @param stream
     * @return the newly created {@link CountingWritableByteChannel}
     */
    public static CountingWritableByteChannel from(OutputStream stream)
    {
        return new CountingWritableByteChannel(Channels.newChannel(stream));
    }

    /**
     * Static factory method to create a {@link CountingWritableByteChannel} from an existing {@link File}. If the file
     * already exists its content is purged.
     * 
     * @param file
     * @return the newly created {@link CountingWritableByteChannel}
     * @see RandomAccessFile#RandomAccessFile(File, String)
     */
    public static CountingWritableByteChannel from(File file) throws IOException
    {
        RandomAccessFile raf = new RandomAccessFile(file, "rw");
        raf.setLength(0);
        return new CountingWritableByteChannel(raf.getChannel());
    }

    /**
     * Static factory method to create a {@link CountingWritableByteChannel} from an existing file path. If the file
     * already exists its content is purged.
     * 
     * @param file
     * @return the newly created {@link CountingWritableByteChannel}
     * @see RandomAccessFile#RandomAccessFile(String, String)
     */
    public static CountingWritableByteChannel from(String file) throws IOException
    {
        RandomAccessFile raf = new RandomAccessFile(file, "rw");
        raf.setLength(0);
        return new CountingWritableByteChannel(raf.getChannel());
    }
}
