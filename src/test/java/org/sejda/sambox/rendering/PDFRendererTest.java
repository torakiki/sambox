package org.sejda.sambox.rendering;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.Test;
import org.sejda.io.SeekableSources;
import org.sejda.sambox.input.PDFParser;
import org.sejda.sambox.pdmodel.PDDocument;
import javax.imageio.ImageIO;

public class PDFRendererTest {

    private static final File TARGETPDFDIR = new File("target/pdfs");

    @Test
    public void test_LCMSError13_CouldntLinkTheProfilesError() throws IOException {
        try (PDDocument doc = PDFParser.parse(SeekableSources.seekableSourceFrom(new File(TARGETPDFDIR, "PDFBOX_LCMS_Error_13.pdf")))) {
            for(int i = 0; i < doc.getNumberOfPages(); i++) {
                new PDFRenderer(doc).renderImage(i);
            }
        }
    }

    @Test
    public void disabledTextContentRendering() throws IOException {
        String inputResPath = "/sambox/text-image-rendering.pdf";
        try (PDDocument doc = PDFParser.parse(SeekableSources.onTempFileSeekableSourceFrom(
                getClass().getResourceAsStream(inputResPath)))) {
            PDFRenderer renderer = new PDFRenderer(doc){
                @Override
                protected PageDrawer createPageDrawer(PageDrawerParameters parameters) throws IOException {
                    PageDrawer drawer = super.createPageDrawer(parameters);
                    drawer.setTextContentRendered(false);
                    return drawer;
                }
            };
            
            BufferedImage image = renderer.renderImage(0);
            File actual = Files.createTempFile("text-image-rendering", "jpg").toFile();
            actual.deleteOnExit();
            ImageIO.write(image, "jpeg", new FileOutputStream(actual));
            
            File expected = new File(this.getClass().getResource(inputResPath.replace(".pdf", ".jpg")).getFile());
            ImageCompareUtils.assertSameContents(expected, actual);
        }
    }

    @Test
    public void disabledImageContentRendering() throws IOException
    {
        String inputResPath = "/sambox/text-image-rendering.pdf";
        try (PDDocument doc = PDFParser.parse(SeekableSources.onTempFileSeekableSourceFrom(
                getClass().getResourceAsStream(inputResPath))))
        {
            PDFRenderer renderer = new PDFRenderer(doc)
            {
                @Override
                protected PageDrawer createPageDrawer(PageDrawerParameters parameters)
                        throws IOException
                {
                    PageDrawer drawer = super.createPageDrawer(parameters);
                    drawer.setImageContentRendered(false);
                    return drawer;
                }
            };

            BufferedImage image = renderer.renderImage(0);
            File actual = Files.createTempFile("text-image-rendering_no_images", ".jpg").toFile();
            actual.deleteOnExit();
            ImageIO.write(image, "jpeg", new FileOutputStream(actual));

            File expected = new File(
                    this.getClass().getResource("/sambox/text-image-rendering_no_images.jpg")
                            .getFile());
            ImageCompareUtils.assertSameContents(expected, actual);
        }
    }
}
