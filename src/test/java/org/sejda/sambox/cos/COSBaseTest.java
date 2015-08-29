/*
 * Created on 28/ago/2015
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * @author Andrea Vacondio
 *
 */
public class COSBaseTest
{

    @Test
    public void noId()
    {
        COSDictionary victim = new COSDictionary();
        assertNull(victim.id());
        assertFalse(victim.hasId());
    }

    @Test
    public void id()
    {
        COSDictionary victim = new COSDictionary();
        victim.idIfAbsent("Chuck");
        assertEquals("Chuck", victim.id());
    }

    @Test
    public void idIfAbsent()
    {
        COSDictionary victim = new COSDictionary();
        victim.idIfAbsent("Chuck");
        victim.idIfAbsent("Norris");
        assertEquals("Chuck", victim.id());
    }
}
