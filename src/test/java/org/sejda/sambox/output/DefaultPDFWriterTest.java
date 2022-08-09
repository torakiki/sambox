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
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.sejda.io.BufferedCountingChannelWriter;
import org.sejda.io.SeekableSources;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSInteger;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.IndirectCOSObjectReference;
import org.sejda.sambox.input.ExistingIndirectCOSObject;
import org.sejda.sambox.input.PDFParser;
import org.sejda.sambox.pdmodel.PDDocument;
import org.sejda.sambox.util.SpecVersionUtils;
import org.sejda.sambox.xref.FileTrailer;
import org.sejda.sambox.xref.XrefEntry;

public class DefaultPDFWriterTest
{
    private BufferedCountingChannelWriter writer;
    private IndirectObjectsWriter objectWriter;
    private DefaultPDFWriter victim;
    private PDFWriteContext context;
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void setUp()
    {
        context = new PDFWriteContext(null);
        writer = mock(BufferedCountingChannelWriter.class);
        objectWriter = new IndirectObjectsWriter(writer, context);
        victim = new DefaultPDFWriter(objectWriter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullConstructor()
    {
        new DefaultPDFWriter(null);
    }

    @Test
    public void writeHeader() throws IOException
    {
        InOrder inOrder = Mockito.inOrder(writer);
        victim.writeHeader(SpecVersionUtils.V1_5);
        inOrder.verify(writer).write("%PDF-");
        inOrder.verify(writer).write(SpecVersionUtils.V1_5);
        inOrder.verify(writer).writeEOL();
        inOrder.verify(writer).write((byte) '%');
        inOrder.verify(writer)
                .write(aryEq(new byte[] { (byte) 0xA7, (byte) 0xE3, (byte) 0xF1, (byte) 0xF1 }));
        inOrder.verify(writer).writeEOL();
    }

    @Test
    public void writeIncrementalXrefTable() throws IOException
    {
        IndirectCOSObjectReference ref = new IndirectCOSObjectReference(1, 0, COSInteger.get(100));
        IndirectCOSObjectReference ref2 = new IndirectCOSObjectReference(2, 0, COSInteger.get(200));
        IndirectCOSObjectReference ref3 = new IndirectCOSObjectReference(3, 0, COSInteger.get(300));
        IndirectCOSObjectReference ref4 = new IndirectCOSObjectReference(8, 0, COSInteger.get(400));
        InOrder inOrder = Mockito.inOrder(writer);
        when(writer.offset()).thenReturn(1L);
        objectWriter.writeObject(ref);
        when(writer.offset()).thenReturn(2L);
        objectWriter.writeObject(ref2);
        when(writer.offset()).thenReturn(3L);
        objectWriter.writeObject(ref3);
        when(writer.offset()).thenReturn(4L);
        objectWriter.writeObject(ref4);
        assertEquals(4, victim.writeXrefTable());
        inOrder.verify(writer).write("xref");
        inOrder.verify(writer).writeEOL();
        inOrder.verify(writer).write("0 4");
        inOrder.verify(writer).writeEOL();
        inOrder.verify(writer).write(XrefEntry.DEFAULT_FREE_ENTRY.toXrefTableEntry());
        inOrder.verify(writer).write(ref.xrefEntry().toXrefTableEntry());
        inOrder.verify(writer).write(ref2.xrefEntry().toXrefTableEntry());
        inOrder.verify(writer).write(ref3.xrefEntry().toXrefTableEntry());
        inOrder.verify(writer).write("8 1");
        inOrder.verify(writer).writeEOL();
        inOrder.verify(writer).write(ref4.xrefEntry().toXrefTableEntry());
    }

    @Test
    public void writeTrailerSomeKeysAreRemoved() throws IOException
    {
        IndirectCOSObjectReference ref = new IndirectCOSObjectReference(1, 0, COSInteger.get(100));
        objectWriter.writeObject(ref);
        COSDictionary existingTrailer = new COSDictionary();
        existingTrailer.setName(COSName.PREV, "value");
        existingTrailer.setName(COSName.XREF_STM, "value");
        existingTrailer.setName(COSName.DOC_CHECKSUM, "value");
        existingTrailer.setName(COSName.DECODE_PARMS, "value");
        existingTrailer.setName(COSName.FILTER, "value");
        existingTrailer.setName(COSName.F_DECODE_PARMS, "value");
        existingTrailer.setName(COSName.F_FILTER, "value");
        existingTrailer.setName(COSName.F, "value");
        existingTrailer.setName(COSName.LENGTH, "value");

        victim.writeTrailer(existingTrailer, 1);
        assertFalse(existingTrailer.containsKey(COSName.PREV));
        assertFalse(existingTrailer.containsKey(COSName.XREF_STM));
        assertFalse(existingTrailer.containsKey(COSName.DOC_CHECKSUM));
        assertFalse(existingTrailer.containsKey(COSName.DECODE_PARMS));
        assertFalse(existingTrailer.containsKey(COSName.F_DECODE_PARMS));
        assertFalse(existingTrailer.containsKey(COSName.F_FILTER));
        assertFalse(existingTrailer.containsKey(COSName.F));
        assertFalse(existingTrailer.containsKey(COSName.FILTER));
        assertFalse(existingTrailer.containsKey(COSName.LENGTH));
    }

    @Test
    public void writeTrailerSizeIsSet() throws IOException
    {
        IndirectCOSObjectReference ref = new IndirectCOSObjectReference(1, 0, COSInteger.get(100));
        IndirectCOSObjectReference ref2 = new IndirectCOSObjectReference(3, 0, COSInteger.get(100));
        objectWriter.writeObject(ref);
        objectWriter.writeObject(ref2);
        COSDictionary existingTrailer = new COSDictionary();
        victim.writeTrailer(existingTrailer, 1);
        assertEquals(4, existingTrailer.getInt(COSName.SIZE));
    }

    @Test
    public void writeTrailer() throws IOException
    {
        IndirectCOSObjectReference ref = new IndirectCOSObjectReference(1, 0, COSInteger.get(100));
        InOrder inOrder = Mockito.inOrder(writer);
        objectWriter.writeObject(ref);
        COSDictionary existingTrailer = new COSDictionary();
        victim.writeTrailer(existingTrailer, 10);
        inOrder.verify(writer).write(aryEq("trailer".getBytes(StandardCharsets.US_ASCII)));
        inOrder.verify(writer).writeEOL();
        inOrder.verify(writer).write(aryEq("startxref".getBytes(StandardCharsets.US_ASCII)));
        inOrder.verify(writer).writeEOL();
        inOrder.verify(writer).write("10");
        inOrder.verify(writer).writeEOL();
        inOrder.verify(writer).write(aryEq("%%EOF".getBytes(StandardCharsets.US_ASCII)));
        inOrder.verify(writer).writeEOL();
    }

    @Test
    public void xrefTableSizeIsDirect() throws Exception
    {

        File temp = folder.newFile();
        try (PDDocument document = PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(
                getClass().getResourceAsStream("/sambox/trailer_size_matching_indirect.pdf"))))
        {
            document.writeTo(temp);
        }
        try (PDDocument newDoc = PDFParser.parse(SeekableSources.seekableSourceFrom(temp)))
        {
            FileTrailer trailer = newDoc.getDocument().getTrailer();
            assertFalse(trailer.getCOSObject()
                    .getItem(COSName.SIZE) instanceof ExistingIndirectCOSObject);
        }
    }

    @Test
    public void writeTrailerWithPrev() throws IOException
    {
        IndirectCOSObjectReference ref = new IndirectCOSObjectReference(1, 0, COSInteger.get(100));
        objectWriter.writeObject(ref);
        COSDictionary existingTrailer = new COSDictionary();
        victim.writeTrailer(existingTrailer, 10, 123);
        assertEquals(123, existingTrailer.getLong(COSName.PREV));
    }

    @Test
    public void writeXrefStream() throws IOException
    {
        COSDictionary existingTrailer = new COSDictionary();
        IndirectCOSObjectReference ref = new IndirectCOSObjectReference(1, 0, COSInteger.get(100));
        when(writer.offset()).thenReturn(12345L);
        objectWriter.writeObject(ref);
        InOrder inOrder = Mockito.inOrder(writer);
        victim.writeXrefStream(existingTrailer);
        assertTrue(objectWriter.context().hasWritten(XrefEntry.DEFAULT_FREE_ENTRY));
        inOrder.verify(writer).write("2");
        inOrder.verify(writer).write(DefaultCOSWriter.SPACE);
        inOrder.verify(writer).write("0");
        inOrder.verify(writer).write(DefaultCOSWriter.SPACE);
        inOrder.verify(writer).write(aryEq("obj".getBytes(StandardCharsets.US_ASCII)));
        inOrder.verify(writer).writeEOL();
        inOrder.verify(writer).write(aryEq("startxref".getBytes(StandardCharsets.US_ASCII)));
        inOrder.verify(writer).writeEOL();
        inOrder.verify(writer).write("12345");
        inOrder.verify(writer).writeEOL();
    }

    @Test
    public void writeXrefStreamWithPrev() throws IOException
    {
        COSDictionary existingTrailer = new COSDictionary();
        IndirectCOSObjectReference ref = new IndirectCOSObjectReference(1, 0, COSInteger.get(100));
        when(writer.offset()).thenReturn(12345L);
        objectWriter.writeObject(ref);
        victim.writeXrefStream(existingTrailer, 123);
        assertEquals(123, existingTrailer.getLong(COSName.PREV));
    }

    @Test
    public void writeXrefStreamSomeKeysAreRemoved() throws IOException
    {
        COSDictionary existingTrailer = new COSDictionary();
        existingTrailer.setName(COSName.PREV, "value");
        existingTrailer.setName(COSName.XREF_STM, "value");
        existingTrailer.setName(COSName.DOC_CHECKSUM, "value");
        existingTrailer.setName(COSName.DECODE_PARMS, "value");
        existingTrailer.setName(COSName.F_DECODE_PARMS, "value");
        existingTrailer.setName(COSName.F_FILTER, "value");
        existingTrailer.setName(COSName.F, "value");
        existingTrailer.setInt(COSName.LENGTH, 10);
        existingTrailer.setName(COSName.ENCRYPT, "value");
        IndirectCOSObjectReference ref = new IndirectCOSObjectReference(1, 0, COSInteger.get(100));
        when(writer.offset()).thenReturn(12345L);
        objectWriter.writeObject(ref);
        victim.writeXrefStream(existingTrailer);
        assertFalse(existingTrailer.containsKey(COSName.PREV));
        assertFalse(existingTrailer.containsKey(COSName.XREF_STM));
        assertFalse(existingTrailer.containsKey(COSName.DOC_CHECKSUM));
        assertFalse(existingTrailer.containsKey(COSName.DECODE_PARMS));
        assertFalse(existingTrailer.containsKey(COSName.F_DECODE_PARMS));
        assertFalse(existingTrailer.containsKey(COSName.F_FILTER));
        assertFalse(existingTrailer.containsKey(COSName.F));
        assertFalse(existingTrailer.containsKey(COSName.FILTER));
        assertFalse(existingTrailer.containsKey(COSName.LENGTH));
    }

    @Test
    public void writeXrefStreamIndexAndWAsDirect() throws IOException
    {
        COSDictionary existingTrailer = new COSDictionary();
        IndirectCOSObjectReference ref = spy(context.createIndirectReferenceFor(COSInteger.get(1)));
        context.addWritten(ref.xrefEntry());
        victim.writeXrefStream(existingTrailer);
        verify(writer, never()).write("1 0 R");
    }

    @Test
    public void close() throws Exception
    {
        victim.close();
        verify(writer).close();
    }
}
