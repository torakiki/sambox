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

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.sejda.commons.util.RequireUtils.requireNotBlank;
import static org.sejda.io.CountingWritableByteChannel.from;
import static org.sejda.sambox.cos.DirectCOSObject.asDirectObject;
import static org.sejda.sambox.util.SpecVersionUtils.V1_4;
import static org.sejda.sambox.util.SpecVersionUtils.isAtLeast;

import java.awt.Point;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.apache.fontbox.ttf.TrueTypeFont;
import org.apache.xmpbox.XMPMetadata;
import org.apache.xmpbox.schema.AdobePDFSchema;
import org.apache.xmpbox.xml.DomXmpParser;
import org.apache.xmpbox.xml.XmpParsingException;
import org.apache.xmpbox.xml.XmpSerializer;
import org.sejda.commons.util.IOUtils;
import org.sejda.io.CountingWritableByteChannel;
import org.sejda.io.SeekableSources;
import org.sejda.sambox.SAMBox;
import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSDocument;
import org.sejda.sambox.cos.COSInteger;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSNumber;
import org.sejda.sambox.cos.COSString;
import org.sejda.sambox.cos.DirectCOSObject;
import org.sejda.sambox.encryption.EncryptionContext;
import org.sejda.sambox.encryption.MessageDigests;
import org.sejda.sambox.encryption.StandardSecurity;
import org.sejda.sambox.input.PDFParser;
import org.sejda.sambox.output.PDDocumentWriter;
import org.sejda.sambox.output.PreSaveCOSTransformer;
import org.sejda.sambox.output.WriteOption;
import org.sejda.sambox.pdmodel.common.PDMetadata;
import org.sejda.sambox.pdmodel.encryption.AccessPermission;
import org.sejda.sambox.pdmodel.encryption.PDEncryption;
import org.sejda.sambox.pdmodel.encryption.SecurityHandler;
import org.sejda.sambox.pdmodel.font.Subsettable;
import org.sejda.sambox.pdmodel.graphics.color.PDDeviceRGB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.xml.transform.TransformerException;

/**
 * This is the in-memory representation of the PDF document.
 *
 * @author Ben Litchfield
 */
public class PDDocument implements Closeable
{
    private static final Logger LOG = LoggerFactory.getLogger(PDDocument.class);

    /**
     * avoid concurrency issues with PDDeviceRGB and deadlock in COSNumber/COSInteger
     */
    static
    {
        try
        {
            PDDeviceRGB.INSTANCE.toRGBImage(
                    Raster.createBandedRaster(DataBuffer.TYPE_BYTE, 1, 1, 3, new Point(0, 0)));
        }
        catch (IOException e)
        {
            LOG.warn("This shouldn't happen", e);
        }
        try
        {
            // TODO remove this and deprecated COSNumber statics in 3.0
            COSNumber.get("0");
            COSNumber.get("1");
        }
        catch (IOException ex)
        {
            //
        }
    }

    private final COSDocument document;
    private PDDocumentCatalog documentCatalog;
    private SecurityHandler securityHandler;
    private boolean open = true;
    private OnClose onClose = () -> LOG.debug("Closing document");
    private OnBeforeWrite onBeforeWrite = () -> LOG.trace("About to write document");
    private PreSaveCOSTransformer preSaveCOSTransformer;
    private ResourceCache resourceCache = new DefaultResourceCache();

    // fonts to subset before saving
    private final Set<Subsettable> fontsToSubset = new HashSet<>();

    public PDDocument()
    {
        document = new COSDocument();
        document.getCatalog().setItem(COSName.VERSION, COSName.getPDFName("1.4"));
        COSDictionary pages = new COSDictionary();
        document.getCatalog().setItem(COSName.PAGES, pages);
        pages.setItem(COSName.TYPE, COSName.PAGES);
        pages.setItem(COSName.KIDS, new COSArray());
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
     */
    public PDDocument(COSDocument document, SecurityHandler securityHandler)
    {
        this.document = document;
        this.securityHandler = securityHandler;
    }

    /**
     * This will add a page to the document. This is a convenience method, that will add the page to
     * the root of the hierarchy and set the parent of the page to the root.
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
        COSDictionary infoDic = document.getTrailer().getCOSObject()
                .getDictionaryObject(COSName.INFO, COSDictionary.class);
        if (infoDic == null)
        {
            infoDic = new COSDictionary();
            document.getTrailer().getCOSObject().setItem(COSName.INFO, infoDic);
        }
        return new PDDocumentInformation(infoDic);
    }

    /**
     * This will set the document information for this document.
     *
     * @param documentInformation The updated document information.
     */
    public void setDocumentInformation(PDDocumentInformation documentInformation)
    {
        requireOpen();
        document.getTrailer().getCOSObject()
                .setItem(COSName.INFO, documentInformation.getCOSObject());
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
     * For internal PDFBox use when creating PDF documents: register a TrueTypeFont to make sure it
     * is closed when the PDDocument is closed to avoid memory leaks. Users don't have to call this
     * method, it is done by the appropriate PDFont classes.
     */
    public void registerTrueTypeFontForClosing(TrueTypeFont ttf)
    {
        addOnCloseAction(() -> IOUtils.closeQuietly(ttf));
    }

    /**
     * @return the {@link Set} of fonts which will be subset before the document is saved.
     */
    public Set<Subsettable> getFontsToSubset()
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
     * Returns the access permissions granted when the document was decrypted. If the document was
     * not decrypted this method returns the access permission for a document owner (ie can do
     * everything). The returned object is in read only mode so that permissions cannot be changed.
     * Methods providing access to content should rely on this object to verify if the current user
     * is allowed to proceed.
     *
     * @return the access permissions for the current user on the document.
     */
    public AccessPermission getCurrentAccessPermission()
    {
        return ofNullable(securityHandler).map(SecurityHandler::getCurrentAccessPermission)
                .orElseGet(AccessPermission::getOwnerAccessPermission);
    }

    public SecurityHandler getSecurityHandler()
    {
        return securityHandler;
    }

    /**
     * @return The version of the PDF specification to which the document conforms.
     */
    public String getVersion()
    {
        String headerVersion = getDocument().getHeaderVersion();
        if (isAtLeast(headerVersion, V1_4))
        {
            return ofNullable(getDocumentCatalog().getVersion()).filter(
                            catalogVersion -> (catalogVersion.compareTo(headerVersion) > 0))
                    .orElse(headerVersion);
        }
        return headerVersion;
    }

    /**
     * Sets the version of the PDF specification to which the document conforms. Downgrading of the
     * document version is not allowed.
     *
     * @param newVersion the new PDF version
     */
    public void setVersion(String newVersion)
    {
        requireOpen();
        requireNotBlank(newVersion, "Spec version cannot be blank");
        int compare = getVersion().compareTo(newVersion);
        if (compare > 0)
        {
            LOG.info("Spec version downgrade not allowed");
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
     * If the document is not at the given version or above, it sets the version of the PDF
     * specification to which the document conforms.
     */
    public void requireMinVersion(String version)
    {
        if (!isAtLeast(getVersion(), version))
        {
            LOG.debug("Minimum spec version required is {}", version);
            setVersion(version);
        }
    }

    /**
     * Deprecated since the name suggests a setter while we actually have {@link OnClose} actions
     * composition (we don't set the action, we add it to existing actions).
     */
    @Deprecated
    public void setOnCloseAction(OnClose onClose)
    {
        addOnCloseAction(onClose);
    }

    /**
     * Adds the given {@link OnClose} to the set of actions to be executed right before this
     * {@link PDDocument} is closed.
     */
    public void addOnCloseAction(OnClose onClose)
    {
        requireOpen();
        this.onClose = onClose.andThen(this.onClose);
    }

    public void setOnBeforeWriteAction(OnBeforeWrite onBeforeWrite)
    {
        requireOpen();
        this.onBeforeWrite = onBeforeWrite.andThen(this.onBeforeWrite);
    }

    private void requireOpen() throws IllegalStateException
    {
        if (!isOpen())
        {
            throw new IllegalStateException("The document is closed");
        }
    }

    /**
     * Generates file identifier as defined in the chap 14.4 PDF 32000-1:2008 and sets it as first
     * and second value for the ID array in the document trailer.
     */
    private void generateFileIdentifier(byte[] md5Update, EncryptionContext encContext)
    {
        COSString id = generateFileIdentifier(md5Update);
        if (nonNull(encContext))
        {
            encContext.documentId(id.getBytes());
        }
        DirectCOSObject directId = asDirectObject(id);
        getDocument().getTrailer().getCOSObject()
                .setItem(COSName.ID, asDirectObject(new COSArray(directId, directId)));
    }

    /**
     * @return a newly generated ID based on the input bytes, current timestamp and some other
     * information, to be used as value of the ID array in the document trailer.
     */
    public COSString generateFileIdentifier(byte[] md5Update)
    {
        MessageDigest md5 = MessageDigests.md5();
        md5.update(Long.toString(System.currentTimeMillis()).getBytes(StandardCharsets.ISO_8859_1));
        md5.update(md5Update);
        ofNullable(getDocument().getTrailer().getCOSObject()
                .getDictionaryObject(COSName.INFO, COSDictionary.class)).ifPresent(d -> {
            for (COSBase current : d.getValues())
            {
                md5.update(current.toString().getBytes(StandardCharsets.ISO_8859_1));
            }
        });
        COSString retVal = COSString.newInstance(md5.digest());
        retVal.setForceHexForm(true);
        retVal.encryptable(false);
        return retVal;
    }

    /**
     * Writes the document to the given {@link File}. The document is closed once written.
     *
     * @see PDDocument#close()
     */
    public void writeTo(File file, WriteOption... options) throws IOException
    {
        writeTo(from(file), null, options);
    }

    /**
     * Writes the document to the file corresponding the given file name. The document is closed
     * once written.
     *
     * @see PDDocument#close()
     */
    public void writeTo(String filename, WriteOption... options) throws IOException
    {
        writeTo(from(filename), null, options);
    }

    /**
     * Writes the document to the given {@link WritableByteChannel}. The document is closed once
     * written.
     *
     * @see PDDocument#close()
     */
    public void writeTo(WritableByteChannel channel, WriteOption... options) throws IOException
    {
        writeTo(from(channel), null, options);
    }

    /**
     * Writes the document to the given {@link OutputStream}. The document is closed once written.
     *
     * @see PDDocument#close()
     */
    public void writeTo(OutputStream out, WriteOption... options) throws IOException
    {
        writeTo(from(out), null, options);
    }

    /**
     * Writes the document to the given {@link File} encrypting it using the given security. The
     * document is closed once written.
     *
     * @see PDDocument#close()
     */
    public void writeTo(File file, StandardSecurity security, WriteOption... options)
            throws IOException
    {
        writeTo(from(file), security, options);
    }

    /**
     * Writes the document to the file corresponding the given file name encrypting it using the
     * given security. The document is closed once written.
     *
     * @see PDDocument#close()
     */
    public void writeTo(String filename, StandardSecurity security, WriteOption... options)
            throws IOException
    {
        writeTo(from(filename), security, options);
    }

    /**
     * Writes the document to the given {@link WritableByteChannel} encrypting it using the given
     * security. The document is closed once written.
     *
     * @see PDDocument#close()
     */
    public void writeTo(WritableByteChannel channel, StandardSecurity security,
            WriteOption... options) throws IOException
    {
        writeTo(from(channel), security, options);
    }

    /**
     * Writes the document to the given {@link OutputStream} encrypting it using the given security.
     * The document is closed once written.
     *
     * @see PDDocument#close()
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

        updateMetadata(options);
        subsetFonts();

        EncryptionContext encryptionContext = ofNullable(security).map(EncryptionContext::new)
                .orElse(null);
        generateFileIdentifier(output.toString().getBytes(StandardCharsets.ISO_8859_1),
                encryptionContext);
        try (PDDocumentWriter writer = new PDDocumentWriter(output, encryptionContext,
                preSaveCOSTransformer, options))
        {
            onBeforeWrite.onBeforeWrite();
            writer.write(this);
        }
        finally
        {
            IOUtils.close(this);
        }
    }

    private void subsetFonts()
    {
        for (Subsettable font : fontsToSubset)
        {
            try
            {
                font.subset();
            }
            catch (Exception e)
            {
                LOG.warn("Exception occurred while subsetting font: " + font, e);
            }
        }
        fontsToSubset.clear();
    }

    private void updateMetadata(WriteOption[] options)
    {
        if (Arrays.stream(options)
                .noneMatch(i -> i == WriteOption.NO_METADATA_PRODUCER_MODIFIED_DATE_UPDATE))
        {
            // update producer and last modification date only if the write option doesn't state otherwise
            getDocumentInformation().setProducer(SAMBox.PRODUCER);
            getDocumentInformation().setModificationDate(Calendar.getInstance());
        }
        if (Arrays.stream(options).anyMatch(o -> o == WriteOption.UPSERT_DOCUMENT_METADATA_STREAM))
        {
            requireMinVersion(V1_4);
            var metadataStream = new PDMetadata();
            try (var metadataOutputStream = new BufferedOutputStream(
                    metadataStream.getCOSObject().createUnfilteredStream()))
            {
                var meta = getDocumentInformation().toXMPMetadata(getOrCreateXmpMetadata());
                AdobePDFSchema pdfSchema = ofNullable(meta.getAdobePDFSchema()).orElseGet(
                        meta::createAndAddAdobePDFSchema);
                pdfSchema.setPDFVersion(getVersion());
                new XmpSerializer().serialize(meta, metadataOutputStream, true);
                getDocumentCatalog().setMetadata(metadataStream);
            }
            catch (IOException | TransformerException e)
            {
                LOG.warn("Unable to set xmp document metadata", e);
            }
            catch (XmpParsingException e)
            {
                LOG.warn("Unable to parse existing document level xmp metadata", e);
            }
        }
    }

    private XMPMetadata getOrCreateXmpMetadata() throws XmpParsingException, IOException
    {
        var metadata = getDocumentCatalog().getMetadata();
        if (nonNull(metadata))
        {
            try
            {
                var parser = new DomXmpParser();
                parser.setStrictParsing(false);
                return parser.parse(new BufferedInputStream(metadata.createInputStream()));
            }
            finally
            {
                metadata.getCOSObject().unDecode();
            }
        }
        return XMPMetadata.createXMPMetadata();
    }

    /**
     * Sets the {@link PreSaveCOSTransformer} for this PDDocument object. This transformer is
     * responsible for visiting COSBase objects before they are written to the output. The intended
     * use is {@code document.withPreSaveTransformer(processor).writeTo(file, options);}
     */
    public PDDocument withPreSaveTransformer(PreSaveCOSTransformer transformer)
    {
        this.preSaveCOSTransformer = transformer;
        return this;
    }

    /**
     * @return true if the {@link PDDocument} is open
     */
    public boolean isOpen()
    {
        return this.open;
    }

    /**
     * Closes the {@link PDDocument} executing the set onClose action. Once closed the document is
     * pretty much unusable since most of the methods requires an open document.
     *
     * @see PDDocument#setOnCloseAction(OnClose)
     */
    @Override
    public void close() throws IOException
    {
        if (isOpen())
        {
            onClose.onClose();
            this.resourceCache.clear();
            this.open = false;
        }
    }

    /**
     * Action to be performed before the {@link PDDocument} is close
     *
     * @author Andrea Vacondio
     */
    @FunctionalInterface
    public interface OnClose
    {
        /**
         * Sets an action to be performed right before this {@link PDDocument} is closed.
         */
        void onClose() throws IOException;

        default OnClose andThen(OnClose after)
        {
            Objects.requireNonNull(after);
            return () -> {
                onClose();
                after.onClose();
            };
        }
    }

    /**
     * Action to be performed right before this {@link PDDocument} is written to a file.
     */
    @FunctionalInterface
    public interface OnBeforeWrite
    {
        void onBeforeWrite() throws IOException;

        default OnBeforeWrite andThen(OnBeforeWrite after)
        {
            Objects.requireNonNull(after);
            return () -> {
                onBeforeWrite();
                after.onBeforeWrite();
            };
        }
    }

    /**
     * Returns the resource cache associated with this document, or null if there is none.
     */
    public ResourceCache getResourceCache()
    {
        return resourceCache;
    }

    // bridge to pdfbox style api, used in tests
    public static PDDocument load(File file) throws IOException
    {
        return PDFParser.parse(SeekableSources.seekableSourceFrom(file));
    }

    public static PDDocument load(String path) throws IOException
    {
        return load(new File(path));
    }

    public void save(String path) throws IOException
    {
        writeTo(new File(path), WriteOption.COMPRESS_STREAMS);
    }

    public boolean hasParseErrors()
    {
        return this.document.getTrailer().getFallbackScanStatus() != null;
    }

    public void assertNumberOfPagesIsAccurate()
    {
        this.getPages().iterator();
    }

}
