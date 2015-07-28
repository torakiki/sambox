/*
 * Copyright 2014 The Apache Software Foundation.
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
package org.sejda.sambox.pdmodel.graphics.image;

import static org.sejda.sambox.pdmodel.graphics.image.ValidateXImage.checkIdent;
import static org.sejda.sambox.pdmodel.graphics.image.ValidateXImage.validate;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import junit.framework.TestCase;

import org.sejda.io.SeekableSources;
import org.sejda.sambox.input.PDFParser;
import org.sejda.sambox.pdmodel.PDDocument;
import org.sejda.sambox.pdmodel.PDPage;
import org.sejda.sambox.pdmodel.PDPageContentStream;
import org.sejda.sambox.pdmodel.common.PDRectangle;
import org.sejda.sambox.pdmodel.graphics.color.PDDeviceGray;
/**
 * Unit tests for CCITTFactory
 *
 * @author Tilman Hausherr
 */
public class CCITTFactoryTest extends TestCase
{
    private final File testResultsDir = new File("target/test-output/graphics");

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        testResultsDir.mkdirs();
    }

    /**
     * Tests CCITTFactory#createFromRandomAccess(PDDocument document, RandomAccess reader) with a single page TIFF
     */
    public void testCreateFromRandomAccessSingle() throws IOException
    {
        String tiffG3Path = "src/test/resources/org/sejda/sambox/pdmodel/graphics/image/ccittg3.tif";
        String tiffG4Path = "src/test/resources/org/sejda/sambox/pdmodel/graphics/image/ccittg4.tif";

        try (PDDocument document = new PDDocument())
        {
            PDImageXObject ximage3 = CCITTFactory.createFromFile(document, new File(tiffG3Path));
            validate(ximage3, 1, 344, 287, "tiff", PDDeviceGray.INSTANCE.getName());
            BufferedImage bim3 = ImageIO.read(new File(tiffG3Path));
            checkIdent(bim3, ximage3.getOpaqueImage());
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            PDPageContentStream contentStream = new PDPageContentStream(document, page, true, false);
            contentStream.drawImage(ximage3, 0, 0, ximage3.getWidth(), ximage3.getHeight());
            contentStream.close();

            PDImageXObject ximage4 = CCITTFactory.createFromFile(document, new File(tiffG4Path));
            validate(ximage4, 1, 344, 287, "tiff", PDDeviceGray.INSTANCE.getName());
            BufferedImage bim4 = ImageIO.read(new File(tiffG3Path));
            checkIdent(bim4, ximage4.getOpaqueImage());
            page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            contentStream = new PDPageContentStream(document, page, true, false);
            contentStream.drawImage(ximage4, 0, 0);
            contentStream.close();

            document.writeTo(testResultsDir + "/singletiff.pdf");
        }
        try (PDDocument doc = PDFParser.parse(SeekableSources.seekableSourceFrom(new File(
                testResultsDir, "singletiff.pdf"))))
        {
            assertEquals(2, doc.getNumberOfPages());
        }
    }

    /**
     * Tests CCITTFactory#createFromRandomAccess(PDDocument document, RandomAccess reader) with a multi page TIFF
     */
    public void testCreateFromRandomAccessMulti() throws IOException
    {
        String tiffPath = "src/test/resources/org/sejda/sambox/pdmodel/graphics/image/ccittg4multi.tif";

        ImageInputStream is = ImageIO.createImageInputStream(new File(tiffPath));
        ImageReader imageReader = ImageIO.getImageReaders(is).next();
        imageReader.setInput(is);
        int countTiffImages = imageReader.getNumImages(true);
        assertTrue(countTiffImages > 1);

        try (PDDocument document = new PDDocument())
        {

            int pdfPageNum = 0;
            while (true)
            {
                PDImageXObject ximage = CCITTFactory.createFromFile(document, new File(tiffPath),
                        pdfPageNum);
                if (ximage == null)
                {
                    break;
                }
                BufferedImage bim = imageReader.read(pdfPageNum);
                validate(ximage, 1, bim.getWidth(), bim.getHeight(), "tiff",
                        PDDeviceGray.INSTANCE.getName());
                checkIdent(bim, ximage.getOpaqueImage());
                PDPage page = new PDPage(PDRectangle.A4);
                float fX = ximage.getWidth() / page.getMediaBox().getWidth();
                float fY = ximage.getHeight() / page.getMediaBox().getHeight();
                float factor = Math.max(fX, fY);
                document.addPage(page);
                PDPageContentStream contentStream = new PDPageContentStream(document, page, true,
                        false);
                contentStream.drawImage(ximage, 0, 0, ximage.getWidth() / factor,
                        ximage.getHeight() / factor);
                contentStream.close();
                ++pdfPageNum;
            }

            assertEquals(countTiffImages, pdfPageNum);

            document.writeTo(testResultsDir + "/multitiff.pdf");
        }
        try (PDDocument doc = PDFParser.parse(SeekableSources.seekableSourceFrom(new File(
                testResultsDir, "multitiff.pdf"))))
        {
            assertEquals(countTiffImages, doc.getNumberOfPages());
        }
        imageReader.dispose();
    }
}
