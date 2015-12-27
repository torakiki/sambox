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

/**
 * Encryption engine to use in general encryption algorithms as defined in Chapter 7.6.2 of the PDF 32000-1
 * 
 * @author Andrea Vacondio
 *
 */
interface EncryptionAlgorithmEngine
{
    /**
     * @param data data to encrypt
     * @param key
     * @return a stream where encrypted data can be read from
     * @throws EncryptionException
     */
    InputStream encryptStream(InputStream data, byte[] key);

    /**
     * @param data data to encrypt
     * @param key
     * @return the encrypted data
     * @throws EncryptionException
     */
    byte[] encryptBytes(byte[] data, byte[] key);
}
