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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import org.hamcrest.core.IsInstanceOf;
import org.junit.Before;
import org.junit.Test;
import org.sejda.sambox.SAMBox;
import org.sejda.sambox.cos.COSInteger;
import org.sejda.sambox.cos.COSStream;
import org.sejda.sambox.cos.IndirectCOSObjectReference;
import org.sejda.sambox.xref.CompressedXrefEntry;

/**
 * @author Andrea Vacondio
 */
public class ObjectsStreamPDFBodyObjectsWriterTest
{
    private PDFBodyObjectsWriter delegate;
    private ObjectsStreamPDFBodyObjectsWriter victim;
    private PDFWriteContext context;

    @Before
    public void setUp()
    {
        this.delegate = mock(PDFBodyObjectsWriter.class);
        context = new PDFWriteContext(null, null);
        this.victim = new ObjectsStreamPDFBodyObjectsWriter(context, delegate);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullContext()
    {
        new ObjectsStreamPDFBodyObjectsWriter(null, delegate);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullDelegate()
    {
        new ObjectsStreamPDFBodyObjectsWriter(context, null);
    }

    @Test
    public void cosDirectlyStreamWritten() throws IOException
    {
        IndirectCOSObjectReference ref = new IndirectCOSObjectReference(1, 0, new COSStream());
        this.victim.writeObject(ref);
        verify(delegate).writeObject(ref);
    }

    @Test
    public void compressedXrefEntryIsAdded() throws IOException
    {
        IndirectCOSObjectReference ref = new IndirectCOSObjectReference(2, 0, COSInteger.THREE);
        this.victim.writeObject(ref);
        assertEquals(1, context.written());
        assertThat(context.getWritten(2L), new IsInstanceOf(CompressedXrefEntry.class));
    }

    @Test
    public void fillingStreamWritesItDown() throws IOException
    {
        System.setProperty(SAMBox.OBJECTS_STREAM_SIZE_PROPERTY, "2");
        victim.writeObject(new IndirectCOSObjectReference(2, 0, COSInteger.THREE));
        verify(delegate, never()).writeObject(any());
        victim.writeObject(new IndirectCOSObjectReference(3, 0, COSInteger.THREE));
        // stream and length
        verify(delegate, times(2)).writeObject(any());
        System.getProperties().remove(SAMBox.OBJECTS_STREAM_SIZE_PROPERTY);
    }

    @Test
    public void onWriteCompletionWritesDown() throws IOException
    {
        victim.writeObject(new IndirectCOSObjectReference(2, 0, COSInteger.THREE));
        victim.writeObject(new IndirectCOSObjectReference(3, 0, COSInteger.THREE));
        verify(delegate, never()).writeObject(any());
        victim.onWriteCompletion();
        // stream and length
        verify(delegate, times(2)).writeObject(any());
    }

    @Test
    public void onWriteCompletion() throws IOException
    {
        victim.onWriteCompletion();
        verify(delegate).onWriteCompletion();
    }

    @Test
    public void closeDelegate() throws IOException
    {
        victim.close();
        verify(delegate).close();
    }
}
