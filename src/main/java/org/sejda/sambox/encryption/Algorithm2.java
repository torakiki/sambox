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

    byte[] computeEncryptionKey(EncryptionContext context)
    {
        digest.reset();
        digest.update(EncryptUtils.padOrTruncate(context.security.getUserPassword()));
        digest.update(algo.computePassword(context));

        int permissions = context.security.permissions.getPermissionBytes();
        digest.update((byte) permissions);
        digest.update((byte) (permissions >>> 8));
        digest.update((byte) (permissions >>> 16));
        digest.update((byte) (permissions >>> 24));

        digest.update(context.documentId());
        if (StandardSecurityHandlerRevision.R4.compareTo(context.security.encryption.revision) <= 0
                && !context.security.encryptMetadata)
        {
            digest.update(NO_METADATA);
        }
        byte[] hash = digest.digest();
        if (StandardSecurityHandlerRevision.R3.compareTo(context.security.encryption.revision) <= 0)
        {
            for (int i = 0; i < 50; i++)
            {
                digest.update(hash, 0, context.security.encryption.revision.length);
                hash = digest.digest();
            }
        }
        return Arrays.copyOf(hash, context.security.encryption.revision.length);
    }
}
