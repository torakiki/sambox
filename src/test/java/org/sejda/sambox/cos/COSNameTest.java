/*
 * Copyright 2018 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sejda.sambox.cos;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.sejda.io.SeekableSources;
import org.sejda.sambox.input.PDFParser;
import org.sejda.sambox.pdmodel.PDDocument;
import org.sejda.sambox.pdmodel.PDPage;

public class COSNameTest
{
    /**
     * PDFBOX-4076: Check that characters outside of US_ASCII are not replaced with "?".
     *
     * @throws IOException
     */
    @Test
    public void PDFBox4076() throws IOException
    {
        String special = "中国你好!";
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PDDocument document = new PDDocument())
        {
            PDPage page = new PDPage();
            document.addPage(page);
            document.getDocumentCatalog().getCOSObject()
                    .setString(COSName.getPDFName(special), special);

            document.writeTo(baos);
        }
        try (PDDocument document = PDFParser.parse(
                SeekableSources.inMemorySeekableSourceFrom(baos.toByteArray())))
        {
            COSDictionary catalogDict = document.getDocumentCatalog().getCOSObject();
            assertTrue(catalogDict.containsKey(special));
            assertEquals(special, catalogDict.getString(special));
        }
    }

    @Test
    public void latin1AndUtf8AreDifferentNames()
    {
        var latin1 = COSName.getPDFName("\u00e9".getBytes(ISO_8859_1));
        var utf8 = COSName.getPDFName("\u00e9".getBytes(UTF_8));
        assertNotEquals(latin1, utf8);
        assertEquals("\u00e9", latin1.getName());
        assertEquals("\u00e9", utf8.getName());
    }

    @Test
    public void sameBytesSameInstance()
    {
        var bytes = "m\u00e4nnlich".getBytes(ISO_8859_1);
        assertSame(COSName.getPDFName(bytes), COSName.getPDFName(bytes.clone()));
        assertSame(COSName.TYPE, COSName.getPDFName("Type"));
        assertSame(COSName.TYPE, COSName.getPDFName("Type".getBytes(ISO_8859_1)));
    }

    @Test
    public void getPDFNameFromStringIsUtf8()
    {
        assertArrayEquals("\u00e9".getBytes(UTF_8), COSName.getPDFName("\u00e9").getBytes());
    }

    @Test
    public void getNameDecodesUtf8()
    {
        assertEquals("\u4e2d\u56fd", COSName.getPDFName("\u4e2d\u56fd".getBytes(UTF_8)).getName());
    }

    @Test
    public void getNameFallbackForInvalidUtf8()
    {
        assertEquals("m\u00e4nnlich",
                COSName.getPDFName("m\u00e4nnlich".getBytes(ISO_8859_1)).getName());
        assertEquals("\u0080", COSName.getPDFName(new byte[] { (byte) 0x80 }).getName());
    }

    @Test
    public void getBytesIsDefensiveCopy()
    {
        var name = COSName.getPDFName("Chuck");
        name.getBytes()[0] = 'D';
        assertArrayEquals("Chuck".getBytes(ISO_8859_1), name.getBytes());
    }

    @Test
    public void compareToIsUnsigned()
    {
        var withHighByte = COSName.getPDFName(new byte[] { (byte) 0xE9 });
        assertTrue(withHighByte.compareTo(COSName.getPDFName("z")) > 0);
        assertTrue(COSName.getPDFName("z").compareTo(withHighByte) < 0);
    }

    @Test
    public void emptyName()
    {
        var empty = COSName.getPDFName(new byte[0]);
        assertSame(COSName.getPDFName(""), empty);
        assertEquals("", empty.getName());
    }
}
