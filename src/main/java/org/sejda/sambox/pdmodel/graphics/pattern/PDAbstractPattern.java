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

import java.awt.geom.AffineTransform;
import java.io.IOException;

import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSFloat;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.pdmodel.ResourceCache;
import org.sejda.sambox.pdmodel.common.PDDictionaryWrapper;
import org.sejda.sambox.util.Matrix;

/**
 * A Pattern dictionary from a page's resources.
 */
public abstract class PDAbstractPattern extends PDDictionaryWrapper
{
    /** Tiling pattern type. */
    public static final int TYPE_TILING_PATTERN = 1;

    /**
     * Shading pattern type.
     */
    public static final int TYPE_SHADING_PATTERN = 2;

    /**
     * Create the correct PD Model pattern based on the COS base pattern.
     *
     * @param resourceDictionary the COS pattern dictionary
     * @return the newly created pattern resources object
     * @throws IOException If we are unable to create the PDPattern object.
     */
    public static PDAbstractPattern create(COSDictionary resourceDictionary) throws IOException
    {
        return create(resourceDictionary, null);
    }

    /**
     * Create the correct PD Model pattern based on the COS base pattern.
     *
     * @param resourceDictionary the COS pattern dictionary
     * @param resourceCache      the resource cache, may be null, useful for tiling patterns.
     * @return the newly created pattern object
     * @throws IllegalArgumentException If we are unable to create the PDPattern object.
     */
    public static PDAbstractPattern create(COSDictionary resourceDictionary,
            ResourceCache resourceCache) throws IllegalArgumentException
    {
        int patternType = resourceDictionary.getInt(COSName.PATTERN_TYPE, 0);
        return switch (patternType)
        {
            case TYPE_TILING_PATTERN -> new PDTilingPattern(resourceDictionary, resourceCache);
            case TYPE_SHADING_PATTERN -> new PDShadingPattern(resourceDictionary);
            default -> throw new IllegalArgumentException("Unknown pattern type " + patternType);
        };
    }

    public PDAbstractPattern()
    {
        super(COSDictionary.of(COSName.TYPE, COSName.PATTERN));
    }

    public PDAbstractPattern(COSDictionary resourceDictionary)
    {
        super(resourceDictionary);
    }


    public void setPaintType(int paintType)
    {
        getCOSObject().setInt(COSName.PAINT_TYPE, paintType);
    }

    public String getType()
    {
        return COSName.PATTERN.getName();
    }

    public void setPatternType(int patternType)
    {
        getCOSObject().setInt(COSName.PATTERN_TYPE, patternType);
    }

    public abstract int getPatternType();

    /**
     * @return the pattern matrix, or the identity matrix is none is available.
     */
    public Matrix getMatrix()
    {
        return Matrix.createMatrix(getCOSObject().getDictionaryObject(COSName.MATRIX));
    }

    /**
     * Sets the optional Matrix entry for the Pattern.
     * @param transform the transformation matrix
     */
    public void setMatrix(AffineTransform transform)
    {
        COSArray matrix = new COSArray();
        double[] values = new double[6];
        transform.getMatrix(values);
        for (double v : values)
        {
            matrix.add(new COSFloat((float)v));
        }
        getCOSObject().setItem(COSName.MATRIX, matrix);
    }

}
