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

import org.junit.Test;
import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSFloat;
import org.sejda.sambox.cos.COSName;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

/**
 * Test the {@link Matrix} class.
 * @author Neil McErlean
 * @since 1.4.0
 */
public class TestMatrix
{
    @Test
    public void testConstructionAndCopy() throws Exception
    {
        Matrix m1 = new Matrix();
        assertMatrixIsPristine(m1);

        Matrix m2 = m1.clone();
        assertNotSame(m1, m2);
        assertMatrixIsPristine(m2);
    }

    @Test
    public void testGetScalingFactor()
    {
        // check scaling factor of an initial matrix
        Matrix m1 = new Matrix();
        assertEquals(1, m1.getScalingFactorX(), 0);
        assertEquals(1, m1.getScalingFactorY(), 0);

        // check scaling factor of an initial matrix
        Matrix m2 = new Matrix(2, 4, 4, 2, 0, 0);
        assertEquals((float) Math.sqrt(20), m2.getScalingFactorX(), 0);
        assertEquals((float) Math.sqrt(20), m2.getScalingFactorY(), 0);
    }

    @Test
    public void testCreateMatrixUsingInvalidInput()
    {
        // anything but a COSArray is invalid and leads to an initial matrix
        Matrix createMatrix = Matrix.createMatrix(COSName.A);
        assertMatrixIsPristine(createMatrix);

        // a COSArray with fewer than 6 entries leads to an initial matrix
        COSArray cosArray = new COSArray();
        cosArray.add(COSName.A);
        createMatrix = Matrix.createMatrix(cosArray);
        assertMatrixIsPristine(createMatrix);

        // a COSArray containing other kind of objects than COSNumber leads to an initial matrix
        cosArray = new COSArray();
        for (int i = 0; i < 6; i++)
        {
            cosArray.add(COSName.A);
        }
        createMatrix = Matrix.createMatrix(cosArray);
        assertMatrixIsPristine(createMatrix);
    }

    @Test
    public void testMultiplication() throws Exception
    {
        // This matrix will not change - we use it to drive the various multiplications.
        final Matrix testMatrix = new Matrix();

        // Create matrix with values
        // [ 0, 1, 2
        // 1, 2, 3
        // 2, 3, 4]
        for (int x = 0; x < 3; x++)
        {
            for (int y = 0; y < 3; y++)
            {
                testMatrix.setValue(x, y, x + y);
            }
        }

        Matrix m1 = testMatrix.clone();
        Matrix m2 = testMatrix.clone();

        // Multiply two matrices together producing a new result matrix.
        Matrix product = m1.multiply(m2);

        assertNotSame(m1, product);
        assertNotSame(m2, product);

        // Operand 1 should not have changed
        assertMatrixValuesEqualTo(new float[] { 0, 1, 2, 1, 2, 3, 2, 3, 4 }, m1);
        // Operand 2 should not have changed
        assertMatrixValuesEqualTo(new float[] { 0, 1, 2, 1, 2, 3, 2, 3, 4 }, m2);
        assertMatrixValuesEqualTo(new float[] { 5, 8, 11, 8, 14, 20, 11, 20, 29 }, product);

        // Multiply two matrices together with the result being written to a third matrix
        // (Any existing values there will be overwritten).
        Matrix resultMatrix = new Matrix();

        Matrix retVal = m1.multiply(m2, resultMatrix);
        assertSame(retVal, resultMatrix);
        // Operand 1 should not have changed
        assertMatrixValuesEqualTo(new float[] { 0, 1, 2, 1, 2, 3, 2, 3, 4 }, m1);
        // Operand 2 should not have changed
        assertMatrixValuesEqualTo(new float[] { 0, 1, 2, 1, 2, 3, 2, 3, 4 }, m2);
        assertMatrixValuesEqualTo(new float[] { 5, 8, 11, 8, 14, 20, 11, 20, 29 }, resultMatrix);

        // Multiply two matrices together with the result being written into the other matrix
        retVal = m1.multiply(m2, m2);
        assertSame(retVal, m2);
        // Operand 1 should not have changed
        assertMatrixValuesEqualTo(new float[] { 0, 1, 2, 1, 2, 3, 2, 3, 4 }, m1);
        assertMatrixValuesEqualTo(new float[] { 5, 8, 11, 8, 14, 20, 11, 20, 29 }, retVal);

        // Multiply two matrices together with the result being written into 'this' matrix
        m1 = testMatrix.clone();
        m2 = testMatrix.clone();

        retVal = m1.multiply(m2, m1);
        assertSame(retVal, m1);
        // Operand 2 should not have changed
        assertMatrixValuesEqualTo(new float[] { 0, 1, 2, 1, 2, 3, 2, 3, 4 }, m2);
        assertMatrixValuesEqualTo(new float[] { 5, 8, 11, 8, 14, 20, 11, 20, 29 }, retVal);

        // Multiply the same matrix with itself with the result being written into 'this' matrix
        m1 = testMatrix.clone();

        retVal = m1.multiply(m1, m1);
        assertSame(retVal, m1);
        assertMatrixValuesEqualTo(new float[] { 5, 8, 11, 8, 14, 20, 11, 20, 29 }, retVal);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalValueNaN1()
    {
        Matrix m = new Matrix();
        m.setValue(0, 0, Float.MAX_VALUE);
        m.multiply(m, m);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalValueNaN2()
    {
        Matrix m = new Matrix();
        m.setValue(0, 0, Float.NaN);
        m.multiply(m, m);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalValuePositiveInfinity()
    {
        Matrix m = new Matrix();
        m.setValue(0, 0, Float.POSITIVE_INFINITY);
        m.multiply(m, m);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalValueNegativeInfinity()
    {
        Matrix m = new Matrix();
        m.setValue(0, 0, Float.NEGATIVE_INFINITY);
        m.multiply(m, m);
    }

    /**
     * Test of PDFBOX-2872 bug
     */
    @Test
    public void testPdfbox2872()
    {
        Matrix m = new Matrix(2, 4, 5, 8, 2, 0);
        COSArray toCOSArray = m.toCOSArray();
        assertEquals(new COSFloat(2), toCOSArray.get(0));
        assertEquals(new COSFloat(4), toCOSArray.get(1));
        assertEquals(new COSFloat(5), toCOSArray.get(2));
        assertEquals(new COSFloat(8), toCOSArray.get(3));
        assertEquals(new COSFloat(2), toCOSArray.get(4));
        assertEquals(new COSFloat(0), toCOSArray.get(5));

    }

    @Test
    public void testGetValues()
    {
        Matrix m = new Matrix(2, 4, 4, 2, 15, 30);
        float[][] values = m.getValues();
        assertEquals(2, values[0][0], 0);
        assertEquals(4, values[0][1], 0);
        assertEquals(0, values[0][2], 0);
        assertEquals(4, values[1][0], 0);
        assertEquals(2, values[1][1], 0);
        assertEquals(0, values[1][2], 0);
        assertEquals(15, values[2][0], 0);
        assertEquals(30, values[2][1], 0);
        assertEquals(1, values[2][2], 0);
    }

    @Test
    public void testScaling()
    {
        Matrix m = new Matrix(2, 4, 4, 2, 15, 30);
        m.scale(2, 3);
        // first row, multiplication with 2
        assertEquals(4, m.getValue(0, 0), 0);
        assertEquals(8, m.getValue(0, 1), 0);
        assertEquals(0, m.getValue(0, 2), 0);

        // second row, multiplication with 3
        assertEquals(12, m.getValue(1, 0), 0);
        assertEquals(6, m.getValue(1, 1), 0);
        assertEquals(0, m.getValue(1, 2), 0);

        // third row, no changes at all
        assertEquals(15, m.getValue(2, 0), 0);
        assertEquals(30, m.getValue(2, 1), 0);
        assertEquals(1, m.getValue(2, 2), 0);
    }

    @Test
    public void testTranslation()
    {
        Matrix m = new Matrix(2, 4, 4, 2, 15, 30);
        m.translate(2, 3);
        // first row, no changes at all
        assertEquals(2, m.getValue(0, 0), 0);
        assertEquals(4, m.getValue(0, 1), 0);
        assertEquals(0, m.getValue(0, 2), 0);

        // second row, no changes at all
        assertEquals(4, m.getValue(1, 0), 0);
        assertEquals(2, m.getValue(1, 1), 0);
        assertEquals(0, m.getValue(1, 2), 0);

        // third row, translated values
        assertEquals(31, m.getValue(2, 0), 0);
        assertEquals(44, m.getValue(2, 1), 0);
        assertEquals(1, m.getValue(2, 2), 0);
    }

    /**
     * This method asserts that the matrix values for the given {@link Matrix} object are equal to
     * the pristine, or original, values.
     *
     * @param m the Matrix to test.
     */
    private void assertMatrixIsPristine(Matrix m)
    {
        assertMatrixValuesEqualTo(new float[] { 1, 0, 0, 0, 1, 0, 0, 0, 1 }, m);
    }

    /**
     * This method asserts that the matrix values for the given {@link Matrix} object have the specified values.
     *
     * @param values the expected values
     * @param m the matrix to test
     */
    private void assertMatrixValuesEqualTo(float[] values, Matrix m)
    {
        float delta = 0.00001f;
        for (int i = 0; i < values.length; i++)
        {
            // Need to convert a (row, column) coordinate into a straight index.
            int row = (int) Math.floor(i / 3);
            int column = i % 3;
            StringBuilder failureMsg = new StringBuilder();
            failureMsg.append("Incorrect value for matrix[").append(row).append(",").append(column)
                      .append("]");
            assertEquals(failureMsg.toString(), values[i], m.getValue(row, column), delta);
        }
    }
}
