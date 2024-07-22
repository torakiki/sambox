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
package org.sejda.sambox.pdmodel.graphics.shading;

import java.awt.Paint;
import java.awt.geom.AffineTransform;

import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSFloat;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.util.Matrix;

/**
 * Resources for a function based shading.
 */
public class PDShadingType1 extends PDShading
{
    public PDShadingType1(COSDictionary shadingDictionary)
    {
        super(shadingDictionary);
    }

    @Override
    public int getShadingType()
    {
        return PDShading.SHADING_TYPE1;
    }

    /**
     * @return the optional Matrix of a function based shading.
     */
    public Matrix getMatrix()
    {
        return Matrix.createMatrix(getCOSObject().getDictionaryObject(COSName.MATRIX));
    }

    /**
     * Sets the optional Matrix entry for the function based shading.
     *
     * @param transform the transformation matrix
     */
    public void setMatrix(AffineTransform transform)
    {
        COSArray matrix = new COSArray();
        double[] values = new double[6];
        transform.getMatrix(values);
        for (double v : values)
        {
            matrix.add(new COSFloat((float) v));
        }
        getCOSObject().setItem(COSName.MATRIX, matrix);
    }

    /**
     * @return the optional Domain values of a function based shading.
     */
    public COSArray getDomain()
    {
        return getCOSObject().getDictionaryObject(COSName.DOMAIN, COSArray.class);
    }

    /**
     * Sets the optional Domain entry for the function based shading.
     */
    public void setDomain(COSArray domain)
    {
        getCOSObject().setItem(COSName.DOMAIN, domain);
    }

    @Override
    public Paint toPaint(Matrix matrix)
    {
        return new Type1ShadingPaint(this, matrix);
    }
}
