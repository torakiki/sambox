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

import org.junit.Test;
import org.sejda.io.SeekableSources;
import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSBoolean;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.input.PDFParser;
import org.sejda.sambox.pdmodel.PDDocument;
import org.sejda.sambox.pdmodel.PDDocumentCatalog;
import org.sejda.sambox.pdmodel.PDResources;
import org.sejda.sambox.pdmodel.font.PDFont;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests for building AcroForm entries form Widget annotations.
 */
public class PDAcroFormFromAnnotsTest
{
    /**
     * PDFBOX-4985 AcroForms entry but empty Fields array
     * <p>
     * Using the default get acroform call with error correction
     *
     * @throws IOException
     */
    @Test
    public void testFromAnnots4985DefaultMode() throws IOException
    {

        int numFormFieldsByAcrobat = 0;

        try (PDDocument testPdf = PDFParser.parse(SeekableSources.seekableSourceFrom(
                new File("target/pdfs/POPPLER-806-acrobat.pdf"))))
        {
            PDDocumentCatalog catalog = testPdf.getDocumentCatalog();
            PDAcroForm acroForm = catalog.getAcroForm();
            numFormFieldsByAcrobat = acroForm.getFields().size();
        }

        try (PDDocument testPdf = PDFParser.parse(
                SeekableSources.seekableSourceFrom(new File("target/pdfs/POPPLER-806.pdf"))))
        {
            PDDocumentCatalog catalog = testPdf.getDocumentCatalog();
            // need to do a low level cos access as the PDModel access will build the AcroForm 
            COSDictionary cosAcroForm = (COSDictionary) catalog.getCOSObject()
                    .getDictionaryObject(COSName.ACRO_FORM);
            COSArray cosFields = cosAcroForm.getDictionaryObject(COSName.FIELDS, COSArray.class);
            assertEquals("Initially there shall be 0 fields", 0, cosFields.size());
            PDAcroForm acroForm = catalog.getAcroFromWithFixups();
            assertEquals("After rebuild there shall be " + numFormFieldsByAcrobat + " fields",
                    numFormFieldsByAcrobat, acroForm.getFields().size());
        }
    }

    /**
     * PDFBOX-3891 AcroForm with empty fields entry
     * <p>
     * Special fixup to create fields
     *
     * @throws IOException
     */
    @Test
    public void testFromAnnots3891CreateFields() throws IOException
    {

        int numFormFieldsByAcrobat = 0;

        // will build the expected fields using the acrobat source document
        Map<String, PDField> fieldsByName = new HashMap<>();

        try (PDDocument testPdf = PDFParser.parse(SeekableSources.seekableSourceFrom(
                new File("target/pdfs/merge-test-na-acrobat.pdf"))))
        {
            PDDocumentCatalog catalog = testPdf.getDocumentCatalog();
            PDAcroForm acroForm = catalog.getAcroForm();
            numFormFieldsByAcrobat = acroForm.getFields().size();
            for (PDField field : acroForm.getFieldTree())
            {
                fieldsByName.put(field.getFullyQualifiedName(), field);
            }
        }

        try (PDDocument testPdf = PDFParser.parse(
                SeekableSources.seekableSourceFrom(new File("target/pdfs/merge-test.pdf"))))
        {
            PDDocumentCatalog catalog = testPdf.getDocumentCatalog();
            // need to do a low level cos access as the PDModel access will build the AcroForm
            COSDictionary cosAcroForm = catalog.getCOSObject()
                    .getDictionaryObject(COSName.ACRO_FORM, COSDictionary.class);
            cosAcroForm.setItem(COSName.NEED_APPEARANCES, COSBoolean.TRUE);
            COSArray cosFields = cosAcroForm.getDictionaryObject(COSName.FIELDS, COSArray.class);
            assertEquals("Initially there shall be 0 fields", 0, cosFields.size());
            PDAcroForm acroForm = catalog.getAcroFromWithFixups();
            assertEquals("After rebuild there shall be " + numFormFieldsByAcrobat + " fields",
                    numFormFieldsByAcrobat, acroForm.getFields().size());

            // the the fields found are contained in the map
            for (PDField field : acroForm.getFieldTree())
            {
                assertNotNull(fieldsByName.get(field.getFullyQualifiedName()));
            }

            // test all fields in the map are also found in the AcroForm
            for (String fieldName : fieldsByName.keySet())
            {
                assertNotNull(acroForm.getField(fieldName));
            }
        }
    }

    /**
     * PDFBOX-3891 AcroForm with empty fields entry
     * <p>
     * Check if the font resources added by PDFBox matches these by Acrobat which are taken from the
     * widget normal appearance resources
     *
     * @throws IOException
     */
    @Test
    public void testFromAnnots3891ValidateFont() throws IOException
    {

        // will build the expected font respurce names and font decriptor names using the acrobat source document
        Map<String, String> fontNames = new HashMap<>();
        try (PDDocument testPdf = PDFParser.parse(SeekableSources.seekableSourceFrom(
                new File("target/pdfs/merge-test-na-acrobat.pdf"))))
        {
            PDDocumentCatalog catalog = testPdf.getDocumentCatalog();
            PDAcroForm acroForm = catalog.getAcroForm();
            PDResources acroFormResources = acroForm.getDefaultResources();
            if (acroFormResources != null)
            {
                for (COSName fontName : acroFormResources.getFontNames())
                {
                    PDFont font = acroFormResources.getFont(fontName);
                    font.getFontDescriptor().getFontName();
                    fontNames.put(fontName.getName(), font.getName());
                }
            }
        }

        try (PDDocument testPdf = PDFParser.parse(
                SeekableSources.seekableSourceFrom(new File("target/pdfs/merge-test.pdf"))))
        {
            PDDocumentCatalog catalog = testPdf.getDocumentCatalog();
            PDAcroForm acroForm = catalog.getAcroForm();
            PDResources acroFormResources = acroForm.getDefaultResources();
            if (acroFormResources != null)
            {
                for (COSName fontName : acroFormResources.getFontNames())
                {
                    PDFont font = acroFormResources.getFont(fontName);
                    String pdfBoxFontName = font.getFontDescriptor().getFontName();
                    assertEquals(
                            "font resource added by Acrobat shall match font resource added by PDFBox",
                            fontNames.get(fontName.getName()), pdfBoxFontName);
                }
            }
        }

    }

    /**
     * PDFBOX-3891 null PDFieldFactory.createField
     *
     * @throws IOException
     */
    @Test
    public void testFromAnnots3891NullField() throws IOException
    {
        try (PDDocument testPdf = PDFParser.parse(
                SeekableSources.seekableSourceFrom(new File("target/pdfs/poppler-14433-0.pdf"))))
        {
            testPdf.getDocumentCatalog().getAcroForm();
        }
    }

}
