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

import static java.util.Objects.requireNonNull;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.SecurityHandler;

/**
 * @author Andrea Vacondio
 *
 */
public class DefaultPDFWriter implements Closeable
{
    private PDDocument document;
    private PDFWriter writer;

    public DefaultPDFWriter(PDDocument document)
    {
        requireNonNull(document);
        this.document = document;
    }

    public void writeTo(CountingWritableByteChannel channel, WriteOption... options)
            throws IOException
    {
        if (document.getEncryption() != null)
        {
            // TODO refactor the encrypt/decrypt
            SecurityHandler securityHandler = document.getEncryption().getSecurityHandler();
            if (!securityHandler.hasProtectionPolicy())
            {
                throw new IllegalStateException(
                        "PDF contains an encryption dictionary, please remove it with "
                                + "setAllSecurityToBeRemoved() or set a protection policy with protect()");
            }
            securityHandler.prepareDocumentForEncryption(document);
        }
        writer = new PDFWriter(channel);
        writer.writeHeader(document.getDocument().getHeaderVersion());
        List<WriteOption> opts = Arrays.asList(options);
        writeBody(opts);
        writeXref(opts);
    }

    private void writeBody(List<WriteOption> opts) throws IOException
    {
        if (opts.contains(WriteOption.SYNC_BODY_WRITE))
        {
            writer.writeBodySync(document.getDocument());
        }
        else
        {
            writer.writeBodyAsync(document.getDocument());
        }
    }

    private void writeXref(List<WriteOption> opts) throws IOException
    {
        if (opts.contains(WriteOption.XREF_STREAM))
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
        IOUtils.close(document);
    }
}
