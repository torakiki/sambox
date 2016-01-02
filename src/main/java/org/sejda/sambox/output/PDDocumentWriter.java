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
import static org.sejda.util.RequireUtils.requireNotNullArg;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.WritableByteChannel;
import java.util.Optional;

import org.sejda.io.CountingWritableByteChannel;
import org.sejda.sambox.cos.COSDocument;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.encryption.GeneralEncryptionAlgorithm;
import org.sejda.sambox.encryption.StandardSecurity;
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
    private Optional<StandardSecurity> standardSecurity;

    public PDDocumentWriter(CountingWritableByteChannel channel, StandardSecurity standardSecurity,
            WriteOption... options)
    {
        requireNotNullArg(channel, "Cannot write to a null channel");
        this.standardSecurity = Optional.ofNullable(standardSecurity);
        context = new PDFWriteContext(
                this.standardSecurity.map(StandardSecurity::encryptionAlgorithm)
                        .orElse(GeneralEncryptionAlgorithm.IDENTITY),
                options);
        this.writer = new DefaultPDFWriter(new IndirectObjectsWriter(channel, context));
    }

    /**
     * Writes the {@link PDDocument}.
     * 
     * @param document
     * @param standardSecurity
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
        ofNullable(document.getDocument().getTrailer())
                .ifPresent(t -> t.removeItem(COSName.ENCRYPT));

        standardSecurity.ifPresent(s -> {
            document.getDocument()
                    .setEncryptionDictionary(s.encryption.generateEncryptionDictionary(s));
            LOG.debug("Generated encryption dictionary");
            ofNullable(document.getDocumentCatalog().getMetadata()).map(m -> m.getCOSObject())
                    .ifPresent(str -> str.encryptable(s.encryptMetadata));
        });

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
