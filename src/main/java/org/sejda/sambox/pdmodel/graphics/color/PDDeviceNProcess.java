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
import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A DeviceN Process Dictionary
 *
 * @author John Hewson
 */
public class PDDeviceNProcess
{
    private final COSDictionary dictionary;

    /**
     * Creates a new DeviceN Process Dictionary.
     */
    public PDDeviceNProcess()
    {
        dictionary = new COSDictionary();
    }

    /**
     * Creates a new  DeviceN Process Dictionary from the given attributes.
     *
     * @param attributes a DeviceN attributes dictionary
     */
    public PDDeviceNProcess(COSDictionary attributes)
    {
        dictionary = attributes;
    }

    /**
     * Returns the underlying COS dictionary.
     *
     * @return the underlying COS dictionary.
     */
    public COSDictionary getCOSDictionary()
    {
        return dictionary;
    }

    /**
     * Returns the process color space
     *
     * @return the process color space
     * @throws IOException if the color space cannot be read
     */
    public PDColorSpace getColorSpace() throws IOException
    {
        COSBase cosColorSpace = dictionary.getDictionaryObject(COSName.COLORSPACE);
        if (cosColorSpace == null)
        {
            return null; // TODO: return a default?
        }
        return PDColorSpace.create(cosColorSpace);
    }

    /**
     * Returns the names of the color components.
     *
     * @return the names of the color components
     */
    public List<String> getComponents()
    {
        List<String> components = new ArrayList<String>();
        COSArray cosComponents = dictionary.getDictionaryObject(COSName.COMPONENTS, COSArray.class);
        if (cosComponents == null)
        {
            return components;
        }
        for (COSBase name : cosComponents)
        {
            components.add(((COSName) name).getName());
        }
        return components;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder("Process{");
        try
        {
            sb.append(getColorSpace());
            for (String component : getComponents())
            {
                sb.append(" \"");
                sb.append(component);
                sb.append('\"');
            }
        }
        catch (IOException e)
        {
            sb.append("ERROR");
        }
        sb.append('}');
        return sb.toString();
    }

}
