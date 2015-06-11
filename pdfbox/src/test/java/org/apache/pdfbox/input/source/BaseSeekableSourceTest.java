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
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.junit.Before;
import org.junit.Test;

/**
 * @author Andrea Vacondio
 *
 */
public class BaseSeekableSourceTest
{

    private BaseSeekableSource victim;

    @Before
    public void setUp()
    {
        victim = new BaseSeekableSource("id")
        {
            @Override
            public int read(ByteBuffer dst) throws IOException
            {
                return 0;
            }

            @Override
            public SeekableSource view(long startingPosition, long length) throws IOException
            {
                return null;
            }

            @Override
            public long size()
            {
                return 0;
            }

            @Override
            public int read() throws IOException
            {
                return 0;
            }

            @Override
            public SeekableSource position(long position) throws IOException
            {
                return null;
            }

            @Override
            public long position() throws IOException
            {
                return 0;
            }
        };
    }

    @Test
    public void isOpen() throws IOException
    {
        assertTrue(victim.isOpen());
        victim.close();
        assertFalse(victim.isOpen());
    }

    @Test
    public void requireOpen() throws IOException
    {
        assertTrue(victim.isOpen());
        victim.requireOpen();
    }

    @Test(expected = IOException.class)
    public void failingRequireOpen() throws IOException
    {
        assertTrue(victim.isOpen());
        victim.close();
        victim.requireOpen();
    }

    @Test
    public void id()
    {
        assertEquals("id", victim.id());
    }
}
