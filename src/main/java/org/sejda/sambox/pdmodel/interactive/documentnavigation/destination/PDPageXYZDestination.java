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
package org.sejda.sambox.pdmodel.interactive.documentnavigation.destination;

import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSFloat;
import org.sejda.sambox.cos.COSInteger;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSNumber;

/**
 * This represents a destination to a page at an x,y coordinate with a zoom setting. The default x,y,z will be whatever
 * is the current value in the viewer application and are not required.
 *
 * @author Ben Litchfield
 */
public class PDPageXYZDestination extends PDPageDestination
{
    /**
     * The type of this destination.
     */
    protected static final String TYPE = "XYZ";

    public PDPageXYZDestination()
    {
        super();
        array.growToSize(5);
        array.set(1, COSName.getPDFName(TYPE));
    }

    /**
     * Constructor from an existing destination array.
     *
     * @param arr The destination array.
     */
    public PDPageXYZDestination(COSArray arr)
    {
        super(arr);
    }

    /**
     * @return The left x coordinate. A value of -1 implies that the current x-coordinate will be used.
     */
    public int getLeft()
    {
        return array.getInt(2);
    }

    /**
     * Set the left x-coordinate, a value of -1 implies that null will be used and the current x-coordinate will be
     * used.
     * 
     * @param x The left x coordinate.
     */
    public void setLeft(int x)
    {
        array.growToSize(3);
        if (x == -1)
        {
            array.set(2, null);
        }
        else
        {
            array.set(2, COSInteger.get(x));
        }
    }

    /**
     * @return The top y coordinate. A value of -1 implies that the current y-coordinate will be used.
     */
    public int getTop()
    {
        return array.getInt(3);
    }

    /**
     * Set the top y-coordinate, a value of -1 implies that null will be used and the current y-coordinate will be used.
     * 
     * @param y The top ycoordinate.
     */
    public void setTop(int y)
    {
        array.growToSize(4);
        if (y == -1)
        {
            array.set(3, null);
        }
        else
        {
            array.set(3, COSInteger.get(y));
        }
    }

    /**
     *
     * @return The zoom value for the page. Values of 0 or -1 imply that the current zoom will be used
     */
    public float getZoom()
    {
        COSBase obj = array.getObject(4);
        if (obj instanceof COSNumber)
        {
            return ((COSNumber) obj).floatValue();
        }
        return -1;
    }

    /**
     * Set the zoom value for the page, values 0 or -1 imply that the current zoom will be used.
     * 
     * @param zoom The zoom value.
     */
    public void setZoom(float zoom)
    {
        array.growToSize(5);
        if (zoom == -1)
        {
            array.set(4, null);
        }
        else
        {
            array.set(4, new COSFloat(zoom));
        }
    }
}
