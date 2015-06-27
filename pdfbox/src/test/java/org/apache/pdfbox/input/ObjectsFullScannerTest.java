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
package org.apache.pdfbox.input;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.pdfbox.cos.COSObjectKey;
import org.apache.pdfbox.input.source.SeekableSources;
import org.apache.pdfbox.util.IOUtils;
import org.apache.pdfbox.xref.Xref;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Andrea Vacondio
 *
 */
public class ObjectsFullScannerTest
{

    private SourceReader reader;
    private ObjectsFullScanner victim;

    @Before
    public void setUp() throws Exception
    {
        reader = new SourceReader(SeekableSources.inMemorySeekableSourceFrom(getClass()
                .getResourceAsStream("/input/test_multiple_xref_tables.pdf")));
        victim = new ObjectsFullScanner(reader);
    }

    @After
    public void tearDown() throws IOException
    {
        IOUtils.close(reader);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullArgument()
    {
        new ObjectsFullScanner(null);
    }

    @Test
    public void positionIsRestored() throws IOException
    {
        reader.position(50);
        victim.entries();
        assertEquals(50, reader.position());
    }

    @Test
    public void scan()
    {
        Xref xref = victim.entries();
        assertEquals(7, xref.values().size());
        assertEquals(317, xref.get(new COSObjectKey(6, 0)).getByteOffset());
        assertEquals(717, xref.get(new COSObjectKey(3, 0)).getByteOffset());
    }

}
