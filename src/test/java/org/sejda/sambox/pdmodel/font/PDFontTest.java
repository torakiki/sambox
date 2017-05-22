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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.fontbox.ttf.TTFParser;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.junit.Assert;
import org.junit.Test;
import org.sejda.io.SeekableSources;
import org.sejda.sambox.input.PDFParser;
import org.sejda.sambox.pdmodel.PDDocument;
import org.sejda.sambox.pdmodel.PDPage;
import org.sejda.sambox.pdmodel.PDPageContentStream;
import org.sejda.sambox.pdmodel.common.PDRectangle;
import org.sejda.sambox.rendering.PDFRenderer;
import org.sejda.sambox.text.PDFTextStripper;

import junit.framework.TestCase;

/**
 * 
 * @author adam
 */
public class PDFontTest extends TestCase
{

    /**
     * Test of the error reported in PDFBox-988
     */
    public void testPDFBox988() throws Exception
    {
        try (PDDocument doc = PDFParser.parse(SeekableSources
                .inMemorySeekableSourceFrom(getClass().getResourceAsStream("F001u_3_7j.pdf"))))
        {
            PDFRenderer renderer = new PDFRenderer(doc);
            renderer.renderImage(0);
            // the allegation is that renderImage() will crash the JVM or hang
        }
    }

    /**
     * PDFBOX-3337: Test ability to reuse a TrueTypeFont for several PDFs to avoid parsing it over and over again.
     *
     * @throws java.io.IOException
     */
    public void testPDFBox3337() throws IOException
    {
        InputStream ttfStream = PDFontTest.class.getClassLoader()
                .getResourceAsStream("org/sejda/sambox/ttf/LiberationSans-Regular.ttf");
        final TrueTypeFont ttf = new TTFParser().parse(ttfStream);

        for (int i = 0; i < 2; ++i)
        {
            PDDocument doc = new PDDocument();

            final PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            PDPageContentStream cs = new PDPageContentStream(doc, page);
            PDFont font = PDType0Font.load(doc, ttf, true);
            cs.setFont(font, 10);
            cs.beginText();
            cs.showText("PDFBOX");
            cs.endText();
            cs.close();
            doc.writeTo(new ByteArrayOutputStream());
            doc.close();
        }
    }

    /**
     * PDFBOX-3747: Test that using "-" with Calibri in Windows 7 has "-" in text extraction and not \u2010, which was
     * because of a wrong ToUnicode mapping because prior to the bugfix, CmapSubtable#getCharCodes provided values in
     * random order.
     *
     * @throws IOException
     */
    @Test
    public void testPDFBox3747() throws IOException
    {
        File file = new File("c:/windows/fonts", "calibri.ttf");
        if (!file.exists())
        {
            System.out.println("testPDFBox3747 skipped");
            return;
        }
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
        try (PDDocument doc = PDFParser
                .parse(SeekableSources.inMemorySeekableSourceFrom(baos.toByteArray())))
        {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);
            Assert.assertEquals("PDFBOX-3747", text.trim());
        }
    }
}
