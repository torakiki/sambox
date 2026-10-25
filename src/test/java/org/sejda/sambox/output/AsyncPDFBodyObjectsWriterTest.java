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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sejda.sambox.cos.IndirectCOSObjectReference;

/**
 * @author Andrea Vacondio
 *
 */
public class AsyncPDFBodyObjectsWriterTest
{

    private IndirectObjectsWriter writer;
    private AsyncPDFBodyObjectsWriter victim;

    @BeforeEach
    public void setUp()
    {
        writer = mock(IndirectObjectsWriter.class);
        victim = new AsyncPDFBodyObjectsWriter(writer);
    }

    @AfterEach
    public void tearDown()
    {
        victim.close();
    }

    @Test
    public void nullConstructor()
    {
        assertThrows(IllegalArgumentException.class, () -> new AsyncPDFBodyObjectsWriter(null));
    }

    @Test
    public void asyncIOExceptionIsProcessed() throws IOException
    {
        doThrow(IOException.class).when(writer).writeObjectIfNotWritten(any());
        victim.writeObject(mock(IndirectCOSObjectReference.class));
        assertThrows(IOException.class, () -> victim.onWriteCompletion());
    }

    @Test
    public void asyncExceptionIsProcessed() throws IOException
    {
        doThrow(RuntimeException.class).when(writer).writeObjectIfNotWritten(any());
        victim.writeObject(mock(IndirectCOSObjectReference.class));
        assertThrows(IOException.class, () -> victim.onWriteCompletion());
    }

    @Test
    public void asyncErrorIsProcessed() throws IOException
    {
        doThrow(StackOverflowError.class).when(writer).writeObjectIfNotWritten(any());
        victim.writeObject(mock(IndirectCOSObjectReference.class));
        assertThrows(IOException.class, () -> victim.onWriteCompletion());
    }

    @Test
    public void asyncIOExceptionIsRethrownAsIs() throws IOException
    {
        // the failure happens while onWriteCompletion is waiting for the writing to complete
        var failure = new IOException("Failed");
        doAnswer(i -> {
            Thread.sleep(200);
            throw failure;
        }).when(writer).writeObjectIfNotWritten(any());
        victim.writeObject(mock(IndirectCOSObjectReference.class));
        assertSame(failure, assertThrows(IOException.class, () -> victim.onWriteCompletion()));
    }

    @Test
    public void interruptedOnWriteCompletion() throws Exception
    {
        var release = new CountDownLatch(1);
        doAnswer(i -> {
            release.await();
            return null;
        }).when(writer).writeObjectIfNotWritten(any());
        victim.writeObject(mock(IndirectCOSObjectReference.class));
        Thread.currentThread().interrupt();
        try
        {
            assertThrows(InterruptedIOException.class, () -> victim.onWriteCompletion());
            assertTrue(Thread.interrupted());
        }
        finally
        {
            release.countDown();
        }
    }

    @Test
    public void closeWaitsForTheObjectBeingWritten() throws Exception
    {
        // close is called while an object is being written and must return only once the writing is done
        var writing = new CountDownLatch(1);
        var written = new AtomicBoolean();
        doAnswer(i -> {
            writing.countDown();
            Thread.sleep(200);
            written.set(true);
            return null;
        }).when(writer).writeObjectIfNotWritten(any());
        victim.writeObject(mock(IndirectCOSObjectReference.class));
        writing.await();
        victim.close();
        assertTrue(written.get());
    }

    @Test
    public void writeObjectAfterCloseFails()
    {
        victim.close();
        assertThrows(IllegalStateException.class,
                () -> victim.writeObject(mock(IndirectCOSObjectReference.class)));
    }

    @Test
    public void closeTwice()
    {
        victim.close();
        assertDoesNotThrow(victim::close);
    }
}
