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
package org.sejda.sambox;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;

import org.junit.Ignore;

/**
 * @author Andrea Vacondio
 */
@Ignore
public final class TestUtils
{

    private static final NotInstanceOf NOT_INSTANCE_OF = new NotInstanceOf();

    private TestUtils()
    {
        // util
    }

    /**
     * Sets the given property to the given instance at the given value.
     * 
     * @param instance
     * @param propertyName
     * @param propertyValue
     */
    public static void setProperty(Object instance, String propertyName, Object propertyValue)
    {
        Field field;
        try
        {
            field = instance.getClass().getDeclaredField(propertyName);
            field.setAccessible(true);
            field.set(instance, propertyValue);
        }
        catch (NoSuchFieldException | IllegalAccessException e)
        {
            throw new IllegalStateException(String.format("Unable to set field %s", propertyName),
                    e);
        }
    }

    /**
     * Test that the equals and hashCode implementations respect the general rules being reflexive, transitive and
     * symmetric.
     * 
     * @param <T>
     * @param eq1 equal instance
     * @param eq2 equal instance
     * @param eq3 equal instance
     * @param diff not equal instance
     */
    public static <T> void testEqualsAndHashCodes(T eq1, T eq2, T eq3, T diff)
    {
        // null safe
        assertFalse(eq1.equals(null));

        // not instance of
        assertFalse(eq1.equals(NOT_INSTANCE_OF));

        // reflexive
        assertTrue(eq1.equals(eq1));
        assertTrue(eq1.hashCode() == eq1.hashCode());

        // symmetric
        assertTrue(eq1.equals(eq2));
        assertTrue(eq2.equals(eq1));
        assertTrue(eq1.hashCode() == eq2.hashCode());
        assertFalse(eq2.equals(diff));
        assertFalse(diff.equals(eq2));
        assertFalse(diff.hashCode() == eq2.hashCode());

        // transitive
        assertTrue(eq1.equals(eq2));
        assertTrue(eq2.equals(eq3));
        assertTrue(eq1.equals(eq3));
        assertTrue(eq1.hashCode() == eq2.hashCode());
        assertTrue(eq2.hashCode() == eq3.hashCode());
        assertTrue(eq1.hashCode() == eq3.hashCode());
    }

    /**
     * Class used to test instance of returning false.
     * 
     * @author Andrea Vacondio
     * 
     */
    private static final class NotInstanceOf
    {
        // nothing
    }

}
