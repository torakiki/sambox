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
 * Algorithm 4 as defined in Chap 7.6.3.4 of PDF 32000-1:2008
 * 
 * @author Andrea Vacondio
 *
 */
class Algorithm4 implements PasswordAlgorithm
{
    private ARC4Engine engine = new ARC4Engine();

    @Override
    public byte[] computePassword(EncryptionContext context)
    {
        context.security.encryption.revision.require(StandardSecurityHandlerRevision.R2,
                "Algorithm 4 requires a security handler of revision 2");

        return engine.encryptBytes(EncryptUtils.ENCRYPT_PADDING, context.key());
    }
}
