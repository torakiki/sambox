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
import java.nio.channels.ReadableByteChannel;

/**
 * @author Andrea Vacondio
 *
 */
public interface SeekableSource extends ReadableByteChannel
{
    /**
     * @return the current source position as a positive long
     * @throws IOException
     */
    long position() throws IOException;

    /**
     * Sets the source position. Setting the position to a value that is greater than the source's size is legal but
     * does not change the size of the source. A later attempt to read bytes at such a position will immediately
     * 
     * @param position a non-negative long for the new position
     * @return this source
     * @throws IOException
     */
    SeekableSource position(long position) throws IOException;

    /**
     * @return The source size, measured in bytes
     */
    long size();

    /**
     * Reads a byte of data from this source. The byte is returned as an integer in the range 0 to 255 (
     * {@code 0x00-0xff} ).
     * 
     * @return the next byte of data, or {@code -1} if there is no more data.
     * @throws IOException
     */
    int read() throws IOException;

    /**
     * Skips the given number of bytes
     * 
     * @param offset the number of bytes to skip, either positive or negative to move the position back
     * @return this source
     * @throws IOException
     */
    default SeekableSource skip(long offset) throws IOException
    {
        long newPosition = position() + offset;
        if (newPosition <= 0 || newPosition > size())
        {
            throw new IllegalArgumentException("Skipping " + offset
                    + " moves outside of source boundaries");
        }
        position(newPosition);
        return this;
    }

    /**
     * Reads the next byte and sets the position back by one.
     * 
     * @return the next byte or {@code -1} if there is no more data.
     * @throws IOException
     * @see {@link #read()}
     */
    default int peek() throws IOException
    {
        int val = read();
        if (val != -1)
        {
            skip(-1);
        }
        return val;
    }
}
