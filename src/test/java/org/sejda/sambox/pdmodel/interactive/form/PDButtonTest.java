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
import org.junit.jupiter.api.Assertions;
import org.sejda.io.SeekableSources;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.input.PDFParser;
import org.sejda.sambox.pdmodel.PDDocument;
import org.sejda.sambox.pdmodel.PDPage;
import org.sejda.sambox.pdmodel.interactive.annotation.*;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.*;

/**
 * Test for the PDButton class.
 */
public class PDButtonTest
{
    private PDDocument document;
    private PDAcroForm acroForm;

    private PDDocument acrobatDocument;
    private PDAcroForm acrobatAcroForm;

    @Before
    public void setUp() throws IOException
    {
        document = new PDDocument();
        document.addPage(new PDPage());
        acroForm = new PDAcroForm(document);

        acrobatDocument = PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(
                getClass().getResourceAsStream(
                        "/org/sejda/sambox/pdmodel/interactive/form/AcroFormsBasicFields.pdf")));
        acrobatAcroForm = acrobatDocument.getDocumentCatalog().getAcroForm();
    }

    @Test
    public void createCheckBox()
    {
        PDButton buttonField = new PDCheckBox(acroForm);

        assertEquals(buttonField.getFieldType(),
                buttonField.getCOSObject().getNameAsString(COSName.FT));
        assertEquals(buttonField.getFieldType(), "Btn");
        assertFalse(buttonField.isPushButton());
        assertFalse(buttonField.isRadioButton());
    }

    @Test
    public void createCheckBoxWithoutOnValues() throws IOException
    {
        PDCheckBox checkbox = new PDCheckBox(acroForm);
        PDAnnotationWidget widget = new PDAnnotationWidget();
        PDAppearanceDictionary appearance = new PDAppearanceDictionary();
        appearance.setNormalAppearance(new PDAppearanceEntry(new COSDictionary()));
        widget.setAppearance(appearance);

        checkbox.setWidgets(Arrays.asList(widget));

        checkbox.check();
        checkbox.unCheck();
    }

    @Test
    public void createPushButton()
    {
        PDButton buttonField = new PDPushButton(acroForm);

        assertEquals(buttonField.getFieldType(),
                buttonField.getCOSObject().getNameAsString(COSName.FT));
        assertEquals(buttonField.getFieldType(), "Btn");
        assertTrue(buttonField.isPushButton());
        assertFalse(buttonField.isRadioButton());
    }

    @Test
    public void createRadioButton()
    {
        PDButton buttonField = new PDRadioButton(acroForm);

        assertEquals(buttonField.getFieldType(),
                buttonField.getCOSObject().getNameAsString(COSName.FT));
        assertEquals(buttonField.getFieldType(), "Btn");
        assertTrue(buttonField.isRadioButton());
        assertFalse(buttonField.isPushButton());
    }

    /**
     * PDFBOX-3656
     * <p>
     * Test a radio button with options. This was causing an ArrayIndexOutOfBoundsException when
     * trying to set to "Off", as this wasn't treated to be a valid option.
     *
     * @throws IOException
     */
    @Test
    public void testRadioButtonWithOptions() throws IOException
    {
        try (PDDocument document = PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(
                this.getClass().getResourceAsStream(
                        "/org/sejda/sambox/pdmodel/interactive/form/radio_with_options.pdf"))))
        {

            PDRadioButton radioButton = (PDRadioButton) document.getDocumentCatalog().getAcroForm()
                    .getField("Checking/Savings");
            radioButton.setValue("Off");
            for (PDAnnotationWidget widget : radioButton.getWidgets())
            {
                assertEquals("The widget should be set to Off", COSName.Off,
                        widget.getCOSObject().getItem(COSName.AS));
            }

        }

    }

    @Test
    public void testRadioButtonWithOptionsThatDontMatchNormalAppearance() throws IOException
    {
        try (PDDocument document = PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(
                this.getClass().getResourceAsStream(
                        "/org/sejda/sambox/pdmodel/interactive/form/simple_form.pdf"))))
        {

            PDRadioButton radioButton = (PDRadioButton) document.getDocumentCatalog().getAcroForm()
                    .getField("Choice_Caption_0wUBrGuJDKIWD9g7kWcKpg");

            radioButton.setIgnoreExportOptions(true);

            radioButton.setValue("1");
            assertFieldV_widgetAS(radioButton, "1", "Off", "1");
        }
    }

    @Test
    public void checkboxMultipleOnNormalAppearances() throws IOException {
        try (PDDocument document = parseDoc("/org/sejda/sambox/pdmodel/interactive/form/checkbox-multiple-on-entries-normal-appearance.pdf")) {

            PDCheckBox field = (PDCheckBox) document.getDocumentCatalog().getAcroForm()
                    .getField("CheckBox_Captio_s897lNLPSLVG32zNkn2DGg");
            
            assertEquals(field.getWidgets().size(), 1);
            PDAnnotationWidget widget = field.getWidgets().get(0);
            
            assertEquals(widget.getAppearance().getNormalAppearance().getSubDictionary().keySet(), 
                    new HashSet<>(Arrays.asList(COSName.getPDFName("On"), COSName.Off, COSName.YES)));
            
            assertEquals(field.getNormalAppearanceValues(), Arrays.asList("On"));
        }
    }

    @Test
    public void testRadioButtonWithOptionsThatDoMatchNormalAppearance() throws IOException
    {
        try (PDDocument document = PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(
                this.getClass().getResourceAsStream(
                        "/org/sejda/sambox/pdmodel/interactive/form/PDFBOX-3656 - test.pdf"))))
        {

            PDRadioButton radioButton = (PDRadioButton) document.getDocumentCatalog().getAcroForm()
                    .getField("RadioButton");
            radioButton.setValue("c");

            assertEquals("Export value does exist in normal appearance. Do export value",
                    radioButton.getValue(), "c");

            assertEquals("First widget should be Off", COSName.Off,
                    radioButton.getWidgets().get(0).getCOSObject().getItem(COSName.AS));

            assertEquals("Second widget should be Off", COSName.Off,
                    radioButton.getWidgets().get(1).getCOSObject().getItem(COSName.AS));

            assertEquals("Third widget should be set to c", COSName.getPDFName("c"),
                    radioButton.getWidgets().get(2).getCOSObject().getItem(COSName.AS));

        }
    }

    @Test
    public void testCheckboxWithExportValuesMoreThanWidgetsButSameExportValue() throws IOException
    {
        try (PDDocument document = PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(
                this.getClass().getResourceAsStream(
                        "/org/sejda/sambox/pdmodel/interactive/form/P020130830121570742708.pdf"))))
        {

            PDCheckBox checkbox = (PDCheckBox) document.getDocumentCatalog().getAcroForm()
                    .getField("Check Box3");
            
            checkbox.setIgnoreExportOptions(true);

            checkbox.check();
            assertFieldV_widgetAS(checkbox, "0", "0");
        }
    }

    @Test
    public void testMalformedCheckboxNormalAppearances() throws IOException
    {
        try (PDDocument document = PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(
                this.getClass().getResourceAsStream(
                        "/sambox/forms-malformed-checkbox-normal-appearances.pdf"))))
        {

            PDCheckBox checkbox = (PDCheckBox) document.getDocumentCatalog().getAcroForm()
                    .getField("English IELTS");

            assertEquals(checkbox.getOnValues().size(), 0);
            assertFalse(checkbox.getWidgets().get(0).getAppearance().getNormalAppearance().isSubDictionary());
            assertEquals(checkbox.getNormalAppearanceValues().size(), 0);
            assertFalse(checkbox.isChecked());
            assertFieldV_widgetAS(checkbox, "Off", "Off");
            
            checkbox.setIgnoreExportOptions(true);

            RuntimeException thrown = Assertions.assertThrows(
                    RuntimeException.class, () -> checkbox.check());

            assertThat(thrown.getMessage(), containsString(
                    "Check/radio has a single normal appearance, might look the same when checked or not"));

//            checkbox.check();
//            assertFieldV_widgetAS(checkbox, "Yes", "Yes");
//            assertTrue(checkbox.isChecked());
//
//            checkbox.unCheck();
//            assertFieldV_widgetAS(checkbox, "Off", "Off");
//            assertFalse(checkbox.isChecked());
//
//            checkbox.setValue("Yes");
//            assertFieldV_widgetAS(checkbox, "Yes", "Yes");
//            assertTrue(checkbox.isChecked());
        }
    }

    /**
     * PDFBOX-3682
     * <p>
     * Test a radio button with options. Special handling for a radio button with /Opt and the On
     * state not being named after the index.
     *
     * @throws IOException
     */
    @Test
    public void testOptionsAndNamesNotNumbers() throws IOException
    {

        try (PDDocument document = PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(
                this.getClass().getResourceAsStream(
                        "/org/sejda/sambox/pdmodel/interactive/form/options_names_not_numbers.pdf"))))
        {

            document.getDocumentCatalog().getAcroForm().getField("RadioButton").setValue("c");
            PDRadioButton radioButton = (PDRadioButton) document.getDocumentCatalog().getAcroForm()
                    .getField("RadioButton");
            radioButton.setValue("c");

            // test that the old behavior is now invalid
            assertFalse("This shall no longer be 2", "2".equals(radioButton.getValueAsString()));
            assertFalse("This shall no longer be 2", "2".equals(
                    radioButton.getWidgets().get(2).getCOSObject().getNameAsString(COSName.AS)));

            // test for the correct behavior
            assertTrue("This shall be c", "c".equals(radioButton.getValueAsString()));
            assertTrue("This shall be c", "c".equals(
                    radioButton.getWidgets().get(2).getCOSObject().getNameAsString(COSName.AS)));

        }

    }

    @Test
    public void retrieveAcrobatCheckBoxProperties()
    {
        PDCheckBox checkbox = (PDCheckBox) acrobatAcroForm.getField("Checkbox");
        assertNotNull(checkbox);
        assertEquals(checkbox.getOnValue(), "Yes");
        assertEquals(checkbox.getOnValues().size(), 1);
        assertTrue(checkbox.getOnValues().contains("Yes"));
    }

    @Test
    public void testAcrobatCheckBoxProperties() throws IOException
    {
        PDCheckBox checkbox = (PDCheckBox) acrobatAcroForm.getField("Checkbox");
        assertEquals(checkbox.getValue(), "Off");
        assertEquals(checkbox.isChecked(), false);

        checkbox.check();
        assertEquals(checkbox.getValue(), checkbox.getOnValue());
        assertEquals(checkbox.isChecked(), true);

        checkbox.setValue("Yes");
        assertEquals(checkbox.getValue(), checkbox.getOnValue());
        assertEquals(checkbox.isChecked(), true);
        assertEquals(checkbox.getCOSObject().getDictionaryObject(COSName.AS), COSName.YES);

        checkbox.setValue("Off");
        assertEquals(checkbox.getValue(), COSName.Off.getName());
        assertEquals(checkbox.isChecked(), false);
        assertEquals(checkbox.getCOSObject().getDictionaryObject(COSName.AS), COSName.Off);

        checkbox = (PDCheckBox) acrobatAcroForm.getField("Checkbox-DefaultValue");
        assertEquals(checkbox.getDefaultValue(), checkbox.getOnValue());

        checkbox.setDefaultValue("Off");
        assertEquals(checkbox.getDefaultValue(), COSName.Off.getName());
    }

    @Test
    public void testUncheck() throws IOException
    {
        PDCheckBox checkbox = (PDCheckBox) acrobatAcroForm.getField("Checkbox");

        checkbox.unCheck();
        assertEquals(checkbox.getValue(), COSName.Off.getName());
        assertEquals(checkbox.isChecked(), false);
    }

    @Test
    public void testCheckboxWithExportValuesThatDoNotMatchAppearances() throws IOException
    {
        PDCheckBox checkbox = (PDCheckBox) acrobatAcroForm.getField("Checkbox");
        checkbox.setExportValues(Collections.singletonList("exportValue1"));
        assertEquals(checkbox.getExportValues(), Arrays.asList("exportValue1"));

        checkbox.setIgnoreExportOptions(true);

        checkbox.unCheck();
        assertEquals(checkbox.getValue(), COSName.Off.getName());
        assertEquals(checkbox.isChecked(), false);

        checkbox.check();
        assertEquals(checkbox.getValue(), "Yes");
        assertEquals(checkbox.isChecked(), true);

        checkbox.setIgnoreExportOptions(false);
        assertEquals(checkbox.getOnValues(), new HashSet<>(Arrays.asList("Yes", "exportValue1")));
        
        checkbox.setIgnoreExportOptions(true);
        assertEquals(checkbox.getOnValues(), new HashSet<>(Arrays.asList("Yes")));
        assertEquals(checkbox.getOnValues(), new HashSet<>(Arrays.asList("Yes")));
    }

    @Test
    public void setValueForAbstractedAcrobatCheckBox() throws IOException
    {
        PDField checkbox = acrobatAcroForm.getField("Checkbox");

        checkbox.setValue("Yes");
        assertEquals(checkbox.getValueAsString(), ((PDCheckBox) checkbox).getOnValue());
        assertEquals(((PDCheckBox) checkbox).isChecked(), true);
        assertEquals(checkbox.getCOSObject().getDictionaryObject(COSName.AS), COSName.YES);

        checkbox.setValue("Off");
        assertEquals(checkbox.getValueAsString(), COSName.Off.getName());
        assertEquals(((PDCheckBox) checkbox).isChecked(), false);
        assertEquals(checkbox.getCOSObject().getDictionaryObject(COSName.AS), COSName.Off);
    }

    @Test
    public void testAcrobatCheckBoxGroupProperties() throws IOException
    {
        PDCheckBox checkbox = (PDCheckBox) acrobatAcroForm.getField("CheckboxGroup");
        assertEquals(checkbox.getValue(), "Off");
        assertEquals(checkbox.isChecked(), false);

        checkbox.check();
        assertEquals(checkbox.getValue(), checkbox.getOnValue());
        assertEquals(checkbox.isChecked(), true);

        assertEquals(checkbox.getOnValues().size(), 3);
        assertTrue(checkbox.getOnValues().contains("Option1"));
        assertTrue(checkbox.getOnValues().contains("Option2"));
        assertTrue(checkbox.getOnValues().contains("Option3"));

        // test a value which sets one of the individual checkboxes within the group
        checkbox.setValue("Option1");
        assertEquals("Option1", checkbox.getValue());
        assertEquals("Option1", checkbox.getValueAsString());

        // ensure that for the widgets representing the individual checkboxes
        // the AS entry has been set
        assertEquals("Option1", checkbox.getWidgets().get(0).getAppearanceState().getName());
        assertEquals("Off", checkbox.getWidgets().get(1).getAppearanceState().getName());
        assertEquals("Off", checkbox.getWidgets().get(2).getAppearanceState().getName());
        assertEquals("Off", checkbox.getWidgets().get(3).getAppearanceState().getName());

        // test a value which sets two of the individual chekboxes within the group
        // as the have the same name entry for being checked
        checkbox.setValue("Option3");
        assertEquals("Option3", checkbox.getValue());
        assertEquals("Option3", checkbox.getValueAsString());

        // ensure that for both widgets representing the individual checkboxes
        // the AS entry has been set
        assertEquals("Off", checkbox.getWidgets().get(0).getAppearanceState().getName());
        assertEquals("Off", checkbox.getWidgets().get(1).getAppearanceState().getName());
        assertEquals("Option3", checkbox.getWidgets().get(2).getAppearanceState().getName());
        assertEquals("Option3", checkbox.getWidgets().get(3).getAppearanceState().getName());
    }

    @Test
    public void setValueForAbstractedCheckBoxGroup() throws IOException
    {
        PDField checkbox = acrobatAcroForm.getField("CheckboxGroup");

        // test a value which sets one of the individual checkboxes within the group
        checkbox.setValue("Option1");
        assertEquals("Option1", checkbox.getValueAsString());

        // ensure that for the widgets representing the individual checkboxes
        // the AS entry has been set
        assertEquals("Option1", checkbox.getWidgets().get(0).getAppearanceState().getName());
        assertEquals("Off", checkbox.getWidgets().get(1).getAppearanceState().getName());
        assertEquals("Off", checkbox.getWidgets().get(2).getAppearanceState().getName());
        assertEquals("Off", checkbox.getWidgets().get(3).getAppearanceState().getName());

        // test a value which sets two of the individual chekboxes within the group
        // as the have the same name entry for being checked
        checkbox.setValue("Option3");
        assertEquals("Option3", checkbox.getValueAsString());

        // ensure that for both widgets representing the individual checkboxes
        // the AS entry has been set
        assertEquals("Off", checkbox.getWidgets().get(0).getAppearanceState().getName());
        assertEquals("Off", checkbox.getWidgets().get(1).getAppearanceState().getName());
        assertEquals("Option3", checkbox.getWidgets().get(2).getAppearanceState().getName());
        assertEquals("Option3", checkbox.getWidgets().get(3).getAppearanceState().getName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void setCheckboxInvalidValue() throws IOException
    {
        PDCheckBox checkbox = (PDCheckBox) acrobatAcroForm.getField("Checkbox");
        // Set a value which doesn't match the radio button list
        checkbox.setValue("InvalidValue");
    }

    @Test(expected = IllegalArgumentException.class)
    public void setCheckboxGroupInvalidValue() throws IOException
    {
        PDCheckBox checkbox = (PDCheckBox) acrobatAcroForm.getField("CheckboxGroup");
        // Set a value which doesn't match the radio button list
        checkbox.setValue("InvalidValue");
    }

    @Test(expected = IllegalArgumentException.class)
    public void setAbstractedCheckboxInvalidValue() throws IOException
    {
        PDField checkbox = acrobatAcroForm.getField("Checkbox");
        // Set a value which doesn't match the radio button list
        checkbox.setValue("InvalidValue");
    }

    @Test(expected = IllegalArgumentException.class)
    public void setAbstractedCheckboxGroupInvalidValue() throws IOException
    {
        PDField checkbox = acrobatAcroForm.getField("CheckboxGroup");
        // Set a value which doesn't match the radio button list
        checkbox.setValue("InvalidValue");
    }

    @Test
    public void retrieveAcrobatRadioButtonProperties() throws IOException
    {
        PDRadioButton radioButton = (PDRadioButton) acrobatAcroForm.getField("RadioButtonGroup");
        assertNotNull(radioButton);
        assertEquals(radioButton.getOnValues().size(), 2);
        assertTrue(radioButton.getOnValues().contains("RadioButton01"));
        assertTrue(radioButton.getOnValues().contains("RadioButton02"));
    }

    @Test
    public void testAcrobatRadioButtonProperties() throws IOException
    {
        PDRadioButton radioButton = (PDRadioButton) acrobatAcroForm.getField("RadioButtonGroup");

        // Set value so that first radio button option is selected
        radioButton.setValue("RadioButton01");
        assertEquals(radioButton.getValue(), "RadioButton01");
        // First option shall have /RadioButton01, second shall have /Off
        assertEquals(radioButton.getWidgets().get(0).getCOSObject().getDictionaryObject(COSName.AS),
                COSName.getPDFName("RadioButton01"));
        assertEquals(radioButton.getWidgets().get(1).getCOSObject().getDictionaryObject(COSName.AS),
                COSName.Off);

        // Set value so that second radio button option is selected
        radioButton.setValue("RadioButton02");
        assertEquals(radioButton.getValue(), "RadioButton02");
        // First option shall have /Off, second shall have /RadioButton02
        assertEquals(radioButton.getWidgets().get(0).getCOSObject().getDictionaryObject(COSName.AS),
                COSName.Off);
        assertEquals(radioButton.getWidgets().get(1).getCOSObject().getDictionaryObject(COSName.AS),
                COSName.getPDFName("RadioButton02"));
    }

    @Test
    public void setValueForAbstractedAcrobatRadioButton() throws IOException
    {
        PDField radioButton = acrobatAcroForm.getField("RadioButtonGroup");

        // Set value so that first radio button option is selected
        radioButton.setValue("RadioButton01");
        assertEquals(radioButton.getValueAsString(), "RadioButton01");
        // First option shall have /RadioButton01, second shall have /Off
        assertEquals(radioButton.getWidgets().get(0).getCOSObject().getDictionaryObject(COSName.AS),
                COSName.getPDFName("RadioButton01"));
        assertEquals(radioButton.getWidgets().get(1).getCOSObject().getDictionaryObject(COSName.AS),
                COSName.Off);

        // Set value so that second radio button option is selected
        radioButton.setValue("RadioButton02");
        assertEquals(radioButton.getValueAsString(), "RadioButton02");
        // First option shall have /Off, second shall have /RadioButton02
        assertEquals(radioButton.getWidgets().get(0).getCOSObject().getDictionaryObject(COSName.AS),
                COSName.Off);
        assertEquals(radioButton.getWidgets().get(1).getCOSObject().getDictionaryObject(COSName.AS),
                COSName.getPDFName("RadioButton02"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void setRadioButtonInvalidValue() throws IOException
    {
        PDRadioButton radioButton = (PDRadioButton) acrobatAcroForm.getField("RadioButtonGroup");
        // Set a value which doesn't match the radio button list
        radioButton.setValue("InvalidValue");
    }

    @Test(expected = IllegalArgumentException.class)
    public void setAbstractedRadioButtonInvalidValue() throws IOException
    {
        PDField radioButton = acrobatAcroForm.getField("RadioButtonGroup");
        // Set a value which doesn't match the radio button list
        radioButton.setValue("InvalidValue");
    }

    @Test
    public void checkBoxWithoutAppearances() throws IOException
    {
        try(PDDocument doc = parseDoc("/org/sejda/sambox/pdmodel/interactive/form/checkbox-without-appearances.pdf"))
        {
            PDCheckBox checkBox = (PDCheckBox)doc.getDocumentCatalog().getAcroForm().getField("checkbox_1vpsz");

            assertNull(checkBox.getWidgets().get(0).getAppearance());
            assertEquals(checkBox.getWidgets().size(), 1);
            assertEquals(checkBox.getOnValue(), "");

            checkBox.setIgnoreExportOptions(true);

            checkBox.check();
            
            assertTrue(checkBox.isChecked());
            assertFieldV_widgetAS(checkBox, "", "");
            
            checkBox.unCheck();

            assertFalse(checkBox.isChecked());
            assertFieldV_widgetAS(checkBox, "Off", "Off");
        }
    }
    
    @Test
    public void radioButtonWithOneWidgetAndMoreExportValues() throws IOException
    {
        try(PDDocument doc = parseDoc("/org/sejda/sambox/pdmodel/interactive/form/radio-field-one-widget-more-export-values.pdf")){
            PDRadioButton radio = (PDRadioButton)doc.getDocumentCatalog().getAcroForm().getField("radio_group_test");

            assertEquals(radio.getWidgets().size(), 1);
            assertEquals(radio.getExportValues(), Arrays.asList("0", "Yes", "Yes"));
            assertEquals(radio.getValue(), "Off");

            radio.setIgnoreExportOptions(true);

            radio.setValue("Yes");
            
            assertEquals(radio.getValue(), "Yes");
            assertFieldV_widgetAS(radio, "Yes", "Yes");
        }
        
    }

    @Test
    public void radioButtonWithOneWidgetAndMoreExportValuesIntegers() throws IOException
    {
        try(PDDocument doc = parseDoc("/org/sejda/sambox/pdmodel/interactive/form/radio-field-more-export-values-integers.pdf")) {
         
            PDRadioButton radio = (PDRadioButton) doc.getDocumentCatalog().getAcroForm().getField("radio_group_test");

            radio.setIgnoreExportOptions(true);
            
            assertEquals(radio.getWidgets().size(), 1);
            assertEquals(radio.getExportValues(), Arrays.asList("0", "1"));
            assertEquals(radio.getValue(), "Off");

            radio.setValue("1");
            
            assertEquals(radio.getValue(), "1");
            assertFieldV_widgetAS(radio, "1", "1");
        }
    }

    @Test
    public void testExportOptionsAreIntegers() throws IOException {
        try (PDDocument document = parseDoc("/org/sejda/sambox/pdmodel/interactive/form/radio-field-export-values-integers.pdf")) 
        {
            PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();
            PDRadioButton field = (PDRadioButton) acroForm.getField("radio_group_test");
            
            field.setIgnoreExportOptions(true);
            
            field.setValue("1");
            assertFieldV_widgetAS(field, "1", "1", "Off");

            field.setValue("2");
            assertFieldV_widgetAS(field, "2", "Off", "2");
        }
    }
    
    @Test
    public void multipleCheckboxesSameName() throws IOException {
        try (PDDocument document = parseDoc("/org/sejda/sambox/pdmodel/interactive/form/multiple-checkboxes-same-name.pdf"))
        {
            PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();
            PDCheckBox field = (PDCheckBox) acroForm.getField("checkbox_test");

            field.setIgnoreExportOptions(true);

            field.setValue("Yes");
            assertFieldV_widgetAS(field, "Yes", "Yes", "Yes", "Off", "Off");

            field.setValue("No");
            assertFieldV_widgetAS(field, "No", "Off", "Off", "No", "No");
        }
    }

    @Test
    public void checkboxOnlyWithOffNormalAppearance() throws IOException {
        try (PDDocument document = parseDoc("/org/sejda/sambox/pdmodel/interactive/form/checkbox_normal_appearances_only_off.pdf"))
        {
            PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();
            PDCheckBox field = (PDCheckBox) acroForm.getField("checkbox_test");

            field.setIgnoreExportOptions(true);
            
            field.setValue("");
            assertFieldV_widgetAS(field, "", "");
        }
    }

    @Test
    public void multipleRadiosSameName() throws IOException {
        try (PDDocument document = parseDoc("/org/sejda/sambox/pdmodel/interactive/form/multiple-radios-same-name.pdf"))
        {
            PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();
            PDRadioButton field = (PDRadioButton) acroForm.getField("radio_group_test");

            field.setIgnoreExportOptions(true);

            field.setValue("Value1");
            assertFieldV_widgetAS(field, "Value1", "Value1", "Off", "Off", "Value1", "Off");

            field.setValue("Value3");
            assertFieldV_widgetAS(field, "Value3", "Off", "Off", "Value3", "Off", "Off");
        }
    }
    
    

    @Test
    public void radioExtraDictItemsInNormalAppearance() throws IOException {
        try(PDDocument doc = parseDoc("/org/sejda/sambox/pdmodel/interactive/form/radio-field-extra-dict-items-normal-appearance.pdf")) {

            PDRadioButton radio = (PDRadioButton) doc.getDocumentCatalog().getAcroForm().getField("radio_group_test");

            radio.setIgnoreExportOptions(true);

            assertEquals(new HashSet<>(Arrays.asList("1")), radio.getOnValues());

            radio.setValue("1");

            assertEquals(radio.getValue(), "1");
            assertFieldV_widgetAS(radio, "1", "1");
        }
    }

    @After
    public void tearDown() throws IOException
    {
        document.close();
        acrobatDocument.close();
    }
    
    private PDDocument parseDoc(String res) throws IOException {
        if (new File(res).exists()) 
        {
            return PDFParser.parse(SeekableSources.seekableSourceFrom(
                    new File(res)));
        } else 
        {
            return PDFParser.parse(SeekableSources.onTempFileSeekableSourceFrom(
                    getClass().getResourceAsStream(res)));
        }
    }
    
    private void assertFieldV_widgetAS(PDField field, String expectedV, String... expectedASs)
    {
        assertEquals("Field V", COSName.getPDFName(expectedV), field.getInheritableAttribute(COSName.V));
        List<String> actualASList = field.getWidgets().stream()
                .map(w -> w.getCOSObject().getCOSName(COSName.AS).getName()).collect(Collectors.toList());
        
        List<String> expectedASList = Arrays.asList(expectedASs);
        
        assertEquals("Widgets AS", expectedASList, actualASList);    
    }

}
