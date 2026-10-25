/*
 * Created on 25/10/26
 * Copyright 2026 by Sober Lemur S.r.l. (info@soberlemur.com).
 *
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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.sejda.io.SeekableSources;
import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.encryption.StandardSecurity;
import org.sejda.sambox.encryption.StandardSecurityEncryption;
import org.sejda.sambox.input.PDFParser;
import org.sejda.sambox.pdmodel.PDPage;

public class EncryptingIndirectReferencesAwareCOSWriterTest
{
    @Test
    public void stringSharedByTwoObjectsIsEncryptedOnce() throws IOException
    {
        for (var encryption : StandardSecurityEncryption.values())
        {
            var encrypted = writeWithCopiedPage(encryption);
            try (var document = PDFParser.parse(
                    SeekableSources.inMemorySeekableSourceFrom(encrypted), "user"))
            {
                for (var page : document.getPages())
                {
                    var annotation = firstAnnotation(page.getCOSObject());
                    assertEquals("Shared note", annotation.getString(COSName.CONTENTS),
                            encryption.name());
                    assertEquals("Author", annotation.getString(COSName.T), encryption.name());
                }
            }
        }
    }

    private byte[] writeWithCopiedPage(StandardSecurityEncryption encryption) throws IOException
    {
        var out = new ByteArrayOutputStream();
        try (var document = PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(
                getClass().getResourceAsStream("/sambox/text_annotation.pdf"))))
        {
            // shallow copies of the page and of its annotation, the copied annotation has the same /Contents and /T
            // COSString instances of the original one
            var copy = document.getPage(0).getCOSObject().duplicate();
            copy.setItem(COSName.ANNOTS, new COSArray(firstAnnotation(copy).duplicate()));
            document.addPage(new PDPage(copy));
            document.writeTo(out, new StandardSecurity("owner", "user", encryption, true));
        }
        return out.toByteArray();
    }

    private static COSDictionary firstAnnotation(COSDictionary page)
    {
        return page.getDictionaryObject(COSName.ANNOTS, COSArray.class)
                .getObject(0, COSDictionary.class);
    }
}
