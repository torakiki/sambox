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
import static org.junit.Assert.assertNotEquals;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.sejda.io.BufferedCountingChannelWriter;
import org.sejda.sambox.cos.COSInteger;
import org.sejda.sambox.cos.COSNull;
import org.sejda.sambox.cos.IndirectCOSObjectReference;

/**
 * @author Andrea Vacondio
 *
 */
public class IndirectObjectsWriterTest
{

    private BufferedCountingChannelWriter writer;
    private IndirectObjectsWriter victim;
    private PDFWriteContext context;

    @Before
    public void setUp()
    {
        context = new PDFWriteContext(null);
        writer = mock(BufferedCountingChannelWriter.class);
        victim = new IndirectObjectsWriter(writer, context);

    }

    @Test(expected = IllegalArgumentException.class)
    public void nullWriter()
    {
        new IndirectObjectsWriter((BufferedCountingChannelWriter) null, context);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullContext()
    {
        new IndirectObjectsWriter(writer, null);
    }

    @Test
    public void writerObject() throws IOException
    {
        when(writer.offset()).thenReturn(12345L);
        IndirectCOSObjectReference ref = new IndirectCOSObjectReference(123, 0,
                COSInteger.get(100));
        InOrder inOrder = Mockito.inOrder(writer);
        victim.writeObjectIfNotWritten(ref);
        inOrder.verify(writer).write("123");
        inOrder.verify(writer).write(DefaultCOSWriter.SPACE);
        inOrder.verify(writer).write("0");
        inOrder.verify(writer).write(DefaultCOSWriter.SPACE);
        inOrder.verify(writer).write(aryEq("obj".getBytes(StandardCharsets.US_ASCII)));
        inOrder.verify(writer).writeEOL();
        inOrder.verify(writer).write("100");
        inOrder.verify(writer).writeEOL();
        inOrder.verify(writer).write(aryEq("endobj".getBytes(StandardCharsets.US_ASCII)));
        inOrder.verify(writer).writeEOL();
        assertEquals(12345, ref.xrefEntry().getByteOffset());
    }

    @Test
    public void writerObjectOffsetIsSet() throws IOException
    {
        when(writer.offset()).thenReturn(12345L);
        IndirectCOSObjectReference ref = new IndirectCOSObjectReference(123, 0,
                COSInteger.get(100));
        victim.writeObjectIfNotWritten(ref);
        assertEquals(12345, ref.xrefEntry().getByteOffset());
    }

    @Test
    public void writerObjectReleaseIsCalled() throws IOException
    {
        IndirectCOSObjectReference ref = new IndirectCOSObjectReference(123, 0,
                COSInteger.get(100));
        assertNotEquals(COSNull.NULL, ref.getCOSObject());
        victim.writeObjectIfNotWritten(ref);
        assertEquals(COSNull.NULL, ref.getCOSObject());
    }

    @Test
    public void writerObjectMultipleTimesWritesOnce() throws IOException
    {
        IndirectCOSObjectReference ref = new IndirectCOSObjectReference(123, 0,
                COSInteger.get(100));
        victim.writeObjectIfNotWritten(ref);
        victim.writeObjectIfNotWritten(ref);
        victim.writeObjectIfNotWritten(ref);
        victim.writeObjectIfNotWritten(ref);
        verify(writer).write("123");
    }
}
