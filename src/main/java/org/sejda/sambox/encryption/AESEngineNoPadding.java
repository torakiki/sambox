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

import static java.util.Objects.nonNull;
import static org.bouncycastle.util.Arrays.copyOf;

import java.io.InputStream;

import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.AESFastEngine;
import org.bouncycastle.crypto.io.CipherInputStream;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

/**
 * AES implementation of a {@link EncryptionAlgorithmEngine} with no pudding
 * 
 * @author Andrea Vacondio
 *
 */
class AESEngineNoPadding implements AESEncryptionAlgorithmEngine
{
    private BufferedBlockCipher cipher;

    AESEngineNoPadding(BufferedBlockCipher cipher)
    {
        this.cipher = cipher;
    }

    @Override
    public InputStream encryptStream(InputStream data, byte[] key, byte[] iv)
    {
        init(key, iv);
        return new CipherInputStream(data, cipher);
    }

    @Override
    public InputStream encryptStream(InputStream data, byte[] key)
    {
        return encryptStream(data, key, null);
    }

    @Override
    public byte[] encryptBytes(byte[] data, byte[] key, byte[] iv)
    {
        init(key, iv);
        try
        {
            byte[] buf = new byte[cipher.getOutputSize(data.length)];
            int len = cipher.processBytes(data, 0, data.length, buf, 0);
            len += cipher.doFinal(buf, len);
            return copyOf(buf, len);
        }
        catch (DataLengthException | IllegalStateException | InvalidCipherTextException e)
        {
            throw new EncryptionException(e);
        }
    }

    @Override
    public byte[] encryptBytes(byte[] data, byte[] key)
    {
        return encryptBytes(data, key, null);
    }

    private void init(byte[] key, byte[] iv)
    {
        cipher.reset();
        if (nonNull(iv))
        {
            cipher.init(true, new ParametersWithIV(new KeyParameter(key), iv));
        }
        else
        {
            cipher.init(true, new KeyParameter(key));
        }
    }

    /**
     * @return and instance of EncryptionAlgorithmEngine AES/CBC/NoPadding and no initialization vector
     */
    static AESEngineNoPadding cbc()
    {
        return new AESEngineNoPadding(
                new BufferedBlockCipher(new CBCBlockCipher(new AESFastEngine())));
    }

    /**
     * @return and instance of EncryptionAlgorithmEngine AES/ECB/NoPadding and no initialization vector
     */
    static AESEngineNoPadding ecb()
    {
        return new AESEngineNoPadding(new BufferedBlockCipher(new AESFastEngine()));
    }
}
