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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.sejda.sambox.cos.COSObjectKey;
import org.sejda.sambox.cos.COSStream;
import org.sejda.sambox.cos.COSString;

/**
 * @author Andrea Vacondio
 *
 */
public class Algorithm1Test
{
    @Test(expected = IllegalArgumentException.class)
    public void nullKey()
    {
        Algorithm1.withARC4Engine(null);
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
        str.accept(Algorithm1.withARC4Engine(new byte[] { 34, -93, -39, -90, 31, 109, -77, -83, 113,
                101, 21, -10, -13, -22, 42, 116 }));
        assertArrayEquals("A String".getBytes(), str.getBytes());
    }

    @Test
    public void encryptableString() throws Exception
    {
        COSString str = COSString.parseLiteral("it-IT");
        str.encryptable(true);
        Algorithm1 victim = Algorithm1.withARC4Engine(new byte[] { 34, -93, -39, -90, 31, 109, -77,
                -83, 113, 101, 21, -10, -13, -22, 42, 116 });
        victim.setCurrentCOSObjectKey(new COSObjectKey(1, 0));
        str.accept(victim);
        assertArrayEquals(new byte[] { -105, 59, 58, -19, -4 }, str.getBytes());
    }

    @Test(expected = EncryptionException.class)
    public void missingCosObjectKeyString() throws Exception
    {
        COSString str = COSString.parseLiteral("it-IT");
        str.encryptable(true);
        Algorithm1 victim = Algorithm1.withARC4Engine(new byte[] { 34, -93, -39, -90, 31, 109, -77,
                -83, 113, 101, 21, -10, -13, -22, 42, 116 });
        str.accept(victim);
    }

    @Test(expected = EncryptionException.class)
    public void missingCosObjectKeyStream() throws Exception
    {
        COSStream str = new COSStream();
        str.encryptable(true);
        Algorithm1 victim = Algorithm1.withARC4Engine(new byte[] { 34, -93, -39, -90, 31, 109, -77,
                -83, 113, 101, 21, -10, -13, -22, 42, 116 });
        str.accept(victim);
    }

    @Test
    public void encryptableStream() throws Exception
    {
        COSStream str = spy(new COSStream());
        str.encryptable(true);
        Algorithm1 victim = Algorithm1.withARC4Engine(new byte[] { 34, -93, -39, -90, 31, 109, -77,
                -83, 113, 101, 21, -10, -13, -22, 42, 116 });
        victim.setCurrentCOSObjectKey(new COSObjectKey(1, 0));
        str.accept(victim);
        verify(str).setEncryptor(any());
    }
}
