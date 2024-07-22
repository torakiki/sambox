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

import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.util.Matrix;

/**
 * Resources for an axial shading.
 */
public class PDShadingType2 extends PDShading
{

    public PDShadingType2(COSDictionary shadingDictionary)
    {
        super(shadingDictionary);
    }

    @Override
    public int getShadingType()
    {
        return PDShading.SHADING_TYPE2;
    }

    /**
     * @return the optional Extend values for this shading.
     */
    public COSArray getExtend()
    {
        return getCOSObject().getDictionaryObject(COSName.EXTEND, COSArray.class);
    }

    /**
     * Sets the optional Extend entry for this shading.
     */
    public void setExtend(COSArray extend)
    {
        getCOSObject().setItem(COSName.EXTEND, extend);
    }

    /**
     * @return the optional Domain values for this shading.
     */
    public COSArray getDomain()
    {
        return getCOSObject().getDictionaryObject(COSName.DOMAIN, COSArray.class);
    }

    /**
     * Sets the optional Domain entry for this shading.
     */
    public void setDomain(COSArray domain)
    {
        getCOSObject().setItem(COSName.DOMAIN, domain);
    }

    /**
     * This will get the Coords values for this shading.
     *
     * @return the Coords values for this shading.
     */
    public COSArray getCoords()
    {
        return getCOSObject().getDictionaryObject(COSName.COORDS, COSArray.class);
    }

    /**
     * Sets the Coords entry for this shading.
     */
    public void setCoords(COSArray coords)
    {
        getCOSObject().setItem(COSName.COORDS, coords);
    }

    @Override
    public Paint toPaint(Matrix matrix)
    {
        return new AxialShadingPaint(this, matrix);
    }
}
