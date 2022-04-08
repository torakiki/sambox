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
import org.sejda.sambox.cos.COSInteger;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.util.Matrix;

import java.awt.geom.Point2D;

/**
 * Display the page designated by page, with its contents magnified just enough to fit the rectangle
 * specified by the coordinates left, bottom, right, and top entirely within the window both
 * horizontally and vertically
 *
 * @author Ben Litchfield
 */
public class PDPageFitRectangleDestination extends PDPageDestination
{
    /**
     * The type of this destination.
     */
    protected static final String TYPE = "FitR";

    public PDPageFitRectangleDestination()
    {
        array.growToSize(6);
        array.set(1, COSName.getPDFName(TYPE));

    }

    /**
     * Constructor from an existing destination array.
     *
     * @param arr The destination array.
     */
    public PDPageFitRectangleDestination(COSArray arr)
    {
        super(arr);
    }

    /**
     * Get the left x coordinate.  A return value of -1 implies that the current x-coordinate will
     * be used.
     *
     * @return The left x coordinate.
     */
    public int getLeft()
    {
        return array.getInt(2);
    }

    /**
     * Set the left x-coordinate, a value of -1 implies that the current x-coordinate will be used.
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
     * Get the bottom y coordinate.  A return value of -1 implies that the current y-coordinate will
     * be used.
     *
     * @return The bottom y coordinate.
     */
    public int getBottom()
    {
        return array.getInt(3);
    }

    /**
     * Set the bottom y-coordinate, a value of -1 implies that the current y-coordinate will be
     * used.
     *
     * @param y The bottom y coordinate.
     */
    public void setBottom(int y)
    {
        array.growToSize(6);
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
     * Get the right x coordinate.  A return value of -1 implies that the current x-coordinate will
     * be used.
     *
     * @return The right x coordinate.
     */
    public int getRight()
    {
        return array.getInt(4);
    }

    /**
     * Set the right x-coordinate, a value of -1 implies that the current x-coordinate will be
     * used.
     *
     * @param x The right x coordinate.
     */
    public void setRight(int x)
    {
        array.growToSize(6);
        if (x == -1)
        {
            array.set(4, null);
        }
        else
        {
            array.set(4, COSInteger.get(x));
        }
    }

    /**
     * Get the top y coordinate.  A return value of -1 implies that the current y-coordinate will be
     * used.
     *
     * @return The top y coordinate.
     */
    public int getTop()
    {
        return array.getInt(5);
    }

    /**
     * Set the top y-coordinate, a value of -1 implies that the current y-coordinate will be used.
     *
     * @param y The top ycoordinate.
     */
    public void setTop(int y)
    {
        array.growToSize(6);
        if (y == -1)
        {
            array.set(5, null);
        }
        else
        {
            array.set(5, COSInteger.get(y));
        }
    }

    @Override
    public void transform(Matrix transformation)
    {
        Point2D.Float newCoord = transformation.transformPoint(getLeft(), getTop());
        setLeft((int) newCoord.x);
        setTop((int) newCoord.y);
        Point2D.Float newCoord1 = transformation.transformPoint(getRight(), getBottom());
        setRight((int) newCoord1.x);
        setBottom((int) newCoord1.y);
    }
}
