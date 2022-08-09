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

import static org.bouncycastle.util.Arrays.concatenate;
import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

/**
 * @author Andrea Vacondio
 */
public class Algorithm2BTest
{
    private final byte[] key = new byte[] { -91, 100, 117, 33, 83, 54, -34, -59, 87, -94, -94, 18,
            -113, -77, -60, 105, 95, -3, 70, -110, 19, -44, -15, 104, 44, 1, 31, 17, -97, 107, 59,
            5 };
    private final byte[] userValidationSalt = new byte[] { 34, 67, -1, -34, 1, 87, 3, 65 };
    private final byte[] userKeySalt = new byte[] { 23, 56, -12, 45, 91, -34, 2, 12 };
    private final byte[] ownerValidationSalt = new byte[] { 8, 45, -16, -34, 4, 76, 3, -7 };
    private final byte[] ownerKeySalt = new byte[] { 98, 101, -32, -55, 33, 1, 54, 78 };
    private final byte[] u = new byte[] { -122, -13, -23, -33, -37, -9, 107, -124, 48, 124, -24,
            116, -126, 8, 16, -12, 44, 114, 66, -96, -128, 82, -65, -71, 110, -74, -41, 15, 68, 51,
            -11, 24, 34, 67, -1, -34, 1, 87, 3, 65, 23, 56, -12, 45, 91, -34, 2, 12 };

    @Test
    public void testComputeUHash()
    {
        EncryptionContext ctx = new EncryptionContext(
                new StandardSecurity("Chuck", "Norris", StandardSecurityEncryption.AES_256, true));
        ctx.key(key);
        byte[] userBytes = ctx.security.getUserPasswordUTF();
        assertArrayEquals(
                new byte[] { -122, -13, -23, -33, -37, -9, 107, -124, 48, 124, -24, 116, -126, 8,
                        16, -12, 44, 114, 66, -96, -128, 82, -65, -71, 110, -74, -41, 15, 68, 51,
                        -11, 24 },
                new Algorithm2B().computeHash(concatenate(userBytes, userValidationSalt),
                        userBytes));
    }

    @Test
    public void testComputeUEHash()
    {
        EncryptionContext ctx = new EncryptionContext(
                new StandardSecurity("Chuck", "Norris", StandardSecurityEncryption.AES_256, true));
        ctx.key(key);
        byte[] userBytes = ctx.security.getUserPasswordUTF();
        assertArrayEquals(
                new byte[] { -108, -70, 29, -52, 102, -87, -61, -34, -16, 83, -1, 11, 124, 5, -1,
                        96, 126, 86, -98, -77, 90, -127, -9, 123, 102, -52, 48, 116, 24, 96, -116,
                        -126 },
                new Algorithm2B().computeHash(concatenate(userBytes, userKeySalt), userBytes));
    }

    @Test
    public void testComputeOHash()
    {
        EncryptionContext ctx = new EncryptionContext(
                new StandardSecurity("Chuck", "Norris", StandardSecurityEncryption.AES_256, true));
        ctx.key(key);
        byte[] ownerBytes = ctx.security.getOwnerPasswordUTF();
        assertArrayEquals(
                new byte[] { 83, 9, -124, -89, 118, -28, 30, 64, -96, -39, -100, -14, 78, -74, 104,
                        -91, -110, 77, 94, 75, -101, 62, 25, -92, -96, -52, -18, 3, 85, 27, -80,
                        -95 },
                new Algorithm2B(u).computeHash(concatenate(ownerBytes, ownerValidationSalt, u),
                        ownerBytes));
    }

    @Test
    public void testComputeOEHash()
    {
        EncryptionContext ctx = new EncryptionContext(
                new StandardSecurity("Chuck", "Norris", StandardSecurityEncryption.AES_256, true));
        ctx.key(key);
        byte[] ownerBytes = ctx.security.getOwnerPasswordUTF();
        assertArrayEquals(
                new byte[] { -16, -94, -35, 90, -104, -77, 8, -108, 106, -110, 8, -25, 37, 9, 68,
                        31, -2, -18, 47, 68, -35, -69, -80, -9, -25, 56, -65, -28, 89, -91, -19,
                        49 },
                new Algorithm2B(u).computeHash(concatenate(ownerBytes, ownerKeySalt, u),
                        ownerBytes));
    }
}
