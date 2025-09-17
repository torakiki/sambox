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
package org.sejda.sambox.pdmodel.documentinterchange.taggedpdf;

import static java.util.Optional.ofNullable;

import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSNull;
import org.sejda.sambox.cos.COSObjectable;
import org.sejda.sambox.pdmodel.graphics.color.PDGamma;

/**
 * An object for four colours.
 *
 * @author Johannes Koch
 */
public class PDFourColours implements COSObjectable
{

    private final COSArray array;

    public PDFourColours()
    {
        this.array = new COSArray(COSNull.NULL, COSNull.NULL, COSNull.NULL, COSNull.NULL);
    }

    public PDFourColours(COSArray array)
    {
        this.array = array;
        // ensure that array has 4 items
        if (this.array.size() < 4)
        {
            for (int i = (this.array.size() - 1); i < 4; i++)
            {
                this.array.add(COSNull.NULL);
            }
        }
    }

    /**
     * @return the colour for the before edge
     */
    public PDGamma getBeforeColour()
    {
        return this.getColourByIndex(0);
    }

    /**
     * @param colour the colour for the before edge
     */
    public void setBeforeColour(PDGamma colour)
    {
        this.setColourByIndex(0, colour);
    }

    /**
     * @return the colour for the after edge
     */
    public PDGamma getAfterColour()
    {
        return this.getColourByIndex(1);
    }

    /**
     * @param colour the colour for the after edge
     */
    public void setAfterColour(PDGamma colour)
    {
        this.setColourByIndex(1, colour);
    }

    /**
     * @return the colour for the start edge
     */
    public PDGamma getStartColour()
    {
        return this.getColourByIndex(2);
    }

    /**
     * @param colour the colour for the start edge
     */
    public void setStartColour(PDGamma colour)
    {
        this.setColourByIndex(2, colour);
    }

    /**
     * @return the colour for the end edge
     */
    public PDGamma getEndColour()
    {
        return this.getColourByIndex(3);
    }

    /**
     * @param colour the colour for the end edge
     */
    public void setEndColour(PDGamma colour)
    {
        this.setColourByIndex(3, colour);
    }

    @Override
    public COSArray getCOSObject()
    {
        return this.array;
    }

    /**
     * @param index edge index
     * @return the colour by edge index
     */
    private PDGamma getColourByIndex(int index)
    {
        COSBase item = this.array.getObject(index);
        if (item instanceof COSArray array)
        {
            return new PDGamma(array);
        }
        return null;
    }

    /**
     * @param index  the edge index
     * @param colour the colour to set
     */
    private void setColourByIndex(int index, PDGamma colour)
    {
        this.array.set(index,
                ofNullable(colour).map(c -> (COSBase) c.getCOSObject()).orElse(COSNull.NULL));
    }

}
