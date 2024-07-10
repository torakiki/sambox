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
package org.sejda.sambox.output;

import static org.mockito.ArgumentMatchers.anyByte;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.sejda.sambox.cos.COSDictionary.of;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.sejda.io.BufferedCountingChannelWriter;
import org.sejda.io.CountingWritableByteChannel;
import org.sejda.sambox.contentstream.operator.Operator;
import org.sejda.sambox.contentstream.operator.OperatorName;
import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSBoolean;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSInteger;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSStream;
import org.sejda.sambox.cos.IndirectCOSObjectReference;
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
        verify(writer).write("Tj".getBytes(StandardCharsets.ISO_8859_1));
        verify(writer).writeEOL();
    }

    @Test
    public void writeOperatorWithOperands() throws Exception
    {
        victim.writeOperator(Arrays.asList(COSInteger.ONE, new COSInteger(50)),
                Operator.getOperator("Td"));
        verify(writer).write("1");
        verify(writer).write("50");
        verify(writer).write("Td".getBytes(StandardCharsets.ISO_8859_1));
        verify(writer).writeEOL();
        verify(writer, times(2)).write((byte) 0x20);
    }

    @Test
    public void writeTokens() throws Exception
    {
        List<Object> tokens = new ArrayList<>();
        tokens.add(Operator.getOperator(OperatorName.BEGIN_INLINE_IMAGE));
        COSArray array = mock(COSArray.class);
        tokens.add(array);
        victim.writeTokens(tokens);
        verify(array).accept(victim);
        verify(writer).write(OperatorName.BEGIN_INLINE_IMAGE.getBytes(StandardCharsets.ISO_8859_1));
        verify(writer)
                .write(OperatorName.BEGIN_INLINE_IMAGE_DATA.getBytes(StandardCharsets.US_ASCII));
        verify(writer).write(OperatorName.END_INLINE_IMAGE.getBytes(StandardCharsets.US_ASCII));
        verify(writer, times(4)).writeEOL();
    }

    @Test
    public void writeInlineImage() throws Exception
    {
        byte[] imageDataArray = new byte[] { (byte) 0x41, (byte) 0x42, (byte) 0x43 };
        COSDictionary imageDictionary = of(COSName.A, COSBoolean.FALSE);
        Operator image = Operator.getOperator(OperatorName.BEGIN_INLINE_IMAGE);
        image.setImageData(imageDataArray);
        image.setImageParameters(imageDictionary);
        victim.writeTokens(image);
        verify(writer).write(OperatorName.BEGIN_INLINE_IMAGE.getBytes(StandardCharsets.ISO_8859_1));
        verify(writer)
                .write(OperatorName.BEGIN_INLINE_IMAGE_DATA.getBytes(StandardCharsets.US_ASCII));
        verify(writer).write(OperatorName.END_INLINE_IMAGE.getBytes(StandardCharsets.US_ASCII));
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
