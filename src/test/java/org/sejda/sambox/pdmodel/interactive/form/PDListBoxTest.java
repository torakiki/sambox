package org.sejda.sambox.pdmodel.interactive.form;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.junit.Test;
import org.sejda.sambox.pdmodel.PDDocument;
import org.sejda.sambox.pdmodel.PDResources;
import org.sejda.sambox.pdmodel.common.PDRectangle;
import org.sejda.sambox.pdmodel.font.PDFont;
import org.sejda.sambox.pdmodel.font.PDType0Font;
import org.sejda.sambox.pdmodel.interactive.annotation.PDAnnotationWidget;

public class PDListBoxTest
{

    @Test
    public void overridingAppearanceFont() throws IOException
    {
        PDDocument doc = new PDDocument();
        PDAcroForm acroForm = new PDAcroForm(doc);
        acroForm.setDefaultResources(new PDResources());

        PDListBox listBox = new PDListBox(acroForm);

        PDAnnotationWidget widget = new PDAnnotationWidget();
        widget.setRectangle(new PDRectangle(0, 0, 100, 100));

        listBox.addWidgetIfMissing(widget);
        listBox.setOptions(Arrays.asList("Diacritiçésţșâă"));

        try
        {
            listBox.setValue("Diacritiçésţșâă");
            fail("Exception expected, due to diacritics not being available in Helvetica");
        }
        catch (IllegalArgumentException ex)
        {
            assertThat(ex.getMessage(), containsString("is not available in this font Helvetica"));
        }

        InputStream input = PDFont.class
                .getResourceAsStream("/org/sejda/sambox/resources/ttf/LiberationSans-Regular.ttf");
        PDType0Font font = PDType0Font.load(doc, input);

        listBox.getAcroForm().getDefaultResources().add(font);
        listBox.setAppearanceOverrideFont(font);

        listBox.setValue("Diacritiçésţșâă");
    }
}
