package org.sejda.sambox.pdmodel.font;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.Test;

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
}
