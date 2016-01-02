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

import org.junit.Test;

/**
 * @author Andrea Vacondio
 *
 */
public class Algorithm2Test
{
    private Algorithm2 victim = new Algorithm2();

    @Test
    public void computeEncryptionKeyARC4()
    {
        StandardSecurity security = new StandardSecurity("test", null,
                StandardSecurityEncryption.ARC4_128, true);
        security.documentId(new byte[] { -5, 78, 122, -45, 106, -102, 20, -35, -125, 7, 43, -52,
                -104, 23, -68, 113 });
        byte[] expected = new byte[] { -40, -23, -118, -66, -77, -34, 42, 9, 11, 22, 105, 86, -92,
                23, 57, 4 };
        assertArrayEquals(expected, victim.computeEncryptionKey(security));
    }

    @Test
    public void computeEncryptionKeyAES128()
    {
        StandardSecurity security = new StandardSecurity("test", null,
                StandardSecurityEncryption.AES_128, true);
        security.documentId(new byte[] { -5, 78, 122, -45, 106, -102, 20, -35, -125, 7, 43, -52,
                -104, 23, -68, 113 });
        byte[] expected = new byte[] { -40, -23, -118, -66, -77, -34, 42, 9, 11, 22, 105, 86, -92,
                23, 57, 4 };
        assertArrayEquals(expected, victim.computeEncryptionKey(security));
    }

    @Test
    public void computeEncryptionKeyAES128NoMeta()
    {
        StandardSecurity security = new StandardSecurity("test", null,
                StandardSecurityEncryption.AES_128, false);
        security.documentId(new byte[] { -5, 78, 122, -45, 106, -102, 20, -35, -125, 7, 43, -52,
                -104, 23, -68, 113 });
        byte[] expected = new byte[] { 16, 21, -116, 88, -24, 99, -16, -74, -35, -46, -31, -21, 87,
                116, 111, -90 };
        assertArrayEquals(expected, victim.computeEncryptionKey(security));
    }

}
