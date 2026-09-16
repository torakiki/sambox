/*
 * Created on 04/09/26
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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.sejda.commons.util.RequireUtils.requireNotNullArg;
import static org.sejda.sambox.cos.COSDictionary.of;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.sejda.commons.util.IOUtils;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;

/**
 * @author Andrea Vacondio
 */
public class BrotliFilterTest
{
    private final BrotliFilter victim = new BrotliFilter();

    private byte[] resource(String name) throws IOException
    {
        try (InputStream in = getClass().getResourceAsStream("/sambox/" + name))
        {
            requireNotNullArg(in, "Missing test resource " + name);
            return IOUtils.toByteArray(in);
        }
    }

    @Test
    public void filterFactoryReturnsBrotliFilterForBrotliDecodeName() throws IOException
    {
        assertInstanceOf(BrotliFilter.class,
                FilterFactory.INSTANCE.getFilter(COSName.BROTLI_DECODE));
    }

    @Test
    public void decodesARealBrotliStreamProducedByTheReferenceImplementation() throws IOException
    {
        String expected =
                "This document introduces a new filter BrotliDecode representing stream data "
                        + "that is compressed using Brotli as defined in IETF RFC 7932. No additional "
                        + "headers or encodings are used.";

        ByteArrayOutputStream decoded = new ByteArrayOutputStream();
        victim.decode(new ByteArrayInputStream(resource("brotli-spec-snippet.br")), decoded,
                new COSDictionary(), 0);

        assertEquals(expected, decoded.toString(StandardCharsets.US_ASCII));
    }

    @Test
    public void decodesAnEmptyBrotliStream() throws IOException
    {
        ByteArrayOutputStream decoded = new ByteArrayOutputStream();
        victim.decode(new ByteArrayInputStream(resource("brotli-empty.br")), decoded,
                new COSDictionary(), 0);

        assertEquals(0, decoded.size());
    }

    @Test
    public void appliesThePngUpPredictorToTheDecodedBrotliStream() throws IOException
    {
        // brotli-predictor.br decompresses to two PNG-predicted rows (Colors=1, BitsPerComponent=8,
        // Columns=4): row 1 uses filter type "None" for pixels [10,20,30,40], row 2 uses filter type
        // "Up" to encode pixels [15,25,35,45] relative to row 1.
        COSDictionary decodeParms = new COSDictionary();
        decodeParms.setInt(COSName.PREDICTOR, 15);
        decodeParms.setInt(COSName.COLORS, 1);
        decodeParms.setInt(COSName.BITS_PER_COMPONENT, 8);
        decodeParms.setInt(COSName.COLUMNS, 4);
        COSDictionary parameters = of(COSName.FILTER, COSName.BROTLI_DECODE, COSName.DECODE_PARMS,
                decodeParms);

        ByteArrayOutputStream decoded = new ByteArrayOutputStream();
        victim.decode(new ByteArrayInputStream(resource("brotli-predictor.br")), decoded,
                parameters, 0);

        assertArrayEquals(new byte[] { 10, 20, 30, 40, 15, 25, 35, 45 }, decoded.toByteArray());
    }

    @Test
    public void flushesATrailingIncompletePredictorRow() throws IOException
    {
        // brotli-predictor-partial-row.br decompresses to a complete PNG-predicted row (pixels
        // [10,20,30,40]) followed by an INCOMPLETE row carrying only 2 of its 4 pixel bytes ([50,60]).
        // Predictor.PredictorOutputStream only emits such a trailing partial row when flushed, so this
        // guards against silently dropping it.
        COSDictionary decodeParms = new COSDictionary();
        decodeParms.setInt(COSName.PREDICTOR, 15);
        decodeParms.setInt(COSName.COLORS, 1);
        decodeParms.setInt(COSName.BITS_PER_COMPONENT, 8);
        decodeParms.setInt(COSName.COLUMNS, 4);
        COSDictionary parameters = of(COSName.FILTER, COSName.BROTLI_DECODE, COSName.DECODE_PARMS,
                decodeParms);

        ByteArrayOutputStream decoded = new ByteArrayOutputStream();
        victim.decode(new ByteArrayInputStream(resource("brotli-predictor-partial-row.br")),
                decoded, parameters, 0);

        assertArrayEquals(new byte[] { 10, 20, 30, 40, 50, 60, 0, 0 }, decoded.toByteArray());
    }

    @Test
    public void decodeThrowsIOExceptionForACorruptBrotliStream()
    {
        byte[] garbage = { 1, 2, 3, 4, 5, 6, 7, 8 };
        assertThrows(IOException.class,
                () -> victim.decode(new ByteArrayInputStream(garbage), new ByteArrayOutputStream(),
                        new COSDictionary(), 0));
    }

    @Test
    public void encodeIsNotSupported()
    {
        assertThrows(UnsupportedOperationException.class,
                () -> victim.encode(new ByteArrayInputStream(new byte[0]),
                        new ByteArrayOutputStream(), new COSDictionary()));
    }
}
