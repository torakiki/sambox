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

import static java.util.Objects.nonNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.Test;
import org.sejda.io.SeekableSources;
import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSFloat;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSNull;
import org.sejda.sambox.cos.COSNumber;
import org.sejda.sambox.cos.COSStream;
import org.sejda.sambox.input.PDFParser;
import org.sejda.sambox.pdmodel.common.PDRectangle;
import org.sejda.sambox.pdmodel.font.PDType0Font;
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

    @Test
    public void cropBoxCoordinatesToDraw()
    {
        PDPage page = new PDPage(PDRectangle.A4);
        page.setCropBox(new PDRectangle(2, 10, 500, 800));
        assertEquals(new Point(8, 20), page.cropBoxCoordinatesToDraw(new Point2D.Float(6, 10)));
        page.setRotation(90);
        assertEquals(new Point(492, 16), page.cropBoxCoordinatesToDraw(new Point2D.Float(6, 10)));
        page.setRotation(180);
        assertEquals(new Point(496, 800), page.cropBoxCoordinatesToDraw(new Point2D.Float(6, 10)));
        page.setRotation(270);
        assertEquals(new Point(12, 804), page.cropBoxCoordinatesToDraw(new Point2D.Float(6, 10)));
    }

    @Test
    public void sanitize() throws IOException
    {
        try (PDDocument doc = new PDDocument())
        {
            PDPage page = new PDPage();
            PDType0Font font = PDType0Font.load(doc,
                    PDType0Font.class.getClassLoader().getResourceAsStream(
                            "org/sejda/sambox/resources/ttf/LiberationSans-Regular.ttf"));

            try (PDPageContentStream formContents = new PDPageContentStream(doc, page))
            {
                formContents.beginText();
                formContents.setFont(font, 22);
                formContents.newLineAtOffset(100, 100);
                formContents.showText("Chuck Norris");
                formContents.endText();
            }
            COSStream stream = page.getCOSObject().getDictionaryObject(COSName.CONTENTS,
                    COSStream.class);
            stream.setItem(COSName.ANNOTS, new COSDictionary());
            assertTrue(nonNull(
                    page.getCOSObject().getDictionaryObject(COSName.CONTENTS, COSStream.class)
                            .getItem(COSName.ANNOTS)));
            page.sanitizeDictionary();
            assertFalse(nonNull(
                    page.getCOSObject().getDictionaryObject(COSName.CONTENTS, COSStream.class)
                            .getItem(COSName.ANNOTS)));
        }
    }

    COSArray toArray(float n1, float n2, float n3, float n4)
    {
        COSArray result = new COSArray();
        result.add(new COSFloat(n1));
        result.add(new COSFloat(n2));
        result.add(new COSFloat(n3));
        result.add(new COSFloat(n4));
        return result;
    }

    @Test
    public void invalidDocument() throws IOException {
        PDDocument document = new PDDocument();
        PDPage page = new PDPage();
        document.addPage(page);
        
        page.getCOSObject().setItem(COSName.CONTENTS, new COSArray(COSName.TYPE));
        
        File tempFile = Files.createTempFile("invalid-document", ".pdf").toFile();
        document.writeTo(tempFile);
        
        PDDocument read = PDFParser.parse(SeekableSources.seekableSourceFrom(tempFile));
        assertFalse("", read.getPage(0).getContentStreams().hasNext());
    }

}
