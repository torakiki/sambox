/*
 * Created on 27/ago/2015
 * Copyright 2010 by Andrea Vacondio (andrea.vacondio@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sejda.sambox.output;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.sejda.commons.util.IOUtils;
import org.sejda.io.BufferedCountingChannelWriter;
import org.sejda.io.CountingWritableByteChannel;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSString;
import org.sejda.sambox.cos.IndirectCOSObjectReference;

/**
 * @author Andrea Vacondio
 */
public class IndirectReferencesAwareCOSWriterTest
{

    // TODO make sure we reuse things, even when we add Existing and wrapped COSBase

    private BufferedCountingChannelWriter writer;
    private IndirectReferencesAwareCOSWriter victim;
    private PDFWriteContext context;

    @Before
    public void setUp()
    {
        context = new PDFWriteContext(null, null);
        writer = mock(BufferedCountingChannelWriter.class);
        victim = new IndirectReferencesAwareCOSWriter(writer, context);
    }

    @After
    public void tearDown()
    {
        IOUtils.closeQuietly(victim);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullWriterConstructor()
    {
        new IndirectReferencesAwareCOSWriter((BufferedCountingChannelWriter) null, context);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullChannelConstructor()
    {
        new IndirectReferencesAwareCOSWriter((CountingWritableByteChannel) null, context);
    }

    @Test
    public void visitCOSDictionary() throws Exception
    {
        COSDictionary dictionary = new COSDictionary();
        COSString string = COSString.parseLiteral("Chuck");
        dictionary.setItem(COSName.A, string);
        dictionary.setInt(COSName.C, 151);
        IndirectCOSObjectReference ref = context.getOrCreateIndirectReferenceFor(string);
        InOrder inOrder = Mockito.inOrder(writer);
        victim.visit(dictionary);
        inOrder.verify(writer, times(2)).write((byte) 0x3C);
        inOrder.verify(writer).writeEOL();
        inOrder.verify(writer).write((byte) 0x2F);
        inOrder.verify(writer).write((byte) 0x41);
        inOrder.verify(writer).write((byte) 0x20);
        inOrder.verify(writer).write(ref.toString());
        inOrder.verify(writer).writeEOL();
        inOrder.verify(writer).write((byte) 0x2F);
        inOrder.verify(writer).write((byte) 0x43);
        inOrder.verify(writer).write((byte) 0x20);
        inOrder.verify(writer).write("151");
        inOrder.verify(writer).writeEOL();
        inOrder.verify(writer, times(2)).write((byte) 0x3E);
        inOrder.verify(writer).writeEOL();
    }

}
