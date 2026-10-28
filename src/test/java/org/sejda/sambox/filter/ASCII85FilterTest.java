/*
 * Created on 28/10/26
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
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;
import org.sejda.sambox.cos.COSDictionary;

public class ASCII85FilterTest
{
    private final ASCII85Filter victim = new ASCII85Filter();

    @Test
    public void decode() throws IOException
    {
        assertEquals("abcdefgh", decode("@:E_WAS,Rg~>"));
    }

    @Test
    public void decodePartialGroup() throws IOException
    {
        // ISO 32000-1 7.4.3: a final partial group of n + 1 characters is decoded to n bytes
        assertEquals("abcdefghij", decode("@:E_WAS,RgBkd~>"));
    }

    @Test
    public void missingEOD() throws IOException
    {
        // the end of the data behaves like the ~> EOD marker
        assertEquals("abcdefgh", decode("@:E_WAS,Rg"));
    }

    @Test
    public void missingEODPartialGroup() throws IOException
    {
        assertEquals("abcdefghij", decode("@:E_WAS,RgBkd"));
    }

    @Test
    public void streamWithoutEOD() throws IOException
    {
        // a content stream ASCII85 encoded, wrapped every 75 characters and without the ~> EOD marker
        try (var encoded = getClass().getResourceAsStream("/sambox/ascii85_no_eod.txt"))
        {
            assertEquals("""
                    BT /F1 18 Tf 20 750 Td (ASCII85 encoded content stream) Tj ET
                    BT /F1 18 Tf 20 720 Td (without the EOD marker!) Tj ET
                    """, decode(encoded));
        }
    }

    private String decode(String encoded) throws IOException
    {
        return decode(new ByteArrayInputStream(encoded.getBytes(US_ASCII)));
    }

    private String decode(InputStream encoded) throws IOException
    {
        var decoded = new ByteArrayOutputStream();
        victim.decode(encoded, decoded, new COSDictionary(), 0);
        return decoded.toString(US_ASCII);
    }
}
