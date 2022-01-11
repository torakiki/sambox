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
package org.sejda.sambox.filter;

import junit.framework.TestCase;
import org.sejda.commons.util.IOUtils;
import org.sejda.io.SeekableSources;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.input.PDFParser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

/**
 * This will test all of the filters in the PDFBox system.
 */
public class TestFilters extends TestCase
{
    /**
     * This will test all of the filters in the system. There will be COUNT of deterministic tests and COUNT of
     * non-deterministic tests, see also the discussion in PDFBOX-1977.
     *
     * @throws IOException If there is an exception while encoding.
     */
    public void testFilters() throws IOException
    {
        final int COUNT = 10;
        Random rd = new Random(123456);
        for (int iter = 0; iter < COUNT * 2; iter++)
        {
            long seed;
            if (iter < COUNT)
            {
                // deterministic seed
                seed = rd.nextLong();
            }
            else
            {
                // non-deterministic seed
                seed = new Random().nextLong();
            }
            boolean success = false;
            try
            {
                final Random random = new Random(seed);
                final int numBytes = 10000 + random.nextInt(20000);
                byte[] original = new byte[numBytes];

                int upto = 0;
                while (upto < numBytes)
                {
                    final int left = numBytes - upto;
                    if (random.nextBoolean() || left < 2)
                    {
                        // Fill w/ pseudo-random bytes:
                        final int end = upto + Math.min(left, 10 + random.nextInt(100));
                        while (upto < end)
                        {
                            original[upto++] = (byte) random.nextInt();
                        }
                    }
                    else
                    {
                        // Fill w/ very predictable bytes:
                        final int end = upto + Math.min(left, 2 + random.nextInt(10));
                        final byte value = (byte) random.nextInt(4);
                        while (upto < end)
                        {
                            original[upto++] = value;
                        }
                    }
                }

                for (Filter filter : FilterFactory.INSTANCE.getAllFilters())
                {
                    // Skip filters that don't currently support roundtripping
                    if (filter instanceof DCTFilter || filter instanceof CCITTFaxFilter
                            || filter instanceof JPXFilter || filter instanceof JBIG2Filter
                            || filter instanceof RunLengthDecodeFilter)
                    {
                        continue;
                    }

                    checkEncodeDecode(filter, original);
                }
                success = true;
            }
            finally
            {
                if (!success)
                {
                    System.err.println("NOTE: test failed with seed=" + seed);
                }
            }
        }
    }

    /**
     * This will test the use of identity filter to decode stream and string. This test threw an IOException before the
     * correction.
     * 
     * @throws IOException
     */
    public void testPDFBOX4517() throws IOException
    {
        PDFParser.parse(
                SeekableSources
                        .seekableSourceFrom(new File("target/pdfs", "PDFBOX-4517-cryptfilter.pdf")),
                "userpassword1234");
    }

    /**
     * This will test the LZW filter with the sequence that failed in PDFBOX-1977. To check that the
     * test itself is legit, revert LZWFilter.java to rev 1571801, which should fail this test.
     *
     * @throws IOException
     */
    public void testPDFBOX1977() throws IOException
    {
        Filter lzwFilter = FilterFactory.INSTANCE.getFilter(COSName.LZW_DECODE);
        byte[] byteArray = IOUtils.toByteArray(
                this.getClass().getResourceAsStream("PDFBOX-1977.bin"));
        checkEncodeDecode(lzwFilter, byteArray);
    }

    private void checkEncodeDecode(Filter filter, byte[] original) throws IOException
    {
        ByteArrayOutputStream encoded = new ByteArrayOutputStream();
        filter.encode(new ByteArrayInputStream(original), encoded, new COSDictionary());
        ByteArrayOutputStream decoded = new ByteArrayOutputStream();
        filter.decode(new ByteArrayInputStream(encoded.toByteArray()), decoded, new COSDictionary(),
                0);

        assertTrue(
                "Data that is encoded and then decoded through " + filter.getClass()
                        + " does not match the original data",
                Arrays.equals(original, decoded.toByteArray()));
    }
}
