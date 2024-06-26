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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.io.OutputStream;

import org.junit.Before;
import org.junit.Test;
import org.sejda.io.SeekableSources;
import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSObjectKey;
import org.sejda.sambox.cos.COSStream;
import org.sejda.sambox.cos.IndirectCOSObjectIdentifier;
import org.sejda.sambox.input.PDFParser;
import org.sejda.sambox.pdmodel.PDDocument;

/**
 * @author Andrea Vacondio
 */
public class PDFBodyWriterTest
{

    private PDFBodyWriter victim;
    private PDFBodyObjectsWriter writer;
    private PDDocument document;
    private PDFWriteContext context;

    @Before
    public void setUp()
    {
        context = spy(new PDFWriteContext(null, null, WriteOption.COMPRESS_STREAMS));
        writer = mock(PDFBodyObjectsWriter.class);
        document = new PDDocument();
        document.getDocumentInformation().setAuthor("Chuck Norris");
        COSDictionary someDic = new COSDictionary();
        someDic.setInt(COSName.SIZE, 4);
        document.getDocument().getCatalog().setItem(COSName.G, someDic);
        document.getDocument().getCatalog().setItem(COSName.H, someDic);
        victim = new PDFBodyWriter(context, writer);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullWriter()
    {
        new PDFBodyWriter(context, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullContext()
    {
        new PDFBodyWriter(null, writer);
    }

    @Test
    public void writeBodyExistingDocument() throws Exception
    {
        try (PDDocument document = PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(
                getClass().getResourceAsStream("/sambox/simple_test.pdf"))))
        {
            victim.write(document.getDocument());
        }
        verify(writer, times(9)).writeObject(any());
    }

    @Test
    public void writeAsyncBodyExistingDocument() throws Exception
    {
        IndirectObjectsWriter objectsWriter = mock(IndirectObjectsWriter.class);
        victim = new PDFBodyWriter(context, new AsyncPDFBodyObjectsWriter(objectsWriter));
        try (PDDocument document = PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(
                getClass().getResourceAsStream("/sambox/simple_test.pdf"))))
        {
            victim.write(document.getDocument());
        }
        verify(objectsWriter, timeout(1000).times(9)).writeObjectIfNotWritten(any());
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
        victim = new PDFBodyWriter(new PDFWriteContext(null, null), writer);
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

    @Test
    public void close() throws IOException
    {
        victim.close();
        verify(writer).close();
    }

    @Test(expected = IllegalStateException.class)
    public void cantWriteToClosedWriter() throws IOException
    {
        victim.close();
        victim.write(document.getDocument());
    }

    @Test
    public void createIndirectReferenceIfNeededFor()
    {
        COSDictionary dic = new COSDictionary();
        dic.idIfAbsent(new IndirectCOSObjectIdentifier(new COSObjectKey(20, 0), "ff"));
        victim.createIndirectReferenceIfNeededFor(dic);
        verify(context).createIndirectReferenceFor(dic);
    }

    @Test
    public void noCreateIndirectReferenceIfNeededFor()
    {
        COSDictionary dic = new COSDictionary();
        dic.idIfAbsent(new IndirectCOSObjectIdentifier(new COSObjectKey(20, 0), "ff"));
        context.createIndirectReferenceFor(dic);
        verify(context, times(1)).createIndirectReferenceFor(dic);
        victim.createIndirectReferenceIfNeededFor(dic);
        verify(context, times(1)).createIndirectReferenceFor(dic);
    }
}
