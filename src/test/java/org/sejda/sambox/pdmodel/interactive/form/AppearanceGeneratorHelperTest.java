package org.sejda.sambox.pdmodel.interactive.form;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.Collections;

import org.junit.Test;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSString;
import org.sejda.sambox.pdmodel.PDDocument;
import org.sejda.sambox.pdmodel.PDPage;
import org.sejda.sambox.pdmodel.common.PDRectangle;
import org.sejda.sambox.pdmodel.font.PDType1Font;
import org.sejda.sambox.pdmodel.interactive.annotation.PDAnnotationWidget;

public class AppearanceGeneratorHelperTest
{

    @Test
    public void testCalculateFontSizeForFauxMultiline() throws IOException
    {
        AppearanceGeneratorHelper helper = new AppearanceGeneratorHelper(multilineTextField());
        helper.setAppearanceValue("This is a test");
        PDRectangle content = new PDRectangle(2, 2, 517, 10);

        float fontSize = helper.calculateFontSize(PDType1Font.HELVETICA(), content);
        assertEquals(10.81, fontSize, 0.1);
    }

    @Test
    public void testCalculateFontSizeForFauxMultilineFieldButUserEnteredMultipleLines()
            throws IOException
    {
        AppearanceGeneratorHelper helper = new AppearanceGeneratorHelper(multilineTextField());
        helper.setAppearanceValue("This is a test\nright here");
        PDRectangle content = new PDRectangle(2, 2, 517, 10);

        float fontSize = helper.calculateFontSize(PDType1Font.HELVETICA(), content);
        assertEquals(5.40, fontSize, 0.1);
    }

    @Test
    public void testCalculateFontSizeForMultiline() throws IOException
    {
        AppearanceGeneratorHelper helper = new AppearanceGeneratorHelper(multilineTextField());
        helper.setAppearanceValue("Line 1\nLine 2\nLine 3\nLine 4\nLine 5");

        PDRectangle content = new PDRectangle(2, 2, 517, 50);
        float fontSize = helper.calculateFontSize(PDType1Font.HELVETICA(), content);
        assertEquals(10.81, fontSize, 0.1);
    }

    @Test
    public void testCalculateFontSizeForMultilineDefaultFontSizeFits() throws IOException
    {
        AppearanceGeneratorHelper helper = new AppearanceGeneratorHelper(multilineTextField());
        helper.setAppearanceValue("Line 1\nLine 2\nLine 3\nLine 4\nLine 5");

        PDRectangle content = new PDRectangle(2, 2, 517, 150);
        float fontSize = helper.calculateFontSize(PDType1Font.HELVETICA(), content);
        assertEquals(12.0, fontSize, 0.1);
    }

    @Test
    public void testCalculateFontSize() throws IOException
    {
        AppearanceGeneratorHelper helper = new AppearanceGeneratorHelper(textField());
        helper.setAppearanceValue("This is a test");

        PDRectangle content = new PDRectangle(2, 2, 517, 10);
        float fontSize = helper.calculateFontSize(PDType1Font.HELVETICA(), content);
        assertEquals(10.81, fontSize, 0.1);
    }

    @Test
    public void testCalculateFontSizeWidthTooLarge() throws IOException
    {
        AppearanceGeneratorHelper helper = new AppearanceGeneratorHelper(textField());
        helper.setAppearanceValue(
                "This is a test for a field that is very wide so the font will be shrinked to fit the width of the field on the page aaaaaaaaaa");

        PDRectangle content = new PDRectangle(2, 2, 517, 10);
        float fontSize = helper.calculateFontSize(PDType1Font.HELVETICA(), content);
        assertEquals(9.8, fontSize, 0.1);
    }

    @Test
    public void testInvalidAppearanceString() throws IOException
    {
        String invalid = "/Helv Tf 0.25 g";

        // make sure this is an invalid default appearance string
        try
        {
            new PDDefaultAppearanceString(COSString.parseLiteral(invalid), null);
        }
        catch (IOException ex)
        {
            assertThat(ex.getMessage(), startsWith("Missing operands for set font operator"));
        }

        PDTextField field = textField();

        // create a widget with invalid default appearance
        PDAnnotationWidget widget = new PDAnnotationWidget();
        widget.getCOSObject().setString(COSName.DA, invalid);
        field.setWidgets(Collections.singletonList(widget));

        // we expect not to crash and instead use a fallback appearance string
        AppearanceGeneratorHelper helper = new AppearanceGeneratorHelper(field);
        helper.setAppearanceValue("Some text");
    }

    @Test
    public void testCalculateFontSizeWithNonPrintableChars() throws IOException
    {
        PDTextField field = multilineTextFieldFaux();

        AppearanceGeneratorHelper helper = new AppearanceGeneratorHelper(field);
        helper.setAppearanceValue("This isnt multiple lines\r");

        assertNotNull(field.getWidgets().get(0).getNormalAppearanceStream());
    }

    @Test
    public void testCalculateFontSizeWithExoticWhiteSpace() throws IOException
    {
        PDTextField field = multilineTextFieldFaux();

        AppearanceGeneratorHelper helper = new AppearanceGeneratorHelper(field);
        helper.setAppearanceValue("This is\u2002whitespace");

        assertNotNull(field.getWidgets().get(0).getNormalAppearanceStream());
    }

    @Test
    public void testComboBoxAppearanceWithExoticWhiteSpace() throws IOException
    {
        PDComboBox field = comboBox();

        // U+2002 (en space) has no glyph in Helvetica / WinAnsiEncoding. Before it is
        // normalized to a regular space, generating the combo box appearance threw
        // IllegalArgumentException from PDFont.encode (reached via getStringWidth in
        // calculateFontSize, and via showText in the rendering path).
        //
        // setValue drives the exact path from the original crash report:
        // PDChoice.setValue -> applyChange -> constructAppearances -> setAppearanceValue
        field.setValue("Option\u2002One");

        // appearance generation must complete and produce a stream, not silently skip
        assertNotNull(field.getWidgets().get(0).getNormalAppearanceStream());
    }

    private PDComboBox comboBox()
    {
        PDDocument doc = new PDDocument();
        doc.addPage(new PDPage());
        PDAcroForm form = new PDAcroForm(doc);
        PDComboBox cb = new PDComboBox(form);
        cb.setDefaultAppearance("/Helvetica 10.00 Tf 0 g");
        cb.getWidgets().get(0).setRectangle(new PDRectangle(2, 2, 100, 20));
        return cb;
    }

    private PDTextField multilineTextField()
    {
        PDTextField tf = textField();
        tf.setMultiline(true);
        return tf;
    }

    private PDTextField multilineTextFieldFaux()
    {
        PDTextField field = multilineTextField();
        // faux multiline, content rectangle not tall enough
        field.getWidgets().get(0).setRectangle(new PDRectangle(2, 2, 517, 10));
        field.setDefaultAppearance("Helvetica 0 Tf 0 g");
        return field;
    }

    private PDTextField textField()
    {
        PDDocument doc = new PDDocument();
        doc.addPage(new PDPage());
        PDAcroForm form = new PDAcroForm(doc);
        PDTextField tf = new PDTextField(form);
        tf.setDefaultAppearance("/Helvetica 10.00 Tf 0 g");
        return tf;
    }
}
