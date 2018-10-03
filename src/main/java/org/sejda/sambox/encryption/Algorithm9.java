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

import static java.util.Objects.requireNonNull;
import static org.bouncycastle.util.Arrays.concatenate;
import static org.bouncycastle.util.Arrays.copyOf;
import static org.sejda.sambox.encryption.EncryptUtils.rnd;
import static org.sejda.commons.util.RequireUtils.requireArg;

/**
 * Algorithm 9 as defined in Chap 7.6.3.4.7 of ISO 32000-2
 * 
 * @author Andrea Vacondio
 */
public class Algorithm9 implements PasswordAlgorithm
{
    private byte[] ownerValidationSalt;
    private byte[] ownerKeySalt;
    private byte[] u;
    private Algorithm2AHash hashAlgo;
    private AESEngineNoPadding engine = AESEngineNoPadding.cbc();

    Algorithm9(Algorithm2AHash hashAlgo, byte[] u, byte[] ownerValidationSalt, byte[] ownerKeySalt)
    {
        requireNonNull(hashAlgo);
        requireNonNull(u);
        requireArg(u.length == 48, "Generated U string must be 48 bytes long");
        this.hashAlgo = hashAlgo;
        this.u = u;
        this.ownerValidationSalt = ownerValidationSalt;
        this.ownerKeySalt = ownerKeySalt;
    }

    Algorithm9(Algorithm2AHash hashAlgo, byte[] u)
    {
        this(hashAlgo, u, rnd(8), rnd(8));
    }

    @Override
    public byte[] computePassword(EncryptionContext context)
    {
        context.security.encryption.revision.requireAtLeast(StandardSecurityHandlerRevision.R5,
                "Algorithm 9 requires a security handler of revision 5 or greater");
        byte[] ownerBytes = context.security.getOwnerPasswordUTF();
        return concatenate(
                hashAlgo.computeHash(concatenate(ownerBytes, ownerValidationSalt, u), ownerBytes),
                ownerValidationSalt, ownerKeySalt);
    }

    public byte[] computeOE(EncryptionContext context)
    {
        context.security.encryption.revision.requireAtLeast(StandardSecurityHandlerRevision.R5,
                "Algorithm 8 requires a security handler of revision 5 or greater");
        byte[] ownerBytes = context.security.getOwnerPasswordUTF();
        byte[] key = hashAlgo.computeHash(concatenate(ownerBytes, ownerKeySalt, u), ownerBytes);
        return copyOf(engine.encryptBytes(context.key(), key), 32);
    }
}
