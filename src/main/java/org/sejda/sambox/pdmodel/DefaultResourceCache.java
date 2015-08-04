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

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.sejda.sambox.cos.COSObjectKey;
import org.sejda.sambox.pdmodel.documentinterchange.markedcontent.PDPropertyList;
import org.sejda.sambox.pdmodel.font.PDFont;
import org.sejda.sambox.pdmodel.graphics.PDXObject;
import org.sejda.sambox.pdmodel.graphics.color.PDColorSpace;
import org.sejda.sambox.pdmodel.graphics.pattern.PDAbstractPattern;
import org.sejda.sambox.pdmodel.graphics.shading.PDShading;
import org.sejda.sambox.pdmodel.graphics.state.PDExtendedGraphicsState;

/**
 * A resource cached based on SoftReference, retains resources until memory pressure causes them to be garbage
 * collected.
 *
 * @author John Hewson
 */
public class DefaultResourceCache implements ResourceCache
{
    private final Map<COSObjectKey, SoftReference<PDFont>> fonts = new HashMap<>();
    private final Map<COSObjectKey, SoftReference<PDColorSpace>> colorSpaces = new HashMap<>();
    private final Map<COSObjectKey, SoftReference<PDXObject>> xobjects = new HashMap<>();
    private final Map<COSObjectKey, SoftReference<PDExtendedGraphicsState>> extGStates = new HashMap<>();
    private final Map<COSObjectKey, SoftReference<PDShading>> shadings = new HashMap<>();
    private final Map<COSObjectKey, SoftReference<PDAbstractPattern>> patterns = new HashMap<>();
    private final Map<COSObjectKey, SoftReference<PDPropertyList>> properties = new HashMap<>();

    @Override
    public PDFont getFont(COSObjectKey key)
    {
        return Optional.ofNullable(fonts.get(key)).map(SoftReference::get).orElse(null);
    }

    @Override
    public void put(COSObjectKey key, PDFont font)
    {
        fonts.put(key, new SoftReference<>(font));
    }

    @Override
    public PDColorSpace getColorSpace(COSObjectKey key)
    {
        return Optional.ofNullable(colorSpaces.get(key)).map(SoftReference::get).orElse(null);
    }

    @Override
    public void put(COSObjectKey key, PDColorSpace colorSpace)
    {
        colorSpaces.put(key, new SoftReference<>(colorSpace));
    }

    @Override
    public PDExtendedGraphicsState getExtGState(COSObjectKey key)
    {
        return Optional.ofNullable(extGStates.get(key)).map(SoftReference::get).orElse(null);
    }

    @Override
    public void put(COSObjectKey key, PDExtendedGraphicsState extGState)
    {
        extGStates.put(key, new SoftReference<>(extGState));
    }

    @Override
    public PDShading getShading(COSObjectKey key)
    {
        return Optional.ofNullable(shadings.get(key)).map(SoftReference::get).orElse(null);
    }

    @Override
    public void put(COSObjectKey key, PDShading shading)
    {
        shadings.put(key, new SoftReference<>(shading));
    }

    @Override
    public PDAbstractPattern getPattern(COSObjectKey key)
    {
        return Optional.ofNullable(patterns.get(key)).map(SoftReference::get).orElse(null);
    }

    @Override
    public void put(COSObjectKey key, PDAbstractPattern pattern)
    {
        patterns.put(key, new SoftReference<>(pattern));
    }

    @Override
    public PDPropertyList getProperties(COSObjectKey key)
    {
        return Optional.ofNullable(properties.get(key)).map(SoftReference::get).orElse(null);
    }

    @Override
    public void put(COSObjectKey key, PDPropertyList propertyList)
    {
        properties.put(key, new SoftReference<>(propertyList));
    }

    @Override
    public PDXObject getXObject(COSObjectKey key)
    {
        return Optional.ofNullable(xobjects.get(key)).map(SoftReference::get).orElse(null);
    }

    @Override
    public void put(COSObjectKey key, PDXObject xobject)
    {
        xobjects.put(key, new SoftReference<>(xobject));
    }

    @Override
    public void clear()
    {
        fonts.clear();
        colorSpaces.clear();
        extGStates.clear();
        patterns.clear();
        properties.clear();
        shadings.clear();
        xobjects.clear();
    }
}
