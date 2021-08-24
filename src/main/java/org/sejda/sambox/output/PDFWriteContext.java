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

import static java.util.Optional.ofNullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Function;

import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSObjectKey;
import org.sejda.sambox.cos.IndirectCOSObjectIdentifier;
import org.sejda.sambox.cos.IndirectCOSObjectReference;
import org.sejda.sambox.cos.NonStorableInObjectStreams;
import org.sejda.sambox.encryption.GeneralEncryptionAlgorithm;
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
    private Map<IndirectCOSObjectIdentifier, IndirectCOSObjectReference> lookupNewRef = new ConcurrentHashMap<>();
    private List<WriteOption> opts;
    private SortedMap<Long, XrefEntry> written = new ConcurrentSkipListMap<>();
    public final Optional<GeneralEncryptionAlgorithm> encryptor;

    PDFWriteContext(GeneralEncryptionAlgorithm encryptor, WriteOption... options)
    {
        this.encryptor = ofNullable(encryptor);
        this.opts = Arrays.asList(options);
        this.referencesProvider = new IndirectReferenceProvider();
    }

    PDFWriteContext(long highestExistingReferenceNumber, GeneralEncryptionAlgorithm encryptor,
            WriteOption... options)
    {
        this.encryptor = ofNullable(encryptor);
        this.encryptor.ifPresent(e -> LOG.debug("Encryptor: {}", e));
        this.opts = Arrays.asList(options);
        this.referencesProvider = new IndirectReferenceProvider(highestExistingReferenceNumber);
        LOG.debug("PDFWriteContext created with highest object reference number {}",
                highestExistingReferenceNumber);
    }

    /**
     * Creates a new {@link IndirectCOSObjectReference} for the given item. We store the association between the item id
     * and the reference so that if we meet the same id later, we can retrieve the reference that was previously
     * written.
     * 
     * @param item
     * @return the created reference
     */
    IndirectCOSObjectReference createIndirectReferenceFor(COSBase item)
    {
        return createNewReference(item, referencesProvider::nextReferenceFor);
    }

    /**
     * Creates a new {@link NonStorableInObjectStreams} for the given item. We store the association between the item id
     * and the reference so that if we meet the same id later, we can retrieve the reference that was previously
     * written.
     * 
     * @param item
     * @return the created reference
     */
    IndirectCOSObjectReference createNonStorableInObjectStreamIndirectReferenceFor(COSBase item)
    {
        return createNewReference(item,
                referencesProvider::nextNonStorableInObjectStreamsReferenceFor);
    }

    /**
     * Creates an empty {@link NonStorableInObjectStreams}
     * 
     * @return the created reference
     */
    IndirectCOSObjectReference createNonStorableInObjectStreamIndirectReference()
    {
        return referencesProvider.nextNonStorableInObjectStreamsReference();
    }

    private IndirectCOSObjectReference createNewReference(COSBase item,
            Function<COSBase, IndirectCOSObjectReference> supplier)
    {
        IndirectCOSObjectReference newRef = supplier.apply(item);
        LOG.trace("Created new indirect reference {} for {}", newRef, item.id());
        // It's not an existing indirect object
        if (!(item instanceof ExistingIndirectCOSObject))
        {
            // it's a new COSBase
            item.idIfAbsent(new IndirectCOSObjectIdentifier(newRef.xrefEntry().key(), contextId));
        }
        // we store the ID if the item reference can be reused. At this point we should always have an id since we get
        // here only with ExistingIndirectCOSObject, COSDictionary or IndirectCOSObject
        if (item.hasId())
        {
            lookupNewRef.put(item.id(), newRef);
        }
        else
        {
            LOG.warn("Unexpected indirect reference for {}", item);
        }
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
        return ofNullable(getIndirectReferenceFor(item))
                .orElseGet(() -> createIndirectReferenceFor(item));
    }

    /**
     * @param item
     * @return the {@link IndirectCOSObjectReference} for the given item or null if an
     * {@link IndirectCOSObjectReference} has not been created for the item or the item has no id.
     */
    IndirectCOSObjectReference getIndirectReferenceFor(COSBase item)
    {
        if (item.hasId())
        {
            return lookupNewRef.get(item.id());
        }
        return null;
    }

    /**
     * adds the reference to the context. When later queried, the context will return the existing indirect reference an
     * no new reference will be created. This is used during incremental updates when we don't want to create new
     * references for existing objects
     * 
     * @param existing
     */
    void addExistingReference(ExistingIndirectCOSObject existing)
    {
        lookupNewRef.put(existing.id(),
                new IndirectCOSObjectReference(existing.id().objectIdentifier.objectNumber(),
                        existing.id().objectIdentifier.generation(), null));
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
    XrefEntry addWritten(XrefEntry entry)
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
     * @return the highest object number that this context knows
     */
    long highestObjectNumber()
    {
        if (written.isEmpty())
        {
            return referencesProvider.referencesCounter.get();
        }
        return Math.max(written.lastKey(), referencesProvider.referencesCounter.get());
    }

    /**
     * @param objectNumber
     * @return the written entry with the given object number if any, null otherwise.
     */
    XrefEntry getWritten(Long objectNumber)
    {
        return written.get(objectNumber);
    }

    /**
     * @return a list of contiguous groups of written object numbers
     */
    List<List<Long>> getWrittenContiguousGroups()
    {
        List<List<Long>> contiguous = new ArrayList<>();
        if (!written.isEmpty())
        {
            LinkedList<Long> group = new LinkedList<>();
            contiguous.add(group);
            for (Long current : written.keySet())
            {
                if (group.isEmpty() || current == group.getLast() + 1)
                {
                    group.addLast(current);
                }
                else
                {
                    group = new LinkedList<>(Arrays.asList(current));
                    contiguous.add(group);
                }
            }
        }
        return contiguous;
    }

    /**
     * Informs the context of the object is about the written
     * 
     * @param key
     */
    void writing(COSObjectKey key)
    {
        encryptor.ifPresent(e -> e.setCurrentCOSObjectKey(key));
    }
}
