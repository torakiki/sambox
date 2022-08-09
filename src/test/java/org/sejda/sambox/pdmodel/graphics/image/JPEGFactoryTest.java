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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.sejda.sambox.pdmodel.graphics.image.ValidateXImage.colorCount;
import static org.sejda.sambox.pdmodel.graphics.image.ValidateXImage.doWritePDF;
import static org.sejda.sambox.pdmodel.graphics.image.ValidateXImage.validate;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sejda.commons.util.IOUtils;
import org.sejda.io.SeekableSources;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.input.PDFParser;
import org.sejda.sambox.pdmodel.PDDocument;
import org.sejda.sambox.pdmodel.graphics.color.PDDeviceGray;
import org.sejda.sambox.pdmodel.graphics.color.PDDeviceRGB;
import javax.imageio.ImageIO;

/**
 * Unit tests for JPEGFactory
 *
 * @author Tilman Hausherr
 */
public class JPEGFactoryTest
{
    private final File testResultsDir = new File("target/test-output/graphics");

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void setUp()
    {
        testResultsDir.mkdirs();
    }

    @Test
    public void testCreateFromFile() throws IOException
    {
        try (InputStream stream = JPEGFactoryTest.class.getResourceAsStream("jpeg.jpg"))
        {
            File file = folder.newFile();
            Files.copy(stream, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            PDImageXObject ximage = JPEGFactory.createFromFile(file);
            validate(ximage, 8, 344, 287, "jpg", PDDeviceRGB.INSTANCE.getName());

            doWritePDF(new PDDocument(), ximage, testResultsDir, "jpegrgbstream.pdf");
            checkJpegStream(testResultsDir, "jpegrgbstream.pdf",
                    JPEGFactoryTest.class.getResourceAsStream("jpeg.jpg"));
        }
    }

    @Test
    public void testCreateFromFile256() throws IOException
    {
        try (InputStream stream = JPEGFactoryTest.class.getResourceAsStream("jpeg256.jpg"))
        {
            File file = folder.newFile();
            Files.copy(stream, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            PDImageXObject ximage = JPEGFactory.createFromFile(file);
            validate(ximage, 8, 344, 287, "jpg", PDDeviceGray.INSTANCE.getName());

            doWritePDF(new PDDocument(), ximage, testResultsDir, "jpeg256stream.pdf");
            checkJpegStream(testResultsDir, "jpeg256stream.pdf",
                    JPEGFactoryTest.class.getResourceAsStream("jpeg256.jpg"));
        }
    }

    @Test
    public void testCreateFromImageRGB() throws IOException
    {
        PDDocument document = new PDDocument();
        BufferedImage image = ImageIO.read(JPEGFactoryTest.class.getResourceAsStream("jpeg.jpg"));
        assertEquals(3, image.getColorModel().getNumComponents());
        PDImageXObject ximage = JPEGFactory.createFromImage(image);
        validate(ximage, 8, 344, 287, "jpg", PDDeviceRGB.INSTANCE.getName());

        doWritePDF(document, ximage, testResultsDir, "jpegrgb.pdf");
    }

    @Test
    public void testCreateFromImage256() throws IOException
    {
        PDDocument document = new PDDocument();
        BufferedImage image = ImageIO
                .read(JPEGFactoryTest.class.getResourceAsStream("jpeg256.jpg"));
        assertEquals(1, image.getColorModel().getNumComponents());
        PDImageXObject ximage = JPEGFactory.createFromImage(image);
        validate(ximage, 8, 344, 287, "jpg", PDDeviceGray.INSTANCE.getName());

        doWritePDF(document, ximage, testResultsDir, "jpeg256.pdf");
    }

    @Test
    public void testCreateFromImageINT_ARGB() throws IOException
    {
        // workaround Open JDK bug
        // http://bugs.java.com/bugdatabase/view_bug.do?bug_id=7044758
        if (System.getProperty("java.runtime.name").equals("OpenJDK Runtime Environment")
                && (System.getProperty("java.specification.version").equals("1.6")
                        || System.getProperty("java.specification.version").equals("1.7")
                        || System.getProperty("java.specification.version").equals("1.8")))
        {
            return;
        }

        PDDocument document = new PDDocument();
        BufferedImage image = ImageIO.read(JPEGFactoryTest.class.getResourceAsStream("jpeg.jpg"));

        // create an ARGB image
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage argbImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics ag = argbImage.getGraphics();
        ag.drawImage(image, 0, 0, null);
        ag.dispose();

        for (int x = 0; x < argbImage.getWidth(); ++x)
        {
            for (int y = 0; y < argbImage.getHeight(); ++y)
            {
                argbImage.setRGB(x, y, (argbImage.getRGB(x, y) & 0xFFFFFF) | ((y / 10 * 10) << 24));
            }
        }

        PDImageXObject ximage = JPEGFactory.createFromImage(argbImage);
        validate(ximage, 8, width, height, "jpg", PDDeviceRGB.INSTANCE.getName());
        assertNotNull(ximage.getSoftMask());
        validate(ximage.getSoftMask(), 8, width, height, "jpg", PDDeviceGray.INSTANCE.getName());
        assertTrue(colorCount(ximage.getSoftMask().getImage()) > image.getHeight() / 10);

        doWritePDF(document, ximage, testResultsDir, "jpeg-intargb.pdf");
    }

    @Test
    public void testCreateFromImage4BYTE_ABGR() throws IOException
    {
        // workaround Open JDK bug
        // http://bugs.java.com/bugdatabase/view_bug.do?bug_id=7044758
        if (System.getProperty("java.runtime.name").equals("OpenJDK Runtime Environment")
                && (System.getProperty("java.specification.version").equals("1.6")
                        || System.getProperty("java.specification.version").equals("1.7")
                        || System.getProperty("java.specification.version").equals("1.8")))
        {
            return;
        }

        PDDocument document = new PDDocument();
        BufferedImage image = ImageIO.read(JPEGFactoryTest.class.getResourceAsStream("jpeg.jpg"));

        // create an ARGB image
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage argbImage = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics ag = argbImage.getGraphics();
        ag.drawImage(image, 0, 0, null);
        ag.dispose();

        for (int x = 0; x < argbImage.getWidth(); ++x)
        {
            for (int y = 0; y < argbImage.getHeight(); ++y)
            {
                argbImage.setRGB(x, y, (argbImage.getRGB(x, y) & 0xFFFFFF) | ((y / 10 * 10) << 24));
            }
        }

        PDImageXObject ximage = JPEGFactory.createFromImage(argbImage);
        validate(ximage, 8, width, height, "jpg", PDDeviceRGB.INSTANCE.getName());
        assertNotNull(ximage.getSoftMask());
        validate(ximage.getSoftMask(), 8, width, height, "jpg", PDDeviceGray.INSTANCE.getName());
        assertTrue(colorCount(ximage.getSoftMask().getImage()) > image.getHeight() / 10);

        doWritePDF(document, ximage, testResultsDir, "jpeg-4bargb.pdf");
    }

    /**
     * Tests USHORT_555_RGB JPEGFactory#createFromImage(PDDocument document, BufferedImage image), see also PDFBOX-4674.
     * 
     * @throws java.io.IOException
     */
    @Test
    public void testCreateFromImageUSHORT_555_RGB() throws IOException
    {
        // workaround Open JDK bug
        // http://bugs.java.com/bugdatabase/view_bug.do?bug_id=7044758
        if (System.getProperty("java.runtime.name").equals("OpenJDK Runtime Environment")
                && (System.getProperty("java.specification.version").equals("1.6")
                        || System.getProperty("java.specification.version").equals("1.7")
                        || System.getProperty("java.specification.version").equals("1.8")))
        {
            return;
        }

        PDDocument document = new PDDocument();
        BufferedImage image = ImageIO.read(JPEGFactoryTest.class.getResourceAsStream("jpeg.jpg"));

        // create an USHORT_555_RGB image
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage rgbImage = new BufferedImage(width, height,
                BufferedImage.TYPE_USHORT_555_RGB);
        Graphics ag = rgbImage.getGraphics();
        ag.drawImage(image, 0, 0, null);
        ag.dispose();

        for (int x = 0; x < rgbImage.getWidth(); ++x)
        {
            for (int y = 0; y < rgbImage.getHeight(); ++y)
            {
                rgbImage.setRGB(x, y, (rgbImage.getRGB(x, y) & 0xFFFFFF) | ((y / 10 * 10) << 24));
            }
        }

        PDImageXObject ximage = JPEGFactory.createFromImage(rgbImage);
        validate(ximage, 8, width, height, "jpg", PDDeviceRGB.INSTANCE.getName());
        assertNull(ximage.getSoftMask());

        doWritePDF(document, ximage, testResultsDir, "jpeg-ushort555rgb.pdf");
    }

    @Test
    public void testCreateFromSeekableSource() throws IOException
    {
        try (InputStream stream = JPEGFactoryTest.class.getResourceAsStream("jpeg.jpg"))
        {
            PDImageXObject ximage = JPEGFactory
                    .createFromSeekableSource(SeekableSources.onTempFileSeekableSourceFrom(stream));
            validate(ximage, 8, 344, 287, "jpg", PDDeviceRGB.INSTANCE.getName());

            doWritePDF(new PDDocument(), ximage, testResultsDir, "jpegrgbstream.pdf");
            checkJpegStream(testResultsDir, "jpegrgbstream.pdf",
                    JPEGFactoryTest.class.getResourceAsStream("jpeg.jpg"));
        }
    }

    // check whether it is possible to extract the jpeg stream exactly
    // as it was passed to createFromStream
    private void checkJpegStream(File testResultsDir, String filename, InputStream resourceStream)
            throws IOException
    {
        try (PDDocument doc = PDFParser
                .parse(SeekableSources.seekableSourceFrom(new File(testResultsDir, filename))))
        {
            PDImageXObject img = (PDImageXObject) doc.getPage(0).getResources()
                    .getXObject(COSName.getPDFName("Im1"));
            InputStream dctStream = img.getCOSObject().getFilteredStream();
            assertArrayEquals(IOUtils.toByteArray(resourceStream), IOUtils.toByteArray(dctStream));
            resourceStream.close();
            dctStream.close();
        }
    }
}
