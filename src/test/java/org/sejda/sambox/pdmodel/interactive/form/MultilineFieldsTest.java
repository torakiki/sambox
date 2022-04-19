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

package org.sejda.sambox.pdmodel.interactive.form;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sejda.commons.util.IOUtils;
import org.sejda.io.SeekableSources;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSNumber;
import org.sejda.sambox.cos.COSString;
import org.sejda.sambox.input.ContentStreamParser;
import org.sejda.sambox.input.PDFParser;
import org.sejda.sambox.pdmodel.PDDocument;
import org.sejda.sambox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.sejda.sambox.rendering.TestPDFToImageTest;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class MultilineFieldsTest
{
    private static final File OUT_DIR = new File("target/test-output");
    private static final File IN_DIR = new File(
            "src/test/resources/org/sejda/sambox/pdmodel/interactive/form");
    private static final String NAME_OF_PDF = "MultilineFields.pdf";
    private static final String TEST_VALUE = "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, "
            + "sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam";

    private PDDocument document;
    private PDAcroForm acroForm;

    @Before
    public void setUp() throws IOException
    {
        document = PDFParser
                .parse(SeekableSources.seekableSourceFrom(new File(IN_DIR, NAME_OF_PDF)));
        acroForm = document.getDocumentCatalog().getAcroForm();
        OUT_DIR.mkdirs();
    }

    @Test
    public void fillFields() throws IOException
    {
        PDTextField field = (PDTextField) acroForm.getField("AlignLeft");
        field.setValue(TEST_VALUE);

        field = (PDTextField) acroForm.getField("AlignMiddle");
        field.setValue(TEST_VALUE);

        field = (PDTextField) acroForm.getField("AlignRight");
        field.setValue(TEST_VALUE);

        field = (PDTextField) acroForm.getField("AlignLeft-Border_Small");
        field.setValue(TEST_VALUE);

        field = (PDTextField) acroForm.getField("AlignMiddle-Border_Small");
        field.setValue(TEST_VALUE);

        field = (PDTextField) acroForm.getField("AlignRight-Border_Small");
        field.setValue(TEST_VALUE);

        field = (PDTextField) acroForm.getField("AlignLeft-Border_Medium");
        field.setValue(TEST_VALUE);

        field = (PDTextField) acroForm.getField("AlignMiddle-Border_Medium");
        field.setValue(TEST_VALUE);

        field = (PDTextField) acroForm.getField("AlignRight-Border_Medium");
        field.setValue(TEST_VALUE);

        field = (PDTextField) acroForm.getField("AlignLeft-Border_Wide");
        field.setValue(TEST_VALUE);

        field = (PDTextField) acroForm.getField("AlignMiddle-Border_Wide");
        field.setValue(TEST_VALUE);

        field = (PDTextField) acroForm.getField("AlignRight-Border_Wide");
        field.setValue(TEST_VALUE);

        // compare rendering
        File file = new File(OUT_DIR, NAME_OF_PDF);
        document.writeTo(file);
        TestPDFToImageTest testPDFToImage = new TestPDFToImageTest(TestPDFToImageTest.class.getName());
        if (!testPDFToImage.doTestFile(file, IN_DIR.getAbsolutePath(), OUT_DIR.getAbsolutePath()))
        {
            Assert.fail(
                    "Rendering of " + file + " failed or is not identical to expected rendering in "
                            + IN_DIR + " directory");
        }
    }

    // Test for PDFBOX-3812
    @Test
    public void testMultilineAuto() throws IOException
    {
        try (PDDocument document = PDFParser.parse(SeekableSources.seekableSourceFrom(
                new File(IN_DIR, "PDFBOX3812-acrobat-multiline-auto.pdf"))))
        {
            PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();

            // Get and store the field sizes in the original PDF
            PDTextField fieldMultiline = (PDTextField) acroForm.getField("Multiline");
            float fontSizeMultiline = getFontSizeFromAppearanceStream(fieldMultiline);

            PDTextField fieldSingleline = (PDTextField) acroForm.getField("Singleline");
            float fontSizeSingleline = getFontSizeFromAppearanceStream(fieldSingleline);

            PDTextField fieldMultilineAutoscale = (PDTextField) acroForm.getField(
                    "MultilineAutoscale");
            float fontSizeMultilineAutoscale = getFontSizeFromAppearanceStream(
                    fieldMultilineAutoscale);

            PDTextField fieldSinglelineAutoscale = (PDTextField) acroForm.getField(
                    "SinglelineAutoscale");
            float fontSizeSinglelineAutoscale = getFontSizeFromAppearanceStream(
                    fieldSinglelineAutoscale);

            fieldMultiline.setValue("Multiline - Fixed");
            fieldSingleline.setValue("Singleline - Fixed");
            fieldMultilineAutoscale.setValue("Multiline - auto");
            fieldSinglelineAutoscale.setValue("Singleline - auto");

            assertEquals(fontSizeMultiline, getFontSizeFromAppearanceStream(fieldMultiline),
                    0.001f);
            assertEquals(fontSizeSingleline, getFontSizeFromAppearanceStream(fieldSingleline),
                    0.001f);
            assertEquals(fontSizeMultilineAutoscale,
                    getFontSizeFromAppearanceStream(fieldMultilineAutoscale), 0.001f);
            assertEquals(fontSizeSinglelineAutoscale,
                    getFontSizeFromAppearanceStream(fieldSinglelineAutoscale), 0.025f);
        }
    }

    // Test for PDFBOX-3812
    @Test
    public void testMultilineBreak() throws IOException
    {
        final String TEST_PDF = "PDFBOX-3835-input-acrobat-wrap.pdf";
        try (PDDocument document = PDFParser.parse(
                SeekableSources.seekableSourceFrom(new File(IN_DIR, TEST_PDF))))
        {
            PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();

            // Get and store the field sizes in the original PDF
            PDTextField fieldInput = (PDTextField) acroForm.getField("filled");
            String fieldValue = fieldInput.getValue();
            List<String> acrobatLines = getTextLinesFromAppearanceStream(fieldInput);
            fieldInput.setValue(fieldValue);
            List<String> pdfboxLines = getTextLinesFromAppearanceStream(fieldInput);
            assertEquals("Number of lines generated by PDFBox shall match Acrobat",
                    acrobatLines.size(), pdfboxLines.size());
            for (int i = 0; i < acrobatLines.size(); i++)
            {
                assertEquals(
                        "Number of characters per lines generated by PDFBox shall match Acrobat",
                        acrobatLines.get(i).length(), pdfboxLines.get(i).length());
            }
        }
    }

    private float getFontSizeFromAppearanceStream(PDField field) throws IOException
    {
        PDAnnotationWidget widget = field.getWidgets().get(0);
        ContentStreamParser parser = new ContentStreamParser(widget.getNormalAppearanceStream());

        Object token = parser.nextParsedToken();

        while (token != null)
        {
            if (token instanceof COSName && ((COSName) token).getName().equals("Helv"))
            {
                token = parser.nextParsedToken();
                if (token instanceof COSNumber)
                {
                    return ((COSNumber) token).floatValue();
                }
            }
            token = parser.nextParsedToken();
        }
        return 0;
    }

    private List<String> getTextLinesFromAppearanceStream(PDField field) throws IOException
    {
        PDAnnotationWidget widget = field.getWidgets().get(0);
        ContentStreamParser parser = new ContentStreamParser(widget.getNormalAppearanceStream());

        Object token = parser.nextParsedToken();

        List<String> lines = new ArrayList<String>();

        while (token != null)
        {
            if (token instanceof COSString)
            {
                lines.add(((COSString) token).getString());
            }
            token = parser.nextParsedToken();
        }
        return lines;
    }

    @After
    public void tearDown() throws IOException
    {
        IOUtils.close(document);
    }

}
