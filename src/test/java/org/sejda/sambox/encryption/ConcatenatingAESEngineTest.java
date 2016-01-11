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

import org.apache.commons.io.IOUtils;
import org.bouncycastle.util.Arrays;
import org.junit.Test;

/**
 * @author Andrea Vacondio
 *
 */
public class ConcatenatingAESEngineTest
{
    private ConcatenatingAESEngine victim = new ConcatenatingAESEngine();

    @Test
    public void encryptBytes()
    {
        byte[] key = new byte[] { -40, -23, -118, -66, -77, -34, 42, 9, 11, 22, 105, 86, -92, 23,
                57, 4 };
        byte[] iv = new byte[] { 18, -87, 49, -32, -126, 116, -128, -36, -78, 70, 99, -98, -65, 90,
                -95, 101 };
        byte[] expected = new byte[] { -125, -84, -39, -13, -125, 92, 23, -82, 68, 81, -78, 105, 34,
                21, -70, -14 };
        assertArrayEquals(Arrays.concatenate(iv, expected),
                victim.encryptBytes("ChuckNorris".getBytes(), key, iv));
    }

    @Test
    public void encryptStream() throws IOException
    {
        byte[] key = new byte[] { -40, -23, -118, -66, -77, -34, 42, 9, 11, 22, 105, 86, -92, 23,
                57, 4 };
        byte[] iv = new byte[] { 18, -87, 49, -32, -126, 116, -128, -36, -78, 70, 99, -98, -65, 90,
                -95, 101 };
        byte[] expected = new byte[] { -125, -84, -39, -13, -125, 92, 23, -82, 68, 81, -78, 105, 34,
                21, -70, -14 };

        InputStream inputStream = victim
                .encryptStream(new ByteArrayInputStream("ChuckNorris".getBytes()), key, iv);
        assertArrayEquals(Arrays.concatenate(iv, expected), IOUtils.toByteArray(inputStream));

    }
}
