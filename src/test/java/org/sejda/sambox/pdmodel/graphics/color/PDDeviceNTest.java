/*
 * Created on 19/11/26
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
package org.sejda.sambox.pdmodel.graphics.color;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSInteger;
import org.sejda.sambox.cos.COSName;

class PDDeviceNTest
{
    @Test
    void attributesThatAreNotADictionaryAreIgnored() throws IOException
    {
        var deviceN = new PDDeviceN(deviceNArray(COSName.getPDFName("NotADictionary")));

        assertNull(deviceN.getAttributes());
        assertEquals(List.of("Cyan"), deviceN.getColorantNames());
    }

    @Test
    void dictionaryAttributesAreRead() throws IOException
    {
        var deviceN = new PDDeviceN(deviceNArray(new COSDictionary()));

        assertNotNull(deviceN.getAttributes());
    }

    @Test
    void nullAttributesWrapAnEmptyDictionary()
    {
        assertNotNull(new PDDeviceNAttributes(null).getCOSObject());
    }

    private static COSArray deviceNArray(COSBase attributes)
    {
        return new COSArray(COSName.DEVICEN, new COSArray(COSName.getPDFName("Cyan")),
                COSName.DEVICECMYK, cyanTintTransform(), attributes);
    }

    private static COSDictionary cyanTintTransform()
    {
        var function = new COSDictionary();
        function.setInt(COSName.FUNCTION_TYPE, 2);
        function.setItem(COSName.DOMAIN, COSArray.fromFloats(new float[] { 0, 1 }));
        function.setItem(COSName.C0, COSArray.fromFloats(new float[] { 0, 0, 0, 0 }));
        function.setItem(COSName.C1, COSArray.fromFloats(new float[] { 1, 0, 0, 0 }));
        function.setItem(COSName.N, COSInteger.ONE);
        return function;
    }
}
