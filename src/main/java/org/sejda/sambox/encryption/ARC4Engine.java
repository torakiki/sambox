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

import java.io.InputStream;

import org.bouncycastle.crypto.StreamCipher;
import org.bouncycastle.crypto.engines.RC4Engine;
import org.bouncycastle.crypto.io.CipherInputStream;
import org.bouncycastle.crypto.params.KeyParameter;

/**
 * ARC4 implementation of a {@link EncryptionAlgorithmEngine}
 * 
 * @author Andrea Vacondio
 *
 */
class ARC4Engine implements EncryptionAlgorithmEngine
{

    private StreamCipher cipher;

    public ARC4Engine()
    {
        cipher = new RC4Engine();
    }

    @Override
    public InputStream encryptStream(InputStream data, byte[] key)
    {
        init(key);
        return new CipherInputStream(data, cipher);
    }

    @Override
    public byte[] encryptBytes(byte[] data, byte[] key)
    {
        init(key);
        byte[] out = new byte[data.length];
        cipher.processBytes(data, 0, data.length, out, 0);
        return out;
    }

    private void init(byte[] key)
    {
        cipher.init(true, new KeyParameter(key));
    }
}
