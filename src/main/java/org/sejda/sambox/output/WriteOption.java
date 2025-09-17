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

/**
 * Options that can be selected when writing a PDF document.
 *
 * @author Andrea Vacondio
 */
public enum WriteOption
{
    /**
     * Writes the xref data as stream
     */
    XREF_STREAM,
    /**
     * Writes the document using the asynchronous writer as opposed to the default sync one
     */
    ASYNC_BODY_WRITE,
    /**
     * Writes pdf objects using objects stream
     */
    OBJECT_STREAMS,
    /**
     * Adds a Flate filter to the streams if not already there
     */
    COMPRESS_STREAMS,
    /**
     * Does not automatically update metadata modified date and producer when saving
     */
    NO_METADATA_PRODUCER_MODIFIED_DATE_UPDATE,
    /**
     * It creates or updates the document XMP metadata before the document is written.
     * <ul>
     *     <li>Creates: if the document XMP metadata does not exist, it creates a new one based on the info dictionary</li>
     *     <li>Updates: if the document XMP metadata exists, it updates all the values corresponding to the info dictionary (see ISO 32000-2:2020 Chap 14.3.3 Table 349).
     *     If an exception happens while parsing the metadata we fallback to the Creates case</li>
     * </ul>
     */
    UPSERT_DOCUMENT_METADATA_STREAM
}
