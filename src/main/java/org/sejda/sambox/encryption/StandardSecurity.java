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

import java.util.Objects;

import org.sejda.sambox.pdmodel.encryption.AccessPermission;
import org.sejda.sambox.util.Charsets;

/**
 * Object holding all the data necessary to specify how to encrypt the document.
 * 
 * @author Andrea Vacondio
 *
 */
public class StandardSecurity
{
    public final byte[] ownerPassword;
    public final byte[] userPassword;
    public final AccessPermission permissions = new AccessPermission();
    public final StandardSecurityEncryption encryption;
    private byte[] documentId;
    public final boolean encryptMetadata;

    public StandardSecurity(String ownerPassword, String userPassword,
            StandardSecurityEncryption encryption, boolean encryptMetadata)
    {
        requireNonNull(encryption, "Encryption algorithm cannot be null");
        this.ownerPassword = Objects.toString(ownerPassword, "").getBytes(Charsets.ISO_8859_1);
        this.userPassword = Objects.toString(userPassword, "").getBytes(Charsets.ISO_8859_1);
        this.encryption = encryption;
        this.encryptMetadata = encryptMetadata;
    }

    /**
     * Sets the document ID to use in those algorithms requiring it. There is no need to set this since it's going to be
     * automatically set when the document is written out.
     * 
     * @param documentId
     */
    public void documentId(byte[] documentId)
    {
        this.documentId = documentId;
    }

    public byte[] documentId()
    {
        return documentId;
    }

}
