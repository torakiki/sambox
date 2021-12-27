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

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sejda.io.CountingWritableByteChannel.from;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sejda.io.DevNullWritableByteChannel;
import org.sejda.sambox.TestUtils;
import org.sejda.sambox.cos.COSDocument;
import org.sejda.sambox.pdmodel.PDDocument;
import org.sejda.sambox.xref.FileTrailer;

/**
 * @author Andrea Vacondio
 *
 */
public class PDDocumentWriterTest
{

    private PDDocumentWriter victim;
    private DefaultPDFWriter writer;
    private IndirectObjectsWriter objectsWriter;

    @Before
    public void setUp()
    {
        this.writer = mock(DefaultPDFWriter.class);
        this.objectsWriter = mock(IndirectObjectsWriter.class);
        when(writer.writer()).thenReturn(objectsWriter);
        this.victim = new PDDocumentWriter(from(new DevNullWritableByteChannel()), null);
        TestUtils.setProperty(victim, "writer", this.writer);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullConstructor()
    {
        new PDDocumentWriter(null, Optional.empty());
    }

    @Test
    public void writeHaader() throws Exception
    {
        PDDocument document = new PDDocument();
        victim.write(document);
        verify(writer).writeHeader(document.getDocument().getHeaderVersion());
    }

    @Test
    public void writeBodySync() throws Exception
    {
        PDDocument document = mock(PDDocument.class);
        COSDocument cosDoc = mock(COSDocument.class);
        FileTrailer trailer = new FileTrailer();
        when(document.getDocument()).thenReturn(cosDoc);
        when(cosDoc.getTrailer()).thenReturn(trailer);
        this.victim = new PDDocumentWriter(from(new DevNullWritableByteChannel()), null);
        TestUtils.setProperty(victim, "writer", this.writer);
        ArgumentCaptor<PDFBodyWriter> bodyWriter = ArgumentCaptor.forClass(PDFBodyWriter.class);
        victim.write(document);
        verify(cosDoc).accept(bodyWriter.capture());
        assertThat(bodyWriter.getValue().objectsWriter, instanceOf(SyncPDFBodyObjectsWriter.class));
    }

    @Test
    public void writeBodyCompressed() throws Exception
    {
        PDDocument document = mock(PDDocument.class);
        COSDocument cosDoc = mock(COSDocument.class);
        FileTrailer trailer = new FileTrailer();
        when(document.getDocument()).thenReturn(cosDoc);
        when(cosDoc.getTrailer()).thenReturn(trailer);
        this.victim = new PDDocumentWriter(from(new DevNullWritableByteChannel()), null,
                WriteOption.OBJECT_STREAMS);
        TestUtils.setProperty(victim, "writer", this.writer);
        ArgumentCaptor<PDFBodyWriter> bodyWriter = ArgumentCaptor.forClass(PDFBodyWriter.class);
        victim.write(document);
        verify(cosDoc).accept(bodyWriter.capture());
        assertThat(bodyWriter.getValue().objectsWriter,
                instanceOf(ObjectsStreamPDFBodyObjectsWriter.class));
    }

    @Test
    public void writeBodyAsync() throws Exception
    {
        PDDocument document = mock(PDDocument.class);
        COSDocument cosDoc = mock(COSDocument.class);
        FileTrailer trailer = new FileTrailer();
        when(document.getDocument()).thenReturn(cosDoc);
        when(cosDoc.getTrailer()).thenReturn(trailer);
        this.victim = new PDDocumentWriter(from(new DevNullWritableByteChannel()), null,
                WriteOption.ASYNC_BODY_WRITE);
        TestUtils.setProperty(victim, "writer", this.writer);
        ArgumentCaptor<PDFBodyWriter> bodyWriter = ArgumentCaptor.forClass(PDFBodyWriter.class);
        victim.write(document);
        verify(cosDoc).accept(bodyWriter.capture());
        assertThat(bodyWriter.getValue().objectsWriter,
                instanceOf(AsyncPDFBodyObjectsWriter.class));
    }

    @Test
    public void writeXrefTable() throws Exception
    {
        try (PDDocument document = new PDDocument())
        {
            victim.write(document);
            verify(writer).writeXrefTable();
            verify(writer).writeTrailer(eq(document.getDocument().getTrailer().getCOSObject()),
                    anyLong());
        }
    }

    @Test
    public void writeXrefStream() throws Exception
    {
        try (PDDocument document = new PDDocument())
        {
            this.victim = new PDDocumentWriter(from(new DevNullWritableByteChannel()), null,
                    WriteOption.XREF_STREAM);
            TestUtils.setProperty(victim, "writer", this.writer);
            victim.write(document);
            verify(writer).writeXrefStream(document.getDocument().getTrailer().getCOSObject());
        }
    }

    @Test
    public void close() throws Exception
    {
        PDDocument document = new PDDocument();
        victim.write(document);
        victim.close();
        verify(writer).close();
    }
}
