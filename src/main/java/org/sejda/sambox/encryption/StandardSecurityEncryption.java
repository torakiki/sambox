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
        public COSDictionary generateEncryptionDictionary(EncryptionContext context)
        {
            COSDictionary encryptionDictionary = super.generateEncryptionDictionary(context);
            encryptionDictionary.setItem(COSName.O,
                    pwdString(new Algorithm3().computePassword(context)));
            encryptionDictionary.setItem(COSName.U,
                    pwdString(new Algorithm5().computePassword(context)));
            return encryptionDictionary;
        }

        @Override
        public GeneralEncryptionAlgorithm encryptionAlgorithm(EncryptionContext context)
        {
            context.key(new Algorithm2().computeEncryptionKey(context));
            return Algorithm1.withARC4Engine(context.key());
        }
    },
    AES_128(StandardSecurityHandlerRevision.R4, 4)
    {
        @Override
        public COSDictionary generateEncryptionDictionary(EncryptionContext context)
        {
            COSDictionary encryptionDictionary = super.generateEncryptionDictionary(context);
            encryptionDictionary.setItem(COSName.O,
                    pwdString(new Algorithm3().computePassword(context)));
            encryptionDictionary.setItem(COSName.U,
                    pwdString(new Algorithm5().computePassword(context)));
            encryptionDictionary.setBoolean(COSName.ENCRYPT_META_DATA,
                    context.security.encryptMetadata);
            COSDictionary standardCryptFilterDictionary = new COSDictionary();
            standardCryptFilterDictionary.setItem(COSName.CFM, COSName.AESV2);
            standardCryptFilterDictionary.setItem(COSName.AUTEVENT, COSName.DOC_OPEN);
            standardCryptFilterDictionary.setInt(COSName.LENGTH, revision.length);
            COSDictionary cryptFilterDictionary = new COSDictionary();
            cryptFilterDictionary.setItem(COSName.STD_CF,
                    asDirectObject(standardCryptFilterDictionary));
            encryptionDictionary.setItem(COSName.CF, asDirectObject(cryptFilterDictionary));
            encryptionDictionary.setItem(COSName.STM_F, COSName.STD_CF);
            encryptionDictionary.setItem(COSName.STR_F, COSName.STD_CF);
            return encryptionDictionary;
        }

        @Override
        public GeneralEncryptionAlgorithm encryptionAlgorithm(EncryptionContext context)
        {
            context.key(new Algorithm2().computeEncryptionKey(context));
            return Algorithm1.withAESEngine(context.key());
        }
    },
    AES_256(StandardSecurityHandlerRevision.R6, 5)
    {

        @Override
        public COSDictionary generateEncryptionDictionary(EncryptionContext context)
        {
            COSDictionary encryptionDictionary = super.generateEncryptionDictionary(context);
            encryptionDictionary.setInt(COSName.R, this.revision.revisionNumber);
            Algorithm8 algo8 = new Algorithm8(new Algorithm2B());
            byte[] u = algo8.computePassword(context);
            encryptionDictionary.setItem(COSName.U, pwdString(u));
            encryptionDictionary.setItem(COSName.UE, pwdString(algo8.computeUE(context)));
            Algorithm9 algo9 = new Algorithm9(new Algorithm2B(u), u);
            encryptionDictionary.setItem(COSName.O, pwdString(algo9.computePassword(context)));
            encryptionDictionary.setItem(COSName.OE, pwdString(algo9.computeOE(context)));
            encryptionDictionary.setBoolean(COSName.ENCRYPT_META_DATA,
                    context.security.encryptMetadata);
            encryptionDictionary.setItem(COSName.PERMS,
                    pwdString(new Algorithm10().computePerms(context)));
            COSDictionary standardCryptFilterDictionary = new COSDictionary();
            standardCryptFilterDictionary.setItem(COSName.CFM, COSName.AESV3);
            standardCryptFilterDictionary.setItem(COSName.AUTEVENT, COSName.DOC_OPEN);
            standardCryptFilterDictionary.setInt(COSName.LENGTH, revision.length);
            COSDictionary cryptFilterDictionary = new COSDictionary();
            cryptFilterDictionary.setItem(COSName.STD_CF,
                    asDirectObject(standardCryptFilterDictionary));
            encryptionDictionary.setItem(COSName.CF, asDirectObject(cryptFilterDictionary));
            encryptionDictionary.setItem(COSName.STM_F, COSName.STD_CF);
            encryptionDictionary.setItem(COSName.STR_F, COSName.STD_CF);
            return encryptionDictionary;
        }

        @Override
        public GeneralEncryptionAlgorithm encryptionAlgorithm(EncryptionContext context)
        {
            context.key(EncryptUtils.rnd(32));
            return new Algorithm1A(context.key());
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
     * @param context
     * @return
     */
    public COSDictionary generateEncryptionDictionary(EncryptionContext context)
    {
        COSDictionary encryptionDictionary = new COSDictionary();
        encryptionDictionary.setItem(COSName.FILTER, COSName.STANDARD);
        encryptionDictionary.setInt(COSName.V, this.version);
        encryptionDictionary.setInt(COSName.LENGTH, this.revision.length * 8);
        encryptionDictionary.setInt(COSName.R, this.revision.revisionNumber);
        encryptionDictionary.setInt(COSName.P, context.security.permissions.getPermissionBytes());
        return encryptionDictionary;
    }

    public abstract GeneralEncryptionAlgorithm encryptionAlgorithm(EncryptionContext context);

    private static COSString pwdString(byte[] raw)
    {
        COSString string = new COSString(raw);
        string.encryptable(false);
        string.setForceHexForm(true);
        return string;
    }
}
