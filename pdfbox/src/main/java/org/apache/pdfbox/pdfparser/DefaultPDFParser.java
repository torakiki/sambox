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
package org.apache.pdfbox.pdfparser;

import static java.util.Objects.requireNonNull;
import static org.apache.pdfbox.util.RequireUtils.requireIOCondition;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.cos.BaseCOSParser;
import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.io.PushBackInputStream;
import org.apache.pdfbox.io.RandomAccessBufferedFileInputStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.DecryptionMaterial;
import org.apache.pdfbox.pdmodel.encryption.PDEncryption;
import org.apache.pdfbox.pdmodel.encryption.PublicKeyDecryptionMaterial;
import org.apache.pdfbox.pdmodel.encryption.SecurityHandler;
import org.apache.pdfbox.pdmodel.encryption.StandardDecryptionMaterial;
import org.apache.pdfbox.xref.XrefParser;

/**
 * @author Andrea Vacondio
 *
 */
public class DefaultPDFParser
{
    private static final Log LOG = LogFactory.getLog(DefaultPDFParser.class);

    private static final String PDF_HEADER = "%PDF-";
    private static final int EXPECTED_HEADER_LENGTH = 8;

    /**
     * Parses the given {@link File} returning the corresponding {@link PDDocument}.
     * 
     * @param file
     * @return the parsed document
     * @throws IOException
     */
    public static PDDocument parse(File file) throws IOException
    {
        return parse(file, null, null, null);
    }

    /**
     * Parses the given {@link File} using the provided password returning the corresponding decrypted
     * {@link PDDocument}.
     * 
     * @param file
     * @param password the password to decrypt the document
     * @return the parsed document
     * @throws IOException
     */
    public static PDDocument parse(File file, String password) throws IOException
    {
        return parse(file, password, null, null);
    }

    /**
     * Parses the given {@link File} using the given keystore and password, returning the corresponding decrypted
     * {@link PDDocument}.
     * 
     * @param file
     * @param password password to be used for decryption.
     * @param keyStore key store to be used for decryption when using public key security
     * @param keyAlias alias to be used for decryption when using public key security
     * @return the parsed document
     * @throws IOException
     */
    public static PDDocument parse(File file, String password, InputStream keyStore, String keyAlias)
            throws IOException
    {
        requireNonNull(file);
        BaseCOSParser parser = new BaseCOSParser(new PushBackInputStream(
                new RandomAccessBufferedFileInputStream(file), 4096));
        parser.length(file.length());
        float headerVersion = readHeader(parser);
        XrefParser xrefParser = new XrefParser(parser);
        xrefParser.parse();
        COSDocument document = new COSDocument(xrefParser.getTrailer(), headerVersion);
        if (document.isEncrypted())
        {
            LOG.debug("Preparing for document decryption");
            DecryptionMaterial decryptionMaterial = getDecryptionMaterial(password, keyStore,
                    keyAlias);
            PDEncryption encryption = new PDEncryption(document.getEncryptionDictionary());

            SecurityHandler securityHandler = encryption.getSecurityHandler();
            securityHandler.prepareForDecryption(encryption, document.getDocumentID(),
                    decryptionMaterial);
            parser.provider().decryptWith(securityHandler);
            return new PDDocument(document, securityHandler.getCurrentAccessPermission());
        }
        return new PDDocument(document);
    }

    private static float readHeader(BaseCOSParser parser) throws IOException
    {
        parser.offset(0);
        int headerIndex = -1;
        String header = parser.readLine();
        while ((headerIndex = header.indexOf(PDF_HEADER)) < 0)
        {
            // if a line starts with a digit, it has to be the first one with data in it
            if ((header.length() > 0) && (Character.isDigit(header.charAt(0))))
            {
                throw new IOException("Unable to find expected file header");
            }
            header = parser.readLine();
        }

        header = header.substring(headerIndex, header.length());
        requireIOCondition(header.length() >= EXPECTED_HEADER_LENGTH,
                "Unable to find expected header '%PDF-n.n'");
        try
        {
            LOG.debug("Found header " + header);
            return Float.valueOf(header.substring(EXPECTED_HEADER_LENGTH - 1,
                    EXPECTED_HEADER_LENGTH));
        }
        catch (NumberFormatException exception)
        {
            throw new IOException("Unable to get header version from " + header);
        }
    }

    private static DecryptionMaterial getDecryptionMaterial(String password, InputStream keyStore,
            String keyAlias) throws IOException
    {
        if (keyStore != null)
        {
            try
            {
                KeyStore ks = KeyStore.getInstance("PKCS12");
                ks.load(keyStore, password.toCharArray());
                return new PublicKeyDecryptionMaterial(ks, keyAlias, password);
            }
            catch (KeyStoreException | NoSuchAlgorithmException | CertificateException e)
            {
                throw new IOException(e);
            }
        }
        return new StandardDecryptionMaterial(password);
    }
}
