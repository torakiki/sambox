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
import static org.apache.pdfbox.cos.DirectCOSObject.asDirectObject;

import java.io.Closeable;
import java.io.IOException;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.DirectCOSObject;
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



    public void writeTo(CountingWritableByteChannel channel) throws IOException
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
        DirectCOSObject id = asDirectObject(document.generateFileIdentifier());
        document.getDocument().getTrailer()
                .setItem(COSName.ID, asDirectObject(new COSArray(id, id)));
        writer = new PDFWriter(channel);
        writer.writeHeader(document.getDocument().getHeaderVersion());
        writer.writeBody(document.getDocument());
        long startxref = writer.writeXrefTable();
        writer.writeTrailer(document.getDocument().getTrailer(), startxref);
    }


    @Override
    public void close() throws IOException
    {
        IOUtils.close(writer);
        IOUtils.close(document);
    }
}
