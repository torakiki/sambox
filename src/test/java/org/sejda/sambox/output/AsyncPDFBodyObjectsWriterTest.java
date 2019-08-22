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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.sejda.sambox.cos.IndirectCOSObjectReference;

/**
 * @author Andrea Vacondio
 *
 */
public class AsyncPDFBodyObjectsWriterTest
{

    private IndirectObjectsWriter writer;
    private AsyncPDFBodyObjectsWriter victim;

    @Before
    public void setUp()
    {
        writer = mock(IndirectObjectsWriter.class);
        victim = new AsyncPDFBodyObjectsWriter(writer);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullConstructor()
    {
        new AsyncPDFBodyObjectsWriter(null);
    }

    @Test(expected = IOException.class)
    public void asyncIOExceptionIsProcessed() throws IOException
    {
        doThrow(IOException.class).when(writer).writeObjectIfNotWritten(any());
        victim.writeObject(mock(IndirectCOSObjectReference.class));
        victim.onWriteCompletion();
    }

    @Test(expected = IOException.class)
    public void asyncExceptionIsProcessed() throws IOException
    {
        doThrow(RuntimeException.class).when(writer).writeObjectIfNotWritten(any());
        victim.writeObject(mock(IndirectCOSObjectReference.class));
        victim.onWriteCompletion();
    }

}
