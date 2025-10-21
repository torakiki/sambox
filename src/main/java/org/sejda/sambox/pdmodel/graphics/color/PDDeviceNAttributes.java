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

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.pdmodel.common.PDDictionaryWrapper;

/**
 * Contains additional information about the components of colour space. Instead of using the
 * alternate color space and tint transform, conforming readers may use custom blending algorithms,
 * along with other information provided in the attributes dictionary.
 *
 * @author Ben Litchfield
 */
public final class PDDeviceNAttributes extends PDDictionaryWrapper
{

    public PDDeviceNAttributes()
    {
        super();
    }

    public PDDeviceNAttributes(COSDictionary attributes)
    {
        super(attributes);
    }

    /**
     * @return a map of colorants and their associated Separation color space.
     * @throws IOException If there is an error reading a color space
     */
    public Map<String, PDSeparation> getColorants() throws IOException
    {
        COSDictionary colorants = getCOSObject().getDictionaryObject(COSName.COLORANTS,
                COSDictionary.class);
        if (nonNull(colorants))
        {
            Map<String, PDSeparation> actuals = new HashMap<>();
            for (COSName name : colorants.keySet())
            {
                var cs = PDColorSpace.create(colorants.getDictionaryObject(name));
                if (cs instanceof PDSeparation)
                {
                    actuals.put(name.getName(), (PDSeparation) cs);
                }
            }
            return actuals;
        }
        return Collections.emptyMap();
    }

    /**
     * @return the DeviceN Process Dictionary, or null if it is missing.
     */
    public PDDeviceNProcess getProcess()
    {
        COSDictionary process = getCOSObject().getDictionaryObject(COSName.PROCESS,
                COSDictionary.class);
        if (nonNull(process))
        {
            return new PDDeviceNProcess(process);
        }
        return null;
    }

    /**
     * @return true if this is an NChannel color space.
     */
    public boolean isNChannel()
    {
        return "NChannel".equals(getCOSObject().getNameAsString(COSName.SUBTYPE));
    }

    /**
     * @param colorants the map of colorants
     */
    public void setColorants(Map<String, PDColorSpace> colorants)
    {
        getCOSObject().setItem(COSName.COLORANTS, ofNullable(colorants).map(c -> {
            COSDictionary dictionary = new COSDictionary();
            c.forEach((name, colorspace) -> dictionary.setItem(COSName.getPDFName(name),
                    colorspace.getCOSObject()));
            return dictionary;
        }).orElse(null));
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder(getCOSObject().getNameAsString(COSName.SUBTYPE));
        sb.append('{');
        PDDeviceNProcess process = getProcess();
        if (process != null)
        {
            sb.append(process);
            sb.append(' ');
        }

        Map<String, PDSeparation> colorants;
        try
        {
            colorants = getColorants();
            sb.append("Colorants{");
            for (Map.Entry<String, PDSeparation> col : colorants.entrySet())
            {
                sb.append('\"');
                sb.append(col.getKey());
                sb.append("\": ");
                sb.append(col.getValue());
                sb.append(' ');
            }
            sb.append('}');
        }
        catch (IOException e)
        {
            sb.append("ERROR");
        }
        sb.append('}');
        return sb.toString();
    }

}
