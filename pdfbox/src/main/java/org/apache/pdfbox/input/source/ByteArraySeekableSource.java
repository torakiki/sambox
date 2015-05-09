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

import java.nio.ByteBuffer;

/**
 * A byte array based {@link SeekableSource} with a max size of 2GB.
 * 
 * @author Andrea Vacondio
 *
 */
public class ByteArraySeekableSource implements SeekableSource
{
    private byte[] bytes;
    private long position;

    public ByteArraySeekableSource(byte[] bytes)
    {
        requireNonNull(bytes, "Cannot create a byte array source from a null byte array");
        this.bytes = bytes;
    }

    @Override
    public long position()
    {
        return position;
    }

    @Override
    public SeekableSource position(long position)
    {
        requireArg(position >= 0, "Cannot set position to a negative value");
        this.position = Math.min(position, size());
        return this;
    }

    @Override
    public long size()
    {
        return bytes.length;
    }

    @Override
    public int read(ByteBuffer dst)
    {
        if (position < size())
        {
            int toCopy = (int) Math.min(dst.remaining(), size() - position);
            dst.put(bytes, (int) position, toCopy);
            position += toCopy;
            return toCopy;
        }
        return -1;
    }

    @Override
    public int read()
    {
        if (position < size())
        {
            return bytes[(int) position++] & 0xff;
        }
        return -1;
    }

    @Override
    public void close()
    {
        // nothing to do
    }

}
