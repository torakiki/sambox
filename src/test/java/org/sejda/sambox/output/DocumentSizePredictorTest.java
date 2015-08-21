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
package org.sejda.sambox.output;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.sejda.io.SeekableSources;
import org.sejda.sambox.input.PDFParser;
import org.sejda.sambox.pdmodel.PDDocument;
import org.sejda.sambox.pdmodel.PDPage;

/**
 * @author Andrea Vacondio
 *
 */
public class DocumentSizePredictorTest
{

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception
    {
    }

    @Test
        public void testPredictedSize() throws IOException
        {
            PDDocument document = PDFParser.parse(SeekableSources.seekableSourceFrom(new File(
                    "/home/torakiki/Scrivania/test_merge/test_outline.pdf")));
            DocumentSizePredictor victim = new DocumentSizePredictor();
            for (PDPage page : document.getPages())
            {
                victim.addPage(page);
                System.out.println(victim.predictedSize());
                System.out.println(victim.items());
                System.out.println("-----");
    
            }
        }
}
