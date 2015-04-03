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
package org.apache.pdfbox.io;

import java.io.IOException;
import java.io.OutputStream;

/**
 * This will write to a RandomAccessFile in the filesystem and keep track of the position it is writing to and the
 * length of the stream.
 *
 * @author Ben Litchfield
 */
public class RandomAccessFileOutputStream extends OutputStream
{
    private final RandomAccess file;
    private final long position;
    private long lengthWritten = 0;

    /**
     * Constructor to create an output stream that will write to the end of a random access file.
     *
     * @param raf The file to write to.
     *
     * @throws IOException If there is a problem accessing the raf.
     */
    public RandomAccessFileOutputStream(RandomAccess raf) throws IOException
    {
        file = raf;
        // first get the position that we will be writing to
        position = raf.length();
    }

    /**
     * This will get the position in the RAF that the stream was written to.
     *
     * @return The position in the raf where the file can be obtained.
     */
    public long getPosition()
    {
        return position;
    }

    /**
     * @return The number of bytes actually written to this stream.
     */
    public long getLength()
    {
        return lengthWritten;
    }

    @Override
    public void write(byte[] b, int offset, int length) throws IOException
    {
        file.seek(position + lengthWritten);
        lengthWritten += length;
        file.write(b, offset, length);

    }

    @Override
    public void write(int b) throws IOException
    {
        file.seek(position + lengthWritten);
        lengthWritten++;
        file.write(b);
    }

}
