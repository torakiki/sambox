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
package org.sejda.sambox.util;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * @author Uwe Pachler
 */
public class TestSort
{

    <T extends Comparable<T>> void doTest(T[] input, T[] expected)
    {
        List<T> list = Arrays.asList(input);
        IterativeMergeSort.sort(list, new Comparator<T>()
        {
            @Override
            public int compare(T comparable, T o)
            {
                return comparable.compareTo(o);
            }
        });
        assertArrayEquals(list.toArray(new Object[input.length]), expected);
    }

    /**
     * Test for different cases.
     */
    @Test
    public void testSort()
    {

        doTest(new Integer[] { 9, 8, 7, 6, 5, 4, 3, 2, 1 },
                new Integer[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 });
        doTest(new Integer[] { 4, 3, 2, 1, 9, 8, 7, 6, 5 },
                new Integer[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 });
        doTest(new Integer[] {}, new Integer[] {});
        doTest(new Integer[] { 5 }, new Integer[] { 5 });
        doTest(new Integer[] { 5, 6 }, new Integer[] { 5, 6 });
        doTest(new Integer[] { 6, 5 }, new Integer[] { 5, 6 });

        Random rnd = new Random(12345);
        for (int cnt = 0; cnt < 100; ++cnt)
        {
            int len = rnd.nextInt(20000) + 2;
            Integer[] input = new Integer[len];
            Integer[] expected = new Integer[len];
            for (int i = 0; i < len; ++i)
            {
                // choose values so that there are some duplicates
                expected[i] = input[i] = rnd.nextInt(rnd.nextInt(100) + 1);
            }
            Arrays.sort(expected);
            doTest(input, expected);
        }
    }
}
