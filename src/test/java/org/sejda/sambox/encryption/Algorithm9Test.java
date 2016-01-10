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
public class Algorithm9Test
{
    private byte[] key = new byte[] { -91, 100, 117, 33, 83, 54, -34, -59, 87, -94, -94, 18, -113,
            -77, -60, 105, 95, -3, 70, -110, 19, -44, -15, 104, 44, 1, 31, 17, -97, 107, 59, 5 };
    private byte[] ownerValidationSalt = new byte[] { 8, 45, -16, -34, 4, 76, 3, -7 };
    private byte[] ownerKeySalt = new byte[] { 98, 101, -32, -55, 33, 1, 54, 78 };
    private byte[] u = new byte[] { -122, -13, -23, -33, -37, -9, 107, -124, 48, 124, -24, 116,
            -126, 8, 16, -12, 44, 114, 66, -96, -128, 82, -65, -71, 110, -74, -41, 15, 68, 51, -11,
            24, 34, 67, -1, -34, 1, 87, 3, 65, 23, 56, -12, 45, 91, -34, 2, 12 };

    @Test
    public void testComputePassword()
    {
        Algorithm9 algo9 = new Algorithm9(new Algorithm2B(u), u, ownerValidationSalt, ownerKeySalt);
        EncryptionContext ctx = new EncryptionContext(
                new StandardSecurity("Chuck", "Norris", StandardSecurityEncryption.AES_256, true));
        ctx.key(key);
        assertArrayEquals(
                new byte[] { 83, 9, -124, -89, 118, -28, 30, 64, -96, -39, -100, -14, 78, -74, 104,
                        -91, -110, 77, 94, 75, -101, 62, 25, -92, -96, -52, -18, 3, 85, 27, -80,
                        -95, 8, 45, -16, -34, 4, 76, 3, -7, 98, 101, -32, -55, 33, 1, 54, 78 },
                algo9.computePassword(ctx));
    }

    @Test
    public void testComputeUE()
    {
        Algorithm9 algo9 = new Algorithm9(new Algorithm2B(u), u, ownerValidationSalt, ownerKeySalt);
        EncryptionContext ctx = new EncryptionContext(
                new StandardSecurity("Chuck", "Norris", StandardSecurityEncryption.AES_256, true));
        ctx.key(key);
        assertArrayEquals(new byte[] { 93, 91, -4, -90, -37, -72, -44, 107, 120, -24, 100, -123,
                -75, -4, 86, 97, 57, -66, -13, -91, -63, -81, 116, -42, -108, 89, -38, -72, 115,
                -114, 69, -22 }, algo9.computeOE(ctx));
    }
}
