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
package org.sejda.sambox.input;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSInteger;
import org.sejda.sambox.cos.COSNull;
import org.sejda.sambox.cos.COSObjectKey;
import org.sejda.sambox.cos.COSVisitor;

/**
 * @author Andrea Vacondio
 *
 */
public class ExistingIndirectCOSObjectTest
{
    private IndirectObjectsProvider provider;
    private COSObjectKey key = new COSObjectKey(10, 0);
    private ExistingIndirectCOSObject victim;

    @Before
    public void setUp()
    {
        provider = mock(IndirectObjectsProvider.class);
        when(provider.id()).thenReturn("sourceId");
        victim = new ExistingIndirectCOSObject(key, provider);
    }

    @Test
    public void nonExistingGetCOSObject()
    {
        assertEquals(COSNull.NULL, victim.getCOSObject());
        verify(provider).get(key);
    }

    @Test
    public void existingGetCOSObject()
    {
        COSBase value = COSInteger.ONE;
        when(provider.get(key)).thenReturn(value);
        assertEquals(value, victim.getCOSObject());
        assertEquals(value, victim.getCOSObject());
        verify(provider, times(2)).get(key);
    }

    @Test
    public void getAssignSameId()
    {
        COSBase value = new COSDictionary();
        assertFalse(value.hasId());
        when(provider.get(key)).thenReturn(value);
        assertEquals(victim.id(), victim.getCOSObject().id());
        assertTrue(value.hasId());

    }

    @Test
    public void accept() throws IOException
    {
        COSDictionary value = mock(COSDictionary.class);
        COSVisitor visitor = mock(COSVisitor.class);
        when(provider.get(key)).thenReturn(value);
        victim.accept(visitor);
        verify(value).accept(visitor);
    }

    @Test
    public void sourceId()
    {
        assertEquals("10 0 sourceId", victim.id());
    }

    @Test
    public void release()
    {
        COSBase value = COSInteger.ONE;
        when(provider.get(key)).thenReturn(value);
        assertEquals(value, victim.getCOSObject());
        victim.releaseCOSObject();
        verify(provider).release(key);
        assertEquals(value, victim.getCOSObject());
        verify(provider, times(2)).get(key);
    }
}
