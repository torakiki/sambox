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
package org.sejda.sambox.pdmodel;

import org.junit.Test;
import org.sejda.sambox.contentstream.operator.Operator;
import org.sejda.sambox.contentstream.operator.OperatorName;
import org.sejda.sambox.cos.COSNumber;
import org.sejda.sambox.input.ContentStreamParser;
import org.sejda.sambox.pdmodel.PDPageContentStream.AppendMode;
import org.sejda.sambox.pdmodel.graphics.state.PDExtendedGraphicsState;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Yegor Kozlov
 */
public class TestPDPageContentStream
{
    @Test
    public void testSetCmykColors() throws IOException
    {
        try (PDDocument doc = new PDDocument())
        {

            PDPage page = new PDPage();
            doc.addPage(page);

            PDPageContentStream contentStream = new PDPageContentStream(doc, page,
                    AppendMode.OVERWRITE, true);
            // pass a non-stroking color in CMYK color space
            contentStream.setNonStrokingColor(0.1f, 0.2f, 0.3f, 0.4f);
            contentStream.close();

            // now read the PDF stream and verify that the CMYK values are correct
            try (ContentStreamParser parser = new ContentStreamParser(page))
            {
                List<Object> pageTokens = parser.tokens();
                // expected five tokens :
                // [0] = COSFloat{0.1}
                // [1] = COSFloat{0.2}
                // [2] = COSFloat{0.3}
                // [3] = COSFloat{0.4}
                // [4] = PDFOperator{"k"}
                assertEquals(0.1f, ((COSNumber) pageTokens.get(0)).floatValue(), 0.00001d);
                assertEquals(0.2f, ((COSNumber) pageTokens.get(1)).floatValue(), 0.00001d);
                assertEquals(0.3f, ((COSNumber) pageTokens.get(2)).floatValue(), 0.00001d);
                assertEquals(0.4f, ((COSNumber) pageTokens.get(3)).floatValue(), 0.00001d);
                assertEquals(OperatorName.NON_STROKING_CMYK,
                        ((Operator) pageTokens.get(4)).getName());
            }
            // same as above but for PDPageContentStream#setStrokingColor
            page = new PDPage();
            doc.addPage(page);

            contentStream = new PDPageContentStream(doc, page, AppendMode.OVERWRITE, false);
            // pass a non-stroking color in CMYK color space
            contentStream.setStrokingColor(0.5f, 0.6f, 0.7f, 0.8f);
            contentStream.close();

            // now read the PDF stream and verify that the CMYK values are correct
            try (ContentStreamParser parser = new ContentStreamParser(page))
            {
                List<Object> pageTokens = parser.tokens();
                // expected five tokens :
                // [0] = COSFloat{0.5}
                // [1] = COSFloat{0.6}
                // [2] = COSFloat{0.7}
                // [3] = COSFloat{0.8}
                // [4] = PDFOperator{"K"}
                assertEquals(0.5f, ((COSNumber) pageTokens.get(0)).floatValue(), 0.00001d);
                assertEquals(0.6f, ((COSNumber) pageTokens.get(1)).floatValue(), 0.00001d);
                assertEquals(0.7f, ((COSNumber) pageTokens.get(2)).floatValue(), 0.00001d);
                assertEquals(0.8f, ((COSNumber) pageTokens.get(3)).floatValue(), 0.00001d);
                assertEquals(OperatorName.STROKING_COLOR_CMYK,
                        ((Operator) pageTokens.get(4)).getName());
            }
        }
    }

    @Test
    public void testSetRGBandGColors() throws IOException
    {
        try (PDDocument doc = new PDDocument())
        {
            PDPage page = new PDPage();
            doc.addPage(page);

            PDPageContentStream contentStream = new PDPageContentStream(doc, page,
                    AppendMode.OVERWRITE, true);

            // pass a non-stroking color in RGB and Gray color space
            contentStream.setNonStrokingColor(0.1f, 0.2f, 0.3f);
            contentStream.setNonStrokingColor(1, 2, 3);
            contentStream.setNonStrokingColor(0.8f);
            contentStream.setNonStrokingColor(8);
            contentStream.close();

            // now read the PDF stream and verify that the values are correct
            try (ContentStreamParser parser = new ContentStreamParser(page))
            {
                List<Object> pageTokens = parser.tokens();
                assertEquals(0.1f, ((COSNumber) pageTokens.get(0)).floatValue(), 0);
                assertEquals(0.2f, ((COSNumber) pageTokens.get(1)).floatValue(), 0);
                assertEquals(0.3f, ((COSNumber) pageTokens.get(2)).floatValue(), 0);
                assertEquals(OperatorName.NON_STROKING_RGB,
                        ((Operator) pageTokens.get(3)).getName());
                assertEquals(1 / 255f, ((COSNumber) pageTokens.get(4)).floatValue(), 0.00001d);
                assertEquals(2 / 255f, ((COSNumber) pageTokens.get(5)).floatValue(), 0.00001d);
                assertEquals(3 / 255f, ((COSNumber) pageTokens.get(6)).floatValue(), 0.00001d);
                assertEquals(OperatorName.NON_STROKING_RGB,
                        ((Operator) pageTokens.get(7)).getName());
                assertEquals(0.8f, ((COSNumber) pageTokens.get(8)).floatValue(), 0);
                assertEquals(OperatorName.NON_STROKING_GRAY,
                        ((Operator) pageTokens.get(9)).getName());
                assertEquals(8 / 255f, ((COSNumber) pageTokens.get(10)).floatValue(), 0.00001d);
                assertEquals(OperatorName.NON_STROKING_GRAY,
                        ((Operator) pageTokens.get(11)).getName());
            }
            // same as above but for PDPageContentStream#setStrokingColor
            page = new PDPage();
            doc.addPage(page);

            contentStream = new PDPageContentStream(doc, page, AppendMode.OVERWRITE, false);

            // pass a non-stroking color in RGB and Gray color space
            contentStream.setStrokingColor(0.5f, 0.6f, 0.7f);
            contentStream.setStrokingColor(5, 6, 7);
            contentStream.setStrokingColor(0.8f);
            contentStream.setStrokingColor(8);
            contentStream.close();

            // now read the PDF stream and verify that the values are correct
            try (ContentStreamParser parser = new ContentStreamParser(page))
            {
                List<Object> pageTokens = parser.tokens();
                assertEquals(0.5f, ((COSNumber) pageTokens.get(0)).floatValue(), 0.00001d);
                assertEquals(0.6f, ((COSNumber) pageTokens.get(1)).floatValue(), 0.00001d);
                assertEquals(0.7f, ((COSNumber) pageTokens.get(2)).floatValue(), 0.00001d);
                assertEquals(OperatorName.STROKING_COLOR_RGB,
                        ((Operator) pageTokens.get(3)).getName());
                assertEquals(5 / 255f, ((COSNumber) pageTokens.get(4)).floatValue(), 0.00001d);
                assertEquals(6 / 255f, ((COSNumber) pageTokens.get(5)).floatValue(), 0.00001d);
                assertEquals(7 / 255f, ((COSNumber) pageTokens.get(6)).floatValue(), 0.00001d);
                assertEquals(OperatorName.STROKING_COLOR_RGB,
                        ((Operator) pageTokens.get(7)).getName());
                assertEquals(0.8f, ((COSNumber) pageTokens.get(8)).floatValue(), 0.00001d);
                assertEquals(OperatorName.STROKING_COLOR_GRAY,
                        ((Operator) pageTokens.get(9)).getName());
                assertEquals(8 / 255f, ((COSNumber) pageTokens.get(10)).floatValue(), 0.00001d);
                assertEquals(OperatorName.STROKING_COLOR_GRAY,
                        ((Operator) pageTokens.get(11)).getName());
            }
        }
    }

    /**
     * PDFBOX-3510: missing content stream should not fail.
     *
     * @throws IOException
     */
    @Test
    public void testMissingContentStream() throws IOException
    {
        PDPage page = new PDPage();
        ContentStreamParser parser = new ContentStreamParser(page);
        List<Object> tokens = parser.tokens();
        assertEquals(0, tokens.size());
    }

    /**
     * Check that close() can be called twice.
     *
     * @throws IOException
     */
    @Test
    public void testCloseContract() throws IOException
    {
        try (PDDocument doc = new PDDocument())
        {
            PDPage page = new PDPage();
            doc.addPage(page);
            PDPageContentStream contentStream = new PDPageContentStream(doc, page,
                    AppendMode.OVERWRITE, true);
            contentStream.close();
            contentStream.close();
        }
    }

    /**
     * Check that general graphics state operators are allowed in text mode.
     *
     * @throws IOException
     */
    @Test
    public void testGeneralGraphicStateOperatorTextMode() throws IOException
    {
        try (PDDocument doc = new PDDocument())
        {
            PDPage page = new PDPage();
            doc.addPage(page);
            PDPageContentStream contentStream = new PDPageContentStream(doc, page);
            contentStream.beginText();
            // J
            contentStream.setLineCapStyle(0);
            // j
            contentStream.setLineJoinStyle(0);
            // w
            contentStream.setLineWidth(10f);
            // d
            contentStream.setLineDashPattern(new float[] { 2, 1 }, 0f);
            // M
            contentStream.setMiterLimit(1.0f);
            // gs
            contentStream.setGraphicsStateParameters(new PDExtendedGraphicsState());
            // ri, i are not supported with a specific setter
            contentStream.endText();
        }
    }
}
