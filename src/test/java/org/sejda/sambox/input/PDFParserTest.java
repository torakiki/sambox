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
package org.sejda.sambox.input;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;
import org.sejda.io.SeekableSources;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.pdmodel.PDDocument;
import org.sejda.sambox.util.SpecVersionUtils;

/**
 * @author Andrea Vacondio
 *
 */
public class PDFParserTest
{
    @Test
    public void positive() throws IOException
    {
        assertNotNull(PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(
                getClass().getResourceAsStream("/sambox/simple_test.pdf"))));
    }

    @Test
    public void positiveIncremental() throws IOException
    {
        assertNotNull(PDFParser.parseToIncrement(SeekableSources.inMemorySeekableSourceFrom(
                getClass().getResourceAsStream("/sambox/simple_test.pdf"))));
    }

    @Test(expected = IOException.class)
    public void notAPdf() throws IOException
    {
        PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(
                getClass().getResourceAsStream("/sambox/not_a_pdf.pdf")));
    }

    @Test
    public void notEncryted() throws IOException
    {
        try (PDDocument doc = PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(
                getClass().getResourceAsStream("/sambox/simple_test.pdf"))))
        {
            assertNotNull(doc);
            assertFalse(doc.isEncrypted());
            assertTrue(doc.isOpen());
            assertEquals(SpecVersionUtils.V1_5, doc.getVersion());
        }
    }

    @Test
    public void encrypted() throws IOException
    {
        try (PDDocument doc = PDFParser.parse(
                SeekableSources.inMemorySeekableSourceFrom(
                        getClass().getResourceAsStream("/sambox/encrypted_simple_test.pdf")),
                "test"))
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
        try (PDDocument doc = PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(
                getClass().getResourceAsStream("/sambox/simple_test.pdf")), "test"))
        {
            assertNotNull(doc);
            assertFalse(doc.isEncrypted());
            assertTrue(doc.isOpen());
            assertEquals(SpecVersionUtils.V1_5, doc.getVersion());
        }
    }

    @Test
    public void badHeader() throws IOException
    {
        try (PDDocument doc = PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(
                getClass().getResourceAsStream("/sambox/bad_header.pdf"))))
        {
            assertNotNull(doc);
            assertFalse(doc.isEncrypted());
            assertTrue(doc.isOpen());
            assertEquals(SpecVersionUtils.V1_4, doc.getVersion());
        }
    }

    @Test
    public void headerWithSpaces() throws IOException
    {
        try (PDDocument doc = PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(
                getClass().getResourceAsStream("/sambox/spaces_in_header.pdf"))))
        {
            assertNotNull(doc);
            assertFalse(doc.isEncrypted());
            assertTrue(doc.isOpen());
            assertEquals(SpecVersionUtils.V1_4, doc.getVersion());
        }
    }

    @Test
    public void invalidButRightLengthHeader() throws IOException
    {
        try (PDDocument doc = PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(
                getClass().getResourceAsStream("/sambox/invalid_header_right_length.pdf"))))
        {
            assertNotNull(doc);
            assertFalse(doc.isEncrypted());
            assertTrue(doc.isOpen());
            assertEquals(SpecVersionUtils.V1_6, doc.getVersion());
        }
    }

    @Test(expected = IOException.class)
    public void trunkatedHeader() throws IOException
    {
        PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(
                getClass().getResourceAsStream("/sambox/trunkated_header.pdf")));
    }

    @Test(expected = IOException.class)
    public void missingHeader() throws IOException
    {
        PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(
                getClass().getResourceAsStream("/sambox/missing_header.pdf")));
    }

    @Test
    public void secondLineHeader() throws IOException
    {
        try (PDDocument doc = PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(
                getClass().getResourceAsStream("/sambox/second_line_header.pdf"))))
        {
            assertNotNull(doc);
            assertFalse(doc.isEncrypted());
            assertTrue(doc.isOpen());
            assertEquals(SpecVersionUtils.V1_4, doc.getVersion());
        }
    }

    @Test
    public void thirdLineHeader() throws IOException
    {
        try (PDDocument doc = PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(
                getClass().getResourceAsStream("/sambox/third_line_header.pdf"))))
        {
            assertNotNull(doc);
            assertFalse(doc.isEncrypted());
            assertTrue(doc.isOpen());
            assertEquals(SpecVersionUtils.V1_4, doc.getVersion());
        }
    }

    @Test(expected = IOException.class)
    public void tooMuchPreGarbageHeader() throws IOException
    {
        PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(
                getClass().getResourceAsStream("/sambox/too_much_garbage_header.pdf")));
    }

    @Test
    public void missingPageType() throws IOException
    {
        try (PDDocument doc = PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(
                getClass().getResourceAsStream("/sambox/missing_page_type.pdf"))))
        {
            assertNotNull(doc.getPage(0));
        }
    }

    @Test
    public void missingCatalog() throws IOException
    {
        try (PDDocument doc = PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(
                getClass().getResourceAsStream("/org/sejda/sambox/input/MissingCatalog.pdf"))))
        {
            assertNotNull(doc);
            assertFalse(doc.isEncrypted());
            assertTrue(doc.isOpen());
            assertEquals(SpecVersionUtils.V1_4, doc.getVersion());
            assertNotNull(doc.getDocumentCatalog());
            assertEquals(COSName.CATALOG,
                    doc.getDocumentCatalog().getCOSObject().getItem(COSName.TYPE));
        }
    }

    @Test
    public void trunkatedXref() throws IOException
    {
        try (PDDocument doc = PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(
                getClass().getResourceAsStream("/sambox/test_trunkated_xref_table.pdf"))))
        {
            assertNotNull(doc.getPage(0));
        }
    }

}
