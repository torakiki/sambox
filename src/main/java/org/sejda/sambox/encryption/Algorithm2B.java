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

import static org.bouncycastle.util.Arrays.concatenate;
import static org.bouncycastle.util.Arrays.copyOf;
import static org.bouncycastle.util.Arrays.copyOfRange;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Algorithm 2B as defined in Chap 7.6.3.3.3 of PDF 32000-2
 * 
 * @author Andrea Vacondio
 *
 */
class Algorithm2B implements Algorithm2AHash
{

    private static final BigInteger THREE = new BigInteger("3");
    private static final Map<Integer, MessageDigest> HASHES = new HashMap<>();

    static
    {
        HASHES.put(0, MessageDigests.sha256());
        HASHES.put(1, MessageDigests.sha384());
        HASHES.put(2, MessageDigests.sha512());
    }

    private AESEncryptionAlgorithmEngine aes128 = AESEngineNoPadding.cbc();
    private final byte[] u;

    Algorithm2B(byte[] u)
    {
        this.u = Optional.ofNullable(u).orElse(new byte[0]);
    }

    Algorithm2B()
    {
        this.u = new byte[0];
    }

    @Override
    public byte[] computeHash(byte[] input, byte[] password)
    {
        byte[] k = copyOf(HASHES.get(0).digest(input), 32);
        byte[] e = new byte[0];
        for (int round = 0; round < 64 || (e[e.length - 1] & 0xFF) > round - 32; round++)
        {
            byte[] k1Element = concatenate(password, k, u);
            byte[] k1 = new byte[0];
            for (int i = 0; i < 64; i++)
            {
                k1 = concatenate(k1, k1Element);
            }
            e = aes128.encryptBytes(k1, copyOf(k, 16), copyOfRange(k, 16, 32));
            k = HASHES.get(new BigInteger(1, copyOf(e, 16)).mod(THREE).intValue()).digest(e);
        }
        return copyOf(k, 32);
    }
}
