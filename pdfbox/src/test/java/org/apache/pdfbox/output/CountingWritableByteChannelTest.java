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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

public class CountingWritableByteChannelTest
{
    private ByteArrayOutputStream out;
    private CountingWritableByteChannel victim;
    private WritableByteChannel wrapped;
    private ByteBuffer src = ByteBuffer.wrap(new byte[] { '1', '1', '2', '1', '1' });

    @Before
    public void setUp()
    {
        out = new ByteArrayOutputStream();
        wrapped = Channels.newChannel(out);
        victim = new CountingWritableByteChannel(wrapped);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullConstructor()
    {
        new CountingWritableByteChannel(null);
    }

    @Test
    public void count() throws Exception
    {
        assertEquals(0, victim.count());
        ByteBuffer src = ByteBuffer.wrap(new byte[] { '1', '1', '2', '1', '1' });
        victim.write(src);
        assertEquals(5, victim.count());
    }

    @Test(expected = ClosedChannelException.class)
    public void closedWrite() throws Exception
    {
        victim.close();
        victim.write(src);
    }

    @Test
    public void write() throws Exception
    {
        victim.write(src);
        assertTrue(Arrays.equals(out.toByteArray(), src.array()));
    }

    @Test
    public void isOpen()
    {
        assertTrue(victim.isOpen());
        assertTrue(wrapped.isOpen());
    }

    @Test
    public void close() throws Exception
    {
        assertTrue(victim.isOpen());
        assertTrue(wrapped.isOpen());
        victim.close();
        assertFalse(victim.isOpen());
        assertFalse(wrapped.isOpen());
    }

    @Test
    public void testFromWritableByteChannel() throws Exception
    {
        victim = CountingWritableByteChannel.from(Channels.newChannel(out));
        victim.write(src);
        assertTrue(Arrays.equals(out.toByteArray(), src.array()));
    }

    @Test
    public void fromOutputStream() throws Exception
    {
        victim = CountingWritableByteChannel.from(out);
        victim.write(src);
        assertTrue(Arrays.equals(out.toByteArray(), src.array()));
    }

    @Test
    public void fromFile() throws Exception
    {
        Path tempFile = Files.createTempFile("SAMBox", null);
        try
        {
            assertEquals(0, Files.size(tempFile));
            victim = CountingWritableByteChannel.from(tempFile.toFile());
            victim.write(src);
            assertTrue(Arrays.equals(Files.readAllBytes(tempFile), src.array()));
        }
        finally
        {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    public void fromString() throws Exception
    {
        Path tempFile = Files.createTempFile("SAMBox", null);
        try
        {
            assertEquals(0, Files.size(tempFile));
            victim = CountingWritableByteChannel.from(tempFile.toAbsolutePath().toString());
            victim.write(src);
            assertTrue(Arrays.equals(Files.readAllBytes(tempFile), src.array()));
        }
        finally
        {
            Files.deleteIfExists(tempFile);
        }
    }

}
