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

import static org.bouncycastle.util.Arrays.concatenate;
import static org.bouncycastle.util.Arrays.copyOf;
import static org.sejda.util.RequireUtils.requireArg;

import java.security.SecureRandom;

/**
 * @author Andrea Vacondio
 *
 */
public final class EncryptUtils
{
    public static final byte[] ENCRYPT_PADDING = { (byte) 0x28, (byte) 0xBF, (byte) 0x4E,
            (byte) 0x5E, (byte) 0x4E, (byte) 0x75, (byte) 0x8A, (byte) 0x41, (byte) 0x64,
            (byte) 0x00, (byte) 0x4E, (byte) 0x56, (byte) 0xFF, (byte) 0xFA, (byte) 0x01,
            (byte) 0x08, (byte) 0x2E, (byte) 0x2E, (byte) 0x00, (byte) 0xB6, (byte) 0xD0,
            (byte) 0x68, (byte) 0x3E, (byte) 0x80, (byte) 0x2F, (byte) 0x0C, (byte) 0xA9,
            (byte) 0xFE, (byte) 0x64, (byte) 0x53, (byte) 0x69, (byte) 0x7A };

    private EncryptUtils()
    {
        // nothing
    }

    public static byte[] padOrTruncate(byte[] password)
    {
        byte[] padded = copyOf(password, Math.min(password.length, 32));
        if (padded.length < 32)
        {
            return concatenate(padded, copyOf(ENCRYPT_PADDING, 32 - padded.length));
        }
        return padded;
    }

    public static byte[] truncate127(byte[] password)
    {
        byte[] padded = copyOf(password, Math.min(password.length, 32));
        if (padded.length < 32)
        {
            return concatenate(padded, copyOf(ENCRYPT_PADDING, 32 - padded.length));
        }
        return padded;

    }

    public static byte[] rnd(int length)
    {
        requireArg(length > 0, "Cannot generate a negative length byte array");
        SecureRandom random = new SecureRandom();
        byte[] rnd = new byte[length];
        random.nextBytes(rnd);
        return rnd;
    }
}
