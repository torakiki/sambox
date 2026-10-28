package org.sejda.sambox.pdmodel.font;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.sejda.io.SeekableSources.inMemorySeekableSourceFrom;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.input.PDFParser;

public class PDType1FontTest
{

    @Test
    public void lazyLoadingOneInstance()
    {
        PDType1Font font1 = PDType1Font.HELVETICA();
        PDType1Font font2 = PDType1Font.HELVETICA();

        assertTrue(font1 == font2);
    }

    @Test
    public void lazyLoadingAllDefined()
    {
        for (PDType1Font.StandardFont key : PDType1Font.StandardFont.values())
        {
            assertNotNull(PDType1Font.getStandardFont(key));
        }
    }

    @Test
    public void widthsAfterNonNumberEntry() throws IOException
    {
        // Helvetica with /FirstChar 65 /Widths [600 null 700 800 900 1000], the null entry doesn't shift the
        // following widths
        try (var document = PDFParser.parse(inMemorySeekableSourceFrom(
                getClass().getResourceAsStream("/sambox/widths_non_number.pdf"))))
        {
            var font = document.getPage(0).getResources().getFont(COSName.getPDFName("F1"));
            assertEquals(600f, font.getWidth('A'));
            assertEquals(0f, font.getWidth('B'));
            assertEquals(700f, font.getWidth('C'));
            assertEquals(800f, font.getWidth('D'));
            assertEquals(900f, font.getWidth('E'));
            assertEquals(1000f, font.getWidth('F'));
        }
    }

    @Test
    public void standardFontWidthsConcurrently() throws Exception
    {
        // standard 14 fonts are singletons, shared by the documents processed by different threads
        var font = PDType1Font.COURIER_BOLD_OBLIQUE();
        // codes not used elsewhere, so their widths are not cached yet. They are all .notdef with a width of 250
        Callable<Void> widths = () -> {
            for (int code = 1000; code < 21000; code++)
            {
                assertEquals(250f, font.getWidth(code));
            }
            return null;
        };
        try (var executor = Executors.newFixedThreadPool(8))
        {
            for (var result : executor.invokeAll(Collections.nCopies(8, widths)))
            {
                result.get();
            }
        }
    }
}
