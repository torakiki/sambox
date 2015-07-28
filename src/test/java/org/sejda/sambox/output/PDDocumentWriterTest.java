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

import java.io.ByteArrayOutputStream;

import org.junit.Before;
import org.junit.Test;
import org.sejda.io.CountingWritableByteChannel;
import org.sejda.sambox.TestUtils;
import org.sejda.sambox.pdmodel.PDDocument;

/**
 * @author Andrea Vacondio
 *
 */
public class PDDocumentWriterTest
{

    private PDDocumentWriter victim;
    private PDFWriter writer;

    @Before
    public void setUp()
    {
        this.writer = mock(PDFWriter.class);
        this.victim = new PDDocumentWriter(
                CountingWritableByteChannel.from(new ByteArrayOutputStream()));
        TestUtils.setProperty(victim, "writer", this.writer);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullConstructor()
    {
        new PDDocumentWriter(null);
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
        PDDocument document = new PDDocument();
        victim.write(document, WriteOption.SYNC_BODY_WRITE);
        verify(writer).writeBody(eq(document.getDocument()), isA(SyncPdfBodyWriter.class));
    }

    @Test
    public void writeBodyAsync() throws Exception
    {
        PDDocument document = new PDDocument();
        victim.write(document);
        verify(writer).writeBody(eq(document.getDocument()), isA(AsyncPdfBodyWriter.class));
    }

    @Test
    public void writeXrefTable() throws Exception
    {
        PDDocument document = new PDDocument();
        victim.write(document);
        verify(writer).writeXrefTable();
        verify(writer).writeTrailer(eq(document.getDocument().getTrailer()), anyLong());
    }

    @Test
    public void writeXrefStream() throws Exception
    {
        PDDocument document = new PDDocument();
        victim.write(document, WriteOption.XREF_STREAM);
        verify(writer).writeXrefStream(document.getDocument().getTrailer());
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
