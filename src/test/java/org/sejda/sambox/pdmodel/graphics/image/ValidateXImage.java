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

import org.sejda.io.SeekableSources;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSStream;
import org.sejda.sambox.input.PDFParser;
import org.sejda.sambox.pdmodel.PDDocument;
import org.sejda.sambox.pdmodel.PDPage;
import org.sejda.sambox.pdmodel.PDPageContentStream;
import org.sejda.sambox.pdmodel.PDPageContentStream.AppendMode;
import org.sejda.sambox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.spi.ImageWriterSpi;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

/**
 * Helper class to do some validations for PDImageXObject.
 *
 * @author Tilman Hausherr
 */
public class ValidateXImage
{
    public static void validate(PDImageXObject ximage, int bpc, int width, int height,
            String format, String colorSpaceName) throws IOException
    {
        // check the dictionary
        assertNotNull(ximage);
        COSStream cosStream = ximage.getCOSObject();
        assertNotNull(cosStream);
        assertEquals(COSName.XOBJECT, cosStream.getItem(COSName.TYPE));
        assertEquals(COSName.IMAGE, cosStream.getItem(COSName.SUBTYPE));
        assertTrue(ximage.getCOSObject().getFilteredLength() > 0);
        assertEquals(bpc, ximage.getBitsPerComponent());
        assertEquals(width, ximage.getWidth());
        assertEquals(height, ximage.getHeight());
        assertEquals(format, getSuffix(ximage));
        assertEquals(colorSpaceName, ximage.getColorSpace().getName());

        // check the image
        assertNotNull(ximage.getImage());
        assertEquals(ximage.getWidth(), ximage.getImage().getWidth());
        assertEquals(ximage.getHeight(), ximage.getImage().getHeight());
        WritableRaster rawRaster = ximage.getRawRaster();
        assertNotNull(rawRaster);
        assertEquals(rawRaster.getWidth(), ximage.getWidth());
        assertEquals(rawRaster.getHeight(), ximage.getHeight());
        if (colorSpaceName.equals("ICCBased"))
        {
            BufferedImage rawImage = ximage.getRawImage();
            assertNotNull(rawImage);
            assertEquals(rawImage.getWidth(), ximage.getWidth());
            assertEquals(rawImage.getHeight(), ximage.getHeight());
        }

        boolean canEncode = true;
        boolean writeOk;
        // jdk11+ no longer encodes ARGB jpg
        // https://bugs.openjdk.java.net/browse/JDK-8211748
        if ("jpg".equals(format) && ximage.getImage().getType() == BufferedImage.TYPE_INT_ARGB)
        {
            ImageWriter writer = ImageIO.getImageWritersBySuffix(format).next();
            ImageWriterSpi originatingProvider = writer.getOriginatingProvider();
            canEncode = originatingProvider.canEncodeImage(ximage.getImage());
        }
        if (canEncode)
        {
            writeOk = ImageIO.write(ximage.getImage(), format, new ByteArrayOutputStream());
            assertTrue(writeOk);
        }
        writeOk = ImageIO.write(ximage.getOpaqueImage(), format, new ByteArrayOutputStream());
        assertTrue(writeOk);
    }

    static int colorCount(BufferedImage bim)
    {
        Set<Integer> colors = new HashSet<>();
        int w = bim.getWidth();
        int h = bim.getHeight();
        for (int y = 0; y < h; y++)
        {
            for (int x = 0; x < w; x++)
            {
                colors.add(bim.getRGB(x, y));
            }
        }
        return colors.size();
    }

    // write image twice (overlapped) in document, close document and re-read PDF
    static void doWritePDF(PDDocument document, PDImageXObject ximage, File testResultsDir,
            String filename) throws IOException
    {
        File pdfFile = new File(testResultsDir, filename);

        // This part isn't really needed because this test doesn't break
        // if the mask has the wrong colorspace (PDFBOX-2057), but it is still useful
        // if something goes wrong in the future and we want to have a PDF to open.

        PDPage page = new PDPage();
        document.addPage(page);
        PDPageContentStream contentStream = new PDPageContentStream(document, page,
                AppendMode.APPEND, false);
        contentStream.drawImage(ximage, 150, 300);
        contentStream.drawImage(ximage, 200, 350);
        contentStream.close();

        // check that the resource map is up-to-date
        assertEquals(1, count(document.getPage(0).getResources().getXObjectNames()));

        document.writeTo(pdfFile);
        document.close();

        try (PDDocument doc = PDFParser.parse(SeekableSources.seekableSourceFrom(pdfFile)))
        {
            assertEquals(1, count(doc.getPage(0).getResources().getXObjectNames()));
            new PDFRenderer(doc).renderImage(0);
        }

    }

    private static int count(Iterable<COSName> iterable)
    {
        int count = 0;
        for (COSName name : iterable)
        {
            count++;
        }
        return count;
    }

    /**
     * Check whether the images are identical.
     *
     * @param expectedImage
     * @param actualImage
     */
    public static void checkIdent(BufferedImage expectedImage, BufferedImage actualImage)
    {
        String errMsg = "";

        int w = expectedImage.getWidth();
        int h = expectedImage.getHeight();
        assertEquals(w, actualImage.getWidth());
        assertEquals(h, actualImage.getHeight());
        for (int y = 0; y < h; ++y)
        {
            for (int x = 0; x < w; ++x)
            {
                if (expectedImage.getRGB(x, y) != actualImage.getRGB(x, y))
                {
                    errMsg = String.format("(%d,%d) expected <%08X> but was: <%08X>", x, y,
                            expectedImage.getRGB(x, y), actualImage.getRGB(x, y));
                }
                assertEquals(errMsg, expectedImage.getRGB(x, y), actualImage.getRGB(x, y));
            }
        }
    }

    /**
     * This will get the suffix for this image type, e.g. jpg/png.
     *
     * @return The image suffix or null if not available.
     */
    public static String getSuffix(PDImageXObject ximage)
    {
        List<COSName> filters = ximage.getStream().getFilters();

        if (filters == null)
        {
            return "png";
        }
        if (filters.contains(COSName.DCT_DECODE))
        {
            return "jpg";
        }
        if (filters.contains(COSName.JPX_DECODE))
        {
            return "jpx";
        }
        if (filters.contains(COSName.CCITTFAX_DECODE))
        {
            return "tiff";
        }
        if (filters.contains(COSName.FLATE_DECODE) || filters.contains(COSName.LZW_DECODE)
                || filters.contains(COSName.RUN_LENGTH_DECODE))
        {
            return "png";
        }
        return null;
    }

}
