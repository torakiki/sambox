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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.pdmodel.PDDocument;

import org.sejda.sambox.pdmodel.PDPage;
import org.sejda.sambox.pdmodel.common.PDRectangle;
import org.sejda.sambox.pdmodel.graphics.color.PDColor;
import org.sejda.sambox.pdmodel.graphics.color.PDDeviceRGB;
import org.sejda.sambox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.sejda.sambox.pdmodel.interactive.annotation.PDAppearanceCharacteristicsDictionary;
import org.sejda.sambox.pdmodel.interactive.annotation.PDBorderStyleDictionary;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * This will test the functionality of Radio Buttons in PDFBox.
 */
public class TestCheckBox
{
    /**
     * This will test the radio button PDModel.
     *
     * @throws IOException If there is an error creating the field.
     */
    @org.junit.Test
    public void testCheckboxPDModel() throws IOException
    {
        PDDocument doc = null;
        try
        {
            doc = new PDDocument();
            PDAcroForm form = new PDAcroForm( doc );
            PDCheckBox checkBox = new PDCheckBox(form);
            
            // test that there are no nulls returned for an empty field
            // only specific methods are tested here
            assertNotNull(checkBox.getExportValues());
            assertNotNull(checkBox.getValue());
            
            // Test setting/getting option values - the dictionaries Opt entry
            List<String> options = new ArrayList<>();
            options.add("Value01");
            options.add("Value02");
            checkBox.setExportValues(options);

            COSArray optItem = (COSArray) checkBox.getCOSObject().getItem(COSName.OPT);

            // assert that the values have been correctly set
            assertNotNull(checkBox.getCOSObject().getItem(COSName.OPT));
            assertEquals(optItem.size(),2);
            assertEquals(options.get(0), optItem.getString(0));
            
            // assert that the values can be retrieved correctly
            List<String> retrievedOptions = checkBox.getExportValues();
            assertEquals(retrievedOptions.size(),2);
            assertEquals(retrievedOptions, options);

            // assert that the Opt entry is removed
            checkBox.setExportValues(null);
            assertNull(checkBox.getCOSObject().getItem(COSName.OPT));
            // if there is no Opt entry an empty List shall be returned
            assertEquals(checkBox.getExportValues(), new ArrayList<String>());
        }
        finally
        {
            if( doc != null )
            {
                doc.close();
            }
        }
    }

    /**
     * PDFBOX-4366: Create and test a checkbox with no /AP. The created file works with Adobe Reader!
     *
     * @throws IOException
     */
    @org.junit.Test
    public void testCheckBoxNoAppearance() throws IOException
    {
        PDDocument doc = new PDDocument();
        PDPage page = new PDPage();
        doc.addPage(page);
        PDAcroForm acroForm = new PDAcroForm(doc);
        acroForm.setNeedAppearances(true); // need this or it won't appear on Adobe Reader
        doc.getDocumentCatalog().setAcroForm(acroForm);
        List<PDField> fields = new ArrayList<>();
        PDCheckBox checkBox = new PDCheckBox(acroForm);
        checkBox.setPartialName("checkbox");
        PDAnnotationWidget widget = checkBox.getWidgets().get(0);
        widget.setRectangle(new PDRectangle(50, 600, 100, 100));
        PDBorderStyleDictionary bs = new PDBorderStyleDictionary();
        bs.setStyle(PDBorderStyleDictionary.STYLE_SOLID);
        bs.setWidth(1);
        COSDictionary acd = new COSDictionary();
        PDAppearanceCharacteristicsDictionary ac = new PDAppearanceCharacteristicsDictionary(acd);
        ac.setBackground(new PDColor(new float[] { 1, 1, 0 }, PDDeviceRGB.INSTANCE));
        ac.setBorderColour(new PDColor(new float[] { 1, 0, 0 }, PDDeviceRGB.INSTANCE));
        ac.setNormalCaption("4"); // 4 is checkmark, 8 is cross
        widget.setAppearanceCharacteristics(ac);
        widget.setBorderStyle(bs);
        checkBox.setValue("Off");
        fields.add(checkBox);
        page.getAnnotations().add(widget);
        acroForm.setFields(fields);
        doc.close();
    }
}