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
package org.sejda.sambox.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

/**
 * @author Andrea Vacondio
 *
 */
public class SpecVersionUtilsTest
{

    @Test(expected = IOException.class)
    public void failingParseHeaderString() throws IOException
    {
        SpecVersionUtils.parseHeaderString("ChuckNorris");
    }

    @Test
    public void parseHeaderString() throws IOException
    {
        assertEquals("1.7", SpecVersionUtils.parseHeaderString("%PDF-1.7"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void failingAtLeastNullVersiont()
    {
        SpecVersionUtils.isAtLeast(null, SpecVersionUtils.V1_1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void failingisAtLeastNullAtLeast()
    {
        SpecVersionUtils.isAtLeast(SpecVersionUtils.V1_1, null);
    }

    public void isAtLeast()
    {
        assertTrue(SpecVersionUtils.isAtLeast(SpecVersionUtils.V1_5, SpecVersionUtils.V1_4));
        assertTrue(SpecVersionUtils.isAtLeast(SpecVersionUtils.V1_5, SpecVersionUtils.V1_5));
        assertFalse(SpecVersionUtils.isAtLeast(SpecVersionUtils.V1_3, SpecVersionUtils.V1_5));
    }

}
