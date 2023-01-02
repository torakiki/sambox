package org.sejda.sambox.pdmodel.interactive.form;
/*
 * Copyright 2022 Sober Lemur S.r.l.
 * Copyright 2022 Sejda BV
 *
 * Created 30/03/22
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.sejda.io.SeekableSources;
import org.sejda.sambox.input.PDFParser;
import org.sejda.sambox.pdmodel.PDDocument;
import org.sejda.sambox.rendering.TestPDFToImageTest;

/**
 * @author Andrea Vacondio
 */
public class CombAlignmentTest
{
    private static final File OUT_DIR = new File("target/test-output");
    private static final File IN_DIR = new File(
            "src/test/resources/org/sejda/sambox/pdmodel/interactive/form");
    private static final String NAME_OF_PDF = "CombTest.pdf";
    private static final String TEST_VALUE = "1234567";

    @BeforeAll
    public static void setUp() throws IOException
    {
        OUT_DIR.mkdirs();
    }

    // PDFBOX-5256
    @Test
    public void testCombFields() throws IOException
    {
        File outFile = new File(OUT_DIR, NAME_OF_PDF);
        try (PDDocument document = PDFParser.parse(
                SeekableSources.seekableSourceFrom(new File(IN_DIR, NAME_OF_PDF))))
        {
            PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();
            PDField field = acroForm.getField("PDFBoxCombLeft");
            field.setValue(TEST_VALUE);
            field = acroForm.getField("PDFBoxCombMiddle");
            field.setValue(TEST_VALUE);
            field = acroForm.getField("PDFBoxCombRight");
            field.setValue(TEST_VALUE);
            // compare rendering
            document.writeTo(outFile);
        }
        TestPDFToImageTest testPDFToImage = new TestPDFToImageTest(TestPDFToImageTest.class.getName());
        if (!testPDFToImage.doTestFile(outFile, IN_DIR.getAbsolutePath(),
                OUT_DIR.getAbsolutePath()))
        {
            Assertions.fail("Rendering of " + outFile
                    + " failed or is not identical to expected rendering in " + IN_DIR
                    + " directory");
        }
    }
}
