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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.security.SecureRandom;

import org.bouncycastle.crypto.engines.AESFastEngine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;

/**
 * AES implementation of a {@link EncryptionAlgorithmEngine} that concatenates the results to the initialization vectors
 * as required by Algorithm 1 and 1A. A random IV is created for those methods that don't take one as parameter
 * 
 * @author Andrea Vacondio
 *
 */
public class ConcatenatingAESEngine extends AESEngineNoPadding
{
    private SecureRandom random;

    ConcatenatingAESEngine()
    {
        super(new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESFastEngine())));
        random = new SecureRandom();
    }

    @Override
    public InputStream encryptStream(InputStream data, byte[] key)
    {
        return encryptStream(data, key, initializationVector());
    }

    @Override
    public InputStream encryptStream(InputStream data, byte[] key, byte[] iv)
    {
        return new SequenceInputStream(new ByteArrayInputStream(iv),
                super.encryptStream(data, key, iv));
    }

    @Override
    public byte[] encryptBytes(byte[] data, byte[] key)
    {
        return encryptBytes(data, key, initializationVector());
    }

    @Override
    public byte[] encryptBytes(byte[] data, byte[] key, byte[] iv)
    {
        return concatenate(iv, super.encryptBytes(data, key, iv));
    }

    private byte[] initializationVector()
    {
        byte[] iv = new byte[16];
        random.nextBytes(iv);
        return iv;
    }
}
