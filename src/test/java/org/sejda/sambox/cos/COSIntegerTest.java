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

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.Test;

/**
 * @author Andrea Vacondio
 *
 */
public class COSIntegerTest
{

    @Test
    public void equalsContract()
    {
        EqualsVerifier.forClass(COSInteger.class).withIgnoredFields("id")
                .suppress(Warning.NULL_FIELDS).verify();
    }

    @Test
    public void intValue()
    {
        assertEquals(20, COSInteger.get(20).intValue());
    }

    @Test
    public void negativeIntValue()
    {
        assertEquals(-20, COSInteger.get(-20).intValue());
    }

    @Test
    public void longValue()
    {
        assertEquals(2L, COSInteger.get(2).longValue());
    }

    @Test
    public void floatValue()
    {
        assertEquals(2, COSInteger.get(2).floatValue(), 0);
    }

}
