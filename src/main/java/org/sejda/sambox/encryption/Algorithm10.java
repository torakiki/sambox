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

/**
 * Algorithm 10 as defined in Chap 7.6.3.4.8 of ISO 32000-2
 * 
 * @author Andrea Vacondio
 */
class Algorithm10
{
    private AESEngineNoPadding engine = AESEngineNoPadding.ecb();

    byte[] computePerms(EncryptionContext context)
    {
        byte[] perms = EncryptUtils.rnd(16);
        perms[0] = (byte) context.security.permissions.getPermissionBytes();
        perms[1] = (byte) (context.security.permissions.getPermissionBytes() >>> 8);
        perms[2] = (byte) (context.security.permissions.getPermissionBytes() >>> 16);
        perms[3] = (byte) (context.security.permissions.getPermissionBytes() >>> 24);
        perms[4] = (byte) 0xFF;
        perms[5] = (byte) 0xFF;
        perms[6] = (byte) 0xFF;
        perms[7] = (byte) 0xFF;
        perms[8] = (byte) (context.security.encryptMetadata ? 'T' : 'F');
        perms[9] = 'a';
        perms[10] = 'd';
        perms[11] = 'b';
        return engine.encryptBytes(perms, context.key());
    }
}
