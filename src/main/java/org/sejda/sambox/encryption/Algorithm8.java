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

/**
 * Algorithm 8 as defined in Chap 7.6.3.4.6 of ISO 32000-2
 * 
 * @author Andrea Vacondio
 *
 */
class Algorithm8 implements PasswordAlgorithm
{

    private byte[] userValidationSalt;
    private byte[] userKeySalt;
    private Algorithm2AHash hashAlgo;
    private AESEngineNoPadding engine = AESEngineNoPadding.cbc();

    Algorithm8(Algorithm2AHash hashAlgo)
    {
        this(hashAlgo, rnd(8), rnd(8));
    }

    Algorithm8(Algorithm2AHash hashAlgo, byte[] userValidationSalt, byte[] userKeySalt)
    {
        requireNonNull(hashAlgo);
        this.hashAlgo = hashAlgo;
        this.userValidationSalt = userValidationSalt;
        this.userKeySalt = userKeySalt;
    }

    @Override
    public byte[] computePassword(EncryptionContext context)
    {
        context.security.encryption.revision.requireAtLeast(StandardSecurityHandlerRevision.R5,
                "Algorithm 8 requires a security handler of revision 5 or greater");
        byte[] userPassword = context.security.getUserPasswordUTF();
        return concatenate(
                hashAlgo.computeHash(concatenate(userPassword, userValidationSalt), userPassword),
                userValidationSalt, userKeySalt);
    }

    public byte[] computeUE(EncryptionContext context)
    {
        context.security.encryption.revision.requireAtLeast(StandardSecurityHandlerRevision.R5,
                "Algorithm 8 requires a security handler of revision 5 or greater");
        byte[] userPassword = context.security.getUserPasswordUTF();
        byte[] key = hashAlgo.computeHash(concatenate(userPassword, userKeySalt), userPassword);
        return copyOf(engine.encryptBytes(context.key(), key), 32);
    }
}
