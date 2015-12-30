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

import java.security.MessageDigest;
import java.util.Arrays;

/**
 * Algorithm 2 as defined in Chap 7.6.3.3 of PDF 32000-1:2008
 * 
 * @author Andrea Vacondio
 *
 */
class Algorithm2
{
    private static final byte[] NO_METADATA = { (byte) 255, (byte) 255, (byte) 255, (byte) 255 };

    private MessageDigest digest = MessageDigests.md5();
    private Algorithm3 algo = new Algorithm3();


    byte[] computeEncryptionKey(StandardSecurity security)
    {
        digest.update(EncryptUtils.padOrTruncate(security.userPassword));
        digest.update(algo.computePassword(security));

        int permissions = security.permissions.getPermissionBytes();
        digest.update((byte) permissions);
        digest.update((byte) (permissions >>> 8));
        digest.update((byte) (permissions >>> 16));
        digest.update((byte) (permissions >>> 24));

        digest.update(security.documentId());
        if (StandardSecurityHandlerRevision.R4.compareTo(security.encryption.revision) <= 0
                && !security.encryptMetadata)
        {
            digest.update(NO_METADATA);
        }
        byte[] hash = digest.digest();
        if (StandardSecurityHandlerRevision.R3.compareTo(security.encryption.revision) <= 0
                && !security.encryptMetadata)
        {
            for (int i = 0; i < 50; i++)
            {
                digest.update(hash, 0, security.encryption.revision.length);
                hash = digest.digest();
            }
        }
        return Arrays.copyOf(hash, security.encryption.revision.length);
    }
}
