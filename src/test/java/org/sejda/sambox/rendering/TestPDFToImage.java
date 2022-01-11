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
package org.sejda.sambox.rendering;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.sejda.commons.util.IOUtils;
import org.sejda.io.SeekableSources;
import org.sejda.sambox.ParallelParameterized;
import org.sejda.sambox.input.PDFParser;
import org.sejda.sambox.pdmodel.PDDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.fail;

/**
 * Test suite for rendering.
 *
 * FILE SET VALIDATION
 *
 * This test suite is designed to test PDFToImage using a set of PDF files and known good output for each. The default
 * mode of testAll() is to process each *.pdf file in "src/test/resources/input/rendering". An output file is created in
 * "target/test-output/rendering" with the same name as the PDF file, plus an additional page number and ".png" suffix.
 *
 * The output file is then tested against a known good result file from the input directory (again, with the same name
 * as the tested PDF file, but with the additional page number and ".png" suffix).
 *
 * If the two aren't identical, a graphical .diff.png file is created. If they are identical, the output .png file is
 * deleted. If a "good result" file doesn't exist, the output .png file is left there for human inspection.
 *
 * Errors are flagged by creating empty files with appropriate names in the target directory.
 *
 * @author Daniel Wilson
 * @author Ben Litchfield
 * @author Tilman Hausherr
 */
@RunWith(ParallelParameterized.class)
public class TestPDFToImage
{

    /**
     * Logger instance.
     */
    private static final Logger LOG = LoggerFactory.getLogger(TestPDFToImage.class);

    static String inDir = "src/test/resources/input/rendering";
    static String outDir = "target/test-output/rendering/";

    String filename;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data()
    {
        File[] testFiles = new File(inDir).listFiles(new FilenameFilter()
        {
            @Override
            public boolean accept(File dir, String name)
            {
                return (name.toLowerCase().endsWith(".pdf") || name.toLowerCase().endsWith(".ai"));
            }
        });

        List<Object[]> params = new ArrayList<>();
        for (File file : testFiles)
        {
            params.add(new Object[] { file.getName() });
        }
        return params;
    }

    /**
     * Test class constructor.
     *
     * @param filename The name of the test class.
     *
     * @throws IOException If there is an error creating the test.
     */
    public TestPDFToImage(String filename) throws IOException
    {
        this.filename = filename;
    }

    /**
     * Test to validate image rendering of file.
     *
     * @throws IOException when there is an exception
     */
    @Test
    public void testRenderImage() throws IOException
    {
        new File(outDir).mkdirs();

        if (!doTestFile(new File(inDir, filename), inDir, outDir))
        {
            fail("failure, see test log for details");
        }
    }

    /**
     * Create an image; the part between the smaller and the larger image is painted black, the rest in white
     *
     * @param minWidth width of the smaller image
     * @param minHeight width of the smaller image
     * @param maxWidth height of the larger image
     * @param maxHeight height of the larger image
     *
     * @return
     */
    private BufferedImage createEmptyDiffImage(int minWidth, int minHeight, int maxWidth,
            int maxHeight)
    {
        BufferedImage bim3 = new BufferedImage(maxWidth, maxHeight, BufferedImage.TYPE_INT_RGB);
        Graphics graphics = bim3.getGraphics();
        if (minWidth != maxWidth || minHeight != maxHeight)
        {
            graphics.setColor(Color.BLACK);
            graphics.fillRect(0, 0, maxWidth, maxHeight);
        }
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, minWidth, minHeight);
        graphics.dispose();
        return bim3;
    }

    /**
     * Get the difference between two images, identical colors are set to white, differences are xored, the highest bit
     * of each color is reset to avoid colors that are too light
     *
     * @param bim1
     * @param bim2
     * @return If the images are different, the function returns a diff image If the images are identical, the function
     * returns null If the size is different, a black border on the botton and the right is created
     *
     * @throws IOException
     */
    private BufferedImage diffImages(BufferedImage bim1, BufferedImage bim2) throws IOException
    {
        int minWidth = Math.min(bim1.getWidth(), bim2.getWidth());
        int minHeight = Math.min(bim1.getHeight(), bim2.getHeight());
        int maxWidth = Math.max(bim1.getWidth(), bim2.getWidth());
        int maxHeight = Math.max(bim1.getHeight(), bim2.getHeight());
        BufferedImage bim3 = null;
        if (minWidth != maxWidth || minHeight != maxHeight)
        {
            bim3 = createEmptyDiffImage(minWidth, minHeight, maxWidth, maxHeight);
        }

        long difference = 0;
        for (int x = 0; x < minWidth; ++x)
        {
            for (int y = 0; y < minHeight; ++y)
            {
                int rgb1 = bim1.getRGB(x, y);
                int rgb2 = bim2.getRGB(x, y);
                int threshold = 3;
                int diffR = Math.abs((rgb1 & 0xFF) - (rgb2 & 0xFF));
                int diffG = Math.abs(((rgb1 >> 8) & 0xFF) - ((rgb2 >> 8) & 0xFF));
                int diffB = Math.abs(((rgb1 >> 16) & 0xFF) - ((rgb2 >> 16) & 0xFF));

                difference = difference + diffR + diffG + diffB;

                if (rgb1 != rgb2 && (diffR > threshold || diffG > threshold || diffB > threshold))
                {
                    if (bim3 == null)
                    {
                        bim3 = createEmptyDiffImage(minWidth, minHeight, maxWidth, maxHeight);
                    }
                    int r = Math.abs((rgb1 & 0xFF) - (rgb2 & 0xFF));
                    int g = Math.abs((rgb1 & 0xFF00) - (rgb2 & 0xFF00));
                    int b = Math.abs((rgb1 & 0xFF0000) - (rgb2 & 0xFF0000));
                    bim3.setRGB(x, y, 0xFFFFFF - (r | g | b));
                }
                else
                {
                    if (bim3 != null)
                    {
                        bim3.setRGB(x, y, Color.WHITE.getRGB());
                    }
                }
            }
        }

        // Total number of red pixels = width * height
        // Total number of blue pixels = width * height
        // Total number of green pixels = width * height
        // So total number of pixels = width * height * 3
        double totalPixels = bim1.getWidth() * bim1.getHeight() * 3d;

        // Normalizing the value of different pixels
        // for accuracy(average pixels per color
        // component)
        double avgDifferentPixels = difference / totalPixels;

        // There are 255 values of pixels in total
        double percentage = 100 - (avgDifferentPixels / 255) * 100d;

        if (percentage < 99.5d)
        {
            LOG.warn("Similarity percentage: " + percentage + "%");
            return bim3;
        }
        else
        {
            LOG.info("Similarity percentage: " + percentage + "%");
            return null;
        }
    }

    public boolean doTestFile(final File file, File inDir, File outDir) throws IOException
    {
        return doTestFile(file, inDir.getAbsolutePath(), outDir.getAbsolutePath());
    }

    /**
     * Validate the renderings of a single file.
     *
     * @param file The file to validate
     * @param inDir Name of the input directory
     * @param outDir Name of the output directory
     * @return false if the test failed (not identical or other problem), true if the test succeeded (all identical)
     * @throws IOException when there is an exception
     */
    public boolean doTestFile(final File file, String inDir, String outDir) throws IOException
    {
        PDDocument document = null;
        boolean failed = false;

        LOG.info("Opening: " + file.getName());
        try
        {
            document = PDFParser.parse(SeekableSources.seekableSourceFrom(file));
            String outputPrefix = file.getName() + "-";
            int numPages = document.getNumberOfPages();
            if (numPages < 1)
            {
                failed = true;
                LOG.error("file " + file.getName() + " has < 1 page");
            }

            LOG.info("Rendering: " + file.getName());
            PDFRenderer renderer = new PDFRenderer(document);
            for (int i = 0; i < numPages; i++)
            {
                String fileName = outputPrefix + (i + 1) + ".png";
                BufferedImage image = renderer.renderImageWithDPI(i, 96); // Windows native DPI
                LOG.info("Writing: " + fileName);
                ImageIO.write(image, "PNG", new File(outDir, fileName));
            }

            // test to see whether file is destroyed in pdfbox
            File tmpFile = File.createTempFile("pdfbox", ".pdf");
            document.writeTo(tmpFile);
            try (PDDocument doc = PDFParser.parse(SeekableSources.seekableSourceFrom(tmpFile)))
            {
            }
            tmpFile.delete();
        }
        catch (IOException e)
        {
            failed = true;
            LOG.error("Error converting file " + file.getName(), e);
            throw e;
        }
        finally
        {
            IOUtils.close(document);
        }

        LOG.info("Comparing: " + file.getName());

        // Now check the resulting files ... did we get identical PNG(s)?
        try
        {
            File[] outFiles = new File(outDir).listFiles(new FilenameFilter()
            {
                @Override
                public boolean accept(File dir, String name)
                {
                    return (name.endsWith(".png") && name.startsWith(file.getName(), 0))
                            && !name.endsWith(".png-diff.png");
                }
            });
            if (outFiles.length == 0)
            {
                failed = true;
                LOG.warn("*** TEST FAILURE *** Output missing for file: " + file.getName());
            }
            for (File outFile : outFiles)
            {
                File inFile = new File(inDir, outFile.getName());
                if (!inFile.exists())
                {
                    failed = true;
                    LOG.warn("*** TEST FAILURE *** Input missing for file: " + inFile.getName()
                            + " " + inFile.getAbsolutePath());
                }
                else if (!filesAreIdentical(outFile, inFile))
                {
                    // different files might still have identical content
                    // save the difference (if any) into a diff image
                    BufferedImage bim3 = diffImages(ImageIO.read(inFile), ImageIO.read(outFile));
                    if (bim3 != null)
                    {
                        failed = true;
                        LOG.warn("*** TEST FAILURE *** Input and output not identical for file: "
                                + inFile.getName());
                        ImageIO.write(bim3, "png",
                                new File(outFile.getAbsolutePath() + "-diff.png"));
                        LOG.error("Files differ: " + inFile.getAbsolutePath() + "\n"
                                + "              " + outFile.getAbsolutePath());
                    }
                    else
                    {
                        LOG.info("*** TEST OK *** for file: " + inFile.getName());
                        LOG.info("Deleting: " + outFile.getName());
                        outFile.delete();
                        outFile.deleteOnExit();
                    }
                }
                else
                {
                    LOG.info("*** TEST OK *** for file: " + inFile.getName());
                    LOG.info("Deleting: " + outFile.getName());
                    outFile.delete();
                    outFile.deleteOnExit();
                }
            }
        }
        catch (Exception e)
        {
            failed = true;
            LOG.error("Error comparing file output for " + file.getName(), e);
        }

        return !failed;
    }

    private boolean filesAreIdentical(File left, File right) throws IOException
    {
        // http://forum.java.sun.com/thread.jspa?threadID=688105&messageID=4003259
        // http://web.archive.org/web/20060515173719/http://forum.java.sun.com/thread.jspa?threadID=688105&messageID=4003259

        /*
         * -- I reworked ASSERT's into IF statement -- dwilson assert left != null; assert right != null; assert
         * left.exists(); assert right.exists();
         */
        if (left != null && right != null && left.exists() && right.exists())
        {
            if (left.length() != right.length())
            {
                return false;
            }

            FileInputStream lin = new FileInputStream(left);
            FileInputStream rin = new FileInputStream(right);
            try
            {
                byte[] lbuffer = new byte[4096];
                byte[] rbuffer = new byte[lbuffer.length];
                int lcount;
                while ((lcount = lin.read(lbuffer)) > 0)
                {
                    int bytesRead = 0;
                    int rcount;
                    while ((rcount = rin.read(rbuffer, bytesRead, lcount - bytesRead)) > 0)
                    {
                        bytesRead += rcount;
                    }
                    for (int byteIndex = 0; byteIndex < lcount; byteIndex++)
                    {
                        if (lbuffer[byteIndex] != rbuffer[byteIndex])
                        {
                            return false;
                        }
                    }
                }
            }
            finally
            {
                lin.close();
                rin.close();
            }
            return true;
        }
        else
        {
            return false;
        }
    }

}
