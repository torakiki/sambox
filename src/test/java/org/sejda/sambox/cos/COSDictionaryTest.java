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

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @author Andrea Vacondio
 *
 */
public class COSDictionaryTest
{

    @Test
    public void getDictionaryObjectMultipleKeys()
    {
        var victim = COSDictionary.of(COSName.A, COSInteger.ONE);
        assertEquals(COSInteger.ONE,
                victim.getDictionaryObject(COSName.A, COSName.B, COSInteger.class));
        assertEquals(COSInteger.ONE,
                victim.getDictionaryObject(COSName.B, COSName.A, COSInteger.class));
        assertNull(victim.getDictionaryObject(COSName.A, COSName.B, COSName.class));
        assertNull(victim.getDictionaryObject(COSName.C, COSName.B, COSInteger.class));
    }

    @Test
    public void testComputeIfAbsent()
    {
        COSDictionary victim = new COSDictionary();
        COSName key = COSName.A;

        var result = victim.computeIfAbsent(key, k -> COSInteger.ONE);
        assertEquals(COSInteger.ONE, result);
        assertEquals(result, victim.getItem(key));
    }

    @Test
    public void testComputeIfAbsentWithBaseMappingFunction()
    {
        COSDictionary victim = new COSDictionary();
        COSName key = COSName.A;
        COSInteger value = COSInteger.ONE;

        // Test when the key is absent and a new value is computed
        COSBase result = victim.computeIfAbsent(key, k -> value);
        assertSame(value, result);
        assertSame(value, victim.getItem(key));

        // Test when the key is present
        COSBase existingResult = victim.computeIfAbsent(key, k -> COSInteger.TWO);
        assertSame(value, existingResult);
    }

    @Test
    public void testComputeIfAbsentWithBaseMappingFunctionNullValue()
    {
        COSDictionary victim = new COSDictionary();
        COSName key = COSName.A;

        // Test when the mapping function returns null
        COSBase result = victim.computeIfAbsent(key, k -> null);
        assertNull(result);
        assertNull(victim.getItem(key));
    }

    @Test
    public void testComputeIfAbsentWithTypedMappingFunction()
    {
        COSDictionary victim = new COSDictionary();
        COSName key = COSName.A;
        COSInteger value = COSInteger.ONE;

        // Test when the key is absent and a new value is computed
        COSInteger result = victim.computeIfAbsent(key, k -> value, COSInteger.class);
        assertSame(value, result);
        assertSame(value, victim.getItem(key));

        // Test when the key is present
        COSInteger existingResult = victim.computeIfAbsent(key, k -> COSInteger.TWO,
                COSInteger.class);
        assertSame(value, existingResult);
    }

    @Test
    public void testComputeIfAbsentWithTypedMappingFunctionNullValue()
    {
        COSDictionary victim = new COSDictionary();
        COSName key = COSName.A;

        // Test when the mapping function returns null
        COSInteger result = victim.computeIfAbsent(key, k -> null, COSInteger.class);
        assertNull(result);
        assertNull(victim.getItem(key));
    }

    @Test
    public void testComputeIfAbsentWithTypedMappingFunctionWrongType()
    {
        COSName key = COSName.A;
        COSString value = COSString.parseLiteral("test");
        var victim = COSDictionary.of(key, value);

        // Test when the key is present but the type is wrong
        COSInteger result = victim.computeIfAbsent(key, k -> COSInteger.ONE, COSInteger.class);
        assertSame(COSInteger.ONE, result);
        assertSame(COSInteger.ONE, victim.getItem(key));
    }

    @Test
    public void testComputeIfAbsentWithBaseMappingFunctionExistingKey()
    {
        COSName key = COSName.A;
        COSInteger initialValue = COSInteger.ONE;
        COSInteger newValue = COSInteger.TWO;

        var victim = COSDictionary.of(key, initialValue);

        // Ensure computeIfAbsent does not override existing key
        COSBase result = victim.computeIfAbsent(key, k -> newValue);
        assertSame(initialValue, result);
        assertSame(initialValue, victim.getItem(key));
    }

    @Test
    public void testComputeIfAbsentWithTypedMappingFunctionExistingKey()
    {
        COSName key = COSName.A;
        COSInteger initialValue = COSInteger.ONE;
        COSInteger newValue = COSInteger.TWO;

        var victim = COSDictionary.of(key, initialValue);

        // Ensure computeIfAbsent does not override existing key
        COSInteger result = victim.computeIfAbsent(key, k -> newValue, COSInteger.class);
        assertSame(initialValue, result);
        assertSame(initialValue, victim.getItem(key));
    }

    @Test
    public void testComputeIfAbsentWithTypedMappingFunctionWrongTypeExistingKey()
    {
        COSName key = COSName.A;
        COSString initialValue = COSString.parseLiteral("test");

        var victim = COSDictionary.of(key, initialValue);

        // Ensure computeIfAbsent does override existing key when type is wrong
        COSInteger result = victim.computeIfAbsent(key, k -> COSInteger.ONE, COSInteger.class);
        assertSame(COSInteger.ONE, result);
        assertSame(COSInteger.ONE, victim.getItem(key));
    }

    @Test
    public void testComputeIfAbsentWithTypedMappingFunctionCorrectTypeExistingKey()
    {
        COSName key = COSName.A;
        COSInteger initialValue = COSInteger.ONE;

        var victim = COSDictionary.of(key, initialValue);

        // Ensure computeIfAbsent does not override existing key when type is correct
        COSInteger result = victim.computeIfAbsent(key, k -> COSInteger.TWO, COSInteger.class);
        assertSame(initialValue, result);
        assertSame(initialValue, victim.getItem(key));
    }

    @Test
    public void getKeyForValueWhenValueExists()
    {
        var value = COSInteger.THREE;
        var victim = COSDictionary.of(COSName.A, value);

        assertEquals(COSName.A, victim.getKeyForValue(value));
    }

    @Test
    public void getKeyForValueWhenValueDoesNotExist()
    {
        var victim = COSDictionary.of(COSName.A, COSInteger.ONE);

        assertNull(victim.getKeyForValue(COSInteger.TWO));
    }

    @Test
    public void getKeyForValueWithEmptyDictionary()
    {
        var victim = new COSDictionary();

        assertNull(victim.getKeyForValue(COSInteger.ONE));
    }

    @Test
    public void getKeyForValueWithNullValue()
    {
        var victim = COSDictionary.of(COSName.A, COSInteger.ONE);

        assertNull(victim.getKeyForValue(null));
    }

    @Test
    public void getKeyForValueReturnsFirstKeyWhenMultipleKeysHaveSameValue()
    {
        var value = COSInteger.ONE;
        var victim = new COSDictionary();
        victim.setItem(COSName.A, value);
        victim.setItem(COSName.B, value);
        victim.setItem(COSName.C, value);

        assertEquals(COSName.A, victim.getKeyForValue(value));
    }

    @Test
    public void getKeyForValueIgnoresNullValuesInDictionary()
    {
        var value = COSInteger.ONE;
        var victim = new COSDictionary();
        victim.setItem(COSName.A, null);
        victim.setItem(COSName.B, value);

        assertEquals(COSName.B, victim.getKeyForValue(value));
    }

    @Test
    public void getKeyForValueWithDifferentTypes()
    {
        var stringValue = COSString.parseLiteral("test");
        var nameValue = COSName.getPDFName("Name");
        var intValue = COSInteger.get(42);

        var victim = new COSDictionary();
        victim.setItem(COSName.A, stringValue);
        victim.setItem(COSName.B, nameValue);
        victim.setItem(COSName.C, intValue);

        assertEquals(COSName.A, victim.getKeyForValue(stringValue));
        assertEquals(COSName.B, victim.getKeyForValue(nameValue));
        assertEquals(COSName.C, victim.getKeyForValue(intValue));
    }

    @Test
    @DisplayName("Keys with different bytes but the same text (Latin-1 /F#E9 and UTF-8 /F#C3#A9, both \"F\u00e9\") are distinct keys and both entries are kept")
    public void keepsKeysWithSameText()
    {
        var latin1 = COSName.getPDFName("F\u00e9".getBytes(ISO_8859_1));
        var utf8 = COSName.getPDFName("F\u00e9".getBytes(UTF_8));
        var victim = COSDictionary.of(latin1, COSInteger.ONE, utf8, COSInteger.TWO);
        assertEquals(2, victim.size());
        assertEquals(COSInteger.ONE, victim.getItem(latin1));
        assertEquals(COSInteger.TWO, victim.getItem(utf8));
    }

    @Test
    public void findKeyByTextPrefersUtf8Key()
    {
        var latin1 = COSName.getPDFName("F\u00e9".getBytes(ISO_8859_1));
        var utf8 = COSName.getPDFName("F\u00e9".getBytes(UTF_8));
        var victim = COSDictionary.of(latin1, COSInteger.ONE, utf8, COSInteger.TWO);
        assertSame(utf8, victim.findKeyByText("F\u00e9").orElseThrow());
    }

    @Test
    public void findKeyByTextFallsBackToNonUtf8Key()
    {
        var latin1 = COSName.getPDFName("F\u00e9".getBytes(ISO_8859_1));
        var victim = COSDictionary.of(latin1, COSInteger.ONE);
        assertSame(latin1, victim.findKeyByText("F\u00e9").orElseThrow());
    }

    @Test
    public void findKeyByTextMissing()
    {
        var victim = COSDictionary.of(COSName.A, COSInteger.ONE);
        assertTrue(victim.findKeyByText("B").isEmpty());
        assertTrue(victim.findKeyByText(null).isEmpty());
    }
}
