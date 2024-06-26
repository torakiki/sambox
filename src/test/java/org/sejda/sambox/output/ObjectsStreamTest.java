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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.InflaterOutputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sejda.commons.util.IOUtils;
import org.sejda.sambox.SAMBox;
import org.sejda.sambox.cos.COSInteger;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.output.ObjectsStreamPDFBodyObjectsWriter.ObjectsStream;

/**
 * @author Andrea Vacondio
 *
 */
public class ObjectsStreamTest
{

    private ObjectsStream victim;
    private PDFWriteContext context;

    @Before
    public void setUp()
    {
        context = new PDFWriteContext(null, null);
        victim = new ObjectsStream(context);
    }

    @After
    public void tearDown()
    {
        IOUtils.closeQuietly(victim);
    }

    @Test
    public void hasItems() throws IOException
    {
        assertFalse(victim.hasItems());
        victim.addItem(context.createIndirectReferenceFor(COSInteger.ZERO));
        assertTrue(victim.hasItems());
    }

    @Test
    public void isFull() throws IOException
    {
        System.setProperty(SAMBox.OBJECTS_STREAM_SIZE_PROPERTY, "2");
        assertFalse(victim.isFull());
        victim.addItem(context.createIndirectReferenceFor(COSInteger.ZERO));
        assertFalse(victim.isFull());
        victim.addItem(context.createIndirectReferenceFor(COSInteger.ZERO));
        assertTrue(victim.isFull());
        System.getProperties().remove(SAMBox.OBJECTS_STREAM_SIZE_PROPERTY);
    }

    @Test
    public void addItem() throws IOException
    {
        victim.addItem(context.createIndirectReferenceFor(COSInteger.ZERO));
        victim.addItem(context.createIndirectReferenceFor(COSInteger.THREE));
        victim.prepareForWriting();
        assertEquals(COSName.OBJ_STM.getName(), victim.getNameAsString(COSName.TYPE));
        assertEquals(2, victim.getInt(COSName.N));
        assertEquals(8, victim.getInt(COSName.FIRST));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtils.copy(victim.getFilteredStream(), new InflaterOutputStream(out));
        byte[] data = new byte[] { 50, 32, 48, 32, 51, 32, 50, 32, 48, 32, 51, 32 };
        assertArrayEquals(data, out.toByteArray());
    }
}
