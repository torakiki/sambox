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

import java.io.Closeable;
import java.io.IOException;
import java.util.Optional;

import org.sejda.commons.util.IOUtils;
import org.sejda.io.CountingWritableByteChannel;
import org.sejda.sambox.cos.COSDocument;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.encryption.EncryptionContext;
import org.sejda.sambox.pdmodel.PDDocument;
import org.sejda.sambox.util.SpecVersionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writer for a {@link PDDocument}. This component provides methods to write a {@link PDDocument} to a
 * {@link CountingWritableByteChannel}.
 * 
 * @author Andrea Vacondio
 */
public class PDDocumentWriter implements Closeable
{
    private static final Logger LOG = LoggerFactory.getLogger(PDDocumentWriter.class);
    private DefaultPDFWriter writer;
    private PDFWriteContext context;
    private Optional<EncryptionContext> encryptionContext;

    public PDDocumentWriter(CountingWritableByteChannel channel,
            Optional<EncryptionContext> encryptionContext, WriteOption... options)
    {
        requireNotNullArg(channel, "Cannot write to a null channel");
        this.encryptionContext = ofNullable(encryptionContext).orElseGet(Optional::empty);
        this.context = new PDFWriteContext(
                this.encryptionContext.map(EncryptionContext::encryptionAlgorithm).orElse(null),
                options);
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
        ofNullable(document.getDocument().getTrailer()).map(t -> t.getCOSObject())
                .ifPresent(t -> t.removeItem(COSName.ENCRYPT));

        encryptionContext.ifPresent(c -> {
            document.getDocument()
                    .setEncryptionDictionary(c.security.encryption.generateEncryptionDictionary(c));
            LOG.debug("Generated encryption dictionary");
            ofNullable(document.getDocumentCatalog().getMetadata()).map(m -> m.getCOSObject())
                    .ifPresent(str -> str.encryptable(c.security.encryptMetadata));
        });

        writer.writeHeader(document.getDocument().getHeaderVersion());
        writeBody(document.getDocument());
        writeXref(document);
    }

    private void writeBody(COSDocument document) throws IOException
    {
        try (PDFBodyWriter bodyWriter = new PDFBodyWriter(context,
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

    private void writeXref(PDDocument document) throws IOException
    {
        if (context.hasWriteOption(WriteOption.XREF_STREAM)
                || context.hasWriteOption(WriteOption.OBJECT_STREAMS))
        {
            writer.writeXrefStream(document.getDocument().getTrailer().getCOSObject());
        }
        else
        {
            long startxref = writer.writeXrefTable();
            writer.writeTrailer(document.getDocument().getTrailer().getCOSObject(), startxref);
        }
    }

    @Override
    public void close() throws IOException
    {
        IOUtils.close(writer);
    }
}
