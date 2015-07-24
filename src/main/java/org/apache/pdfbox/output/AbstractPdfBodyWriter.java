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
package org.apache.pdfbox.output;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSBoolean;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.cos.COSFloat;
import org.apache.pdfbox.cos.COSInteger;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSNull;
import org.apache.pdfbox.cos.COSObjectKey;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.cos.COSVisitor;
import org.apache.pdfbox.cos.IndirectCOSObjectReference;
import org.apache.pdfbox.input.ExistingIndirectCOSObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base component providing methods to write the body of a pdf document. This implementation starts from the document
 * trailer and visits the whole document graph replacing {@link COSDictionary} and {@link ExistingIndirectCOSObject} with a
 * newly created {@link IndirectCOSObjectReference}. {@link IndirectCOSObjectReference}s are cached and reused if the
 * corresponding {@link COSDictionary}/{@link ExistingIndirectCOSObject} is found again while exploring the graph. Once all the
 * values of a {@link COSDictionary} or {@link COSArray} have been processed, the {@link COSDictionary}/{@link COSArray}
 * is written as an object, this allows an async implementation to write objects while the writer is still perfoming its
 * algorithm.
 * 
 * @author Andrea Vacondio
 */
abstract class AbstractPdfBodyWriter implements COSVisitor, Closeable
{
    private static final Logger LOG = LoggerFactory.getLogger(AbstractPdfBodyWriter.class);

    private Map<String, Map<COSObjectKey, IndirectCOSObjectReference>> bySourceExistingIndirectToNewXref = new HashMap<>();
    private Map<COSBase, IndirectCOSObjectReference> newObjects = new HashMap<>();
    private AtomicInteger objectsCounter = new AtomicInteger(0);

    private Queue<IndirectCOSObjectReference> stack = new LinkedList<>();

    /**
     * Writes the body of the given document
     * 
     * @param document
     * @throws IOException
     */
    public void write(COSDocument document) throws IOException
    {
        document.accept(this);
    }

    @Override
    public void visit(COSDocument document) throws IOException
    {
        // TODO add Encrypt once implemented
        // for (COSName k : Arrays.asList(COSName.ROOT, COSName.INFO, COSName.ENCRYPT))
        for (COSName k : Arrays.asList(COSName.ROOT, COSName.INFO))
        {
            COSBase value = document.getTrailer().getItem(k);
            if (value != null)
            {
                IndirectCOSObjectReference ref = getOrCreateIndirectReferenceFor(value);
                document.getTrailer().setItem(k, ref);
            }
        }
        IndirectCOSObjectReference item;
        while ((item = stack.poll()) != null)
        {
            item.getCOSObject().accept(this);
            writeObject(item);
        }
        onCompletion();
    }

    abstract void writeObject(IndirectCOSObjectReference ref) throws IOException;

    abstract void onCompletion() throws IOException;

    @Override
    public void visit(COSArray array) throws IOException
    {
        for (int i = 0; i < array.size(); i++)
        {
            COSBase item = array.get(i);
            if (item instanceof ExistingIndirectCOSObject || item instanceof COSDictionary)
            {
                IndirectCOSObjectReference ref = getOrCreateIndirectReferenceFor(item);
                array.set(i, ref);
            }
            else
            {
                item.accept(this);
            }
        }
    }

    @Override
    public void visit(COSDictionary value) throws IOException
    {
        for (COSName key : value.keySet())
        {
            COSBase item = value.getItem(key);
            if (item instanceof ExistingIndirectCOSObject || item instanceof COSDictionary)
            {
                IndirectCOSObjectReference ref = getOrCreateIndirectReferenceFor(item);
                value.setItem(key, ref);
            }
            else
            {
                item.accept(this);
            }
        }
    }

    @Override
    public void visit(COSStream value) throws IOException
    {
        value.removeItem(COSName.LENGTH);
        this.visit((COSDictionary) value);

    }

    private IndirectCOSObjectReference getOrCreateIndirectReferenceFor(COSBase item)
    {
        if (item instanceof ExistingIndirectCOSObject)
        {
            String sourceId = ((ExistingIndirectCOSObject) item).sourceId();
            Map<COSObjectKey, IndirectCOSObjectReference> indirectsForSource = Optional.ofNullable(
                    bySourceExistingIndirectToNewXref.get(sourceId)).orElseGet(() -> {
                HashMap<COSObjectKey, IndirectCOSObjectReference> newMap = new HashMap<>();
                bySourceExistingIndirectToNewXref.put(sourceId, newMap);
                return newMap;
            });

            COSObjectKey key = ((ExistingIndirectCOSObject) item).key();
            return Optional.ofNullable(indirectsForSource.get(key)).orElseGet(
                    () -> {
                        IndirectCOSObjectReference newRef = nextReferenceFor(item);
                        LOG.trace("Created new indirect reference " + newRef
                                + " replacing the existing one " + key);
                        indirectsForSource.put(key, newRef);
                        return newRef;
                    });
        }
        else if (item instanceof COSDictionary)
        {
            return Optional.ofNullable(newObjects.get(item)).orElseGet(() -> {
                IndirectCOSObjectReference newRef = nextReferenceFor(item);
                LOG.trace("Created new indirect reference '" + newRef + "' for dictionary item");
                newObjects.put(item, newRef);
                return newRef;
            });
        }
        throw new IllegalArgumentException("Indirect reference is not supperted for type "
                + item.getClass());
    }

    private IndirectCOSObjectReference nextReferenceFor(COSBase baseObject)
    {
        IndirectCOSObjectReference ref = new IndirectCOSObjectReference(
                objectsCounter.incrementAndGet(), 0, baseObject);
        stack.add(ref);
        return ref;
    }

    @Override
    public void visit(COSBoolean value)
    {
        // nothing to do
    }

    @Override
    public void visit(COSFloat value)
    {
        // nothing to do
    }

    @Override
    public void visit(COSInteger value)
    {
        // nothing to do
    }

    @Override
    public void visit(COSName value)
    {
        // nothing to do
    }

    @Override
    public void visit(COSNull value)
    {
        // nothing to do
    }

    @Override
    public void visit(COSString value)
    {
        // nothing to do
    }

    @Override
    public void visit(IndirectCOSObjectReference value)
    {
        // nothing to do
    }

    @Override
    public void close()
    {
        bySourceExistingIndirectToNewXref.clear();
        newObjects.clear();
    }

}
