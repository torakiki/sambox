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
package org.sejda.sambox.pdmodel.common;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * @author Andrea Vacondio
 *
 */
public class PDRectangleTest
{

    @Test
    public void testRotate()
    {
        PDRectangle victim = new PDRectangle(10, 50, 120, 200);
        PDRectangle rotated = victim.rotate();
        assertEquals(victim.getLowerLeftX(), rotated.getLowerLeftX(), 0);
        assertEquals(victim.getLowerLeftY(), rotated.getLowerLeftY(), 0);
        assertEquals(victim.getWidth(), rotated.getHeight(), 0);
        assertEquals(victim.getHeight(), rotated.getWidth(), 0);
    }

    @Test
    public void testRotate90Degrees()
    {
        PDRectangle victim = new PDRectangle(10, 50, 120, 200);
        PDRectangle rotated = victim.rotate(90);
        assertEquals(victim.getLowerLeftX(), rotated.getLowerLeftX(), 0);
        assertEquals(victim.getLowerLeftY(), rotated.getLowerLeftY(), 0);
        assertEquals(victim.getWidth(), rotated.getHeight(), 0);
        assertEquals(victim.getHeight(), rotated.getWidth(), 0);
    }

    @Test
    public void testRotate180Degrees()
    {
        PDRectangle victim = new PDRectangle(10, 50, 120, 200);
        PDRectangle rotated = victim.rotate(180);
        assertEquals(victim.getLowerLeftX(), rotated.getLowerLeftX(), 0);
        assertEquals(victim.getLowerLeftY(), rotated.getLowerLeftY(), 0);
        assertEquals(victim.getWidth(), rotated.getWidth(), 0);
        assertEquals(victim.getHeight(), rotated.getHeight(), 0);
    }

    @Test
    public void testRotate270Degrees()
    {
        PDRectangle victim = new PDRectangle(10, 50, 120, 200);
        PDRectangle rotated = victim.rotate(270);
        assertEquals(victim.getLowerLeftX(), rotated.getLowerLeftX(), 0);
        assertEquals(victim.getLowerLeftY(), rotated.getLowerLeftY(), 0);
        assertEquals(victim.getWidth(), rotated.getHeight(), 0);
        assertEquals(victim.getHeight(), rotated.getWidth(), 0);
    }

    @Test
    public void testRotate360Degrees()
    {
        PDRectangle victim = new PDRectangle(10, 50, 120, 200);
        PDRectangle rotated = victim.rotate(360);
        assertEquals(victim.getLowerLeftX(), rotated.getLowerLeftX(), 0);
        assertEquals(victim.getLowerLeftY(), rotated.getLowerLeftY(), 0);
        assertEquals(victim.getWidth(), rotated.getWidth(), 0);
        assertEquals(victim.getHeight(), rotated.getHeight(), 0);
    }

    @Test
    public void testRotate450Degrees()
    {
        PDRectangle victim = new PDRectangle(10, 50, 120, 200);
        PDRectangle rotated = victim.rotate(450);
        assertEquals(victim.getLowerLeftX(), rotated.getLowerLeftX(), 0);
        assertEquals(victim.getLowerLeftY(), rotated.getLowerLeftY(), 0);
        assertEquals(victim.getWidth(), rotated.getHeight(), 0);
        assertEquals(victim.getHeight(), rotated.getWidth(), 0);
    }

}
