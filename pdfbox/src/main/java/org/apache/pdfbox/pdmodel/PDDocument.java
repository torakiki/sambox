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

import static org.apache.pdfbox.cos.DirectCOSObject.asDirectObject;
import static org.apache.pdfbox.output.CountingWritableByteChannel.from;
import static org.apache.pdfbox.util.RequireUtils.requireNotBlank;
import static org.apache.pdfbox.util.SpecVersionUtils.V1_4;
import static org.apache.pdfbox.util.SpecVersionUtils.isAtLeast;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.WritableByteChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.cos.COSInteger;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.cos.DirectCOSObject;
import org.apache.pdfbox.output.CountingWritableByteChannel;
import org.apache.pdfbox.output.PDDocumentWriter;
import org.apache.pdfbox.output.WriteOption;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.PDEncryption;
import org.apache.pdfbox.pdmodel.encryption.ProtectionPolicy;
import org.apache.pdfbox.pdmodel.encryption.SecurityHandler;
import org.apache.pdfbox.pdmodel.encryption.SecurityHandlerFactory;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.util.Charsets;
import org.apache.pdfbox.util.IOUtils;

/**
 * This is the in-memory representation of the PDF document.
 * 
 * @author Ben Litchfield
 */
public class PDDocument implements Closeable
{
    private static final Log LOG = LogFactory.getLog(PDDocument.class);

    private final COSDocument document;
    private PDDocumentCatalog documentCatalog;
    private PDEncryption encryption;
    private AccessPermission accessPermission;
    private boolean open = true;
    private OnClose onClose;

    // fonts to subset before saving
    private final Set<PDFont> fontsToSubset = new HashSet<>();

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
        requireOpen();
        getPages().add(page);
    }

    /**
     * Remove the page from the document.
     * 
     * @param page The page to remove from the document.
     */
    public void removePage(PDPage page)
    {
        requireOpen();
        getPages().remove(page);
    }

    /**
     * Remove the page from the document.
     * 
     * @param pageNumber 0 based index to page number.
     */
    public void removePage(int pageNumber)
    {
        requireOpen();
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
        requireOpen();
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
        requireOpen();
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
     * @param encryption The encryption
     */
    public void setEncryption(PDEncryption encryption)
    {
        requireOpen();
        this.encryption = encryption;
    }

    /**
     * @param removes any encryption configuration and encryption dictionary
     */
    public void removeEncryption()
    {
        requireOpen();
        this.encryption = null;
        this.getDocument().getTrailer().removeItem(COSName.ENCRYPT);
    }

    /**
     * @return the list of fonts which will be subset before the document is saved.
     */
    Set<PDFont> getFontsToSubset()
    {
        return fontsToSubset;
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
        requireOpen();
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
     * @return The version of the PDF specification to which the document conforms.
     */
    public String getVersion()
    {
        String headerVersion = getDocument().getHeaderVersion();
        if (isAtLeast(headerVersion, V1_4))
        {
            return Optional.ofNullable(getDocumentCatalog().getVersion())
                    .filter(catalogVersion -> (catalogVersion.compareTo(headerVersion) > 0))
                    .orElse(headerVersion);
        }
        return headerVersion;
    }

    /**
     * Sets the version of the PDF specification to which the document conforms. Downgrading of the document version is
     * not allowed.
     *
     * @param newVersion the new PDF version
     * 
     */
    public void setVersion(String newVersion)
    {
        requireOpen();
        requireNotBlank(newVersion, "Spec version cannot be blank");
        int compare = getVersion().compareTo(newVersion);
        if (compare > 0)
        {
            LOG.warn("Spec version downgrade not allowed");
        }
        else if (compare < 0)
        {
            if (isAtLeast(newVersion, V1_4))
            {
                getDocumentCatalog().setVersion(newVersion);
            }
            getDocument().setHeaderVersion(newVersion);
        }
    }

    /**
     * If the document is not at the given version or above, it sets the version of the PDF specification to which the
     * document conforms.
     * 
     * @param version
     */
    public void requireMinVersion(String version)
    {
        if (!isAtLeast(getVersion(), version))
        {
            setVersion(version);
        }
    }

    /**
     * Sets an action to be performed right before this {@link PDDocument} is closed.
     * 
     * @param onClose
     */
    public void setOnCloseAction(OnClose onClose)
    {
        requireOpen();
        this.onClose = onClose;
    }

    private void requireOpen() throws IllegalStateException
    {
        if (!isOpen())
        {
            throw new IllegalStateException("The document is closed");
        }
    }

    /**
     * Generates file identifier as defined in the chap 14.4 PDF 32000-1:2008 and sets it as ID value of the document
     * trailer.
     */
    private void generateFileIdentifier()
    {
        try
        {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(Long.toString(System.currentTimeMillis()).getBytes(Charsets.ISO_8859_1));
            Optional.ofNullable(getDocument().getTrailer().getDictionaryObject(COSName.INFO))
                    .map(d -> (COSDictionary) d).ifPresent(d -> {
                        for (COSBase current : d.getValues())
                        {
                            md5.update(current.toString().getBytes(Charsets.ISO_8859_1));
                        }
                    });
            COSString retVal = COSString.newInstance(md5.digest());
            retVal.setForceHexForm(true);
            DirectCOSObject id = asDirectObject(retVal);
            getDocument().getTrailer().setItem(COSName.ID, asDirectObject(new COSArray(id, id)));
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Writes the document to the given {@link File}. The document is closed once written.
     * 
     * @see PDDocument#close()
     * @param file
     * @param options
     * @throws IOException
     */
    public void writeTo(File file, WriteOption... options) throws IOException
    {
        writeTo(from(file), options);
    }

    /**
     * Writes the document to the file corresponding the given file name. The document is closed once written.
     * 
     * @see PDDocument#close()
     * @param filename
     * @param options
     * @throws IOException
     */
    public void writeTo(String filename, WriteOption... options) throws IOException
    {
        writeTo(from(filename), options);
    }

    /**
     * Writes the document to the given {@link WritableByteChannel}. The document is closed once written.
     * 
     * @see PDDocument#close()
     * @param channel
     * @param options
     * @throws IOException
     */
    public void writeTo(WritableByteChannel channel, WriteOption... options) throws IOException
    {
        writeTo(from(channel), options);
    }

    /**
     * Writes the document to the given {@link OutputStream}. The document is closed once written.
     * 
     * @see PDDocument#close()
     * @param out
     * @param options
     * @throws IOException
     */
    public void writeTo(OutputStream out, WriteOption... options) throws IOException
    {
        writeTo(from(out), options);
    }

    private void writeTo(CountingWritableByteChannel output, WriteOption... options)
            throws IOException
    {
        requireOpen();
        for (PDFont font : fontsToSubset)
        {
            font.subset();
        }
        fontsToSubset.clear();
        generateFileIdentifier();
        try (PDDocumentWriter writer = new PDDocumentWriter(output))
        {
            writer.write(this, options);
        }
        finally
        {
            IOUtils.close(this);
        }
    }

    /**
     * @return true if the {@link PDDocument} is open
     */
    public boolean isOpen()
    {
        return this.open;
    }

    /**
     * Closes the {@link PDDocument} executing an onClose action is set. Once closed the document is pretty much
     * unusable since most of the mothods requires an open document.
     * 
     * @see PDDocument#setOnCloseAction(OnClose)
     */
    @Override
    public void close() throws IOException
    {
        if (onClose != null)
        {
            onClose.onClose();
        }
        this.open = false;
    }

    /**
     * Action to be performed before the {@link PDDocument} is close
     * 
     * @author Andrea Vacondio
     */
    @FunctionalInterface
    public static interface OnClose
    {
        /**
         * Sets an action to be performed right before this {@link PDDocument} is closed.
         * 
         * @param onClose
         */
        void onClose() throws IOException;
    }
}
