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
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import org.hamcrest.core.IsInstanceOf;
import org.junit.Before;
import org.junit.Test;
import org.sejda.io.SeekableSources;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.IndirectCOSObjectReference;
import org.sejda.sambox.input.PDFParser;
import org.sejda.sambox.pdmodel.PDDocument;

/**
 * @author Andrea Vacondio
 *
 */
public class SyncPdfBodyWriterTest
{

    private IndirectObjectsWriter writer;
    private SyncPdfBodyWriter victim;
    private PDDocument document;

    @Before
    public void setUp()
    {
        writer = mock(IndirectObjectsWriter.class);
        victim = new SyncPdfBodyWriter(writer);
        document = new PDDocument();
        document.getDocumentInformation().setAuthor("Chuck Norris");
        COSDictionary someDic = new COSDictionary();
        someDic.setInt(COSName.SIZE, 4);
        document.getDocument().getCatalog().setItem(COSName.G, someDic);
        document.getDocument().getCatalog().setItem(COSName.H, someDic);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullConstructor()
    {
        new SyncPdfBodyWriter(null);
    }

    @Test
    public void writer()
    {
        assertEquals(writer, victim.writer());
    }

    @Test
    public void writeBodyReusesDictionaryRef() throws IOException
    {
        victim.write(document.getDocument());
        assertEquals(document.getDocument().getCatalog().getItem(COSName.G), document.getDocument()
                .getCatalog().getItem(COSName.H));
        assertThat(document.getDocument().getCatalog().getItem(COSName.G), new IsInstanceOf(
                IndirectCOSObjectReference.class));
        verify(writer, times(4)).writeObjectIfNotWritten(any()); // catalog,info,pages,someDic
    }

    @Test
    public void nullSafeWriteBody() throws IOException
    {
        document.getDocument().getTrailer().setItem(COSName.INFO, null);
        victim.write(document.getDocument());
    }

    @Test
    public void writeBodyExistingDocument() throws Exception
    {
        try (PDDocument document = PDFParser.parse(SeekableSources
                .inMemorySeekableSourceFrom(getClass()
                        .getResourceAsStream("/input/simple_test.pdf"))))
        {
            victim.write(document.getDocument());
        }
        verify(writer, times(8)).writeObjectIfNotWritten(any());
    }
}
