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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.cos.COSInteger;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSNull;
import org.apache.pdfbox.cos.IndirectCOSObjectReference;
import org.apache.pdfbox.util.Charsets;
import org.apache.pdfbox.util.SpecVersionUtils;
import org.apache.pdfbox.xref.XrefEntry;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

public class PDFWriterTest
{
    private BufferedDestinationWriter writer;
    private PDFWriter victim;

    @Before
    public void setUp()
    {
        writer = mock(BufferedDestinationWriter.class);
        victim = new PDFWriter(writer);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullConstructor()
    {
        new PDFWriter(null);
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
        inOrder.verify(writer).write(
                aryEq(new byte[] { (byte) 0xA7, (byte) 0xE3, (byte) 0xF1, (byte) 0xF1 }));
        inOrder.verify(writer).writeEOL();
    }

    @Test
    public void writerObject() throws IOException
    {
        when(writer.offset()).thenReturn(12345l);
        IndirectCOSObjectReference ref = new IndirectCOSObjectReference(123, 0, COSInteger.get(100));
        InOrder inOrder = Mockito.inOrder(writer);
        victim.writerObject(ref);
        inOrder.verify(writer).write("123");
        inOrder.verify(writer).write(COSWriter.SPACE);
        inOrder.verify(writer).write("0");
        inOrder.verify(writer).write(COSWriter.SPACE);
        inOrder.verify(writer).write(aryEq("obj".getBytes(Charsets.US_ASCII)));
        inOrder.verify(writer).writeEOL();
        inOrder.verify(writer).write("100");
        inOrder.verify(writer).writeEOL();
        inOrder.verify(writer).write(aryEq("endobj".getBytes(Charsets.US_ASCII)));
        inOrder.verify(writer).writeEOL();
        assertEquals(12345, ref.xrefEntry().getByteOffset());
    }

    @Test
    public void writerObjectOffsetIsSet() throws IOException
    {
        when(writer.offset()).thenReturn(12345l);
        IndirectCOSObjectReference ref = new IndirectCOSObjectReference(123, 0, COSInteger.get(100));
        victim.writerObject(ref);
        assertEquals(12345, ref.xrefEntry().getByteOffset());
    }

    @Test
    public void writerObjectReleaseIsCalled() throws IOException
    {
        IndirectCOSObjectReference ref = new IndirectCOSObjectReference(123, 0, COSInteger.get(100));
        assertNotEquals(COSNull.NULL, ref.getCOSObject());
        victim.writerObject(ref);
        assertEquals(COSNull.NULL, ref.getCOSObject());
    }

    @Test
    public void writerObjectMultipleTimesWritesOnce() throws IOException
    {
        IndirectCOSObjectReference ref = new IndirectCOSObjectReference(123, 0, COSInteger.get(100));
        victim.writerObject(ref);
        victim.writerObject(ref);
        victim.writerObject(ref);
        victim.writerObject(ref);
        verify(writer).write("123");
    }

    @Test
    public void writeBody() throws IOException
    {
        COSDocument document = new COSDocument();
        AbstractPdfBodyWriter bodyWriter = mock(AbstractPdfBodyWriter.class);
        InOrder inOrder = Mockito.inOrder(bodyWriter);
        victim.writeBody(document, bodyWriter);
        inOrder.verify(bodyWriter).write(document);
        inOrder.verify(bodyWriter).close();
    }

    @Test
    public void writeXrefTable() throws IOException
    {
        IndirectCOSObjectReference ref = new IndirectCOSObjectReference(1, 0, COSInteger.get(100));
        IndirectCOSObjectReference ref2 = new IndirectCOSObjectReference(3, 0, COSInteger.get(200));
        InOrder inOrder = Mockito.inOrder(writer);
        when(writer.offset()).thenReturn(12345l);
        victim.writerObject(ref);
        when(writer.offset()).thenReturn(54321l);
        victim.writerObject(ref2);
        when(writer.offset()).thenReturn(98765l);
        assertEquals(98765, victim.writeXrefTable());
        inOrder.verify(writer).write("xref");
        inOrder.verify(writer).writeEOL();
        inOrder.verify(writer).write("0 3");
        inOrder.verify(writer).writeEOL();
        inOrder.verify(writer).write(XrefEntry.DEFAULT_FREE_ENTRY.toXrefTableEntry());
        inOrder.verify(writer).write(ref.xrefEntry().toXrefTableEntry());
        inOrder.verify(writer).write(XrefEntry.DEFAULT_FREE_ENTRY.toXrefTableEntry());
        inOrder.verify(writer).write(ref2.xrefEntry().toXrefTableEntry());
    }

    @Test
    public void writeTrailerSomeKeysAreRemoved() throws IOException
    {
        IndirectCOSObjectReference ref = new IndirectCOSObjectReference(1, 0, COSInteger.get(100));
        victim.writerObject(ref);
        COSDictionary existingTrailer = new COSDictionary();
        existingTrailer.setName(COSName.PREV, "value");
        existingTrailer.setName(COSName.XREF_STM, "value");
        existingTrailer.setName(COSName.DOC_CHECKSUM, "value");
        existingTrailer.setName(COSName.DECODE_PARMS, "value");
        existingTrailer.setName(COSName.F_DECODE_PARMS, "value");
        existingTrailer.setName(COSName.F_FILTER, "value");
        existingTrailer.setName(COSName.F, "value");
        // TODO remove this test once encryption is implemented
        existingTrailer.setName(COSName.ENCRYPT, "value");
        victim.writeTrailer(existingTrailer, 1);
        assertFalse(existingTrailer.containsKey(COSName.PREV));
        assertFalse(existingTrailer.containsKey(COSName.XREF_STM));
        assertFalse(existingTrailer.containsKey(COSName.DOC_CHECKSUM));
        assertFalse(existingTrailer.containsKey(COSName.DECODE_PARMS));
        assertFalse(existingTrailer.containsKey(COSName.F_DECODE_PARMS));
        assertFalse(existingTrailer.containsKey(COSName.F_FILTER));
        assertFalse(existingTrailer.containsKey(COSName.F));
        assertFalse(existingTrailer.containsKey(COSName.ENCRYPT));
    }

    @Test
    public void writeTrailerSizeIsSet() throws IOException
    {
        IndirectCOSObjectReference ref = new IndirectCOSObjectReference(1, 0, COSInteger.get(100));
        IndirectCOSObjectReference ref2 = new IndirectCOSObjectReference(3, 0, COSInteger.get(100));
        victim.writerObject(ref);
        victim.writerObject(ref2);
        COSDictionary existingTrailer = new COSDictionary();
        victim.writeTrailer(existingTrailer, 1);
        assertEquals(4, existingTrailer.getInt(COSName.SIZE));
    }

    @Test
    public void writeTrailer() throws IOException
    {
        IndirectCOSObjectReference ref = new IndirectCOSObjectReference(1, 0, COSInteger.get(100));
        InOrder inOrder = Mockito.inOrder(writer);
        victim.writerObject(ref);
        COSDictionary existingTrailer = new COSDictionary();
        victim.writeTrailer(existingTrailer, 10);
        inOrder.verify(writer).write(aryEq("trailer".getBytes(Charsets.US_ASCII)));
        inOrder.verify(writer).writeEOL();
        inOrder.verify(writer).write(aryEq("startxref".getBytes(Charsets.US_ASCII)));
        inOrder.verify(writer).writeEOL();
        inOrder.verify(writer).write("10");
        inOrder.verify(writer).writeEOL();
        inOrder.verify(writer).write(aryEq("%%EOF".getBytes(Charsets.US_ASCII)));
        inOrder.verify(writer).writeEOL();
    }

    @Test
    public void writeXrefStream() throws IOException
    {
        COSDictionary existingTrailer = new COSDictionary();
        IndirectCOSObjectReference ref = new IndirectCOSObjectReference(1, 0, COSInteger.get(100));
        when(writer.offset()).thenReturn(12345l);
        victim.writerObject(ref);
        InOrder inOrder = Mockito.inOrder(writer);
        victim.writeXrefStream(existingTrailer);
        inOrder.verify(writer).write("2");
        inOrder.verify(writer).write(COSWriter.SPACE);
        inOrder.verify(writer).write("0");
        inOrder.verify(writer).write(COSWriter.SPACE);
        inOrder.verify(writer).write(aryEq("obj".getBytes(Charsets.US_ASCII)));
        inOrder.verify(writer).writeEOL();
        // write the stream
        inOrder.verify(writer).write(aryEq("startxref".getBytes(Charsets.US_ASCII)));
        inOrder.verify(writer).writeEOL();
        inOrder.verify(writer).write("12345");
        inOrder.verify(writer).writeEOL();
    }

    @Test
    public void close() throws Exception
    {
        victim.close();
        verify(writer).close();
    }
}
