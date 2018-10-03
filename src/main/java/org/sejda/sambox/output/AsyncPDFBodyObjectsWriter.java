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

import static org.sejda.commons.util.RequireUtils.requireNotNullArg;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicReference;

import org.sejda.sambox.cos.IndirectCOSObjectReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of a PDFBodyObjectsWriter that asynchronously writes {@link IndirectCOSObjectReference}. Objects are
 * written submitting a task to a single thread executor service.
 * 
 * @author Andrea Vacondio
 *
 */
class AsyncPDFBodyObjectsWriter implements PDFBodyObjectsWriter
{

    private static final Logger LOG = LoggerFactory.getLogger(AsyncPDFBodyObjectsWriter.class);

    private ExecutorService executor = Executors.newSingleThreadExecutor(new ThreadFactory()
    {
        @Override
        public Thread newThread(Runnable target)
        {
            return new Thread(null, target, "pdf-writer-thread", 0);
        }
    });
    private AtomicReference<IOException> executionException = new AtomicReference<>();
    private IndirectObjectsWriter writer;

    AsyncPDFBodyObjectsWriter(IndirectObjectsWriter writer)
    {
        requireNotNullArg(writer, "Cannot write to a null writer");
        this.writer = writer;
    }

    @Override
    public void writeObject(IndirectCOSObjectReference ref) throws IOException
    {
        assertCanSubmitAsyncTask();
        executor.execute(() -> {
            try
            {
                if (executionException.get() == null)
                {
                    writer.writeObjectIfNotWritten(ref);
                }
            }
            catch (IOException e)
            {
                executionException.set(e);
            }
            catch (Exception e)
            {
                executionException.set(new IOException(e));
            }
        });
    }

    private void assertCanSubmitAsyncTask() throws IOException
    {
        IOException previous = executionException.get();
        if (previous != null)
        {
            executor.shutdownNow();
            throw previous;
        }
    }

    @Override
    public void onWriteCompletion() throws IOException
    {
        assertCanSubmitAsyncTask();
        try
        {
            executor.submit(() -> {
                IOException previous = executionException.get();
                if (previous != null)
                {
                    throw previous;
                }
                LOG.debug("Written document body");
                return null;
            }).get();
        }
        catch (InterruptedException e)
        {
            throw new IOException(e);
        }
        catch (ExecutionException e)
        {
            throw new IOException(e.getCause());
        }

    }

    @Override
    public void close()
    {
        executor.shutdown();
    }
}
