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

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;

import org.junit.Test;
import org.sejda.io.SeekableSources;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.input.PDFParser;
import org.sejda.sambox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.sejda.sambox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.*;

/**
 * @author Andrea Vacondio
 *
 */
public class PDPageTreeTest
{

    @Test
    public void indexOfPageFromOutlineDestination() throws IOException
    {
        try (PDDocument doc = PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(
                PDPageTreeTest.class.getResourceAsStream("with_outline.pdf"))))
        {
            PDDocumentOutline outline = doc.getDocumentCatalog().getDocumentOutline();
            for (PDOutlineItem current : outline.children())
            {
                if (current.getTitle().contains("Second"))
                {
                    assertEquals(2, doc.getPages().indexOf(current.findDestinationPage(doc)));
                }
            }
        }

    }

    @Test
    public void positiveSingleLevel() throws IOException
    {
        try (PDDocument doc = PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(
                PDPageTreeTest.class.getResourceAsStream("with_outline.pdf"))))
        {
            for (int i = 0; i < doc.getNumberOfPages(); i++)
            {
                assertEquals(i, doc.getPages().indexOf(doc.getPage(i)));
            }
        }
    }

    @Test
    public void positiveMultipleLevel() throws IOException
    {
        try (PDDocument doc = PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(
                PDPageTreeTest.class.getResourceAsStream("page_tree_multiple_levels.pdf"))))
        {
            for (int i = 0; i < doc.getNumberOfPages(); i++)
            {
                assertEquals(i, doc.getPages().indexOf(doc.getPage(i)));
            }
        }
    }

    @Test
    public void wrongMultipleLevel() throws IOException
    {
        try (PDDocument doc = PDFParser
                .parse(SeekableSources.inMemorySeekableSourceFrom(PDPageTreeTest.class
                        .getResourceAsStream("page_tree_multiple_levels_wrong_kid_type.pdf"))))
        {
            for (PDPage page : doc.getPages())
            {
                assertNotNull(page);
            }
        }
    }

    @Test
    public void negative() throws IOException
    {
        try (PDDocument doc = PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(
                PDPageTreeTest.class.getResourceAsStream("with_outline.pdf"))))
        {
            assertEquals(-1, doc.getPages().indexOf(new PDPage()));
        }
    }

    @Test
    public void pagesAsStream() throws IOException
    {
        try (PDDocument doc = PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(
                PDPageTreeTest.class.getResourceAsStream("with_outline.pdf"))))
        {
            assertEquals(6, doc.getPages().stream().count());
        }
    }

    @Test
    public void pagesAsStreamOrder() throws IOException
    {
        try (PDDocument doc = PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(
                PDPageTreeTest.class.getResourceAsStream("with_outline.pdf"))))
        {
            assertEquals(doc.getPage(0), doc.getPages().stream().findFirst().get());
            assertEquals(doc.getPage(1), doc.getPages().stream().skip(1).findFirst().get());
            assertEquals(doc.getPage(2), doc.getPages().stream().skip(2).findFirst().get());
            assertEquals(doc.getPage(3), doc.getPages().stream().skip(3).findFirst().get());
            assertEquals(doc.getPage(4), doc.getPages().stream().skip(4).findFirst().get());
            assertEquals(doc.getPage(5), doc.getPages().stream().skip(5).findFirst().get());
        }
    }

    @Test
    public void pageTypeIsSanitized() throws IOException
    {
        try (PDDocument doc = PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(
                PDPageTreeTest.class.getResourceAsStream("with_outline.pdf"))))
        {
            PDPage page = doc.getPage(0);
            page.getCOSObject().setItem(COSName.TYPE, null);
            assertEquals(COSName.PAGE, doc.getPage(0).getCOSObject().getItem(COSName.TYPE));
            // found localized cosname in the wild o_O
            page.getCOSObject().setName(COSName.TYPE, "Pagina");
            assertEquals(COSName.PAGE, doc.getPage(0).getCOSObject().getItem(COSName.TYPE));
        }
    }

    @Test
    public void pageNotFoundIncludesSourcePathAndPageNumber() throws IOException
    {
        try (PDDocument doc = PDFParser.parse(SeekableSources.onTempFileSeekableSourceFrom(
                PDPageTreeTest.class.getResourceAsStream("with_outline.pdf"))))
        {
            try {
                doc.getPage(99 /* 0 based */);
                fail("Exception expected");
            } catch (PageNotFoundException ex) {
                assertEquals(ex.getPage(), 100 /* 1 based, for humans */);
                assertThat(ex.getSourcePath(), containsString(File.separator + "SejdaIO"));
                assertThat(ex.getSourcePath(), endsWith(".tmp"));
            }
        }
    }
}
