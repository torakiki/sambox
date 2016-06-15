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
import static org.sejda.sambox.encryption.EncryptUtils.truncate127;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;

import org.sejda.sambox.pdmodel.encryption.AccessPermission;

/**
 * Object holding all the data necessary to specify how to encrypt the document.
 * 
 * @author Andrea Vacondio
 *
 */
public class StandardSecurity
{
    public final String ownerPassword;
    public final String userPassword;
    public final AccessPermission permissions;
    public final StandardSecurityEncryption encryption;
    public final boolean encryptMetadata;

    public StandardSecurity(String ownerPassword, String userPassword,
            StandardSecurityEncryption encryption, boolean encryptMetadata)
    {
        this(ownerPassword, userPassword, encryption, new AccessPermission(), encryptMetadata);
    }

    public StandardSecurity(String ownerPassword, String userPassword,
            StandardSecurityEncryption encryption, AccessPermission permissions,
            boolean encryptMetadata)
    {
        requireNonNull(encryption, "Encryption algorithm cannot be null");
        this.ownerPassword = Objects.toString(ownerPassword, "");
        this.userPassword = Objects.toString(userPassword, "");
        this.encryption = encryption;
        this.permissions = Optional.ofNullable(permissions).orElseGet(AccessPermission::new);
        // RC4 128 has a version 2 and encryptMetadata is true by default
        this.encryptMetadata = encryptMetadata
                || StandardSecurityEncryption.ARC4_128.equals(encryption);
    }

    /**
     * @return the UTF-8 user password bytes truncated to a length o 127
     */
    byte[] getUserPasswordUTF()
    {
        return truncate127(userPassword.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * the UTF-8 owner password bytes truncated to a length o 127
     */
    byte[] getOwnerPasswordUTF()
    {
        return truncate127(ownerPassword.getBytes(StandardCharsets.UTF_8));
    }

    byte[] getUserPassword()
    {
        return userPassword.getBytes(StandardCharsets.ISO_8859_1);
    }

    byte[] getOwnerPassword()
    {
        return ownerPassword.getBytes(StandardCharsets.ISO_8859_1);
    }

}
