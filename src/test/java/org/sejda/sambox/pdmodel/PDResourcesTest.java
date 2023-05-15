package org.sejda.sambox.pdmodel;

import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;

/*
 * Copyright 2023 Sober Lemur S.r.l.
 * Copyright 2023 Sejda BV
 *
 * Created 15/05/23
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
class PDResourcesTest
{

    @Test
    public void wrongFontType() throws IOException
    {
        var fontDictionary = new COSDictionary();
        var resources = new COSDictionary();
        resources.setItem(COSName.FONT, fontDictionary);
        var victim = new PDResources(resources);
        fontDictionary.setItem("F1", COSName.EMBEDDED_FILES);
        assertNull(victim.getFont(COSName.getPDFName("F1")));
    }

    @Test
    public void wrongExtGStateType() throws IOException
    {
        var state = new COSDictionary();
        var resources = new COSDictionary();
        resources.setItem(COSName.EXT_G_STATE, state);
        var victim = new PDResources(resources);
        state.setItem("es1", COSName.EMBEDDED_FILES);
        assertNull(victim.getExtGState(COSName.getPDFName("es1")));
    }

    @Test
    public void wrongPatternType() throws IOException
    {
        var patternDictionary = new COSDictionary();
        var resources = new COSDictionary();
        resources.setItem(COSName.PATTERN, patternDictionary);
        var victim = new PDResources(resources);
        patternDictionary.setItem("P1", COSName.EMBEDDED_FILES);
        assertNull(victim.getPattern(COSName.getPDFName("P1")));
    }

    @Test
    public void wrongPropertiesType() throws IOException
    {
        var propertiesDictionary = new COSDictionary();
        var resources = new COSDictionary();
        resources.setItem(COSName.PROPERTIES, propertiesDictionary);
        var victim = new PDResources(resources);
        propertiesDictionary.setItem("P1", COSName.EMBEDDED_FILES);
        assertNull(victim.getProperties(COSName.getPDFName("P1")));
    }

    @Test
    public void wrongShadingType() throws IOException
    {
        var shadingDictionary = new COSDictionary();
        var resources = new COSDictionary();
        resources.setItem(COSName.SHADING, shadingDictionary);
        var victim = new PDResources(resources);
        shadingDictionary.setItem("S1", COSName.EMBEDDED_FILES);
        assertNull(victim.getShading(COSName.getPDFName("S1")));
    }
}