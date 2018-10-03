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

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Test;
import org.sejda.commons.util.IOUtils;

public class ARC4EngineTest
{
    static
    {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Test
    public void encryptBytes() throws Exception
    {
        byte[] expected = new byte[] { (byte) 0xa2, (byte) 0xc9, (byte) 0xf3, (byte) 0xfa,
                (byte) 0x70, (byte) 0x8b, (byte) 0x59, (byte) 0xde, (byte) 0x4a, (byte) 0x8d,
                (byte) 0xc1 };
        assertArrayEquals(expected,
                new ARC4Engine().encryptBytes("ChuckNorris".getBytes(StandardCharsets.UTF_8),
                        "ABCDE".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    public void encryptStream() throws Exception
    {
        byte[] expected = new byte[] { (byte) 0xa2, (byte) 0xc9, (byte) 0xf3, (byte) 0xfa,
                (byte) 0x70, (byte) 0x8b, (byte) 0x59, (byte) 0xde, (byte) 0x4a, (byte) 0x8d,
                (byte) 0xc1 };
        assertArrayEquals(expected,
                IOUtils.toByteArray(new ARC4Engine().encryptStream(
                        new ByteArrayInputStream("ChuckNorris".getBytes(StandardCharsets.UTF_8)),
                        "ABCDE".getBytes(StandardCharsets.UTF_8))));
    }

}
