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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sejda.commons.util.IOUtils;
import org.sejda.io.SeekableSources;
import org.sejda.sambox.input.PDFParser;
import org.sejda.sambox.pdmodel.PDDocument;
import org.sejda.sambox.rendering.TestPDFToImageTest;

import java.io.File;
import java.io.IOException;

public class AlignmentTest
{
    private static final File OUT_DIR = new File("target/test-output");
    private static final File IN_DIR = new File(
            "src/test/resources/org/sejda/sambox/pdmodel/interactive/form");
    private static final String NAME_OF_PDF = "AlignmentTests.pdf";
    private static final String TEST_VALUE = "sdfASDF1234äöü";

    private PDDocument document;
    private PDAcroForm acroForm;

    @Before
    public void setUp() throws IOException
    {
        document = PDFParser.parse(
                SeekableSources.seekableSourceFrom(new File(IN_DIR, NAME_OF_PDF)));
        acroForm = document.getDocumentCatalog().getAcroForm();
        OUT_DIR.mkdirs();
    }

    @Test
    public void fillFields() throws IOException
    {
        PDField field = acroForm.getField("AlignLeft");
        field.setValue(TEST_VALUE);

        field = acroForm.getField("AlignLeft-Border_Small");
        field.setValue(TEST_VALUE);

        field = acroForm.getField("AlignLeft-Border_Medium");
        field.setValue(TEST_VALUE);

        field = acroForm.getField("AlignLeft-Border_Wide");
        field.setValue(TEST_VALUE);

        field = acroForm.getField("AlignLeft-Border_Wide_Clipped");
        field.setValue(TEST_VALUE);

        field = acroForm.getField("AlignLeft-Border_Small_Outside");
        field.setValue(TEST_VALUE);

        field = acroForm.getField("AlignMiddle");
        field.setValue(TEST_VALUE);

        field = acroForm.getField("AlignMiddle-Border_Small");
        field.setValue(TEST_VALUE);

        field = acroForm.getField("AlignMiddle-Border_Medium");
        field.setValue(TEST_VALUE);

        field = acroForm.getField("AlignMiddle-Border_Wide");
        field.setValue(TEST_VALUE);

        field = acroForm.getField("AlignMiddle-Border_Wide_Clipped");
        field.setValue(TEST_VALUE);

        field = acroForm.getField("AlignMiddle-Border_Medium_Outside");
        field.setValue(TEST_VALUE);

        field = acroForm.getField("AlignRight");
        field.setValue(TEST_VALUE);

        field = acroForm.getField("AlignRight-Border_Small");
        field.setValue(TEST_VALUE);

        field = acroForm.getField("AlignRight-Border_Medium");
        field.setValue(TEST_VALUE);

        field = acroForm.getField("AlignRight-Border_Wide");
        field.setValue(TEST_VALUE);

        field = acroForm.getField("AlignRight-Border_Wide_Clipped");
        field.setValue(TEST_VALUE);

        field = acroForm.getField("AlignRight-Border_Wide_Outside");
        field.setValue(TEST_VALUE);

        // compare rendering
        File file = new File(OUT_DIR, NAME_OF_PDF);
        document.writeTo(file);
        TestPDFToImageTest testPDFToImage = new TestPDFToImageTest(TestPDFToImageTest.class.getName());
        if (!testPDFToImage.doTestFile(file, IN_DIR.getAbsolutePath(), OUT_DIR.getAbsolutePath()))
        {
            Assert.fail(
                    "Rendering of " + file.getAbsolutePath() + " failed or is not identical to expected rendering in "
                            + IN_DIR + " directory");
        }
    }

    @After
    public void tearDown() throws IOException
    {
        IOUtils.close(document);
    }

}
