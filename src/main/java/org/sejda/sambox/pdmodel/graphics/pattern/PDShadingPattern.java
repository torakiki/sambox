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
package org.sejda.sambox.pdmodel.graphics.pattern;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;

import java.io.IOException;

import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.pdmodel.graphics.shading.PDShading;
import org.sejda.sambox.pdmodel.graphics.state.PDExtendedGraphicsState;

/**
 * A shading pattern dictionary.
 *
 */
public class PDShadingPattern extends PDAbstractPattern
{
    private PDExtendedGraphicsState extendedGraphicsState;
    private PDShading shading;

    public PDShadingPattern()
    {
        getCOSObject().setInt(COSName.PATTERN_TYPE, PDAbstractPattern.TYPE_SHADING_PATTERN);
    }

    public PDShadingPattern(COSDictionary resourceDictionary)
    {
        super(resourceDictionary);
    }

    @Override
    public int getPatternType()
    {
        return PDAbstractPattern.TYPE_SHADING_PATTERN;
    }

    public PDExtendedGraphicsState getExtendedGraphicsState()
    {
        if (extendedGraphicsState == null)
        {
            extendedGraphicsState = ofNullable(
                    getCOSObject().getDictionaryObject(COSName.EXT_G_STATE,
                            COSDictionary.class)).map(PDExtendedGraphicsState::new).orElse(null);

        }
        return extendedGraphicsState;
    }

    public void setExtendedGraphicsState(PDExtendedGraphicsState extendedGraphicsState)
    {
        this.extendedGraphicsState = extendedGraphicsState;
        getCOSObject().setItem(COSName.EXT_G_STATE, extendedGraphicsState);
    }

    public PDShading getShading() throws IOException
    {
        if (shading == null)
        {
            var shadingDictionary = getCOSObject().getDictionaryObject(COSName.SHADING,
                    COSDictionary.class);
            if (nonNull(shadingDictionary))
            {
                shading = PDShading.create(shadingDictionary);
            }
        }
        return shading;
    }

    public void setShading( PDShading shadingResources )
    {
        shading = shadingResources;
        getCOSObject().setItem(COSName.SHADING, shadingResources);
    }
}
