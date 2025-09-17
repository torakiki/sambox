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
package org.sejda.sambox.pdmodel.graphics.color;

import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSFloat;
import org.sejda.sambox.cos.COSNumber;
import org.sejda.sambox.cos.COSObjectable;

/**
 * A gamma array, or collection of three floating point parameters used for color operations.
 *
 * @author Ben Litchfield
 */
public final class PDGamma implements COSObjectable
{
    private final COSArray values;

    /**
     * Creates a new gamma. Defaults all values to 0, 0, 0.
     */
    public PDGamma()
    {
        values = new COSArray(new COSFloat(0.0f), new COSFloat(0.0f), new COSFloat(0.0f));
    }

    /**
     * @param array the array containing the XYZ values
     */
    public PDGamma(COSArray array)
    {
        values = array;
    }

    @Override
    public COSArray getCOSObject()
    {
        return values;
    }

    /**
     * Returns the r value of the tristimulus.
     *
     * @return the R value.
     */
    public float getR()
    {
        return ((COSNumber) values.get(0)).floatValue();
    }

    /**
     * Sets the r value of the tristimulus.
     *
     * @param r the r value for the tristimulus
     */
    public void setR(float r)
    {
        values.set(0, new COSFloat(r));
    }

    /**
     * Returns the g value of the tristimulus.
     *
     * @return the g value
     */
    public float getG()
    {
        return ((COSNumber) values.get(1)).floatValue();
    }

    /**
     * Sets the g value of the tristimulus.
     *
     * @param g the g value for the tristimulus
     */
    public void setG(float g)
    {
        values.set(1, new COSFloat(g));
    }

    /**
     * Returns the b value of the tristimulus.
     *
     * @return the B value
     */
    public float getB()
    {
        return ((COSNumber) values.get(2)).floatValue();
    }

    /**
     * Sets the b value of the tristimulus.
     *
     * @param b he b value for the tristimulus
     */
    public void setB(float b)
    {
        values.set(2, new COSFloat(b));
    }
}
