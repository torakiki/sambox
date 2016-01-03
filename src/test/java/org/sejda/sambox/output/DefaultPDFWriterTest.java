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
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.sejda.io.BufferedCountingChannelWriter;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSInteger;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.IndirectCOSObjectReference;
import org.sejda.sambox.util.Charsets;
import org.sejda.sambox.util.SpecVersionUtils;
import org.sejda.sambox.xref.XrefEntry;

public class DefaultPDFWriterTest
{
    private BufferedCountingChannelWriter writer;
    private IndirectObjectsWriter objectWriter;
    private DefaultPDFWriter victim;
    private PDFWriteContext context;

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
        inOrder.verify(writer).write(
                aryEq(new byte[] { (byte) 0xA7, (byte) 0xE3, (byte) 0xF1, (byte) 0xF1 }));
        inOrder.verify(writer).writeEOL();
    }

    @Test
    public void writeXrefTable() throws IOException
    {
        IndirectCOSObjectReference ref = new IndirectCOSObjectReference(1, 0, COSInteger.get(100));
        IndirectCOSObjectReference ref2 = new IndirectCOSObjectReference(3, 0, COSInteger.get(200));
        InOrder inOrder = Mockito.inOrder(writer);
        when(writer.offset()).thenReturn(12345l);
        objectWriter.writeObject(ref);
        when(writer.offset()).thenReturn(54321l);
        objectWriter.writeObject(ref2);
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
        objectWriter.writeObject(ref);
        COSDictionary existingTrailer = new COSDictionary();
        existingTrailer.setName(COSName.PREV, "value");
        existingTrailer.setName(COSName.XREF_STM, "value");
        existingTrailer.setName(COSName.DOC_CHECKSUM, "value");
        existingTrailer.setName(COSName.DECODE_PARMS, "value");
        existingTrailer.setName(COSName.F_DECODE_PARMS, "value");
        existingTrailer.setName(COSName.F_FILTER, "value");
        existingTrailer.setName(COSName.F, "value");
        existingTrailer.setName(COSName.ENCRYPT, "value");
        victim.writeTrailer(existingTrailer, 1);
        assertFalse(existingTrailer.containsKey(COSName.PREV));
        assertFalse(existingTrailer.containsKey(COSName.XREF_STM));
        assertFalse(existingTrailer.containsKey(COSName.DOC_CHECKSUM));
        assertFalse(existingTrailer.containsKey(COSName.DECODE_PARMS));
        assertFalse(existingTrailer.containsKey(COSName.F_DECODE_PARMS));
        assertFalse(existingTrailer.containsKey(COSName.F_FILTER));
        assertFalse(existingTrailer.containsKey(COSName.F));
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
        objectWriter.writeObject(ref);
        InOrder inOrder = Mockito.inOrder(writer);
        victim.writeXrefStream(existingTrailer);
        inOrder.verify(writer).write("2");
        inOrder.verify(writer).write(DefaultCOSWriter.SPACE);
        inOrder.verify(writer).write("0");
        inOrder.verify(writer).write(DefaultCOSWriter.SPACE);
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
