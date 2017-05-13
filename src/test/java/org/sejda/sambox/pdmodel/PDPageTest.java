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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;
import org.sejda.sambox.cos.*;
import org.sejda.sambox.pdmodel.common.PDRectangle;
import org.sejda.sambox.pdmodel.interactive.annotation.PDAnnotationLink;

/**
 * @author Andrea Vacondio
 *
 */
public class PDPageTest
{

    @Test
    public void nullBeads()
    {
        PDPage victim = new PDPage();
        victim.getCOSObject().setItem(COSName.B, null);
        assertTrue(victim.getThreadBeads().isEmpty());
    }

    @Test
    public void cosNullBeadsItem()
    {
        PDPage victim = new PDPage();
        COSArray beads = new COSArray(COSNull.NULL);
        victim.getCOSObject().setItem(COSName.B, beads);
        assertTrue(victim.getThreadBeads().isEmpty());
    }

    @Test
    public void wrongTypeBeadsItem() throws IOException
    {
        PDPage victim = new PDPage();
        COSArray beads = new COSArray(COSNumber.get("2"));
        victim.getCOSObject().setItem(COSName.B, beads);
        assertTrue(victim.getThreadBeads().isEmpty());
    }

    @Test
    public void nonNullBeadsItem()
    {
        PDPage victim = new PDPage();
        COSArray beads = new COSArray(new COSDictionary());
        victim.getCOSObject().setItem(COSName.B, beads);
        assertFalse(victim.getThreadBeads().isEmpty());
    }

    @Test
    public void nullAnnotations()
    {
        PDPage victim = new PDPage();
        victim.getCOSObject().setItem(COSName.ANNOTS, null);
        assertTrue(victim.getAnnotations().isEmpty());
    }

    @Test
    public void cosNullAnnotsItem()
    {
        PDPage victim = new PDPage();
        COSArray beads = new COSArray(COSNull.NULL);
        victim.getCOSObject().setItem(COSName.ANNOTS, beads);
        assertTrue(victim.getAnnotations().isEmpty());
    }

    @Test
    public void wrongTypeAnnotsItem() throws IOException
    {
        PDPage victim = new PDPage();
        COSArray beads = new COSArray(COSNumber.get("2"));
        victim.getCOSObject().setItem(COSName.ANNOTS, beads);
        assertTrue(victim.getAnnotations().isEmpty());
    }

    @Test
    public void nonNullAnnotsItem()
    {
        PDPage victim = new PDPage();
        COSArray beads = new COSArray(new PDAnnotationLink().getCOSObject());
        victim.getCOSObject().setItem(COSName.ANNOTS, beads);
        assertFalse(victim.getAnnotations().isEmpty());
    }

    @Test
    public void cropBoxSlighlyOutOfMediaBoxBounds()
    {
        PDPage page = new PDPage();
        page.setMediaBox(new PDRectangle(toArray(0, 0, 287.07f, 831)));
        page.setCropBox(new PDRectangle(toArray(1, 214, 294, 624)));
        assertEquals(page.getCropBox(), new PDRectangle(toArray(1, 214, 287.07f, 624)));
    }

    COSArray toArray(float n1, float n2, float n3, float n4) {
        COSArray result = new COSArray();
        result.add(new COSFloat(n1));
        result.add(new COSFloat(n2));
        result.add(new COSFloat(n3));
        result.add(new COSFloat(n4));
        return result;
    }
}
