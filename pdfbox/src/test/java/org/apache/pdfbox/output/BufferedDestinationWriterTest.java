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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.Arrays;

import org.apache.pdfbox.SAMBox;
import org.apache.pdfbox.util.Charsets;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Andrea Vacondio
 *
 */
public class BufferedDestinationWriterTest
{

    private ByteArrayOutputStream out;
    private CountingWritableByteChannel channel;
    private BufferedDestinationWriter victim;

    @Before
    public void setUp()
    {
        out = new ByteArrayOutputStream();
        channel = CountingWritableByteChannel.from(out);
        victim = new BufferedDestinationWriter(channel);
    }

    @After
    public void after()
    {
        System.getProperties().remove(SAMBox.OUTPUT_BUFFER_SIZE_PROPERTY);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullConstructor()
    {
        new BufferedDestinationWriter(null);
    }

    @Test
    public void close() throws IOException
    {
        victim.close();
        assertFalse(channel.isOpen());
    }

    @Test
    public void closeFlushes() throws IOException
    {
        channel = mock(CountingWritableByteChannel.class);
        victim = new BufferedDestinationWriter(channel);
        victim.writeEOL();
        verify(channel, times(0)).write(any());
        victim.close();
        verify(channel).write(any());
    }

    @Test
    public void writeEOL() throws IOException
    {
        victim.writeEOL();
        victim.close();
        assertTrue(Arrays.equals(new byte[] { '\n' }, out.toByteArray()));
    }

    @Test
    public void prettyPrintJustOneEOL() throws IOException
    {
        victim.writeEOL();
        victim.writeEOL();
        victim.write((byte) -1);
        victim.writeEOL();
        victim.writeEOL();
        victim.close();
        assertTrue(Arrays.equals(new byte[] { '\n', -1, '\n' }, out.toByteArray()));
    }

    @Test(expected = ClosedChannelException.class)
    public void flushOnClosed() throws IOException
    {
        victim.close();
        victim.writeEOL();
        victim.close();
    }

    @Test
    public void writeString() throws IOException
    {
        victim.write("ChuckNorris");
        victim.close();
        assertTrue(Arrays.equals("ChuckNorris".getBytes(Charsets.ISO_8859_1), out.toByteArray()));
    }

    @Test
    public void writeBytesExceedingBuffer() throws IOException
    {
        System.getProperties().setProperty(SAMBox.OUTPUT_BUFFER_SIZE_PROPERTY, "4");
        victim = new BufferedDestinationWriter(channel);
        byte[] bytes = new byte[] { '1', '1', '2', '1', '1' };
        victim.write(bytes);
        assertEquals(5, victim.offset());
        victim.close();
        assertTrue(Arrays.equals(bytes, out.toByteArray()));
    }

    @Test
    public void writeInputStream() throws IOException
    {
        byte[] bytes = new byte[] { '1', '1', '2', '1', '1' };
        victim.write(bytes);
        byte[] streamBytes = "ChuckNorris".getBytes(Charsets.ISO_8859_1);
        ByteArrayInputStream is = new ByteArrayInputStream(streamBytes);
        victim.write(is);
        victim.close();
        byte[] expected = Arrays.copyOf(bytes, bytes.length + streamBytes.length);
        System.arraycopy(streamBytes, 0, expected, bytes.length, streamBytes.length);
        assertTrue(Arrays.equals(expected, out.toByteArray()));
    }
}
