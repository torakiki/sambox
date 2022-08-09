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

import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class ImageCompareUtils {

    private static final Logger LOG = LoggerFactory.getLogger(ImageCompareUtils.class);
    
    private ImageCompareUtils() {
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
    public static BufferedImage diff(BufferedImage bim1, BufferedImage bim2) throws IOException
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

        // Tests on jdk8 fail, be more lenient when it comes to image compare
        double threshold = System.getProperty("java.version").startsWith("1.8") ? 99.45d : 99.5d;

        if (percentage < threshold)
        {
            LOG.warn("Similarity percentage: " + percentage + "%");
            return bim3;
        }
        LOG.info("Similarity percentage: " + percentage + "%");
        return null;
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
    private static BufferedImage createEmptyDiffImage(int minWidth, int minHeight, int maxWidth,
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

    public static boolean filesAreIdentical(File left, File right) throws IOException
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

            try (FileInputStream lin = new FileInputStream(
                    left); FileInputStream rin = new FileInputStream(right))
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
            return true;
        }
        return false;
    }
    
    public static void assertSameContents(File expected, File actual) throws IOException {
        if (!filesAreIdentical(actual, expected))
        {
            BufferedImage diffImage = ImageCompareUtils.diff(ImageIO.read(expected), ImageIO.read(actual));
            if (diffImage != null)
            {
                LOG.warn("Images differ:\n" + expected.getAbsolutePath() + "\n" + actual.getAbsolutePath());
                Assert.fail("Images differ");
            }
        }
    }
}
