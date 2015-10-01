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

package org.sejda.sambox.text;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sejda.io.SeekableSources;
import org.sejda.sambox.input.PDFParser;
import org.sejda.sambox.pdmodel.PDDocument;

/**
 * Test for the PDButton class.
 *
 */
public class BidiTest
{
    private static final String ENCODING = "UTF-8";

    private PDDocument document;
    private PDFTextStripper stripper;

    @Before
    public void setUp() throws IOException
    {
        document = PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(
                getClass().getResourceAsStream("/org/sejda/sambox/text/BidiSample.pdf")));
        stripper = new PDFTextStripper();
        stripper.setLineSeparator("\n");
    }

    @Test
    public void testSorted() throws IOException
    {
        stripper.setSortByPosition(true);
        String extractedText = stripper.getText(document);

        try (BufferedReader bufferedCompareTextReader = new BufferedReader(new InputStreamReader(
                getClass().getResourceAsStream("/org/sejda/sambox/text/BidiSample.pdf-sorted.txt"),
                ENCODING)))
        {

            StringBuilder compareTextBuilder = new StringBuilder();

            String line = bufferedCompareTextReader.readLine();

            while (line != null)
            {
                compareTextBuilder.append(line);
                compareTextBuilder.append('\n');
                line = bufferedCompareTextReader.readLine();
            }

            bufferedCompareTextReader.close();

            assertEquals(extractedText, compareTextBuilder.toString());
        }
    }

    @Test
    public void testNotSorted() throws IOException
    {
        stripper.setSortByPosition(false);
        String extractedText = stripper.getText(document);

        try (BufferedReader bufferedCompareTextReader = new BufferedReader(new InputStreamReader(
                getClass().getResourceAsStream("/org/sejda/sambox/text/BidiSample.pdf.txt"),
                ENCODING)))
        {

        StringBuilder compareTextBuilder = new StringBuilder();
        String line = bufferedCompareTextReader.readLine();

        while (line != null)
        {
            compareTextBuilder.append(line);
            compareTextBuilder.append('\n');
            line = bufferedCompareTextReader.readLine();
        }

        bufferedCompareTextReader.close();

        assertEquals(extractedText, compareTextBuilder.toString());
        }
    }

    @After
    public void tearDown() throws IOException
    {
        document.close();
    }

}
