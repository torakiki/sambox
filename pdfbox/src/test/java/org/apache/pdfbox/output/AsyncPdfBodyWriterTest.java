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
package org.apache.pdfbox.output;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.concurrent.RejectedExecutionException;

import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.IndirectCOSObjectReference;
import org.apache.pdfbox.input.PDFParser;
import org.apache.pdfbox.input.source.SeekableSources;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.hamcrest.core.IsInstanceOf;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Andrea Vacondio
 *
 */
public class AsyncPdfBodyWriterTest
{

    private PDFWriter writer;
    private AsyncPdfBodyWriter victim;
    private PDDocument document;

    @Before
    public void setUp()
    {
        writer = mock(PDFWriter.class);
        victim = new AsyncPdfBodyWriter(writer);
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
        new AsyncPdfBodyWriter(null);
    }

    @Test
    public void writeBodyReusesDictionaryRef() throws IOException
    {
        victim.write(document.getDocument());
        assertEquals(document.getDocument().getCatalog().getItem(COSName.G), document.getDocument()
                .getCatalog().getItem(COSName.H));
        assertThat(document.getDocument().getCatalog().getItem(COSName.G), new IsInstanceOf(
                IndirectCOSObjectReference.class));
        verify(writer, timeout(1000).times(4)).writerObject(any()); // catalog,info,pages,someDic
    }

    @Test(expected = IOException.class)
    public void asyncExceptionIsProcessed() throws IOException
    {
        doThrow(IOException.class).when(writer).writerObject(any());
        victim.write(document.getDocument());
    }

    @Test(expected = RejectedExecutionException.class)
    public void cantWriteToClosedWriter() throws IOException
    {
        victim.close();
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
        verify(writer, timeout(1000).times(8)).writerObject(any());
    }
}
