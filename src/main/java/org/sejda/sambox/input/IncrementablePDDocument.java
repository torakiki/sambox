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
package org.sejda.sambox.input;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.sejda.commons.util.RequireUtils.requireNotBlank;
import static org.sejda.commons.util.RequireUtils.requireNotNullArg;
import static org.sejda.commons.util.RequireUtils.requireState;
import static org.sejda.io.CountingWritableByteChannel.from;
import static org.sejda.sambox.cos.DirectCOSObject.asDirectObject;
import static org.sejda.sambox.util.SpecVersionUtils.V1_4;
import static org.sejda.sambox.util.SpecVersionUtils.isAtLeast;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.sejda.commons.util.IOUtils;
import org.sejda.io.CountingWritableByteChannel;
import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSNull;
import org.sejda.sambox.cos.COSObjectKey;
import org.sejda.sambox.cos.COSObjectable;
import org.sejda.sambox.cos.COSString;
import org.sejda.sambox.cos.DirectCOSObject;
import org.sejda.sambox.cos.IndirectCOSObjectIdentifier;
import org.sejda.sambox.cos.IndirectCOSObjectReference;
import org.sejda.sambox.output.IncrementablePDDocumentWriter;
import org.sejda.sambox.output.WriteOption;
import org.sejda.sambox.pdmodel.PDDocument;
import org.sejda.sambox.xref.FileTrailer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Model for a document to be used to incrementally update an existing PDF file. Has info regarding PDF objects that
 * need to be replaced in the original document.
 * 
 * @author Andrea Vacondio
 *
 */
public class IncrementablePDDocument implements Closeable
{
    private static final Logger LOG = LoggerFactory.getLogger(IncrementablePDDocument.class);

    private Map<IndirectCOSObjectIdentifier, COSBase> replacements = new HashMap<>();
    private Set<COSBase> newIndirects = new HashSet<>();
    private PDDocument incremented;
    public final COSParser parser;

    IncrementablePDDocument(PDDocument incremented, COSParser parser)
    {
        requireNotNullArg(incremented, "Incremented document cannot be null");
        requireNotNullArg(parser, "COSParser cannot be null");
        this.incremented = incremented;
        this.parser = parser;
    }

    public PDDocument incremented()
    {
        return incremented;
    }

    /**
     * @return the trailer of the incremented document
     */
    public FileTrailer trailer()
    {
        return incremented.getDocument().getTrailer();
    }

    /**
     * @return the incremented document as a stream to be written "as is"
     * @throws IOException
     */
    public InputStream incrementedAsStream() throws IOException
    {
        parser.source().position(0);
        return parser.source().asInputStream();
    }

    /**
     * @return the highest object reference in the document that is being incrementally updated
     */
    public COSObjectKey highestExistingReference()
    {
        return parser.provider().highestKey();
    }

    /**
     * Replaces the object with the given {@link IndirectCOSObjectIdentifier} during the incremental update
     * 
     * @param toReplace
     * @param replacement
     */
    public void replace(IndirectCOSObjectIdentifier toReplace, COSObjectable replacement)
    {
        requireNotNullArg(toReplace, "Missing id of the object to be replaced");
        replacements.put(toReplace,
                ofNullable(replacement).map(COSObjectable::getCOSObject).orElse(COSNull.NULL));
    }

    /**
     * Adds the given object as modified, this object will be written as part of the incremental update.
     * 
     * @param modified
     * @return true if the {@link COSBase} was added, false if not. In case where false is returned, the {@link COSBase}
     * doesn't have an id, meaning it's not written as indirect object in the original document but it's written as
     * direct object. In this case we have to call {@link IncrementablePDDocument#modified(COSBase)} on the first
     * indirect parent because incremental updates are meant to replace indirect references.
     */
    public boolean modified(COSObjectable modified)
    {
        requireNotNullArg(modified, "Missing modified object");
        if (modified.getCOSObject().hasId())
        {
            replacements.put(modified.getCOSObject().id(), modified.getCOSObject());
            return true;
        }
        return false;
    }

    /**
     * Adds the given object to the set of the new indirect objects. These objects will be written as new indirect
     * objects (with a new object number) as part of the incremental update. If, when writing the incremental update, a
     * new object that was not added using this method is found, it will be written as direct object and no indirect
     * reference will be created.
     * 
     * @param newObject
     */
    public void newIndirect(COSObjectable newObject)
    {
        requireNotNullArg(newObject, "Missing new object object");
        newIndirects.add(newObject.getCOSObject());
    }

    /**
     * @return a list of {@link IndirectCOSObjectReference} to be written as replacements for this incremental update
     */
    public List<IndirectCOSObjectReference> replacements()
    {

        return replacements.entrySet().stream()
                .map(e -> new IndirectCOSObjectReference(e.getKey().objectIdentifier.objectNumber(),
                        e.getKey().objectIdentifier.generation(), e.getValue().getCOSObject()))
                .collect(Collectors.toList());
    }

    /**
     * @return a set of objects for which a new indirect reference should be created
     */
    public Set<COSBase> newIndirects()
    {
        return Collections.unmodifiableSet(newIndirects);
    }

    /**
     * @return the encryption dictionary for the existing document
     */
    public COSDictionary encryptionDictionary()
    {
        return incremented.getDocument().getEncryptionDictionary();
    }

    /**
     * @return the encryption key, if the incremented document is encrypted, null otherwise.
     */
    public byte[] encryptionKey()
    {
        return ofNullable(incremented.getSecurityHandler()).map(s -> s.getEncryptionKey())
                .orElse(null);
    }

    @Override
    public void close() throws IOException
    {
        incremented.close();
        IOUtils.close(parser.provider());
        IOUtils.close(parser);
    }

    /**
     * Writes the document to the given {@link File}. The document is closed once written.
     * 
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
        requireState(incremented.isOpen(), "The document is closed");
        requireState(!replacements.isEmpty(), "No update to be incrementally written");
        updateId(output.toString().getBytes(StandardCharsets.ISO_8859_1));

        try (IncrementablePDDocumentWriter writer = new IncrementablePDDocumentWriter(output,
                options))
        {
            writer.write(this);
        }
        finally
        {
            IOUtils.close(this);
        }
    }

    /**
     * Updates the file identifier as defined in the chap 14.4 PDF 32000-1:2008
     * 
     * @param bytes
     */
    private void updateId(byte[] bytes)
    {
        DirectCOSObject id = asDirectObject(incremented.generateFileIdentifier(bytes));
        COSArray existingId = incremented.getDocument().getTrailer().getCOSObject()
                .getDictionaryObject(COSName.ID, COSArray.class);
        if (nonNull(existingId) && existingId.size() == 2)
        {
            ((COSString) existingId.get(0).getCOSObject()).encryptable(false);
            existingId.set(1, id);
        }
        else
        {
            incremented.getDocument().getTrailer().getCOSObject().setItem(COSName.ID,
                    asDirectObject(new COSArray(id, id)));
        }
    }

    /**
     * Sets the version for this document if not at the minimum version required
     * 
     * @param version
     */
    public void requireMinVersion(String version)
    {
        if (!isAtLeast(incremented.getVersion(), version))
        {
            LOG.debug("Minimum spec version required is {}", version);
            setVersion(version);
        }
    }

    public void setVersion(String newVersion)
    {
        requireState(incremented.isOpen(), "The document is closed");
        requireNotBlank(newVersion, "Spec version cannot be blank");
        int compare = incremented.getVersion().compareTo(newVersion);
        if (compare > 0)
        {
            LOG.info("Spec version downgrade not allowed");
        }
        else if (compare < 0)
        {
            if (isAtLeast(newVersion, V1_4))
            {
                COSDictionary catalog = incremented.getDocument().getCatalog();
                catalog.setName(COSName.VERSION, newVersion);
                modified(catalog);
            }
            else
            {
                LOG.warn(
                        "Sepc version must be at least 1.4 to be set as catalog entry in an incremental update");
            }
        }
    }

}
