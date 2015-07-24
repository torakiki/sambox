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
package org.apache.pdfbox.input;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.SpecVersionUtils;
import org.junit.Test;
import org.sejda.io.SeekableSources;

/**
 * @author Andrea Vacondio
 *
 */
public class PDFParserTest
{

    @Test(expected = IOException.class)
    public void notAPdf() throws IOException
    {
        PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(getClass().getResourceAsStream(
                "/input/not_a_pdf.pdf")));
    }

    @Test
    public void notEncryted() throws IOException
    {
        try (PDDocument doc = PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(getClass()
                .getResourceAsStream("/input/simple_test.pdf"))))
        {
            assertNotNull(doc);
            assertFalse(doc.isEncrypted());
            assertTrue(doc.isOpen());
            assertEquals(SpecVersionUtils.V1_4, doc.getVersion());
        }
    }

    @Test
    public void encrypted() throws IOException
    {
        try (PDDocument doc = PDFParser.parse(
                SeekableSources.inMemorySeekableSourceFrom(getClass().getResourceAsStream(
                        "/input/encrypted_simple_test.pdf")), "test"))
        {
            assertNotNull(doc);
            assertTrue(doc.isEncrypted());
            assertTrue(doc.isOpen());
            assertEquals(SpecVersionUtils.V1_6, doc.getVersion());
        }
    }

    @Test
    public void notEncrytedWithPwd() throws IOException
    {
        try (PDDocument doc = PDFParser.parse(
                SeekableSources.inMemorySeekableSourceFrom(getClass().getResourceAsStream(
                        "/input/simple_test.pdf")), "test"))
        {
            assertNotNull(doc);
            assertFalse(doc.isEncrypted());
            assertTrue(doc.isOpen());
            assertEquals(SpecVersionUtils.V1_4, doc.getVersion());
        }
    }

    @Test
    public void badHeader() throws IOException
    {
        try (PDDocument doc = PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(getClass()
                .getResourceAsStream("/input/bad_header.pdf"))))
        {
            assertNotNull(doc);
            assertFalse(doc.isEncrypted());
            assertTrue(doc.isOpen());
            assertEquals(SpecVersionUtils.V1_4, doc.getVersion());
        }
    }

    @Test(expected = IOException.class)
    public void veryBadHeader() throws IOException
    {
        PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(getClass().getResourceAsStream(
                "/input/unparsable_bad_header.pdf")));
    }

    @Test(expected = IOException.class)
    public void trunkatedHeader() throws IOException
    {
        PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(getClass().getResourceAsStream(
                "/input/trunkated_header.pdf")));
    }

    @Test(expected = IOException.class)
    public void missingHeader() throws IOException
    {
        PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(getClass().getResourceAsStream(
                "/input/missing_header.pdf")));
    }

    @Test
    public void secondLineHeader() throws IOException
    {
        try (PDDocument doc = PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(getClass()
                .getResourceAsStream("/input/second_line_header.pdf"))))
        {
            assertNotNull(doc);
            assertFalse(doc.isEncrypted());
            assertTrue(doc.isOpen());
            assertEquals(SpecVersionUtils.V1_4, doc.getVersion());
        }
    }

}
