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

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.apache.pdfbox.input.LazyIndirectCOSObject;

/**
 * Component that visits pdf document collecting pdf objects that need to be written, replacing references to them with
 * an {@link IndirectCOSObjectReference} and writing the collected objects using the given {@link PDFWriter}.
 * 
 * @author Andrea Vacondio
 *
 */
class PdfBodyWriter implements COSVisitor
{

    private static final Log LOG = LogFactory.getLog(PdfBodyWriter.class);

    private Map<COSObjectKey, IndirectCOSObjectReference> existingIndirectToNewXref = new HashMap<>();
    private Map<COSBase, IndirectCOSObjectReference> newObjects = new HashMap<>();
    private AtomicInteger objectsCounter = new AtomicInteger(0);
    private PDFWriter writer;

    PdfBodyWriter(PDFWriter writer)
    {
        requireNonNull(writer);
        this.writer = writer;
    }

    private IndirectCOSObjectReference nextReferenceFor(COSBase baseObject)
    {
        return new IndirectCOSObjectReference(objectsCounter.incrementAndGet(), 0, baseObject);
    }

    @Override
    public void visit(COSDocument document) throws IOException
    {
        for (COSName k : Arrays.asList(COSName.ROOT, COSName.INFO, COSName.ENCRYPT))
        {
            COSBase value = document.getTrailer().getItem(k);
            if (value != null)
            {
                IndirectCOSObjectReference ref = getOrCreateIndirectReferenceFor(value);
                value.accept(this);
                document.getTrailer().setItem(k, ref);
                writer.writerObject(ref);
            }
        }
    }

    @Override
    public void visit(COSArray array) throws IOException
    {
        for (int i = 0; i < array.size(); i++)
        {
            COSBase item = array.get(i);
            if (item instanceof LazyIndirectCOSObject || item instanceof COSDictionary)
            {
                IndirectCOSObjectReference ref = getOrCreateIndirectReferenceFor(item);
                array.set(i, ref);
                item.accept(this);
                writer.writerObject(ref);
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
            if (item instanceof LazyIndirectCOSObject || item instanceof COSDictionary)
            {
                IndirectCOSObjectReference ref = getOrCreateIndirectReferenceFor(item);
                value.setItem(key, ref);
                item.accept(this);
                writer.writerObject(ref);
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
        if (item instanceof LazyIndirectCOSObject)
        {
            COSObjectKey key = ((LazyIndirectCOSObject) item).key();

            return Optional.ofNullable(existingIndirectToNewXref.get(key)).orElseGet(
                    () -> {
                        IndirectCOSObjectReference newRef = nextReferenceFor(item);
                        LOG.trace("Created new indirect reference '" + newRef
                                + "' replacing the existing one: " + key);
                        existingIndirectToNewXref.put(key, newRef);
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
        throw new IllegalArgumentException("Idirect reference is not supperted for type "
                + item.getClass());
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
        existingIndirectToNewXref.clear();
        newObjects.clear();
    }
}
