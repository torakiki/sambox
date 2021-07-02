package org.sejda.sambox.pdmodel.interactive.form;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSString;
import org.sejda.sambox.pdmodel.PDDocument;
import org.sejda.sambox.pdmodel.PDResources;
import org.sejda.sambox.pdmodel.common.PDRectangle;
import org.sejda.sambox.pdmodel.font.PDFont;
import org.sejda.sambox.pdmodel.font.PDType0Font;
import org.sejda.sambox.pdmodel.interactive.annotation.PDAnnotationWidget;

public class PDListBoxTest
{

    private PDDocument doc;
    private PDAcroForm acroForm;

    @Before
    public void setUp()
    {
        doc = new PDDocument();
        acroForm = new PDAcroForm(doc);
        acroForm.setDefaultResources(new PDResources());
    }
    
    @Test
    public void overridingAppearanceFont() throws IOException
    {
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

    @Test
    public void optionsWithTabChars() throws IOException {
        PDChoice choiceField = new PDListBox(acroForm);

        // makes sure an appearance will be generated when we set a value
        choiceField.getWidgets().get(0).setRectangle(new PDRectangle(10, 10, 100, 10));

        COSArray choiceFieldOptions = new COSArray();
        choiceFieldOptions.add(COSString.parseLiteral("Option\t1"));
        choiceFieldOptions.add(COSString.parseLiteral("Option\t2"));

        // add the options using the low level COS model as the PD model will
        // abstract the COSArray
        choiceField.getCOSObject().setItem(COSName.OPT, choiceFieldOptions);

        assertEquals(Arrays.asList("Option\t1", "Option\t2"), choiceField.getOptions());

        choiceField.setValue("Option\t1");
        assertEquals(choiceField.getValue(), Arrays.asList("Option\t1"));
    }
}
