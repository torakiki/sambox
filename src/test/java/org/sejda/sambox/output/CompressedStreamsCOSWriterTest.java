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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.OutputStream;

import org.junit.Before;
import org.junit.Test;
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
public class CompressedStreamsCOSWriterTest
{

    private DefaultCOSWriter writer;
    private CompressedStreamsCOSWriter victim;

    @Before
    public void setUp()
    {
        writer = mock(DefaultCOSWriter.class);
        victim = new CompressedStreamsCOSWriter(writer);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullWriterConstructor()
    {
        new CompressedStreamsCOSWriter(null);
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
        stream.setFilters(COSName.ASCII_HEX_DECODE);
        assertEquals(COSName.ASCII_HEX_DECODE, stream.getFilters());
        victim.visit(stream);
        assertArrayEquals(new COSArray(COSName.FLATE_DECODE, COSName.ASCII_HEX_DECODE).toArray(),
                ((COSArray) stream.getFilters()).toArray());
    }

    @Test
    public void visitCOSStreamSingleFilter() throws Exception
    {
        byte[] data = new byte[] { (byte) 0x41, (byte) 0x42, (byte) 0x43 };
        COSStream stream = new COSStream();
        stream.setInt(COSName.B, 2);
        try (OutputStream out = stream.createUnfilteredStream())
        {
            out.write(data);
        }
        assertNull(stream.getFilters());
        victim.visit(stream);
        assertEquals(COSName.FLATE_DECODE, stream.getFilters());
    }
    @Test
    public void visitCOSArray() throws Exception
    {
        COSArray array = new COSArray(COSBoolean.TRUE, COSInteger.get(10));
        victim.visit(array);
        verify(writer).visit(array);
    }

    @Test
    public void visitCOSBoolean() throws Exception
    {
        victim.visit(COSBoolean.FALSE);
        verify(writer).visit(COSBoolean.FALSE);
    }

    @Test
    public void visitCOSDictionary() throws Exception
    {
        COSDictionary dictionary = new COSDictionary();
        dictionary.setBoolean(COSName.A, true);
        dictionary.setInt(COSName.C, 151);
        victim.visit(dictionary);
        verify(writer).visit(dictionary);
    }

    @Test
    public void visitCOSFloat() throws Exception
    {
        COSFloat floatNum = new COSFloat(2.12345f);
        victim.visit(floatNum);
        verify(writer).visit(floatNum);
    }

    @Test
    public void visitCOSInteger() throws Exception
    {
        COSInteger intNum = COSInteger.get(123456);
        victim.visit(intNum);
        verify(writer).visit(intNum);
    }

    @Test
    public void visitCOSName() throws Exception
    {
        COSName name = COSName.getPDFName("a()7");
        victim.visit(name);
        verify(writer).visit(name);
    }

    @Test
    public void visitCOSNull() throws Exception
    {
        victim.visit(COSNull.NULL);
        verify(writer).visit(COSNull.NULL);
    }

    @Test
    public void visitHexCOSString() throws Exception
    {
        COSString string = COSString.parseLiteral("}Ap");
        string.setForceHexForm(true);
        victim.visit(string);
        verify(writer).visit(string);
    }

    @Test
    public void visitCOSString() throws Exception
    {
        COSString string = COSString.parseLiteral("Ap(C#");
        victim.visit(string);
        verify(writer).visit(string);
    }

    @Test
    public void visitIndirectCOSObjectReference() throws Exception
    {
        IndirectCOSObjectReference ref = new IndirectCOSObjectReference(123, 0, new COSDictionary());
        victim.visit(ref);
        verify(writer).visit(ref);
    }

    @Test
    public void visitCOSDocument() throws Exception
    {
        COSDocument doc = new COSDocument();
        victim.visit(doc);
        verify(writer).visit(doc);
    }

    @Test
    public void close() throws Exception
    {
        victim.close();
        verify(writer).close();
    }
}
