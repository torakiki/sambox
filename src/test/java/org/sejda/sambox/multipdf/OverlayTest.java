package org.sejda.sambox.multipdf;

import org.junit.Test;
import org.sejda.io.SeekableSources;
import org.sejda.sambox.input.PDFParser;
import org.sejda.sambox.pdmodel.PDDocument;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class OverlayTest {

    @Test
    public void testOverlay() throws IOException {
        Overlay overlay = new Overlay();

        overlay.setOverlayPosition(Overlay.Position.FOREGROUND);

        PDDocument background = PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(getClass().getResourceAsStream("test-file-scan.pdf")));
        PDDocument foreground = PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(getClass().getResourceAsStream("test-file-scan-text.pdf")));

        PDDocument result = overlay.overlayDocuments(background, foreground);

        // TODO: how to check more things?
        assertEquals(3, result.getNumberOfPages());
    }

}