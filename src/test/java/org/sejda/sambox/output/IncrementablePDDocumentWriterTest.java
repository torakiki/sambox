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
package org.sejda.sambox.output;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sejda.io.CountingWritableByteChannel.from;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.sejda.io.DevNullWritableByteChannel;
import org.sejda.io.SeekableSources;
import org.sejda.sambox.TestUtils;
import org.sejda.sambox.input.IncrementablePDDocument;
import org.sejda.sambox.input.PDFParser;

/**
 * @author Andrea Vacondio
 *
 */
public class IncrementablePDDocumentWriterTest
{

    private IncrementablePDDocumentWriter victim;
    private DefaultPDFWriter writer;
    private IndirectObjectsWriter objectsWriter;

    @Before
    public void setUp()
    {
        this.writer = mock(DefaultPDFWriter.class);
        this.objectsWriter = mock(IndirectObjectsWriter.class);
        when(writer.writer()).thenReturn(objectsWriter);
        this.victim = new IncrementablePDDocumentWriter(from(new DevNullWritableByteChannel()),
                null);
        TestUtils.setProperty(victim, "writer", this.writer);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullConstructor()
    {
        new IncrementablePDDocumentWriter(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullDoc() throws IOException
    {
        victim.write(null);
    }

    @Test(expected = IllegalStateException.class)
    public void fullScan() throws Exception
    {
        try (IncrementablePDDocument incrementable = PDFParser
                .parseToIncrement(SeekableSources.inMemorySeekableSourceFrom(
                        getClass().getResourceAsStream("/sambox/test_xref_missing_xref.pdf"))))
        {
            victim.write(incrementable);
        }
    }

}
