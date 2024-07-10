/*
 * Created on 20 ott 2018
 * Copyright 2010 by Andrea Vacondio (andrea.vacondio@gmail.com).
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * 
 * http://www.apache.org/licenses/LICENSE-2.0 
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License. 
 */
package org.sejda.sambox.cos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * @author Andrea Vacondio
 *
 */
public class COSDictionaryTest
{

    @Test
    public void getDictionaryObjectMultipleKeys()
    {
        COSDictionary victim = COSDictionary.of(COSName.A, COSInteger.ONE);
        assertEquals(COSInteger.ONE,
                victim.getDictionaryObject(COSName.A, COSName.B, COSInteger.class));
        assertEquals(COSInteger.ONE,
                victim.getDictionaryObject(COSName.B, COSName.A, COSInteger.class));
        assertNull(victim.getDictionaryObject(COSName.A, COSName.B, COSName.class));
        assertNull(victim.getDictionaryObject(COSName.C, COSName.B, COSInteger.class));
    }
}
