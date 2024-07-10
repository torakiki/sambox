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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.sejda.sambox.cos.COSDictionary.of;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.sejda.io.SeekableSources;
import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSInteger;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.input.PDFParser;
import org.sejda.sambox.pdmodel.PDDocument;
import org.sejda.sambox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.sejda.sambox.pdmodel.interactive.annotation.PDAppearanceDictionary;
import org.sejda.sambox.pdmodel.interactive.annotation.PDAppearanceEntry;
import org.sejda.sambox.pdmodel.interactive.annotation.PDAppearanceStream;

/**
 * This will test the functionality of Radio Buttons in PDFBox.
 */
public class TestRadioButtons
{

    /**
     * This will test the radio button PDModel.
     *
     * @throws IOException If there is an error creating the field.
     */
    @Test
    public void testRadioButtonPDModel() throws IOException
    {
        try (PDDocument doc = new PDDocument())
        {
            PDAcroForm form = new PDAcroForm(doc);
            PDRadioButton radioButton = new PDRadioButton(form);

            // test that there are no nulls returned for an empty field
            // only specific methods are tested here
            assertNotNull(radioButton.getDefaultValue());
            assertNotNull(radioButton.getSelectedExportValues());
            assertNotNull(radioButton.getExportValues());
            assertNotNull(radioButton.getValue());

            // Test setting/getting option values - the dictionaries Opt entry
            List<String> options = new ArrayList<>();
            options.add("Value01");
            options.add("Value02");
            radioButton.setExportValues(options);

            // Test getSelectedExportValues()
            List<PDAnnotationWidget> widgets = new ArrayList<>();
            for (String option : options)
            {
                PDAnnotationWidget widget = new PDAnnotationWidget();
                COSDictionary streamDictionary = of(COSName.LENGTH, COSInteger.get(63));
                COSDictionary apNDict = new COSDictionary();
                apNDict.setItem(COSName.Off, new PDAppearanceStream());
                apNDict.setItem(option, new PDAppearanceStream());

                PDAppearanceDictionary appearance = new PDAppearanceDictionary();
                PDAppearanceEntry appearanceNEntry = new PDAppearanceEntry(apNDict);
                appearance.setNormalAppearance(appearanceNEntry);
                widget.setAppearance(appearance);
                widget.setAppearanceState("Off");
                widgets.add(widget);
            }
            radioButton.setWidgets(widgets);

            radioButton.setValue("Value01");
            assertEquals("Value01", radioButton.getValue());
            assertEquals(1, radioButton.getSelectedExportValues().size());
            assertEquals("Value01", radioButton.getSelectedExportValues().get(0));
            assertEquals("Value01", widgets.get(0).getAppearanceState().getName());
            assertEquals("Off", widgets.get(1).getAppearanceState().getName());

            radioButton.setValue("Value02");
            assertEquals("Value02", radioButton.getValue());
            assertEquals(1, radioButton.getSelectedExportValues().size());
            assertEquals("Value02", radioButton.getSelectedExportValues().get(0));
            assertEquals("Off", widgets.get(0).getAppearanceState().getName());
            assertEquals("Value02", widgets.get(1).getAppearanceState().getName());

            radioButton.setValue("Off");
            assertEquals("Off", radioButton.getValue());
            assertEquals(0, radioButton.getSelectedExportValues().size());
            assertEquals("Off", widgets.get(0).getAppearanceState().getName());
            assertEquals("Off", widgets.get(1).getAppearanceState().getName());

            COSArray optItem = (COSArray) radioButton.getCOSObject().getItem(COSName.OPT);

            // assert that the values have been correctly set
            assertNotNull(radioButton.getCOSObject().getItem(COSName.OPT));
            assertEquals(2, optItem.size());
            assertEquals(options.get(0), optItem.getString(0));

            // assert that the values can be retrieved correctly
            List<String> retrievedOptions = radioButton.getExportValues();
            assertEquals(2, retrievedOptions.size());
            assertEquals(retrievedOptions, options);

            // assert that the Opt entry is removed
            radioButton.setExportValues(null);
            assertNull(radioButton.getCOSObject().getItem(COSName.OPT));
            // if there is no Opt entry an empty List shall be returned
            assertEquals(radioButton.getExportValues(), new ArrayList<String>());
        }
    }

    /**
     * PDFBOX-4617 Enable getting selected index
     *
     * @throws IOException
     */
    @Test
    public void testPDFBox4617IndexNoneSelected() throws IOException
    {
        try (PDDocument testPdf = PDFParser.parse(
                SeekableSources.seekableSourceFrom(new File("target/pdfs/SF1199AEG.pdf"))))
        {
            PDAcroForm acroForm = testPdf.getDocumentCatalog().getAcroForm();
            PDRadioButton field = (PDRadioButton) acroForm.getField("Checking/Savings");
            assertEquals(-1, field.getSelectedIndex(),
                    "if there is no value set the index shall be -1");
        }
    }

    /**
     * PDFBOX-4617 Enable getting selected index for value being set by option
     *
     * @throws IOException
     */
    @Test
    public void testPDFBox4617IndexForSetByOption() throws IOException
    {
        try (PDDocument testPdf = PDFParser.parse(
                SeekableSources.seekableSourceFrom(new File("target/pdfs/SF1199AEG.pdf"))))
        {
            PDAcroForm acroForm = testPdf.getDocumentCatalog().getAcroForm();
            PDRadioButton field = (PDRadioButton) acroForm.getField("Checking/Savings");
            field.setValue("Checking");
            assertEquals(0, field.getSelectedIndex(),
                    "the index shall be equal with the first entry of Checking which is 0");
        }
    }

    /**
     * PDFBOX-3656 Radio button field with FLAG_RADIOS_IN_UNISON false
     *
     * @throws IOException
     */
    @Test
    public void testPDFBox3656NotInUnison() throws IOException
    {

        try (PDDocument testPdf = PDFParser.parse(
                SeekableSources.seekableSourceFrom(new File("target/pdfs/SF1199AEG.pdf"))))
        {

            PDAcroForm acroForm = testPdf.getDocumentCatalog().getAcroForm();
            PDRadioButton field = (PDRadioButton) acroForm.getField("Checking/Savings");
            assertFalse(field.isRadiosInUnison(),
                    "The radio buttons can be selected individually although having the same ON value");
        }
    }

    /**
     * PDFBOX-3656 Set by value
     * <p>
     * There are 6 radio buttons where 3 share the same common values but they are not set in unison
     * Setting by the first export value shall only select the first radio button
     *
     * @throws IOException
     */
    @Test
    public void testPDFBox3656ByValidExportValue() throws IOException
    {
        try (PDDocument testPdf = PDFParser.parse(
                SeekableSources.seekableSourceFrom(new File("target/pdfs/SF1199AEG.pdf"))))
        {
            PDAcroForm acroForm = testPdf.getDocumentCatalog().getAcroForm();
            PDRadioButton field = (PDRadioButton) acroForm.getField("Checking/Savings");
            // check defaults
            assertFalse(field.isRadiosInUnison(),
                    "The radio buttons can be selected individually although having the same ON value");
            assertEquals("Off", field.getValue(), "Initially no option shall be selected");
            // set the field to a valid export value
            field.setValue("Checking");
            assertEquals("Checking", field.getValueAsString(),
                    "setting by the export value should also return that");
        }
    }

    /**
     * PDFBOX-3656 Set by invalid export value
     *
     * @throws IOException
     */
    @Test
    public void testPDFBox3656ByInvalidExportValue() throws IOException
    {
        try (PDDocument testPdf = PDFParser.parse(
                SeekableSources.seekableSourceFrom(new File("target/pdfs/SF1199AEG.pdf"))))
        {
            PDAcroForm acroForm = testPdf.getDocumentCatalog().getAcroForm();
            PDRadioButton field = (PDRadioButton) acroForm.getField("Checking/Savings");
            // check defaults
            assertFalse(field.isRadiosInUnison(),
                    "The radio buttons can be selected individually although having the same ON value");
            assertEquals("Off", field.getValue(), "Initially no option shall be selected");

            //SAMBox specific. This is different from PDFBox because we allow to set by options (OPT values) or Normal Apperance keys so our exception msg is slightly different
            //see ISO 32000-2:2017 Table 230, I think our approach makes more sense
            IllegalArgumentException thrown = Assertions.assertThrows(
                    IllegalArgumentException.class, () -> field.setValue("Invalid"));

            assertThat(thrown.getMessage(), containsString(
                    "value 'Invalid' is not a valid option for the field Checking/Savings"));

        }
    }

    /**
     * PDFBOX-3656 Set by a valid index
     * <p>
     * There are 6 radio buttons where 3 share the same common values but they are not set in unison
     * Setting by the index shall only select the corresponding radio button
     *
     * @throws IOException
     */
    @Test
    public void testPDFBox3656ByValidIndex() throws IOException
    {
        try (PDDocument testPdf = PDFParser.parse(
                SeekableSources.seekableSourceFrom(new File("target/pdfs/SF1199AEG.pdf"))))
        {
            PDAcroForm acroForm = testPdf.getDocumentCatalog().getAcroForm();
            PDRadioButton field = (PDRadioButton) acroForm.getField("Checking/Savings");
            // check defaults
            assertFalse(field.isRadiosInUnison(),
                    "The radio buttons can be selected individually although having the same ON value");
            assertEquals("Off", field.getValue(), "Initially no option shall be selected");
            // set the field to a valid index
            field.setValue(4);
            assertEquals("Checking", field.getValueAsString(),
                    "Setting by the index value should return the corresponding export");
        }
    }

    /**
     * PDFBOX-3656 Set by an invalid index
     * <p>
     * There are 6 radio buttons where 3 share the same common values but they are not set in unison
     * Setting by the index shall only select the corresponding radio button
     *
     * @throws IOException
     */
    @Test
    public void testPDFBox3656ByInvalidIndex() throws IOException
    {
        try (PDDocument testPdf = PDFParser.parse(
                SeekableSources.seekableSourceFrom(new File("target/pdfs/SF1199AEG.pdf"))))
        {
            PDAcroForm acroForm = testPdf.getDocumentCatalog().getAcroForm();
            PDRadioButton field = (PDRadioButton) acroForm.getField("Checking/Savings");
            // check defaults
            assertFalse(field.isRadiosInUnison(),
                    "The radio buttons can be selected individually although having the same ON value");
            assertEquals("Off", field.getValue(), "Tnitially no option shall be selected");

            try
            {
                field.setValue(6);
                fail("Expected an IndexOutOfBoundsException to be thrown");
            }
            catch (Exception ex)
            {
                // compare the messages
                String expectedMessage = "index '6' is not a valid index for the field Checking/Savings, valid indices are from 0 to 5";
                String actualMessage = ex.getMessage();
                assertTrue(actualMessage.contains(expectedMessage));
            }

            assertEquals("Off", field.getValue(), "No option shall be selected");
            assertTrue(field.getSelectedExportValues().isEmpty(), "no export values are selected");
        }
    }
}
