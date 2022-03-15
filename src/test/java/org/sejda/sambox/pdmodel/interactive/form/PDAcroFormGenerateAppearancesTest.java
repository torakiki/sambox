package org.sejda.sambox.pdmodel.interactive.form;
/*
 * Copyright 2022 Sober Lemur S.a.s. di Vacondio Andrea
 * Copyright 2022 Sejda BV
 *
 * Created 11/03/22
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

import org.junit.jupiter.api.Test;
import org.sejda.io.SeekableSources;
import org.sejda.sambox.input.PDFParser;
import org.sejda.sambox.pdmodel.PDDocument;

import java.io.File;
import java.io.IOException;

/**
 * @author Andrea Vacondio
 */
public class PDAcroFormGenerateAppearancesTest
{
    /**
     * PDFBOX-5041 Missing font descriptor
     *
     * @throws IOException
     */
    @Test
    public void test5041MissingFontDescriptor() throws IOException
    {
        try (PDDocument testPdf = PDFParser.parse(
                SeekableSources.seekableSourceFrom(new File("target/pdfs/REDHAT-1301016-0.pdf"))))
        {
            testPdf.getDocumentCatalog().getAcroForm();
        }
    }

    /**
     * PDFBOX-4086 Character missing for encoding
     *
     * @throws IOException
     */
    @Test
    public void test4086CharNotEncodable() throws IOException
    {
        try (PDDocument testPdf = PDFParser.parse(
                SeekableSources.seekableSourceFrom(new File("target/pdfs/AML1.PDF"))))
        {
            testPdf.getDocumentCatalog().getAcroForm();
        }
    }

    /**
     * PDFBOX-5043 PaperMetaData
     *
     * @throws IOException
     */
    @Test
    public void test5043PaperMetaData() throws IOException
    {

        try (PDDocument testPdf = PDFParser.parse(
                SeekableSources.seekableSourceFrom(new File("target/pdfs/PDFBOX-3891-5.pdf"))))
        {
            testPdf.getDocumentCatalog().getAcroForm();
        }
    }
}
