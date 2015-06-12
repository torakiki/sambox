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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.pdfbox.util.IOUtils;
import org.junit.After;
import org.junit.Test;

/**
 * @author Andrea Vacondio
 *
 */
public abstract class BaseTestSeekableSource
{

    abstract SeekableSource victim();

    @After
    public void tearDown() throws IOException
    {
        IOUtils.close(victim());
    }

    @Test(expected = IllegalArgumentException.class)
    public void illegalPosition() throws IOException
    {
        victim().position(-10);
    }

    @Test(expected = IllegalStateException.class)
    public void viewClosed() throws IOException
    {
        victim().close();
        victim().view(0, 2);
    }

    @Test
    public void view() throws IOException
    {
        assertNotNull(victim().view(0, 2));
    }

    @Test
    public void close() throws IOException
    {
        victim().read();
        assertTrue(victim().isOpen());
        victim().close();
        assertFalse(victim().isOpen());
    }

    @Test(expected = IllegalStateException.class)
    public void readClosed() throws IOException
    {
        victim().close();
        victim().read();
    }

    @Test(expected = IllegalStateException.class)
    public void readByteBuffClosed() throws IOException
    {
        victim().close();
        victim().read(ByteBuffer.allocate(5));
    }

    @Test
    public void forward() throws IOException
    {
        assertEquals(0, victim().position());
        assertEquals(1, victim().forward(1).position());
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidForward() throws IOException
    {
        assertEquals(0, victim().position());
        victim().forward(victim().size() + 1);
    }

    @Test
    public void back() throws IOException
    {
        assertEquals(1, victim().forward(1).position());
        assertEquals(0, victim().back().position());
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidBack() throws IOException
    {
        assertEquals(0, victim().position());
        victim().back();
    }

    @Test
    public void peek() throws IOException
    {
        assertEquals(0, victim().position());
        assertNotEquals(-1, victim().peek());
        assertEquals(0, victim().position());
    }

    @Test
    public void peekEOF() throws IOException
    {
        victim().position(victim().size());
        assertEquals(-1, victim().peek());
    }

}
