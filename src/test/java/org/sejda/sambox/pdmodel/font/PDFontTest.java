/*
 *  Copyright 2011 adam.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.fontbox.ttf.TTFParser;
import org.apache.fontbox.ttf.TrueTypeCollection;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.apache.fontbox.util.autodetect.FontFileFinder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sejda.commons.util.IOUtils;
import org.sejda.io.SeekableSources;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.input.PDFParser;
import org.sejda.sambox.pdmodel.PDDocument;
import org.sejda.sambox.pdmodel.PDPage;
import org.sejda.sambox.pdmodel.PDPageContentStream;
import org.sejda.sambox.pdmodel.font.encoding.WinAnsiEncoding;
import org.sejda.sambox.rendering.PDFRenderer;
import org.sejda.sambox.text.PDFTextStripper;

/**
 * @author adam
 * @author Tilman Hausherr
 */
public class PDFontTest
{

    private static final File OUT_DIR = new File("target/test-output");

    @Before
    public void setUp() throws Exception
    {
        OUT_DIR.mkdirs();
    }

    /**
     * Test of the error reported in PDFBox-988
     */
    public void testPDFBox988() throws Exception
    {
        try (PDDocument doc = PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(
                getClass().getResourceAsStream("F001u_3_7j.pdf"))))
        {
            PDFRenderer renderer = new PDFRenderer(doc);
            renderer.renderImage(0);
            // the allegation is that renderImage() will crash the JVM or hang
        }
    }

    /**
     * PDFBOX-3747: Test that using "-" with Calibri in Windows 7 has "-" in text extraction and not
     * \u2010, which was because of a wrong ToUnicode mapping because prior to the bugfix,
     * CmapSubtable#getCharCodes provided values in random order.
     *
     * @throws IOException
     */
    @Test
    public void testPDFBox3747() throws IOException
    {
        File file = new File("target/fonts", "PDFBOX-3747-calibri.ttf");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PDDocument doc = new PDDocument())
        {
            PDPage page = new PDPage();
            doc.addPage(page);
            PDFont font = PDType0Font.load(doc, file);

            PDPageContentStream cs = new PDPageContentStream(doc, page);
            cs.beginText();
            cs.setFont(font, 10);
            cs.showText("PDFBOX-3747");
            cs.endText();
            cs.close();

            doc.writeTo(baos);
        }
        try (PDDocument doc = PDFParser.parse(
                SeekableSources.inMemorySeekableSourceFrom(baos.toByteArray())))
        {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);
            Assert.assertEquals("PDFBOX-3747", text.trim());
        }
    }

    /**
     * PDFBOX-3826: Test ability to reuse a TrueTypeFont created from a file or a stream for several
     * PDFs to avoid parsing it over and over again. Also check that full or partial embedding is
     * done, and do render and text extraction.
     *
     * @throws IOException
     * @throws URISyntaxException
     */
    @Test
    public void testPDFBox3826() throws IOException, URISyntaxException
    {
        URL url = PDFont.class.getResource(
                "/org/sejda/sambox/resources/ttf/LiberationSans-Regular.ttf");
        File fontFile = new File(url.toURI());

        TrueTypeFont ttf1 = new TTFParser().parse(fontFile);
        testPDFBox3826checkFonts(testPDFBox3826createDoc(ttf1), fontFile);
        ttf1.close();

        TrueTypeFont ttf2 = new TTFParser().parse(new FileInputStream(fontFile));
        testPDFBox3826checkFonts(testPDFBox3826createDoc(ttf2), fontFile);
        ttf2.close();
    }

    /**
     * PDFBOX-4115: Test ability to create PDF with german umlaut glyphs with a type 1 font. Test
     * for everything that went wrong before this was fixed.
     *
     * @throws IOException
     */
    @Test
    public void testPDFBOX4115() throws IOException
    {
        File fontFile = new File("target/fonts", "n019003l.pfb");
        File outputFile = new File(OUT_DIR, "FontType1.pdf");
        String text = "äöüÄÖÜ";

        PDDocument doc = new PDDocument();

        PDPage page = new PDPage();
        PDPageContentStream contentStream = new PDPageContentStream(doc, page);

        PDType1Font font = new PDType1Font(doc, new FileInputStream(fontFile),
                WinAnsiEncoding.INSTANCE);

        contentStream.beginText();
        contentStream.setFont(font, 10);
        contentStream.newLineAtOffset(10, 700);
        contentStream.showText(text);
        contentStream.endText();
        contentStream.close();

        doc.addPage(page);

        doc.writeTo(outputFile);
        doc.close();

        doc = PDFParser.parse(SeekableSources.seekableSourceFrom(outputFile));

        font = (PDType1Font) doc.getPage(0).getResources().getFont(COSName.getPDFName("F1"));
        Assert.assertEquals(font.getEncoding(), WinAnsiEncoding.INSTANCE);

        for (char c : text.toCharArray())
        {
            String name = font.getEncoding().getName(c);
            Assert.assertEquals("dieresis", name.substring(1));
            Assert.assertFalse(font.getPath(name).getBounds2D().isEmpty());
        }

        PDFTextStripper stripper = new PDFTextStripper();
        Assert.assertEquals(text, stripper.getText(doc).trim());

        doc.close();
    }

    /**
     * Test whether bug from PDFBOX-4318 is fixed, which had the wrong cache key.
     *
     * @throws java.io.IOException
     */
    @Test
    public void testPDFox4318() throws IOException
    {
        try
        {
            PDType1Font.HELVETICA_BOLD.encode("\u0080");
            Assert.fail("should have thrown IllegalArgumentException");
        }
        catch (IllegalArgumentException ex)
        {
        }
        PDType1Font.HELVETICA_BOLD.encode("€");
        try
        {
            PDType1Font.HELVETICA_BOLD.encode("\u0080");
            Assert.fail("should have thrown IllegalArgumentException");
        }
        catch (IllegalArgumentException ex)
        {
        }
    }

    @Test
    public void testFullEmbeddingTTC() throws IOException
    {
        FontFileFinder fff = new FontFileFinder();
        TrueTypeCollection ttc = null;
        for (URI uri : fff.find())
        {
            if (uri.getPath().endsWith(".ttc"))
            {
                File file = new File(uri);
                ttc = new TrueTypeCollection(file);
                break;
            }
        }
        if (ttc == null)
        {
            return;
        }

        final List<String> names = new ArrayList<>();
        ttc.processAllFonts(ttf -> names.add(ttf.getName()));

        TrueTypeFont ttf = ttc.getFontByName(names.get(0)); // take the first one

        try
        {
            PDType0Font.load(new PDDocument(), ttf, false);
        }
        catch (IOException ex)
        {
            Assert.assertEquals("Full embedding of TrueType font collections not supported",
                    ex.getMessage());
            return;
        }
        Assert.fail("should have thrown IOException");
    }

    /**
     * Test using broken Type1C font.
     *
     * @throws IOException
     */
    @Test
    public void testPDFox5048() throws IOException
    {
        try (PDDocument testPdf = PDFParser.parse(
                SeekableSources.seekableSourceFrom(new File("target/pdfs/stringwidth.pdf"))))
        {
            PDPage page = testPdf.getPage(0);
            PDFont font = page.getResources().getFont(COSName.getPDFName("F70"));
            assertTrue(font.isDamaged());
            assertEquals(0f, font.getHeight(0));
            assertEquals(0f, font.getStringWidth("Pa"));
        }

    }

    private void testPDFBox3826checkFonts(byte[] byteArray, File fontFile) throws IOException
    {
        try (PDDocument doc = PDFParser.parse(
                SeekableSources.inMemorySeekableSourceFrom(byteArray)))
        {

            PDPage page2 = doc.getPage(0);

            // F1 = type0 subset
            PDType0Font fontF1 = (PDType0Font) page2.getResources()
                    .getFont(COSName.getPDFName("F1"));
            Assert.assertTrue(fontF1.getName().contains("+"));
            Assert.assertTrue(fontFile.length() > fontF1.getFontDescriptor().getFontFile2()
                    .toByteArray().length);

            // F2 = type0 full embed
            PDType0Font fontF2 = (PDType0Font) page2.getResources()
                    .getFont(COSName.getPDFName("F2"));
            Assert.assertFalse(fontF2.getName().contains("+"));
            Assert.assertEquals(fontFile.length(),
                    fontF2.getFontDescriptor().getFontFile2().toByteArray().length);

            // F3 = tt full embed
            PDTrueTypeFont fontF3 = (PDTrueTypeFont) page2.getResources()
                    .getFont(COSName.getPDFName("F3"));
            Assert.assertFalse(fontF2.getName().contains("+"));
            Assert.assertEquals(fontFile.length(),
                    fontF3.getFontDescriptor().getFontFile2().toByteArray().length);

            new PDFRenderer(doc).renderImage(0);

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setLineSeparator("\n");
            String text = stripper.getText(doc);
            Assert.assertEquals(
                    "testMultipleFontFileReuse1\ntestMultipleFontFileReuse2\ntestMultipleFontFileReuse3",
                    text.trim());
        }
    }

    private byte[] testPDFBox3826createDoc(TrueTypeFont ttf) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PDDocument doc = new PDDocument())
        {

            PDPage page = new PDPage();
            doc.addPage(page);

            // type 0 subset embedding
            PDFont font = PDType0Font.load(doc, ttf, true);
            PDPageContentStream cs = new PDPageContentStream(doc, page);

            cs.beginText();
            cs.newLineAtOffset(10, 700);
            cs.setFont(font, 10);
            cs.showText("testMultipleFontFileReuse1");
            cs.endText();

            // type 0 full embedding
            font = PDType0Font.load(doc, ttf, false);

            cs.beginText();
            cs.newLineAtOffset(10, 650);
            cs.setFont(font, 10);
            cs.showText("testMultipleFontFileReuse2");
            cs.endText();

            // tt full embedding but only WinAnsiEncoding
            font = PDTrueTypeFont.load(doc, ttf, WinAnsiEncoding.INSTANCE);

            cs.beginText();
            cs.newLineAtOffset(10, 600);
            cs.setFont(font, 10);
            cs.showText("testMultipleFontFileReuse3");
            cs.endText();

            cs.close();

            doc.writeTo(baos);
        }
        return baos.toByteArray();
    }

    @Test
    public void testEncodeLeniently() throws IOException
    {
        String unsupported = "ł";

        try
        {
            PDType1Font.HELVETICA.encode(unsupported);
            fail("Exception expected");
        }
        catch (IllegalArgumentException ex)
        {
            // expected
        }

        PDType1Font.HELVETICA.encodeLeniently(unsupported);
    }

    /**
     * Check that font can be deleted after usage.
     *
     * @throws IOException
     */
    @Test
    public void testDeleteFont() throws IOException
    {
        File tempFontFile = new File(OUT_DIR, "LiberationSans-Regular.ttf");
        File tempPdfFile = new File(OUT_DIR, "testDeleteFont.pdf");
        String text = "Test PDFBOX-4823";

        InputStream is = PDFont.class.getResourceAsStream(
                "/org/sejda/sambox/resources/ttf/LiberationSans-Regular.ttf");

        OutputStream os = new FileOutputStream(tempFontFile);
        IOUtils.copy(is, os);
        is.close();
        os.close();

        try (PDDocument doc = new PDDocument())
        {
            PDPage page = new PDPage();
            doc.addPage(page);
            PDPageContentStream cs = new PDPageContentStream(doc, page);
            PDFont font1 = PDType0Font.load(doc, tempFontFile);
            cs.beginText();
            cs.setFont(font1, 50);
            cs.newLineAtOffset(50, 700);
            cs.showText(text);
            cs.endText();
            cs.close();
            doc.writeTo(tempPdfFile);
        }

        Assert.assertTrue(tempFontFile.delete());

        try (PDDocument doc = PDDocument.load(tempPdfFile))
        {
            PDFTextStripper stripper = new PDFTextStripper();
            String extractedText = stripper.getText(doc);
            Assert.assertEquals(text, extractedText.trim());
        }

        Assert.assertTrue(tempPdfFile.delete());
    }

    /**
     * PDFBOX-5115: U+00AD (soft hyphen) should work with WinAnsiEncoding.
     */
    @Test
    public void testSoftHyphen() throws IOException
    {
        String text = "- \u00AD";
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PDDocument doc = new PDDocument())
        {
            PDPage page = new PDPage();
            doc.addPage(page);
            PDFont font1 = PDType1Font.HELVETICA;
            PDFont font2 = PDType0Font.load(doc, PDFontTest.class.getResourceAsStream(
                    "/org/sejda/sambox/resources/ttf/LiberationSans-Regular.ttf"));

            Assert.assertEquals(font1.getStringWidth("-"), font1.getStringWidth("\u00AD"), 0);
            Assert.assertEquals(font2.getStringWidth("-"), font2.getStringWidth("\u00AD"), 0);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page))
            {
                cs.beginText();
                cs.newLineAtOffset(100, 500);
                cs.setFont(font1, 10);
                cs.showText(text);
                cs.newLineAtOffset(0, 100);
                cs.setFont(font2, 10);
                cs.showText(text);
                cs.endText();
            }
            doc.writeTo(baos);
        }

        try (PDDocument doc = PDFParser.parse(
                SeekableSources.inMemorySeekableSourceFrom(baos.toByteArray())))
        {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setLineSeparator("\n");
            String extractedText = stripper.getText(doc);
            Assert.assertEquals(text + "\n" + text, extractedText.trim());
        }
    }
}
