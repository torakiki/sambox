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

import static java.util.Objects.isNull;
import static org.bouncycastle.util.Arrays.concatenate;
import static org.sejda.commons.util.RequireUtils.requireArg;
import static org.sejda.commons.util.RequireUtils.requireNotNullArg;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.function.Function;

import org.bouncycastle.crypto.engines.AESEngine;
import org.sejda.sambox.cos.COSObjectKey;
import org.sejda.sambox.cos.COSStream;
import org.sejda.sambox.cos.COSString;

/**
 * Algorithm 1 defined in Chapter 7.6.2 (General Encryption Algorithm) PDF 32100-1:2008
 * 
 * @author Andrea Vacondio
 *
 */
class Algorithm1 implements GeneralEncryptionAlgorithm
{

    private static final byte[] AES_SALT = { (byte) 0x73, (byte) 0x41, (byte) 0x6c, (byte) 0x54 };

    private EncryptionAlgorithmEngine engine;
    private MessageDigest digest = MessageDigests.md5();
    private Function<COSObjectKey, byte[]> keyCalculator;
    private Function<byte[], byte[]> md5Initializer;
    private Function<byte[], byte[]> md5ToKey;
    private COSObjectKey currentCOSObjectKey;

    private Algorithm1(EncryptionAlgorithmEngine engine, byte[] key)
    {
        requireNotNullArg(engine, "Encryption engine cannot be null");
        requireArg(key != null && key.length > 0, "Encryption key cannot be blank");
        this.engine = engine;
        keyCalculator = (cosKey) -> {
            requireNotNullArg(cosKey, "Cannot encrypt a reference with a null key");
            byte[] append = new byte[5];
            append[0] = (byte) (cosKey.objectNumber() & 0xff);
            append[1] = (byte) (cosKey.objectNumber() >> 8 & 0xff);
            append[2] = (byte) (cosKey.objectNumber() >> 16 & 0xff);
            append[3] = (byte) (cosKey.generation() & 0xff);
            append[4] = (byte) (cosKey.generation() >> 8 & 0xff);
            return concatenate(key, append);
        };
        md5Initializer = (newKey) -> {
            digest.reset();
            digest.update(newKey);
            return newKey;
        };
        md5ToKey = (newKey) -> {
            return Arrays.copyOf(digest.digest(), Math.min(newKey.length, 16));
        };
    }

    @Override
    public void setCurrentCOSObjectKey(COSObjectKey currentCOSObjectKey)
    {
        this.currentCOSObjectKey = currentCOSObjectKey;
    }

    @Override
    public void visit(COSString value)
    {
        if (value.encryptable())
        {
            requireObjectKey();
            value.setValue(engine.encryptBytes(value.getBytes(), keyCalculator
                    .andThen(md5Initializer).andThen(md5ToKey).apply(currentCOSObjectKey)));
        }
    }

    @Override
    public void visit(COSStream value)
    {
        if (value.encryptable())
        {
            requireObjectKey();
            value.setEncryptor((i) -> engine.encryptStream(i, keyCalculator.andThen(md5Initializer)
                    .andThen(md5ToKey).apply(currentCOSObjectKey)));
        }
    }

    private void requireObjectKey()
    {
        if (isNull(currentCOSObjectKey))
        {
            throw new EncryptionException(
                    "General encryption algorithm 1 requires object number and generation number");
        }
    }

    @Override
    public String toString()
    {
        return "Algorithm1 with engine " + engine;
    }

    /**
     * Factory method for an {@link Algorithm1} with an {@link AESEngine}
     * 
     * @param key
     * @return
     */
    static Algorithm1 withAESEngine(byte[] key)
    {
        Algorithm1 algorithm = new Algorithm1(new ConcatenatingAESEngine(), key);
        algorithm.md5Initializer = algorithm.md5Initializer.andThen(k -> {
            algorithm.digest.update(AES_SALT);
            return k;
        });
        return algorithm;
    }

    /**
     * Factory method for an {@link Algorithm1} with an {@link ARC4Engine}
     * 
     * @param key
     * @return
     */
    static Algorithm1 withARC4Engine(byte[] key)
    {
        return new Algorithm1(new ARC4Engine(), key);
    }

}
