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

import static java.util.Objects.nonNull;

import java.io.IOException;

import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSString;
import org.sejda.sambox.pdmodel.common.PDDestinationOrAction;

/**
 * This represents a destination in a PDF document.
 *
 * @author Ben Litchfield
 */
public abstract class PDDestination implements PDDestinationOrAction
{

    /**
     * This will create a new destination depending on the type of COSBase that is passed in.
     *
     * @param base The base level object.
     *
     * @return A new destination.
     *
     * @throws IOException If the base cannot be converted to a Destination.
     */
    public static PDDestination create(COSBase base) throws IOException
    {
        if (nonNull(base))
        {
            if (base instanceof COSString)
            {
                return new PDNamedDestination((COSString) base);
            }
            if (base instanceof COSName)
            {
                return new PDNamedDestination((COSName) base);
            }
            if (base instanceof COSArray && ((COSArray) base).size() > 1
                    && ((COSArray) base).getObject(1) instanceof COSName)
            {
                COSArray array = (COSArray) base;
                String typeString = ((COSName) array.getObject(1)).getName();
                if (typeString.equals(PDPageFitDestination.TYPE)
                        || typeString.equals(PDPageFitDestination.TYPE_BOUNDED))
                {
                    return new PDPageFitDestination(array);
                }
                if (typeString.equals(PDPageFitHeightDestination.TYPE)
                        || typeString.equals(PDPageFitHeightDestination.TYPE_BOUNDED))
                {
                    return new PDPageFitHeightDestination(array);
                }
                if (typeString.equals(PDPageFitRectangleDestination.TYPE))
                {
                    return new PDPageFitRectangleDestination(array);
                }
                if (typeString.equals(PDPageFitWidthDestination.TYPE)
                        || typeString.equals(PDPageFitWidthDestination.TYPE_BOUNDED))
                {
                    return new PDPageFitWidthDestination(array);
                }
                if (typeString.equals(PDPageXYZDestination.TYPE))
                {
                    return new PDPageXYZDestination(array);
                }
                throw new IOException("Unknown destination type: " + typeString);
            }
            throw new IOException("Cannot convert " + base + " to a destination");
        }
        return null;
    }

}
