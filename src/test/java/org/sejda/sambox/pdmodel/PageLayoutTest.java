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
package org.sejda.sambox.pdmodel;

import static org.junit.Assert.assertEquals;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

/**
 * @author Andrea Vacondio
 *
 */
public class PageLayoutTest
{
    /**
     * Test for completeness (PDFBOX-3362).
     */
    @Test
    public void testValues()
    {
        Set<PageLayout> pageLayoutSet = EnumSet.noneOf(PageLayout.class);
        Set<String> stringSet = new HashSet<>();
        for (PageLayout pl : PageLayout.values())
        {
            String s = pl.stringValue();
            stringSet.add(s);
            pageLayoutSet.add(PageLayout.fromString(s));
        }
        assertEquals(PageLayout.values().length, pageLayoutSet.size());
        assertEquals(PageLayout.values().length, stringSet.size());
    }
}
