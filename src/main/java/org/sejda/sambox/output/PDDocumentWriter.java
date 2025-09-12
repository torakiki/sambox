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

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.sejda.commons.util.RequireUtils.requireNotNullArg;

import java.io.Closeable;
import java.io.IOException;

import org.sejda.commons.util.IOUtils;
import org.sejda.io.CountingWritableByteChannel;
import org.sejda.sambox.cos.COSDocument;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.encryption.EncryptionContext;
import org.sejda.sambox.pdmodel.PDDocument;
import org.sejda.sambox.pdmodel.common.PDDictionaryWrapper;
import org.sejda.sambox.pdmodel.common.PDStream;
import org.sejda.sambox.util.SpecVersionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writer for a {@link PDDocument}. This component provides methods to write a {@link PDDocument} to
 * a {@link CountingWritableByteChannel}.
 *
 * @author Andrea Vacondio
 */
public class PDDocumentWriter implements Closeable
{
    private static final Logger LOG = LoggerFactory.getLogger(PDDocumentWriter.class);
    private final DefaultPDFWriter writer;
    private final PDFWriteContext context;
    private final EncryptionContext encryptionContext;

    public PDDocumentWriter(CountingWritableByteChannel channel,
            EncryptionContext encryptionContext, PreSaveCOSVisitor preSaveCOSVisitor,
            WriteOption... options)
    {
        requireNotNullArg(channel, "Cannot write to a null channel");
        this.encryptionContext = encryptionContext;
        this.context = new PDFWriteContext(
                ofNullable(this.encryptionContext).map(EncryptionContext::encryptionAlgorithm)
                        .orElse(null), preSaveCOSVisitor, options);
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
        if (context.hasWriteOption(WriteOption.XREF_STREAM) || context.hasWriteOption(
                WriteOption.OBJECT_STREAMS))
        {
            document.requireMinVersion(SpecVersionUtils.V1_5);
        }
        ofNullable(document.getDocument().getTrailer()).map(PDDictionaryWrapper::getCOSObject)
                .ifPresent(t -> t.removeItem(COSName.ENCRYPT));

        if (nonNull(encryptionContext))
        {
            document.getDocument().setEncryptionDictionary(
                    encryptionContext.security.encryption.generateEncryptionDictionary(
                            encryptionContext));
            LOG.debug("Generated encryption dictionary");
            ofNullable(document.getDocumentCatalog().getMetadata()).map(PDStream::getCOSObject)
                    .ifPresent(str -> str.encryptable(encryptionContext.security.encryptMetadata));
        }

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
        var trailer = context.maybeTransform(document.getDocument().getTrailer().getCOSObject());
        if (context.hasWriteOption(WriteOption.XREF_STREAM) || context.hasWriteOption(
                WriteOption.OBJECT_STREAMS))
        {
            writer.writeXrefStream(trailer);
        }
        else
        {
            long startxref = writer.writeXrefTable();
            writer.writeTrailer(trailer, startxref);
        }
    }

    @Override
    public void close() throws IOException
    {
        IOUtils.close(writer);
    }
}
