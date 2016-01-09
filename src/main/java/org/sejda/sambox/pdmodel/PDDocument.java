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
package org.sejda.sambox.pdmodel;

import static java.util.Optional.ofNullable;
import static org.sejda.io.CountingWritableByteChannel.from;
import static org.sejda.sambox.cos.DirectCOSObject.asDirectObject;
import static org.sejda.sambox.util.SpecVersionUtils.V1_4;
import static org.sejda.sambox.util.SpecVersionUtils.isAtLeast;
import static org.sejda.util.RequireUtils.requireNotBlank;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.WritableByteChannel;
import java.security.MessageDigest;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.sejda.io.CountingWritableByteChannel;
import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSDocument;
import org.sejda.sambox.cos.COSInteger;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSString;
import org.sejda.sambox.cos.DirectCOSObject;
import org.sejda.sambox.encryption.EncryptionContext;
import org.sejda.sambox.encryption.MessageDigests;
import org.sejda.sambox.encryption.StandardSecurity;
import org.sejda.sambox.output.PDDocumentWriter;
import org.sejda.sambox.output.WriteOption;
import org.sejda.sambox.pdmodel.common.PDStream;
import org.sejda.sambox.pdmodel.encryption.AccessPermission;
import org.sejda.sambox.pdmodel.encryption.PDEncryption;
import org.sejda.sambox.pdmodel.font.PDFont;
import org.sejda.sambox.util.Charsets;
import org.sejda.sambox.util.Version;
import org.sejda.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the in-memory representation of the PDF document.
 * 
 * @author Ben Litchfield
 */
public class PDDocument implements Closeable
{
    private static final Logger LOG = LoggerFactory.getLogger(PDDocument.class);

    private final COSDocument document;
    private PDDocumentCatalog documentCatalog;
    private AccessPermission permission;
    private boolean open = true;
    private OnClose onClose;
    private ResourceCache resourceCache = new DefaultResourceCache();

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
     * @param document The COSDocument that this document wraps.
     */
    public PDDocument(COSDocument document)
    {
        this(document, null);
    }

    /**
     * Constructor that uses an existing document. The COSDocument that is passed in must be valid.
     * 
     * @param document The COSDocument that this document wraps.
     * @param permission he access permissions of the pdf
     * 
     */
    public PDDocument(COSDocument document, AccessPermission permission)
    {
        this.document = document;
        this.permission = permission;
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
     */
    public PDPage importPage(PDPage page)
    {
        requireOpen();
        PDPage importedPage = new PDPage(page.getCOSObject().duplicate());
        InputStream in = null;
        try
        {
            in = page.getContents();
            if (in != null)
            {
                PDStream dest = new PDStream(page.getContents(), COSName.FLATE_DECODE);
                importedPage.setContents(dest);

            }
            addPage(importedPage);
        }
        catch (IOException e)
        {
            IOUtils.closeQuietly(in);
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
        COSDictionary infoDic = (COSDictionary) document.getTrailer()
                .getDictionaryObject(COSName.INFO);
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
     * This will get the encryption dictionary for this document.
     *
     * @return The encryption dictionary
     */
    public PDEncryption getEncryption()
    {
        if (isEncrypted())
        {
            return new PDEncryption(document.getEncryptionDictionary());
        }
        return new PDEncryption();
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
     * Returns the access permissions granted when the document was decrypted. If the document was not decrypted this
     * method returns the access permission for a document owner (ie can do everything). The returned object is in read
     * only mode so that permissions cannot be changed. Methods providing access to content should rely on this object
     * to verify if the current user is allowed to proceed.
     * 
     * @return the access permissions for the current user on the document.
     */
    public AccessPermission getCurrentAccessPermission()
    {
        return ofNullable(permission).orElseGet(AccessPermission::getOwnerAccessPermission);
    }

    /**
     * @return The version of the PDF specification to which the document conforms.
     */
    public String getVersion()
    {
        String headerVersion = getDocument().getHeaderVersion();
        if (isAtLeast(headerVersion, V1_4))
        {
            return ofNullable(getDocumentCatalog().getVersion())
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
     * 
     * @param md5Update
     * @param encContext
     */
    private void generateFileIdentifier(byte[] md5Update, Optional<EncryptionContext> encContext)
    {
        MessageDigest md5 = MessageDigests.md5();
        md5.update(Long.toString(System.currentTimeMillis()).getBytes(Charsets.ISO_8859_1));
        md5.update(md5Update);
        ofNullable(getDocument().getTrailer().getDictionaryObject(COSName.INFO))
                .map(d -> (COSDictionary) d).ifPresent(d -> {
                    for (COSBase current : d.getValues())
                    {
                        md5.update(current.toString().getBytes(Charsets.ISO_8859_1));
                    }
                });
        COSString retVal = COSString.newInstance(md5.digest());
        encContext.ifPresent(c -> c.documentId(retVal.getBytes()));
        retVal.setForceHexForm(true);
        retVal.encryptable(false);
        DirectCOSObject id = asDirectObject(retVal);
        getDocument().getTrailer().setItem(COSName.ID, asDirectObject(new COSArray(id, id)));
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
        writeTo(from(file), null, options);
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
        writeTo(from(filename), null, options);
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
        writeTo(from(channel), null, options);
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
        writeTo(from(out), null, options);
    }

    /**
     * Writes the document to the given {@link File} encrypting it using the given security. The document is closed once
     * written.
     * 
     * @see PDDocument#close()
     * @param file
     * @param security
     * @param options
     * @throws IOException
     */
    public void writeTo(File file, StandardSecurity security, WriteOption... options)
            throws IOException
    {
        writeTo(from(file), security, options);
    }

    /**
     * Writes the document to the file corresponding the given file name encrypting it using the given security. The
     * document is closed once written.
     * 
     * @see PDDocument#close()
     * @param filename
     * @param security
     * @param options
     * @throws IOException
     */
    public void writeTo(String filename, StandardSecurity security, WriteOption... options)
            throws IOException
    {
        writeTo(from(filename), security, options);
    }

    /**
     * Writes the document to the given {@link WritableByteChannel} encrypting it using the given security. The document
     * is closed once written.
     * 
     * @see PDDocument#close()
     * @param channel
     * @param security
     * @param options
     * @throws IOException
     */
    public void writeTo(WritableByteChannel channel, StandardSecurity security,
            WriteOption... options) throws IOException
    {
        writeTo(from(channel), security, options);
    }

    /**
     * Writes the document to the given {@link OutputStream} encrypting it using the given security. The document is
     * closed once written.
     * 
     * @see PDDocument#close()
     * @param out
     * @param security
     * @param options
     * @throws IOException
     */
    public void writeTo(OutputStream out, StandardSecurity security, WriteOption... options)
            throws IOException
    {
        writeTo(from(out), security, options);
    }

    private void writeTo(CountingWritableByteChannel output, StandardSecurity security,
            WriteOption... options) throws IOException
    {
        requireOpen();
        getDocumentInformation().setProducer("SAMBox " + Version.getVersion() + " (www.sejda.org)");
        getDocumentInformation().setModificationDate(Calendar.getInstance());
        for (PDFont font : fontsToSubset)
        {
            font.subset();
        }
        fontsToSubset.clear();
        Optional<EncryptionContext> encryptionContext = ofNullable(
                ofNullable(security).map(EncryptionContext::new).orElse(null));
        generateFileIdentifier(output.toString().getBytes(Charsets.ISO_8859_1), encryptionContext);
        try (PDDocumentWriter writer = new PDDocumentWriter(output, encryptionContext, options))
        {
            writer.write(this);
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
        this.resourceCache.clear();
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

    /**
     * Returns the resource cache associated with this document, or null if there is none.
     */
    public ResourceCache getResourceCache()
    {
        return resourceCache;
    }

}
