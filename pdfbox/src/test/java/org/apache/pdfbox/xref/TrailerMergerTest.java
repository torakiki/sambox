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
package org.apache.pdfbox.xref;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.xref.TrailerMerger;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Andrea Vacondio
 *
 */
public class TrailerMergerTest
{
    private TrailerMerger merger;

    @Before
    public void setUp()
    {
        merger = new TrailerMerger();
    }

    @Test
    public void trailerNotNull()
    {
        assertNotNull(merger.getTrailer());
    }

    @Test
    public void mergeDoesntOverwrite()
    {
        COSDictionary first = new COSDictionary();
        first.setString("name", "Chuck");
        first.setString("surname", "Norris");
        merger.mergeTrailerWithoutOverwriting(1, first);
        COSDictionary second = new COSDictionary();
        second.setString("name", "Steven");
        second.setString("middle", "Frederic");
        second.setString("surname", "Seagal");
        merger.mergeTrailerWithoutOverwriting(10, second);
        assertEquals("Chuck", merger.getTrailer().getString("name"));
        assertEquals("Frederic", merger.getTrailer().getString("middle"));
        assertEquals("Norris", merger.getTrailer().getString("surname"));
    }

    @Test
    public void history()
    {
        COSDictionary first = new COSDictionary();
        first.setString("key", "First");
        COSDictionary second = new COSDictionary();
        second.setString("key", "Second");
        COSDictionary third = new COSDictionary();
        third.setString("key", "Third");
        merger.mergeTrailerWithoutOverwriting(1, first);
        merger.mergeTrailerWithoutOverwriting(2, second);
        merger.mergeTrailerWithoutOverwriting(3, third);
        assertEquals(first, merger.getFirstTrailer());
        assertEquals(third, merger.getLastTrailer());
    }

    @Test
    public void reset()
    {
        COSDictionary first = new COSDictionary();
        first.setString("key", "First");
        merger.mergeTrailerWithoutOverwriting(1, first);
        assertEquals(1, merger.getTrailer().size());
        merger.reset();
        assertEquals(0, merger.getTrailer().size());
    }
}
