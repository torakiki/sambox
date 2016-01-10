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

import java.security.MessageDigest;

import org.bouncycastle.util.Arrays;

/**
 * The algorithm used to compute hash in Algorithm 2.A for extension level 3. This is used with Version 5 and Revision 5
 * of the security handler.
 * 
 * @author Andrea Vacondio
 */
class Algorithm2BExtensionLevel3 implements Algorithm2AHash
{

    private MessageDigest digest = MessageDigests.sha256();

    @Override
    public byte[] computeHash(byte[] input, byte[] password)
    {
        return Arrays.copyOf(digest.digest(input), 32);
    }
}
