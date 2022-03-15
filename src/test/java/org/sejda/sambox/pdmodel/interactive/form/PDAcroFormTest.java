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
import org.junit.Before;
import org.junit.Test;
import org.sejda.commons.FastByteArrayOutputStream;
import org.sejda.io.SeekableSources;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.input.PDFParser;
import org.sejda.sambox.pdmodel.PDDocument;
import org.sejda.sambox.pdmodel.PDDocumentCatalog;
import org.sejda.sambox.pdmodel.PDPage;
import org.sejda.sambox.pdmodel.PDResources;
import org.sejda.sambox.pdmodel.common.PDRectangle;
import org.sejda.sambox.pdmodel.font.PDFont;
import org.sejda.sambox.pdmodel.font.PDType1Font;
import org.sejda.sambox.pdmodel.interactive.annotation.PDAnnotation;
import org.sejda.sambox.pdmodel.interactive.annotation.PDAnnotationWidget;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test for the PDButton class.
 */
public class PDAcroFormTest
{

    private PDDocument document;
    private PDAcroForm form;

    @Before
    public void setUp()
    {
        document = new PDDocument();
        form = new PDAcroForm(document);
        document.getDocumentCatalog().setAcroForm(form);
    }

    @Test
    public void testFieldsEntry()
    {
        // the /Fields entry has been created with the AcroForm
        // as this is a required entry
        assertNotNull(form.getFields());
        assertEquals(form.getFields().size(), 0);

        // there shouldn't be an exception if there is no such field
        assertNull(form.getField("foo"));

        // remove the required entry which is the case for some
        // PDFs (see PDFBOX-2965)
        form.getCOSObject().removeItem(COSName.FIELDS);

        // ensure there is always an empty collection returned
        assertNotNull(form.getFields());
        assertEquals(form.getFields().size(), 0);

        // there shouldn't be an exception if there is no such field
        assertNull(form.getField("foo"));
        assertEquals("", form.getDefaultAppearance());
    }

    @Test
    public void testAcroFormProperties()
    {
        assertTrue(form.getDefaultAppearance().isEmpty());
        form.setDefaultAppearance("/Helv 0 Tf 0 g");
        assertEquals(form.getDefaultAppearance(), "/Helv 0 Tf 0 g");
    }

    @Test
    public void testFlatten() throws IOException
    {
        try (PDDocument doc = PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(
                getClass().getResourceAsStream(
                        "/org/sejda/sambox/pdmodel/interactive/form/AlignmentTests.pdf"))))
        {
            doc.getDocumentCatalog().getAcroForm().flatten();
            doc.writeTo(new ByteArrayOutputStream());
        }
    }

    /*
     * Same as above but remove the page reference from the widget annotation before doing the flatten() to ensure that
     * the widgets page reference is properly looked up (PDFBOX-3301)
     */
    @Test
    public void testFlattenWidgetNoRef() throws IOException
    {
        try (PDDocument doc = PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(
                getClass().getResourceAsStream(
                        "/org/sejda/sambox/pdmodel/interactive/form/AlignmentTests.pdf"))))
        {
            PDAcroForm acroForm = doc.getDocumentCatalog().getAcroForm();
            for (PDField field : acroForm.getFieldTree())
            {
                for (PDAnnotationWidget widget : field.getWidgets())
                {
                    widget.getCOSObject().removeItem(COSName.P);
                }
            }
            doc.getDocumentCatalog().getAcroForm().flatten();
            // 36 non widget annotations shall not be flattened
            assertEquals(36, doc.getPage(0).getAnnotations().size());
            assertTrue(doc.getDocumentCatalog().getAcroForm().getFields().isEmpty());
        }
    }

    @Test
    public void testFlattenSpecificFieldsOnly() throws IOException
    {

        try (PDDocument doc = PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(
                getClass().getResourceAsStream(
                        "/org/sejda/sambox/pdmodel/interactive/form/AlignmentTests.pdf"))))
        {
            List<PDField> fieldsToFlatten = new ArrayList<>();
            PDAcroForm acroFormToFlatten = doc.getDocumentCatalog().getAcroForm();
            int numFieldsBeforeFlatten = acroFormToFlatten.getFields().size();
            int numWidgetsBeforeFlatten = countWidgets(doc);

            fieldsToFlatten.add(acroFormToFlatten.getField("AlignLeft-Border_Small-Filled"));
            fieldsToFlatten.add(acroFormToFlatten.getField("AlignLeft-Border_Medium-Filled"));
            fieldsToFlatten.add(acroFormToFlatten.getField("AlignLeft-Border_Wide-Filled"));
            fieldsToFlatten.add(acroFormToFlatten.getField("AlignLeft-Border_Wide_Clipped-Filled"));

            acroFormToFlatten.flatten(fieldsToFlatten, true);
            int numFieldsAfterFlatten = acroFormToFlatten.getFields().size();
            int numWidgetsAfterFlatten = countWidgets(doc);

            assertEquals(numFieldsBeforeFlatten, numFieldsAfterFlatten + fieldsToFlatten.size());
            assertEquals(numWidgetsBeforeFlatten, numWidgetsAfterFlatten + fieldsToFlatten.size());
        }
    }

    /**
     * calling flatten() removes all the fields, also non terminal in case of a hierarchy
     *
     * @throws IOException
     */
    @Test
    public void testFlattenAllRemovesFields() throws IOException
    {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream();
        try (PDDocument doc = PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(
                getClass().getResourceAsStream(
                        "/org/sejda/sambox/pdmodel/interactive/form/flatten_fields_hierarchy.pdf"))))
        {
            PDAcroForm acroForm = doc.getDocumentCatalog().getAcroForm();
            acroForm.flatten();
            doc.writeTo(out);
        }
        try (PDDocument doc = PDFParser.parse(
                SeekableSources.inMemorySeekableSourceFrom(out.toByteArray())))
        {
            PDAcroForm acroForm = doc.getDocumentCatalog().getAcroForm();
            assertNotNull(acroForm);
            assertTrue(acroForm.getFields().isEmpty());
        }
    }

    /**
     * PDFBOX_3941 and PDFBOX-3809
     *
     * @throws IOException
     */
    @Test
    public void testFlattenOnySomeFields() throws IOException
    {
        flattenOnlySome("/org/sejda/sambox/pdmodel/interactive/form/flatten_fields.pdf");
    }

    /**
     * PDFBOX_3941 and PDFBOX-3809
     *
     * @throws IOException
     */
    @Test
    public void testFlattenOnySomeFieldsWithHierarchy() throws IOException
    {
        flattenOnlySome("/org/sejda/sambox/pdmodel/interactive/form/flatten_fields_hierarchy.pdf");

    }

    public void flattenOnlySome(String victim) throws IOException
    {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream();
        try (PDDocument doc = PDFParser.parse(
                SeekableSources.inMemorySeekableSourceFrom(getClass().getResourceAsStream(victim))))
        {
            PDAcroForm acroForm = doc.getDocumentCatalog().getAcroForm();
            List<PDField> fields = new ArrayList<>();
            fields.add(acroForm.getField("flatten"));
            acroForm.flatten(fields, true);
            doc.writeTo(out);
        }
        try (PDDocument doc = PDFParser.parse(
                SeekableSources.inMemorySeekableSourceFrom(out.toByteArray())))
        {
            PDAcroForm acroForm = doc.getDocumentCatalog().getAcroForm();
            assertNotNull(acroForm);
            assertNull(acroForm.getField("flatten"));
            assertNotNull(acroForm.getField("do_not_flatten"));
        }
    }

    /**
     * PDFBOX-4235: a bad /DA string should not result in an NPE.
     *
     * @throws IOException
     */
    @Test
    public void testBadDA() throws IOException
    {
        PDDocument doc = new PDDocument();

        PDPage page = new PDPage();
        doc.addPage(page);

        PDAcroForm acroForm = new PDAcroForm(document);
        doc.getDocumentCatalog().setAcroForm(acroForm);
        acroForm.setDefaultResources(new PDResources());

        PDTextField textBox = new PDTextField(acroForm);
        textBox.setPartialName("SampleField");

        // https://stackoverflow.com/questions/50609478/
        // "tf" is a typo, should have been "Tf" and this results that no font is chosen
        textBox.setDefaultAppearance("/Helv 0 tf 0 g");
        acroForm.getFields().add(textBox);

        PDAnnotationWidget widget = textBox.getWidgets().get(0);
        PDRectangle rect = new PDRectangle(50, 750, 200, 20);
        widget.setRectangle(rect);
        widget.setPage(page);

        page.getAnnotations().add(widget);

        try
        {
            textBox.setValue("huhu");
        }
        catch (IllegalArgumentException ex)
        {
            return;
        }
        finally
        {
            doc.close();
        }
        fail("IllegalArgumentException should have been thrown");
    }

    @Test
    public void remove()
    {
        PDNonTerminalField b = new PDNonTerminalField(form);
        b.setPartialName("B");
        PDNonTerminalField d = new PDNonTerminalField(form);
        d.setPartialName("D");
        PDNonTerminalField g = new PDNonTerminalField(form);
        g.setPartialName("G");
        PDNonTerminalField i = new PDNonTerminalField(form);
        i.setPartialName("I");
        PDTextField a = new PDTextField(form);
        a.setPartialName("A");
        PDTextField c = new PDTextField(form);
        c.setPartialName("C");
        PDTextField h = new PDTextField(form);
        h.setPartialName("H");
        PDTextField e = new PDTextField(form);
        e.setPartialName("E");
        b.addChild(a);
        b.addChild(d);
        d.addChild(c);
        d.addChild(e);
        g.addChild(i);
        i.addChild(h);
        form.addFields(Arrays.asList(b, g));
        assertNotNull(form.getField("B"));
        assertNotNull(form.getField("B.D.C"));
        // removes only from root
        assertNull(form.removeField(e));
        assertNotNull(form.removeField(PDFieldFactory.createField(form, b.getCOSObject(), null)));
        assertNull(form.getField("B"));
        assertNull(form.getField("B.D.C"));
    }

    @Test
    public void calculatingOrder() throws IOException
    {
        try (PDDocument doc = PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(
                getClass().getResourceAsStream(
                        "/org/sejda/sambox/pdmodel/interactive/form/test_form_with_calc.pdf"))))
        {
            PDAcroForm form = doc.getDocumentCatalog().getAcroForm();
            assertEquals(2, form.getFields().size());
            assertEquals(1, form.getCalculationOrder().size());
            assertTrue(form.getFields().contains(form.getCalculationOrder().get(0)));
        }
    }

    /**
     * Test that we do not modify an AcroForm with missing resource information when loading the
     * document only. (PDFBOX-3752)
     */
    @Test
    public void testDontAddMissingInformationOnDocumentLoad() throws IOException
    {
        try (PDDocument document = PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(
                createAcroFormWithMissingResourceInformation())))
        {
            // do a low level access to the AcroForm to avoid the generation of missing entries
            PDDocumentCatalog documentCatalog = document.getDocumentCatalog();
            COSDictionary catalogDictionary = documentCatalog.getCOSObject();
            COSDictionary acroFormDictionary = (COSDictionary) catalogDictionary.getDictionaryObject(
                    COSName.ACRO_FORM);

            // ensure that the missing information has not been generated
            assertNull(acroFormDictionary.getDictionaryObject(COSName.DA));
            assertNull(acroFormDictionary.getDictionaryObject(COSName.RESOURCES));
        }
    }

    /**
     * Test that we add missing ressouce information to an AcroForm when accessing the AcroForm on
     * the PD level (PDFBOX-3752)
     *
     * @throws IOException
     */
    @Test
    public void testAddMissingInformationOnAcroFormAccess() throws IOException
    {
        try (PDDocument document = PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(
                createAcroFormWithMissingResourceInformation())))
        {
            PDDocumentCatalog documentCatalog = document.getDocumentCatalog();

            // this call shall trigger the generation of missing information
            PDAcroForm theAcroForm = documentCatalog.getAcroForm();

            // ensure that the missing information has been generated
            // DA entry
            assertEquals("/Helv 0 Tf 0 g ", theAcroForm.getDefaultAppearance());
            assertNotNull(theAcroForm.getDefaultResources());

            // DR entry
            PDResources acroFormResources = theAcroForm.getDefaultResources();
            assertNotNull(acroFormResources.getFont(COSName.getPDFName("Helv")));
            assertEquals("Helvetica",
                    acroFormResources.getFont(COSName.getPDFName("Helv")).getName());
            assertNotNull(acroFormResources.getFont(COSName.getPDFName("ZaDb")));
            assertEquals("ZapfDingbats",
                    acroFormResources.getFont(COSName.getPDFName("ZaDb")).getName());

        }
    }

    /**
     * PDFBOX-3732, PDFBOX-4303, PDFBOX-4393: Test whether /Helv and /ZaDb get added, but only if
     * they don't exist.
     */
    @Test
    public void testAcroFormDefaultFonts() throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (PDDocument document = new PDDocument())
        {

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            PDAcroForm acroForm2 = new PDAcroForm(document);
            document.getDocumentCatalog().setAcroForm(acroForm2);
            PDResources defaultResources = acroForm2.getDefaultResources();
            assertNull(defaultResources);
            defaultResources = new PDResources();
            acroForm2.setDefaultResources(defaultResources);
            assertNull(defaultResources.getFont(COSName.HELV));
            assertNull(defaultResources.getFont(COSName.ZA_DB));

            // getting AcroForm sets the two fonts
            acroForm2 = document.getDocumentCatalog().getAcroForm();
            defaultResources = acroForm2.getDefaultResources();
            assertNotNull(defaultResources.getFont(COSName.HELV));
            assertNotNull(defaultResources.getFont(COSName.ZA_DB));

            // repeat with a new AcroForm (to delete AcroForm cache) and thus missing /DR
            document.getDocumentCatalog().setAcroForm(new PDAcroForm(document));
            acroForm2 = document.getDocumentCatalog().getAcroForm();
            defaultResources = acroForm2.getDefaultResources();
            PDFont helv = defaultResources.getFont(COSName.HELV);
            PDFont zadb = defaultResources.getFont(COSName.ZA_DB);
            assertNotNull(helv);
            assertNotNull(zadb);
            document.writeTo(baos); // this is a working PDF
        }
        try (PDDocument doc = PDFParser.parse(
                SeekableSources.inMemorySeekableSourceFrom(baos.toByteArray())))
        {
            PDAcroForm acroForm2 = doc.getDocumentCatalog().getAcroForm();
            PDResources defaultResources = acroForm2.getDefaultResources();
            PDFont helv = defaultResources.getFont(COSName.HELV);
            PDFont zadb = defaultResources.getFont(COSName.ZA_DB);
            assertNotNull(helv);
            assertNotNull(zadb);
            // make sure that font wasn't overwritten
            assertNotEquals(PDType1Font.HELVETICA, helv);
            assertNotEquals(PDType1Font.ZAPF_DINGBATS, zadb);
        }
    }

    @Test
    public void testIllegalFieldsDefinition() throws IOException
    {
        {
            try (PDDocument testPdf = PDFParser.parse(
                    SeekableSources.seekableSourceFrom(new File("target/pdfs/D1790B.PDF"))))
            {
                testPdf.getDocumentCatalog().getAcroForm();
            }
        }
    }

    @After
    public void tearDown() throws IOException
    {
        document.close();
    }

    private byte[] createAcroFormWithMissingResourceInformation() throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PDDocument document = new PDDocument())
        {
            PDPage page = new PDPage();
            document.addPage(page);

            PDAcroForm newAcroForm = new PDAcroForm(document);
            document.getDocumentCatalog().setAcroForm(newAcroForm);

            PDTextField textBox = new PDTextField(newAcroForm);
            textBox.setPartialName("SampleField");
            newAcroForm.getFields().add(textBox);

            PDAnnotationWidget widget = textBox.getWidgets().get(0);
            PDRectangle rect = new PDRectangle(50, 750, 200, 20);
            widget.setRectangle(rect);
            widget.setPage(page);

            page.getAnnotations().add(widget);

            // acroForm.setNeedAppearances(true);
            // acroForm.getField("SampleField").getCOSObject().setString(COSName.V, "content");

            document.writeTo(baos); // this is a working PDF
        }
        return baos.toByteArray();
    }

    private int countWidgets(PDDocument documentToTest)
    {
        int count = 0;
        for (PDPage page : documentToTest.getPages())
        {
            for (PDAnnotation annotation : page.getAnnotations())
            {
                if (annotation instanceof PDAnnotationWidget)
                {
                    count++;
                }
            }
        }
        return count;
    }
}
