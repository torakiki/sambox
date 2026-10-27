/*
 * Created on 27/10/26
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
package org.sejda.sambox.encryption;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.sejda.sambox.encryption.SignatureUtils.isSignature;

import org.junit.jupiter.api.Test;
import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSInteger;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSString;

public class SignatureUtilsTest
{
    @Test
    public void signatureType()
    {
        assertTrue(isSignature(COSDictionary.of(COSName.TYPE, COSName.SIG)));
    }

    @Test
    public void documentTimestampType()
    {
        assertTrue(isSignature(COSDictionary.of(COSName.TYPE, COSName.DOC_TIME_STAMP)));
    }

    @Test
    public void contentsAndByteRangeWithoutType()
    {
        assertTrue(isSignature(COSDictionary.of(COSName.CONTENTS, COSString.parseLiteral("value"),
                COSName.BYTERANGE,
                new COSArray(COSInteger.ZERO, COSInteger.ONE, COSInteger.TWO, COSInteger.THREE))));
    }

    @Test
    public void annotationWithContents()
    {
        assertFalse(isSignature(COSDictionary.of(COSName.TYPE, COSName.ANNOT, COSName.CONTENTS,
                COSString.parseLiteral("A note"))));
    }

    @Test
    public void emptyDictionary()
    {
        assertFalse(isSignature(new COSDictionary()));
    }
}
