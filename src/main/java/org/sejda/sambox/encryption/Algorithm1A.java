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

import static org.sejda.commons.util.RequireUtils.requireArg;
import static org.sejda.commons.util.RequireUtils.requireNotNullArg;

import org.sejda.sambox.cos.COSObjectKey;
import org.sejda.sambox.cos.COSStream;
import org.sejda.sambox.cos.COSString;

/**
 * Algorithm 1 defined in Chapter 7.6.2 (General Encryption Algorithm) PDF 32100-1:2008
 * 
 * @author Andrea Vacondio
 *
 */
class Algorithm1A implements GeneralEncryptionAlgorithm
{

    private AESEncryptionAlgorithmEngine engine = new ConcatenatingAESEngine();
    private byte[] key;

    Algorithm1A(byte[] key, AESEncryptionAlgorithmEngine engine)
    {
        this(key);
        requireNotNullArg(engine, "Enecryption engine cannot be null");
        this.engine = engine;
    }

    Algorithm1A(byte[] key)
    {
        requireNotNullArg(key, "Encryption key cannot be null");
        requireArg(key.length == 32, "General encryption algorithm 1.A requires a 32 bytes key");
        this.key = key;
    }

    @Override
    public void visit(COSString value)
    {
        if (value.encryptable())
        {
            value.setValue(engine.encryptBytes(value.getBytes(), key));
        }
    }

    @Override
    public void visit(COSStream value)
    {
        if (value.encryptable())
        {
            value.setEncryptor((i) -> engine.encryptStream(i, key));
        }
    }

    @Override
    public void setCurrentCOSObjectKey(COSObjectKey key)
    {
        // nothing
    }

    @Override
    public String toString()
    {
        return "Algorithm1A with engine " + engine;
    }
}
