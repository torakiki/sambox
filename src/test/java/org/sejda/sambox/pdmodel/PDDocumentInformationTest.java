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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.sejda.io.SeekableSources;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.input.PDFParser;

import junit.framework.TestCase;

import static org.junit.Assert.*;

/**
 * This class tests the extraction of document-level metadata.
 * 
 * @author Neil McErlean
 * @since 1.3.0
 */
public class PDDocumentInformationTest
{

    @Test
    public void testMetadataExtraction() throws Exception
    {
        try (PDDocument doc = PDFParser.parse(SeekableSources
                .inMemorySeekableSourceFrom(getClass().getResourceAsStream("/input/hello3.pdf"))))
        {
            // This document has been selected for this test as it contains custom metadata.
            PDDocumentInformation info = doc.getDocumentInformation();

            assertEquals("Wrong author", "Brian Carrier", info.getAuthor());
            assertNotNull("Wrong creationDate", info.getCreationDate());
            assertEquals("Wrong creator", "Acrobat PDFMaker 8.1 for Word", info.getCreator());
            assertNull("Wrong keywords", info.getKeywords());
            assertNotNull("Wrong modificationDate", info.getModificationDate());
            assertEquals("Wrong producer", "Acrobat Distiller 8.1.0 (Windows)", info.getProducer());
            assertNull("Wrong subject", info.getSubject());
            assertNull("Wrong trapped", info.getTrapped());

            List<String> expectedMetadataKeys = Arrays
                    .asList(new String[] { "CreationDate", "Author", "Creator", "Producer",
                            "ModDate", "Company", "SourceModified", "Title" });
            assertEquals("Wrong metadata key count", expectedMetadataKeys.size(),
                    info.getMetadataKeys().size());
            for (String key : expectedMetadataKeys)
            {
                assertTrue("Missing metadata key:" + key, info.getMetadataKeys().contains(key));
            }

            // Custom metadata fields.
            assertEquals("Wrong company", "Basis Technology Corp.",
                    info.getCustomMetadataValue("Company"));
            assertEquals("Wrong sourceModified", "D:20080819181502",
                    info.getCustomMetadataValue("SourceModified"));
        }
    }

    /**
     * PDFBOX-3068: test that indirect /Title element of /Info entry can be found.
     * 
     * @throws Exception
     */
    @Test
    public void testPDFBox3068() throws Exception
    {
        try (PDDocument doc = PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(
                getClass().getResourceAsStream("/org/sejda/sambox/pdmodel/PDFBOX-3068.pdf"))))
        {
            PDDocumentInformation documentInformation = doc.getDocumentInformation();
            assertEquals("Title", documentInformation.getTitle());
        }
    }
    
    @Test
    public void removeMetadataField() throws IOException {
        try (PDDocument doc = PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(
                getClass().getResourceAsStream("/org/sejda/sambox/pdmodel/PDFBOX-3068.pdf"))))
        {
            PDDocumentInformation documentInformation = doc.getDocumentInformation();
            assertEquals("Title", documentInformation.getTitle());
            assertEquals("NOTEPAD", documentInformation.getCreator());
            
            documentInformation.removeMetadataField(COSName.TITLE.getName());
            documentInformation.removeMetadataField(COSName.CREATOR.getName());
            
            File temp = File.createTempFile("metadata-test", ".pdf");
            temp.deleteOnExit();
            
            doc.writeTo(temp);

            try (PDDocument doc2 = PDFParser.parse(SeekableSources.seekableSourceFrom(temp)))
            {
                PDDocumentInformation documentInformation2 = doc2.getDocumentInformation();
                assertNull(documentInformation2.getTitle());
                assertNull(documentInformation2.getCreator());
            }

            temp.delete();
        }
    }
}
