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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.apache.pdfbox.cos.COSObjectKey;
import org.apache.pdfbox.xref.CompressedXrefEntry;
import org.apache.pdfbox.xref.Xref;
import org.apache.pdfbox.xref.XrefEntry;
import org.junit.Test;

/**
 * @author Andrea Vacondio
 *
 */
public class XrefTest
{

    @Test
    public void add(){
        Xref xref = new Xref();
        XrefEntry entry = XrefEntry.inUseEntry(50, 4000, 0);
        assertNull(xref.add(entry));
        assertEquals(entry, xref.add(XrefEntry.inUseEntry(50, 2000, 0)));
    }

    @Test
    public void contains()
    {
        Xref xref = new Xref();
        XrefEntry entry = XrefEntry.inUseEntry(50, 4000, 0);
        xref.add(entry);
        assertTrue(xref.contains(new COSObjectKey(50, 0)));
    }

    @Test
    public void get()
    {
        Xref xref = new Xref();
        XrefEntry entry = XrefEntry.inUseEntry(50, 4000, 0);
        xref.add(entry);
        assertEquals(entry, xref.get(new COSObjectKey(50, 0)));
    }

    @Test
    public void values()
    {
        Xref xref = new Xref();
        xref.add(XrefEntry.inUseEntry(50, 4000, 0));
        xref.add(CompressedXrefEntry.compressedEntry(20, 50));
        assertEquals(2, xref.values().size());
    }
}
