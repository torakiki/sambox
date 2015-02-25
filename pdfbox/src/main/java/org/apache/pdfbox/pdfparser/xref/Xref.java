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
package org.apache.pdfbox.pdfparser.xref;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

import org.apache.pdfbox.cos.COSObjectKey;

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
     * Adds the given entry to the {@link Xref} if an entry with the given object number is not already present.
     * 
     * @param entry
     * @return null if the entry was added. The current entry with the given object number if the entry was already
     * present.
     */
    public XrefEntry add(XrefEntry entry)
    {
        return data.putIfAbsent(
                new COSObjectKey(entry.getObjectNumber(), entry.getGenerationNumber()), entry);
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
     * @return and unmodifiable view of the entries in this xref
     */
    public Collection<XrefEntry> values()
    {
        return Collections.unmodifiableCollection(data.values());
    }

    /**
     * @param stmObjKey
     * @return true if a value for the given key is registered to this xref
     */
    public boolean contains(COSObjectKey objectKey)
    {
        return data.containsKey(objectKey);
    }
}
