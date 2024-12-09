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
package org.sejda.sambox.pdmodel.interactive.form;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSString;
import org.sejda.sambox.pdmodel.PDResources;
import org.sejda.sambox.pdmodel.font.PDType1Font;
import org.sejda.sambox.pdmodel.graphics.color.PDDeviceRGB;

/**
 * @author Andrea Vacondio
 *
 */
public class PDDefaultAppearanceStringTest
{
    // Used to check resources lookup
    private PDResources resources;
    private COSName fontResourceName;

    @Before
    public void setUp()
    {
        resources = new PDResources();
        // the resource name is created when the font is added so need
        // to capture that
        fontResourceName = resources.add(PDType1Font.HELVETICA());
    }

    @Test
    public void testParseDAString() throws IOException
    {
        COSString sampleString = COSString
                .parseLiteral("/" + fontResourceName.getName() + " 12 Tf 0.019 0.305 0.627 rg");

        PDDefaultAppearanceString defaultAppearanceString = new PDDefaultAppearanceString(
                sampleString, resources);

        assertEquals(12, defaultAppearanceString.getFontSize(), 0.001);
        assertEquals(PDType1Font.HELVETICA(), defaultAppearanceString.getFont());
        assertEquals(PDDeviceRGB.INSTANCE, defaultAppearanceString.getFontColor().getColorSpace());
        assertEquals(0.019, defaultAppearanceString.getFontColor().getComponents()[0], 0.0001);
        assertEquals(0.305, defaultAppearanceString.getFontColor().getComponents()[1], 0.0001);
        assertEquals(0.627, defaultAppearanceString.getFontColor().getComponents()[2], 0.0001);
    }

    @Test
    public void testFontResourceUnavailable() throws IOException
    {
        COSString sampleString = COSString.parseLiteral("/Helvetica 12 Tf 0.019 0.305 0.627 rg");
        PDDefaultAppearanceString defaultAppearanceString = new PDDefaultAppearanceString(sampleString, resources);
        assertEquals(PDType1Font.HELVETICA(), defaultAppearanceString.getFont());
    }

    @Test(expected = IOException.class)
    public void testWrongNumberOfColorArguments() throws IOException
    {
        COSString sampleString = COSString.parseLiteral("/Helvetica 12 Tf 0.305 0.627 rg");
        new PDDefaultAppearanceString(sampleString, resources);
    }
}
