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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.Test;
import org.sejda.commons.util.IOUtils;
import org.sejda.sambox.filter.Filter;
import org.sejda.sambox.filter.FilterFactory;

public class COSStreamTest
{

    /**
     * Tests encoding of a stream without any filter applied.
     *
     * @throws IOException
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
     *
     * @throws IOException
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
     *
     * @throws IOException
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
     *
     * @throws IOException
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
     *
     * @throws IOException
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
     *
     * @throws IOException
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
     *
     * @throws IOException
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
     *
     * @throws IOException
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
     * Tests tests that encoding is done correctly even if the the stream is closed twice. Closeable.close() allows
     * streams to be closed multiple times. The second and subsequent close() calls should have no effect.
     *
     * @throws IOException
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
        assertTrue("Encoded data doesn't match input", Arrays.equals(expected, encoded));
    }

    private static void validateDecoded(COSStream stream, byte[] expected) throws IOException
    {
        byte[] decoded = IOUtils.toByteArray(stream.getUnfilteredStream());
        stream.close();
        assertTrue("Decoded data doesn't match input", Arrays.equals(expected, decoded));
    }
}
