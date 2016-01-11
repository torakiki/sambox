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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

import org.junit.Test;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSString;

/**
 * @author Andrea Vacondio
 *
 */
public class StandardSecurityEncryptionTest
{

    @Test
    public void aes256encryptionAlgorithm()
    {
        EncryptionContext ctx = new EncryptionContext(mock(StandardSecurity.class));
        assertNull(ctx.key());
        StandardSecurityEncryption.AES_256.encryptionAlgorithm(ctx);
        assertNotNull(ctx.key());
    }

    @Test
    public void arc4_128encryptionAlgorithm()
    {
        EncryptionContext ctx = new EncryptionContext(
                new StandardSecurity("Chuck", "Norris", StandardSecurityEncryption.ARC4_128, true));
        ctx.documentId(new byte[] { -28, -125, 59, -122, 68, -117, 76, -35, 120, -5, 115, -100,
                -122, -62, -123, 121 });
        assertNull(ctx.key());
        StandardSecurityEncryption.ARC4_128.encryptionAlgorithm(ctx);
        assertArrayEquals(new byte[] { 34, -93, -39, -90, 31, 109, -77, -83, 113, 101, 21, -10, -13,
                -22, 42, 116 }, ctx.key());
    }

    @Test
    public void aes4_128encryptionAlgorithm()
    {
        EncryptionContext ctx = new EncryptionContext(
                new StandardSecurity("Chuck", "Norris", StandardSecurityEncryption.AES_128, true));
        ctx.documentId(new byte[] { -28, -125, 59, -122, 68, -117, 76, -35, 120, -5, 115, -100,
                -122, -62, -123, 121 });
        assertNull(ctx.key());
        StandardSecurityEncryption.AES_128.encryptionAlgorithm(ctx);
        assertArrayEquals(new byte[] { 34, -93, -39, -90, 31, 109, -77, -83, 113, 101, 21, -10, -13,
                -22, 42, 116 }, ctx.key());
    }

    @Test
    public void arc4_128generateEncryptionDictionary()
    {
        EncryptionContext ctx = new EncryptionContext(
                new StandardSecurity("Chuck", "Norris", StandardSecurityEncryption.ARC4_128, true));
        ctx.documentId(new byte[] { -28, -125, 59, -122, 68, -117, 76, -35, 120, -5, 115, -100,
                -122, -62, -123, 121 });
        StandardSecurityEncryption.ARC4_128.encryptionAlgorithm(ctx);
        COSDictionary result = StandardSecurityEncryption.ARC4_128
                .generateEncryptionDictionary(ctx);
        assertEquals("Standard", result.getNameAsString(COSName.FILTER));
        assertEquals(StandardSecurityEncryption.ARC4_128.version, result.getInt(COSName.V));
        assertEquals(StandardSecurityEncryption.ARC4_128.revision.length * 8,
                result.getInt(COSName.LENGTH));
        assertEquals(StandardSecurityEncryption.ARC4_128.revision.revisionNumber,
                result.getInt(COSName.R));
        assertEquals(ctx.security.permissions.getPermissionBytes(), result.getInt(COSName.P));
        assertArrayEquals(new byte[] { 75, -127, 10, 89, -93, 64, -109, -17, -30, -57, -96, 40, -89,
                -90, -110, 28, 40, -65, 78, 94, 78, 117, -118, 65, 100, 0, 78, 86, -1, -6, 1, 8 },
                ((COSString) result.getItem(COSName.U)).getBytes());
        assertArrayEquals(new byte[] { 33, 113, -60, -103, -11, 46, -124, 21, -45, 71, 66, -122,
                108, -3, 106, -64, -88, 9, -25, 121, -46, -118, 63, -29, -65, 77, -112, 9, 32, 6,
                -118, -28 }, ((COSString) result.getItem(COSName.O)).getBytes());
    }

    @Test
    public void aes128generateEncryptionDictionary()
    {
        EncryptionContext ctx = new EncryptionContext(
                new StandardSecurity("Chuck", "Norris", StandardSecurityEncryption.AES_128, true));
        ctx.documentId(new byte[] { -28, -125, 59, -122, 68, -117, 76, -35, 120, -5, 115, -100,
                -122, -62, -123, 121 });
        StandardSecurityEncryption.AES_128.encryptionAlgorithm(ctx);
        COSDictionary result = StandardSecurityEncryption.AES_128.generateEncryptionDictionary(ctx);
        assertEquals("Standard", result.getNameAsString(COSName.FILTER));
        assertEquals(true, result.getBoolean(COSName.ENCRYPT_META_DATA, false));
        assertEquals(StandardSecurityEncryption.AES_128.version, result.getInt(COSName.V));
        assertEquals(StandardSecurityEncryption.AES_128.revision.length * 8,
                result.getInt(COSName.LENGTH));
        assertEquals(StandardSecurityEncryption.AES_128.revision.revisionNumber,
                result.getInt(COSName.R));
        assertEquals(ctx.security.permissions.getPermissionBytes(), result.getInt(COSName.P));
        assertEquals(COSName.STD_CF, result.getItem(COSName.STM_F));
        assertEquals(COSName.STD_CF, result.getItem(COSName.STR_F));
        COSDictionary cryptFilter = (COSDictionary) result.getItem(COSName.CF).getCOSObject();
        COSDictionary stdCryptFilter = (COSDictionary) cryptFilter.getItem(COSName.STD_CF)
                .getCOSObject();
        assertEquals(COSName.AESV2, stdCryptFilter.getItem(COSName.CFM));
        assertEquals(COSName.DOC_OPEN, stdCryptFilter.getItem(COSName.AUTEVENT));
        assertEquals(StandardSecurityEncryption.AES_128.revision.length,
                stdCryptFilter.getInt(COSName.LENGTH));
        assertArrayEquals(new byte[] { 75, -127, 10, 89, -93, 64, -109, -17, -30, -57, -96, 40, -89,
                -90, -110, 28, 40, -65, 78, 94, 78, 117, -118, 65, 100, 0, 78, 86, -1, -6, 1, 8 },
                ((COSString) result.getItem(COSName.U)).getBytes());
        assertArrayEquals(new byte[] { 33, 113, -60, -103, -11, 46, -124, 21, -45, 71, 66, -122,
                108, -3, 106, -64, -88, 9, -25, 121, -46, -118, 63, -29, -65, 77, -112, 9, 32, 6,
                -118, -28 }, ((COSString) result.getItem(COSName.O)).getBytes());
    }

    @Test
    public void aes256generateEncryptionDictionary()
    {
        EncryptionContext ctx = new EncryptionContext(
                new StandardSecurity("Chuck", "Norris", StandardSecurityEncryption.AES_256, true));
        ctx.documentId(new byte[] { -28, -125, 59, -122, 68, -117, 76, -35, 120, -5, 115, -100,
                -122, -62, -123, 121 });
        StandardSecurityEncryption.AES_256.encryptionAlgorithm(ctx);
        COSDictionary result = StandardSecurityEncryption.AES_256.generateEncryptionDictionary(ctx);
        assertEquals("Standard", result.getNameAsString(COSName.FILTER));
        assertEquals(true, result.getBoolean(COSName.ENCRYPT_META_DATA, false));
        assertEquals(StandardSecurityEncryption.AES_256.version, result.getInt(COSName.V));
        assertEquals(StandardSecurityEncryption.AES_256.revision.length * 8,
                result.getInt(COSName.LENGTH));
        assertEquals(StandardSecurityEncryption.AES_256.revision.revisionNumber,
                result.getInt(COSName.R));
        assertEquals(ctx.security.permissions.getPermissionBytes(), result.getInt(COSName.P));
        assertEquals(COSName.STD_CF, result.getItem(COSName.STM_F));
        assertEquals(COSName.STD_CF, result.getItem(COSName.STR_F));
        COSDictionary cryptFilter = (COSDictionary) result.getItem(COSName.CF).getCOSObject();
        COSDictionary stdCryptFilter = (COSDictionary) cryptFilter.getItem(COSName.STD_CF)
                .getCOSObject();
        assertEquals(COSName.AESV3, stdCryptFilter.getItem(COSName.CFM));
        assertEquals(COSName.DOC_OPEN, stdCryptFilter.getItem(COSName.AUTEVENT));
        assertEquals(StandardSecurityEncryption.AES_256.revision.length,
                stdCryptFilter.getInt(COSName.LENGTH));
        assertNotNull(result.getItem(COSName.U));
        assertNotNull(result.getItem(COSName.UE));
        assertNotNull(result.getItem(COSName.O));
        assertNotNull(result.getItem(COSName.OE));
        assertNotNull(result.getItem(COSName.PERMS));
    }

}
