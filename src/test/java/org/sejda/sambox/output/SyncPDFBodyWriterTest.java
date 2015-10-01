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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.io.OutputStream;

import org.junit.Before;
import org.junit.Test;
import org.sejda.io.SeekableSources;
import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSInteger;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSStream;
import org.sejda.sambox.input.PDFParser;
import org.sejda.sambox.pdmodel.PDDocument;

/**
 * @author Andrea Vacondio
 *
 */
public class SyncPDFBodyWriterTest
{

    private IndirectObjectsWriter writer;
    private SyncPDFBodyWriter victim;
    private PDDocument document;
    private PDFWriteContext context;

    @Before
    public void setUp()
    {
        context = new PDFWriteContext(WriteOption.COMPRESS_STREAMS);
        writer = mock(IndirectObjectsWriter.class);
        victim = new SyncPDFBodyWriter(writer, context);
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
        new SyncPDFBodyWriter(null, null);
    }

    @Test
    public void writeThreads() throws IOException
    {
        COSArray threads = new COSArray(COSInteger.THREE, COSInteger.ONE);
        document.getDocument().getCatalog().setItem(COSName.THREADS, threads);
        victim.write(document.getDocument());
        assertTrue(context.hasIndirectReferenceFor(threads));
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
.getResourceAsStream("/sambox/simple_test.pdf"))))
        {
            victim.write(document.getDocument());
        }
        verify(writer, times(8)).writeObjectIfNotWritten(any());
    }

    @Test
    public void writeCompressedStream() throws IOException
    {
        byte[] data = new byte[] { (byte) 0x41, (byte) 0x42, (byte) 0x43 };
        COSStream stream = new COSStream();
        stream.setInt(COSName.B, 2);
        try (OutputStream out = stream.createUnfilteredStream())
        {
            out.write(data);
        }
        assertNull(stream.getFilters());
        document.getDocument().getCatalog().setItem(COSName.SA, stream);
        victim.write(document.getDocument());
        assertEquals(COSName.FLATE_DECODE, stream.getFilters());
    }

    @Test
    public void writeUncompressedStream() throws IOException
    {
        victim = new SyncPDFBodyWriter(writer, new PDFWriteContext());
        byte[] data = new byte[] { (byte) 0x41, (byte) 0x42, (byte) 0x43 };
        COSStream stream = new COSStream();
        stream.setInt(COSName.B, 2);
        try (OutputStream out = stream.createUnfilteredStream())
        {
            out.write(data);
        }
        assertNull(stream.getFilters());
        document.getDocument().getCatalog().setItem(COSName.SA, stream);
        victim.write(document.getDocument());
        assertNull(stream.getFilters());
    }

    @Test
    public void writeCompressedStreamMultipleFilters() throws Exception
    {
        byte[] data = new byte[] { (byte) 0x41, (byte) 0x42, (byte) 0x43 };
        COSStream stream = new COSStream();
        stream.setInt(COSName.B, 2);
        try (OutputStream out = stream.createUnfilteredStream())
        {
            out.write(data);
        }
        stream.setFilters(COSName.ASCII_HEX_DECODE);
        assertEquals(COSName.ASCII_HEX_DECODE, stream.getFilters());
        document.getDocument().getCatalog().setItem(COSName.SA, stream);
        victim.write(document.getDocument());
        assertArrayEquals(new COSArray(COSName.FLATE_DECODE, COSName.ASCII_HEX_DECODE).toArray(),
                ((COSArray) stream.getFilters()).toArray());
    }
}
