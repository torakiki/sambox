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
package org.sejda.sambox.pdmodel.graphics.form;

import java.io.IOException;

import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.pdmodel.PDResources;
import org.sejda.sambox.pdmodel.common.PDDictionaryWrapper;
import org.sejda.sambox.pdmodel.graphics.color.PDColorSpace;

/**
 * Transparency group attributes.
 * 
 * @author Kühn & Weyh Software, GmbH
 */
public final class PDTransparencyGroupAttributes extends PDDictionaryWrapper
{
    private PDColorSpace colorSpace;

    /**
     * Creates a group object with /Transparency subtype entry.
     */
    public PDTransparencyGroupAttributes()
    {
        getCOSObject().setItem(COSName.TYPE, COSName.GROUP);
        getCOSObject().setItem(COSName.S, COSName.TRANSPARENCY);
    }

    public PDTransparencyGroupAttributes(COSDictionary dictionary)
    {
        super(dictionary);
    }

    public PDColorSpace getColorSpace() throws IOException
    {
        return getColorSpace(null);
    }

    /**
     * Returns the group color space or null if it isn't defined.
     * 
     * @return the group color space.
     * @throws IOException
     */
    public PDColorSpace getColorSpace(PDResources resources) throws IOException
    {
        if (colorSpace == null && getCOSObject().containsKey(COSName.CS))
        {
            COSBase dictionaryObject = getCOSObject().getDictionaryObject(COSName.CS);
            if (dictionaryObject != null)
            {
                colorSpace = PDColorSpace.create(dictionaryObject, resources);
            }
        }
        return colorSpace;
    }

    /**
     * @return true if this group is isolated. Isolated groups begin with the fully transparent image, non-isolated
     * begin with the current backdrop.
     */
    public boolean isIsolated()
    {
        return getCOSObject().getBoolean(COSName.I, false);
    }

    public void setIsolated()
    {
        getCOSObject().setBoolean(COSName.I, true);
    }

    /**
     * @return true if this group is a knockout. A knockout group blends with original backdrop, a non-knockout group
     * blends with the current backdrop.
     */
    public boolean isKnockout()
    {
        return getCOSObject().getBoolean(COSName.K, false);
    }

    public void setKnockout()
    {
        getCOSObject().setBoolean(COSName.K, true);
    }
}
