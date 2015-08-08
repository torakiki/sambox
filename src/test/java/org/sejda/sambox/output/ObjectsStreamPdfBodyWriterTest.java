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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.sejda.sambox.SAMBox;
import org.sejda.sambox.cos.COSInteger;
import org.sejda.sambox.cos.COSStream;
import org.sejda.sambox.cos.IndirectCOSObjectReference;
import org.sejda.sambox.xref.CompressedXrefEntry;

/**
 * @author Andrea Vacondio
 *
 */
public class ObjectsStreamPdfBodyWriterTest
{
    private AbstractPdfBodyWriter writer;
    private ObjectsStreamPdfBodyWriter victim;

    @Before
    public void setUp()
    {
        this.writer = mock(AbstractPdfBodyWriter.class);
        this.victim = new ObjectsStreamPdfBodyWriter(writer);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullConstructor()
    {
        new ObjectsStreamPdfBodyWriter(null);
    }

    @Test
    public void cosDirectlyStreamWritten() throws IOException
    {
        IndirectCOSObjectReference ref = new IndirectCOSObjectReference(1, 0, new COSStream());
        this.victim.writeObject(ref);
        verify(writer).writeObject(ref);
    }

    @Test
    public void compressedXrefEntryIsAdded() throws IOException
    {
        IndirectObjectsWriter indirectObjWriter = mock(IndirectObjectsWriter.class);
        when(writer.writer()).thenReturn(indirectObjWriter);
        IndirectCOSObjectReference ref = new IndirectCOSObjectReference(2, 0, COSInteger.THREE);
        this.victim.writeObject(ref);
        verify(indirectObjWriter).put(any(CompressedXrefEntry.class));
    }

    @Test
    public void onCompletionDelegate() throws IOException
    {
        victim.onCompletion();
        verify(writer).onCompletion();
    }

    @Test
    public void fillingStreamWritesItDown() throws IOException
    {
        IndirectObjectsWriter indirectObjWriter = mock(IndirectObjectsWriter.class);
        when(writer.writer()).thenReturn(indirectObjWriter);
        System.setProperty(SAMBox.OBJECTS_STREAM_SIZE_PROPERTY, "2");
        victim.writeObject(new IndirectCOSObjectReference(2, 0, COSInteger.THREE));
        verify(writer, never()).writeObject(any());
        victim.writeObject(new IndirectCOSObjectReference(3, 0, COSInteger.THREE));
        // stream and length
        verify(writer, times(2)).writeObject(any());
    }

    @Test
    public void onCompletionWritesDown() throws IOException
    {
        IndirectObjectsWriter indirectObjWriter = mock(IndirectObjectsWriter.class);
        when(writer.writer()).thenReturn(indirectObjWriter);
        victim.writeObject(new IndirectCOSObjectReference(2, 0, COSInteger.THREE));
        victim.writeObject(new IndirectCOSObjectReference(3, 0, COSInteger.THREE));
        verify(writer, never()).writeObject(any());
        victim.onCompletion();
        // stream and length
        verify(writer, times(2)).writeObject(any());
    }
}
