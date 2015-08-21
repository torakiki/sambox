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

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;

import org.sejda.io.CountingWritableByteChannel;
import org.sejda.io.DevNullWritableByteChannel;
import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSBoolean;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSDocument;
import org.sejda.sambox.cos.COSFloat;
import org.sejda.sambox.cos.COSInteger;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSNull;
import org.sejda.sambox.cos.COSStream;
import org.sejda.sambox.cos.COSString;
import org.sejda.sambox.cos.COSVisitor;
import org.sejda.sambox.cos.IndirectCOSObjectReference;
import org.sejda.sambox.input.ExistingIndirectCOSObject;
import org.sejda.sambox.pdmodel.PDPage;

/**
 * Component that tries to predict the size of a document. Added pages are supposed to be coming from an existing
 * document.
 * 
 * @author Andrea Vacondio
 */
public class DocumentSizePredictor implements COSVisitor
{

    private Map<String, IndirectCOSObjectReference> lookupNewRef = new HashMap<>();
    private IndirectReferenceProvider referencesProvider = new IndirectReferenceProvider();
    private SizeCollector sizeCollector = new SizeCollector();
    private Queue<IndirectCOSObjectReference> stack = new LinkedList<>();
    private List<WriteOption> opts;

    public DocumentSizePredictor(WriteOption... options)
    {
        this.opts = Arrays.asList(options);
    }

    public DocumentSizePredictor(DocumentSizePredictor parent)
    {
        this.lookupNewRef.putAll(parent.lookupNewRef);
        this.opts = parent.opts;
    }

    /**
     * Adds the given page to the predicted size.
     * 
     * @param page
     * @throws IOException
     */
    public void addPage(PDPage page) throws IOException
    {
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

        COSDictionary existing = page.getCOSObject();
        COSDictionary copy = new COSDictionary(existing);
        copy.removeItem(COSName.PARENT);
        getOrCreateIndirectReferenceFor(copy);
    }

    @Override
    public void visit(COSDocument value)
    {
        // nothing
    }

    @Override
    public void visit(COSArray array) throws IOException
    {
        COSArray copy = new COSArray();
        for (int i = 0; i < array.size(); i++)
        {
            COSBase item = Optional.ofNullable(array.get(i)).orElse(COSNull.NULL);
            if (item instanceof ExistingIndirectCOSObject)
            {
                IndirectCOSObjectReference ref = getOrCreateIndirectReferenceFor((ExistingIndirectCOSObject) item);
                copy.add(i, ref);
            }
            else
            {
                item.accept(this);
                copy.add(i, item);
            }
        }
        sizeCollector.add(copy);
    }

    @Override
    public void visit(COSDictionary value) throws IOException
    {
        COSDictionary copy = new COSDictionary();
        for (COSName key : value.keySet())
        {
            COSBase item = Optional.ofNullable(value.getItem(key)).orElse(COSNull.NULL);
            if (item instanceof ExistingIndirectCOSObject)
            {
                IndirectCOSObjectReference ref = getOrCreateIndirectReferenceFor((ExistingIndirectCOSObject) item);
                copy.setItem(key, ref);
            }
            else
            {
                item.accept(this);
                copy.setItem(key, item);
            }
        }
        sizeCollector.add(copy);
    }

    @Override
    public void visit(COSBoolean value)
    {
        // nothing
    }

    @Override
    public void visit(COSFloat value)
    {
        // nothing
    }

    @Override
    public void visit(COSInteger value)
    {
        // nothing
    }

    @Override
    public void visit(COSName value)
    {
        // nothing
    }

    @Override
    public void visit(COSNull value)
    {
        // nothing
    }

    @Override
    public void visit(COSStream value) throws IOException
    {
        if (opts.contains(WriteOption.COMPRESS_STREAMS))
        {
            value.addCompression();
        }
        sizeCollector.add(value.getFilteredLength());
        // stream, endstream and 2x CRLF
        sizeCollector.add(19);
        visit((COSDictionary) value);
    }

    @Override
    public void visit(COSString value)
    {
        // nothing
    }

    @Override
    public void visit(IndirectCOSObjectReference value)
    {
        // nothing
    }

    private IndirectCOSObjectReference getOrCreateIndirectReferenceFor(
            ExistingIndirectCOSObject item)
    {

        return Optional.ofNullable(lookupNewRef.get(item.id())).orElseGet(() -> {
            IndirectCOSObjectReference newRef = referencesProvider.nextReferenceFor(item);
            lookupNewRef.put(item.id(), newRef);
            return newRef;
        });
    }

    /**
     * @return the current predicted size
     * @throws IOException
     */
    public long predictedSize() throws IOException
    {
        return sizeCollector.size();
    }

    /**
     * @return the current predicted xref size
     * @throws IOException
     */
    public long predictedXrefTableSize()
    {
        // each entry is 21 bytes plus the xref keyword and section header
        return (21 * (items() + 1)) + 10;
    }

    /**
     * @return the current number of written items
     */
    public long items()
    {
        return sizeCollector.items();
    }

    private static class SizeCollector
    {
        private CountingWritableByteChannel channel = new CountingWritableByteChannel(
                new DevNullWritableByteChannel());
        private IndirectObjectsWriter writer = new IndirectObjectsWriter(new DefaultCOSWriter(
                channel));
        private long size;
        private long items;

        private void add(long bytes)
        {
            this.size += bytes;
        }

        private void add(COSBase item) throws IOException
        {
            items++;
            item.accept(writer);
        }

        private long size() throws IOException
        {
            writer.writer().flush();
            return channel.count() + size;
        }

        private long items()
        {
            return items;
        }
    }

}
