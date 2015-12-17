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

import java.io.IOException;

import org.junit.Test;
import org.sejda.io.SeekableSources;
import org.sejda.sambox.input.PDFParser;
import org.sejda.sambox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.sejda.sambox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;

/**
 * @author Andrea Vacondio
 *
 */
public class TestPDPageTree
{

    @Test
    public void indexOfPageFromOutlineDestination() throws IOException
    {
        try (PDDocument doc = PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(
                TestPDPageTree.class.getResourceAsStream("with_outline.pdf"))))
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
                TestPDPageTree.class.getResourceAsStream("with_outline.pdf"))))
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
                TestPDPageTree.class.getResourceAsStream("page_tree_multiple_levels.pdf"))))
        {
            for (int i = 0; i < doc.getNumberOfPages(); i++)
            {
                assertEquals(i, doc.getPages().indexOf(doc.getPage(i)));
            }
        }
    }

    @Test
    public void negative() throws IOException
    {
        try (PDDocument doc = PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(
                TestPDPageTree.class.getResourceAsStream("with_outline.pdf"))))
        {
            assertEquals(-1, doc.getPages().indexOf(new PDPage()));
        }
    }

    @Test
    public void pagesAsStream() throws IOException
    {
        try (PDDocument doc = PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(
                TestPDPageTree.class.getResourceAsStream("with_outline.pdf"))))
        {
            assertEquals(6, doc.getPages().stream().count());
        }
    }

    @Test
    public void pagesAsStreamOrder() throws IOException
    {
        try (PDDocument doc = PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(
                TestPDPageTree.class.getResourceAsStream("with_outline.pdf"))))
        {
            assertEquals(doc.getPage(0), doc.getPages().stream().findFirst().get());
            assertEquals(doc.getPage(1), doc.getPages().stream().skip(1).findFirst().get());
            assertEquals(doc.getPage(2), doc.getPages().stream().skip(2).findFirst().get());
            assertEquals(doc.getPage(3), doc.getPages().stream().skip(3).findFirst().get());
            assertEquals(doc.getPage(4), doc.getPages().stream().skip(4).findFirst().get());
            assertEquals(doc.getPage(5), doc.getPages().stream().skip(5).findFirst().get());
        }
    }
}
