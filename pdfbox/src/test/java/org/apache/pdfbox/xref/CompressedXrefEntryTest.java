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

import static org.apache.pdfbox.xref.CompressedXrefEntry.compressedEntry;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * @author Andrea Vacondio
 *
 */
public class CompressedXrefEntryTest
{
    @Test(expected = IllegalArgumentException.class)
    public void negativeObjectStreamNumber()
    {
        compressedEntry(10, -10, 1);
    }

    @Test
    public void unknownOffset()
    {
        assertTrue(compressedEntry(10, 100, 1).isUnknownOffset());
    }

    @Test
    public void toXrefStreamEntry()
    {
        CompressedXrefEntry entry = compressedEntry(10, 5, 3);
        byte[] bytes = entry.toXrefStreamEntry(2, 1);
        assertEquals(4, bytes.length);
        assertEquals(0b00000010, bytes[0]);
        assertEquals(0b00000000, bytes[1]);
        assertEquals(0b00000101, bytes[2]);
        assertEquals(0b00000011, bytes[3]);
    }
}
