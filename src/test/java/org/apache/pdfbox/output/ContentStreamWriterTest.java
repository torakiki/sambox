/*
 * Created on 24/lug/2015
 * Copyright 2015 by Andrea Vacondio (andrea.vacondio@gmail.com).
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * 
 * http://www.apache.org/licenses/LICENSE-2.0 
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License. 
 */
package org.apache.pdfbox.output;

import static org.mockito.Matchers.anyByte;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.cos.IndirectCOSObjectReference;
import org.apache.pdfbox.util.Charsets;
import org.junit.Before;
import org.junit.Test;
import org.sejda.io.BufferedCountingChannelWriter;
import org.sejda.io.CountingWritableByteChannel;

/**
 * @author Andrea Vacondio
 *
 */
public class ContentStreamWriterTest
{
    private BufferedCountingChannelWriter writer;
    private ContentStreamWriter victim;

    @Before
    public void setUp()
    {
        writer = mock(BufferedCountingChannelWriter.class);
        victim = new ContentStreamWriter(writer);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullWriterConstructor()
    {
        new ContentStreamWriter((BufferedCountingChannelWriter) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullChannelConstructor()
    {
        new ContentStreamWriter((CountingWritableByteChannel) null);
    }

    @Test
    public void writeContent() throws Exception
    {
        byte[] data = new byte[] { (byte) 0x41, (byte) 0x42, (byte) 0x43 };
        victim.writeContent(data);
        verify(writer).write(data);
    }

    @Test
    public void writeEOL() throws Exception
    {
        victim.writeEOL();
        verify(writer).writeEOL();
    }

    @Test
    public void writeSpace() throws Exception
    {
        victim.writeSpace();
        verify(writer).write((byte) 0x20);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void visitStream()
    {
        victim.visit(mock(COSStream.class));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void visitIndirectCOSObjectReference()
    {
        victim.visit(mock(IndirectCOSObjectReference.class));
    }

    @Test
    public void writeComplexObjectSeparator() throws Exception
    {
        victim.writeComplexObjectSeparator();
        verify(writer, never()).write(anyByte());
    }

    @Test
    public void writeOperator() throws Exception
    {
        victim.writeTokens(Operator.getOperator("Tj"));
        verify(writer).write("Tj".getBytes(Charsets.ISO_8859_1));
        verify(writer).writeEOL();
    }

    @Test
    public void writeTokens() throws Exception
    {
        List<Object> tokens = new ArrayList<>();
        tokens.add(Operator.getOperator(Operator.BI_OPERATOR));
        COSArray array = mock(COSArray.class);
        tokens.add(array);
        victim.writeTokens(tokens);
        verify(array).accept(victim);
        verify(writer).write(Operator.BI_OPERATOR.getBytes(Charsets.ISO_8859_1));
        verify(writer).write(Operator.ID_OPERATOR.getBytes(Charsets.US_ASCII));
        verify(writer).write(Operator.EI_OPERATOR.getBytes(Charsets.US_ASCII));
        verify(writer, times(4)).writeEOL();
    }

    @Test
    public void writeInlineImage() throws Exception
    {
        byte[] imageDataArray = new byte[] { (byte) 0x41, (byte) 0x42, (byte) 0x43 };
        COSDictionary imageDictionary = new COSDictionary();
        imageDictionary.setBoolean(COSName.A, false);
        Operator image = Operator.getOperator(Operator.BI_OPERATOR);
        image.setImageData(imageDataArray);
        image.setImageParameters(imageDictionary);
        victim.writeTokens(image);
        verify(writer).write(Operator.BI_OPERATOR.getBytes(Charsets.ISO_8859_1));
        verify(writer).write(Operator.ID_OPERATOR.getBytes(Charsets.US_ASCII));
        verify(writer).write(Operator.EI_OPERATOR.getBytes(Charsets.US_ASCII));
        verify(writer).write(imageDataArray);
        verify(writer, times(5)).writeEOL();
    }

    @Test(expected = IOException.class)
    public void writeUnsupportedToken() throws Exception
    {
        List<Object> tokens = new ArrayList<>();
        tokens.add("A String");
        victim.writeTokens(tokens);
    }

}
