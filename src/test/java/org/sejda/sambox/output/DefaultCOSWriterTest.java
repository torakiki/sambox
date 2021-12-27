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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyByte;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.AdditionalMatchers;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.sejda.commons.util.IOUtils;
import org.sejda.io.BufferedCountingChannelWriter;
import org.sejda.io.CountingWritableByteChannel;
import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSBoolean;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSDocument;
import org.sejda.sambox.cos.COSFloat;
import org.sejda.sambox.cos.COSInteger;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSNull;
import org.sejda.sambox.cos.COSStream;
import org.sejda.sambox.cos.COSString;
import org.sejda.sambox.cos.IndirectCOSObjectReference;

/**
 * @author Andrea Vacondio
 *
 */
public class DefaultCOSWriterTest
{
    private BufferedCountingChannelWriter writer;
    private DefaultCOSWriter victim;

    @Before
    public void setUp()
    {
        writer = mock(BufferedCountingChannelWriter.class);
        victim = new DefaultCOSWriter(writer);
    }

    @After
    public void tearDown()
    {
        IOUtils.closeQuietly(victim);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullWriterConstructor()
    {
        new DefaultCOSWriter((BufferedCountingChannelWriter) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullChannelConstructor()
    {
        new DefaultCOSWriter((CountingWritableByteChannel) null);
    }

    @Test
    public void visitCOSArray() throws Exception
    {
        COSArray array = new COSArray(COSBoolean.TRUE, COSInteger.get(10));
        InOrder inOrder = Mockito.inOrder(writer);
        victim.visit(array);
        inOrder.verify(writer).write((byte) 0x5B);
        inOrder.verify(writer).write("true");
        inOrder.verify(writer).write((byte) 0x20);
        inOrder.verify(writer).write("10");
        inOrder.verify(writer).write((byte) 0x5D);
        inOrder.verify(writer).writeEOL();
    }

    @Test
    public void visitCOSBoolean() throws Exception
    {
        victim.visit(COSBoolean.FALSE);
        verify(writer).write(COSBoolean.FALSE.toString());
    }

    @Test
    public void visitCOSDictionary() throws Exception
    {
        COSDictionary dictionary = new COSDictionary();
        dictionary.setBoolean(COSName.A, true);
        dictionary.setInt(COSName.C, 151);
        InOrder inOrder = Mockito.inOrder(writer);
        victim.visit(dictionary);
        inOrder.verify(writer, times(2)).write((byte) 0x3C);
        inOrder.verify(writer).writeEOL();
        inOrder.verify(writer).write((byte) 0x2F);
        inOrder.verify(writer).write((byte) 0x41);
        inOrder.verify(writer).write((byte) 0x20);
        inOrder.verify(writer).write(COSBoolean.TRUE.toString());
        inOrder.verify(writer).writeEOL();
        inOrder.verify(writer).write((byte) 0x2F);
        inOrder.verify(writer).write((byte) 0x43);
        inOrder.verify(writer).write((byte) 0x20);
        inOrder.verify(writer).write("151");
        inOrder.verify(writer).writeEOL();
        inOrder.verify(writer, times(2)).write((byte) 0x3E);
        inOrder.verify(writer).writeEOL();
    }

    @Test
    public void visitCOSFloat() throws Exception
    {
        victim.visit(new COSFloat(2.12345f));
        verify(writer).write("2.12345");
    }

    @Test
    public void visitCOSInteger() throws Exception
    {
        victim.visit(COSInteger.get(123456));
        verify(writer).write("123456");
    }

    @Test
    public void visitCOSName() throws Exception
    {
        COSName name = COSName.getPDFName("a()7");
        InOrder inOrder = Mockito.inOrder(writer);
        victim.visit(name);
        inOrder.verify(writer).write((byte) 0x2F);
        inOrder.verify(writer).write((byte) 0x61);
        inOrder.verify(writer).write((byte) 0x23);
        inOrder.verify(writer).write(AdditionalMatchers.aryEq(new byte[] { 0x32, 0x38 }));
        inOrder.verify(writer).write((byte) 0x23);
        inOrder.verify(writer).write(AdditionalMatchers.aryEq(new byte[] { 0x32, 0x39 }));
        inOrder.verify(writer).write((byte) 0x37);
    }

    @Test
    public void visitCOSNull() throws Exception
    {
        victim.visit(COSNull.NULL);
        verify(writer).write("null".getBytes(StandardCharsets.US_ASCII));
    }

    @Test
    public void visitCOSStream() throws Exception
    {
        byte[] data = new byte[] { (byte) 0x41, (byte) 0x42, (byte) 0x43 };
        COSStream stream = new COSStream();
        stream.setInt(COSName.B, 2);
        try (OutputStream out = stream.createUnfilteredStream())
        {
            out.write(data);
        }
        InOrder inOrder = Mockito.inOrder(writer);
        victim.visit(stream);

        inOrder.verify(writer, times(2)).write((byte) 0x3C);
        inOrder.verify(writer).writeEOL();
        inOrder.verify(writer).write((byte) 0x2F);
        inOrder.verify(writer).write((byte) 0x42);
        inOrder.verify(writer).write((byte) 0x20);
        inOrder.verify(writer).write("2");
        inOrder.verify(writer).writeEOL();
        inOrder.verify(writer).write((byte) 0x2F);
        inOrder.verify(writer).write((byte) 0x4C);
        inOrder.verify(writer).write((byte) 0x65);
        inOrder.verify(writer).write((byte) 0x6E);
        inOrder.verify(writer).write((byte) 0x67);
        inOrder.verify(writer).write((byte) 0x74);
        inOrder.verify(writer).write((byte) 0x68);
        inOrder.verify(writer).write((byte) 0x20);
        inOrder.verify(writer).write("3");
        inOrder.verify(writer).writeEOL();
        inOrder.verify(writer, times(2)).write((byte) 0x3E);
        inOrder.verify(writer).writeEOL();
        inOrder.verify(writer)
                .write(AdditionalMatchers.aryEq("stream".getBytes(StandardCharsets.US_ASCII)));
        inOrder.verify(writer).write(AdditionalMatchers.aryEq(new byte[] { '\r', '\n' }));
        inOrder.verify(writer).write(any(InputStream.class));
        inOrder.verify(writer).write(AdditionalMatchers.aryEq(new byte[] { '\r', '\n' }));
        inOrder.verify(writer)
                .write(AdditionalMatchers.aryEq("endstream".getBytes(StandardCharsets.US_ASCII)));
        inOrder.verify(writer).writeEOL();
    }

    @Test
    public void visitCOSStreamIndirectLength() throws Exception
    {
        victim = new DefaultCOSWriter(new BufferedCountingChannelWriter(
                CountingWritableByteChannel.from(new ByteArrayOutputStream())));
        byte[] data = new byte[] { (byte) 0x41, (byte) 0x42, (byte) 0x43 };
        COSStream stream = new COSStream();
        stream.setInt(COSName.B, 2);
        try (OutputStream out = stream.createUnfilteredStream())
        {
            out.write(data);
        }
        stream.setItem(COSName.LENGTH, new IndirectCOSObjectReference(10, 0, COSNull.NULL));
        victim.visit(stream);
        assertEquals(COSInteger.THREE, stream.getDictionaryObject(COSName.LENGTH).getCOSObject());
    }

    @Test
    public void visitHexCOSString() throws Exception
    {
        COSString string = COSString.parseLiteral("}Ap");
        string.setForceHexForm(true);
        victim.visit(string);
        verify(writer).write((byte) 0x3C);
        verify(writer).write("7D4170");
        verify(writer).write((byte) 0x3E);
    }

    @Test
    public void visitCOSString() throws Exception
    {
        COSString string = COSString.parseLiteral("Ap(C#");
        InOrder inOrder = Mockito.inOrder(writer);
        victim.visit(string);
        inOrder.verify(writer).write((byte) 0x28);
        inOrder.verify(writer).write((byte) 0x41);
        inOrder.verify(writer).write((byte) 0x70);
        inOrder.verify(writer).write((byte) 0x5C);
        inOrder.verify(writer).write((byte) 0x28);
        inOrder.verify(writer).write((byte) 0x43);
        inOrder.verify(writer).write((byte) 0x23);
        inOrder.verify(writer).write((byte) 0x29);
    }

    @Test
    public void visitCOSStringWithEscapeSequences() throws Exception
    {
        COSString string = COSString.parseLiteral("a\ns\t\r");
        InOrder inOrder = Mockito.inOrder(writer);
        victim.visit(string);
        inOrder.verify(writer).write((byte) 0x3C);
        inOrder.verify(writer).write("610A73090D");
        inOrder.verify(writer).write((byte) 0x3E);
    }

    @Test
    public void visitIndirectCOSObjectReference() throws Exception
    {
        IndirectCOSObjectReference ref = new IndirectCOSObjectReference(123, 0,
                new COSDictionary());
        victim.visit(ref);
        verify(writer).write(ref.toString());
    }

    @Test
    public void visitCOSDocument() throws Exception
    {
        victim.visit(new COSDocument());
        verify(writer, never()).write(anyByte());
    }

    @Test
    public void close() throws Exception
    {
        victim.close();
        verify(writer).close();
    }

}
