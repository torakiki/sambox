package org.sejda.sambox.pdmodel.graphics.color;

import org.junit.Test;
import org.sejda.io.SeekableSources;
import org.sejda.sambox.input.PDFParser;
import org.sejda.sambox.pdmodel.PDDocument;
import org.sejda.sambox.pdmodel.common.PDRectangle;
import org.sejda.sambox.rendering.PDFRenderer;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class PDColorSpaceTest {

    private static final int scale = 1;

    @Test
    public void testPDFBox4022() throws IOException {
        try(PDDocument doc = PDFParser.parse(SeekableSources.seekableSourceFrom(
                new File("target/pdfs", "PDFBOX-4022-selection.pdf")))) {
            PDFRenderer renderer = new PDFRenderer(doc);
            for(int i = 0; i < doc.getNumberOfPages(); i++) {
                PDRectangle cropBox = doc.getPage(i).getCropBox();
                BufferedImage image = new BufferedImage((int)cropBox.getWidth() * scale, (int)cropBox.getHeight() * scale, BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics2D = image.createGraphics();
                renderer.renderPageToGraphics(i, graphics2D);
            }
        }
    }
}