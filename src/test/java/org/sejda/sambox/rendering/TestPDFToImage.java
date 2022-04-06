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
                else if (!ImageCompareUtils.filesAreIdentical(outFile, inFile))
                {
                    // different files might still have identical content
                    // save the difference (if any) into a diff image
                    BufferedImage bim3 = ImageCompareUtils.diff(ImageIO.read(inFile), ImageIO.read(outFile));
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

    

}
