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
package org.apache.pdfbox;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import org.apache.pdfbox.input.PDFParser;
import org.apache.pdfbox.output.WriteOption;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.junit.Test;
import org.sejda.io.SeekableSources;
import org.sejda.util.IOUtils;

/**
 * @author Andrea Vacondio
 *
 */
public class MyTest
{

    private static final String PATH = "/home/torakiki/Scaricati/Nora Roberts - Vision In white.pdf";

    @Test
    public void testMerge() throws IOException
    {
        PDDocument first = PDFParser.parse(SeekableSources.seekableSourceFrom(new File(
                "/home/torakiki/Scaricati/Form 1221.pdf")));
        PDDocument second = PDFParser.parse(SeekableSources.seekableSourceFrom(new File(
                "/home/torakiki/Scaricati/1221.pdf")));
        try (PDDocument doc = new PDDocument())
        {
            first.getPages().forEach(doc::addPage);
            second.getPages().forEach(doc::addPage);
            doc.writeTo("/home/torakiki/Scaricati/merged.pdf", WriteOption.XREF_STREAM);
        }
        finally
        {
            IOUtils.close(first);
            IOUtils.close(second);
        }
    }

    @Test
    public void testBox() throws IOException
    {
        try (PDDocument doc = PDFParser.parse(SeekableSources.seekableSourceFrom(new File(PATH))))
        {
            doc.writeTo(
                    "/home/torakiki/Scaricati/Nora Roberts - Vision In white_ompressed_xref.pdf",
                    WriteOption.XREF_STREAM);
        }
    }

    private static final String PATH2 = "/home/torakiki/Scaricati/PP Civils_uncompressed_to_compressed_xref.pdf";

    @Test
    public void testBox2() throws IOException
    {
        try (PDDocument doc = PDFParser.parse(SeekableSources.seekableSourceFrom(new File(PATH2))))
        {
            doc.writeTo("/home/torakiki/Scaricati/PP Civils_compressed_to_compressed_xref.pdf",
                    WriteOption.XREF_STREAM);
        }
    }

    @Test
    public void testRender() throws IOException
    {
        try (PDDocument doc = PDFParser.parse(SeekableSources.seekableSourceFrom(new File(PATH))))
        {
            PDFRenderer renderer = new PDFRenderer(doc);
            BufferedImage page = renderer.renderImage(4);
        }
    }
}
