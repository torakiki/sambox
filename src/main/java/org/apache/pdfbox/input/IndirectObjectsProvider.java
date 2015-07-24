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
package org.apache.pdfbox.input;

import java.io.Closeable;

import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSObjectKey;
import org.apache.pdfbox.pdmodel.encryption.SecurityHandler;
import org.apache.pdfbox.xref.XrefEntry;

/**
 * Component providing {@link COSBase} objects for given keys. It's used when an indirect reference is asked to resolve
 * to the actual COS object. This component is populated during the xref parsing process by adding {@link XrefEntry}s
 * found in the xref table/stream, it's then initialized with {@link COSParser} to use to parse and retrieve
 * requested objects and the {@link SecurityHandler} required (if any) to decrypt streams and string.
 * 
 * @author Andrea Vacondio
 *
 */
interface IndirectObjectsProvider extends Closeable
{

    /**
     * @param key
     * @return the {@link COSBase} corresponding to the given key.
     */
    public COSBase get(COSObjectKey key);

    /**
     * Signals that the object corresponding to the given key is no longer needed and can be released
     * 
     * @param key
     */
    public void release(COSObjectKey key);

    /**
     * Adds the given xref entry to the {@link org.apache.pdfbox.xref.Xref} if absent
     * 
     * @param entry
     * @return null if the entry was added. The current entry with the given object number and generation if the entry
     * was already present.
     * @see org.apache.pdfbox.xref.Xref#addIfAbsent(XrefEntry)
     */
    public XrefEntry addEntryIfAbsent(XrefEntry entry);

    /**
     * Adds the given xref entry to the {@link org.apache.pdfbox.xref.Xref}
     * 
     * @param entry
     * @return the previous value or null if no entry was previously associated to the given object number and
     * generation.
     * @see org.apache.pdfbox.xref.Xref#add(XrefEntry)
     */
    public XrefEntry addEntry(XrefEntry entry);

    /**
     * Initialize the component with the {@link COSParser} to use to retrieve and parse requested object
     * 
     * @param parser
     * @return this provider
     */
    public IndirectObjectsProvider initializeWith(COSParser parser);

    /**
     * Initialize the component with the {@link SecurityHandler} to decrypt streams and strings.
     * 
     * @param handler
     * @return this provider
     */
    public IndirectObjectsProvider initializeWith(SecurityHandler handler);

    /**
     * @return the unique id for the provider.
     */
    String id();
}
