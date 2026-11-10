/*
 * Created on 10/11/26
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
package org.sejda.sambox.pdmodel.font;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sejda.io.SeekableSources;
import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSInteger;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSStream;
import org.sejda.sambox.input.PDFParser;

class PDType3FontTest
{
    @Test
    void charProcFoundByUtf8Name() throws IOException
    {
        var font = fontWithGlyph(COSName.getPDFName("gé"));
        assertNotNull(font.getCharProc(65));
        assertTrue(font.hasGlyph("gé"));
    }

    @Test
    @DisplayName("A glyph name that isn't UTF-8 (/g#E9 in Latin-1) is found in /CharProcs by the text the encoding gives for it")
    void charProcFoundByLatin1Name() throws IOException
    {
        try (var document = PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(
                getClass().getResourceAsStream("/sambox/type3-latin1-glyph-name.pdf"))))
        {
            var font = (PDType3Font) document.getPage(0).getResources()
                    .getFont(COSName.getPDFName("T3"));
            assertNotNull(font.getCharProc(65));
            assertTrue(font.hasGlyph(font.getEncoding().getName(65)));
        }
    }

    @Test
    void hasGlyphMissingName() throws IOException
    {
        var font = fontWithGlyph(COSName.getPDFName("gé"));
        assertFalse(font.hasGlyph("missing"));
        assertNull(font.getCharProc(66));
    }

    /**
     * @return a Type3 font where the code 65 is the given glyph
     */
    private static PDType3Font fontWithGlyph(COSName glyphName) throws IOException
    {
        var encoding = COSDictionary.of(COSName.TYPE, COSName.ENCODING, COSName.DIFFERENCES,
                new COSArray(COSInteger.get(65), glyphName));
        return new PDType3Font(
                COSDictionary.of(COSName.SUBTYPE, COSName.TYPE3, COSName.ENCODING, encoding,
                        COSName.CHAR_PROCS, COSDictionary.of(glyphName, new COSStream())));
    }
}
