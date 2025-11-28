package org.sejda.sambox.pdmodel.xmp;
/*
 * Copyright 2025 Sober Lemur S.r.l.
 * Copyright 2025 Sejda BV
 *
 * Created 27/11/25
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.sejda.sambox.pdmodel.PDDocument;
import org.sejda.sambox.pdmodel.common.PDMetadata;

/**
 * Component providing the document level XMP metadata based on the input document
 *
 * @author Andrea Vacondio
 */
@FunctionalInterface
public interface DocumentXmpMetadataProvider
{
    /**
     * @return Document level metadata in the form of an XMP metadata stream with values populated
     * based on the input document. The xmp stream is newly created if the document doesn't have
     * one. A null value is returned if anything goes wrong.
     */
    PDMetadata xmpMetadataFor(PDDocument document);
}
