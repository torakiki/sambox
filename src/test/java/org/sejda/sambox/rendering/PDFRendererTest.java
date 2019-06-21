package org.sejda.sambox.rendering;

import org.junit.Test;
import org.sejda.io.SeekableSources;
import org.sejda.sambox.input.PDFParser;
import org.sejda.sambox.pdmodel.PDDocument;

import java.io.File;
import java.io.IOException;

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
}