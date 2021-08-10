/*
 * Created on 28/ago/2015
 * Copyright 2010 by Andrea Vacondio (andrea.vacondio@gmail.com).
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * 
 * http://www.apache.org/licenses/LICENSE-2.0 
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License. 
 */
package org.sejda.sambox.output;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSNull;
import org.sejda.sambox.cos.COSObjectKey;
import org.sejda.sambox.cos.IndirectCOSObjectIdentifier;
import org.sejda.sambox.cos.IndirectCOSObjectReference;
import org.sejda.sambox.cos.NonStorableInObjectStreams;
import org.sejda.sambox.encryption.GeneralEncryptionAlgorithm;
import org.sejda.sambox.input.ExistingIndirectCOSObject;
import org.sejda.sambox.xref.XrefEntry;

/**
 * @author Andrea Vacondio
 *
 */
public class PDFWriteContextTest
{
    private PDFWriteContext context;
    private XrefEntry entry = XrefEntry.inUseEntry(10, 12345, 0);
    private XrefEntry entry2 = XrefEntry.inUseEntry(100, 7564, 0);

    @Before
    public void setUp()
    {
        context = new PDFWriteContext(null);
    }

    @Test
    public void hasWriteOption()
    {
        PDFWriteContext context = new PDFWriteContext(null, WriteOption.OBJECT_STREAMS);
        assertTrue(context.hasWriteOption(WriteOption.OBJECT_STREAMS));
        assertFalse(context.hasWriteOption(WriteOption.ASYNC_BODY_WRITE));
    }

    @Test
    public void createIndirectReferenceFor()
    {
        COSDictionary dic = new COSDictionary();
        IndirectCOSObjectReference ref = context.createIndirectReferenceFor(dic);
        IndirectCOSObjectReference ref2 = context.createIndirectReferenceFor(dic);
        assertNotEquals(ref, ref2);
        ExistingIndirectCOSObject existing = mock(ExistingIndirectCOSObject.class);
        when(existing.id())
                .thenReturn(new IndirectCOSObjectIdentifier(new COSObjectKey(10, 0), "Source"));
        IndirectCOSObjectReference ref3 = context.createIndirectReferenceFor(existing);
        assertEquals(existing, ref3.getCOSObject());
    }

    @Test
    public void highestExisting()
    {
        context = new PDFWriteContext(50, null);
        IndirectCOSObjectReference ref = context
                .getOrCreateIndirectReferenceFor(new COSDictionary());
        assertEquals(51, ref.xrefEntry().getObjectNumber());
    }

    @Test
    public void highestObjectNumber()
    {
        context = new PDFWriteContext(50, null);
        assertEquals(50, context.highestObjectNumber());
        context.getOrCreateIndirectReferenceFor(new COSDictionary());
        assertEquals(51, context.highestObjectNumber());
    }

    @Test
    public void contiguousGroups()
    {
        context.addWritten(XrefEntry.unknownOffsetEntry(1, 0));
        context.addWritten(XrefEntry.unknownOffsetEntry(2, 0));
        context.addWritten(XrefEntry.unknownOffsetEntry(111, 0));
        context.addWritten(XrefEntry.unknownOffsetEntry(3, 0));
        context.addWritten(XrefEntry.unknownOffsetEntry(40, 0));
        context.addWritten(XrefEntry.unknownOffsetEntry(7, 0));
        context.addWritten(XrefEntry.unknownOffsetEntry(8, 0));
        context.addWritten(XrefEntry.unknownOffsetEntry(110, 0));
        List<List<Long>> groups = context.getWrittenContiguousGroups();
        assertEquals(4, groups.size());
        assertEquals(3, groups.get(0).size());
        assertEquals(2, groups.get(1).size());
        assertEquals(1, groups.get(2).size());
        assertEquals(2, groups.get(3).size());
    }

    @Test
    public void emptyContiguousGroups()
    {
        List<List<Long>> groups = context.getWrittenContiguousGroups();
        assertEquals(0, groups.size());
    }

    @Test
    public void getOrCreateIndirectReferenceFor()
    {
        COSDictionary dic = new COSDictionary();
        IndirectCOSObjectReference ref = context.getOrCreateIndirectReferenceFor(dic);
        IndirectCOSObjectReference ref2 = context.getOrCreateIndirectReferenceFor(dic);
        assertEquals(ref, ref2);
    }

    @Test
    public void createNonStorableInObjectStreamIndirectReferenceFor()
    {
        IndirectCOSObjectReference ref = context
                .createNonStorableInObjectStreamIndirectReferenceFor(new COSDictionary());
        assertThat(ref, instanceOf(NonStorableInObjectStreams.class));
    }

    @Test
    public void getIndirectReferenceFor()
    {
        COSDictionary dic = new COSDictionary();
        assertNull(dic.id());
        IndirectCOSObjectReference ref = context.getOrCreateIndirectReferenceFor(dic);
        assertEquals(ref, context.getIndirectReferenceFor(dic));
        assertNotNull(dic.id());
    }

    @Test
    public void getIndirectReferenceForNoIdNoNPE()
    {
        COSDictionary dic = new COSDictionary();
        assertNull(dic.id());
        assertNull(context.getIndirectReferenceFor(dic));
    }

    @Test
    public void getIndirectReferenceForNull()
    {
        IndirectCOSObjectReference ref = context.getOrCreateIndirectReferenceFor(COSNull.NULL);
        IndirectCOSObjectReference ref2 = context.getOrCreateIndirectReferenceFor(COSNull.NULL);
        assertNotEquals(ref, ref2);
    }

    @Test
    public void getIndirectReferenceForCOSNameAsExisting()
    {
        IndirectCOSObjectIdentifier id = new IndirectCOSObjectIdentifier(new COSObjectKey(10, 0),
                "Source");
        COSDictionary dic = new COSDictionary();
        dic.idIfAbsent(id);
        ExistingIndirectCOSObject existing = mock(ExistingIndirectCOSObject.class);
        when(existing.id()).thenReturn(id);
        when(existing.getCOSObject()).thenReturn(dic);
        context.getOrCreateIndirectReferenceFor(existing);
        assertTrue(context.hasIndirectReferenceFor(dic));

        IndirectCOSObjectIdentifier id2 = new IndirectCOSObjectIdentifier(new COSObjectKey(11, 0),
                "Source");
        COSName name = COSName.getPDFName("Chuck");
        name.idIfAbsent(id2);
        ExistingIndirectCOSObject existing2 = mock(ExistingIndirectCOSObject.class);
        when(existing2.id()).thenReturn(id2);
        when(existing2.getCOSObject()).thenReturn(name);
        context.getOrCreateIndirectReferenceFor(existing2);
        assertFalse(context.hasIndirectReferenceFor(name));
    }

    @Test
    public void createNonStorableInObjectStreamIndirectReference()
    {
        assertNotNull(context.createNonStorableInObjectStreamIndirectReference());
    }

    @Test
    public void hasIndirectReferenceFor()
    {
        PDFWriteContext anotherContext = new PDFWriteContext(null, WriteOption.OBJECT_STREAMS);
        COSDictionary dic = new COSDictionary();
        COSDictionary anotherDic = new COSDictionary();
        anotherContext.createIndirectReferenceFor(anotherDic);
        context.getOrCreateIndirectReferenceFor(dic);
        assertTrue(context.hasIndirectReferenceFor(dic));
        assertFalse(context.hasIndirectReferenceFor(new COSDictionary()));
        assertFalse(context.hasIndirectReferenceFor(anotherDic));
    }

    @Test
    public void addExisting()
    {
        ExistingIndirectCOSObject existing = mock(ExistingIndirectCOSObject.class);
        when(existing.id())
                .thenReturn(new IndirectCOSObjectIdentifier(new COSObjectKey(10, 0), "Source"));
        when(existing.hasId()).thenReturn(Boolean.TRUE);
        context.addExistingReference(existing);
        assertTrue(context.hasIndirectReferenceFor(existing));
        assertEquals(existing.id().objectIdentifier,
                context.getIndirectReferenceFor(existing).xrefEntry().key());
    }

    @Test
    public void written()
    {
        assertEquals(0, context.written());
        context.addWritten(entry);
        assertEquals(1, context.written());
    }

    @Test
    public void hasWritten()
    {
        assertFalse(context.hasWritten(entry));
        context.addWritten(entry);
        assertTrue(context.hasWritten(entry));
    }

    @Test
    public void addGetWritten()
    {
        assertNull(context.getWritten(10L));
        context.addWritten(entry);
        assertEquals(entry, context.getWritten(10L));
    }

    @Test
    public void highestWritten()
    {
        context.addWritten(entry);
        context.addWritten(entry2);
        assertEquals(entry2, context.highestWritten());
    }

    @Test
    public void writing()
    {
        GeneralEncryptionAlgorithm encryptor = mock(GeneralEncryptionAlgorithm.class);
        PDFWriteContext victim = new PDFWriteContext(encryptor);
        COSObjectKey key = new COSObjectKey(10, 0);
        victim.writing(key);
        verify(encryptor).setCurrentCOSObjectKey(key);
    }
}
