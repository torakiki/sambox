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
package org.sejda.sambox.pdmodel;

import java.io.IOException;

import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.pdmodel.common.PDNameTreeNode;
import org.sejda.sambox.pdmodel.interactive.documentnavigation.destination.PDDestination;
import org.sejda.sambox.pdmodel.interactive.documentnavigation.destination.PDPageDestination;

/**
 * This class holds all of the name trees that are available at the document level.
 *
 * @author Ben Litchfield
 */
public class PDDestinationNameTreeNode extends PDNameTreeNode<PDPageDestination>
{
    /**
     * Constructor.
     */
    public PDDestinationNameTreeNode()
    {
        super();
    }

    /**
     * Constructor.
     *
     * @param dic The COS dictionary.
     */
    public PDDestinationNameTreeNode(COSDictionary dic)
    {
        super(dic);
    }

    @Override
    protected PDPageDestination convertCOSToPD(COSBase base) throws IOException
    {
        if (base instanceof COSDictionary)
        {
            return (PDPageDestination) PDDestination.create(((COSDictionary) base)
                    .getDictionaryObject(COSName.D));
        }
        return (PDPageDestination) PDDestination.create(base);
    }

    @Override
    protected PDNameTreeNode<PDPageDestination> createChildNode(COSDictionary dic)
    {
        return new PDDestinationNameTreeNode(dic);
    }
}
