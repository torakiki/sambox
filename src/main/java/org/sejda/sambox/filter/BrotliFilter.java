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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.sejda.commons.util.IOUtils;
import org.sejda.sambox.cos.COSDictionary;

/**
 * Decompresses data encoded using the Brotli compression method (IETF RFC 7932). This is the
 * {@code BrotliDecode} filter defined by the PDF Association's "Brotli compression in PDF 2.0"
 * extension to ISO 32000-2.
 * <p>
 * Decoding requires the optional {@code org.brotli:dec} dependency to be available at runtime. If
 * it isn't on the classpath, decoding fails with an {@link IOException}.
 *
 * @author Andrea Vacondio
 * @see <a href="https://pdfa.org/resource/extension-brotli/">Brotli compression in PDF 2.0</a>
 */
final class BrotliFilter extends Filter
{

    @Override
    public DecodeResult decode(InputStream encoded, OutputStream decoded, COSDictionary parameters,
            int index) throws IOException
    {
        OutputStream predictor = Predictor.wrapPredictor(decoded,
                getDecodeParams(parameters, index));
        IOUtils.copy(brotliStream(encoded), predictor);
        predictor.flush();
        return new DecodeResult(parameters);
    }

    @Override
    public void encode(InputStream input, OutputStream encoded, COSDictionary parameters)
    {
        throw new UnsupportedOperationException("Brotli encoding is not supported");
    }

    private static InputStream brotliStream(InputStream wrapped) throws IOException
    {
        try
        {
            return (InputStream) Class.forName("org.brotli.dec.BrotliInputStream")
                    .getDeclaredConstructor(InputStream.class).newInstance(wrapped);
        }
        catch (Exception e)
        {
            throw new IOException(
                    "Unable to decode Brotli encoded stream, missing org.brotli:dec dependency", e);
        }
    }
}
