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
package org.sejda.sambox.pdmodel.graphics;

import java.io.IOException;
import java.util.Optional;

import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSFloat;
import org.sejda.sambox.cos.COSNumber;
import org.sejda.sambox.cos.COSObjectable;
import org.sejda.sambox.pdmodel.font.PDFont;
import org.sejda.sambox.pdmodel.font.PDFontFactory;

/**
 * This class represents a font setting used for the graphics state.  A font setting is a font and a
 * font size.  Maybe there is a better name for this?
 *
 * @author Ben Litchfield
 */
public class PDFontSetting implements COSObjectable
{
    private COSArray fontSetting = null;

    /**
     * Creates a blank font setting, font will be null, size will be 1.
     */
    public PDFontSetting()
    {
        fontSetting = new COSArray(null, new COSFloat(1));
    }

    /**
     * Constructs a font setting from an existing array.
     *
     * @param fontSetting The new font setting value.
     */
    public PDFontSetting(COSArray fontSetting)
    {
        this.fontSetting = fontSetting;
    }

    @Override
    public COSBase getCOSObject()
    {
        return fontSetting;
    }

    /**
     * This will get the font for this font setting.
     *
     * @return The font for this setting or null if one was not found.
     * @throws IOException If there is an error getting the font.
     */
    public PDFont getFont() throws IOException
    {
        if (fontSetting.getObject(0) instanceof COSDictionary dictionary)
        {
            return PDFontFactory.createFont(dictionary);
        }
        return null;
    }

    public void setFont(PDFont font)
    {
        fontSetting.set(0, font);
    }

    /**
     * @return The size of the font or 0 if size is of the wrong type.
     */
    public float getFontSize()
    {
        return Optional.ofNullable(fontSetting.getObject(1, COSNumber.class))
                .map(COSNumber::floatValue).orElse(0f);
    }

    public void setFontSize(float size)
    {
        fontSetting.set(1, new COSFloat(size));
    }
}
