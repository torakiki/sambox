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
public class Algorithm5Test
{

    private Algorithm5 victim = new Algorithm5();

    @Test
    public void computePasswordNoUserARC128()
    {
        EncryptionContext context = new EncryptionContext(
                new StandardSecurity("test", null, StandardSecurityEncryption.ARC4_128, true));
        context.documentId(new byte[] { -5, 78, 122, -45, 106, -102, 20, -35, -125, 7, 43, -52,
                -104, 23, -68, 113 });
        byte[] expected = new byte[] { 69, -55, -39, 93, -69, 34, -117, -17, 58, -63, -20, -18, -51,
                92, 111, 95, 40, -65, 78, 94, 78, 117, -118, 65, 100, 0, 78, 86, -1, -6, 1, 8 };
        context.key(new Algorithm2().computeEncryptionKey(context));
        assertArrayEquals(expected, victim.computePassword(context));
        assertArrayEquals(expected, victim.computePassword(context));
        assertArrayEquals(expected, victim.computePassword(context));
    }

    @Test
    public void computePasswordARC128()
    {
        EncryptionContext context = new EncryptionContext(
                new StandardSecurity("test", "userPwd", StandardSecurityEncryption.ARC4_128, true));
        EncryptionContext contextNoMeta = new EncryptionContext(new StandardSecurity("test",
                "userPwd", StandardSecurityEncryption.ARC4_128, false));

        context.documentId(new byte[] { -5, 78, 122, -45, 106, -102, 20, -35, -125, 7, 43, -52,
                -104, 23, -68, 113 });
        contextNoMeta.documentId(new byte[] { -5, 78, 122, -45, 106, -102, 20, -35, -125, 7, 43,
                -52, -104, 23, -68, 113 });

        byte[] expected = new byte[] { 47, 113, -64, 46, -100, 75, 92, -84, 63, -77, -29, -90, -78,
                108, 54, 89, 40, -65, 78, 94, 78, 117, -118, 65, 100, 0, 78, 86, -1, -6, 1, 8 };
        context.key(new Algorithm2().computeEncryptionKey(context));
        contextNoMeta.key(new Algorithm2().computeEncryptionKey(contextNoMeta));
        assertArrayEquals(expected, victim.computePassword(context));
        assertArrayEquals(expected, victim.computePassword(contextNoMeta));
    }

    @Test
    public void computePasswordAES128()
    {
        EncryptionContext context = new EncryptionContext(
                new StandardSecurity("test", "userPwd", StandardSecurityEncryption.AES_128, true));

        EncryptionContext contextNoMeta = new EncryptionContext(
                new StandardSecurity("test", "userPwd", StandardSecurityEncryption.AES_128, false));

        context.documentId(new byte[] { -5, 78, 122, -45, 106, -102, 20, -35, -125, 7, 43, -52,
                -104, 23, -68, 113 });
        contextNoMeta.documentId(new byte[] { -5, 78, 122, -45, 106, -102, 20, -35, -125, 7, 43,
                -52, -104, 23, -68, 113 });

        byte[] expected = new byte[] { 47, 113, -64, 46, -100, 75, 92, -84, 63, -77, -29, -90, -78,
                108, 54, 89, 40, -65, 78, 94, 78, 117, -118, 65, 100, 0, 78, 86, -1, -6, 1, 8 };
        byte[] expectedNoMeta = new byte[] { -20, -82, -14, 50, -114, 98, -24, 54, -23, 124, 30, 57,
                -22, 24, -91, 107, 40, -65, 78, 94, 78, 117, -118, 65, 100, 0, 78, 86, -1, -6, 1,
                8 };
        context.key(new Algorithm2().computeEncryptionKey(context));
        contextNoMeta.key(new Algorithm2().computeEncryptionKey(contextNoMeta));
        assertArrayEquals(expected, victim.computePassword(context));
        assertArrayEquals(expectedNoMeta, victim.computePassword(contextNoMeta));
    }

    @Test
    public void computePasswordNoUserAES128()
    {
        EncryptionContext context = new EncryptionContext(
                new StandardSecurity("test", null, StandardSecurityEncryption.AES_128, true));
        EncryptionContext contextNoMeta = new EncryptionContext(
                new StandardSecurity("test", null, StandardSecurityEncryption.AES_128, false));

        context.documentId(new byte[] { -5, 78, 122, -45, 106, -102, 20, -35, -125, 7, 43, -52,
                -104, 23, -68, 113 });
        contextNoMeta.documentId(new byte[] { -5, 78, 122, -45, 106, -102, 20, -35, -125, 7, 43,
                -52, -104, 23, -68, 113 });

        byte[] expected = new byte[] { 69, -55, -39, 93, -69, 34, -117, -17, 58, -63, -20, -18, -51,
                92, 111, 95, 40, -65, 78, 94, 78, 117, -118, 65, 100, 0, 78, 86, -1, -6, 1, 8 };
        byte[] expectedNoMeta = new byte[] { -36, -88, -20, 86, 52, 86, 61, 2, -22, -6, -29, -68,
                52, -3, 4, -23, 40, -65, 78, 94, 78, 117, -118, 65, 100, 0, 78, 86, -1, -6, 1, 8 };

        context.key(new Algorithm2().computeEncryptionKey(context));
        contextNoMeta.key(new Algorithm2().computeEncryptionKey(contextNoMeta));

        assertArrayEquals(expected, victim.computePassword(context));
        assertArrayEquals(expectedNoMeta, victim.computePassword(contextNoMeta));
    }
}
