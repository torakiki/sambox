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
package org.sejda.sambox.xref;

import static org.junit.Assert.assertTrue;
import static org.sejda.sambox.xref.CompressedXrefEntry.compressedEntry;

import java.util.Arrays;

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
        CompressedXrefEntry entry = compressedEntry(10, 1234, 3);
        byte[] bytes = entry.toXrefStreamEntry(4, 1);
        byte[] expected = new byte[] { 0b00000010, 0b00000000, 0b00000000, 0b00000100,
                (byte) 0b11010010, 0b00000011 };
        assertTrue(Arrays.equals(expected, bytes));
    }
}
