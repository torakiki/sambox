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

import static org.apache.pdfbox.util.RequireUtils.requireArg;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Andrea Vacondio
 *
 */
public class MemoryMappedSeekableSource extends BaseSeekableSource
{
    private static final Log LOG = LogFactory.getLog(MemoryMappedSeekableSource.class);

    private static final long PAGE_SIZE = 1 << 29; // 500MB
    private List<ByteBuffer> pages = new ArrayList<>();
    private long position;
    private long size;

    public MemoryMappedSeekableSource(File file) throws IOException
    {
        super(file);
        try (FileChannel channel = new RandomAccessFile(file, "r").getChannel())
        {
            this.size = channel.size();
            int zeroBasedPagesNumber = (int) (channel.size() / PAGE_SIZE);
            for (int i = 0; i <= zeroBasedPagesNumber; i++)
            {
                if (i == zeroBasedPagesNumber)
                {
                    pages.add(
                            i,
                            channel.map(MapMode.READ_ONLY, i * PAGE_SIZE, channel.size()
                                    - (i * PAGE_SIZE)));
                }
                else
                {
                    pages.add(i, channel.map(MapMode.READ_ONLY, i * PAGE_SIZE, PAGE_SIZE));
                }
            }
            LOG.debug("Created MemoryMappedSeekableSource with " + pages.size() + " pages");
        }
    }

    private MemoryMappedSeekableSource(MemoryMappedSeekableSource parent)
    {
        super(parent.id());
        this.size = parent.size;
        for (ByteBuffer page : parent.pages)
        {
            this.pages.add(page.duplicate());
        }
    }

    @Override
    public long position()
    {
        return position;
    }

    @Override
    public long size()
    {
        return size;
    }

    @Override
    public SeekableSource position(long position)
    {
        requireArg(position >= 0, "Cannot set position to a negative value");
        this.position = Math.min(position, this.size);
        return this;
    }

    @Override
    public int read(ByteBuffer dst)
    {
        int zeroBasedPagesNumber = (int) (position() / PAGE_SIZE);
        ByteBuffer page = pages.get(zeroBasedPagesNumber);
        int relativePosition = (int) (position() - (zeroBasedPagesNumber * PAGE_SIZE));
        if (relativePosition < page.limit())
        {
            int read = readPage(dst, zeroBasedPagesNumber, relativePosition);
            while (dst.hasRemaining())
            {
                int readBytes = readPage(dst, ++zeroBasedPagesNumber, 0);
                if (readBytes == 0)
                {
                    break;
                }
                read += readBytes;
            }
            position += read;
            return read;
        }
        return -1;
    }

    private int readPage(ByteBuffer dst, int pageNumber, int bufferPosition)
    {
        if (pageNumber < pages.size())
        {
            ByteBuffer page = pages.get(pageNumber);
            page.position(bufferPosition);
            if (page.hasRemaining())
            {
                int toRead = Math.min(dst.remaining(), page.remaining());
                byte[] bufToRead = new byte[toRead];
                page.get(bufToRead);
                dst.put(bufToRead);
                return toRead;
            }
        }
        return 0;
    }

    @Override
    public int read()
    {
        int zeroBasedPagesNumber = (int) (position() / PAGE_SIZE);
        ByteBuffer page = pages.get(zeroBasedPagesNumber);
        int relativePosition = (int) (position() - (zeroBasedPagesNumber * PAGE_SIZE));
        if (relativePosition < page.limit())
        {
            position++;
            return page.get(relativePosition);
        }
        return -1;
    }

    @Override
    public void close() throws IOException
    {
        super.close();
        pages.clear();
    }

    @Override
    public InputStream view(long startingPosition, long length)
    {
        return new SeekableSourceViewInputStream(new MemoryMappedSeekableSource(this),
                startingPosition, length);
    }

}
