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
     * @return the font resource for the given key object, if it is in the cache.
     */
    PDFont getFont(COSObjectKey key);

    /**
     * @return the color space resource for the given key object, if it is in the cache.
     */
    PDColorSpace getColorSpace(COSObjectKey key);

    /**
     * @return the external graphics state resource for the given key object, if it is in the
     * cache.
     */
    PDExtendedGraphicsState getExtGState(COSObjectKey key);

    /**
     * @return the shading resource for the given key object, if it is in the cache.
     */
    PDShading getShading(COSObjectKey key);

    /**
     * @return the pattern resource for the given key object, if it is in the cache.
     */
    PDAbstractPattern getPattern(COSObjectKey key);

    /**
     * @return the property list resource for the given key object, if it is in the cache.
     */
    PDPropertyList getProperties(COSObjectKey key);

    /**
     * @return the XObject resource for the given key object, if it is in the cache.
     */
    PDXObject getXObject(COSObjectKey key);

    /**
     * Puts the given key font resource in the cache.
     */
    void put(COSObjectKey key, PDFont font);

    /**
     * Puts the given key color space resource in the cache.
     */
    void put(COSObjectKey key, PDColorSpace colorSpace);

    /**
     * Puts the given key extended graphics state resource in the cache.
     */
    void put(COSObjectKey key, PDExtendedGraphicsState extGState);

    /**
     * Puts the given key shading resource in the cache.
     */
    void put(COSObjectKey key, PDShading shading);

    /**
     * Puts the given key pattern resource in the cache.
     */
    void put(COSObjectKey key, PDAbstractPattern pattern);

    /**
     * Puts the given key property list resource in the cache.
     */
    void put(COSObjectKey key, PDPropertyList propertyList);

    /**
     * Puts the given key XObject resource in the cache.
     */
    void put(COSObjectKey key, PDXObject xobject);

    /**
     * Clears the cache
     */
    void clear();
}
