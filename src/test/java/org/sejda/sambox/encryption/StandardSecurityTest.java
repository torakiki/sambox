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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.sejda.sambox.util.Charsets;

/**
 * @author Andrea Vacondio
 *
 */
public class StandardSecurityTest
{

    @Test(expected = NullPointerException.class)
    public void nullEncAlgo()
    {
        new StandardSecurity("", "", null, true);
    }

    @Test
    public void metaIsTrueForARC4()
    {
        assertTrue(new StandardSecurity("", "", StandardSecurityEncryption.ARC4_128,
                false).encryptMetadata);
    }

    @Test
    public void userAndOwnerDefault()
    {
        StandardSecurity victim = new StandardSecurity(null, null,
                StandardSecurityEncryption.ARC4_128, false);
        assertEquals("", victim.userPassword);
        assertEquals("", victim.ownerPassword);
    }

    @Test
    public void asciiPwd()
    {
        StandardSecurity victim = new StandardSecurity("Chuck", "Norris",
                StandardSecurityEncryption.ARC4_128, false);
        assertArrayEquals("Chuck".getBytes(Charsets.ISO_8859_1), victim.getOwnerPassword());
        assertArrayEquals("Norris".getBytes(Charsets.ISO_8859_1), victim.getUserPassword());
    }

    @Test
    public void utf8Pwd()
    {
        StandardSecurity victim = new StandardSecurity("Ĉħư¢қ", "Ñọŗґǐŝ",
                StandardSecurityEncryption.ARC4_128, false);
        assertArrayEquals("Ĉħư¢қ".getBytes(Charsets.UTF_8), victim.getOwnerPasswordUTF());
        assertArrayEquals("Ñọŗґǐŝ".getBytes(Charsets.UTF_8), victim.getUserPasswordUTF());
    }
}
