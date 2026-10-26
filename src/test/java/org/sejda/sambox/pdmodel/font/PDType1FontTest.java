package org.sejda.sambox.pdmodel.font;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;

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
