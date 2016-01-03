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

import static org.sejda.sambox.cos.DirectCOSObject.asDirectObject;

import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSString;

/**
 * Available standard security encryptions
 * 
 * @author Andrea Vacondio
 *
 */
public enum StandardSecurityEncryption
{
    ARC4_128(StandardSecurityHandlerRevision.R3, 2)
    {
        @Override
        public COSDictionary generateEncryptionDictionary(StandardSecurity security)
        {
            COSDictionary encryptionDictionary = super.generateEncryptionDictionary(security);
            encryptionDictionary.setItem(COSName.O,
                    pwdString(new Algorithm3().computePassword(security)));
            encryptionDictionary.setItem(COSName.U,
                    pwdString(new Algorithm5().computePassword(security)));
            return encryptionDictionary;
        }

        @Override
        public GeneralEncryptionAlgorithm encryptionAlgorithm(StandardSecurity security)
        {
            return Algorithm1.withARC4Engine(new Algorithm2().computeEncryptionKey(security));
        }
    },
    AES_128(StandardSecurityHandlerRevision.R4, 4)
    {
        @Override
        public COSDictionary generateEncryptionDictionary(StandardSecurity security)
        {
            COSDictionary encryptionDictionary = super.generateEncryptionDictionary(security);
            encryptionDictionary.setItem(COSName.O,
                    pwdString(new Algorithm3().computePassword(security)));
            encryptionDictionary.setItem(COSName.U,
                    pwdString(new Algorithm5().computePassword(security)));
            encryptionDictionary.setBoolean(COSName.ENCRYPT_META_DATA, security.encryptMetadata);
            COSDictionary standardCryptFilterDictionary = new COSDictionary();
            standardCryptFilterDictionary.setItem(COSName.CFM, COSName.AESV2);
            standardCryptFilterDictionary.setItem(COSName.AUTEVENT, COSName.DOC_OPEN);
            COSDictionary cryptFilterDictionary = new COSDictionary();
            cryptFilterDictionary.setItem(COSName.STD_CF,
                    asDirectObject(standardCryptFilterDictionary));
            encryptionDictionary.setItem(COSName.CF, asDirectObject(cryptFilterDictionary));
            encryptionDictionary.setItem(COSName.STM_F, COSName.STD_CF);
            encryptionDictionary.setItem(COSName.STR_F, COSName.STD_CF);
            return encryptionDictionary;
        }

        @Override
        public GeneralEncryptionAlgorithm encryptionAlgorithm(StandardSecurity security)
        {
            return Algorithm1.withAESEngine(new Algorithm2().computeEncryptionKey(security));
        }
    },
    AES_256(StandardSecurityHandlerRevision.R6, 5)
    {
        @Override
        public COSDictionary generateEncryptionDictionary(StandardSecurity security)
        {
            COSDictionary encryptionDictionary = super.generateEncryptionDictionary(security);
            encryptionDictionary.setInt(COSName.R, this.revision.revisionNumber);
            // TODO
            return encryptionDictionary;
        }

        @Override
        public GeneralEncryptionAlgorithm encryptionAlgorithm(StandardSecurity security)
        {
            // TODO Algo 2A
            return null;
        }
    };

    public final int version;
    public final StandardSecurityHandlerRevision revision;

    private StandardSecurityEncryption(StandardSecurityHandlerRevision revision, int version)
    {
        this.revision = revision;
        this.version = version;
    }

    /**
     * Generates the encryption dictionary for this standard encryption
     * 
     * @param security
     * @return
     */
    public COSDictionary generateEncryptionDictionary(StandardSecurity security)
    {
        COSDictionary encryptionDictionary = new COSDictionary();
        encryptionDictionary.setName(COSName.FILTER, "Standard");
        encryptionDictionary.setInt(COSName.V, this.version);
        encryptionDictionary.setInt(COSName.LENGTH, this.revision.length * 8);
        encryptionDictionary.setInt(COSName.R, this.revision.revisionNumber);
        encryptionDictionary.setInt(COSName.P, security.permissions.getPermissionBytes());
        return encryptionDictionary;
    }

    public abstract GeneralEncryptionAlgorithm encryptionAlgorithm(StandardSecurity security);

    private static COSString pwdString(byte[] raw)
    {
        COSString string = new COSString(raw);
        string.encryptable(false);
        string.setForceHexForm(true);
        return string;
    }
}
