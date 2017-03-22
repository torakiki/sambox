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

import java.io.IOException;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sejda.io.SeekableSources;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.input.PDFParser;
import org.sejda.sambox.pdmodel.PDDocument;
import org.sejda.sambox.pdmodel.interactive.annotation.PDAnnotationWidget;

/**
 * @author Andrea Vacondio
 *
 */
public class HandleDifferentDALevelsTest
{
    private PDDocument document;
    private PDAcroForm acroForm;

    @Before
    public void setUp() throws IOException
    {
        document = PDFParser.parse(
                SeekableSources.inMemorySeekableSourceFrom(this.getClass().getResourceAsStream(
                        "/org/sejda/sambox/pdmodel/interactive/form/DifferentDALevels.pdf")));
        acroForm = document.getDocumentCatalog().getAcroForm();

        // prefill the fields to generate the appearance streams
        PDTextField field = (PDTextField) acroForm.getField("SingleAnnotation");
        field.setValue("single annotation");

        field = (PDTextField) acroForm.getField("MultipeAnnotations-SameLayout");
        field.setValue("same layout");

        field = (PDTextField) acroForm.getField("MultipleAnnotations-DifferentLayout");
        field.setValue("different layout");
    }

    @After
    public void tearDown() throws IOException
    {
        document.close();
    }

    @Test
    public void checkSingleAnnotation() throws IOException
    {
        PDTextField field = (PDTextField) acroForm.getField("SingleAnnotation");
        String fieldFontSetting = getFontSettingFromDA(field);
        List<PDAnnotationWidget> widgets = field.getWidgets();
        for (PDAnnotationWidget widget : widgets)
        {
            String contentAsString = new String(
                    widget.getNormalAppearanceStream().getContentStream().toByteArray());
            assertTrue(contentAsString.indexOf(fieldFontSetting) > 0);
        }
    }

    @Test
    public void checkSameLayout() throws IOException
    {
        PDTextField field = (PDTextField) acroForm.getField("MultipeAnnotations-SameLayout");
        String fieldFontSetting = getFontSettingFromDA(field);
        List<PDAnnotationWidget> widgets = field.getWidgets();
        for (PDAnnotationWidget widget : widgets)
        {
            String contentAsString = new String(
                    widget.getNormalAppearanceStream().getContentStream().toByteArray());
            assertTrue("font setting in content stream shall be " + fieldFontSetting,
                    contentAsString.indexOf(fieldFontSetting) > 0);
        }
    }

    // TODO: enable the test after issue 3687 has been fixed
    // @Test
    public void checkDifferentLayout() throws IOException
    {
        PDTextField field = (PDTextField) acroForm.getField("MultipleAnnotations-DifferentLayout");
        String fieldFontSetting = getFontSettingFromDA(field);
        List<PDAnnotationWidget> widgets = field.getWidgets();
        for (PDAnnotationWidget widget : widgets)
        {
            String widgetFontSetting = getFontSettingFromDA(widget);
            String fontSetting = widgetFontSetting == null ? fieldFontSetting : widgetFontSetting;
            String contentAsString = new String(
                    widget.getNormalAppearanceStream().getContentStream().toByteArray());
            assertTrue("font setting in content stream shall be " + fontSetting,
                    contentAsString.indexOf(fontSetting) > 0);
        }
    }

    private static String getFontSettingFromDA(PDTextField field)
    {
        String defaultAppearance = field.getDefaultAppearance();
        // get the font setting from the default appearance string
        return defaultAppearance.substring(0, defaultAppearance.lastIndexOf("Tf") + 2);
    }

    private static String getFontSettingFromDA(PDAnnotationWidget widget)
    {
        String defaultAppearance = widget.getCOSObject().getString(COSName.DA);
        if (defaultAppearance != null)
        {
            return defaultAppearance.substring(0, defaultAppearance.lastIndexOf("Tf") + 2);
        }
        return defaultAppearance;
    }
}
