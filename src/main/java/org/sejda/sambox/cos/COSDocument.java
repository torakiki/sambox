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
package org.sejda.sambox.cos;

import static java.util.Optional.ofNullable;
import static org.sejda.util.RequireUtils.requireNotBlank;
import static org.sejda.util.RequireUtils.requireNotNullArg;

import java.io.IOException;
import java.util.Optional;

import org.sejda.sambox.util.SpecVersionUtils;

/**
 * This is the in-memory representation of the PDF document.
 *
 * @author Ben Litchfield
 * 
 */
public class COSDocument extends COSBase
{

    private String headerVersion;
    private COSDictionary trailer;

    public COSDocument()
    {
        this(new COSDictionary(), SpecVersionUtils.V1_4);
        COSDictionary catalog = new COSDictionary();
        catalog.setItem(COSName.TYPE, COSName.CATALOG);
        trailer.setItem(COSName.ROOT, catalog);
    }

    public COSDocument(COSDictionary trailer)
    {
        this(trailer, SpecVersionUtils.V1_4);
    }

    public COSDocument(COSDictionary trailer, String headerVersion)
    {
        requireNotNullArg(trailer, "Trailer cannot be null");
        requireNotBlank(headerVersion, "Header version cannot be blank");
        this.trailer = trailer;
        this.headerVersion = headerVersion;
        ofNullable(this.trailer.getDictionaryObject(COSName.ROOT, COSDictionary.class))
                .ifPresent(d -> d.setItem(COSName.TYPE, COSName.CATALOG));
    }

    /**
     * Sets the version of the PDF specification to write to the file header. File header is defined in Chap 7.5.2 of
     * PDF 32000-1:2008
     * 
     * @param headerVersion
     */
    public void setHeaderVersion(String headerVersion)
    {
        requireNotBlank(headerVersion, "Header version cannot be null");
        this.headerVersion = headerVersion;
    }

    /**
     * @return the version of the PDF specification retrieved from the file header. File header is defined in Chap 7.5.2
     * of PDF 32000-1:2008
     */
    public String getHeaderVersion()
    {
        return headerVersion;
    }

    /**
     * @return true If this document has an encryption dictionary
     */
    public boolean isEncrypted()
    {
        return trailer.getDictionaryObject(COSName.ENCRYPT) != null;
    }

    /**
     * Get the encryption dictionary if the document is encrypted or null if the document is not encrypted.
     *
     * @return The encryption dictionary.
     */
    public COSDictionary getEncryptionDictionary()
    {
        return trailer.getDictionaryObject(COSName.ENCRYPT, COSDictionary.class);
    }

    /**
     * Set the encryption dictionary, this should only be called when encrypting the document.
     *
     * @param dictionary The encryption dictionary.
     */
    public void setEncryptionDictionary(COSDictionary dictionary)
    {
        trailer.setItem(COSName.ENCRYPT, dictionary);
    }

    public COSArray getDocumentID()
    {
        return trailer.getDictionaryObject(COSName.ID, COSArray.class);
    }

    public void setDocumentID(COSArray id)
    {
        trailer.setItem(COSName.ID, id);
    }

    /**
     * @return the catalog for this document
     * @throws IllegalStateException If no catalog can be found.
     */
    public COSDictionary getCatalog()
    {
        return Optional.ofNullable(trailer.getDictionaryObject(COSName.ROOT, COSDictionary.class))
                .orElseThrow(() -> new IllegalStateException("Catalog cannot be found"));
    }

    public COSDictionary getTrailer()
    {
        return trailer;
    }

    @Override
    public void accept(COSVisitor visitor) throws IOException
    {
        visitor.visit(this);
    }
}
