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
package org.sejda.sambox.contentstream.operator.color;

import java.io.IOException;
import java.util.List;

import org.sejda.sambox.contentstream.operator.MissingOperandException;
import org.sejda.sambox.contentstream.operator.Operator;
import org.sejda.sambox.contentstream.operator.OperatorProcessor;
import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSNumber;
import org.sejda.sambox.pdmodel.graphics.color.PDColor;
import org.sejda.sambox.pdmodel.graphics.color.PDColorSpace;
import org.sejda.sambox.pdmodel.graphics.color.PDPattern;

/**
 * sc,scn,SC,SCN: Sets the color to use for stroking or non-stroking operations.
 *
 * @author John Hewson
 */
public abstract class SetColor extends OperatorProcessor
{
    @Override
    public void process(Operator operator, List<COSBase> arguments) throws IOException
    {
        PDColorSpace colorSpace = getColorSpace();
        if (!(colorSpace instanceof PDPattern))
        {
            if (arguments.size() < colorSpace.getNumberOfComponents())
            {
                throw new MissingOperandException(operator, arguments);
            }
            if (!checkArrayTypesClass(arguments, COSNumber.class))
            {
                return;
            }
        }
        COSArray array = new COSArray();
        array.addAll(arguments);
        setColor(new PDColor(array, colorSpace));
    }

    /**
     * @return The stroking or non-stroking color value.
     */
    public abstract PDColor getColor();

    /**
     * Sets either the stroking or non-stroking color value.
     * 
     * @param color The stroking or non-stroking color value.
     */
    protected abstract void setColor(PDColor color);

    /**
     * @return The stroking or non-stroking color space.
     */
    public abstract PDColorSpace getColorSpace();
}
