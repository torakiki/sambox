/*
 * Created on 25/10/26
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
package org.sejda.sambox.pdmodel.encryption;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.sejda.io.SeekableSources;
import org.sejda.sambox.input.PDFParser;

public class SecurityHandlerTest
{
    @Test
    public void compressedMetadataIsDecrypted() throws IOException
    {
        // AES-256 encrypted document with Flate compressed metadata
        try (var document = PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(
                getClass().getResourceAsStream("/sambox/encrypted-metadata.pdf")), "test"))
        {
            var metadata = document.getDocumentCatalog().getMetadata();
            assertNotNull(metadata);
            var xmp = new String(metadata.exportXMPMetadata().readAllBytes(), UTF_8);
            assertTrue(xmp.contains("<xmp:CreatorTool>PDFsam Basic v6.0.6</xmp:CreatorTool>"));
        }
    }
}
