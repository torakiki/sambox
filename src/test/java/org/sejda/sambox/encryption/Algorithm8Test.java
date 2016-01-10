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
public class Algorithm8Test
{

    private byte[] key = new byte[] { -91, 100, 117, 33, 83, 54, -34, -59, 87, -94, -94, 18, -113,
            -77, -60, 105, 95, -3, 70, -110, 19, -44, -15, 104, 44, 1, 31, 17, -97, 107, 59, 5 };
    private byte[] userValidationSalt = new byte[] { 34, 67, -1, -34, 1, 87, 3, 65 };
    private byte[] userKeySalt = new byte[] { 23, 56, -12, 45, 91, -34, 2, 12 };

    @Test
    public void testComputePassword()
    {
        EncryptionContext ctx = new EncryptionContext(
                new StandardSecurity("Chuck", "Norris", StandardSecurityEncryption.AES_256, true));
        ctx.key(key);
        assertArrayEquals(
                new byte[] { -122, -13, -23, -33, -37, -9, 107, -124, 48, 124, -24, 116, -126, 8,
                        16, -12, 44, 114, 66, -96, -128, 82, -65, -71, 110, -74, -41, 15, 68, 51,
                        -11, 24, 34, 67, -1, -34, 1, 87, 3, 65, 23, 56, -12, 45, 91, -34, 2, 12 },
                new Algorithm8(new Algorithm2B(), userValidationSalt, userKeySalt)
                        .computePassword(ctx));
    }

    @Test
    public void testComputeUE()
    {
        EncryptionContext ctx = new EncryptionContext(
                new StandardSecurity("Chuck", "Norris", StandardSecurityEncryption.AES_256, true));
        ctx.key(key);
        assertArrayEquals(
                new byte[] { -52, -35, -121, -101, 51, 33, -54, -116, -31, 126, 40, 7, -65, -94,
                        125, 37, 51, -2, 78, 126, -87, 48, 113, -62, 118, -17, -113, -124, -17, 0,
                        -37, -53 },
                new Algorithm8(new Algorithm2B(), userValidationSalt, userKeySalt).computeUE(ctx));
    }

}
