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
package org.sejda.sambox.pdmodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;

import org.apache.fontbox.ttf.TrueTypeFont;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sejda.commons.util.IOUtils;
import org.sejda.io.SeekableSources;
import org.sejda.sambox.SAMBox;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.input.PDFParser;
import org.sejda.sambox.output.PreSaveCOSTransformer;
import org.sejda.sambox.output.WriteOption;
import org.sejda.sambox.pdmodel.PDDocument.OnClose;
import org.sejda.sambox.util.SpecVersionUtils;

public class TestPDDocument
{

    /**
     * Test document save/load using a stream.
     *
     * @throws IOException if something went wrong
     */
    @Test
    public void testSaveLoadStream() throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // Create PDF with one blank page
        try (PDDocument document = new PDDocument())
        {
            document.addPage(new PDPage());
            document.writeTo(baos);
        }

        // Verify content
        byte[] pdf = baos.toByteArray();
        assertTrue(pdf.length > 200);
        assertEquals("%PDF-1.4", new String(Arrays.copyOfRange(pdf, 0, 8), StandardCharsets.UTF_8));
        assertEquals("%%EOF\n", new String(Arrays.copyOfRange(pdf, pdf.length - 6, pdf.length),
                StandardCharsets.UTF_8));

        // Load
        try (PDDocument loadDoc = PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(pdf)))
        {
            assertEquals(1, loadDoc.getNumberOfPages());
        }
    }

    /**
     * Test document save/load using a file.
     *
     * @throws IOException if something went wrong
     */
    @Test
    public void testSaveLoadFile(@TempDir Path tmp) throws IOException
    {
        var targetFile = Files.createTempFile(tmp, "pddocument-saveloadfile", ".pdf").toFile();
        // Create PDF with one blank page
        try (PDDocument document = new PDDocument())
        {
            document.addPage(new PDPage());
            document.writeTo(targetFile);
        }

        // Verify content
        assertTrue(targetFile.length() > 200);
        InputStream in = new FileInputStream(targetFile);
        byte[] pdf = IOUtils.toByteArray(in);
        in.close();
        assertTrue(pdf.length > 200);
        assertEquals("%PDF-1.4", new String(Arrays.copyOfRange(pdf, 0, 8), StandardCharsets.UTF_8));
        assertEquals("%%EOF\n", new String(Arrays.copyOfRange(pdf, pdf.length - 6, pdf.length),
                StandardCharsets.UTF_8));

        // Load
        try (PDDocument loadDoc = PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(pdf)))
        {
            assertEquals(1, loadDoc.getNumberOfPages());
        }
    }

    /**
     * PDFBOX-3481: Test whether XRef generation results in unusable PDFs if Arab numbering is
     * default.
     */
    @Test
    public void testSaveArabicLocale(@TempDir Path tmp) throws IOException
    {
        Locale defaultLocale = Locale.getDefault();
        try
        {
            Locale arabicLocale = new Locale.Builder().setLanguageTag("ar-EG-u-nu-arab")
                    .build();
            Locale.setDefault(arabicLocale);
            var targetFile = Files.createTempFile(tmp, "pddocument-savearabicfile", ".pdf")
                    .toFile();

            // Create PDF with one blank page
            try (PDDocument document = new PDDocument())
            {
                document.addPage(new PDPage());
                document.writeTo(targetFile);
            }

            // Load
            try (PDDocument loadDoc = PDFParser.parse(
                    SeekableSources.seekableSourceFrom(targetFile)))
            {
                assertEquals(1, loadDoc.getNumberOfPages());
            }
        }
        finally
        {
            Locale.setDefault(defaultLocale);
        }
    }

    @Test
    public void defaultVersion() throws IOException
    {
        try (PDDocument document = new PDDocument())
        {
            // test default version
            assertEquals(SpecVersionUtils.V1_4, document.getVersion());
            assertEquals(SpecVersionUtils.V1_4, document.getDocument().getHeaderVersion());
            assertEquals(SpecVersionUtils.V1_4, document.getDocumentCatalog().getVersion());
        }
    }

    @Test
    public void downgradeVersion() throws IOException
    {
        try (PDDocument document = new PDDocument())
        {
            document.getDocument().setHeaderVersion(SpecVersionUtils.V1_3);
            document.getDocumentCatalog().setVersion(null);
            assertEquals(SpecVersionUtils.V1_3, document.getVersion());
            assertEquals(SpecVersionUtils.V1_3, document.getDocument().getHeaderVersion());
            assertNull(document.getDocumentCatalog().getVersion());
        }
    }

    @Test
    public void cannotDowngradeVersion() throws IOException
    {
        try (PDDocument document = new PDDocument())
        {
            document.setVersion(SpecVersionUtils.V1_3);
            assertEquals(SpecVersionUtils.V1_4, document.getVersion());
            assertEquals(SpecVersionUtils.V1_4, document.getDocument().getHeaderVersion());
            assertEquals(SpecVersionUtils.V1_4, document.getDocumentCatalog().getVersion());
        }
    }

    @Test
    public void versionUpgrade() throws IOException
    {
        try (PDDocument document = new PDDocument())
        {
            document.setVersion(SpecVersionUtils.V1_5);
            assertEquals(SpecVersionUtils.V1_5, document.getVersion());
            assertEquals(SpecVersionUtils.V1_5, document.getDocument().getHeaderVersion());
            assertEquals(SpecVersionUtils.V1_5, document.getDocumentCatalog().getVersion());
        }
    }

    @Test
    public void requiredNotBlankVersion()
    {
        assertThrows(IllegalArgumentException.class,
                () -> new PDDocument().getDocument().setHeaderVersion(" "));

    }

    @Test
    @DisplayName("Multiple close() don't yield multiple onClose action runs")
    public void closeOnce() throws IOException
    {

        OnClose onClose = new OnClose()
        {
            private int count = 0;

            @Override
            public void onClose()
            {
                assertTrue(++count <= 1);
            }
        };

        var document = new PDDocument();
        document.addOnCloseAction(onClose);
        document.addPage(new PDPage());
        document.writeTo(new ByteArrayOutputStream());
        document.close();
        document.close();
    }

    @Test
    public void onCloseComposition() throws IOException
    {
        var onefont = mock(TrueTypeFont.class);
        var anotherfont = mock(TrueTypeFont.class);

        var document = new PDDocument();
        document.registerTrueTypeFontForClosing(onefont);
        document.registerTrueTypeFontForClosing(anotherfont);
        document.addPage(new PDPage());
        document.writeTo(new ByteArrayOutputStream());
        verify(onefont).close();
        verify(anotherfont).close();
    }

    @Test
    public void testWriteWithMetadata(@TempDir Path tmp) throws IOException
    {
        var output = Files.createTempFile(tmp, "", ".pdf").toFile();
        try (var document = new PDDocument())
        {
            document.addPage(new PDPage());
            document.writeTo(output);
        }
        try (var outputDoc = PDFParser.parse(SeekableSources.seekableSourceFrom(output)))
        {
            assertEquals(SAMBox.PRODUCER, outputDoc.getDocumentInformation().getProducer());
        }
    }

    @Test
    public void testWriteNoMetadata(@TempDir Path tmp) throws IOException
    {
        var output = Files.createTempFile(tmp, "", ".pdf").toFile();
        try (var document = new PDDocument())
        {
            document.addPage(new PDPage());
            document.writeTo(output, WriteOption.NO_METADATA_PRODUCER_MODIFIED_DATE_UPDATE);
        }
        try (var outputDoc = PDFParser.parse(SeekableSources.seekableSourceFrom(output)))
        {
            assertNull(outputDoc.getDocumentInformation().getProducer());
        }
    }

    @Test
    public void testPreSaveTransformer(@TempDir Path tmp) throws IOException
    {
        var output = Files.createTempFile(tmp, "", ".pdf").toFile();
        try (var document = new PDDocument())
        {
            document.addPage(new PDPage());
            document.getDocumentCatalog().setPageLayout(PageLayout.SINGLE_PAGE);
            document.withPreSaveTransformer(new PreSaveCOSTransformer()
            {
                @Override
                public void visit(COSDictionary value) throws IOException
                {
                    if (COSName.CATALOG.equals(value.getCOSName(COSName.TYPE)))
                    {
                        value.removeItem(COSName.PAGE_LAYOUT);
                    }
                }
            }).writeTo(output);
        }

        try (var outputDoc = PDFParser.parse(SeekableSources.seekableSourceFrom(output)))
        {
            assertNull(
                    outputDoc.getDocumentCatalog().getCOSObject().getCOSName(COSName.PAGE_LAYOUT));
        }
    }
}
