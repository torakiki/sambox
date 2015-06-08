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
import org.apache.pdfbox.xref.Xref;
import org.apache.pdfbox.xref.XrefEntry;

/**
 * Component providing {@link COSBase} objects for given keys. It's used when an indirect reference is asked to resolve
 * to the actual COS object. This component is populated during the xref parsing process by adding {@link XrefEntry}s
 * found in the xref table/stream, it's then initialized with {@link BaseCOSParser} to use to parse and retrieve
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
     * Adds the given xref entry to the {@link Xref} if absent
     * 
     * @param entry
     */
    public void addEntryIfAbsent(XrefEntry entry);

    /**
     * Adds the given xref entry to the {@link Xref}
     * 
     * @param entry
     */
    public void addEntry(XrefEntry entry);

    /**
     * Initialize the component with the {@link BaseCOSParser} to use to retrieve and parse requested object
     * 
     * @param parser
     */
    public void initializeWith(BaseCOSParser parser);

    /**
     * Initialize the component with the {@link SecurityHandler} to decrypt streams and strings.
     * 
     * @param handler
     */
    public void initializeWith(SecurityHandler handler);
}
