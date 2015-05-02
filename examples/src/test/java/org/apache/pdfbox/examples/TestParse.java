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
package org.apache.pdfbox.examples;

import java.io.File;
import java.io.IOException;

import org.apache.pdfbox.input.DefaultPDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.junit.Test;

/**
 * @author Andrea Vacondio
 *
 */
public class TestParse
{
    private static final String FILEPATH = "/home/torakiki/Scaricati/Responsive Web Design by Example [eBook].pdf";
    private static final String ENC_FILEPATH = "/home/torakiki/repos/sejda/sejda-core/src/test/resources/pdf/enc_usr_own_same_pwd.pdf";
    @Test
    public void testParse() throws IOException
    {
        File file = new File(FILEPATH);
        PDDocument document = DefaultPDFParser.parse(file);
        document.writeTo("/home/torakiki/Scrivania/test_incremental_xref/Responsive_sambox.pdf");
    }

    @Test
    public void testParseEnc() throws IOException
    {
        File file = new File(ENC_FILEPATH);
        PDDocument document = DefaultPDFParser.parse(file, "test");
        print(document);
    }

    @Test(expected = InvalidPasswordException.class)
    public void testWrongParseEnc() throws IOException
    {
        File file = new File(ENC_FILEPATH);
        PDDocument document = DefaultPDFParser.parse(file, "banana");
        print(document);
    }

    private void print(PDDocument document)
    {
        PDDocumentCatalog catalog = document.getDocumentCatalog();
        System.out.println("Got catalog");
        PDPageTree pages = catalog.getPages();
        for (PDPage page : pages)
        {
            page.getCOSObject();
            System.out.println(page.getMediaBox());
        }
    }

}
