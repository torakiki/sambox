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

import java.io.IOException;

import org.junit.Test;
import org.sejda.commons.util.IOUtils;
import org.sejda.io.SeekableSources;
import org.sejda.sambox.input.PDFParser;
import org.sejda.sambox.pdmodel.PDDocument;

/**
 * Test for the PDButton class.
 *
 */
public class BidiTest
{
    @Test
    public void testBidiSample() throws IOException
    {
        testSortedAndNotSorted("BidiSample.pdf");
    }

    @Test
    public void testPDFBOX4531Ligature1() throws IOException
    {
        testSortedAndNotSorted("PDFBOX-4531-bidi-ligature-1.pdf");
    }

    @Test
    public void testPDFBOX4531Ligature2() throws IOException
    {
        testSortedAndNotSorted("PDFBOX-4531-bidi-ligature-2.pdf");
    }
    
    private void testSortedAndNotSorted(String input) throws IOException {
        testSorted(input);
        testNotSorted(input);
    }

    private void testSorted(String input) throws IOException
    {
        try (PDDocument document = parseInput(input))
        {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setLineSeparator("\n");
            
            stripper.setSortByPosition(true);
            
            String actualText = stripper.getText(document);
            String expectedText = readExpectedFile(input + "-sorted.txt");

            assertEquals(expectedText, actualText);
        }
    }

    private void testNotSorted(String input) throws IOException
    {
        try (PDDocument document = parseInput(input)) 
        {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setLineSeparator("\n");

            stripper.setSortByPosition(false);
            
            String actualText = stripper.getText(document);
            String expectedText = readExpectedFile(input + ".txt");

            assertEquals(expectedText, actualText);
        }
    }
    
    private String readExpectedFile(String name) throws IOException {
        return new String(IOUtils.toByteArray(getClass().getResourceAsStream("/org/sejda/sambox/text/" + name)));
    }
    
    private PDDocument parseInput(String name) throws IOException {
        return PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(
            getClass().getResourceAsStream("/org/sejda/sambox/text/" + name)));
    }
}
