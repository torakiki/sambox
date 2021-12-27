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

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.Before;
import org.junit.Test;
import org.sejda.sambox.cos.COSFloat;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.pdmodel.PDDocument;
import org.sejda.sambox.pdmodel.common.PDRectangle;
import org.sejda.sambox.pdmodel.interactive.annotation.PDAnnotationWidget;

/**
 * Test for the PDSignatureField class.
 *
 */
public class PDTextFieldTest
{
    private PDDocument document;
    private PDAcroForm acroForm;

    @Before
    public void setUp()
    {
        document = new PDDocument();
        acroForm = new PDAcroForm(document);
    }

    @Test
    public void createDefaultTextField()
    {
        PDField textField = new PDTextField(acroForm);

        assertEquals(textField.getFieldType(),
                textField.getCOSObject().getNameAsString(COSName.FT));
        assertEquals(textField.getFieldType(), "Tx");
    }

    @Test
    public void createWidgetForGet()
    {
        PDTextField textField = new PDTextField(acroForm);

        assertNull(textField.getCOSObject().getItem(COSName.TYPE));
        assertNull(textField.getCOSObject().getNameAsString(COSName.SUBTYPE));

        PDAnnotationWidget widget = textField.getWidgets().get(0);

        assertEquals(COSName.ANNOT, textField.getCOSObject().getItem(COSName.TYPE));
        assertEquals(COSName.WIDGET.getName(),
                textField.getCOSObject().getNameAsString(COSName.SUBTYPE));

        assertEquals(widget.getCOSObject(), textField.getCOSObject());
    }

    @Test
    public void textValueNotCropped_Width() throws IOException
    {
        PDTextField textField = new PDTextField(acroForm);
        PDAnnotationWidget widget = textField.getWidgets().get(0);
        widget.setRectangle(new PDRectangle(0, 0, 100, 20));
        textField.setValue("This is a long text field value that could get cropped");

        byte[] bytes = widget.getAppearance().getNormalAppearance().getAppearanceStream()
                .getContentStream().toByteArray();
        String appearanceString = new String(bytes, StandardCharsets.UTF_8);
        assertThat(appearanceString, containsString("/Helvetica 4.222"));
    }

    @Test
    public void textFieldWithDefaultAppearanceAsCName() throws IOException
    {
        PDTextField textField = new PDTextField(acroForm);
        textField.setValue("Bla");
        textField.getCOSObject().setName(COSName.DA, "/Helv 12.345 Tf 0 g");


        assertEquals(12.345f, textField.getDefaultAppearanceString().getFontSize(), 0.01);
    }

    @Test
    public void textFieldWithDefaultAppearanceAsUnexpectedCOSFloat() throws IOException
    {
        PDTextField textField = new PDTextField(acroForm);
        textField.setValue("Bla");
        textField.getCOSObject().setItem(COSName.DA, COSFloat.get("12.345"));

        assertEquals(0, textField.getDefaultAppearanceString().getFontSize(), 0.01);
    }
}
