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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.sejda.io.SeekableSources;
import org.sejda.sambox.input.PDFParser;
import org.sejda.sambox.pdmodel.PDDocument;
import org.sejda.sambox.rendering.PDFRenderer;
import org.sejda.sambox.rendering.TestPDFToImageTest;
import javax.imageio.ImageIO;

/**
 * Test flatten different forms and compare with rendering of original (before-flatten) document.
 * <p>
 * The tests are currently disabled to not run within the CI environment as the test results need
 * manual inspection. Enable as needed.
 */
public class PDAcroFormFlattenTest
{

    private static final File TARGETPDFDIR = new File("target/pdfs");

    private static final File IN_DIR = new File("target/test-output/flatten/in");
    private static final File OUT_DIR = new File("target/test-output/flatten/out");

    @BeforeClass
    public static void beforeAll()
    {
        IN_DIR.mkdirs();
        for (File file : IN_DIR.listFiles())
        {
            file.delete();
        }
        OUT_DIR.mkdirs();
        for (File file : OUT_DIR.listFiles())
        {
            file.delete();
        }
    }

    /*
     * PDFBOX-142 Filled template.
     */
    @Test
    public void testFlattenPDFBOX142() throws IOException
    {
        flattenAndCompare("Testformular1.pdf");
    }

    @Test
    public void testNbspaceFormFieldValue() throws IOException
    {
        PDDocument doc = PDFParser.parse(
                SeekableSources.seekableSourceFrom(new File(TARGETPDFDIR, "Testformular1.pdf")));
        PDField field = doc.getDocumentCatalog().getAcroForm().getField("Vorname");
        field.setValue("nbspace\u00A0\u202F");

        doc.writeTo(new File(TARGETPDFDIR, "Testformular1-filled-out-nbspace.pdf"));
        doc.close();

        flattenAndCompare("Testformular1-filled-out-nbspace.pdf");
    }

    /*
     * PDFBOX-563 Filled template.
     */
    @Test
    public void testFlattenPDFBOX563() throws IOException
    {
        flattenAndCompare("TestFax_56972.pdf");
    }

    /*
     * PDFBOX-2469 Empty template.
     */
    @Test
    public void testFlattenPDFBOX2469Empty() throws IOException
    {
        flattenAndCompare("FormI-9-English.pdf");
    }

    /*
     * PDFBOX-2469 Filled template.
     */
    @Test
    public void testFlattenPDFBOX2469Filled() throws IOException
    {
        flattenAndCompare("testPDF_acroForm.pdf");
    }

    /*
     * PDFBOX-2586 Empty template.
     */
    @Test
    public void testFlattenPDFBOX2586() throws IOException
    {
        flattenAndCompare("test-2586.pdf");
    }

    /*
     * PDFBOX-3083 Filled template rotated.
     */
    @Test
    public void testFlattenPDFBOX3083() throws IOException
    {
        flattenAndCompare("mypdf.pdf");
    }

    /*
     * PDFBOX-3262 Hidden fields
     */
    @Test
    public void testFlattenPDFBOX3262() throws IOException
    {
        flattenAndCompare("hidden_fields.pdf");
    }

    /*
     * PDFBOX-3396 Signed Document 1.
     */
    @Test
    public void testFlattenPDFBOX3396_1() throws IOException
    {
        flattenAndCompare("Signed-Document-1.pdf");
    }

    /*
     * PDFBOX-3396 Signed Document 2.
     */
    @Test
    public void testFlattenPDFBOX3396_2() throws IOException
    {
        flattenAndCompare("Signed-Document-2.pdf");
    }

    /*
     * PDFBOX-3396 Signed Document 3.
     */
    @Test
    public void testFlattenPDFBOX3396_3() throws IOException
    {
        flattenAndCompare("Signed-Document-3.pdf");
    }

    /*
     * PDFBOX-3396 Signed Document 4.
     */
    @Test
    public void testFlattenPDFBOX3396_4() throws IOException
    {
        flattenAndCompare("Signed-Document-4.pdf");
    }

    /*
     * PDFBOX-3587 Empty template.
     */
    @Test
    public void testFlattenOpenOfficeForm() throws IOException
    {
        flattenAndCompare("OpenOfficeForm.pdf");
    }

    /*
     * PDFBOX-3587 Filled template.
     */
    @Test
    public void testFlattenOpenOfficeFormFilled() throws IOException
    {
        flattenAndCompare("OpenOfficeForm_filled.pdf");
    }

    /**
     * PDFBOX-4157 Filled template.
     */
    @Test
    public void testFlattenPDFBox4157() throws IOException
    {
        flattenAndCompare("PDFBOX-4157-filled.pdf");
    }

    /**
     * PDFBOX-4172 Filled template.
     */
    @Test
    public void testFlattenPDFBox4172() throws IOException
    {
        flattenAndCompare("PDFBOX-4172-filled.pdf");
    }

    /**
     * PDFBOX-4615 Filled template.
     */
    @Test
    public void testFlattenPDFBox4615() throws IOException
    {
        flattenAndCompare("resetboundingbox-filled.pdf");
    }

    /**
     * PDFBOX-4693: page is not rotated, but the appearance stream is.
     */
    @Test
    public void testFlattenPDFBox4693() throws IOException
    {

        flattenAndCompare("stenotypeTest-3_rotate_no_flatten.pdf");
    }

    /**
     * PDFBOX-4788: non-widget annotations are not to be removed on a page that has no widget
     * annotations.
     */
    @Test
    public void testFlattenPDFBox4788() throws IOException
    {
        flattenAndCompare("flatten.pdf");
    }

    /**
     * PDFBOX-4889: appearance streams with empty /BBox.
     *
     * @throws IOException
     */
    @Test
    public void testFlattenPDFBox4889() throws IOException
    {
        flattenAndCompare("f1040sb test.pdf");
    }

    /**
     * PDFBOX-4955: appearance streams with forms that are not used.
     *
     * @throws IOException
     */
    @Test
    public void testFlattenPDFBox4955() throws IOException
    {
        flattenAndCompare("PDFBOX-4955.pdf");
    }

    /*
     * Flatten and compare with generated image samples.
     */
    private void flattenAndCompare(String fileName) throws IOException
    {
        File inputFile = new File(TARGETPDFDIR, fileName);
        File outputFile = new File(OUT_DIR, fileName);

        generateScreenshotsBefore(inputFile, IN_DIR);

        try (PDDocument doc = PDFParser.parse(SeekableSources.seekableSourceFrom(inputFile)))
        {
            doc.getDocumentCatalog().getAcroForm().flatten();
            assertTrue(doc.getDocumentCatalog().getAcroForm().getFields().isEmpty());
            doc.writeTo(outputFile);
        }

        // compare rendering
        TestPDFToImageTest testPDFToImage = new TestPDFToImageTest(this.getClass().getName());
        if (!testPDFToImage.doTestFile(outputFile, IN_DIR, OUT_DIR))
        {
            System.out.println("Rendering of " + outputFile
                    + " failed or is not identical to expected rendering in "
                    + inputFile.getParent() + " directory;");

            fail("Test failed");
        }
    }

    private void generateScreenshotsBefore(File inputFile, File destinationFolder)
            throws IOException
    {
        try (PDDocument document = PDFParser.parse(SeekableSources.seekableSourceFrom(inputFile)))
        {

            String outputPrefix = inputFile.getName() + "-";
            int numPages = document.getNumberOfPages();

            PDFRenderer renderer = new PDFRenderer(document);
            for (int i = 0; i < numPages; i++)
            {
                String fileName = outputPrefix + (i + 1) + ".png";
                BufferedImage image = renderer.renderImageWithDPI(i, 96); // Windows native DPI
                ImageIO.write(image, "PNG", new File(destinationFolder, fileName));
            }
        }
    }
}
