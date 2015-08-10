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
package org.sejda.sambox.output;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;

import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSBoolean;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSDocument;
import org.sejda.sambox.cos.COSFloat;
import org.sejda.sambox.cos.COSInteger;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSNull;
import org.sejda.sambox.cos.COSObjectKey;
import org.sejda.sambox.cos.COSStream;
import org.sejda.sambox.cos.COSString;
import org.sejda.sambox.cos.COSVisitor;
import org.sejda.sambox.cos.IndirectCOSObjectReference;
import org.sejda.sambox.input.ExistingIndirectCOSObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base component providing methods to write the body of a pdf document. This implementation starts from the document
 * trailer and visits the whole document graph replacing {@link COSDictionary} and {@link ExistingIndirectCOSObject}
 * with a newly created {@link IndirectCOSObjectReference}. {@link IndirectCOSObjectReference}s are cached and reused if
 * the corresponding {@link COSDictionary}/{@link ExistingIndirectCOSObject} is found again while exploring the graph.
 * Once all the values of a {@link COSDictionary} or {@link COSArray} have been processed, the {@link COSDictionary}/
 * {@link COSArray} is written as an object, this allows an async implementation to write objects while the writer is
 * still perfoming its algorithm.
 * 
 * @author Andrea Vacondio
 */
abstract class AbstractPdfBodyWriter implements COSVisitor, Closeable
{
    private static final Logger LOG = LoggerFactory.getLogger(AbstractPdfBodyWriter.class);

    private Map<String, Map<COSObjectKey, IndirectCOSObjectReference>> bySourceExistingIndirectToNewXref = new HashMap<>();
    private Map<COSBase, IndirectCOSObjectReference> newObjects = new HashMap<>();
    private Queue<IndirectCOSObjectReference> stack = new LinkedList<>();
    private IndirectReferenceProvider referencesProvider = new IndirectReferenceProvider();

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

    /**
     * writes the given object
     * 
     * @param ref
     * @throws IOException
     */
    abstract void writeObject(IndirectCOSObjectReference ref) throws IOException;

    /**
     * callback to perform once all the objects have been written
     * 
     * @throws IOException
     */
    abstract void onCompletion() throws IOException;

    abstract IndirectObjectsWriter writer();

    @Override
    public void visit(COSArray array) throws IOException
    {
        for (int i = 0; i < array.size(); i++)
        {
            COSBase item = Optional.ofNullable(array.get(i)).orElse(COSNull.NULL);
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
            COSBase item = Optional.ofNullable(value.getItem(key)).orElse(COSNull.NULL);
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

    protected IndirectCOSObjectReference getOrCreateIndirectReferenceFor(COSBase item)
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
                        IndirectCOSObjectReference newRef = referencesProvider
                                .nextReferenceFor(item);
                        LOG.trace(
                                "Created new indirect reference {} replacing the existing one {}",
                                newRef, key);
                        stack.add(newRef);
                        indirectsForSource.put(key, newRef);
                        return newRef;
                    });
        }
        else if (item instanceof COSDictionary)
        {
            return Optional.ofNullable(newObjects.get(item)).orElseGet(() -> {
                IndirectCOSObjectReference newRef = referencesProvider.nextReferenceFor(item);
                LOG.trace("Created new indirect reference '{}' for dictionary item", newRef);
                stack.add(newRef);
                newObjects.put(item, newRef);
                return newRef;
            });
        }
        throw new IllegalArgumentException("Indirect reference is not supperted for type "
                + item.getClass());
    }

    /**
     * @return the component that keeps track of the object numbers used by the body writer and provides the next
     * available reference.
     */
    protected IndirectReferenceProvider referencesProvider()
    {
        return referencesProvider;
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
