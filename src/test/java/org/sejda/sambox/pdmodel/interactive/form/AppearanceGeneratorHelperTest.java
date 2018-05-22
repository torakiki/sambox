package org.sejda.sambox.pdmodel.interactive.form;

import org.junit.Test;
import org.sejda.sambox.pdmodel.PDDocument;
import org.sejda.sambox.pdmodel.PDPage;
import org.sejda.sambox.pdmodel.common.PDRectangle;
import org.sejda.sambox.pdmodel.font.PDType1Font;

import java.io.IOException;

import static org.junit.Assert.*;

public class AppearanceGeneratorHelperTest {

    @Test
    public void testCalculateFontSizeForFauxMultiline() throws IOException {
        AppearanceGeneratorHelper helper = new AppearanceGeneratorHelper(multilineTextField());
        helper.setAppearanceValue("This is a test");
        PDRectangle content = new PDRectangle(2, 2, 517, 10);

        float fontSize = helper.calculateFontSize(PDType1Font.HELVETICA, content);
        assertEquals(10.81, fontSize, 0.1);
    }

    @Test
    public void testCalculateFontSizeForMultiline() throws IOException {
        AppearanceGeneratorHelper helper = new AppearanceGeneratorHelper(multilineTextField());
        helper.setAppearanceValue("Line 1\nLine 2\nLine 3\nLine 4\nLine 5");

        PDRectangle content = new PDRectangle(2, 2, 517, 50);
        float fontSize = helper.calculateFontSize(PDType1Font.HELVETICA, content);
        assertEquals(10.81, fontSize, 0.1);
    }

    @Test
    public void testCalculateFontSizeForMultilineDefaultFontSizeFits() throws IOException {
        AppearanceGeneratorHelper helper = new AppearanceGeneratorHelper(multilineTextField());
        helper.setAppearanceValue("Line 1\nLine 2\nLine 3\nLine 4\nLine 5");

        PDRectangle content = new PDRectangle(2, 2, 517, 150);
        float fontSize = helper.calculateFontSize(PDType1Font.HELVETICA, content);
        assertEquals(12.0, fontSize, 0.1);
    }

    @Test
    public void testCalculateFontSize() throws IOException {
        AppearanceGeneratorHelper helper = new AppearanceGeneratorHelper(textField());
        helper.setAppearanceValue("This is a test");

        PDRectangle content = new PDRectangle(2, 2, 517, 10);
        float fontSize = helper.calculateFontSize(PDType1Font.HELVETICA, content);
        assertEquals(10.81, fontSize, 0.1);
    }

    @Test
    public void testCalculateFontSizeWidthTooLarge() throws IOException {
        AppearanceGeneratorHelper helper = new AppearanceGeneratorHelper(textField());
        helper.setAppearanceValue("This is a test for a field that is very wide so the font will be shrinked to fit the width of the field on the page aaaaaaaaaa");

        PDRectangle content = new PDRectangle(2, 2, 517, 10);
        float fontSize = helper.calculateFontSize(PDType1Font.HELVETICA, content);
        assertEquals(9.8, fontSize, 0.1);
    }

    private PDTextField multilineTextField() {
        PDTextField tf = textField();
        tf.setMultiline(true);
        return tf;
    }

    private PDTextField textField() {
        PDDocument doc = new PDDocument();
        doc.addPage(new PDPage());
        PDAcroForm form = new PDAcroForm(doc);
        PDTextField tf =  new PDTextField(form);
        tf.setDefaultAppearance("/Helvetica 10.00 Tf 0 g");
        return tf;
    }
}