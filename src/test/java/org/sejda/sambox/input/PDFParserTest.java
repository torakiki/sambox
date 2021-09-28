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

import java.io.File;
import java.io.IOException;

import org.junit.Test;
import org.sejda.io.DevNullWritableByteChannel;
import org.sejda.io.SeekableSources;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.pdmodel.PDDocument;
import org.sejda.sambox.pdmodel.PDDocumentInformation;
import org.sejda.sambox.rendering.PDFRenderer;
import org.sejda.sambox.text.PDFTextStripper;
import org.sejda.sambox.util.DateConverter;
import org.sejda.sambox.util.SpecVersionUtils;

/**
 * @author Andrea Vacondio
 *
 */
public class PDFParserTest
{

    private static final File TARGETPDFDIR = new File("target/pdfs");

    @Test
    public void positive() throws IOException
    {
        PDDocument doc = PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(
                getClass().getResourceAsStream("/sambox/simple_test.pdf")));
        assertNotNull(doc);
        assertTrue(doc.isOpen());
        assertFalse(doc.hasParseErrors());
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
            assertFalse(doc.hasParseErrors());
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
            assertFalse(doc.hasParseErrors());
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

    @Test
    public void truncatedHeader() throws IOException
    {
        try (PDDocument doc = PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(
                getClass().getResourceAsStream("/sambox/trunkated_header.pdf"))))
        {
            assertNotNull(doc);
            assertFalse(doc.isEncrypted());
            assertTrue(doc.isOpen());
            assertEquals(SpecVersionUtils.V1_6, doc.getVersion());
            assertTrue(doc.hasParseErrors());
        }
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
    public void noVersionNumberHeader() throws IOException
    {
        try (PDDocument doc = PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(
                getClass().getResourceAsStream("/sambox/no_version_header.pdf"))))
        {
            assertNotNull(doc);
            assertFalse(doc.isEncrypted());
            assertTrue(doc.isOpen());
            assertEquals(SpecVersionUtils.V1_6, doc.getVersion());
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

    @Test
    public void testPDFBox3208() throws IOException
    {
        try (PDDocument doc = PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(getClass()
                .getResourceAsStream(
                        "/org/sejda/sambox/input/PDFBOX-3208-L33MUTT2SVCWGCS6UIYL5TH3PNPXHIS6.pdf"))))
        {
            PDDocumentInformation di = doc.getDocumentInformation();
            assertEquals("Liquent Enterprise Services", di.getAuthor());
            assertEquals("Liquent services server", di.getCreator());
            assertEquals("Amyuni PDF Converter version 4.0.0.9", di.getProducer());
            assertEquals("", di.getKeywords());
            assertEquals("", di.getSubject());
            assertEquals("892B77DE781B4E71A1BEFB81A51A5ABC_20140326022424.docx", di.getTitle());
            assertEquals(DateConverter.toCalendar("D:20140326142505-02'00'"), di.getCreationDate());
            assertEquals(DateConverter.toCalendar("20140326172513Z"), di.getModificationDate());
            assertTrue(doc.hasParseErrors());
        }
    }

    @Test
    public void testPDFBox3940() throws IOException
    {
        try (PDDocument doc = PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(
                getClass().getResourceAsStream("/org/sejda/sambox/input/PDFBOX-3940-079977.pdf"))))
        {
            PDDocumentInformation di = doc.getDocumentInformation();
            assertEquals("Unknown", di.getAuthor());
            assertEquals("C:REGULA~1IREGSFR_EQ_EM.WP", di.getCreator());
            assertEquals("Acrobat PDFWriter 3.02 for Windows", di.getProducer());
            assertEquals("", di.getKeywords());
            assertEquals("", di.getSubject());
            assertEquals("C:REGULA~1IREGSFR_EQ_EM.PDF", di.getTitle());
            assertEquals(DateConverter.toCalendar("Tuesday, July 28, 1998 4:00:09 PM"),
                    di.getCreationDate());
            assertTrue(doc.hasParseErrors());
        }
    }

    /**
     * PDFBOX-3783: test parsing of file with trash after %%EOF.
     * 
     * @throws IOException
     */
    @Test
    public void testPDFBox3783() throws IOException
    {
        try (PDDocument doc = PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(getClass()
                .getResourceAsStream(
                        "/org/sejda/sambox/input/PDFBOX-3783-72GLBIGUC6LB46ELZFBARRJTLN4RBSQM.pdf"))))
        {
            // noop
            assertTrue(doc.hasParseErrors());
        }
    }

    /**
     * Test parsing the "genko_oc_shiryo1.pdf" file, which is susceptible to regression.
     * 
     * @throws IOException
     */
    @Test
    public void testParseGenko() throws IOException
    {
        try (PDDocument doc = PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(getClass()
                .getResourceAsStream("/org/sejda/sambox/input/genko_oc_shiryo1.pdf"))))
        {
            // noop
        }
    }

    /**
     * PDFBOX-3964: test parsing of broken file.
     *
     * @throws IOException
     */
    @Test
    public void testPDFBox3964() throws IOException
    {
        PDDocument doc = PDDocument.load(new File(TARGETPDFDIR, "PDFBOX-3964-c687766d68ac766be3f02aaec5e0d713_2.pdf"));
        assertEquals(10, doc.getNumberOfPages());
        doc.close();
    }

    /**
     * PDFBOX-3703: ArrayIndexOutOfBoundsException in PDDeviceRGB.toRGB()
     */
    @Test
    public void testPDFBox3703() throws IOException
    {
        try(PDDocument doc = PDDocument.load(new File(TARGETPDFDIR, "PDFBOX-3703-966635-p12.pdf")))
        {
            doc.writeTo(new DevNullWritableByteChannel());
            doc.close();
        }
    }

    /**
     * PDFBOX-3951: test parsing of truncated file.
     *
     * @throws IOException
     */
    @Test
    public void testPDFBox3951() throws IOException
    {
        PDDocument doc = PDDocument.load(new File(TARGETPDFDIR, "PDFBOX-3951-FIHUZWDDL2VGPOE34N6YHWSIGSH5LVGZ.pdf"));
        assertEquals(143, doc.getNumberOfPages());
        doc.close();
    }

    /**
     * PDFBOX-3950: test parsing and rendering of truncated file with missing pages.
     *
     * @throws IOException
     */
    @Test
    public void testPDFBox3950() throws IOException
    {
        PDDocument doc = PDDocument.load(new File(TARGETPDFDIR, "PDFBOX-3950-23EGDHXSBBYQLKYOKGZUOVYVNE675PRD.pdf"));
        assertEquals(8, doc.getNumberOfPages());
        PDFRenderer renderer = new PDFRenderer(doc);
        for (int i = 0; i < doc.getNumberOfPages(); ++i)
        {
            try
            {
                renderer.renderImage(i);
            }
            catch (IOException ex)
            {
                if (i == 3 && ex.getMessage().equals("Missing descendant font array"))
                {
                    continue;
                }
                throw ex;
            }
        }
        doc.close();
    }

    /**
     * PDFBOX-3949: test parsing of file with incomplete object stream.
     *
     * @throws IOException
     */
    @Test
    public void testPDFBox3949() throws IOException
    {
        PDDocument.load(new File(TARGETPDFDIR, "PDFBOX-3949-MKFYUGZWS3OPXLLVU2Z4LWCTVA5WNOGF.pdf")).close();
    }

    /**
     * PDFBOX-3948: test parsing of file with object stream containing some unexpected newlines.
     *
     * @throws IOException
     */
    @Test
    public void testPDFBox3948() throws IOException
    {
        PDDocument.load(new File(TARGETPDFDIR, "PDFBOX-3948-EUWO6SQS5TM4VGOMRD3FLXZHU35V2CP2.pdf")).close();
    }

    @Test
    public void loopInIndirectObject() throws IOException
    {
        try (PDDocument doc = PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(
                getClass().getResourceAsStream("/sambox/self_indirect_ref.pdf"))))
        {
            doc.writeTo(new DevNullWritableByteChannel());
            doc.close();
        }
    }

    @Test
    public void testPDFBOX4372() throws IOException {
        try (PDDocument doc = PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(
                getClass().getResourceAsStream("/org/sejda/sambox/input/PDFBOX-4372-2DAYCLVOFG3FTVO4RMAJJL3VTPNYDFRO-p4_reduced.pdf"))))
        {

            PDFTextStripper textStripper = new PDFTextStripper();
            textStripper.getText(doc);

            doc.close();
        }
    }
}
