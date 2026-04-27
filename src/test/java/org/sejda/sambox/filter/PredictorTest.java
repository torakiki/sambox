/*
 * Copyright 2015 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sejda.sambox.filter;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.sejda.sambox.cos.COSDictionary.of;
import static org.sejda.sambox.cos.COSInteger.get;
import static org.sejda.sambox.filter.Predictor.calcSetBitSeq;
import static org.sejda.sambox.filter.Predictor.getBitSeq;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;

/**
 *
 * @author Tilman Hausherr
 */
public class PredictorTest
{
    private COSDictionary decodeParams(int predictor, int columns, int bitsPerComponent)
    {
        return of(COSName.PREDICTOR, get(predictor), COSName.COLUMNS, get(columns),
                COSName.BITS_PER_COMPONENT, get(bitsPerComponent));
    }

    private COSDictionary decodeParams(int predictor, int columns, int bitsPerComponent, int colors)
    {
        var params = decodeParams(predictor, columns, bitsPerComponent);
        params.setInt(COSName.COLORS, colors);
        return params;
    }

    /**
     * Test of getBitSeq method, of class Predictor.
     */
    @Test
    public void testGetBitSeq()
    {
        assertEquals(Integer.parseInt("11111111", 2),
                getBitSeq(Integer.parseInt("11111111", 2), 0, 8));
        assertEquals(Integer.parseInt("00000000", 2),
                getBitSeq(Integer.parseInt("00000000", 2), 0, 8));
        assertEquals(Integer.parseInt("1", 2), getBitSeq(Integer.parseInt("11111111", 2), 0, 1));
        assertEquals(Integer.parseInt("0", 2), getBitSeq(Integer.parseInt("00000000", 2), 0, 1));
        assertEquals(Integer.parseInt("001", 2), getBitSeq(Integer.parseInt("00110001", 2), 0, 3));
        assertEquals(Integer.parseInt("10101010", 2),
                getBitSeq(Integer.parseInt("10101010", 2), 0, 8));
        assertEquals(Integer.parseInt("10", 2), getBitSeq(Integer.parseInt("10101010", 2), 0, 2));
        assertEquals(Integer.parseInt("01", 2), getBitSeq(Integer.parseInt("10101010", 2), 1, 2));
        assertEquals(Integer.parseInt("10", 2), getBitSeq(Integer.parseInt("10101010", 2), 2, 2));
        assertEquals(Integer.parseInt("101", 2), getBitSeq(Integer.parseInt("10101010", 2), 3, 3));
        assertEquals(Integer.parseInt("1010101", 2),
                getBitSeq(Integer.parseInt("10101010", 2), 1, 7));
        assertEquals(Integer.parseInt("01", 2), getBitSeq(Integer.parseInt("10101010", 2), 3, 2));
        assertEquals(Integer.parseInt("00110001", 2),
                getBitSeq(Integer.parseInt("00110001", 2), 0, 8));
        assertEquals(Integer.parseInt("10001", 2),
                getBitSeq(Integer.parseInt("00110001", 2), 0, 5));
        assertEquals(Integer.parseInt("0011", 2), getBitSeq(Integer.parseInt("00110001", 2), 4, 4));
        assertEquals(Integer.parseInt("110", 2), getBitSeq(Integer.parseInt("00110001", 2), 3, 3));
        assertEquals(Integer.parseInt("00", 2), getBitSeq(Integer.parseInt("00110001", 2), 6, 2));
        assertEquals(Integer.parseInt("1111", 2), getBitSeq(Integer.parseInt("11110000", 2), 4, 4));
        assertEquals(Integer.parseInt("11", 2), getBitSeq(Integer.parseInt("11110000", 2), 6, 2));
        assertEquals(Integer.parseInt("0000", 2), getBitSeq(Integer.parseInt("11110000", 2), 0, 4));
    }

    /**
     * Test of calcSetBitSeq method, of class Predictor.
     */
    @Test
    public void testCalcSetBitSeq()
    {
        assertEquals(Integer.parseInt("00000000", 2),
                calcSetBitSeq(Integer.parseInt("11111111", 2), 0, 8, 0));
        assertEquals(Integer.parseInt("00000001", 2),
                calcSetBitSeq(Integer.parseInt("11111111", 2), 0, 8, 1));
        assertEquals(Integer.parseInt("11111111", 2),
                calcSetBitSeq(Integer.parseInt("11111111", 2), 0, 1, 1));
        assertEquals(Integer.parseInt("11111101", 2),
                calcSetBitSeq(Integer.parseInt("11111111", 2), 0, 2, 1));
        assertEquals(Integer.parseInt("11111001", 2),
                calcSetBitSeq(Integer.parseInt("11111111", 2), 0, 3, 1));
        assertEquals(Integer.parseInt("00000001", 2),
                calcSetBitSeq(Integer.parseInt("00000000", 2), 0, 2, 1));
        assertEquals(Integer.parseInt("11110001", 2),
                calcSetBitSeq(Integer.parseInt("11111111", 2), 0, 4, 1));
        assertEquals(Integer.parseInt("11100011", 2),
                calcSetBitSeq(Integer.parseInt("11111111", 2), 1, 4, 1));
        assertEquals(Integer.parseInt("00000010", 2),
                calcSetBitSeq(Integer.parseInt("00000000", 2), 1, 1, 1));
        assertEquals(Integer.parseInt("11111111", 2),
                calcSetBitSeq(Integer.parseInt("11111111", 2), 7, 1, 1));
        assertEquals(Integer.parseInt("01111111", 2),
                calcSetBitSeq(Integer.parseInt("11111111", 2), 7, 1, 0));
        assertEquals(Integer.parseInt("10000000", 2),
                calcSetBitSeq(Integer.parseInt("00000000", 2), 7, 1, 1));
        assertEquals(Integer.parseInt("00000000", 2),
                calcSetBitSeq(Integer.parseInt("00000000", 2), 7, 1, 0));
        assertEquals(Integer.parseInt("01000000", 2),
                calcSetBitSeq(Integer.parseInt("00000000", 2), 6, 1, 1));
        assertEquals(Integer.parseInt("00000000", 2),
                calcSetBitSeq(Integer.parseInt("00000000", 2), 6, 1, 0));
        assertEquals(Integer.parseInt("00110000", 2),
                calcSetBitSeq(Integer.parseInt("00000000", 2), 3, 3, 6));
        assertEquals(Integer.parseInt("01100000", 2),
                calcSetBitSeq(Integer.parseInt("00000000", 2), 4, 3, 6));
        assertEquals(Integer.parseInt("11000000", 2),
                calcSetBitSeq(Integer.parseInt("00000000", 2), 5, 3, 6));
        assertEquals(Integer.parseInt("11111111", 2),
                calcSetBitSeq(Integer.parseInt("00000000", 2), 0, 8, 0xFF));
        assertEquals(Integer.parseInt("11111111", 2),
                calcSetBitSeq(Integer.parseInt("11111111", 2), 0, 8, 0xFF));
        assertEquals(0x7E, calcSetBitSeq(0xA5, 0, 8, 0xD9 + 0xA5));

        // check truncation
        assertEquals(Integer.parseInt("00000010", 2),
                calcSetBitSeq(Integer.parseInt("00000000", 2), 1, 1, 3));
    }

    @Test
    void wrapPredictorThrowsForZeroColumns()
    {
        assertThrows(IOException.class,
                () -> Predictor.wrapPredictor(new ByteArrayOutputStream(), decodeParams(12, 0, 8)));
    }

    @Test
    void wrapPredictorThrowsForNegativeColumns()
    {
        assertThrows(IOException.class, () -> Predictor.wrapPredictor(new ByteArrayOutputStream(),
                decodeParams(12, -1, 8)));
    }

    @Test
    void wrapPredictorThrowsWhenRowLengthOverflowsInt()
    {
        // columns * colors (default 1) * bitsPerComponent (8) + 7 must fit in int
        // Integer.MAX_VALUE * 8 overflows int
        assertThrows(IOException.class, () -> Predictor.wrapPredictor(new ByteArrayOutputStream(),
                decodeParams(12, Integer.MAX_VALUE, 8)));
    }

    @Test
    void wrapPredictorAcceptsLargeColumnsThatDoNotOverflow()
    {
        // well above the previous 1_000_000 cap and still safe: 100M * 1 * 8 + 7 fits in int
        assertDoesNotThrow(() -> Predictor.wrapPredictor(new ByteArrayOutputStream(),
                decodeParams(12, 100_000_000, 8)));
    }

    @Test
    void wrapPredictorAcceptsExactBoundaryRowLength()
    {
        // (Integer.MAX_VALUE - 7) / 8 = 268_435_455 with colors=1, bpc=8 produces 2^31 - 1
        assertDoesNotThrow(() -> Predictor.wrapPredictor(new ByteArrayOutputStream(),
                decodeParams(12, 268_435_455, 8)));
    }

    @Test
    void wrapPredictorRejectsOneOverBoundaryRowLength()
    {
        // 268_435_456 * 8 + 7 exceeds Integer.MAX_VALUE
        assertThrows(IOException.class, () -> Predictor.wrapPredictor(new ByteArrayOutputStream(),
                decodeParams(12, 268_435_456, 8)));
    }

    // bitsPerComponent validation

    @Test
    void wrapPredictorThrowsForInvalidBitsPerComponent()
    {
        assertThrows(IOException.class, () -> Predictor.wrapPredictor(new ByteArrayOutputStream(),
                decodeParams(12, 100, 3)));
    }

    @Test
    void wrapPredictorThrowsForZeroBitsPerComponent()
    {
        assertThrows(IOException.class, () -> Predictor.wrapPredictor(new ByteArrayOutputStream(),
                decodeParams(12, 100, 0)));
    }

    @Test
    void wrapPredictorThrowsForNegativeBitsPerComponent()
    {
        assertThrows(IOException.class, () -> Predictor.wrapPredictor(new ByteArrayOutputStream(),
                decodeParams(12, 100, -1)));
    }

    @Test
    void wrapPredictorAcceptsAllValidBitsPerComponentValues()
    {
        for (int bpc : new int[] { 1, 2, 4, 8, 16 })
        {
            assertDoesNotThrow(() -> Predictor.wrapPredictor(new ByteArrayOutputStream(),
                    decodeParams(12, 100, bpc)));
        }
    }

    // colors validation (slips past wrapPredictor's Math.min when negative)

    @Test
    void wrapPredictorThrowsForNegativeColors()
    {
        assertThrows(IOException.class, () -> Predictor.wrapPredictor(new ByteArrayOutputStream(),
                decodeParams(12, 100, 8, -1)));
    }

    @Test
    void wrapPredictorThrowsForZeroColors()
    {
        assertThrows(IOException.class, () -> Predictor.wrapPredictor(new ByteArrayOutputStream(),
                decodeParams(12, 100, 8, 0)));
    }
}
