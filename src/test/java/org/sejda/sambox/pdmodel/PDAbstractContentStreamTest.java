/*
 * Created on 12/11/26
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
package org.sejda.sambox.pdmodel;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.sejda.sambox.cos.COSStream;
import org.sejda.sambox.pdmodel.graphics.form.PDFormXObject;
import org.sejda.sambox.pdmodel.interactive.annotation.PDAppearanceStream;

/**
 * A number written after a longer one must not carry the longer number's leftover digits, nor any
 * padding.
 */
class PDAbstractContentStreamTest
{
    private static final String EXPECTED = "0.8 0.8 1 rg\n11 11 98.5 48 re\n";

    @Test
    void formContentStreamWritesOnlyTheFormattedNumbers() throws IOException
    {
        var form = new PDFormXObject(new COSStream());
        try (var contentStream = new PDFormContentStream(form))
        {
            writeShorterNumbersAfterLongerOnes(contentStream);
        }
        assertEquals(EXPECTED, contentOf(form));
    }

    @Test
    void appearanceContentStreamWritesOnlyTheFormattedNumbers() throws IOException
    {
        var appearance = new PDAppearanceStream();
        try (var contentStream = new PDAppearanceContentStream(appearance))
        {
            writeShorterNumbersAfterLongerOnes(contentStream);
        }
        assertEquals(EXPECTED, contentOf(appearance));
    }

    private static void writeShorterNumbersAfterLongerOnes(PDAbstractContentStream contentStream)
            throws IOException
    {
        contentStream.setNonStrokingColor(0.8f, 0.8f, 1f);
        contentStream.addRect(11, 11, 98.5f, 48);
    }

    private static String contentOf(PDFormXObject form) throws IOException
    {
        try (var contents = form.getContents())
        {
            return new String(contents.readAllBytes(), StandardCharsets.US_ASCII);
        }
    }
}
