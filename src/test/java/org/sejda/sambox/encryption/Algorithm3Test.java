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

public class Algorithm3Test
{

    private Algorithm3 victim = new Algorithm3();

    @Test
    public void computePasswordNoUserARC128()
    {
        byte[] expected = new byte[] { -26, 0, -20, -62, 2, -120, -83, -117, 13, 100, -87, 41, -58,
                -88, 62, -30, 81, 118, 121, -86, 2, 24, -66, -50, -22, -117, 121, -122, 114, 106,
                -116, -37 };
        assertArrayEquals(expected, victim.computePassword(new EncryptionContext(
                new StandardSecurity("test", null, StandardSecurityEncryption.ARC4_128, true))));
        assertArrayEquals(expected, victim.computePassword(new EncryptionContext(
                new StandardSecurity("test", "", StandardSecurityEncryption.ARC4_128, true))));
        assertArrayEquals(expected, victim.computePassword(new EncryptionContext(
                new StandardSecurity("test", "", StandardSecurityEncryption.ARC4_128, false))));
    }

    @Test
    public void computePasswordARC128()
    {
        byte[] expected = new byte[] { -69, -52, -57, -18, 28, -118, 67, -30, -42, 42, -71, 49, 76,
                -40, 126, -114, 127, 22, 47, -29, 40, 113, -120, 96, -21, -121, 102, -88, 126, 7,
                101, -114 };
        assertArrayEquals(expected,
                victim.computePassword(new EncryptionContext(new StandardSecurity("test", "userPwd",
                        StandardSecurityEncryption.ARC4_128, true))));
        assertArrayEquals(expected,
                victim.computePassword(new EncryptionContext(new StandardSecurity("test", "userPwd",
                        StandardSecurityEncryption.ARC4_128, false))));
    }

    @Test
    public void computePasswordAES128()
    {
        byte[] expected = new byte[] { -69, -52, -57, -18, 28, -118, 67, -30, -42, 42, -71, 49, 76,
                -40, 126, -114, 127, 22, 47, -29, 40, 113, -120, 96, -21, -121, 102, -88, 126, 7,
                101, -114 };
        byte[] expectedNoMeta = new byte[] { -69, -52, -57, -18, 28, -118, 67, -30, -42, 42, -71,
                49, 76, -40, 126, -114, 127, 22, 47, -29, 40, 113, -120, 96, -21, -121, 102, -88,
                126, 7, 101, -114 };
        assertArrayEquals(expected,
                victim.computePassword(new EncryptionContext(new StandardSecurity("test", "userPwd",
                        StandardSecurityEncryption.AES_128, true))));
        assertArrayEquals(expectedNoMeta,
                victim.computePassword(new EncryptionContext(new StandardSecurity("test", "userPwd",
                        StandardSecurityEncryption.AES_128, false))));
    }

    @Test
    public void computePasswordNoUserAES128()
    {
        byte[] expected = new byte[] { -26, 0, -20, -62, 2, -120, -83, -117, 13, 100, -87, 41, -58,
                -88, 62, -30, 81, 118, 121, -86, 2, 24, -66, -50, -22, -117, 121, -122, 114, 106,
                -116, -37 };
        byte[] expectedNoMeta = new byte[] { -26, 0, -20, -62, 2, -120, -83, -117, 13, 100, -87, 41,
                -58, -88, 62, -30, 81, 118, 121, -86, 2, 24, -66, -50, -22, -117, 121, -122, 114,
                106, -116, -37 };

        assertArrayEquals(expected, victim.computePassword(new EncryptionContext(
                new StandardSecurity("test", "", StandardSecurityEncryption.AES_128, true))));
        assertArrayEquals(expected, victim.computePassword(new EncryptionContext(
                new StandardSecurity("test", null, StandardSecurityEncryption.AES_128, true))));

        assertArrayEquals(expectedNoMeta, victim.computePassword(new EncryptionContext(
                new StandardSecurity("test", "", StandardSecurityEncryption.AES_128, false))));
        assertArrayEquals(expectedNoMeta, victim.computePassword(new EncryptionContext(
                new StandardSecurity("test", null, StandardSecurityEncryption.AES_128, false))));
    }
}
