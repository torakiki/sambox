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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sejda.io.SeekableSources;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.input.PDFParser;
import org.sejda.sambox.pdmodel.PDDocument;
import org.sejda.sambox.pdmodel.interactive.annotation.PDAnnotationWidget;

/**
 * Test for the PDButton class.
 *
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
        try (PDDocument doc = PDFParser
                .parse(SeekableSources.inMemorySeekableSourceFrom(getClass().getResourceAsStream(
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
        try (PDDocument doc = PDFParser
                .parse(SeekableSources.inMemorySeekableSourceFrom(getClass().getResourceAsStream(
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
            assertTrue(doc.getDocumentCatalog().getAcroForm().getFields().isEmpty());
        }
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

    @After
    public void tearDown() throws IOException
    {
        document.close();
    }

}
