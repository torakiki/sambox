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
package org.apache.pdfbox.output;

import static org.sejda.util.RequireUtils.requireNotNullArg;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.cos.IndirectCOSObjectReference;

/**
 * Asynchronous implementation of an {@link AbstractPdfBodyWriter} where a objects are written subbmitting a task to a
 * single thread executor service.
 * 
 * @author Andrea Vacondio
 *
 */
class AsyncPdfBodyWriter extends AbstractPdfBodyWriter
{
    private static final Log LOG = LogFactory.getLog(AsyncPdfBodyWriter.class);

    private ExecutorService executor = Executors.newSingleThreadExecutor(new ThreadFactory()
    {
        @Override
        public Thread newThread(Runnable target)
        {
            return new Thread(null, target, "pdf-writer-thread", 0);
        }
    });
    private AtomicReference<IOException> executionException = new AtomicReference<>();
    private PDFWriter writer;

    AsyncPdfBodyWriter(PDFWriter writer)
    {
        requireNotNullArg(writer, "Cannot write to a null writer");
        this.writer = writer;
    }

    @Override
    void onCompletion() throws IOException
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
    void writeObject(IndirectCOSObjectReference ref) throws IOException
    {
        assertCanSubmitAsyncTask();
        executor.execute(() -> {
            try
            {
                if (executionException.get() == null)
                {
                    writer.writerObject(ref);
                }
            }
            catch (IOException e)
            {
                executionException.set(e);
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
    public void close()
    {
        super.close();
        executor.shutdown();
    }
}
