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

import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.sejda.io.CountingWritableByteChannel;
import org.sejda.sambox.TestUtils;
import org.sejda.sambox.cos.COSDocument;
import org.sejda.sambox.pdmodel.PDDocument;

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
        this.victim = new PDDocumentWriter(
                CountingWritableByteChannel.from(new ByteArrayOutputStream()), null);
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
        when(document.getDocument()).thenReturn(cosDoc);
        this.victim = new PDDocumentWriter(
                CountingWritableByteChannel.from(new ByteArrayOutputStream()), null,
                WriteOption.SYNC_BODY_WRITE);
        TestUtils.setProperty(victim, "writer", this.writer);
        victim.write(document);
        verify(cosDoc).accept(isA(SyncPDFBodyWriter.class));
    }

    @Test
    public void writeBodyCompressed() throws Exception
    {
        PDDocument document = mock(PDDocument.class);
        COSDocument cosDoc = mock(COSDocument.class);
        when(document.getDocument()).thenReturn(cosDoc);
        this.victim = new PDDocumentWriter(
                CountingWritableByteChannel.from(new ByteArrayOutputStream()), null,
                WriteOption.SYNC_BODY_WRITE, WriteOption.OBJECT_STREAMS);
        TestUtils.setProperty(victim, "writer", this.writer);
        victim.write(document);
        verify(cosDoc).accept(isA(ObjectsStreamPDFBodyWriter.class));
    }

    @Test
    public void writeBodyAsync() throws Exception
    {
        PDDocument document = mock(PDDocument.class);
        COSDocument cosDoc = mock(COSDocument.class);
        when(document.getDocument()).thenReturn(cosDoc);
        victim.write(document);
        verify(cosDoc).accept(isA(AsyncPDFBodyWriter.class));
    }

    @Test
    public void writeXrefTable() throws Exception
    {
        try (PDDocument document = new PDDocument())
        {
            victim.write(document);
            verify(writer).writeXrefTable();
            verify(writer).writeTrailer(eq(document.getDocument().getTrailer()), anyLong());
        }
    }

    @Test
    public void writeXrefStream() throws Exception
    {
        try (PDDocument document = new PDDocument())
        {
            this.victim = new PDDocumentWriter(
                    CountingWritableByteChannel.from(new ByteArrayOutputStream()), null,
                    WriteOption.XREF_STREAM);
            TestUtils.setProperty(victim, "writer", this.writer);
            victim.write(document);
            verify(writer).writeXrefStream(document.getDocument().getTrailer());
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
