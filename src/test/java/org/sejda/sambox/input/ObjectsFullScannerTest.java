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

import java.io.IOException;

import org.junit.Test;
import org.sejda.io.SeekableSources;
import org.sejda.sambox.cos.COSObjectKey;
import org.sejda.sambox.xref.Xref;

/**
 * @author Andrea Vacondio
 *
 */
public class ObjectsFullScannerTest
{

    @Test(expected = IllegalArgumentException.class)
    public void nullArgument()
    {
        new ObjectsFullScanner(null);
    }

    @Test
    public void positionIsRestored() throws IOException
    {
        try (SourceReader reader = new SourceReader(SeekableSources.inMemorySeekableSourceFrom(
                getClass().getResourceAsStream("/sambox/test_multiple_xref_tables.pdf"))))
        {
            ObjectsFullScanner victim = new ObjectsFullScanner(reader);
            reader.position(50);
            victim.entries();
            assertEquals(50, reader.position());
        }
    }

    @Test
    public void scan() throws IOException
    {
        try (SourceReader reader = new SourceReader(SeekableSources.inMemorySeekableSourceFrom(
                getClass().getResourceAsStream("/sambox/test_multiple_xref_tables.pdf"))))
        {
            ObjectsFullScanner victim = new ObjectsFullScanner(reader);
            Xref xref = victim.entries();
            assertEquals(7, xref.values().size());
            assertEquals(317, xref.get(new COSObjectKey(6, 0)).getByteOffset());
            assertEquals(717, xref.get(new COSObjectKey(3, 0)).getByteOffset());
        }
    }

    @Test
    public void scanSpacedObjectsNumber() throws IOException
    {
        try (SourceReader reader = new SourceReader(SeekableSources.inMemorySeekableSourceFrom(
                getClass().getResourceAsStream("/sambox/spaced-object-numbers.txt"))))
        {
            ObjectsFullScanner victim = new ObjectsFullScanner(reader);
            Xref xref = victim.entries();
            assertEquals(3, xref.values().size());
            assertEquals(7, xref.get(new COSObjectKey(83, 0)).getByteOffset());
            assertEquals(212, xref.get(new COSObjectKey(84, 0)).getByteOffset());
            assertEquals(309, xref.get(new COSObjectKey(61, 0)).getByteOffset());
        }
    }

    @Test
    public void scanMultipleObjectOnTheSameLine() throws IOException
    {
        try (SourceReader reader = new SourceReader(SeekableSources.inMemorySeekableSourceFrom(
                getClass().getResourceAsStream("/sambox/multiple-objs-same-line.txt"))))
        {
            ObjectsFullScanner victim = new ObjectsFullScanner(reader);
            Xref xref = victim.entries();
            assertEquals(11, xref.values().size());
            assertEquals(0, xref.get(new COSObjectKey(12, 0)).getByteOffset());
            assertEquals(100, xref.get(new COSObjectKey(10, 0)).getByteOffset());
            assertEquals(195, xref.get(new COSObjectKey(5, 0)).getByteOffset());
            assertEquals(646, xref.get(new COSObjectKey(6, 0)).getByteOffset());
            assertEquals(911, xref.get(new COSObjectKey(15, 0)).getByteOffset());
            assertEquals(1190, xref.get(new COSObjectKey(17, 0)).getByteOffset());
        }
    }

}
