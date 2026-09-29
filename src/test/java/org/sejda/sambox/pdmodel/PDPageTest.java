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
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.sejda.sambox.cos.COSDictionary.of;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.sejda.io.SeekableSources;
import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSFloat;
import org.sejda.sambox.cos.COSInteger;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSNull;
import org.sejda.sambox.cos.COSNumber;
import org.sejda.sambox.cos.COSStream;
import org.sejda.sambox.input.PDFParser;
import org.sejda.sambox.pdmodel.common.PDRectangle;
import org.sejda.sambox.pdmodel.font.PDType0Font;
import org.sejda.sambox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.sejda.sambox.pdmodel.interactive.form.PDField;
import org.sejda.sambox.pdmodel.interactive.form.PDRadioButton;

/**
 * @author Andrea Vacondio
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
    public void cropBoxOnlyZeros()
    {
        PDPage page = new PDPage();
        PDRectangle mediaBoxRect = new PDRectangle(toArray(0, 0, 287.07f, 831));
        page.setMediaBox(mediaBoxRect);
        page.setCropBox(new PDRectangle(toArray(0, 0, 0, 0)));
        assertEquals(page.getCropBox(), mediaBoxRect);
    }

    @Test
    public void cropBoxEmptyArray()
    {
        PDPage page = new PDPage();
        PDRectangle mediaBoxRect = new PDRectangle(toArray(0, 0, 287.07f, 831));
        page.setMediaBox(mediaBoxRect);
        page.setCropBox(new PDRectangle(new COSArray()));
        assertEquals(page.getCropBox(), mediaBoxRect);
    }

    /**
     * Matches pdf.js 6.4 dev (91041fb) and PDFium (425098b)
     */
    @Test
    public void cropBoxZeroWidth()
    {
        assertCropBoxFallsBackToMediaBox(toArray(100, 100, 100, 500));
    }

    /**
     * Matches pdf.js 6.4 dev (91041fb) and PDFium (425098b)
     */
    @Test
    public void cropBoxZeroHeight()
    {
        assertCropBoxFallsBackToMediaBox(toArray(100, 100, 200, 100));
    }

    /**
     * Matches pdf.js 6.4 dev (91041fb) and PDFium (425098b)
     */
    @Test
    public void cropBoxNonNumericElementResultingInZeroWidth()
    {
        assertCropBoxFallsBackToMediaBox(
                new COSArray(COSInteger.ZERO, COSInteger.ZERO, COSName.getPDFName("Foo"),
                        COSInteger.get(500)));
    }

    /**
     * Matches pdf.js 6.4 dev (91041fb) and PDFium (425098b)
     */
    @Test
    public void cropBoxNullElementsOnly()
    {
        assertCropBoxFallsBackToMediaBox(
                new COSArray(COSNull.NULL, COSNull.NULL, COSNull.NULL, COSNull.NULL));
    }

    /**
     * Matches pdf.js 6.4 dev (91041fb) and PDFium (425098b)
     */
    @Test
    public void cropBoxTooShort()
    {
        assertCropBoxFallsBackToMediaBox(
                new COSArray(COSInteger.ZERO, COSInteger.ZERO, COSInteger.get(200)));
    }

    /**
     * Matches pdf.js 6.4 dev (91041fb), PDFium (425098b) returns an empty page instead
     */
    @Test
    public void cropBoxOutsideMediaBox()
    {
        assertCropBoxFallsBackToMediaBox(toArray(400, 0, 500, 500));
    }

    /**
     * Matches pdf.js 6.4 dev (91041fb), PDFium (425098b) returns an empty page instead
     */
    @Test
    public void cropBoxTouchingMediaBoxEdge()
    {
        assertCropBoxFallsBackToMediaBox(toArray(287, 0, 400, 500));
    }

    /**
     * Matches pdf.js 6.4 dev (91041fb) and PDFium (425098b)
     */
    @Test
    public void cropBoxInvertedCoordinates()
    {
        PDPage page = new PDPage();
        page.setMediaBox(new PDRectangle(toArray(0, 0, 287, 831)));
        page.getCOSObject().setItem(COSName.CROP_BOX, toArray(200, 500, 10, 20));
        assertEquals(new PDRectangle(toArray(10, 20, 200, 500)), page.getCropBox());
    }

    /**
     * Matches pdf.js 6.4 dev (91041fb) and PDFium (425098b)
     */
    @Test
    public void cropBoxIndirectNumbers() throws IOException
    {
        assertEquals(new PDRectangle(toArray(0, 0, 200, 300)),
                cropBoxOf("cropbox-indirect-numbers.pdf"));
    }

    /**
     * Matches pdf.js 6.4 dev (91041fb) and PDFium (425098b)
     */
    @Test
    public void cropBoxIndirectZeros() throws IOException
    {
        assertEquals(new PDRectangle(toArray(0, 0, 287, 831)),
                cropBoxOf("cropbox-indirect-zeros.pdf"));
    }

    /**
     * Matches pdf.js 6.4 dev (91041fb) and PDFium (425098b)
     */
    @Test
    public void cropBoxIndirectRefToMissingObject() throws IOException
    {
        assertEquals(new PDRectangle(toArray(0, 0, 287, 831)),
                cropBoxOf("cropbox-missing-object-element.pdf"));
    }

    /**
     * Matches PDFium (425098b), pdf.js 6.4 dev (91041fb) falls back to the MediaBox instead
     */
    @Test
    public void cropBoxDanglingRefFallsBackToInherited() throws IOException
    {
        assertEquals(new PDRectangle(toArray(10, 10, 100, 100)),
                cropBoxOf("cropbox-dangling-ref.pdf"));
    }

    /**
     * Matches pdf.js 6.4 dev (91041fb) and PDFium (425098b)
     */
    @Test
    public void cropBoxInvalidShadowsInherited() throws IOException
    {
        assertEquals(new PDRectangle(toArray(0, 0, 287, 831)),
                cropBoxOf("cropbox-invalid-shadows-inherited.pdf"));
    }

    private static PDRectangle cropBoxOf(String resource) throws IOException
    {
        try (var doc = PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(
                requireNonNull(PDPageTest.class.getResourceAsStream("/sambox/" + resource)))))
        {
            return doc.getPage(0).getCropBox();
        }
    }

    private void assertCropBoxFallsBackToMediaBox(COSArray cropBox)
    {
        PDPage page = new PDPage();
        PDRectangle mediaBoxRect = new PDRectangle(toArray(0, 0, 287, 831));
        page.setMediaBox(mediaBoxRect);
        page.getCOSObject().setItem(COSName.CROP_BOX, cropBox);
        assertEquals(mediaBoxRect, page.getCropBox());
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
            PDType0Font font = PDType0Font.load(doc, PDPageTest.class.getResourceAsStream(
                    "/org/sejda/sambox/resources/ttf/LiberationSans-Regular.ttf"));

            try (PDPageContentStream formContents = new PDPageContentStream(doc, page))
            {
                formContents.beginText();
                formContents.setFont(font, 22);
                formContents.newLineAtOffset(100, 100);
                formContents.showText("Chuck Norris");
                formContents.endText();
            }
            COSStream stream = page.getCOSObject()
                    .getDictionaryObject(COSName.CONTENTS, COSStream.class);
            stream.setItem(COSName.ANNOTS, new COSDictionary());
            assertTrue(nonNull(page.getCOSObject()
                    .getDictionaryObject(COSName.CONTENTS, COSStream.class)
                    .getItem(COSName.ANNOTS)));
            page.sanitizeDictionary();
            assertFalse(nonNull(page.getCOSObject()
                    .getDictionaryObject(COSName.CONTENTS, COSStream.class)
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
    public void invalidDocument() throws IOException
    {
        File tempFile;
        try (PDDocument document = new PDDocument())
        {
            PDPage page = new PDPage();
            document.addPage(page);

            page.getCOSObject().setItem(COSName.CONTENTS, new COSArray(COSName.TYPE));

            tempFile = Files.createTempFile("invalid-document", ".pdf").toFile();
            document.writeTo(tempFile);
        }

        PDDocument read = PDFParser.parse(SeekableSources.seekableSourceFrom(tempFile));
        assertFalse(read.getPage(0).getContentStreams().hasNext());
    }

    @Test
    public void invalidPageResources()
    {
        COSDictionary dictionary = of(COSName.RESOURCES, new COSArray());
        PDPage page = new PDPage(dictionary);
        assertNotNull(page.getResources());
    }

    @Test
    public void invalidArtBox()
    {
        PDPage page = new PDPage();
        page.getCOSObject().setItem(COSName.ART_BOX,
                new COSArray(COSInteger.ZERO, COSInteger.ZERO, COSInteger.get(500)));
        assertEquals(PDRectangle.LETTER, page.getArtBox());

        page.getCOSObject().setItem(COSName.ART_BOX,
                new COSArray(COSInteger.ZERO, COSInteger.ZERO, COSInteger.get(500), COSName.AFTER));
        assertEquals(new PDRectangle(
                new COSArray(COSInteger.ZERO, COSInteger.ZERO, COSInteger.get(500),
                        COSInteger.ZERO)), page.getArtBox());
    }

    @Test
    public void invalidBleedBox()
    {
        PDPage page = new PDPage();
        page.getCOSObject().setItem(COSName.BLEED_BOX,
                new COSArray(COSInteger.ZERO, COSInteger.ZERO, COSInteger.get(500)));
        assertEquals(PDRectangle.LETTER, page.getBleedBox());

        page.getCOSObject().setItem(COSName.BLEED_BOX,
                new COSArray(COSInteger.ZERO, COSInteger.ZERO, COSInteger.get(500), COSName.AFTER));
        assertEquals(new PDRectangle(
                new COSArray(COSInteger.ZERO, COSInteger.ZERO, COSInteger.get(500),
                        COSInteger.ZERO)), page.getBleedBox());
    }

    @Test
    public void invalidTrimBox()
    {
        PDPage page = new PDPage();
        page.getCOSObject().setItem(COSName.TRIM_BOX,
                new COSArray(COSInteger.ZERO, COSInteger.ZERO, COSInteger.get(500)));
        assertEquals(PDRectangle.LETTER, page.getTrimBox());

        page.getCOSObject().setItem(COSName.TRIM_BOX,
                new COSArray(COSInteger.ZERO, COSInteger.ZERO, COSInteger.get(500), COSName.AFTER));
        assertEquals(new PDRectangle(
                new COSArray(COSInteger.ZERO, COSInteger.ZERO, COSInteger.get(500),
                        COSInteger.ZERO)), page.getTrimBox());
    }

    @Test
    public void missingFfFlagsOnField() throws IOException
    {
        try (PDDocument doc = PDFParser.parse(SeekableSources.onTempFileSeekableSourceFrom(
                Objects.requireNonNull(PDPageTest.class.getResourceAsStream(
                        "/sambox/forms-radio-buttons-missing-flags.pdf")))))
        {

            PDField field = doc.getDocumentCatalog().getAcroForm().getField("radioBtn");
            assertInstanceOf(PDRadioButton.class, field);
        }
    }

}
