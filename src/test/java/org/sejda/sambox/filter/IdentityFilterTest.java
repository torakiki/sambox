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
package org.sejda.sambox.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.sejda.sambox.cos.COSDictionary.of;

import org.junit.Test;
import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSInteger;
import org.sejda.sambox.cos.COSName;

/**
 * @author Andrea Vacondio
 */
public class IdentityFilterTest
{
    private IdentityFilter victim = new IdentityFilter();

    @Test
    public void nullGetDecode()
    {
        COSDictionary params = victim.getDecodeParams(new COSDictionary(), 10);
        assertNotNull(params);
    }

    @Test
    public void validNameDicDP()
    {
        COSDictionary filter = of(COSName.FILTER, COSName.JBIG2_DECODE);
        COSDictionary dp = of(COSName.A, COSInteger.get(5));
        filter.setItem(COSName.DECODE_PARMS, dp);
        COSDictionary params = victim.getDecodeParams(filter, 0);
        assertEquals(dp, params);
    }

    @Test
    public void validArrayArrayDP()
    {
        COSDictionary filter = of(COSName.FILTER, new COSArray(COSName.JBIG2_DECODE));
        COSDictionary dp = of(COSName.A, COSInteger.get(5));
        filter.setItem(COSName.DECODE_PARMS, new COSArray(dp));
        COSDictionary params = victim.getDecodeParams(filter, 0);
        assertEquals(dp, params);
    }

    @Test
    public void invalidTypeGetDecode()
    {
        COSDictionary filter = of(COSName.FILTER, COSName.JBIG2_DECODE, COSName.A,
                COSInteger.get(5));
        COSDictionary params = victim.getDecodeParams(filter, 0);
        assertNotNull(params);
        assertEquals(0, params.size());
    }

    @Test
    public void invalidTypeArrayValueGetDecode()
    {
        COSDictionary dic = of(COSName.DECODE_PARMS,
                new COSArray(COSInteger.THREE, new COSDictionary()));
        COSDictionary params = victim.getDecodeParams(dic, 0);
        assertNotNull(params);
        assertEquals(0, params.size());
    }

    @Test
    public void arrayValueGetDecode()
    {
        COSDictionary dic = new COSDictionary();
        COSDictionary value = of(COSName.A, COSInteger.get(213));
        COSArray array = new COSArray(value);
        dic.setItem(COSName.DECODE_PARMS, array);
        COSDictionary params = victim.getDecodeParams(dic, 0);
        assertNotNull(params);
        assertEquals(0, params.size());
    }
}
