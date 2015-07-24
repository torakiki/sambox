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
package org.apache.pdfbox.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Test;

/**
 * @author Andrea Vacondio
 *
 */
public class PoolTest
{
    private Supplier<Object> creator;

    @Before
    public void setUp()
    {
        creator = mock(Supplier.class);
        when(creator.get()).thenReturn(new Object());
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullCreator()
    {
        new Pool(null, 10);
    }

    @Test
    public void creatorIsCalled()
    {
        Pool<Object> victim = new Pool<>(creator, 10);
        verify(creator, never()).get();
        victim.borrow();
        verify(creator).get();
    }

    @Test
    public void objectIsPooledCalled()
    {
        Pool<Object> victim = new Pool<>(() -> new Object(), 1);
        Object entry = victim.borrow();
        Object entry2 = victim.borrow();
        assertNotEquals(entry, entry2);
        victim.give(entry);
        assertEquals(entry, victim.borrow());
    }

    @Test
    public void outOfBoundsObjectIsNotPooled()
    {
        Pool<Object> victim = new Pool<>(() -> new Object(), 1);
        Object entry = victim.borrow();
        Object entry2 = victim.borrow();
        victim.give(entry);
        victim.give(entry2);
        assertEquals(entry, victim.borrow());
        assertNotEquals(entry2, victim.borrow());
    }

    @Test
    public void onGiveHit()
    {
        Consumer<Object> onGive = mock(Consumer.class);
        Pool<Object> victim = new Pool<>(() -> new Object(), 1).onGive(onGive);
        Object entry = victim.borrow();
        victim.give(entry);
        verify(onGive).accept(entry);
    }
}
