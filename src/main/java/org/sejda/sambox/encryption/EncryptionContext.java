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
import static java.util.Optional.ofNullable;
import static org.sejda.commons.util.RequireUtils.requireNotNullArg;

import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Context holding the current state of the encryption
 * 
 * @author Andrea Vacondio
 *
 */
public final class EncryptionContext
{
    private static final Logger LOG = LoggerFactory.getLogger(EncryptionContext.class);
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

    /**
     * @param enc
     * @return a {@link GeneralEncryptionAlgorithm} generated using the given encryption algorithm or null if unable to
     * generate.
     */
    public static GeneralEncryptionAlgorithm encryptionAlgorithmFromEncryptionDictionary(
            COSDictionary enc, byte[] encryptionKey)
    {
        if (nonNull(enc))
        {
            if (nonNull(encryptionKey))
            {
                COSName filter = enc.getCOSName(COSName.FILTER);
                if (COSName.STANDARD.equals(filter))
                {
                    int revision = enc.getInt(COSName.R);
                    switch (revision)
                    {
                    case 2:
                    case 3:
                        return Algorithm1.withARC4Engine(encryptionKey);
                    case 4:
                        COSName cryptFilterMethod = ofNullable(
                                enc.getDictionaryObject(COSName.CF, COSDictionary.class))
                                        .map(d -> d.getDictionaryObject(COSName.STD_CF,
                                                COSDictionary.class))
                                        .map(d -> d.getCOSName(COSName.CFM)).orElse(COSName.NONE);
                        if (COSName.V2.equals(cryptFilterMethod))
                        {
                            return Algorithm1.withARC4Engine(encryptionKey);
                        }
                        else if (COSName.AESV2.equals(cryptFilterMethod))
                        {
                            return Algorithm1.withAESEngine(encryptionKey);
                        }
                        LOG.warn("Unable to determine encryption algorithm");
                        return null;
                    case 5:
                    case 6:
                        return new Algorithm1A(encryptionKey);
                    default:
                        LOG.warn(
                                "Unsupported or invalid standard security handler revision number {}",
                                enc.getDictionaryObject(COSName.R));
                        return null;
                    }
                }
                LOG.warn("Unsupported encryption filter {}", filter);
            }
            else
            {
                LOG.warn("Empty encryption key");
            }
        }
        return null;
    }

}
