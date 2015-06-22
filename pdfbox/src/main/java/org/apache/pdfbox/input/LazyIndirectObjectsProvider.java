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
package org.apache.pdfbox.input;

import static java.util.Objects.requireNonNull;
import static org.apache.pdfbox.input.BaseCOSParser.ENDOBJ;
import static org.apache.pdfbox.input.BaseCOSParser.ENDSTREAM;
import static org.apache.pdfbox.input.BaseCOSParser.STREAM;
import static org.apache.pdfbox.input.SourceReader.OBJ;
import static org.apache.pdfbox.util.RequireUtils.requireIOCondition;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSObjectKey;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.pdmodel.encryption.SecurityHandler;
import org.apache.pdfbox.util.IOUtils;
import org.apache.pdfbox.xref.CompressedXrefEntry;
import org.apache.pdfbox.xref.Xref;
import org.apache.pdfbox.xref.XrefEntry;
import org.apache.pdfbox.xref.XrefType;

/**
 * @author Andrea Vacondio
 *
 */
class LazyIndirectObjectsProvider implements IndirectObjectsProvider
{
    private static final Log LOG = LogFactory.getLog(LazyIndirectObjectsProvider.class);

    private Xref xref = new Xref();
    private ObjectsFullScanner scanner;
    // TODO references that the GC can claim
    private Map<COSObjectKey, COSBase> store = new ConcurrentHashMap<>();
    private SecurityHandler securityHandler = null;
    private BaseCOSParser parser;

    @Override
    public COSBase get(COSObjectKey key)
    {
        COSBase value = store.get(key);
        if (value == null)
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
    public void addEntryIfAbsent(XrefEntry entry)
    {
        if (xref.addIfAbsent(entry) == null)
        {
            LOG.trace("Added xref entry " + entry);
        }
    }

    @Override
    public void addEntry(XrefEntry entry)
    {
        LOG.trace("Added xref entry " + entry);
        xref.add(entry);
    }

    @Override
    public LazyIndirectObjectsProvider initializeWith(BaseCOSParser parser)
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

    private void parseObject(COSObjectKey key)
    {
        XrefEntry xrefEntry = xref.get(key);
        try
        {
            requireIOCondition(xrefEntry != null, "Unable to find xref data for " + key);
            doParse(xrefEntry, parser);
        }
        catch (IOException e)
        {
            LOG.warn("An error occurred while parsing " + xrefEntry + ", trying fallback", e);
            doParseFallbackObject(key, parser);
        }
    }

    private void doParseFallbackObject(COSObjectKey key, BaseCOSParser parser)
    {
        LOG.info("Applying fallback strategy for " + key);
        XrefEntry xrefEntry = scanner.entries().get(key);
        if (xrefEntry != null)
        {
            try
            {
                doParse(xrefEntry, parser);
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

    private void doParse(XrefEntry xrefEntry, BaseCOSParser parser) throws IOException
    {
        LOG.trace("Parsing indirect object " + xrefEntry);
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
                LOG.warn("Found double 'endstream' token for " + xrefEntry);
            }
        }
        if (securityHandler != null)
        {
            LOG.trace("Decrypting stream");
            securityHandler.decrypt(found, xrefEntry.getObjectNumber(),
                    xrefEntry.getGenerationNumber());
        }
        if (!parser.skipTokenIfValue(ENDOBJ))
        {
            LOG.warn("Missing 'endobj' token for " + xrefEntry);
        }
        store.put(xrefEntry.key(), found);
    }

    private void parseCompressedEntry(XrefEntry xrefEntry) throws IOException
    {
        XrefEntry containingStreamEntry = xref.get(new COSObjectKey(
                ((CompressedXrefEntry) xrefEntry).getObjectStreamNumber(), 0));

        requireIOCondition(containingStreamEntry != null
                && containingStreamEntry.getType() != XrefType.COMPRESSED,
                "Expected an uncompressed indirect object reference for the ObjectStream");

        parseObject(containingStreamEntry.key());
        COSBase stream = store.get(containingStreamEntry.key()).getCOSObject();

        if (!(stream instanceof COSStream))
        {
            throw new IOException("Expected an object stream instance for " + containingStreamEntry);
        }
        parseObjectStream(containingStreamEntry, (COSStream) stream);
    }

    private void parseObjectStream(XrefEntry containingStreamEntry, COSStream stream)
            throws IOException
    {
        try (BaseCOSParser streamParser = new BaseCOSParser(stream.getUnfilteredSource(), this))
        {
            int numberOfObjects = stream.getInt(COSName.N);
            requireIOCondition(numberOfObjects >= 0,
                    "Missing or negative required objects stream size");
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
            LOG.trace("Found " + entries.size() + " entries in object stream of size "
                    + streamParser.source().size());
            for (Entry<Long, Long> entry : entries.entrySet())
            {
                LOG.trace("Parsing compressed object " + entry.getValue() + " at offset "
                        + entry.getKey());
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
                        LOG.debug("Parsed compressed object " + key + " " + object.getClass());
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

    @Override
    public void close()
    {
        store.clear();
    }

    @Override
    public String id()
    {
        return parser.source().id();
    }
}
