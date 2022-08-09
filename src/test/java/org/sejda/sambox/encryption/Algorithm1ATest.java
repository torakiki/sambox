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
package org.sejda.sambox.encryption;

import static org.junit.Assert.assertArrayEquals;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.sejda.sambox.cos.COSStream;
import org.sejda.sambox.cos.COSString;

/**
 * @author Andrea Vacondio
 *
 */
public class Algorithm1ATest
{

    private final byte[] key = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2 };

    private Algorithm1A victim;
    private AESEncryptionAlgorithmEngine engine;

    @Before
    public void setUp()
    {
        engine = mock(ConcatenatingAESEngine.class);
        victim = new Algorithm1A(key, engine);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullKey()
    {
        new Algorithm1A(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void wrongSizedKey()
    {
        new Algorithm1A(new byte[] { -1, 2, 3 });
    }

    @Test
    public void nonEncryptableString() throws Exception
    {
        COSString str = COSString.parseLiteral("A String");
        str.encryptable(false);
        str.accept(victim);
        verify(engine, never()).encryptBytes(any(), any());
    }

    @Test
    public void encryptableString() throws Exception
    {
        COSString str = COSString.parseLiteral("A String");
        str.encryptable(true);
        when(engine.encryptBytes(aryEq(str.getBytes()), aryEq(key)))
                .thenReturn(new byte[] { 1, 2 });
        str.accept(victim);
        verify(engine).encryptBytes(any(), any());
        assertArrayEquals(new byte[] { 1, 2 }, str.getBytes());
    }

    @Test
    public void nonEncryptableStream() throws Exception
    {
        COSStream str = spy(new COSStream());
        str.encryptable(false);
        str.accept(victim);
        verify(str, never()).setEncryptor(any());
    }

    @Test
    public void encryptableStream() throws Exception
    {
        COSStream str = spy(new COSStream());
        str.encryptable(true);
        str.accept(victim);
        verify(str).setEncryptor(any());
    }
}
