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

import static org.sejda.util.RequireUtils.requireNotNullArg;

/**
 * Context holding the current state of the encryption
 * 
 * @author Andrea Vacondio
 *
 */
public final class EncryptionContext
{

    public final StandardSecurity security;
    private byte[] documentId;
    private byte[] key;

    public EncryptionContext(StandardSecurity security)
    {
        requireNotNullArg(security, "Cannot create an encryption context with a null security");
        this.security = security;
    }

    /**
     * Sets the document ID to use in those algorithms requiring it.
     * 
     * @param documentId
     */
    public void documentId(byte[] documentId)
    {
        this.documentId = documentId;
    }

    byte[] documentId()
    {
        return documentId;
    }

    /**
     * Sets the encryption key
     * 
     * @param key
     */
    void key(byte[] key)
    {
        this.key = key;
    }

    byte[] key()
    {
        return key;
    }

    public GeneralEncryptionAlgorithm encryptionAlgorithm()
    {
        return security.encryption.encryptionAlgorithm(this);
    }
}
