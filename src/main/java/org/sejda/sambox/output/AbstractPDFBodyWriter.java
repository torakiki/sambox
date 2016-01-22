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

import static java.util.Optional.ofNullable;
import static org.sejda.util.RequireUtils.requireNotNullArg;
import static org.sejda.util.RequireUtils.requireState;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSDocument;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSNull;
import org.sejda.sambox.cos.COSStream;
import org.sejda.sambox.cos.COSVisitor;
import org.sejda.sambox.cos.IndirectCOSObjectReference;
import org.sejda.sambox.input.ExistingIndirectCOSObject;

/**
 * Base component providing methods to write the body of a pdf document. This implementation starts from the document
 * trailer and visits the whole document graph updating the {@link PDFWriteContext}. An
 * {@link IndirectCOSObjectReference} is created by the context for each {@link COSDictionary} and
 * {@link ExistingIndirectCOSObject}, if not previously created. Once all the values of a {@link COSDictionary} or
 * {@link COSArray} have been explored, the {@link COSDictionary}/ {@link COSArray} is written as a pdf object, this
 * allows an async implementation to write objects while the body writer is still performing its algorithm.
 * 
 * @author Andrea Vacondio
 */
abstract class AbstractPDFBodyWriter implements COSVisitor, Closeable
{
    private Queue<IndirectCOSObjectReference> stack = new LinkedList<>();
    private PDFWriteContext context;
    private boolean open = true;

    AbstractPDFBodyWriter(PDFWriteContext context)
    {
        requireNotNullArg(context, "Write context cannot be null");
        this.context = context;
    }

    PDFWriteContext context()
    {
        return context;
    }

    /**
     * Writes the body of the given document
     * 
     * @param document
     * @throws IOException
     */
    public void write(COSDocument document) throws IOException
    {
        requireState(open, "The writer is closed");
        document.accept(this);
    }

    @Override
    public void visit(COSDocument document) throws IOException
    {
        for (COSName k : Arrays.asList(COSName.ROOT, COSName.ENCRYPT))
        {
            ofNullable(document.getTrailer().getItem(k)).ifPresent(
                    r -> stack.add(context.createNonStorableInObjectStreamIndirectReferenceFor(r)));
        }
        ofNullable(document.getTrailer().getItem(COSName.INFO))
                .ifPresent(this::createIndirectReferenceIfNeededFor);
        startWriting();
    }

    /**
     * Starts writing whatever has been stacked
     * 
     * @throws IOException
     */
    void startWriting() throws IOException
    {
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

    @Override
    public void visit(COSArray array) throws IOException
    {
        for (int i = 0; i < array.size(); i++)
        {
            COSBase item = ofNullable(array.get(i)).orElse(COSNull.NULL);
            if (item instanceof ExistingIndirectCOSObject || item instanceof COSDictionary)
            {
                createIndirectReferenceIfNeededFor(item);
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
            COSBase item = ofNullable(value.getItem(key)).orElse(COSNull.NULL);
            if (item instanceof ExistingIndirectCOSObject || item instanceof COSDictionary
                    || COSName.THREADS.equals(key))
            {
                createIndirectReferenceIfNeededFor(item);
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
        if (context.hasWriteOption(WriteOption.COMPRESS_STREAMS))
        {
            value.addCompression();
        }
        // with encrypted docs we write length as an indirect ref
        value.indirectLength(context.encryptor.isPresent());
        if (value.indirectLength())
        {
            IndirectCOSObjectReference length = context
                    .createNonStorableInObjectStreamIndirectReferenceFor(COSNull.NULL);
            value.setItem(COSName.LENGTH, length);
            stack.add(length);
        }
        this.visit((COSDictionary) value);

    }

    void createIndirectReferenceIfNeededFor(COSBase item)
    {
        if (!context.hasIndirectReferenceFor(item))
        {
            stack.add(context.getOrCreateIndirectReferenceFor(item));
        }
    }

    @Override
    public void close() throws IOException
    {
        context = null;
        this.open = false;
    }

}
