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
package org.apache.pdfbox.pdfparser.xref;

import static org.apache.pdfbox.pdfparser.xref.CompressedXrefEntry.compressedEntry;
import static org.apache.pdfbox.pdfparser.xref.XrefEntry.freeEntry;
import static org.apache.pdfbox.pdfparser.xref.XrefEntry.inUseEntry;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.pdfbox.cos.COSObjectKey;
import org.junit.Test;

/**
 * @author Andrea Vacondio
 *
 */
public class XrefEntryTest
{

    @Test(expected = IllegalArgumentException.class)
    public void negativeObjectNumber()
    {
        inUseEntry(-10, 10, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativeGenerationNumber()
    {
        inUseEntry(10, 10, -10);
    }

    @Test
    public void key()
    {
        assertEquals(new COSObjectKey(10, 1), inUseEntry(10, 20, 1).key());
    }
    @Test
    public void unknownOffset()
    {
        assertFalse(inUseEntry(10, 20, 1).isUnknownOffset());
    }

    @Test
    public void own()
    {
        assertTrue(inUseEntry(10, 2000, 0).owns(compressedEntry(20, 10)));
        assertFalse(inUseEntry(10, 2000, 0).owns(compressedEntry(20, 50)));
        assertFalse(inUseEntry(10, 2000, 0).owns(null));
        assertFalse(inUseEntry(10, 2000, 0).owns(inUseEntry(20, 500, 10)));
    }

    @Test
    public void types()
    {
        assertEquals(XrefType.IN_USE, inUseEntry(10, -10, 0).getType());
        assertEquals(XrefType.COMPRESSED, compressedEntry(10, 10).getType());
        assertEquals(XrefType.FREE, freeEntry(10, 50, 1).getType());
    }
}
