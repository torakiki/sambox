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
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import org.hamcrest.core.IsInstanceOf;
import org.junit.Before;
import org.junit.Test;
import org.sejda.io.SeekableSources;
import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSInteger;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSStream;
import org.sejda.sambox.cos.IndirectCOSObjectReference;
import org.sejda.sambox.input.ExistingIndirectCOSObject;
import org.sejda.sambox.input.PDFParser;
import org.sejda.sambox.pdmodel.PDDocument;
import org.sejda.sambox.pdmodel.PDPageTree;

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
        victim = new SyncPdfBodyWriter(writer, Arrays.asList(WriteOption.COMPRESS_STREAMS));
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
        new SyncPdfBodyWriter(null, null);
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
    public void writeThreads() throws IOException
    {
        document.getDocument().getCatalog()
                .setItem(COSName.THREADS, new COSArray(COSInteger.THREE, COSInteger.ONE));
        victim.write(document.getDocument());
        assertThat(document.getDocument().getCatalog().getItem(COSName.THREADS), new IsInstanceOf(
                IndirectCOSObjectReference.class));
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

    @Test
    public void writeExistingObjectAsRefAndDirect() throws Exception
    {
        try (PDDocument dest = new PDDocument())
        {
            try (PDDocument document = PDFParser.parse(SeekableSources
                    .inMemorySeekableSourceFrom(getClass().getResourceAsStream(
                            "/input/simple_test.pdf"))))
            {

                // we add it direct
                dest.addPage(document.getPage(0));
                PDPageTree pages = document.getDocumentCatalog().getPages();
                COSArray kids = (COSArray) pages.getCOSObject().getDictionaryObject(COSName.KIDS);
                COSBase page = kids.get(0);
                assertThat(page, new IsInstanceOf(ExistingIndirectCOSObject.class));
                // we add it wrapped
                dest.getDocumentCatalog().getCOSObject().setItem(COSName.T, page);
                victim.write(dest.getDocument());
            }
            PDPageTree pages = dest.getDocumentCatalog().getPages();
            COSArray kids = (COSArray) pages.getCOSObject().getDictionaryObject(COSName.KIDS);
            assertEquals(kids.get(0), dest.getDocumentCatalog().getCOSObject().getItem(COSName.T));
        }
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
        victim = new SyncPdfBodyWriter(writer, null);
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
