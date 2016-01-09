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

import static java.util.Optional.of;
import static org.sejda.sambox.encryption.EncryptUtils.padOrTruncate;

import java.security.MessageDigest;

import org.bouncycastle.util.Arrays;

/**
 * Algorithm 3 as defined in Chap 7.6.3.4 of PDF 32000-1:2008
 * 
 * @author Andrea Vacondio
 *
 */
class Algorithm3 implements PasswordAlgorithm
{
    private MessageDigest digest = MessageDigests.md5();
    private ARC4Engine engine = new ARC4Engine();

    @Override
    public byte[] computePassword(EncryptionContext context)
    {
        byte[] ownerBytes = context.security.getOwnerPassword();
        byte[] userBytes = context.security.getUserPassword();
        byte[] padded = padOrTruncate(
                of(ownerBytes).filter(p -> p.length > 0).orElseGet(() -> userBytes));
        byte[] paddedUser = padOrTruncate(userBytes);
        digest.reset();
        byte[] arc4Key = digest.digest(padded);
        if (StandardSecurityHandlerRevision.R3.compareTo(context.security.encryption.revision) <= 0)
        {
            for (int i = 0; i < 50; ++i)
            {
                digest.update(arc4Key, 0, context.security.encryption.revision.length);
                arc4Key = Arrays.copyOf(digest.digest(),
                        context.security.encryption.revision.length);
            }
            byte[] encrypted = engine.encryptBytes(paddedUser, arc4Key);
            byte[] iterationKey = new byte[arc4Key.length];
            for (int i = 1; i < 20; i++)
            {
                iterationKey = Arrays.copyOf(arc4Key, arc4Key.length);
                for (int j = 0; j < iterationKey.length; j++)
                {
                    iterationKey[j] = (byte) (iterationKey[j] ^ (byte) i);
                }
                encrypted = engine.encryptBytes(encrypted, iterationKey);
            }
            return encrypted;
        }
        return engine.encryptBytes(paddedUser, arc4Key);
    }
}
