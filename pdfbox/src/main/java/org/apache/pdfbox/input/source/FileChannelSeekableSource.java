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
import static org.apache.pdfbox.util.RequireUtils.requireArg;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.apache.pdfbox.io.IOUtils;

/**
 * @author Andrea Vacondio
 *
 */
public class FileChannelSeekableSource extends BaseSeekableSource
{
    private FileChannel channel;
    private long size;

    public FileChannelSeekableSource(File file) throws IOException
    {
        requireNonNull(file);
        channel = new RandomAccessFile(file, "r").getChannel();
        size = channel.size();
    }

    @Override
    public long position() throws IOException
    {
        return channel.position();
    }

    @Override
    public SeekableSource position(long newPosition) throws IOException
    {
        requireArg(newPosition >= 0, "Cannot set position to a negative value");
        channel.position(newPosition);
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
        IOUtils.close(channel);
    }

    @Override
    public int read(ByteBuffer dst) throws IOException
    {
        return channel.read(dst);
    }

    @Override
    public int read() throws IOException
    {
        ByteBuffer buffer = ByteBuffer.allocate(1);
        if (channel.read(buffer) > 0)
        {
            buffer.flip();
            return buffer.get() & 0xff;
        }
        return -1;
    }
}
