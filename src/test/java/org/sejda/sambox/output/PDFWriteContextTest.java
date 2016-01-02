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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSObjectKey;
import org.sejda.sambox.cos.IndirectCOSObjectIdentifier;
import org.sejda.sambox.cos.IndirectCOSObjectReference;
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
        context = new PDFWriteContext(GeneralEncryptionAlgorithm.IDENTITY);
    }

    @Test
    public void hasWriteOption()
    {
        PDFWriteContext context = new PDFWriteContext(GeneralEncryptionAlgorithm.IDENTITY,
                WriteOption.OBJECT_STREAMS);
        assertTrue(context.hasWriteOption(WriteOption.OBJECT_STREAMS));
        assertFalse(context.hasWriteOption(WriteOption.SYNC_BODY_WRITE));
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
    public void getOrCreateIndirectReferenceFor()
    {
        COSDictionary dic = new COSDictionary();
        IndirectCOSObjectReference ref = context.getOrCreateIndirectReferenceFor(dic);
        IndirectCOSObjectReference ref2 = context.getOrCreateIndirectReferenceFor(dic);
        assertEquals(ref, ref2);
    }

    @Test
    public void getIndirectReferenceFor()
    {
        COSDictionary dic = new COSDictionary();
        IndirectCOSObjectReference ref = context.getOrCreateIndirectReferenceFor(dic);
        assertEquals(ref, context.getIndirectReferenceFor(dic));
    }

    @Test
    public void hasIndirectReferenceFor()
    {
        PDFWriteContext anotherContext = new PDFWriteContext(GeneralEncryptionAlgorithm.IDENTITY,
                WriteOption.OBJECT_STREAMS);
        COSDictionary dic = new COSDictionary();
        COSDictionary anotherDic = new COSDictionary();
        anotherContext.createIndirectReferenceFor(anotherDic);
        context.getOrCreateIndirectReferenceFor(dic);
        assertTrue(context.hasIndirectReferenceFor(dic));
        assertFalse(context.hasIndirectReferenceFor(new COSDictionary()));
        assertFalse(context.hasIndirectReferenceFor(anotherDic));
    }

    @Test
    public void written()
    {
        assertEquals(0, context.written());
        context.putWritten(entry);
        assertEquals(1, context.written());
    }

    @Test
    public void hasWritten()
    {
        assertFalse(context.hasWritten(entry));
        context.putWritten(entry);
        assertTrue(context.hasWritten(entry));
    }

    @Test
    public void putGetWritten()
    {
        assertNull(context.getWritten(10L));
        context.putWritten(entry);
        assertEquals(entry, context.getWritten(10L));
    }

    @Test
    public void highestLowestWritten()
    {
        context.putWritten(entry);
        context.putWritten(entry2);
        assertEquals(entry, context.lowestWritten());
        assertEquals(entry2, context.highestWritten());
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullEncryptor()
    {
        new PDFWriteContext(null);
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
