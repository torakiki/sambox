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
package org.sejda.sambox.output;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sejda.commons.util.IOUtils;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.input.ExistingIndirectCOSObject;
import org.sejda.sambox.pdmodel.PDDocument;
import org.sejda.sambox.pdmodel.PDPage;
import org.sejda.sambox.pdmodel.PDPageContentStream;
import org.sejda.sambox.pdmodel.font.PDFont;
import org.sejda.sambox.pdmodel.font.PDType1Font;

/**
 * @author Andrea Vacondio
 *
 */
public class ExistingPagesSizePredictorTest
{

    private ExistingPagesSizePredictor victim;
    private PDPage page;
    private PDDocument document;

    @Before
    public void setUp() throws IOException
    {
        victim = ExistingPagesSizePredictor.instance();
        document = new PDDocument();
        page = new PDPage();
        document.addPage(page);
        PDFont font = PDType1Font.HELVETICA_BOLD();
        try (PDPageContentStream contents = new PDPageContentStream(document, page))
        {
            contents.beginText();
            contents.setFont(font, 12);
            contents.newLineAtOffset(100, 700);
            contents.showText("Chuck Norris");
            contents.endText();
        }
    }

    @After
    public void tearDown()
    {
        IOUtils.closeQuietly(victim);
        IOUtils.closeQuietly(document);
    }

    @Test
    public void addPage() throws IOException
    {
        victim.addPage(page);
        assertEquals(1, victim.pages());
        assertTrue(victim.hasPages());
        assertTrue(victim.predictedPagesSize() > 0);
        assertEquals(5, victim.context().written());
    }

    @Test
    public void writeDoesntRelease() throws IOException
    {
        ExistingIndirectCOSObject item = mock(ExistingIndirectCOSObject.class);
        when(item.getCOSObject()).thenReturn(new COSDictionary());
        victim.addIndirectReferenceFor(item);
        verify(item, never()).releaseCOSObject();
    }

    @Test
    public void addPagTwiceAddsThePageReusesResources() throws IOException
    {
        victim.addPage(page);
        victim.addPage(page);
        assertEquals(2, victim.pages());
        assertTrue(victim.hasPages());
        assertTrue(victim.predictedPagesSize() > 0);
        assertEquals(6, victim.context().written());
    }

    @Test
    public void addPageNull() throws IOException
    {
        victim.addPage(null);
        assertEquals(0, victim.pages());
        assertFalse(victim.hasPages());
        assertFalse(victim.predictedPagesSize() > 0);
        assertEquals(0, victim.context().written());
    }

    @Test
    public void addIndirectReferenceForNull() throws IOException
    {
        victim.addIndirectReferenceFor(null);
        assertEquals(0, victim.pages());
        assertFalse(victim.hasPages());
        assertFalse(victim.predictedPagesSize() > 0);
        assertEquals(0, victim.context().written());
    }

    @Test
    public void addIndirectReferenceFor() throws IOException
    {
        victim.addIndirectReferenceFor(new COSDictionary());
        assertEquals(0, victim.pages());
        assertFalse(victim.hasPages());
        assertTrue(victim.predictedPagesSize() > 0);
        assertEquals(52, victim.predictedXrefTableSize());
        assertEquals(1, victim.context().written());
    }

    @Test
    public void addIndirectReferenceForDoesntAddTwice() throws IOException
    {
        COSDictionary object = new COSDictionary();
        victim.addIndirectReferenceFor(object);
        victim.addIndirectReferenceFor(object);
        assertEquals(52, victim.predictedXrefTableSize());
        assertEquals(1, victim.context().written());
    }
}
