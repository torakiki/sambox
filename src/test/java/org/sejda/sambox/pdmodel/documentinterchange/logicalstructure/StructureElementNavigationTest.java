/*
 * Created on 11/05/26
 * Copyright 2026 by Sober Lemur S.r.l. (info@soberlemur.com).
 *
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
package org.sejda.sambox.pdmodel.documentinterchange.logicalstructure;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sejda.io.SeekableSources;
import org.sejda.sambox.cos.COSInteger;
import org.sejda.sambox.input.PDFParser;
import org.sejda.sambox.pdmodel.PDDocument;

/**
 * Verifies kids navigation and StructureElement subtype resolution on a real PDF/UA tagged
 * document.
 */
class StructureElementNavigationTest
{
    private PDDocument document;
    private PDStructureTreeRoot root;

    @BeforeEach
    void setUp() throws IOException
    {
        try (var in = requireNonNull(getClass().getResourceAsStream("PDFUA-Ref-2-02_Invoice.pdf"),
                "Missing test resource PDFUA-Ref-2-02_Invoice.pdf"))
        {
            document = PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(in));
        }
        root = document.getDocumentCatalog().getStructureTreeRoot();
    }

    @AfterEach
    void tearDown() throws IOException
    {
        document.close();
    }

    // --- Structure tree root ---

    @Test
    void rootHasSingleKid()
    {
        assertEquals(1, root.getKids().size());
    }

    @Test
    void rootKidIsDocumentElement()
    {
        var kid = assertInstanceOf(PDStructureElement.class, root.getKids().getFirst());
        assertEquals("Document", kid.getStructureType());
    }

    // --- Document element ---

    @Test
    void documentHasSingleKid()
    {
        assertEquals(1, documentElement().getKids().size());
    }

    @Test
    void documentKidIsPartElement()
    {
        var kid = assertInstanceOf(PDStructureElement.class,
                documentElement().getKids().getFirst());
        assertEquals("Part", kid.getStructureType());
    }

    // --- Part element ---

    @Test
    void partTitleIsInvoice()
    {
        assertEquals("Invoice 2020-10", getPart().getTitle());
    }

    @Test
    void partHasThreeKids()
    {
        assertEquals(3, getPart().getKids().size());
    }

    @Test
    void partKidsAreAllSectElements()
    {
        List<StructureElement> kids = getPart().getKids();
        for (var kid : kids)
        {
            var sect = assertInstanceOf(PDStructureElement.class, kid);
            assertEquals("Sect", sect.getStructureType());
        }
    }

    @Test
    void secondSectHasKids()
    {
        assertFalse(((PDStructureElement) getPart().getKids().get(1)).getKids().isEmpty());
    }

    @Test
    void thirdSectHasKids()
    {
        assertFalse(((PDStructureElement) getPart().getKids().get(2)).getKids().isEmpty());
    }

    // --- First Sect element (Sender stationery) ---

    @Test
    void firstSectTitleIsSenderStationery()
    {
        assertEquals("Sender stationery", getFirstSect().getTitle());
    }

    @Test
    void firstSectHasFourKids()
    {
        assertEquals(4, getFirstSect().getKids().size());
    }

    @Test
    void firstSectFirstKidIsFigureElement()
    {
        var kid = assertInstanceOf(PDStructureElement.class, getFirstSect().getKids().getFirst());
        assertEquals("Figure", kid.getStructureType());
    }

    @Test
    void firstSectSecondKidIsPElement()
    {
        var kid = assertInstanceOf(PDStructureElement.class, getFirstSect().getKids().get(1));
        assertEquals("P", kid.getStructureType());
    }

    @Test
    void firstSectThirdKidIsFigureElement()
    {
        var kid = assertInstanceOf(PDStructureElement.class, getFirstSect().getKids().get(2));
        assertEquals("Figure", kid.getStructureType());
    }

    @Test
    void firstSectFourthKidIsCaptionElement()
    {
        var kid = assertInstanceOf(PDStructureElement.class, getFirstSect().getKids().get(3));
        assertEquals("Caption", kid.getStructureType());
    }

    // --- Figure (logo) element: kid is a PDMarkedContentReference ---

    @Test
    void figureLogoHasSingleMarkedContentReferenceKid()
    {
        List<StructureElement> kids = getLogoFigure().getKids();
        assertEquals(1, kids.size());
        assertInstanceOf(PDMarkedContentReference.class, kids.getFirst());
    }

    @Test
    void figureLogoMarkedContentReferenceMCIDIsZero()
    {
        var mcr = assertInstanceOf(PDMarkedContentReference.class,
                getLogoFigure().getKids().getFirst());
        assertEquals(0, mcr.getMCID());
    }

    @Test
    void figureLogoMarkedContentReferenceHasPage()
    {
        var mcr = assertInstanceOf(PDMarkedContentReference.class,
                getLogoFigure().getKids().getFirst());
        assertNotNull(mcr.getPage());
    }

    @Test
    void figureLogoMarkedContentReferenceParentIsTheFigure()
    {
        var figure = getLogoFigure();
        var mcr = assertInstanceOf(PDMarkedContentReference.class, figure.getKids().getFirst());
        assertSame(figure.getCOSObject(), mcr.getParent().getCOSObject());
    }

    @Test
    void markedContentReferenceHasNoKids()
    {
        var mcr = assertInstanceOf(PDMarkedContentReference.class,
                getLogoFigure().getKids().getFirst());
        assertTrue(mcr.getKids().isEmpty());
    }

    // --- P element: kid is a PDMarkedContentIdentifier (integer 0) ---

    @Test
    void pElementHasSingleMarkedContentIdentifierKid()
    {
        List<StructureElement> kids = getPElement().getKids();
        assertEquals(1, kids.size());
        assertInstanceOf(PDMarkedContentIdentifier.class, kids.getFirst());
    }

    @Test
    void pElementMarkedContentIdentifierValueIsZero()
    {
        var mci = assertInstanceOf(PDMarkedContentIdentifier.class,
                getPElement().getKids().getFirst());
        assertEquals(COSInteger.get(0), mci.getCOSObject());
    }

    @Test
    void pElementMarkedContentIdentifierParentIsP()
    {
        var p = getPElement();
        var mci = assertInstanceOf(PDMarkedContentIdentifier.class, p.getKids().getFirst());
        assertSame(p.getCOSObject(), mci.getParent().getCOSObject());
    }

    @Test
    void markedContentIdentifierHasNoKids()
    {
        var mci = assertInstanceOf(PDMarkedContentIdentifier.class,
                getPElement().getKids().getFirst());
        assertTrue(mci.getKids().isEmpty());
    }

    // --- Figure (photo) element: kid is a PDMarkedContentIdentifier (integer 1) ---

    @Test
    void photoFigureHasSingleMarkedContentIdentifierKid()
    {
        List<StructureElement> kids = getPhotoFigure().getKids();
        assertEquals(1, kids.size());
        assertInstanceOf(PDMarkedContentIdentifier.class, kids.getFirst());
    }

    @Test
    void photoFigureMarkedContentIdentifierValueIsOne()
    {
        var mci = assertInstanceOf(PDMarkedContentIdentifier.class,
                getPhotoFigure().getKids().getFirst());
        assertEquals(COSInteger.get(1), mci.getCOSObject());
    }

    @Test
    void photoFigureMarkedContentIdentifierParentIsTheFigure()
    {
        var figure = getPhotoFigure();
        var mci = assertInstanceOf(PDMarkedContentIdentifier.class, figure.getKids().getFirst());
        assertSame(figure.getCOSObject(), mci.getParent().getCOSObject());
    }

    // --- Caption element: single P kid ---

    @Test
    void captionHasSinglePElementKid()
    {
        List<StructureElement> kids = getCaption().getKids();
        assertEquals(1, kids.size());
        var captionP = assertInstanceOf(PDStructureElement.class, kids.getFirst());
        assertEquals("P", captionP.getStructureType());
    }

    // --- Caption's P element: kid is a PDMarkedContentIdentifier (integer 2) ---

    @Test
    void captionPElementHasSingleMarkedContentIdentifierKid()
    {
        var captionP = assertInstanceOf(PDStructureElement.class,
                getCaption().getKids().getFirst());
        List<StructureElement> kids = captionP.getKids();
        assertEquals(1, kids.size());
        assertInstanceOf(PDMarkedContentIdentifier.class, kids.getFirst());
    }

    @Test
    void captionPElementMarkedContentIdentifierValueIsTwo()
    {
        var captionP = assertInstanceOf(PDStructureElement.class,
                getCaption().getKids().getFirst());
        var mci = assertInstanceOf(PDMarkedContentIdentifier.class, captionP.getKids().getFirst());
        assertEquals(COSInteger.get(2), mci.getCOSObject());
    }

    // --- Navigation helpers ---

    private PDStructureElement documentElement()
    {
        return (PDStructureElement) root.getKids().getFirst();
    }

    private PDStructureElement getPart()
    {
        return (PDStructureElement) documentElement().getKids().getFirst();
    }

    private PDStructureElement getFirstSect()
    {
        return (PDStructureElement) getPart().getKids().getFirst();
    }

    private PDStructureElement getLogoFigure()
    {
        return (PDStructureElement) getFirstSect().getKids().getFirst();
    }

    private PDStructureElement getPElement()
    {
        return (PDStructureElement) getFirstSect().getKids().get(1);
    }

    private PDStructureElement getPhotoFigure()
    {
        return (PDStructureElement) getFirstSect().getKids().get(2);
    }

    private PDStructureElement getCaption()
    {
        return (PDStructureElement) getFirstSect().getKids().get(3);
    }
}
