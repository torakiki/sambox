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
package org.sejda.sambox.pdmodel.graphics.pattern;

import java.io.IOException;
import java.io.InputStream;

import org.sejda.sambox.contentstream.PDContentStream;
import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSStream;
import org.sejda.sambox.pdmodel.PDResources;
import org.sejda.sambox.pdmodel.ResourceCache;
import org.sejda.sambox.pdmodel.common.PDRectangle;
import org.sejda.sambox.pdmodel.common.PDStream;

/**
 * A tiling pattern dictionary.
 */
public class PDTilingPattern extends PDAbstractPattern implements PDContentStream
{
    /**
     * paint type 1 = colored tiling pattern.
     */
    public static final int PAINT_COLORED = 1;

    /**
     * paint type 2 = uncolored tiling pattern.
     */
    public static final int PAINT_UNCOLORED = 2;

    /**
     * tiling type 1 = constant spacing.
     */
    public static final int TILING_CONSTANT_SPACING = 1;

    /**
     * tiling type 2 = no distortion.
     */
    public static final int TILING_NO_DISTORTION = 2;

    /**
     * tiling type 3 = constant spacing and faster tiling.
     */
    public static final int TILING_CONSTANT_SPACING_FASTER_TILING = 3;
    private final ResourceCache resourceCache;

    /**
     * Creates a new tiling pattern.
     */
    public PDTilingPattern()
    {
        super(new COSStream());
        getCOSObject().setName(COSName.TYPE, COSName.PATTERN.getName());
        getCOSObject().setInt(COSName.PATTERN_TYPE, PDAbstractPattern.TYPE_TILING_PATTERN);

        // Resources required per PDF specification; when missing, pattern is not displayed in Adobe Reader
        setResources(new PDResources());
        resourceCache = null;
    }

    /**
     * Creates a new tiling pattern from the given COS dictionary.
     *
     * @param resourceDictionary The COSDictionary for this pattern resource.
     */
    public PDTilingPattern(COSDictionary resourceDictionary)
    {
        this(resourceDictionary, null);
    }

    /**
     * Creates a new tiling pattern from the given COS dictionary.
     *
     * @param dictionary    The COSDictionary for this pattern.
     * @param resourceCache The resource cache, may be null
     */
    public PDTilingPattern(COSDictionary dictionary, ResourceCache resourceCache)
    {
        super(dictionary);
        this.resourceCache = resourceCache;
    }

    @Override
    public int getPatternType()
    {
        return PDAbstractPattern.TYPE_TILING_PATTERN;
    }

    @Override
    public void setPaintType(int paintType)
    {
        getCOSObject().setInt(COSName.PAINT_TYPE, paintType);
    }

    public int getPaintType()
    {
        return getCOSObject().getInt(COSName.PAINT_TYPE, 0);
    }

    public void setTilingType(int tilingType)
    {
        getCOSObject().setInt(COSName.TILING_TYPE, tilingType);
    }

    public int getTilingType()
    {
        return getCOSObject().getInt(COSName.TILING_TYPE, 0);
    }

    public void setXStep(float xStep)
    {
        getCOSObject().setFloat(COSName.X_STEP, xStep);
    }

    public float getXStep()
    {
        return getCOSObject().getFloat(COSName.X_STEP, 0);
    }

    public void setYStep(float yStep)
    {
        getCOSObject().setFloat(COSName.Y_STEP, yStep);
    }

    public float getYStep()
    {
        return getCOSObject().getFloat(COSName.Y_STEP, 0);
    }

    public PDStream getContentStream()
    {
        return new PDStream((COSStream) getCOSObject());
    }

    @Override
    public InputStream getContents() throws IOException
    {
        COSDictionary dict = getCOSObject();
        if (dict instanceof COSStream)
        {
            return ((COSStream) getCOSObject()).getUnfilteredStream();
        }
        return null;
    }

    /**
     * This will get the resources for this pattern. This will return null if no resources are
     * available at this level.
     *
     * @return The resources for this pattern.
     */
    @Override
    public PDResources getResources()
    {
        COSDictionary resources = getCOSObject().getDictionaryObject(COSName.RESOURCES,
                COSDictionary.class);
        if (resources != null)
        {
            return new PDResources(resources);
        }
        return null;
    }

    public void setResources(PDResources resources)
    {
        getCOSObject().setItem(COSName.RESOURCES, resources);
    }

    /**
     * An array of four numbers in the form coordinate system (see below), giving the coordinates of
     * the left, bottom, right, and top edges, respectively, of the pattern's bounding box.
     *
     * @return The BBox of the pattern.
     */
    @Override
    public PDRectangle getBBox()
    {
        COSArray array = getCOSObject().getDictionaryObject(COSName.BBOX, COSArray.class);
        if (array != null)
        {
            return new PDRectangle(array);
        }
        return null;
    }

    public void setBBox(PDRectangle bbox)
    {
        if (bbox == null)
        {
            getCOSObject().removeItem(COSName.BBOX);
        }
        else
        {
            getCOSObject().setItem(COSName.BBOX, bbox.getCOSObject());
        }
    }
}
