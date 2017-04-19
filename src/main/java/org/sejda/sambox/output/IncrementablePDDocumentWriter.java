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

import static java.util.Optional.ofNullable;
import static org.sejda.sambox.encryption.EncryptionContext.encryptionAlgorithmFromEncryptionDictionary;
import static org.sejda.util.RequireUtils.requireNotNullArg;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import org.sejda.io.CountingWritableByteChannel;
import org.sejda.sambox.encryption.EncryptionContext;
import org.sejda.sambox.input.IncrementablePDDocument;
import org.sejda.sambox.pdmodel.PDDocument;
import org.sejda.sambox.util.SpecVersionUtils;
import org.sejda.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writer for a {@link IncrementablePDDocument}. This component provides methods to write a
 * {@link IncrementablePDDocument} to a {@link CountingWritableByteChannel} performing an incremental update.
 * 
 * @author Andrea Vacondio
 */
public class IncrementablePDDocumentWriter implements Closeable
{
    private static final Logger LOG = LoggerFactory.getLogger(IncrementablePDDocumentWriter.class);
    private DefaultPDFWriter writer;
    private PDFWriteContext context;
    private Optional<EncryptionContext> encryptionContext;
    private CountingWritableByteChannel channel;
    private WriteOption[] options;

    public IncrementablePDDocumentWriter(CountingWritableByteChannel channel,
            WriteOption... options)
    {
        requireNotNullArg(channel, "Cannot write to a null channel");
        this.channel = channel;
        this.options = options;
        this.encryptionContext = ofNullable(encryptionContext).orElseGet(Optional::empty);

    }

    /**
     * Writes the {@link PDDocument}.
     * 
     * @param document
     * @param standardSecurity
     * @throws IOException
     */
    public void write(IncrementablePDDocument document) throws IOException
    {
        requireNotNullArg(document, "Incremented document cannot be null");

        this.context = new PDFWriteContext(document.highestExistingReference().objectNumber(),
                encryptionAlgorithmFromEncryptionDictionary(document.encryptionDictionary(),
                        document.encryptionKey()),
                options);
        this.writer = new DefaultPDFWriter(new IndirectObjectsWriter(channel, context));

        if (context.hasWriteOption(WriteOption.XREF_STREAM)
                || context.hasWriteOption(WriteOption.OBJECT_STREAMS))
        {
            document.requireMinVersion(SpecVersionUtils.V1_5);
        }

        // TODO what to do if doc was corrupted and we did a fullscan
        try (InputStream stream = document.incrementedAsStream())
        {
            writer.writer().write(stream);
        }
        writer.writer().writeEOL();
        writeBody(document);
        writeXref(document);
    }

    private void writeBody(IncrementablePDDocument document) throws IOException
    {
        try (PDFBodyWriter bodyWriter = new IncrementalPDFBodyWriter(context,
                objectStreamWriter(objectsWriter())))
        {
            LOG.debug("Writing body using " + bodyWriter.objectsWriter.getClass());
            bodyWriter.write(document);
        }
    }

    private PDFBodyObjectsWriter objectsWriter()
    {
        if (context.hasWriteOption(WriteOption.SYNC_BODY_WRITE))
        {
            return new SyncPDFBodyObjectsWriter(writer.writer());
        }
        return new AsyncPDFBodyObjectsWriter(writer.writer());
    }

    private PDFBodyObjectsWriter objectStreamWriter(PDFBodyObjectsWriter wrapped)
    {
        if (context.hasWriteOption(WriteOption.OBJECT_STREAMS))
        {
            return new ObjectsStreamPDFBodyObjectsWriter(context, wrapped);
        }
        return wrapped;
    }

    private void writeXref(IncrementablePDDocument document) throws IOException
    {
        // TODO if pref xref is -1 due to full scan
        if (context.hasWriteOption(WriteOption.XREF_STREAM)
                || context.hasWriteOption(WriteOption.OBJECT_STREAMS))
        {
            writer.writeXrefStream(document.trailer().getCOSObject(),
                    document.trailer().xrefOffset());
        }
        else
        {
            long startxref = writer.writeXrefTable();
            writer.writeTrailer(document.trailer().getCOSObject(), startxref,
                    document.trailer().xrefOffset());
        }
    }

    @Override
    public void close() throws IOException
    {
        IOUtils.close(writer);
    }
}
