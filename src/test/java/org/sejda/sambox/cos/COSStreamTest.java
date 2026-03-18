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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sejda.commons.util.IOUtils;
import org.sejda.sambox.filter.Filter;
import org.sejda.sambox.filter.FilterFactory;

public class COSStreamTest
{

    /**
     * Tests encoding of a stream without any filter applied.
     */
    @Test
    public void testUncompressedStreamEncode() throws IOException
    {
        byte[] testString = "This is a test string to be used as input for TestCOSStream".getBytes(
                StandardCharsets.US_ASCII);
        COSStream stream = createStream(testString, null);
        validateEncoded(stream, testString);
    }

    /**
     * Tests decoding of a stream without any filter applied.
     */
    @Test
    public void testUncompressedStreamDecode() throws IOException
    {
        byte[] testString = "This is a test string to be used as input for TestCOSStream".getBytes(
                StandardCharsets.US_ASCII);
        COSStream stream = createStream(testString, null);
        validateDecoded(stream, testString);
    }

    /**
     * Tests encoding of a stream with one filter applied.
     */
    @Test
    public void testCompressedStream1Encode() throws IOException
    {
        byte[] testString = "This is a test string to be used as input for TestCOSStream".getBytes(
                StandardCharsets.US_ASCII);
        byte[] testStringEncoded = encodeData(testString, COSName.FLATE_DECODE);
        COSStream stream = createStream(testString, COSName.FLATE_DECODE);
        validateEncoded(stream, testStringEncoded);
    }

    @Test
    public void testCompressedStreamAddCompression() throws IOException
    {
        byte[] testString = "This is a test string to be used as input for TestCOSStream".getBytes(
                StandardCharsets.US_ASCII);
        byte[] testStringEncoded = encodeData(testString, COSName.FLATE_DECODE);
        COSStream stream = createStream(testString, COSName.FLATE_DECODE);
        stream.addCompression();
        validateEncoded(stream, testStringEncoded);
    }

    /**
     * Tests decoding of a stream with one filter applied.
     */
    @Test
    public void testCompressedStream1Decode() throws IOException
    {
        byte[] testString = "This is a test string to be used as input for TestCOSStream".getBytes(
                StandardCharsets.US_ASCII);
        byte[] testStringEncoded = encodeData(testString, COSName.FLATE_DECODE);
        COSStream stream = new COSStream();
        try (OutputStream output = stream.createFilteredStream())
        {
            output.write(testStringEncoded);
        }

        stream.setItem(COSName.FILTER, COSName.FLATE_DECODE);
        validateDecoded(stream, testString);
    }

    /**
     * Tests encoding of a stream with 2 filters applied.
     */
    @Test
    public void testCompressedStream2Encode() throws IOException
    {
        byte[] testString = "This is a test string to be used as input for TestCOSStream".getBytes(
                StandardCharsets.US_ASCII);
        byte[] testStringEncoded = encodeData(testString, COSName.FLATE_DECODE);
        testStringEncoded = encodeData(testStringEncoded, COSName.ASCII85_DECODE);
        COSArray filters = new COSArray();
        filters.add(COSName.ASCII85_DECODE);
        filters.add(COSName.FLATE_DECODE);
        COSStream stream = createStream(testString, filters);
        validateEncoded(stream, testStringEncoded);
    }

    /**
     * Tests encoding of a stream with 2 filters applied.
     */
    @Test
    public void testCompressedStream2AddCompression() throws IOException
    {
        byte[] testString = "This is a test string to be used as input for TestCOSStream".getBytes(
                StandardCharsets.US_ASCII);
        byte[] testStringEncoded = encodeData(testString, COSName.ASCII85_DECODE);
        testStringEncoded = encodeData(testStringEncoded, COSName.FLATE_DECODE);
        COSStream stream = createStream(testString, COSName.ASCII85_DECODE);
        stream.addCompression();
        validateEncoded(stream, testStringEncoded);
    }

    /**
     * test addCompression doesn't throw anything when ASCIIHex stream has garbage
     */
    @Test
    public void addCompressionWithGarbageAsciiHex() throws IOException
    {
        byte[] testString = "This is a test string to be used as input for TestCOSStream".getBytes(
                StandardCharsets.US_ASCII);
        byte[] testStringEncoded = encodeData(testString, COSName.FLATE_DECODE);
        COSStream stream = createStream(testStringEncoded);
        stream.setItem(COSName.FILTER, COSName.ASCII_HEX_DECODE);
        stream.addCompression();
    }

    /**
     * Tests decoding of a stream with 2 filters applied.
     */
    @Test
    public void testCompressedStream2Decode() throws IOException
    {
        byte[] testString = "This is a test string to be used as input for TestCOSStream".getBytes(
                StandardCharsets.US_ASCII);
        byte[] testStringEncoded = encodeData(testString, COSName.FLATE_DECODE);
        testStringEncoded = encodeData(testStringEncoded, COSName.ASCII85_DECODE);
        COSStream stream = new COSStream();
        COSArray filters = new COSArray();
        filters.add(COSName.ASCII85_DECODE);
        filters.add(COSName.FLATE_DECODE);
        stream.setItem(COSName.FILTER, filters);

        try (OutputStream output = stream.createFilteredStream())
        {
            output.write(testStringEncoded);
        }

        validateDecoded(stream, testString);
    }

    @Test
    public void hasFiltersName() throws IOException
    {
        byte[] testString = "This is a test string to be used as input for TestCOSStream".getBytes(
                StandardCharsets.US_ASCII);
        try (COSStream stream = createStream(testString, COSName.ASCII85_DECODE))
        {
            assertTrue(stream.hasFilter(COSName.ASCII85_DECODE));
            assertFalse(stream.hasFilter(COSName.FLATE_DECODE));
        }
    }

    @Test
    public void hasFiltersArray() throws IOException
    {
        byte[] testString = "This is a test string to be used as input for TestCOSStream".getBytes(
                StandardCharsets.US_ASCII);
        try (COSStream stream = createStream(testString,
                new COSArray(COSName.FLATE_DECODE, COSName.ASCII85_DECODE)))
        {
            assertTrue(stream.hasFilter(COSName.ASCII85_DECODE));
            assertTrue(stream.hasFilter(COSName.FLATE_DECODE));
            assertFalse(stream.hasFilter(COSName.DCT_DECODE));
        }
    }

    /**
     * Tests that encoding is done correctly even if the the stream is closed twice.
     * Closeable.close() allows streams to be closed multiple times. The second and subsequent
     * close() calls should have no effect.
     */
    @Test
    public void testCompressedStreamDoubleClose() throws IOException
    {
        byte[] testString = "This is a test string to be used as input for TestCOSStream".getBytes(
                StandardCharsets.US_ASCII);
        byte[] testStringEncoded = encodeData(testString, COSName.FLATE_DECODE);
        COSStream stream = new COSStream();
        OutputStream output = stream.createFilteredStream();
        output.write(testStringEncoded);
        stream.setItem(COSName.FILTER, COSName.FLATE_DECODE);
        output.close();
        output.close();
        validateEncoded(stream, testStringEncoded);
    }

    @Test
    @DisplayName("Test removeCompression when filters is a COSName")
    public void testRemoveCompression() throws IOException
    {
        byte[] testString = "This is a test string to be used as input for TestCOSStream".getBytes(
                StandardCharsets.US_ASCII);
        COSStream stream = createStream(testString, COSName.FLATE_DECODE);
        assertTrue(stream.hasFilter(COSName.FLATE_DECODE));
        stream.removeCompression();
        assertFalse(stream.hasFilter(COSName.FLATE_DECODE));
        validateDecoded(stream, testString);
    }

    @Test
    @DisplayName("Test removeCompression when filters is a COSArray")
    public void testRemoveCompressionTwoFilter() throws IOException
    {
        byte[] testString = "This is a test string to be used as input for TestCOSStream".getBytes(
                StandardCharsets.US_ASCII);
        byte[] testStringEncoded = encodeData(testString, COSName.ASCII85_DECODE);
        COSStream stream = createStream(testString,
                new COSArray(COSName.FLATE_DECODE, COSName.ASCII85_DECODE));
        assertTrue(stream.hasFilter(COSName.FLATE_DECODE));
        stream.removeCompression();
        assertFalse(stream.hasFilter(COSName.FLATE_DECODE));
        validateEncoded(stream, testStringEncoded);
    }

    @Test
    @DisplayName("removeCompression removes LZWDecode when filter is a single COSName")
    public void testRemoveCompressionLZWSingleFilter() throws IOException
    {
        byte[] testString = "LZW test data for COSStream".getBytes(StandardCharsets.US_ASCII);
        COSStream stream = createStream(testString, COSName.LZW_DECODE);
        assertTrue(stream.hasFilter(COSName.LZW_DECODE));
        var result = stream.removeCompression();
        assertTrue(result);
        assertFalse(stream.hasFilter(COSName.LZW_DECODE));
    }

    @Test
    @DisplayName("removeCompression removes LZWDecode from a COSArray, leaving other filters")
    public void testRemoveCompressionLZWInArray() throws IOException
    {
        byte[] testString = "LZW array test data".getBytes(StandardCharsets.US_ASCII);
        COSStream stream = createStream(testString,
                new COSArray(COSName.LZW_DECODE, COSName.ASCII85_DECODE));
        assertTrue(stream.hasFilter(COSName.LZW_DECODE));
        assertTrue(stream.hasFilter(COSName.ASCII85_DECODE));
        var result = stream.removeCompression();
        assertTrue(result);
        assertFalse(stream.hasFilter(COSName.LZW_DECODE));
        assertTrue(stream.hasFilter(COSName.ASCII85_DECODE));
    }

    @Test
    @DisplayName("removeCompression returns false when no compression filter is present")
    public void testRemoveCompressionNoCompressionFilter() throws IOException
    {
        byte[] testString = "No compression test".getBytes(StandardCharsets.US_ASCII);
        COSStream stream = createStream(testString, COSName.ASCII85_DECODE);
        assertTrue(stream.hasFilter(COSName.ASCII85_DECODE));
        var result = stream.removeCompression();
        assertFalse(result);
        assertTrue(stream.hasFilter(COSName.ASCII85_DECODE));
    }

    @Test
    @DisplayName("removeCompression removes DecodeParms when single filter with DecodeParms")
    public void testRemoveCompressionRemovesDecodeParms() throws IOException
    {
        byte[] testString = "DecodeParms test".getBytes(StandardCharsets.US_ASCII);
        COSStream stream = createStream(testString, COSName.FLATE_DECODE);
        var parms = new COSDictionary();
        parms.setItem(COSName.PREDICTOR, COSInteger.get(12));
        stream.setItem(COSName.DECODE_PARMS, parms);
        var result = stream.removeCompression();
        assertTrue(result);
        assertFalse(stream.hasFilter(COSName.FLATE_DECODE));
        assertNull(stream.getDictionaryObject(COSName.DECODE_PARMS, COSName.DP));
    }

    @Test
    @DisplayName("removeCompression removes correct DecodeParms index from array")
    public void testRemoveCompressionRemovesDecodeParmsArrayIndex() throws IOException
    {
        byte[] testString = "DecodeParms array test".getBytes(StandardCharsets.US_ASCII);
        COSStream stream = createStream(testString,
                new COSArray(COSName.ASCII85_DECODE, COSName.FLATE_DECODE));
        // DecodeParms: [null, {Predictor: 12}]
        var flateParms = new COSDictionary();
        flateParms.setItem(COSName.PREDICTOR, COSInteger.get(12));
        stream.setItem(COSName.DECODE_PARMS, new COSArray(COSNull.NULL, flateParms));
        var result = stream.removeCompression();
        assertTrue(result);
        assertFalse(stream.hasFilter(COSName.FLATE_DECODE));
        assertTrue(stream.hasFilter(COSName.ASCII85_DECODE));
        assertNull(stream.getDictionaryObject(COSName.DECODE_PARMS, COSName.DP));
    }

    @Test
    @DisplayName("removeCompression removes correct DecodeParms from three-filter array")
    public void testRemoveCompressionThreeFiltersDecodeParmsArray() throws IOException
    {
        byte[] testString = "Three filters test".getBytes(StandardCharsets.US_ASCII);
        COSStream stream = createStream(testString,
                new COSArray(COSName.ASCII85_DECODE, COSName.FLATE_DECODE,
                        COSName.ASCII_HEX_DECODE));
        // DecodeParms: [null, {Predictor: 12}, null]
        var flateParms = new COSDictionary();
        flateParms.setItem(COSName.PREDICTOR, COSInteger.get(12));
        stream.setItem(COSName.DECODE_PARMS, new COSArray(COSNull.NULL, flateParms, COSNull.NULL));
        var result = stream.removeCompression();
        assertTrue(result);
        assertFalse(stream.hasFilter(COSName.FLATE_DECODE));
        assertTrue(stream.hasFilter(COSName.ASCII85_DECODE));
        assertTrue(stream.hasFilter(COSName.ASCII_HEX_DECODE));
        // DecodeParms removed since they are all null
        assertNull(stream.getDictionaryObject(COSName.DECODE_PARMS, COSName.DP));
    }

    @Test
    @DisplayName("removeCompression removes DP key instead of DecodeParms when DP is used")
    public void testRemoveCompressionRemovesDPKey() throws IOException
    {
        byte[] testString = "DP key test".getBytes(StandardCharsets.US_ASCII);
        COSStream stream = createStream(testString, COSName.FLATE_DECODE);
        var parms = new COSDictionary();
        parms.setItem(COSName.PREDICTOR, COSInteger.get(12));
        stream.setItem(COSName.DP, parms);
        var result = stream.removeCompression();
        assertTrue(result);
        assertFalse(stream.hasFilter(COSName.FLATE_DECODE));
        assertNull(stream.getDictionaryObject(COSName.DP));
    }

    @Test
    @DisplayName("removeCompression returns false when no filter is present")
    public void testRemoveCompressionNoFilter() throws IOException
    {
        byte[] testString = "No filter test".getBytes(StandardCharsets.US_ASCII);
        COSStream stream = createStream(testString);
        var result = stream.removeCompression();
        assertFalse(result);
    }

    @Test
    @DisplayName("removeCompression removes both FlateDecode and LZWDecode from array")
    public void testRemoveCompressionBothCompressionFilters() throws IOException
    {
        byte[] testString = "Both compression filters test".getBytes(StandardCharsets.US_ASCII);
        COSStream stream = createStream(testString,
                new COSArray(COSName.FLATE_DECODE, COSName.LZW_DECODE));
        assertTrue(stream.hasFilter(COSName.FLATE_DECODE));
        assertTrue(stream.hasFilter(COSName.LZW_DECODE));
        var result = stream.removeCompression();
        assertTrue(result);
        assertFalse(stream.hasFilter(COSName.FLATE_DECODE));
        assertFalse(stream.hasFilter(COSName.LZW_DECODE));
        assertNull(stream.getItem(COSName.FILTER));
    }

    @Test
    @DisplayName("removeCompression removes both compression filters mixed with other filters")
    public void testRemoveCompressionBothCompressionFiltersMixedWithOthers() throws IOException
    {
        byte[] testString = "Mixed filters test".getBytes(StandardCharsets.US_ASCII);
        COSStream stream = createStream(testString,
                new COSArray(COSName.FLATE_DECODE, COSName.ASCII85_DECODE, COSName.LZW_DECODE));
        // DecodeParms: [null, null, {Predictor: 12}]
        var lzwParms = new COSDictionary();
        lzwParms.setItem(COSName.PREDICTOR, COSInteger.get(12));
        stream.setItem(COSName.DECODE_PARMS, new COSArray(COSNull.NULL, COSNull.NULL, lzwParms));
        var result = stream.removeCompression();
        assertTrue(result);
        assertFalse(stream.hasFilter(COSName.FLATE_DECODE));
        assertFalse(stream.hasFilter(COSName.LZW_DECODE));
        assertTrue(stream.hasFilter(COSName.ASCII85_DECODE));
        // DecodeParms remove since only COSNull remained
        assertNull(stream.getDictionaryObject(COSName.DECODE_PARMS));
    }

    @Test
    @DisplayName("removeCompression removes both compression filters with DecodeParms")
    public void testRemoveCompressionBothCompressionFiltersWithDecodeParms() throws IOException
    {
        byte[] testString = "Both filters with parms test".getBytes(StandardCharsets.US_ASCII);
        COSStream stream = createStream(testString,
                new COSArray(COSName.FLATE_DECODE, COSName.LZW_DECODE));
        // DecodeParms: [{Predictor: 12}, {EarlyChange: 0}]
        var flateParms = new COSDictionary();
        flateParms.setItem(COSName.PREDICTOR, COSInteger.get(12));
        var lzwParms = new COSDictionary();
        lzwParms.setItem(COSName.EARLY_CHANGE, COSInteger.get(0));
        stream.setItem(COSName.DECODE_PARMS, new COSArray(flateParms, lzwParms));
        var result = stream.removeCompression();
        assertTrue(result);
        assertFalse(stream.hasFilter(COSName.FLATE_DECODE));
        assertFalse(stream.hasFilter(COSName.LZW_DECODE));
        assertNull(stream.getItem(COSName.FILTER));
        assertNull(stream.getDictionaryObject(COSName.DECODE_PARMS));
    }

    @Test
    @DisplayName("removeCompression with two filters both having DecodeParms keeps remaining parms")
    public void testRemoveCompressionKeepsRemainingDecodeParms() throws IOException
    {
        byte[] testString = "Two filters with parms".getBytes(StandardCharsets.US_ASCII);
        COSStream stream = createStream(testString,
                new COSArray(COSName.FLATE_DECODE, COSName.ASCII85_DECODE));
        // DecodeParms: [{Predictor: 12}, {SomeParam: 1}]
        var flateParms = new COSDictionary();
        flateParms.setItem(COSName.PREDICTOR, COSInteger.get(12));
        var asciiParms = new COSDictionary();
        asciiParms.setItem(COSName.getPDFName("SomeParam"), COSInteger.get(1));
        stream.setItem(COSName.DECODE_PARMS, new COSArray(flateParms, asciiParms));
        var result = stream.removeCompression();
        assertTrue(result);
        assertFalse(stream.hasFilter(COSName.FLATE_DECODE));
        assertTrue(stream.hasFilter(COSName.ASCII85_DECODE));
        var remainingParms = stream.getDictionaryObject(COSName.DECODE_PARMS, COSArray.class);
        assertNotNull(remainingParms);
        assertEquals(1, remainingParms.size());
    }

    private static byte[] encodeData(byte[] original, COSName filter) throws IOException
    {
        Filter encodingFilter = FilterFactory.INSTANCE.getFilter(filter);
        ByteArrayOutputStream encoded = new ByteArrayOutputStream();
        encodingFilter.encode(new ByteArrayInputStream(original), encoded, new COSDictionary());
        return encoded.toByteArray();
    }

    private static COSStream createStream(byte[] testString, COSBase filters) throws IOException
    {
        COSStream stream = new COSStream();
        try (OutputStream output = stream.createFilteredStream(filters))
        {
            output.write(testString);
        }
        return stream;
    }

    private static COSStream createStream(byte[] testString) throws IOException
    {
        COSStream stream = new COSStream();
        try (OutputStream output = stream.createFilteredStream())
        {
            output.write(testString);
        }
        return stream;
    }

    private static void validateEncoded(COSStream stream, byte[] expected) throws IOException
    {
        byte[] encoded = IOUtils.toByteArray(stream.getFilteredStream());
        stream.close();
        assertArrayEquals(expected, encoded, "Encoded data doesn't match input");
    }

    private static void validateDecoded(COSStream stream, byte[] expected) throws IOException
    {
        byte[] decoded = IOUtils.toByteArray(stream.getUnfilteredStream());
        stream.close();
        assertArrayEquals(expected, decoded, "Decoded data doesn't match input");
    }
}
