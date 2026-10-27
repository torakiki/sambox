/*
 * Created on 27/10/26
 * Copyright 2026 by Sober Lemur S.r.l. (info@soberlemur.com).
 *
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
package org.sejda.sambox.filter;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;
import org.sejda.sambox.cos.COSDictionary;

public class ASCIIHexFilterTest
{
    private final ASCIIHexFilter victim = new ASCIIHexFilter();

    @Test
    public void decode() throws IOException
    {
        assertArrayEquals(new byte[] { 0x41, 0x42, (byte) 0xFF }, decode("4142ff>"));
    }

    @Test
    public void whitespacesBetweenDigitsAreIgnored() throws IOException
    {
        // ISO 32000-1 7.4.2: all white-space characters shall be ignored
        assertArrayEquals(new byte[] { 0x41, 0x42, 0x43 }, decode("4 1\n4\r\n2 4\t\f3>"));
    }

    @Test
    public void oddDigitsBeforeEOD() throws IOException
    {
        // ISO 32000-1 7.4.2: an odd number of digits behaves as if a 0 followed the last digit
        assertArrayEquals(new byte[] { 0x41, 0x40 }, decode("41 4 >"));
    }

    @Test
    public void streamWrappedBetweenDigits() throws IOException
    {
        // a content stream ASCIIHex encoded and wrapped every 75 digits, line breaks fall between the digits of a byte
        try (var encoded = getClass().getResourceAsStream("/sambox/ascii_hex_wrapped.txt"))
        {
            assertEquals("""
                    BT /F1 18 Tf 20 750 Td (Hex encoded content stream) Tj ET
                    BT /F1 18 Tf 20 720 Td (wrapped between the digits of a byte) Tj ET
                    """, new String(decode(encoded), US_ASCII));
        }
    }

    private byte[] decode(String encoded) throws IOException
    {
        return decode(new ByteArrayInputStream(encoded.getBytes(US_ASCII)));
    }

    private byte[] decode(InputStream encoded) throws IOException
    {
        var decoded = new ByteArrayOutputStream();
        victim.decode(encoded, decoded, new COSDictionary(), 0);
        return decoded.toByteArray();
    }
}
