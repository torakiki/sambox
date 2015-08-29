/*
 * Created on 27/ago/2015
 * Copyright 2010 by Andrea Vacondio (andrea.vacondio@gmail.com).
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * 
 * http://www.apache.org/licenses/LICENSE-2.0 
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License. 
 */
package org.sejda.sambox.output;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.IndirectCOSObjectReference;
import org.sejda.sambox.input.ExistingIndirectCOSObject;
import org.sejda.sambox.xref.XrefEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Context that contains and keeps track of all the information needed during the process of writing a PDF document. It
 * creates indirect references for {@link COSBase} instances and provide lookup methods to be able to retrieve these
 * data and know what's the indirect reference created by the context for a given {@link COSBase}. I keeps track of what
 * object numbers have been written.
 * 
 * @author Andrea Vacondio
 */
class PDFWriteContext
{
    private static final Logger LOG = LoggerFactory.getLogger(PDFWriteContext.class);

    private String contextId = UUID.randomUUID().toString();
    private IndirectReferenceProvider referencesProvider = new IndirectReferenceProvider();
    private Map<String, IndirectCOSObjectReference> lookupNewRef = new ConcurrentHashMap<>();
    private List<WriteOption> opts;
    private SortedMap<Long, XrefEntry> written = new ConcurrentSkipListMap<>();

    PDFWriteContext(WriteOption... options)
    {
        this.opts = Arrays.asList(options);
    }

    /**
     * Creates a new {@link IndirectCOSObjectReference} for the given item
     * 
     * @param item
     * @return the created reference
     */
    IndirectCOSObjectReference createIndirectReferenceFor(COSBase item)
    {
        // It's an existing indirect object
        if (item instanceof ExistingIndirectCOSObject)
        {
            ExistingIndirectCOSObject existingItem = (ExistingIndirectCOSObject) item;
            IndirectCOSObjectReference newRef = referencesProvider.nextReferenceFor(existingItem);
            LOG.trace("Created new indirect reference {} replacing the existing one {}", newRef,
                    existingItem.key());
            lookupNewRef.put(existingItem.id(), newRef);
            return newRef;

        }
        // it's a new COSBase
        IndirectCOSObjectReference newRef = referencesProvider.nextReferenceFor(item);
        LOG.trace("Created new indirect reference '{}' ", newRef);
        item.idIfAbsent(String.format("%s %d", contextId, newRef.xrefEntry().key().getNumber()));
        lookupNewRef.put(item.id(), newRef);
        return newRef;
    }

    /**
     * Creates a new {@link IndirectCOSObjectReference} for the given item if it has not been created before, it returns
     * the already existing reference otherwise.
     * 
     * @param item
     * @return the reference
     */
    IndirectCOSObjectReference getOrCreateIndirectReferenceFor(COSBase item)
    {
        if (hasIndirectReferenceFor(item))
        {
            // I met it already
            return lookupNewRef.get(item.id());
        }
        return createIndirectReferenceFor(item);
    }

    /**
     * @param item
     * @return the {@link IndirectCOSObjectReference} for the given item or null if an
     * {@link IndirectCOSObjectReference} has not been created for the item.
     */
    IndirectCOSObjectReference getIndirectReferenceFor(COSBase item)
    {
        return lookupNewRef.get(item.id());
    }

    /**
     * @param item
     * @return true if the given item has been added to the context and an indirect reference created for it.
     */
    boolean hasIndirectReferenceFor(COSBase item)
    {
        return item.hasId() && lookupNewRef.containsKey(item.id());
    }

    /**
     * @param opt
     * @return true if the context has the given write option
     */
    boolean hasWriteOption(WriteOption opt)
    {
        return opts.contains(opt);
    }

    /**
     * @return number of written objects so far.
     */
    int written()
    {
        return written.size();
    }

    /**
     * 
     * @param entry
     * @return true if the given entry has been already written
     */
    boolean hasWritten(XrefEntry entry)
    {
        return written.containsKey(entry.getObjectNumber());
    }

    /**
     * Adds an entry to the list of the written entries
     * 
     * @param entry
     * @return the previous value if an entry with the same object number has been already written, null otherwise.
     */
    XrefEntry putWritten(XrefEntry entry)
    {
        return written.put(entry.getObjectNumber(), entry);
    }

    /**
     * @return the written entry with the highest object number
     */
    XrefEntry highestWritten()
    {
        return written.get(written.lastKey());
    }

    /**
     * @return the written entry with the lowest object number
     */
    XrefEntry lowestWritten()
    {
        return written.get(written.firstKey());
    }

    /**
     * @param objectNumber
     * @return the written entry with the given object number if any, null otherwise.
     */
    XrefEntry getWritten(Long objectNumber)
    {
        return written.get(objectNumber);
    }
}
