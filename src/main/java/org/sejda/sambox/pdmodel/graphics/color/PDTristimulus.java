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
 * A tristimulus, or collection of three floating point parameters used for color operations.
 *
 * @author Ben Litchfield
 */
public final class PDTristimulus implements COSObjectable
{
    private final COSArray values;

    /**
     * Default constructor all values to 0, 0, 0.
     */
    public PDTristimulus()
    {
        values = new COSArray(new COSFloat(0.0f), new COSFloat(0.0f), new COSFloat(0.0f));
    }

    /**
     * @param array the array containing the XYZ values
     */
    public PDTristimulus(COSArray array)
    {
        values = array;
    }

    /**
     * @param array the array containing the XYZ values
     */
    public PDTristimulus(float[] array)
    {
        values = new COSArray();
        for (int i = 0; i < array.length && i < 3; i++)
        {
            values.add(new COSFloat(array[i]));
        }
    }

    @Override
    public COSArray getCOSObject()
    {
        return values;
    }

    public float getX()
    {
        return values.getObject(0, COSNumber.class).floatValue();
    }

    public void setX(float x)
    {
        values.set(0, new COSFloat(x));
    }

    public float getY()
    {
        return values.getObject(1, COSNumber.class).floatValue();
    }

    public void setY(float y)
    {
        values.set(1, new COSFloat(y));
    }

    public float getZ()
    {
        return values.getObject(2, COSNumber.class).floatValue();
    }

    public void setZ(float z)
    {
        values.set(2, new COSFloat(z));
    }
}
