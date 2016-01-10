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
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for creating MessageDigest instances.
 * 
 * @author John Hewson
 */
public final class MessageDigests
{
    private MessageDigests()
    {
    }

    /**
     * @return MD5 message digest
     */
    public static MessageDigest md5()
    {
        return get("MD5");

    }

    /**
     * @return SHA-1 message digest
     */
    public static MessageDigest sha1()
    {
        return get("SHA-1");
    }

    /**
     * @return SHA-256 message digest
     */
    public static MessageDigest sha256()
    {
        return get("SHA-256");
    }

    /**
     * @return SHA-384 message digest
     */
    public static MessageDigest sha384()
    {
        return get("SHA-384");
    }

    /**
     * @return SHA-512 message digest
     */
    public static MessageDigest sha512()
    {
        return get("SHA-512");
    }

    private static MessageDigest get(String name)
    {
        try
        {
            return MessageDigest.getInstance(name);
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new EncryptionException(e);
        }
    }
}
