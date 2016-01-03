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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.security.SecureRandom;

import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.AESFastEngine;
import org.bouncycastle.crypto.io.CipherInputStream;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

/**
 * AES implementation of a {@link EncryptionAlgorithmEngine}
 * 
 * @author Andrea Vacondio
 *
 */
class AESEngine implements EncryptionAlgorithmEngine
{

    private BufferedBlockCipher cipher;
    private SecureRandom random;

    public AESEngine()
    {
        cipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESFastEngine()));
        random = new SecureRandom();
    }

    @Override
    public InputStream encryptStream(InputStream data, byte[] key)
    {
        return encryptStream(data, key, initializationVector());
    }

    InputStream encryptStream(InputStream data, byte[] key, byte[] iv)
    {
        init(iv, key);
        return new SequenceInputStream(new ByteArrayInputStream(iv),
                new CipherInputStream(data, cipher));
    }

    @Override
    public byte[] encryptBytes(byte[] data, byte[] key)
    {
        return encryptBytes(data, key, initializationVector());
    }

    // to test
    byte[] encryptBytes(byte[] data, byte[] key, byte[] iv)
    {
        init(iv, key);
        try
        {
            byte[] buf = new byte[cipher.getOutputSize(data.length)];
            int len = cipher.processBytes(data, 0, data.length, buf, 0);
            len += cipher.doFinal(buf, len);

            return concatenate(iv, copyOf(buf, len));
        }
        catch (DataLengthException | IllegalStateException | InvalidCipherTextException e)
        {
            throw new EncryptionException(e);
        }
    }

    private byte[] initializationVector()
    {
        byte[] iv = new byte[16];
        random.nextBytes(iv);
        return iv;
    }

    private void init(byte[] iv, byte[] key)
    {
        cipher.reset();
        cipher.init(true, new ParametersWithIV(new KeyParameter(key), iv));
    }
}
