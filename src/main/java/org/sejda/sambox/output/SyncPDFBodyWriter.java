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

import static org.sejda.util.RequireUtils.requireNotNullArg;

import java.io.IOException;

import org.sejda.sambox.cos.IndirectCOSObjectReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Synchronous implementation of an {@link AbstractPDFBodyWriter}.
 * 
 * @author Andrea Vacondio
 *
 */
class SyncPDFBodyWriter extends AbstractPDFBodyWriter
{
    private static final Logger LOG = LoggerFactory.getLogger(SyncPDFBodyWriter.class);

    private IndirectObjectsWriter writer;

    SyncPDFBodyWriter(IndirectObjectsWriter writer, PDFWriteContext context)
    {
        super(context);
        requireNotNullArg(writer, "Cannot write to a null writer");
        this.writer = writer;
    }

    @Override
    void writeObject(IndirectCOSObjectReference ref) throws IOException
    {
        writer.writeObjectIfNotWritten(ref);
    }

    @Override
    void onCompletion()
    {
        LOG.debug("Written document body");
    }

}
