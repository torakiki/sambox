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
package org.apache.pdfbox.cos;

import static org.apache.pdfbox.cos.BaseCOSParser.ENDOBJ;
import static org.apache.pdfbox.cos.BaseCOSParser.ENDSTREAM;
import static org.apache.pdfbox.cos.BaseCOSParser.STREAM;
import static org.apache.pdfbox.cos.SourceReader.OBJ;
import static org.apache.pdfbox.util.RequireUtils.requireIOCondition;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.io.PushBackInputStream;
import org.apache.pdfbox.pdmodel.encryption.SecurityHandler;
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
    private BaseCOSParser parser;
    // TODO references that the GC can claim
    private Map<COSObjectKey, COSBase> store = new HashMap<>();
    private SecurityHandler securityHandler = null;

    LazyIndirectObjectsProvider(BaseCOSParser parser)
    {
        this.parser = parser;
    }

    @Override
    public COSBase get(COSObjectKey key)
    {
        try
        {
            COSBase value = store.get(key);
            if (value == null)
            {
                parseObject(key);
            }
            return store.get(key);
        }
        catch (IOException e)
        {
            LOG.error("An error occured while retrieving indirect object " + key, e);
        }
        return null;
    }

    @Override
    public Xref xref()
    {
        return xref;
    }

    @Override
    public void decryptWith(SecurityHandler handler)
    {
        this.securityHandler = handler;
    }

    private void parseObject(COSObjectKey key) throws IOException
    {
        XrefEntry xrefEntry = xref.get(key);
        LOG.debug("Starting parse of indirect object " + xrefEntry);
        if (xrefEntry.getType() == XrefType.IN_USE)
        {
            parser.offset(xrefEntry.getByteOffset());
            parser.skipExpectedIndirectObjectDefinition(key);
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
                securityHandler.decrypt(found, xrefEntry.getObjectNumber(),
                        xrefEntry.getGenerationNumber());
            }
            if (!parser.skipTokenIfValue(ENDOBJ))
            {
                LOG.warn("Missing 'endobj' token for " + xrefEntry);
            }
            store.put(key, found);
        }
        if (xrefEntry.getType() == XrefType.COMPRESSED)
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
                throw new IOException("Expected an object stream instance for "
                        + containingStreamEntry);
            }
            parseObjectStream(containingStreamEntry, (COSStream) stream);
        }
    }

    private void parseObjectStream(XrefEntry containingStreamEntry, COSStream stream)
            throws IOException
    {
        try (BaseCOSParser streamParser = new BaseCOSParser(new PushBackInputStream(
                stream.getUnfilteredStream(), 65536), this))
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
            for (Entry<Long, Long> entry : entries.entrySet())
            {
                streamParser.offset(entry.getValue());
                if (streamParser.skipTokenIfValue(OBJ))
                {
                    LOG.warn("Unexptected 'obj' token in objects stream");
                }
                COSBase object = parser.nextParsedToken();
                if (object != null)
                {
                    COSObjectKey key = new COSObjectKey(entry.getValue(), 0);
                    // make sure the xref points to this copy of the object and not one in another more recent stream
                    if (containingStreamEntry.owns(xref.get(key)))
                    {
                        LOG.debug("Parsed compressed object " + key);
                        store.put(key, object);
                    }
                }
                if (streamParser.skipTokenIfValue(ENDOBJ))
                {
                    LOG.warn("Unexptected 'endobj' token in objects stream");
                }
            }
        }
    }
}
