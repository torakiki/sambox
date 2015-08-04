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

package org.sejda.sambox.pdmodel;

import java.io.IOException;

import org.sejda.sambox.cos.COSObjectKey;
import org.sejda.sambox.pdmodel.documentinterchange.markedcontent.PDPropertyList;
import org.sejda.sambox.pdmodel.font.PDFont;
import org.sejda.sambox.pdmodel.graphics.PDXObject;
import org.sejda.sambox.pdmodel.graphics.color.PDColorSpace;
import org.sejda.sambox.pdmodel.graphics.pattern.PDAbstractPattern;
import org.sejda.sambox.pdmodel.graphics.shading.PDShading;
import org.sejda.sambox.pdmodel.graphics.state.PDExtendedGraphicsState;

/**
 * A document-wide cache for page resources.
 *
 * @author John Hewson
 */
public interface ResourceCache
{
    /**
     * Returns the font resource for the given key object, if it is in the cache.
     */
    PDFont getFont(COSObjectKey key) throws IOException;

    /**
     * Returns the color space resource for the given key object, if it is in the cache.
     */
    PDColorSpace getColorSpace(COSObjectKey key) throws IOException;

    /**
     * Returns the external graphics state resource for the given key object, if it is in the cache.
     */
    PDExtendedGraphicsState getExtGState(COSObjectKey key);

    /**
     * Returns the shading resource for the given key object, if it is in the cache.
     */
    PDShading getShading(COSObjectKey key) throws IOException;

    /**
     * Returns the pattern resource for the given key object, if it is in the cache.
     */
    PDAbstractPattern getPattern(COSObjectKey key) throws IOException;

    /**
     * Returns the property list resource for the given key object, if it is in the cache.
     */
    PDPropertyList getProperties(COSObjectKey key);

    /**
     * Returns the XObject resource for the given key object, if it is in the cache.
     */
    PDXObject getXObject(COSObjectKey key) throws IOException;

    /**
     * Puts the given key font resource in the cache.
     */
    void put(COSObjectKey key, PDFont font) throws IOException;

    /**
     * Puts the given key color space resource in the cache.
     */
    void put(COSObjectKey key, PDColorSpace colorSpace) throws IOException;

    /**
     * Puts the given key extended graphics state resource in the cache.
     */
    void put(COSObjectKey key, PDExtendedGraphicsState extGState);

    /**
     * Puts the given key shading resource in the cache.
     */
    void put(COSObjectKey key, PDShading shading) throws IOException;

    /**
     * Puts the given key pattern resource in the cache.
     */
    void put(COSObjectKey key, PDAbstractPattern pattern) throws IOException;

    /**
     * Puts the given key property list resource in the cache.
     */
    void put(COSObjectKey key, PDPropertyList propertyList);

    /**
     * Puts the given key XObject resource in the cache.
     */
    void put(COSObjectKey key, PDXObject xobject) throws IOException;

    /**
     * Clears the cache
     */
    void clear();
}
