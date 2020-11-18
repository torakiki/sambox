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

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static org.sejda.commons.util.RequireUtils.requireIOCondition;
import static org.sejda.sambox.input.BaseCOSParser.ENDOBJ;
import static org.sejda.sambox.input.BaseCOSParser.ENDSTREAM;
import static org.sejda.sambox.input.BaseCOSParser.STREAM;
import static org.sejda.sambox.input.SourceReader.OBJ;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.sejda.commons.util.IOUtils;
import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSNull;
import org.sejda.sambox.cos.COSObjectKey;
import org.sejda.sambox.cos.COSStream;
import org.sejda.sambox.pdmodel.encryption.SecurityHandler;
import org.sejda.sambox.xref.CompressedXrefEntry;
import org.sejda.sambox.xref.Xref;
import org.sejda.sambox.xref.XrefEntry;
import org.sejda.sambox.xref.XrefType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A lazy implementation of the {@link IndirectObjectsProvider} that retrieves {@link COSBase} objects parsing the
 * underlying source on demand (ie. when the {@link IndirectObjectsProvider#get(COSObjectKey)} method is called). Parsed
 * objects are stored in a cache to be reused. If for given a {@link COSObjectKey} no entry is found in the xref, a
 * fallback mechanism is activated performing a full scan of the document to retrieve all the objects defined in it.
 * 
 * @author Andrea Vacondio
 */
class LazyIndirectObjectsProvider implements IndirectObjectsProvider
{
    private static final Logger LOG = LoggerFactory.getLogger(LazyIndirectObjectsProvider.class);

    private Xref xref = new Xref();
    private ObjectsFullScanner scanner;
    // TODO references that the GC can claim
    private Map<COSObjectKey, COSBase> store = new ConcurrentHashMap<>();
    private SecurityHandler securityHandler = null;
    private COSParser parser;

    @Override
    public COSBase get(COSObjectKey key)
    {
        if (isNull(store.get(key)))
        {
            parseObject(key);
        }
        return store.get(key);
    }

    @Override
    public void release(COSObjectKey key)
    {
        store.remove(key);
    }

    @Override
    public XrefEntry addEntryIfAbsent(XrefEntry entry)
    {
        XrefEntry retVal = xref.addIfAbsent(entry);
        if (retVal == null)
        {
            LOG.trace("Added xref entry {}", entry);
        }
        return retVal;
    }

    @Override
    public XrefEntry addEntry(XrefEntry entry)
    {
        LOG.trace("Added xref entry {}", entry);
        return xref.add(entry);
    }

    @Override
    public COSObjectKey highestKey()
    {
        return xref.highestKey();
    }

    @Override
    public LazyIndirectObjectsProvider initializeWith(COSParser parser)
    {
        requireNonNull(parser);
        this.parser = parser;
        this.scanner = new ObjectsFullScanner(parser);
        return this;
    }

    @Override
    public LazyIndirectObjectsProvider initializeWith(SecurityHandler handler)
    {
        this.securityHandler = handler;
        return this;
    }

    private synchronized void parseObject(COSObjectKey key)
    {
        XrefEntry xrefEntry = xref.get(key);
        if (nonNull(xrefEntry))
        {
            try
            {
                doParse(xrefEntry);
            }
            catch (IOException e)
            {
                LOG.warn("An error occurred while parsing " + xrefEntry, e);
                doParseFallbackObject(key);
            }
        }
        else
        {
            LOG.warn("Unable to find xref data for {}", key);
            doParseFallbackObject(key);
        }
    }

    private void doParseFallbackObject(COSObjectKey key)
    {
        LOG.info("Trying fallback strategy for " + key);
        XrefEntry xrefEntry = scanner.entries().get(key);
        if (nonNull(xrefEntry))
        {
            try
            {
                doParse(xrefEntry);
            }
            catch (IOException e)
            {
                LOG.warn("Unable to find fallback xref entry for " + key, e);
            }
        }
        else
        {
            LOG.warn("Unable to find fallback xref entry for " + key);
        }
    }

    private void doParse(XrefEntry xrefEntry) throws IOException
    {
        LOG.trace("Parsing indirect object {}", xrefEntry);
        if (xrefEntry.getType() == XrefType.IN_USE)
        {
            parseInUseEntry(xrefEntry);
        }
        if (xrefEntry.getType() == XrefType.COMPRESSED)
        {
            parseCompressedEntry(xrefEntry);
        }
        LOG.trace("Parsing done");
    }

    private void parseInUseEntry(XrefEntry xrefEntry) throws IOException
    {
        parser.position(xrefEntry.getByteOffset());
        parser.skipExpectedIndirectObjectDefinition(xrefEntry.key());
        parser.skipSpaces();
        COSBase found = parser.nextParsedToken();
        parser.skipSpaces();
        if (parser.isNextToken(STREAM))
        {
            requireIOCondition(found instanceof COSDictionary,
                    "Found stream with missing dictionary");
            found = parser.nextStream((COSDictionary) found);
            if (parser.skipTokenIfValue(ENDSTREAM))
            {
                LOG.warn("Found double 'endstream' token for {}", xrefEntry);
            }
        }
        if (securityHandler != null)
        {
            LOG.trace("Decrypting entry {}", xrefEntry);
            securityHandler.decrypt(found, xrefEntry.getObjectNumber(),
                    xrefEntry.getGenerationNumber());
        }
        if (!parser.skipTokenIfValue(ENDOBJ))
        {
            LOG.warn("Missing 'endobj' token for {}", xrefEntry);
        }

        if (found instanceof ExistingIndirectCOSObject)
        {
            ExistingIndirectCOSObject existingIndirectCOSObject = (ExistingIndirectCOSObject) found;
            // does this point to itself? it would cause a StackOverflowError. Example:
            // 9 0 obj
            // 9 0 R
            // endobj
            if (existingIndirectCOSObject.id().objectIdentifier.equals(xrefEntry.key()))
            {
                LOG.warn("Found indirect object definition pointing to itself, for {}", xrefEntry);
                found = COSNull.NULL;
            }
        }
        store.put(xrefEntry.key(), ofNullable(found).orElse(COSNull.NULL));
    }

    private void parseCompressedEntry(XrefEntry xrefEntry) throws IOException
    {
        XrefEntry containingStreamEntry = xref.get(
                new COSObjectKey(((CompressedXrefEntry) xrefEntry).getObjectStreamNumber(), 0));

        requireIOCondition(
                nonNull(containingStreamEntry)
                        && containingStreamEntry.getType() != XrefType.COMPRESSED,
                "Expected an uncompressed indirect object reference for the ObjectStream");

        parseObject(containingStreamEntry.key());
        COSBase stream = ofNullable(store.get(containingStreamEntry.key()))
                .map(COSBase::getCOSObject).orElseThrow(() -> new IOException(
                        "Unable to find ObjectStream " + containingStreamEntry));

        if (!(stream instanceof COSStream))
        {
            throw new IOException(
                    "Expected an object stream instance for " + containingStreamEntry);
        }
        parseObjectStream(containingStreamEntry, (COSStream) stream);
    }

    private void parseObjectStream(XrefEntry containingStreamEntry, COSStream stream)
            throws IOException
    {
        try (COSParser streamParser = new COSParser(stream.getUnfilteredSource(), this))
        {
            requireIOCondition(
                    !isIndirectContainedIn(stream.getItem(COSName.N), containingStreamEntry),
                    "Objects stream size cannot be store as indirect reference in the ObjStm itself");
            int numberOfObjects = stream.getInt(COSName.N);
            requireIOCondition(numberOfObjects >= 0,
                    "Missing or negative required objects stream size");
            requireIOCondition(
                    !isIndirectContainedIn(stream.getItem(COSName.FIRST), containingStreamEntry),
                    "Objects stream first offset cannot be store as indirect reference in the ObjStm itself");
            long firstOffset = stream.getLong(COSName.FIRST);
            requireIOCondition(firstOffset >= 0,
                    "Missing or negative required bytes offset of the fist object in the objects stream");
            Map<Long, Long> entries = new TreeMap<>();
            for (int i = 0; i < numberOfObjects; i++)
            {
                long number = streamParser.readObjectNumber();
                long offset = firstOffset + streamParser.readLong();
                entries.put(offset, number);
            }
            LOG.trace("Found {} entries in object stream of size {}", entries.size(),
                    streamParser.source().size());
            for (Entry<Long, Long> entry : entries.entrySet())
            {
                LOG.trace("Parsing compressed object {} at offset {}", entry.getValue(),
                        entry.getKey());
                streamParser.position(entry.getKey());
                if (streamParser.skipTokenIfValue(OBJ))
                {
                    LOG.warn("Unexptected 'obj' token in objects stream");
                }
                COSBase object = streamParser.nextParsedToken();
                if (object != null)
                {
                    COSObjectKey key = new COSObjectKey(entry.getValue(), 0);
                    // make sure the xref points to this copy of the object and not one in another more recent stream
                    if (containingStreamEntry.owns(xref.get(key)))
                    {
                        LOG.trace("Parsed compressed object {} {}", key, object.getClass());
                        store.put(key, object);
                    }
                }
                if (streamParser.skipTokenIfValue(ENDOBJ))
                {
                    LOG.warn("Unexptected 'endobj' token in objects stream");
                }
            }
        }
        IOUtils.close(stream);
    }

    private boolean isIndirectContainedIn(COSBase item, XrefEntry containingStreamEntry)
    {
        if (item instanceof ExistingIndirectCOSObject)
        {
            return ofNullable(item.id()).map(i -> i.objectIdentifier).map(xref::get)
                    .filter(e -> e instanceof CompressedXrefEntry).map(e -> (CompressedXrefEntry) e)
                    .map(e -> containingStreamEntry.key().objectNumber() == e
                            .getObjectStreamNumber())
                    .orElse(Boolean.FALSE);
        }
        return false;
    }

    @Override
    public void close()
    {
        store.values().stream().filter(o -> o instanceof Closeable).map(o -> (Closeable) o)
                .forEach(IOUtils::closeQuietly);
        store.clear();
    }

    @Override
    public String id()
    {
        return parser.source().id();
    }

}
