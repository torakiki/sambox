/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sejda.sambox.text;

import org.apache.fontbox.ttf.TrueTypeFont;
import org.apache.fontbox.util.BoundingBox;
import org.sejda.sambox.contentstream.PDFStreamEngine;
import org.sejda.sambox.contentstream.operator.DrawObject;
import org.sejda.sambox.contentstream.operator.color.SetNonStrokingColor;
import org.sejda.sambox.contentstream.operator.color.SetNonStrokingColorN;
import org.sejda.sambox.contentstream.operator.color.SetNonStrokingColorSpace;
import org.sejda.sambox.contentstream.operator.color.SetNonStrokingDeviceCMYKColor;
import org.sejda.sambox.contentstream.operator.color.SetNonStrokingDeviceGrayColor;
import org.sejda.sambox.contentstream.operator.color.SetNonStrokingDeviceRGBColor;
import org.sejda.sambox.contentstream.operator.color.SetStrokingColor;
import org.sejda.sambox.contentstream.operator.color.SetStrokingColorN;
import org.sejda.sambox.contentstream.operator.color.SetStrokingColorSpace;
import org.sejda.sambox.contentstream.operator.color.SetStrokingDeviceCMYKColor;
import org.sejda.sambox.contentstream.operator.color.SetStrokingDeviceGrayColor;
import org.sejda.sambox.contentstream.operator.color.SetStrokingDeviceRGBColor;
import org.sejda.sambox.contentstream.operator.state.Concatenate;
import org.sejda.sambox.contentstream.operator.state.Restore;
import org.sejda.sambox.contentstream.operator.state.Save;
import org.sejda.sambox.contentstream.operator.state.SetGraphicsStateParameters;
import org.sejda.sambox.contentstream.operator.state.SetMatrix;
import org.sejda.sambox.contentstream.operator.text.BeginText;
import org.sejda.sambox.contentstream.operator.text.EndText;
import org.sejda.sambox.contentstream.operator.text.MoveText;
import org.sejda.sambox.contentstream.operator.text.MoveTextSetLeading;
import org.sejda.sambox.contentstream.operator.text.NextLine;
import org.sejda.sambox.contentstream.operator.text.SetCharSpacing;
import org.sejda.sambox.contentstream.operator.text.SetFontAndSize;
import org.sejda.sambox.contentstream.operator.text.SetTextHorizontalScaling;
import org.sejda.sambox.contentstream.operator.text.SetTextLeading;
import org.sejda.sambox.contentstream.operator.text.SetTextRenderingMode;
import org.sejda.sambox.contentstream.operator.text.SetTextRise;
import org.sejda.sambox.contentstream.operator.text.SetWordSpacing;
import org.sejda.sambox.contentstream.operator.text.ShowText;
import org.sejda.sambox.contentstream.operator.text.ShowTextAdjusted;
import org.sejda.sambox.contentstream.operator.text.ShowTextLine;
import org.sejda.sambox.contentstream.operator.text.ShowTextLineAndSpace;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.pdmodel.PDPage;
import org.sejda.sambox.pdmodel.common.PDRectangle;
import org.sejda.sambox.pdmodel.font.PDCIDFont;
import org.sejda.sambox.pdmodel.font.PDCIDFontType2;
import org.sejda.sambox.pdmodel.font.PDFont;
import org.sejda.sambox.pdmodel.font.PDFontDescriptor;
import org.sejda.sambox.pdmodel.font.PDSimpleFont;
import org.sejda.sambox.pdmodel.font.PDTrueTypeFont;
import org.sejda.sambox.pdmodel.font.PDType0Font;
import org.sejda.sambox.pdmodel.font.PDType3Font;
import org.sejda.sambox.pdmodel.font.encoding.GlyphList;
import org.sejda.sambox.pdmodel.graphics.color.PDColor;
import org.sejda.sambox.pdmodel.graphics.state.PDGraphicsState;
import org.sejda.sambox.pdmodel.graphics.state.RenderingMode;
import org.sejda.sambox.util.Matrix;
import org.sejda.sambox.util.Vector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.WeakHashMap;

import static org.sejda.commons.util.RequireUtils.requireNotNullArg;

/**
 * PDFStreamEngine subclass for advanced processing of text via TextPosition.
 *
 * @author Ben Litchfield
 * @author John Hewson
 * @see org.sejda.sambox.text.TextPosition
 */
public class PDFTextStreamEngine extends PDFStreamEngine
{
    private static final Logger LOG = LoggerFactory.getLogger(PDFTextStreamEngine.class);

    private int pageRotation;
    private PDRectangle cropBox;
    private Matrix translateMatrix;
    private static final GlyphList GLYPHLIST;
    private final Map<COSDictionary, Float> fontHeightMap = new WeakHashMap<>();

    static
    {
        // load additional glyph list for Unicode mapping
        try
        {
            InputStream input = GlyphList.class.getResourceAsStream(
                    "/org/sejda/sambox/resources/glyphlist/additional.txt");
            requireNotNullArg(input,
                    "Unable to load org/sejda/sambox/resources/glyphlist/additional.txt");
            GLYPHLIST = new GlyphList(GlyphList.getAdobeGlyphList(), input);
        }
        catch (IOException ex)
        {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Constructor.
     */
    public PDFTextStreamEngine() throws IOException
    {
        addOperator(new BeginText());
        addOperator(new Concatenate());
        addOperator(new DrawObject()); // special text version
        addOperator(new EndText());
        addOperator(new SetGraphicsStateParameters());
        addOperator(new Save());
        addOperator(new Restore());
        addOperator(new NextLine());
        addOperator(new SetCharSpacing());
        addOperator(new MoveText());
        addOperator(new MoveTextSetLeading());
        addOperator(new SetFontAndSize());
        addOperator(new ShowText());
        addOperator(new ShowTextAdjusted());
        addOperator(new SetTextLeading());
        addOperator(new SetMatrix());
        addOperator(new SetTextRenderingMode());
        addOperator(new SetTextRise());
        addOperator(new SetWordSpacing());
        addOperator(new SetTextHorizontalScaling());
        addOperator(new ShowTextLine());
        addOperator(new ShowTextLineAndSpace());

        addOperator(new SetStrokingColorSpace());
        addOperator(new SetNonStrokingColorSpace());
        addOperator(new SetGraphicsStateParameters());

        addOperator(new SetStrokingColor());
        addOperator(new SetStrokingDeviceRGBColor());
        addOperator(new SetStrokingDeviceCMYKColor());
        addOperator(new SetStrokingDeviceGrayColor());
        addOperator(new SetStrokingColorN());

        addOperator(new SetNonStrokingColor());
        addOperator(new SetNonStrokingDeviceRGBColor());
        addOperator(new SetNonStrokingDeviceCMYKColor());
        addOperator(new SetNonStrokingDeviceGrayColor());
        addOperator(new SetNonStrokingColorN());
    }

    /**
     * This will initialise and process the contents of the stream.
     *
     * @param page the page to process
     * @throws java.io.IOException if there is an error accessing the stream.
     */
    @Override
    public void processPage(PDPage page) throws IOException
    {
        this.pageRotation = page.getRotation();
        this.cropBox = page.getCropBox();
        if (cropBox.getLowerLeftX() == 0 && cropBox.getLowerLeftY() == 0)
        {
            translateMatrix = null;
        }
        else
        {
            // translation matrix for cropbox
            translateMatrix = Matrix.getTranslateInstance(-cropBox.getLowerLeftX(),
                    -cropBox.getLowerLeftY());
        }
        super.processPage(page);
    }

    /**
     * Called when a glyph is to be processed. The heuristic calculations here were originally
     * written by Ben Litchfield for PDFStreamEngine.
     */
    @Override
    protected void showGlyph(Matrix textRenderingMatrix, PDFont font, int code, Vector displacement)
            throws IOException
    {
        //
        // legacy calculations which were previously in PDFStreamEngine
        //

        PDGraphicsState state = getGraphicsState();
        Matrix ctm = state.getCurrentTransformationMatrix();
        float fontSize = state.getTextState().getFontSize();
        float horizontalScaling = state.getTextState().getHorizontalScaling() / 100f;
        Matrix textMatrix = getTextMatrix();

        float displacementX = displacement.getX();
        // the sorting algorithm is based on the width of the character. As the displacement
        // for vertical characters doesn't provide any suitable value for it, we have to 
        // calculate our own
        if (font.isVertical())
        {
            displacementX = font.getWidth(code) / 1000;
            // there may be an additional scaling factor for true type fonts
            TrueTypeFont ttf = null;
            if (font instanceof PDTrueTypeFont)
            {
                ttf = ((PDTrueTypeFont) font).getTrueTypeFont();
            }
            else if (font instanceof PDType0Font)
            {
                PDCIDFont cidFont = ((PDType0Font) font).getDescendantFont();
                if (cidFont instanceof PDCIDFontType2)
                {
                    ttf = ((PDCIDFontType2) cidFont).getTrueTypeFont();
                }
            }
            if (ttf != null && ttf.getUnitsPerEm() != 1000)
            {
                displacementX *= 1000f / ttf.getUnitsPerEm();
            }
        }

        //
        // legacy calculations which were previously in PDFStreamEngine
        //
        //  DO NOT USE THIS CODE UNLESS YOU ARE WORKING WITH PDFTextStripper.
        //  THIS CODE IS DELIBERATELY INCORRECT
        //

        // (modified) combined displacement, this is calculated *without* taking the character
        // spacing and word spacing into account, due to legacy code in TextStripper
        float tx = displacementX * fontSize * horizontalScaling;
        float ty = displacement.getY() * fontSize;

        // (modified) combined displacement matrix
        Matrix td = Matrix.getTranslateInstance(tx, ty);

        // (modified) text rendering matrix
        Matrix nextTextRenderingMatrix = td.multiply(textMatrix)
                .multiply(ctm); // text space -> device space
        float nextX = nextTextRenderingMatrix.getTranslateX();
        float nextY = nextTextRenderingMatrix.getTranslateY();

        // (modified) width and height calculations
        float dxDisplay = nextX - textRenderingMatrix.getTranslateX();
        Float fontHeight = fontHeightMap.get(font.getCOSObject());
        if (fontHeight == null)
        {
            fontHeight = computeFontHeight(font);
            fontHeightMap.put(font.getCOSObject(), fontHeight);
        }
        float dyDisplay = fontHeight * textRenderingMatrix.getScalingFactorY();

        //
        // start of the original method
        //

        // Note on variable names. There are three different units being used in this code.
        // Character sizes are given in glyph units, text locations are initially given in text
        // units, and we want to save the data in display units. The variable names should end with
        // Text or Disp to represent if the values are in text or disp units (no glyph units are
        // saved).

        float glyphSpaceToTextSpaceFactor = 1 / 1000f;
        if (font instanceof PDType3Font)
        {
            glyphSpaceToTextSpaceFactor = font.getFontMatrix().getScaleX();
        }

        float spaceWidthText = 0;
        try
        {
            // to avoid crash as described in PDFBOX-614, see what the space displacement should be
            spaceWidthText = font.getSpaceWidth() * glyphSpaceToTextSpaceFactor;
        }
        catch (Throwable exception)
        {
            LOG.warn(exception.getMessage(), exception);
        }

        if (spaceWidthText == 0)
        {
            spaceWidthText = font.getAverageFontWidth() * glyphSpaceToTextSpaceFactor;
            // the average space width appears to be higher than necessary so make it smaller
            spaceWidthText *= .80f;
        }
        if (spaceWidthText == 0)
        {
            spaceWidthText = 1.0f; // if could not find font, use a generic value
        }

        // the space width has to be transformed into display units
        float spaceWidthDisplay = spaceWidthText * textRenderingMatrix.getScalingFactorX();

        // use our additional glyph list for Unicode mapping
        String unicode = font.toUnicode(code, GLYPHLIST);

        // when there is no Unicode mapping available, Acrobat simply coerces the character code
        // into Unicode, so we do the same. Subclasses of PDFStreamEngine don't necessarily want
        // this, which is why we leave it until this point in PDFTextStreamEngine.
        if (unicode == null)
        {
            char c = (char) code;
            unicode = String.valueOf(c);
        }
        // adjust for cropbox if needed
        Matrix translatedTextRenderingMatrix;
        if (translateMatrix == null)
        {
            translatedTextRenderingMatrix = textRenderingMatrix;
        }
        else
        {
            translatedTextRenderingMatrix = Matrix.concatenate(translateMatrix,
                    textRenderingMatrix);
            nextX -= cropBox.getLowerLeftX();
            nextY -= cropBox.getLowerLeftY();
        }

        PDColor color = null;
        RenderingMode renderingMode = state.getTextState().getRenderingMode();
        if (renderingMode.isFill())
        {
            color = state.getNonStrokingColor();
        }
        else if (renderingMode.isStroke())
        {
            color = state.getStrokingColor();
        }

        processTextPosition(new TextPosition(pageRotation, cropBox.getWidth(), cropBox.getHeight(),
                translatedTextRenderingMatrix, nextX, nextY, Math.abs(dyDisplay), dxDisplay,
                Math.abs(spaceWidthDisplay), unicode, new int[] { code }, font, fontSize,
                (int) (fontSize * textMatrix.getScalingFactorX()), color, renderingMode));
    }

    /**
     * Compute the font height. Override this if you want to use own calculations.
     *
     * @param font the font.
     * @return the font height.
     * @throws IOException if there is an error while getting the font bounding box.
     */
    protected float computeFontHeight(PDFont font) throws IOException
    {
        BoundingBox bbox = font.getBoundingBox();
        if (bbox.getLowerLeftY() < Short.MIN_VALUE)
        {
            // PDFBOX-2158 and PDFBOX-3130
            // files by Salmat eSolutions / ClibPDF Library
            bbox.setLowerLeftY(-(bbox.getLowerLeftY() + 65536));
        }
        // 1/2 the bbox is used as the height todo: why?
        float glyphHeight = bbox.getHeight() / 2;

        // sometimes the bbox has very high values, but CapHeight is OK
        PDFontDescriptor fontDescriptor = font.getFontDescriptor();
        if (fontDescriptor != null)
        {
            float capHeight = fontDescriptor.getCapHeight();
            if (Float.compare(capHeight, 0) != 0 && (capHeight < glyphHeight
                    || Float.compare(glyphHeight, 0) == 0))
            {
                glyphHeight = capHeight;
            }
            // PDFBOX-3464, PDFBOX-4480, PDFBOX-4553:
            // sometimes even CapHeight has very high value, but Ascent and Descent are ok
            float ascent = fontDescriptor.getAscent();
            float descent = fontDescriptor.getDescent();
            if (capHeight > ascent && ascent > 0 && descent < 0 && (
                    (ascent - descent) / 2 < glyphHeight || Float.compare(glyphHeight, 0) == 0))
            {
                glyphHeight = (ascent - descent) / 2;
            }
        }

        // transformPoint from glyph space -> text space
        float height;
        if (font instanceof PDType3Font)
        {
            height = font.getFontMatrix().transformPoint(0, glyphHeight).y;
        }
        else
        {
            height = glyphHeight / 1000;
        }

        return height;
    }

    /**
     * A method provided as an event interface to allow a subclass to perform some specific
     * functionality when text needs to be processed.
     *
     * @param text The text to be processed.
     */
    protected void processTextPosition(TextPosition text)
    {
        // subclasses can override to provide specific functionality
    }
}
