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
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;
import org.sejda.commons.util.IOUtils;

/**
 * @author Andrea Vacondio
 *
 */
public class AESEngineNoPaddingTest
{

    @Test
    public void cbcEncryptStream() throws Exception
    {
        AESEngineNoPadding victim = AESEngineNoPadding.cbc();
        byte[] key = new byte[] { 81, -66, -6, 103, 11, 76, 80, 16, 101, 87, -126, -37, 97, -87,
                -124, -118 };
        byte[] iv = new byte[] { -49, -25, 115, 103, -86, 27, 0, -49, 123, -9, -90, 16, -122, -31,
                96, 75 };
        byte[] expected = new byte[] { -102, 116, -56, -70, -101, 33, 97, -1, -12, -126, -107, 9,
                96, -34, 92, -128 };

        try (InputStream inputStream = victim
                .encryptStream(new ByteArrayInputStream("ChuckNorrisKick!".getBytes()), key, iv))
        {
            assertArrayEquals(expected, IOUtils.toByteArray(inputStream));
        }
    }

    @Test
    public void ebcEncryptStream() throws IOException
    {
        AESEngineNoPadding victim = AESEngineNoPadding.ecb();
        byte[] key = new byte[] { -91, 100, 117, 33, 83, 54, -34, -59, 87, -94, -94, 18, -113, -77,
                -60, 105, 95, -3, 70, -110, 19, -44, -15, 104, 44, 1, 31, 17, -97, 107, 59, 5 };
        byte[] expected = new byte[] { -82, 109, -33, 109, -111, -94, 21, -77, 74, -119, 121, -115,
                -62, -128, 0, -43 };
        try (InputStream inputStream = victim.encryptStream(new ByteArrayInputStream(
                new byte[] { -4, -1, -1, -1, -1, -1, -1, -1, 84, 97, 100, 98, -19, -29, 119, 38 }),
                key, null))
        {
            assertArrayEquals(expected, IOUtils.toByteArray(inputStream));
        }
    }

    @Test
    public void cbcEncryptBytes()
    {
        AESEngineNoPadding victim = AESEngineNoPadding.cbc();
        byte[] key = new byte[] { 81, -66, -6, 103, 11, 76, 80, 16, 101, 87, -126, -37, 97, -87,
                -124, -118 };
        byte[] iv = new byte[] { -49, -25, 115, 103, -86, 27, 0, -49, 123, -9, -90, 16, -122, -31,
                96, 75 };
        byte[] expected = new byte[] { -102, 116, -56, -70, -101, 33, 97, -1, -12, -126, -107, 9,
                96, -34, 92, -128 };
        assertArrayEquals(expected, victim.encryptBytes("ChuckNorrisKick!".getBytes(), key, iv));
    }

    @Test
    public void ecbEncryptBytes()
    {
        AESEngineNoPadding victim = AESEngineNoPadding.ecb();
        byte[] key = new byte[] { -91, 100, 117, 33, 83, 54, -34, -59, 87, -94, -94, 18, -113, -77,
                -60, 105, 95, -3, 70, -110, 19, -44, -15, 104, 44, 1, 31, 17, -97, 107, 59, 5 };
        byte[] expected = new byte[] { -82, 109, -33, 109, -111, -94, 21, -77, 74, -119, 121, -115,
                -62, -128, 0, -43 };
        assertArrayEquals(expected, victim.encryptBytes(
                new byte[] { -4, -1, -1, -1, -1, -1, -1, -1, 84, 97, 100, 98, -19, -29, 119, 38 },
                key, null));
    }

}
