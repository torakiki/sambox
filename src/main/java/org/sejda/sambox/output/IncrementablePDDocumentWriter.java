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
import static org.sejda.commons.util.RequireUtils.requireNotNullArg;
import static org.sejda.commons.util.RequireUtils.requireState;
import static org.sejda.sambox.encryption.EncryptionContext.encryptionAlgorithmFromEncryptionDictionary;
import static org.sejda.sambox.util.SpecVersionUtils.V1_5;
import static org.sejda.sambox.util.SpecVersionUtils.isAtLeast;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.sejda.commons.util.IOUtils;
import org.sejda.io.CountingWritableByteChannel;
import org.sejda.sambox.input.IncrementablePDDocument;
import org.sejda.sambox.pdmodel.PDDocument;
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
    private CountingWritableByteChannel channel;
    private Set<WriteOption> options;

    public IncrementablePDDocumentWriter(CountingWritableByteChannel channel,
            WriteOption... options)
    {
        requireNotNullArg(channel, "Cannot write to a null channel");
        this.channel = channel;
        this.options = ofNullable(options).map(Arrays::asList).map(HashSet::new)
                .orElseGet(HashSet::new);
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
        // Trailer offset is -1, we managed to perform a full scan so SAMBox might be able to handle the doc but not for
        // an incremental update which requires to have the newly created xref to point to the previous one and we don't
        // have the previous one offset
        // TODO idea: we write the incremented to a tmp file so we generate a new xref table?
        requireState(document.trailer().xrefOffset() != -1,
                "The incremented document has errors and its xref table couldn't be found");
        sanitizeWriteOptions(document);
        this.context = new PDFWriteContext(document.highestExistingReference().objectNumber(),
                encryptionAlgorithmFromEncryptionDictionary(document.encryptionDictionary(),
                        document.encryptionKey()),
                options.stream().toArray(WriteOption[]::new));
        this.writer = new DefaultPDFWriter(new IndirectObjectsWriter(channel, context));

        try (InputStream stream = document.incrementedAsStream())
        {
            writer.writer().write(stream);
        }
        writer.writer().writeEOL();
        writeBody(document);
        writeXref(document);
    }

    private void sanitizeWriteOptions(IncrementablePDDocument document)
    {
        // for incremental updates we have to write xref stream if the incremented doc has xref streams, xref table
        // otherwise
        if (document.trailer().isXrefStream())
        {
            options.add(WriteOption.XREF_STREAM);
        }
        else
        {
            options.remove(WriteOption.XREF_STREAM);
            options.remove(WriteOption.OBJECT_STREAMS);
        }
        // we remove the write option instead of increasing version because increasing the version would require to
        // update the Catalog as part of the incremental update, potentially breaking existing signatures
        if (!isAtLeast(document.incremented().getVersion(), V1_5))
        {
            options.remove(WriteOption.OBJECT_STREAMS);
        }
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
        if (context.hasWriteOption(WriteOption.ASYNC_BODY_WRITE))
        {
            return new AsyncPDFBodyObjectsWriter(writer.writer());    
        }
        return new SyncPDFBodyObjectsWriter(writer.writer());
        
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
