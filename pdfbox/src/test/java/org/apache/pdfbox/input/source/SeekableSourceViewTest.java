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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Andrea Vacondio
 *
 */
public class SeekableSourceViewTest extends BaseTestSeekableSource
{
    private SeekableSourceView victim;
    private Path tempFile;

    @Before
    public void setUp() throws Exception
    {
        tempFile = Files.createTempFile("SAMBox", null);
        Files.copy(getClass().getResourceAsStream("/input/allah2.pdf"), tempFile,
                StandardCopyOption.REPLACE_EXISTING);
        victim = new SeekableSourceView(new FileChannelSeekableSource(tempFile.toFile()), 50, 100);
    }

    @After
    public void after() throws IOException
    {
        Files.deleteIfExists(tempFile);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullSourceConstructor()
    {
        new SeekableSourceView(null, 50, 100);
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativeStartPositionConstructor()
    {
        new SeekableSourceView(new ByteArraySeekableSource(new byte[] { -1 }), -10, 100);
    }

    @Test(expected = IllegalArgumentException.class)
    public void outOfBoundsStartPositionConstructor()
    {
        new SeekableSourceView(new ByteArraySeekableSource(new byte[] { -1, 2 }), 3, 100);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullNonPositiveLengthConstructor()
    {
        new SeekableSourceView(new ByteArraySeekableSource(new byte[] { -1 }), 0, 0);
    }

    @Test
    public void size()
    {
        assertEquals(100, victim.size());
    }

    @Test
    public void sizeTrimmed()
    {
        assertEquals(2, new SeekableSourceView(new ByteArraySeekableSource(new byte[] { -1, 2 }),
                0, 100).size());
    }

    @Override
    @Test(expected = RuntimeException.class)
    public void view() throws IOException
    {
        victim().view(0, 2);
    }

    @Override
    @Test(expected = RuntimeException.class)
    public void viewClosed() throws IOException
    {
        victim().close();
        victim().view(0, 2);
    }

    @Test(expected = IllegalStateException.class)
    public void parentClosed() throws IOException
    {
        ByteArraySeekableSource wrapped = new ByteArraySeekableSource(new byte[] { -1 });
        victim = new SeekableSourceView(wrapped, 0, 1);
        wrapped.close();
        assertTrue(victim.isOpen());
        victim.read();
    }

    @Override
    SeekableSource victim()
    {
        return victim;
    }

    @Test
    public void read() throws IOException
    {
        assertEquals(0, victim.position());
        assertNotNull(victim.read());
        assertNotNull(victim.read());
        assertEquals(2, victim.position());
        victim.position(victim.size());
        assertEquals(-1, victim.read());
    }

    @Test
    public void readBuff() throws IOException
    {
        victim.position(1);
        ByteBuffer dst = ByteBuffer.allocate(10);
        victim.read(dst);
        dst.flip();
        assertEquals(10, dst.remaining());
        victim.position(victim.size());
        ByteBuffer empty = ByteBuffer.allocate(10);
        victim.read(empty);
        empty.flip();
        assertFalse(empty.hasRemaining());
    }

}
