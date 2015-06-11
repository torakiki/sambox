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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Andrea Vacondio
 *
 */
public class SeekableSourceInputStreamTest
{

    private SeekableSource source;
    private SeekableSourceInputStream victim;

    @Before
    public void setUp()
    {
        source = mock(SeekableSource.class);
        when(source.isOpen()).thenReturn(true);
        victim = new SeekableSourceInputStream(source);
    }

    @Test(expected = NullPointerException.class)
    public void nullSource()
    {
        new SeekableSourceInputStream(null);
    }

    @Test
    public void read() throws IOException
    {
        victim.read();
        verify(source).read();
    }

    @Test(expected = IllegalStateException.class)
    public void readClosed() throws IOException
    {
        when(source.isOpen()).thenReturn(false);
        victim.read();
    }

    @Test
    public void readByteArray() throws IOException
    {
        ArgumentCaptor<ByteBuffer> captor = ArgumentCaptor.forClass(ByteBuffer.class);
        byte[] b = new byte[10];
        when(source.size()).thenReturn(20L);
        when(source.position()).thenReturn(0L);
        victim.read(b);
        verify(source).read(captor.capture());
        ByteBuffer captured = captor.getValue();
        assertEquals(10, captured.capacity());
        assertEquals(0, captured.position());
        assertTrue(captured.hasArray());
    }

    @Test(expected = IllegalStateException.class)
    public void readByteArrayClosed() throws IOException
    {
        when(source.isOpen()).thenReturn(false);
        victim.read(new byte[10]);
    }

    @Test
    public void readByteArrayWithPos() throws IOException
    {
        ArgumentCaptor<ByteBuffer> captor = ArgumentCaptor.forClass(ByteBuffer.class);
        byte[] b = new byte[10];
        when(source.size()).thenReturn(20L);
        when(source.position()).thenReturn(0L);
        victim.read(b, 5, 2);
        verify(source).read(captor.capture());
        ByteBuffer captured = captor.getValue();
        assertEquals(10, captured.capacity());
        assertEquals(5, captured.position());
        assertEquals(7, captured.limit());
        assertTrue(captured.hasArray());
    }

    @Test(expected = IllegalStateException.class)
    public void readByteArrayWithPosClosed() throws IOException
    {
        when(source.isOpen()).thenReturn(false);
        victim.read(new byte[10], 5, 2);
    }

    @Test
    public void available() throws IOException
    {
        when(source.size()).thenReturn(20L);
        when(source.position()).thenReturn(3L);
        assertEquals(17, victim.available());
    }

    @Test
    public void close() throws IOException
    {
        victim.close();
        verify(source).close();
    }

    @Test
    public void skip() throws IOException
    {
        ByteArraySeekableSource source = new ByteArraySeekableSource(new byte[] { -1, 1, 0, 1 });
        SeekableSourceInputStream victim = new SeekableSourceInputStream(source);
        assertEquals(0, source.position());
        victim.skip(2);
        assertEquals(2, source.position());
        assertEquals(2, victim.skip(1000));
    }

    @Test(expected = IllegalStateException.class)
    public void skipClosed() throws IOException
    {
        when(source.isOpen()).thenReturn(false);
        victim.skip(5);
    }
}
