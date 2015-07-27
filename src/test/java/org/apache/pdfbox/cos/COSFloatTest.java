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
package org.apache.pdfbox.cos;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.pdfbox.TestUtils;
import org.junit.Test;

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
    public void doubleValue() throws IOException
    {
        assertEquals(2.04, COSInteger.get("2.04").doubleValue(), 0);
    }

    @Test
    public void floatValue() throws IOException
    {
        assertEquals(2.04f, COSInteger.get("2.04").floatValue(), 0);
    }

    @Test(expected = IOException.class)
    public void invalidValue() throws IOException
    {
        new COSFloat("Chuck");
    }
}
