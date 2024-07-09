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
package org.sejda.sambox.pdmodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.junit.Test;
import org.sejda.commons.util.IOUtils;
import org.sejda.io.SeekableSources;
import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.input.PDFParser;
import org.sejda.sambox.pdmodel.graphics.color.PDOutputIntent;
import org.sejda.sambox.pdmodel.interactive.documentnavigation.destination.PDNamedDestination;

/**
 * Test PDDocument Catalog functionality.
 *
 */
public class TestPDDocumentCatalogTest
{

    /**
     * Test getNumberOfPages().
     * 
     * Test case for <a href="https://issues.apache.org/jira/browse/PDFBOX-911" >PDFBOX-911</a> - Method
     * PDDocument.getNumberOfPages() returns wrong number of pages
     * 
     * @throws IOException in case the document can not be parsed.
     */
    @Test
    public void retrieveNumberOfPages() throws IOException
    {
        try (PDDocument doc = PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(getClass()
                .getResourceAsStream("test.unc.pdf"))))
        {
            assertEquals(4, doc.getNumberOfPages());
        }
    }

    /**
     * Test OutputIntents functionality.
     * 
     * Test case for <a https://issues.apache.org/jira/browse/PDFBOX-2687">PDFBOX-2687</a> ClassCastException when
     * trying to get OutputIntents or add to it.
     * 
     * @throws IOException in case the document can not be parsed.
     */
    @Test
    public void handleOutputIntents() throws IOException
    {
        InputStream colorProfile = null;
        try (PDDocument doc = PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(getClass()
                .getResourceAsStream("test.unc.pdf"))))
        {
            PDDocumentCatalog catalog = doc.getDocumentCatalog();

            // retrieve OutputIntents
            List<PDOutputIntent> outputIntents = catalog.getOutputIntents();
            assertTrue(outputIntents.isEmpty());

            // add an OutputIntent
            colorProfile = TestPDDocumentCatalogTest.class
                    .getResourceAsStream("sRGB.icc");
            // create output intent
            PDOutputIntent oi = new PDOutputIntent(colorProfile);
            oi.setInfo("sRGB IEC61966-2.1");
            oi.setOutputCondition("sRGB IEC61966-2.1");
            oi.setOutputConditionIdentifier("sRGB IEC61966-2.1");
            oi.setRegistryName("http://www.color.org");
            doc.getDocumentCatalog().addOutputIntent(oi);

            // retrieve OutputIntents
            outputIntents = catalog.getOutputIntents();
            assertEquals(1, outputIntents.size());

            // set OutputIntents
            catalog.setOutputIntents(outputIntents);
            outputIntents = catalog.getOutputIntents();
            assertEquals(1, outputIntents.size());

        }
        finally
        {
            IOUtils.close(colorProfile);
        }
    }
    
    @Test
    public void testParsingInvalidNamesArray_KeyWithMissingValue() throws IOException {
        PDDocument broken = PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(
                getClass().getResourceAsStream("document_catalog_invalid_names_array.pdf")));
        broken.getDocumentCatalog().findNamedDestinationPage(new PDNamedDestination("PAGE_1"));
    }

    @Test
    public void invalidAcroForm() throws IOException {
        PDDocument doc = new PDDocument();
        PDDocumentCatalog catalog = doc.getDocumentCatalog();
        catalog.getCOSObject().setItem(COSName.ACRO_FORM, new COSArray());
        
        assertNull(catalog.getAcroForm());
    }
}
