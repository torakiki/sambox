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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.sejda.io.SeekableSources;
import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.input.ExistingIndirectCOSObject;
import org.sejda.sambox.input.IncrementablePDDocument;
import org.sejda.sambox.input.PDFParser;
import org.sejda.sambox.pdmodel.PDPage;
import org.sejda.sambox.pdmodel.common.PDRectangle;
import org.sejda.sambox.pdmodel.interactive.annotation.PDAnnotationText;

/**
 * @author Andrea Vacondio
 *
 */
public class IncrementalPDFBodyWriterTest
{

    private IncrementalPDFBodyWriter victim;
    private PDFWriteContext context;
    private PDFBodyObjectsWriter writer;

    @Before
    public void setUp()
    {
        writer = mock(PDFBodyObjectsWriter.class);
        context = mock(PDFWriteContext.class);
        victim = new IncrementalPDFBodyWriter(context, writer);
    }

    @Test
    public void existingOnPotentialIndirectObject() throws IOException
    {
        ExistingIndirectCOSObject ref = mock(ExistingIndirectCOSObject.class);
        victim.onPotentialIndirectObject(ref);
        verify(context).addExistingReference(ref);
    }

    @Test
    public void nonExistingOnPotentialIndirectObject() throws IOException
    {
        COSDictionary ref = mock(COSDictionary.class);
        victim.onPotentialIndirectObject(ref);
        verify(context, never()).addExistingReference(any());
        verify(ref).accept(victim);
    }

    @Test
    public void createIndirectReferenceIfNeededFor()
    {
        COSDictionary ref = new COSDictionary();
        victim.createIndirectReferenceIfNeededFor(ref);
        verify(context).createIndirectReferenceFor(ref);
    }

    @Test
    public void writeBodyIncrementedDocument() throws Exception
    {
        try (IncrementablePDDocument incrementable = PDFParser
                .parseToIncrement(SeekableSources.inMemorySeekableSourceFrom(
                        getClass().getResourceAsStream("/sambox/simple_test.pdf"))))
        {
            victim = new IncrementalPDFBodyWriter(new PDFWriteContext(
                    incrementable.highestExistingReference().objectNumber(), null, null), writer);
            PDAnnotationText annot = new PDAnnotationText();
            annot.setContents("My Annot");
            annot.setRectangle(new PDRectangle(266, 116, 430, 204));
            PDPage page = incrementable.incremented().getPage(0);
            COSArray annots = page.getCOSObject().getDictionaryObject(COSName.ANNOTS,
                    COSArray.class);
            annots.add(annot.getCOSObject());
            incrementable.modified(page);
            incrementable.newIndirect(annot);
            victim.write(incrementable);
        }
        verify(writer, times(2)).writeObject(any());
    }
}
