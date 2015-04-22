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
package org.apache.pdfbox.pdmodel;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.cos.COSInteger;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdfwriter.COSWriter;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.PDEncryption;
import org.apache.pdfbox.pdmodel.encryption.ProtectionPolicy;
import org.apache.pdfbox.pdmodel.encryption.SecurityHandler;
import org.apache.pdfbox.pdmodel.encryption.SecurityHandlerFactory;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField;

/**
 * This is the in-memory representation of the PDF document. The #close() method must be called once the document is no
 * longer needed.
 * 
 * @author Ben Litchfield
 */
public class PDDocument implements Closeable
{
    private static final Log LOG = LogFactory.getLog(PDDocument.class);

    private final COSDocument document;
    private PDDocumentCatalog documentCatalog;

    // the encryption will be cached here. When the document is decrypted then
    // the COSDocument will not have an "Encrypt" dictionary anymore and this object must be used
    private PDEncryption encryption;

    // holds a flag which tells us if we should remove all security from this documents.
    private boolean allSecurityToBeRemoved;

    private AccessPermission accessPermission;

    // fonts to subset before saving
    private final Set<PDFont> fontsToSubset = new HashSet<PDFont>();

    /**
     * Creates an empty PDF document. You need to add at least one page for the document to be valid.
     */
    public PDDocument()
    {
        document = new COSDocument();
        document.getCatalog().setItem(COSName.VERSION, COSName.getPDFName("1.4"));
        COSDictionary pages = new COSDictionary();
        document.getCatalog().setItem(COSName.PAGES, pages);
        pages.setItem(COSName.TYPE, COSName.PAGES);
        COSArray kidsArray = new COSArray();
        pages.setItem(COSName.KIDS, kidsArray);
        pages.setItem(COSName.COUNT, COSInteger.ZERO);
    }

    /**
     * Constructor that uses an existing document. The COSDocument that is passed in must be valid.
     * 
     * @param doc The COSDocument that this document wraps.
     */
    public PDDocument(COSDocument doc)
    {
        this(doc, null);
    }

    /**
     * Constructor that uses an existing document. The COSDocument that is passed in must be valid.
     * 
     * @param doc The COSDocument that this document wraps.
     * @param permission he access permissions of the pdf
     * 
     */
    public PDDocument(COSDocument doc, AccessPermission permission)
    {
        document = doc;
        accessPermission = permission;
    }

    /**
     * This will add a page to the document. This is a convenience method, that will add the page to the root of the
     * hierarchy and set the parent of the page to the root.
     * 
     * @param page The page to add to the document.
     */
    public void addPage(PDPage page)
    {
        getPages().add(page);
    }

    /**
     * Remove the page from the document.
     * 
     * @param page The page to remove from the document.
     */
    public void removePage(PDPage page)
    {
        getPages().remove(page);
    }

    /**
     * Remove the page from the document.
     * 
     * @param pageNumber 0 based index to page number.
     */
    public void removePage(int pageNumber)
    {
        getPages().remove(pageNumber);
    }

    /**
     * This will import and copy the contents from another location. Currently the content stream is stored in a scratch
     * file. The scratch file is associated with the document. If you are adding a page to this document from another
     * document and want to copy the contents to this document's scratch file then use this method otherwise just use
     * the addPage method.
     * 
     * @param page The page to import.
     * @return The page that was imported.
     * 
     * @throws IOException If there is an error copying the page.
     */
    public PDPage importPage(PDPage page) throws IOException
    {
        PDPage importedPage = new PDPage(new COSDictionary(page.getCOSObject()));
        InputStream is = null;
        OutputStream os = null;
        try
        {
            PDStream src = page.getStream();
            if (src != null)
            {
                PDStream dest = new PDStream(new COSStream());
                dest.addCompression();
                importedPage.setContents(dest);
                is = src.createInputStream();
                os = dest.createOutputStream();
                IOUtils.copy(is, os);
            }
            addPage(importedPage);
        }
        finally
        {
            IOUtils.close(is);
            IOUtils.close(os);
        }
        return importedPage;
    }

    /**
     * @return The document that this layer sits on top of.
     */
    public COSDocument getDocument()
    {
        return document;
    }

    /**
     * This will get the document info dictionary. This is guaranteed to not return null.
     * 
     * @return The documents /Info dictionary
     */
    public PDDocumentInformation getDocumentInformation()
    {
        COSDictionary infoDic = (COSDictionary) document.getTrailer().getDictionaryObject(
                COSName.INFO);
        if (infoDic == null)
        {
            infoDic = new COSDictionary();
            document.getTrailer().setItem(COSName.INFO, infoDic);
        }
        return new PDDocumentInformation(infoDic);
    }

    /**
     * This will set the document information for this document.
     * 
     * @param info The updated document information.
     */
    public void setDocumentInformation(PDDocumentInformation documentInformation)
    {
        document.getTrailer().setItem(COSName.INFO, documentInformation.getCOSObject());
    }

    /**
     * This will get the document CATALOG. This is guaranteed to not return null.
     * 
     * @return The documents /Root dictionary
     */
    public PDDocumentCatalog getDocumentCatalog()
    {
        if (documentCatalog == null)
        {
            documentCatalog = new PDDocumentCatalog(this, document.getCatalog());
        }
        return documentCatalog;
    }

    /**
     * @return true If this document is encrypted.
     */
    public boolean isEncrypted()
    {
        return document.isEncrypted();
    }

    /**
     * This will get the encryption dictionary for this document. This will still return the parameters if the document
     * was decrypted. As the encryption architecture in PDF documents is plugable this returns an abstract class, but
     * the only supported subclass at this time is a PDStandardEncryption object.
     *
     * @return The encryption dictionary(most likely a PDStandardEncryption object)
     */
    public PDEncryption getEncryption()
    {
        if (encryption == null && isEncrypted())
        {
            encryption = new PDEncryption(document.getEncryptionDictionary());
        }
        return encryption;
    }

    /**
     * This will set the encryption dictionary for this document.
     * 
     * @param encryption The encryption dictionary(most likely a PDStandardEncryption object)
     * 
     * @throws IOException If there is an error determining which security handler to use.
     */
    public void setEncryptionDictionary(PDEncryption encryption) throws IOException
    {
        this.encryption = encryption;
    }

    /**
     * This will return the last signature.
     * 
     * @return the last signature as <code>PDSignatureField</code>.
     * @throws IOException if no document catalog can be found.
     */
    public PDSignature getLastSignatureDictionary() throws IOException
    {
        List<PDSignature> signatureDictionaries = getSignatureDictionaries();
        int size = signatureDictionaries.size();
        if (size > 0)
        {
            return signatureDictionaries.get(size - 1);
        }
        return null;
    }

    /**
     * Retrieve all signature fields from the document.
     * 
     * @return a <code>List</code> of <code>PDSignatureField</code>s
     * @throws IOException if no document catalog can be found.
     */
    public List<PDSignatureField> getSignatureFields() throws IOException
    {
        List<PDSignatureField> fields = new LinkedList<PDSignatureField>();
        PDAcroForm acroForm = getDocumentCatalog().getAcroForm();
        if (acroForm != null)
        {
            List<COSDictionary> signatureDictionary = document.getSignatureFields(false);
            for (COSDictionary dict : signatureDictionary)
            {
                fields.add(new PDSignatureField(acroForm, dict, null));
            }
        }
        return fields;
    }

    /**
     * Retrieve all signature dictionaries from the document.
     * 
     * @return a <code>List</code> of <code>PDSignatureField</code>s
     * @throws IOException if no document catalog can be found.
     */
    public List<PDSignature> getSignatureDictionaries() throws IOException
    {
        List<COSDictionary> signatureDictionary = document.getSignatureDictionaries();
        List<PDSignature> signatures = new LinkedList<PDSignature>();
        for (COSDictionary dict : signatureDictionary)
        {
            signatures.add(new PDSignature(dict));
        }
        return signatures;
    }

    /**
     * @return the list of fonts which will be subset before the document is saved.
     */
    Set<PDFont> getFontsToSubset()
    {
        return fontsToSubset;
    }

    /**
     * Save the document to a file.
     * 
     * @param fileName The file to save as.
     * @throws IOException if the output could not be written
     */
    public void save(String fileName) throws IOException
    {
        save(new File(fileName));
    }

    /**
     * Save the document to a file.
     * 
     * @param file The file to save as.
     * @throws IOException if the output could not be written
     */
    public void save(File file) throws IOException
    {
        save(new FileOutputStream(file));
    }

    /**
     * This will save the document to an output stream.
     * 
     * @param output The stream to write to.
     * @throws IOException if the output could not be written
     */
    public void save(OutputStream output) throws IOException
    {
        // subset designated fonts
        for (PDFont font : fontsToSubset)
        {
            font.subset();
        }
        fontsToSubset.clear();

        // save PDF
        COSWriter writer = new COSWriter(output);
        try
        {
            writer.write(this);
            writer.close();
        }
        finally
        {
            writer.close();
        }
    }

    /**
     * @param pageIndex the page index
     * @return the page at the given zero based index.
     */
    public PDPage getPage(int pageIndex)
    {
        return getDocumentCatalog().getPages().get(pageIndex);
    }

    public PDPageTree getPages()
    {
        return getDocumentCatalog().getPages();
    }

    /**
     * @return The total number of pages in the PDF document.
     */
    public int getNumberOfPages()
    {
        return getDocumentCatalog().getPages().getCount();
    }

    /**
     * This will close the underlying COSDocument object.
     * 
     * @throws IOException If there is an error releasing resources.
     */
    @Override
    public void close() throws IOException
    {
        // TODO
    }

    /**
     * Protects the document with the protection policy pp. The document content will be really encrypted when it will
     * be saved. This method only marks the document for encryption.
     *
     * @see org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy
     * @see org.apache.pdfbox.pdmodel.encryption.PublicKeyProtectionPolicy
     * 
     * @param policy The protection policy.
     * 
     * @throws IOException if there isn't any suitable security handler.
     */
    public void protect(ProtectionPolicy policy) throws IOException
    {
        if (!isEncrypted())
        {
            encryption = new PDEncryption();
        }

        SecurityHandler securityHandler = SecurityHandlerFactory.INSTANCE
                .newSecurityHandlerForPolicy(policy);
        if (securityHandler == null)
        {
            throw new IOException("No security handler for policy " + policy);
        }

        getEncryption().setSecurityHandler(securityHandler);
    }

    /**
     * Returns the access permissions granted when the document was decrypted. If the document was not decrypted this
     * method returns the access permission for a document owner (ie can do everything). The returned object is in read
     * only mode so that permissions cannot be changed. Methods providing access to content should rely on this object
     * to verify if the current user is allowed to proceed.
     * 
     * @return the access permissions for the current user on the document.
     */
    public AccessPermission getCurrentAccessPermission()
    {
        if (accessPermission == null)
        {
            accessPermission = AccessPermission.getOwnerAccessPermission();
        }
        return accessPermission;
    }

    /**
     * Indicates if all security is removed or not when writing the pdf.
     * 
     * @return returns true if all security shall be removed otherwise false
     */
    public boolean isAllSecurityToBeRemoved()
    {
        return allSecurityToBeRemoved;
    }

    /**
     * Activates/Deactivates the removal of all security when writing the pdf.
     * 
     * @param removeAllSecurity remove all security if set to true
     */
    public void setAllSecurityToBeRemoved(boolean removeAllSecurity)
    {
        allSecurityToBeRemoved = removeAllSecurity;
    }

    /**
     * @return the PDF specification version this document conforms to (e.g. 1.4f)
     */
    public float getVersion()
    {
        float headerVersionFloat = getDocument().getHeaderVersion();
        // there may be a second version information in the document catalog starting with 1.4
        if (headerVersionFloat >= 1.4f)
        {
            String catalogVersion = getDocumentCatalog().getVersion();
            if (catalogVersion != null)
            {
                try
                {
                    return Math.max(Float.parseFloat(catalogVersion), headerVersionFloat);
                }
                catch (NumberFormatException e)
                {
                    LOG.error("Can't extract the version number of the document catalog.", e);
                }
            }
        }
        return headerVersionFloat;
    }

    /**
     * Sets the PDF specification version for this document.
     *
     * @param newVersion the new PDF version (e.g. 1.4f)
     * 
     */
    public void setVersion(float newVersion)
    {
        float currentVersion = getVersion();
        // nothing to do?
        if (newVersion == currentVersion)
        {
            return;
        }
        // the version can't be downgraded
        if (newVersion < currentVersion)
        {
            LOG.error("It's not allowed to downgrade the version of a pdf.");
            return;
        }
        // update the catalog version if the document version is >= 1.4
        if (getDocument().getHeaderVersion() >= 1.4f)
        {
            getDocumentCatalog().setVersion(Float.toString(newVersion));
        }
        else
        {
            // versions < 1.4f have a version header only
            getDocument().setHeaderVersion(newVersion);
        }
    }

    /**
     * Parses PDF with non sequential parser.
     * 
     * @param file file to be loaded
     * 
     * @return loaded document
     * 
     * @throws IOException in case of a file reading or parsing error
     */
    public static PDDocument load(File file) throws IOException
    {
        return load(file, "");
    }

    /**
     * Parses PDF with non sequential parser.
     * 
     * @param file file to be loaded
     * @param password password to be used for decryption
     * @param useScratchFiles enables the usage of a scratch file if set to true
     * 
     * @return loaded document
     * 
     * @throws IOException in case of a file reading or parsing error
     */
    public static PDDocument load(File file, String password) throws IOException
    {
        return load(file, password, null, null);
    }

    /**
     * Parses PDF with non sequential parser.
     * 
     * @param file file to be loaded
     * @param password password to be used for decryption
     * @param keyStore key store to be used for decryption when using public key security
     * @param alias alias to be used for decryption when using public key security
     * @param useScratchFiles enables the usage of a scratch file if set to true
     * 
     * @return loaded document
     * 
     * @throws IOException in case of a file reading or parsing error
     */
    public static PDDocument load(File file, String password, InputStream keyStore, String alias)
            throws IOException
    {
        PDFParser parser = new PDFParser(file, password, keyStore, alias, false);
        parser.parse();
        PDDocument doc = parser.getPDDocument();
        return doc;
    }

    /**
     * Parses PDF with non sequential parser.
     * 
     * @param input stream that contains the document.
     * 
     * @return loaded document
     * 
     * @throws IOException in case of a file reading or parsing error
     */
    public static PDDocument load(InputStream input) throws IOException
    {
        return load(input, "", null, null);
    }

    /**
     * Parses PDF with non sequential parser.
     * 
     * @param input stream that contains the document.
     * @param password password to be used for decryption
     * 
     * @return loaded document
     * 
     * @throws IOException in case of a file reading or parsing error
     */
    public static PDDocument load(InputStream input, String password) throws IOException
    {
        return load(input, password, null, null);
    }

    /**
     * Parses PDF with non sequential parser.
     * 
     * @param input stream that contains the document.
     * @param password password to be used for decryption
     * @param keyStore key store to be used for decryption when using public key security
     * @param alias alias to be used for decryption when using public key security
     * @param useScratchFiles enables the usage of a scratch file if set to true
     * 
     * @return loaded document
     * 
     * @throws IOException in case of a file reading or parsing error
     */
    public static PDDocument load(InputStream input, String password, InputStream keyStore,
            String alias) throws IOException
    {
        PDFParser parser = new PDFParser(input, password, keyStore, alias, false);
        parser.parse();
        return parser.getPDDocument();
    }
}
