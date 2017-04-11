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
package org.sejda.sambox.xref;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.TreeSet;

import org.sejda.sambox.cos.COSObjectKey;

/**
 * Xref table/stream entries.
 * 
 * @author Andrea Vacondio
 *
 */
public class Xref
{
    private HashMap<COSObjectKey, XrefEntry> data = new HashMap<>();

    /**
     * Adds the given entry to the {@link Xref} if an entry with the given object number and generation is not already
     * present.
     * 
     * @param entry
     * @return null if the entry was added. The current entry with the given object number and generation if the entry
     * was already present.
     */
    public XrefEntry addIfAbsent(XrefEntry entry)
    {
        return data.putIfAbsent(entry.key(), entry);
    }

    /**
     * Adds the given entry to the {@link Xref} replacing any entry previously associated to the given object number and
     * generation.
     * 
     * @param entry
     * @return the previous value or null if no entry was previously associated to the given object number and
     * generation.
     */
    public XrefEntry add(XrefEntry entry)
    {
        return data.put(entry.key(), entry);
    }

    /**
     * @param objectKey
     * @return the {@link XrefEntry} for with the given object Number and generation number or null if nothing is found
     */
    public XrefEntry get(COSObjectKey objectKey)
    {
        return data.get(objectKey);
    }

    /**
     * @return an unmodifiable view of the entries in this xref
     */
    public Collection<XrefEntry> values()
    {
        return Collections.unmodifiableCollection(data.values());
    }

    /**
     * @return the highest key in this xref
     */
    public COSObjectKey highestKey()
    {
        return new TreeSet<>(data.keySet()).last();
    }

    /**
     * @param objectKey
     * @return true if a value for the given key is registered to this xref
     */
    public boolean contains(COSObjectKey objectKey)
    {
        return data.containsKey(objectKey);
    }

}
