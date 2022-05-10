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
package org.sejda.sambox.cos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.junit.Test;

/**
 * @author Andrea Vacondio
 *
 */
public class COSNumberTest
{

    @Test
    public void get() throws IOException
    {
        assertEquals(COSInteger.ZERO, COSNumber.get("0"));
        assertEquals(COSInteger.ONE, COSNumber.get("1"));
        assertEquals(COSInteger.TWO, COSNumber.get("2"));
        assertEquals(COSInteger.THREE, COSNumber.get("3"));
        // Test some arbitrary ints
        assertEquals(COSInteger.get(100), COSNumber.get("100"));
        assertEquals(COSInteger.get(256), COSNumber.get("256"));
        assertEquals(COSInteger.get(-1000), COSNumber.get("-1000"));
        assertEquals(COSInteger.get(2000), COSNumber.get("+2000"));
        // Some arbitrary floats
        assertEquals(new COSFloat(1.1f), COSNumber.get("1.1"));
        assertEquals(new COSFloat(100f), COSNumber.get("100.0"));
        assertEquals(new COSFloat(-100.001f), COSNumber.get("-100.001"));
        // according to the specs the exponential shall not be used
        // but obviously there some
        assertNotNull(COSNumber.get("-2e-006"));
        assertNotNull(COSNumber.get("-8e+05"));
        // PDFBOX-2569: some numbers start with "+"
        assertEquals(COSNumber.get("1"), COSNumber.get("+1"));
        assertEquals(COSNumber.get("123"), COSNumber.get("+123"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void getNull() throws IOException
    {
        COSNumber.get(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getBlank() throws IOException
    {
        COSNumber.get("  ");
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidString() throws IOException
    {
        COSNumber.get("ChuckNorris");
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidChar() throws IOException
    {
        COSNumber.get("A");
    }

    @Test
    public void pdfbox592() throws IOException
    {
        assertEquals(COSInteger.ZERO, COSNumber.get("-"));
        assertEquals(COSInteger.ZERO, COSNumber.get("."));
    }

    @Test
    public void pdfbox3589() throws IOException
    {
        assertEquals(COSInteger.ZERO, COSNumber.get("--242"));
        assertEquals(COSInteger.ZERO, COSNumber.get("-+242"));
        assertEquals(COSInteger.ZERO, COSNumber.get("+-242"));
        assertEquals(COSInteger.ZERO, COSNumber.get("++242"));

        assertEquals(new COSFloat(-242f), COSNumber.get("--242.0"));
        assertEquals(new COSFloat(-242f), COSNumber.get("-+242.0"));
        assertEquals(new COSFloat(-242f), COSNumber.get("---242.0"));
    }
}
