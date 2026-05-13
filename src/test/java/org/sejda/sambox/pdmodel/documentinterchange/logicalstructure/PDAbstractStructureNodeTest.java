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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSInteger;
import org.sejda.sambox.cos.COSName;

/**
 * Unit tests for appendKid, insertBefore and removeKid in PDAbstractStructureNode.
 */
class PDAbstractStructureNodeTest
{
    private PDStructureTreeRoot root;

    @BeforeEach
    void setUp()
    {
        root = new PDStructureTreeRoot();
    }

    // ---- appendKid ----

    @Test
    void appendKidToEmptyNodeReturnedByGetKids()
    {
        var kid = new PDStructureElement(new COSDictionary());
        root.appendKid(kid);
        var kids = root.getKids();
        assertEquals(1, kids.size());
        assertSame(kid.getCOSObject(), kids.getFirst().getCOSObject());
    }

    @Test
    void appendKidToEmptyNodeStoresItDirectlyNotInArray()
    {
        root.appendKid(new PDStructureElement(new COSDictionary()));
        assertFalse(root.getCOSObject().getDictionaryObject(COSName.K) instanceof COSArray);
    }

    @Test
    void appendSecondKidCreatesArray()
    {
        root.appendKid(new PDStructureElement(new COSDictionary()));
        root.appendKid(new PDStructureElement(new COSDictionary()));
        assertEquals(2, root.getKids().size());
        assertInstanceOf(COSArray.class, root.getCOSObject().getDictionaryObject(COSName.K));
    }

    @Test
    void appendThirdKidExtendsExistingArray()
    {
        root.appendKid(new PDStructureElement(new COSDictionary()));
        root.appendKid(new PDStructureElement(new COSDictionary()));
        root.appendKid(new PDStructureElement(new COSDictionary()));
        assertEquals(3, root.getKids().size());
    }

    @Test
    void appendedKidsAreReturnedInOrder()
    {
        var kid1 = new PDStructureElement(new COSDictionary());
        var kid2 = new PDStructureElement(new COSDictionary());
        var kid3 = new PDStructureElement(new COSDictionary());
        root.appendKid(kid1);
        root.appendKid(kid2);
        root.appendKid(kid3);
        var kids = root.getKids();
        assertSame(kid1.getCOSObject(), kids.get(0).getCOSObject());
        assertSame(kid2.getCOSObject(), kids.get(1).getCOSObject());
        assertSame(kid3.getCOSObject(), kids.get(2).getCOSObject());
    }

    @Test
    void appendKidSetsParentOnKid()
    {
        var kid = new PDStructureElement(new COSDictionary());
        root.appendKid(kid);
        assertSame(root.getCOSObject(), kid.getParent().getCOSObject());
    }

    @Test
    void appendNullKidIsIgnored()
    {
        root.appendKid(null);
        assertTrue(root.getKids().isEmpty());
    }

    @Test
    void appendMarkedContentIdentifierKidReturnedByGetKids()
    {
        var mci = new PDMarkedContentIdentifier(COSInteger.get(42));
        root.appendKid(mci);
        var kids = root.getKids();
        assertEquals(1, kids.size());
        assertInstanceOf(PDMarkedContentIdentifier.class, kids.getFirst());
        assertEquals(COSInteger.get(42), kids.getFirst().getCOSObject());
    }

    // ---- removeKid ----

    @Test
    void removeOnlyKidLeavesNodeEmpty()
    {
        var kid = new PDStructureElement(new COSDictionary());
        root.appendKid(kid);
        root.removeKid(kid);
        assertTrue(root.getKids().isEmpty());
    }

    @Test
    void removeKidReturnsTrueWhenRemoved()
    {
        var kid = new PDStructureElement(new COSDictionary());
        root.appendKid(kid);
        assertTrue(root.removeKid(kid));
    }

    @Test
    void removeKidReturnsFalseWhenNotFound()
    {
        var kid1 = new PDStructureElement(new COSDictionary());
        var kid2 = new PDStructureElement(new COSDictionary());
        root.appendKid(kid1);
        assertFalse(root.removeKid(kid2));
    }

    @Test
    void removeKidWhenNoKidsReturnsFalse()
    {
        assertFalse(root.removeKid(new PDStructureElement(new COSDictionary())));
    }

    @Test
    void removeNullKidReturnsFalse()
    {
        assertFalse(root.removeKid(null));
    }

    @Test
    void removeFirstOfTwoKidsUnwrapsKFromArray()
    {
        var kid1 = new PDStructureElement(new COSDictionary());
        var kid2 = new PDStructureElement(new COSDictionary());
        root.appendKid(kid1);
        root.appendKid(kid2);
        root.removeKid(kid1);
        assertEquals(1, root.getKids().size());
        assertFalse(root.getCOSObject().getDictionaryObject(COSName.K) instanceof COSArray);
        assertSame(kid2.getCOSObject(), root.getKids().getFirst().getCOSObject());
    }

    @Test
    void removeSecondOfTwoKidsUnwrapsKFromArray()
    {
        var kid1 = new PDStructureElement(new COSDictionary());
        var kid2 = new PDStructureElement(new COSDictionary());
        root.appendKid(kid1);
        root.appendKid(kid2);
        root.removeKid(kid2);
        assertEquals(1, root.getKids().size());
        assertFalse(root.getCOSObject().getDictionaryObject(COSName.K) instanceof COSArray);
        assertSame(kid1.getCOSObject(), root.getKids().getFirst().getCOSObject());
    }

    @Test
    void removeKidFromThreeKidsLeavesArray()
    {
        var kid1 = new PDStructureElement(new COSDictionary());
        var kid2 = new PDStructureElement(new COSDictionary());
        var kid3 = new PDStructureElement(new COSDictionary());
        root.appendKid(kid1);
        root.appendKid(kid2);
        root.appendKid(kid3);
        root.removeKid(kid2);
        assertEquals(2, root.getKids().size());
        assertInstanceOf(COSArray.class, root.getCOSObject().getDictionaryObject(COSName.K));
    }

    @Test
    void removeMiddleKidPreservesRemainingOrder()
    {
        var kid1 = new PDStructureElement(new COSDictionary());
        var kid2 = new PDStructureElement(new COSDictionary());
        var kid3 = new PDStructureElement(new COSDictionary());
        root.appendKid(kid1);
        root.appendKid(kid2);
        root.appendKid(kid3);
        root.removeKid(kid2);
        var kids = root.getKids();
        assertSame(kid1.getCOSObject(), kids.get(0).getCOSObject());
        assertSame(kid3.getCOSObject(), kids.get(1).getCOSObject());
    }

    // ---- insertBefore ----

    @Test
    void insertBeforeSingleMatchingKidNewKidComesFirst()
    {
        var existing = new PDStructureElement(new COSDictionary());
        var newKid = new PDStructureElement(new COSDictionary());
        root.appendKid(existing);
        root.insertBefore(newKid, existing);
        var kids = root.getKids();
        assertEquals(2, kids.size());
        assertSame(newKid.getCOSObject(), kids.getFirst().getCOSObject());
        assertSame(existing.getCOSObject(), kids.get(1).getCOSObject());
    }

    @Test
    void insertBeforeInArrayPlacesKidAtCorrectIndex()
    {
        var kid1 = new PDStructureElement(new COSDictionary());
        var kid2 = new PDStructureElement(new COSDictionary());
        var newKid = new PDStructureElement(new COSDictionary());
        root.appendKid(kid1);
        root.appendKid(kid2);
        root.insertBefore(newKid, kid2);
        var kids = root.getKids();
        assertEquals(3, kids.size());
        assertSame(kid1.getCOSObject(), kids.get(0).getCOSObject());
        assertSame(newKid.getCOSObject(), kids.get(1).getCOSObject());
        assertSame(kid2.getCOSObject(), kids.get(2).getCOSObject());
    }

    @Test
    void insertBeforeFirstElementInArray()
    {
        var kid1 = new PDStructureElement(new COSDictionary());
        var kid2 = new PDStructureElement(new COSDictionary());
        var newKid = new PDStructureElement(new COSDictionary());
        root.appendKid(kid1);
        root.appendKid(kid2);
        root.insertBefore(newKid, kid1);
        var kids = root.getKids();
        assertEquals(3, kids.size());
        assertSame(newKid.getCOSObject(), kids.get(0).getCOSObject());
        assertSame(kid1.getCOSObject(), kids.get(1).getCOSObject());
        assertSame(kid2.getCOSObject(), kids.get(2).getCOSObject());
    }

    @Test
    void insertBeforeSetsParentOnNewKid()
    {
        var existing = new PDStructureElement(new COSDictionary());
        var newKid = new PDStructureElement(new COSDictionary());
        root.appendKid(existing);
        root.insertBefore(newKid, existing);
        assertSame(root.getCOSObject(), newKid.getParent().getCOSObject());
    }

    @Test
    void insertBeforeNotFoundInArrayAppendsElement()
    {
        // When K is array and beforeThis is not found, element is appended (see implementation)
        var kid1 = new PDStructureElement(new COSDictionary());
        var kid2 = new PDStructureElement(new COSDictionary());
        var notPresent = new PDStructureElement(new COSDictionary());
        var newKid = new PDStructureElement(new COSDictionary());
        root.appendKid(kid1);
        root.appendKid(kid2);
        root.insertBefore(newKid, notPresent);
        var kids = root.getKids();
        assertEquals(3, kids.size());
        assertSame(newKid.getCOSObject(), kids.get(2).getCOSObject());
    }

    @Test
    void insertBeforeSingleKidWhenBeforeThisNotMatchingDoesNotInsert()
    {
        // When K is a single item and beforeThis does not match, nothing is inserted
        var existing = new PDStructureElement(new COSDictionary());
        var notPresent = new PDStructureElement(new COSDictionary());
        var newKid = new PDStructureElement(new COSDictionary());
        root.appendKid(existing);
        root.insertBefore(newKid, notPresent);
        assertEquals(1, root.getKids().size());
    }

    @Test
    void insertBeforeNullElementIsIgnored()
    {
        var existing = new PDStructureElement(new COSDictionary());
        root.appendKid(existing);
        root.insertBefore(null, existing);
        assertEquals(1, root.getKids().size());
    }

    @Test
    void insertBeforeNullBeforeThisIsIgnored()
    {
        var existing = new PDStructureElement(new COSDictionary());
        var newKid = new PDStructureElement(new COSDictionary());
        root.appendKid(existing);
        root.insertBefore(newKid, null);
        assertEquals(1, root.getKids().size());
    }

    // ---- getParent ----

    @Test
    void structureTreeRootParentIsNull()
    {
        assertNull(root.getParent());
    }

    @Test
    void structureTreeRootSetParentThrowsUnsupportedOperationException()
    {
        assertThrows(UnsupportedOperationException.class,
                () -> root.setParent(new PDStructureTreeRoot()));
    }

    @Test
    void structureElementParentIsNullWhenNoPEntry()
    {
        var elem = new PDStructureElement(new COSDictionary());
        assertNull(elem.getParent());
    }

    @Test
    void structureElementParentCOSObjectMatchesParentAfterAppendKid()
    {
        var kid = new PDStructureElement(new COSDictionary());
        root.appendKid(kid);
        assertSame(root.getCOSObject(), kid.getParent().getCOSObject());
    }

    @Test
    void structureElementGetParentReconstructsNewWrapperEachCall()
    {
        // PDStructureElement.getParent() rebuilds the wrapper from /P on every call
        var kid = new PDStructureElement(new COSDictionary());
        root.appendKid(kid);
        var parent1 = kid.getParent();
        var parent2 = kid.getParent();
        assertFalse(parent1 == parent2);
        assertSame(parent1.getCOSObject(), parent2.getCOSObject());
    }

    @Test
    void markedContentIdentifierParentIsNullWhenCreatedWithoutParent()
    {
        var mci = new PDMarkedContentIdentifier(COSInteger.get(1));
        assertNull(mci.getParent());
    }

    @Test
    void markedContentIdentifierParentIsSameInstanceAfterAppendKid()
    {
        // MCI stores parent in a Java field — identity-stable
        var mci = new PDMarkedContentIdentifier(COSInteger.get(5));
        root.appendKid(mci);
        assertSame(root, mci.getParent());
    }

    @Test
    void markedContentReferenceParentIsSameInstanceAfterAppendKid()
    {
        // MCR stores parent in a Java field — identity-stable
        var placeholder = new PDStructureTreeRoot();
        var mcr = new PDMarkedContentReference(placeholder);
        root.appendKid(mcr);
        assertSame(root, mcr.getParent());
    }

    @Test
    void objectReferenceParentIsSameInstanceAfterAppendKid()
    {
        // OBJR stores parent in a Java field — identity-stable
        var placeholder = new PDStructureTreeRoot();
        var objr = new PDObjectReference(placeholder);
        root.appendKid(objr);
        assertSame(root, objr.getParent());
    }

    // ---- elements() and stream() ----

    @Test
    void elementsOfEmptyRootIsEmpty()
    {
        assertFalse(root.elements().iterator().hasNext());
    }

    @Test
    void elementsReturnsSingleKid()
    {
        var kid = new PDStructureElement(new COSDictionary());
        root.appendKid(kid);
        var it = root.elements().iterator();
        assertTrue(it.hasNext());
        assertSame(kid.getCOSObject(), it.next().getCOSObject());
        assertFalse(it.hasNext());
    }

    @Test
    void elementsTraversesInPreOrderDepthFirst()
    {
        var a = new PDStructureElement(new COSDictionary());
        var b = new PDStructureElement(new COSDictionary());
        var c = new PDStructureElement(new COSDictionary());
        var d = new PDStructureElement(new COSDictionary());
        root.appendKid(a);
        root.appendKid(d);
        a.appendKid(b);
        a.appendKid(c);
        // expected pre-order: a, b, c, d
        var it = root.elements().iterator();
        assertSame(a.getCOSObject(), it.next().getCOSObject());
        assertSame(b.getCOSObject(), it.next().getCOSObject());
        assertSame(c.getCOSObject(), it.next().getCOSObject());
        assertSame(d.getCOSObject(), it.next().getCOSObject());
        assertFalse(it.hasNext());
    }

    @Test
    void elementsIncludesLeafTypes()
    {
        var elem = new PDStructureElement(new COSDictionary());
        var mci = new PDMarkedContentIdentifier(COSInteger.get(7));
        root.appendKid(elem);
        elem.appendKid(mci);
        var it = root.elements().iterator();
        assertInstanceOf(PDStructureElement.class, it.next());
        assertInstanceOf(PDMarkedContentIdentifier.class, it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void streamCountMatchesNumberOfDescendants()
    {
        root.appendKid(new PDStructureElement(new COSDictionary()));
        root.appendKid(new PDStructureElement(new COSDictionary()));
        root.appendKid(new PDMarkedContentIdentifier(COSInteger.get(3)));
        assertEquals(3, root.stream().count());
    }

    @Test
    void streamCanFilterByType()
    {
        var elem = new PDStructureElement(new COSDictionary());
        var mci = new PDMarkedContentIdentifier(COSInteger.get(0));
        root.appendKid(elem);
        elem.appendKid(mci);
        var structureElements = root.stream().filter(PDStructureElement.class::isInstance).toList();
        assertEquals(1, structureElements.size());
        assertSame(elem.getCOSObject(), structureElements.getFirst().getCOSObject());
    }
}
