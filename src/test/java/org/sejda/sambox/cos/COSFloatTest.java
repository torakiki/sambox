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

import java.io.IOException;
import java.math.BigDecimal;

import org.junit.Test;
import org.sejda.sambox.TestUtils;

/**
 * @author Andrea Vacondio
 *
 */
public class COSFloatTest
{
    @Test
    public void equals() throws IOException
    {
        TestUtils.testEqualsAndHashCodes(COSFloat.get("2.2"), new COSFloat(2.2f),
                COSFloat.get("2.2"), COSFloat.get("32.2"));
    }

    @Test
    public void intValue() throws IOException
    {
        assertEquals((int) 2.04, COSFloat.get("2.04").intValue());
    }

    @Test
    public void longValue() throws IOException
    {
        assertEquals((long) 2.04, COSFloat.get("2.04").longValue());
    }

    @Test
    public void floatValue() throws IOException
    {
        assertEquals(2.04f, COSFloat.get("2.04").floatValue(), 0);
    }

    @Test
    public void emptyExponentialValue() throws IOException
    {
        assertEquals(4.72856f, COSFloat.get("4.72856e").floatValue(), 0);
    }

    @Test
    public void emptyExponentialCapitalEValue() throws IOException
    {
        assertEquals(4.72856f, COSFloat.get("4.72856E").floatValue(), 0);
    }

    @Test(expected = IOException.class)
    public void invalidExponential() throws IOException
    {
        new COSFloat("4.72856k");
    }

    @Test
    public void exponentialValue() throws IOException
    {
        assertEquals(0.0000204f, COSFloat.get("2.04e-5").floatValue(), 0);
    }

    @Test
    public void exponentialCapitalEValue() throws IOException
    {
        assertEquals(0.0000204f, COSFloat.get("2.04E-5").floatValue(), 0);
    }

    @Test
    public void negativeFloatValue() throws IOException
    {
        assertEquals(-2.04f, COSFloat.get("-2.04").floatValue(), 0);
    }

    @Test
    public void pdfbox2990() throws IOException
    {
        assertEquals(-0.0000033917698f, COSFloat.get("0.00000-33917698").floatValue(), 0);
        assertEquals(-0.0040f, COSFloat.get("0.00-40").floatValue(), 0);
    }

    @Test
    public void sambox75() throws IOException
    {
        assertEquals(0, COSFloat.get("0-66.7").floatValue(), 0);
        assertEquals(0, COSFloat.get("0-").floatValue(), 0);
        assertEquals(0, COSFloat.get("0-66").floatValue(), 0);
    }

    @Test(expected = IOException.class)
    public void invalidValue() throws IOException
    {
        new COSFloat("Chuck");
    }

    @Test
    public void pdfbox3589() throws IOException
    {
        assertEquals(-242.3f, COSFloat.get("---242.3").floatValue(), 0);
        assertEquals(-242.3f, COSFloat.get("-+-242.3").floatValue(), 0);
        assertEquals(-242.3f, COSFloat.get("--+242.3").floatValue(), 0);
        assertEquals(-242.3f, COSFloat.get("--242.3").floatValue(), 0);
        assertEquals(-242.3f, COSFloat.get("-+242.3").floatValue(), 0);
    }

    @Test
    public void pdfbox3500() throws IOException
    {
        assertEquals(-0.262f, COSFloat.get("0.-262").floatValue(), 0);
    }

    @Test
    public void testDoubleNegative() throws IOException
    {
        // PDFBOX-4289
        assertEquals(-16.33f, COSFloat.get("--16.33").floatValue(), 0);
    }

    @Test
    public void multipleDots() throws IOException
    {
        assertEquals(415.750795f, COSFloat.get("415.75.795").floatValue(), 0);
        assertEquals(-415.750795f, COSFloat.get("-415.75.795").floatValue(), 0);
    }

    @Test
    public void testVerySmallValues() throws IOException
    {
        double smallValue = Float.MIN_VALUE / 10d;

        assertEquals("Test must be performed with a value smaller than Float.MIN_VALUE.", -1,
                Double.compare(smallValue, Float.MIN_VALUE));

        // 1.4012984643248171E-46
        String asString = String.valueOf(smallValue);
        COSFloat cosFloat = new COSFloat(asString);
        assertEquals(0.0f, cosFloat.floatValue(), 0);

        // 0.00000000000000000000000000000000000000000000014012984643248171
        asString = new BigDecimal(asString).toPlainString();
        cosFloat = new COSFloat(asString);
        assertEquals(0.0f, cosFloat.floatValue(), 0);

        smallValue *= -1;

        // -1.4012984643248171E-46
        asString = String.valueOf(smallValue);
        cosFloat = new COSFloat(asString);
        assertEquals(0.0f, cosFloat.floatValue(), 0);

        // -0.00000000000000000000000000000000000000000000014012984643248171
        asString = new BigDecimal(asString).toPlainString();
        cosFloat = new COSFloat(asString);
        assertEquals(0.0f, cosFloat.floatValue(), 0);
    }

    @Test
    public void testVeryLargeValues() throws IOException
    {
        double largeValue = Float.MAX_VALUE * 10d;

        assertEquals("Test must be performed with a value larger than Float.MAX_VALUE.", 1,
                Double.compare(largeValue, Float.MIN_VALUE));

        // 1.4012984643248171E-46
        String asString = String.valueOf(largeValue);
        COSFloat cosFloat = new COSFloat(asString);
        assertEquals(Float.MAX_VALUE, cosFloat.floatValue(), 0);

        // 0.00000000000000000000000000000000000000000000014012984643248171
        asString = new BigDecimal(asString).toPlainString();
        cosFloat = new COSFloat(asString);
        assertEquals(Float.MAX_VALUE, cosFloat.floatValue(), 0);

        largeValue *= -1;

        // -1.4012984643248171E-46
        asString = String.valueOf(largeValue);
        cosFloat = new COSFloat(asString);
        assertEquals(-Float.MAX_VALUE, cosFloat.floatValue(), 0);

        // -0.00000000000000000000000000000000000000000000014012984643248171
        asString = new BigDecimal(asString).toPlainString();
        cosFloat = new COSFloat(asString);
        assertEquals(-Float.MAX_VALUE, cosFloat.floatValue(), 0);
    }

}
