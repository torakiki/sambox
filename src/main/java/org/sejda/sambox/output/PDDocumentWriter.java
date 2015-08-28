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

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.WritableByteChannel;

import org.sejda.io.CountingWritableByteChannel;
import org.sejda.sambox.cos.COSDocument;
import org.sejda.sambox.pdmodel.PDDocument;
import org.sejda.sambox.util.SpecVersionUtils;
import org.sejda.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writer for a {@link PDDocument}. This component wraps a required {@link PDDocument} and provides methods to write it
 * to a {@link WritableByteChannel}.
 * 
 * @author Andrea Vacondio
 */
public class PDDocumentWriter implements Closeable
{
    private static final Logger LOG = LoggerFactory.getLogger(PDDocumentWriter.class);
    private DefaultPDFWriter writer;
    private PDFWriteContext context;

    public PDDocumentWriter(CountingWritableByteChannel channel, WriteOption... options)
    {
        requireNotNullArg(channel, "Cannot write to a null channel");
        context = new PDFWriteContext(options);
        this.writer = new DefaultPDFWriter(new IndirectObjectsWriter(channel, context));
    }

    /**
     * Writes the {@link PDDocument}.
     * 
     * @param document
     * @throws IOException
     */
    public void write(PDDocument document) throws IOException
    {
        requireNotNullArg(document, "PDDocument cannot be null");
        if (context.hasWriteOption(WriteOption.XREF_STREAM)
                || context.hasWriteOption(WriteOption.OBJECT_STREAMS))
        {
            document.requireMinVersion(SpecVersionUtils.V1_5);
        }
        if (document.getEncryption() != null)
        {
            // TODO refactor the encrypt/decrypt
            LOG.warn("Encryption is not supported yet, the document will be written decrypted");
        }
        writer.writeHeader(document.getDocument().getHeaderVersion());
        writeBody(document.getDocument());
        writeXref(document);
    }

    private void writeBody(COSDocument document) throws IOException
    {
        try (AbstractPDFBodyWriter bodyWriter = objectStreamWriter(bodyWriter()))
        {
            LOG.debug("Writing body using " + bodyWriter.getClass());
            bodyWriter.write(document);
        }
    }

    private AbstractPDFBodyWriter bodyWriter()
    {
        if (context.hasWriteOption(WriteOption.SYNC_BODY_WRITE))
        {
            return new SyncPDFBodyWriter(writer.writer(), context);
        }
        return new AsyncPDFBodyWriter(writer.writer(), context);
    }

    private AbstractPDFBodyWriter objectStreamWriter(AbstractPDFBodyWriter wrapped)
    {
        if (context.hasWriteOption(WriteOption.OBJECT_STREAMS))
        {
            return new ObjectsStreamPDFBodyWriter(wrapped);
        }
        return wrapped;
    }

    private void writeXref(PDDocument document) throws IOException
    {
        if (context.hasWriteOption(WriteOption.XREF_STREAM)
                || context.hasWriteOption(WriteOption.OBJECT_STREAMS))
        {
            writer.writeXrefStream(document.getDocument().getTrailer());
        }
        else
        {
            long startxref = writer.writeXrefTable();
            writer.writeTrailer(document.getDocument().getTrailer(), startxref);
        }
    }

    @Override
    public void close() throws IOException
    {
        IOUtils.close(writer);
    }
}
