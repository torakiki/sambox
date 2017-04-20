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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.List;

import org.junit.Test;
import org.sejda.io.SeekableSources;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSInteger;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSObjectKey;
import org.sejda.sambox.cos.IndirectCOSObjectIdentifier;
import org.sejda.sambox.cos.IndirectCOSObjectReference;
import org.sejda.sambox.pdmodel.PDDocument;
import org.sejda.sambox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.sejda.sambox.util.SpecVersionUtils;

/**
 * @author Andrea Vacondio
 *
 */
public class IncrementablePDDocumentTest
{
    @Test
    public void positiveIncremental() throws IOException
    {
        try (IncrementablePDDocument incrementable = PDFParser
                .parseToIncrement(SeekableSources.inMemorySeekableSourceFrom(
                        getClass().getResourceAsStream("/sambox/simple_test.pdf"))))
        {
            assertNotNull(incrementable);
            assertNotNull(incrementable.incremented());
            assertNotNull(incrementable.trailer());
            assertEquals(11, incrementable.trailer().getCOSObject().getInt(COSName.SIZE));
            assertEquals(new COSObjectKey(10, 0), incrementable.highestExistingReference());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void replaceNullId() throws IOException
    {
        try (IncrementablePDDocument incrementable = new IncrementablePDDocument(new PDDocument(),
                mock(COSParser.class)))
        {
            incrementable.replace(null, new COSDictionary());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativeReplace() throws IOException
    {
        try (IncrementablePDDocument incrementable = new IncrementablePDDocument(new PDDocument(),
                mock(COSParser.class)))
        {
            incrementable.replace(null, COSInteger.TWO);
        }
    }

    @Test
    public void replacements() throws IOException
    {
        try (IncrementablePDDocument incrementable = new IncrementablePDDocument(new PDDocument(),
                mock(COSParser.class)))
        {
            incrementable.replace(
                    new IndirectCOSObjectIdentifier(new COSObjectKey(10, 0), "source"),
                    COSInteger.TWO);
            incrementable.replace(
                    new IndirectCOSObjectIdentifier(new COSObjectKey(11, 0), "source"),
                    new COSDictionary());
            List<IndirectCOSObjectReference> replacements = incrementable.replacements();
            assertEquals(2, replacements.size());
            assertEquals(new COSObjectKey(10, 0), replacements.get(0).xrefEntry().key());
            assertEquals(COSInteger.TWO, replacements.get(0).getCOSObject());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativeModified() throws IOException
    {
        try (IncrementablePDDocument incrementable = new IncrementablePDDocument(new PDDocument(),
                mock(COSParser.class)))
        {
            incrementable.modified(null);
        }
    }

    @Test
    public void modified() throws IOException
    {
        try (IncrementablePDDocument incrementable = PDFParser
                .parseToIncrement(SeekableSources.inMemorySeekableSourceFrom(
                        getClass().getResourceAsStream("/sambox/simple_test.pdf"))))
        {
            assertFalse(incrementable.modified(new COSDictionary()));
            assertTrue(incrementable.modified(incrementable.incremented().getDocumentCatalog()));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativeNewIndirect() throws IOException
    {
        try (IncrementablePDDocument incrementable = new IncrementablePDDocument(new PDDocument(),
                mock(COSParser.class)))
        {
            incrementable.newIndirect(null);
        }
    }

    @Test
    public void newIndirect() throws IOException
    {
        try (IncrementablePDDocument incrementable = new IncrementablePDDocument(new PDDocument(),
                mock(COSParser.class)))
        {
            PDAnnotationLink link = new PDAnnotationLink();
            incrementable.newIndirect(link);
            assertEquals(1, incrementable.newIndirects().size());
            assertEquals(link.getCOSObject(),
                    incrementable.newIndirects().stream().findFirst().get());
        }
    }

    @Test
    public void notEncrypted() throws IOException
    {
        try (IncrementablePDDocument incrementable = PDFParser
                .parseToIncrement(SeekableSources.inMemorySeekableSourceFrom(
                        getClass().getResourceAsStream("/sambox/simple_test.pdf"))))
        {
            assertNull(incrementable.encryptionDictionary());
            assertNull(incrementable.encryptionKey());
        }
    }

    @Test
    public void encrypted() throws IOException
    {
        try (IncrementablePDDocument incrementable = PDFParser
                .parseToIncrement(SeekableSources.inMemorySeekableSourceFrom(
                        getClass().getResourceAsStream("/sambox/encrypted_simple_test.pdf"))))
        {
            assertNotNull(incrementable.encryptionDictionary());
            assertNotNull(incrementable.encryptionKey());
        }
    }

    @Test
    public void close() throws IOException
    {
        IncrementablePDDocument incrementable = PDFParser
                .parseToIncrement(SeekableSources.inMemorySeekableSourceFrom(
                        getClass().getResourceAsStream("/sambox/simple_test.pdf")));
        incrementable.close();
        assertFalse(incrementable.incremented().isOpen());
    }

    @Test(expected = IllegalStateException.class)
    public void setVersionClosed() throws IOException
    {
        IncrementablePDDocument incrementable = PDFParser
                .parseToIncrement(SeekableSources.inMemorySeekableSourceFrom(
                        getClass().getResourceAsStream("/sambox/simple_test.pdf")));
        incrementable.close();
        incrementable.setVersion(SpecVersionUtils.V1_5);
    }

    @Test(expected = IllegalArgumentException.class)
    public void setVersionBlank() throws IOException
    {
        try (IncrementablePDDocument incrementable = new IncrementablePDDocument(new PDDocument(),
                mock(COSParser.class)))
        {
            incrementable.setVersion(" ");
        }
    }

    @Test
    public void setVersion() throws IOException
    {
        try (IncrementablePDDocument incrementable = PDFParser
                .parseToIncrement(SeekableSources.inMemorySeekableSourceFrom(
                        getClass().getResourceAsStream("/sambox/simple_test.pdf"))))
        {
            incrementable.setVersion(SpecVersionUtils.V1_4);
            assertTrue(incrementable.replacements().isEmpty());
            incrementable.setVersion(SpecVersionUtils.V1_5);
            assertTrue(incrementable.replacements().isEmpty());
            incrementable.setVersion(SpecVersionUtils.V1_6);
            assertFalse(incrementable.replacements().isEmpty());
            IndirectCOSObjectReference catalogReplacement = incrementable.replacements().get(0);
            assertEquals(new COSObjectKey(1, 0), catalogReplacement.xrefEntry().key());
            assertEquals(SpecVersionUtils.V1_6, ((COSDictionary) catalogReplacement.getCOSObject())
                    .getNameAsString(COSName.VERSION));
        }
    }

    @Test
    public void requireMinVersion() throws IOException
    {
        try (IncrementablePDDocument incrementable = PDFParser
                .parseToIncrement(SeekableSources.inMemorySeekableSourceFrom(
                        getClass().getResourceAsStream("/sambox/simple_test.pdf"))))
        {
            incrementable.requireMinVersion(SpecVersionUtils.V1_4);
            assertTrue(incrementable.replacements().isEmpty());
            incrementable.requireMinVersion(SpecVersionUtils.V1_5);
            assertTrue(incrementable.replacements().isEmpty());
            incrementable.requireMinVersion(SpecVersionUtils.V1_6);
            assertFalse(incrementable.replacements().isEmpty());
            IndirectCOSObjectReference catalogReplacement = incrementable.replacements().get(0);
            assertEquals(new COSObjectKey(1, 0), catalogReplacement.xrefEntry().key());
            assertEquals(SpecVersionUtils.V1_6, ((COSDictionary) catalogReplacement.getCOSObject())
                    .getNameAsString(COSName.VERSION));
        }
    }
}
