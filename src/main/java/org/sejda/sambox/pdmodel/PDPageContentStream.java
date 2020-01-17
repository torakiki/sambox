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
package org.sejda.sambox.pdmodel;

import static java.util.Objects.nonNull;
import static org.sejda.commons.util.RequireUtils.requireState;
import static org.sejda.io.CountingWritableByteChannel.from;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Stack;

import org.sejda.commons.util.IOUtils;
import org.sejda.sambox.contentstream.operator.OperatorName;
import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSNumber;
import org.sejda.sambox.cos.COSString;
import org.sejda.sambox.output.ContentStreamWriter;
import org.sejda.sambox.pdmodel.common.PDStream;
import org.sejda.sambox.pdmodel.documentinterchange.markedcontent.PDPropertyList;
import org.sejda.sambox.pdmodel.font.PDFont;
import org.sejda.sambox.pdmodel.graphics.PDXObject;
import org.sejda.sambox.pdmodel.graphics.color.PDColor;
import org.sejda.sambox.pdmodel.graphics.color.PDColorSpace;
import org.sejda.sambox.pdmodel.graphics.color.PDDeviceCMYK;
import org.sejda.sambox.pdmodel.graphics.color.PDDeviceGray;
import org.sejda.sambox.pdmodel.graphics.color.PDDeviceN;
import org.sejda.sambox.pdmodel.graphics.color.PDDeviceRGB;
import org.sejda.sambox.pdmodel.graphics.color.PDICCBased;
import org.sejda.sambox.pdmodel.graphics.color.PDPattern;
import org.sejda.sambox.pdmodel.graphics.color.PDSeparation;
import org.sejda.sambox.pdmodel.graphics.form.PDFormXObject;
import org.sejda.sambox.pdmodel.graphics.image.PDImageXObject;
import org.sejda.sambox.pdmodel.graphics.image.PDInlineImage;
import org.sejda.sambox.pdmodel.graphics.pattern.PDTilingPattern;
import org.sejda.sambox.pdmodel.graphics.shading.PDShading;
import org.sejda.sambox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.sejda.sambox.pdmodel.graphics.state.RenderingMode;
import org.sejda.sambox.util.Matrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides the ability to write to a page content stream.
 *
 * @author Ben Litchfield
 */
public final class PDPageContentStream implements Closeable
{
    /**
     * This is to choose what to do with the stream: overwrite, append or prepend.
     */
    public static enum AppendMode
    {
        /**
         * Overwrite the existing page content streams.
         */
        OVERWRITE,
        /**
         * Append the content stream after all existing page content streams.
         */
        APPEND,
        /**
         * Insert before all other page content streams.
         */
        PREPEND;

        public boolean isOverwrite()
        {
            return this == OVERWRITE;
        }

        public boolean isPrepend()
        {
            return this == PREPEND;
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(PDPageContentStream.class);

    private final PDDocument document;
    private ContentStreamWriter writer;
    private PDResources resources;

    private boolean inTextMode = false;
    private final Stack<PDFont> fontStack = new Stack<>();

    private final Stack<PDColorSpace> nonStrokingColorSpaceStack = new Stack<>();
    private final Stack<PDColorSpace> strokingColorSpaceStack = new Stack<>();

    // number format
    private final NumberFormat formatDecimal = NumberFormat.getNumberInstance(Locale.US);

    /**
     * Create a new PDPage content stream.
     *
     * @param document The document the page is part of.
     * @param sourcePage The page to write the contents to.
     * @throws IOException If there is an error writing to the page contents.
     */
    public PDPageContentStream(PDDocument document, PDPage sourcePage) throws IOException
    {
        this(document, sourcePage, AppendMode.OVERWRITE, true, false);
    }

    /**
     * Create a new PDPage content stream.
     *
     * @param document The document the page is part of.
     * @param sourcePage The page to write the contents to.
     * @param appendContent Indicates whether content will be overwritten, appended or prepended.
     * @param compress Tell if the content stream should compress the page contents.
     * @throws IOException If there is an error writing to the page contents.
     */
    public PDPageContentStream(PDDocument document, PDPage sourcePage, AppendMode appendContent,
            boolean compress) throws IOException
    {
        this(document, sourcePage, appendContent, compress, false);
    }

    /**
     * Create a new PDPage content stream.
     *
     * @param document The document the page is part of.
     * @param sourcePage The page to write the contents to.
     * @param appendContent Indicates whether content will be overwritten, appended or prepended.
     * @param compress Tell if the content stream should compress the page contents.
     * @param resetContext Tell if the graphic context should be reset. This is only relevant when the appendContent
     * parameter is set to {@link AppendMode#APPEND}. You should use this when appending to an existing stream, because
     * the existing stream may have changed graphic properties (e.g. scaling, rotation).
     * @throws IOException If there is an error writing to the page contents.
     */
    public PDPageContentStream(PDDocument document, PDPage sourcePage, AppendMode appendContent,
            boolean compress, boolean resetContext) throws IOException
    {
        this.document = document;
        COSName filter = compress ? COSName.FLATE_DECODE : null;
        // If request specifies the need to append/prepend to the document
        if (!appendContent.isOverwrite() && sourcePage.hasContents())
        {

            // Create a pdstream to append new content
            PDStream contentsToAppend = new PDStream();
            // Add new stream to contents array
            COSBase contents = sourcePage.getCOSObject().getDictionaryObject(COSName.CONTENTS);
            COSArray array;
            if (contents instanceof COSArray)
            {
                // If contents is already an array, a new stream is simply appended to it
                array = (COSArray) contents;
            }
            else
            {
                // Creates a new array and adds the current stream plus a new one to it
                array = new COSArray();
                array.add(contents);
            }
            if (appendContent.isPrepend())
            {
                array.add(0, contentsToAppend.getCOSObject());
            }
            else
            {
                array.add(contentsToAppend);
            }

            // save the initial/unmodified graphics context
            if (resetContext)
            {
                // create a new stream to encapsulate the existing stream
                PDStream saveGraphics = new PDStream();
                this.writer = new ContentStreamWriter(
                        from(saveGraphics.createOutputStream(filter)));
                // save the initial/unmodified graphics context
                saveGraphicsState();
                close();
                // insert the new stream at the beginning
                array.add(0, saveGraphics.getCOSObject());
            }

            // Sets the compoundStream as page contents
            sourcePage.getCOSObject().setItem(COSName.CONTENTS, array);
            this.writer = new ContentStreamWriter(
                    from(contentsToAppend.createOutputStream(filter)));
            // restore the initial/unmodified graphics context
            if (resetContext)
            {
                restoreGraphicsState();
            }
        }
        else
        {
            if (sourcePage.hasContents())
            {
                LOG.warn("You are overwriting an existing content, you should use the append mode");
            }
            PDStream contents = new PDStream();
            sourcePage.setContents(contents);
            this.writer = new ContentStreamWriter(from(contents.createOutputStream(filter)));
        }
        // this has to be done here, as the resources will be set to null when reseting the content stream
        resources = sourcePage.getResources();
        if (resources == null)
        {
            resources = new PDResources();
            sourcePage.setResources(resources);
        }

        // configure NumberFormat
        formatDecimal.setMaximumFractionDigits(5);
        formatDecimal.setGroupingUsed(false);
    }

    /**
     * Create a new appearance stream. Note that this is not actually a "page" content stream.
     *
     * @param doc The document the page is part of.
     * @param appearance The appearance stream to write to.
     * @throws IOException If there is an error writing to the page contents.
     */
    public PDPageContentStream(PDDocument doc, PDFormXObject appearance)
    {
        this(doc, appearance,
                new ContentStreamWriter(from(appearance.getStream().createOutputStream())));
    }

    /**
     * Create a new appearance stream. Note that this is not actually a "page" content stream.
     *
     * @param doc The document the appearance is part of.
     * @param appearance The appearance stream to add to.
     * @param writer The writer to write the apperances
     * @throws IOException If there is an error writing to the page contents.
     */
    public PDPageContentStream(PDDocument doc, PDFormXObject appearance, ContentStreamWriter writer)
    {
        this.document = doc;
        this.writer = writer;
        this.resources = appearance.getResources();

        formatDecimal.setMaximumFractionDigits(4);
        formatDecimal.setGroupingUsed(false);
    }

    /**
     * Create a new appearance stream. Note that this is not actually a "page" content stream.
     *
     * @param doc The document the appearance is part of.
     * @param pattern The pattern to add to.
     * @param outputStream The output stream to write to.
     * @throws IOException If there is an error writing to the page contents.
     */
    public PDPageContentStream(PDDocument doc, PDTilingPattern pattern, ContentStreamWriter writer)
            throws IOException
    {
        this.document = doc;

        this.writer = writer;
        this.resources = pattern.getResources();

        formatDecimal.setMaximumFractionDigits(4);
        formatDecimal.setGroupingUsed(false);
    }

    /**
     * Begin some text operations.
     *
     * @throws IOException If there is an error writing to the stream or if you attempt to nest beginText calls.
     * @throws IllegalStateException If the method was not allowed to be called at this time.
     */
    public void beginText() throws IOException
    {
        if (inTextMode)
        {
            throw new IllegalStateException("Error: Nested beginText() calls are not allowed.");
        }
        writeOperator(OperatorName.BEGIN_TEXT);
        inTextMode = true;
    }

    /**
     * End some text operations.
     *
     * @throws IOException If there is an error writing to the stream or if you attempt to nest endText calls.
     * @throws IllegalStateException If the method was not allowed to be called at this time.
     */
    public void endText() throws IOException
    {
        if (!inTextMode)
        {
            throw new IllegalStateException(
                    "Error: You must call beginText() before calling endText.");
        }
        writeOperator(OperatorName.END_TEXT);
        inTextMode = false;
    }

    public void endTextIfRequired() throws IOException
    {
        if (inTextMode)
        {
            endText();
        }
    }

    /**
     * Set the font and font size to draw text with.
     *
     * @param font The font to use.
     * @param fontSize The font size to draw the text.
     * @throws IOException If there is an error writing the font information.
     */
    public void setFont(PDFont font, float fontSize) throws IOException
    {
        if (fontStack.isEmpty())
        {
            fontStack.add(font);
        }
        else
        {
            fontStack.setElementAt(font, fontStack.size() - 1);
        }

        if (font.willBeSubset())
        {
            document.getFontsToSubset().add(font);
        }

        writeOperand(resources.add(font));
        writeOperand(fontSize);
        writeOperator(OperatorName.SET_FONT_AND_SIZE);
    }

    /**
     * Shows the given text at the location specified by the current text matrix with the given interspersed
     * positioning. This allows the user to efficiently position each glyph or sequence of glyphs.
     *
     * @param textWithPositioningArray An array consisting of String and Float types. Each String is output to the page
     * using the current text matrix. Using the default coordinate system, each interspersed number adjusts the current
     * text matrix by translating to the left or down for horizontal and vertical text respectively. The number is
     * expressed in thousands of a text space unit, and may be negative.
     *
     * @throws IOException if an io exception occurs.
     */
    public void showTextWithPositioning(Object[] textWithPositioningArray) throws IOException
    {
        write("[");
        for (Object obj : textWithPositioningArray)
        {
            if (obj instanceof String)
            {
                showTextInternal((String) obj);
            }
            else if (obj instanceof Float)
            {
                writeOperand((Float) obj);
            }
            else
            {
                throw new IllegalArgumentException(
                        "Argument must consist of array of Float and String types");
            }
        }
        write("] ");
        writeOperator(OperatorName.SHOW_TEXT_ADJUSTED);
    }

    /**
     * Shows the given text at the location specified by the current text matrix.
     *
     * @param text The Unicode text to show.
     * @throws IOException If an io exception occurs.
     * @throws IllegalArgumentException if a character isn't supported by the current font
     */
    public void showText(String text) throws IOException
    {
        showTextInternal(text);
        writer.writeSpace();
        writeOperator(OperatorName.SHOW_TEXT);
    }

    /**
     * Shows the given text at the location specified by the current text matrix.
     *
     * @param text The Unicode text to show.
     * @throws IOException If an io exception occurs.
     */
    protected void showTextInternal(String text) throws IOException
    {
        if (!inTextMode)
        {
            throw new IllegalStateException("Must call beginText() before showText()");
        }

        if (fontStack.isEmpty())
        {
            throw new IllegalStateException("Must call setFont() before showText()");
        }

        PDFont font = fontStack.peek();

        // Unicode code points to keep when subsetting
        if (font.willBeSubset())
        {
            int offset = 0;
            while (offset < text.length())
            {
                int codePoint = text.codePointAt(offset);
                font.addToSubset(codePoint);
                offset += Character.charCount(codePoint);
            }
        }

        COSString.newInstance(font.encode(text)).accept(writer);
    }

    /**
     * Sets the text leading.
     *
     * @param leading The leading in unscaled text units.
     * @throws IOException If there is an error writing to the stream.
     */
    public void setLeading(float leading) throws IOException
    {
        writeOperand(leading);
        writeOperator(OperatorName.SET_TEXT_LEADING);
    }

    /**
     * Move to the start of the next line of text. Requires the leading to have been set.
     *
     * @throws IOException If there is an error writing to the stream.
     */
    public void newLine() throws IOException
    {
        if (!inTextMode)
        {
            throw new IllegalStateException("Must call beginText() before newLine()");
        }
        writeOperator(OperatorName.NEXT_LINE);
    }

    /**
     * The Td operator. Move to the start of the next line, offset from the start of the current line by (tx, ty).
     *
     * @param tx The x translation.
     * @param ty The y translation.
     * @throws IOException If there is an error writing to the stream.
     * @throws IllegalStateException If the method was not allowed to be called at this time.
     */
    public void newLineAtOffset(float tx, float ty) throws IOException
    {
        if (!inTextMode)
        {
            throw new IllegalStateException(
                    "Error: must call beginText() before newLineAtOffset()");
        }
        writeOperand(tx);
        writeOperand(ty);
        writeOperator(OperatorName.MOVE_TEXT);
    }

    /**
     * The Tm operator. Sets the text matrix to the given values. A current text matrix will be replaced with the new
     * one.
     *
     * @param matrix the transformation matrix
     * @throws IOException If there is an error writing to the stream.
     * @throws IllegalStateException If the method was not allowed to be called at this time.
     */
    public void setTextMatrix(Matrix matrix) throws IOException
    {
        if (!inTextMode)
        {
            throw new IllegalStateException("Error: must call beginText() before setTextMatrix");
        }
        writeAffineTransform(matrix.createAffineTransform());
        writeOperator(OperatorName.SET_MATRIX);
    }

    /**
     * Draw an image at the x,y coordinates, with the default size of the image.
     *
     * @param image The image to draw.
     * @param x The x-coordinate to draw the image.
     * @param y The y-coordinate to draw the image.
     *
     * @throws IOException If there is an error writing to the stream.
     */
    public void drawImage(PDImageXObject image, float x, float y) throws IOException
    {
        drawImage(image, x, y, image.getWidth(), image.getHeight());
    }

    public void drawImage(PDFormXObject image, float x, float y) throws IOException
    {
        drawImage(image, x, y, image.getBBox().getWidth(), image.getBBox().getHeight());
    }

    /**
     * Draw an image at the x,y coordinates, with the given size.
     *
     * @param image The image to draw.
     * @param x The x-coordinate to draw the image.
     * @param y The y-coordinate to draw the image.
     * @param width The width to draw the image.
     * @param height The height to draw the image.
     *
     * @throws IOException If there is an error writing to the stream.
     * @throws IllegalStateException If the method was called within a text block.
     */
    public void drawImage(PDImageXObject image, float x, float y, float width, float height)
            throws IOException
    {
        draw(image, new Matrix(new AffineTransform(width, 0, 0, height, x, y)), null);
    }

    public void drawImage(PDFormXObject image, float x, float y, float width, float height)
            throws IOException
    {
        draw(image, new Matrix(new AffineTransform(width, 0, 0, height, x, y)), null);
    }

    /**
     * Draw an image at the origin with the given transformation matrix.
     *
     * @param image The image to draw.
     * @param matrix The transformation matrix to apply to the image.
     * @param state the graphic state to use
     *
     * @throws IOException If there is an error writing to the stream.
     * @throws IllegalStateException If the method was called within a text block.
     */
    public void drawImage(PDImageXObject image, Matrix matrix, PDExtendedGraphicsState state)
            throws IOException
    {
        draw(image, matrix, state);
    }

    public void drawImage(PDFormXObject image, Matrix matrix, PDExtendedGraphicsState state)
            throws IOException
    {
        draw(image, matrix, state);
    }

    private void draw(PDXObject image, Matrix matrix, PDExtendedGraphicsState state)
            throws IOException
    {
        requireState(!inTextMode, "Cannot draw image within a text block.");
        saveGraphicsState();

        transform(matrix);
        if (nonNull(state))
        {
            setGraphicsStateParameters(state);
        }
        if (image instanceof PDImageXObject)
        {
            writeOperand(resources.add((PDImageXObject) image));
        }
        else if (image instanceof PDFormXObject)
        {
            writeOperand(resources.add((PDFormXObject) image));
        }
        else
        {
            throw new IllegalArgumentException("Unsupported xobject type");
        }

        writeOperator(OperatorName.DRAW_OBJECT);

        restoreGraphicsState();
    }

    /**
     * Draw an inline image at the x,y coordinates, with the default size of the image.
     *
     * @param inlineImage The inline image to draw.
     * @param x The x-coordinate to draw the inline image.
     * @param y The y-coordinate to draw the inline image.
     *
     * @throws IOException If there is an error writing to the stream.
     */
    public void drawImage(PDInlineImage inlineImage, float x, float y) throws IOException
    {
        drawImage(inlineImage, x, y, inlineImage.getWidth(), inlineImage.getHeight());
    }

    /**
     * Draw an inline image at the x,y coordinates and a certain width and height.
     *
     * @param inlineImage The inline image to draw.
     * @param x The x-coordinate to draw the inline image.
     * @param y The y-coordinate to draw the inline image.
     * @param width The width of the inline image to draw.
     * @param height The height of the inline image to draw.
     *
     * @throws IOException If there is an error writing to the stream.
     * @throws IllegalStateException If the method was called within a text block.
     */
    public void drawImage(PDInlineImage inlineImage, float x, float y, float width, float height)
            throws IOException
    {
        if (inTextMode)
        {
            throw new IllegalStateException("Error: drawImage is not allowed within a text block.");
        }

        saveGraphicsState();
        transform(new Matrix(width, 0, 0, height, x, y));

        // create the image dictionary
        StringBuilder sb = new StringBuilder();
        sb.append(OperatorName.BEGIN_INLINE_IMAGE);

        sb.append("\n /W ");
        sb.append(inlineImage.getWidth());

        sb.append("\n /H ");
        sb.append(inlineImage.getHeight());

        sb.append("\n /CS ");
        sb.append("/");
        sb.append(inlineImage.getColorSpace().getName());

        if (inlineImage.getDecode() != null && inlineImage.getDecode().size() > 0)
        {
            sb.append("\n /D ");
            sb.append("[");
            for (COSBase base : inlineImage.getDecode())
            {
                sb.append(((COSNumber) base).intValue());
                sb.append(" ");
            }
            sb.append("]");
        }

        if (inlineImage.isStencil())
        {
            sb.append("\n /IM true");
        }

        sb.append("\n /BPC ");
        sb.append(inlineImage.getBitsPerComponent());

        // image dictionary
        write(sb.toString());
        this.writer.writeEOL();

        // binary data
        writeOperator(OperatorName.BEGIN_INLINE_IMAGE_DATA);
        writeBytes(inlineImage.getData());
        this.writer.writeEOL();
        writeOperator(OperatorName.END_INLINE_IMAGE);

        restoreGraphicsState();
    }

    /**
     * Draws the given Form XObject at the current location.
     *
     * @param form Form XObject
     * @throws IOException if the content stream could not be written
     * @throws IllegalStateException If the method was called within a text block.
     */
    public void drawForm(PDFormXObject form) throws IOException
    {
        if (inTextMode)
        {
            throw new IllegalStateException("Error: drawForm is not allowed within a text block.");
        }

        writeOperand(resources.add(form));
        writeOperator(OperatorName.DRAW_OBJECT);
    }

    /**
     * The cm operator. Concatenates the given matrix with the CTM.
     *
     * @param matrix the transformation matrix
     * @throws IOException If there is an error writing to the stream.
     */
    public void transform(Matrix matrix) throws IOException
    {
        if (inTextMode)
        {
            LOG.warn(
                    "Modifying the current transformation matrix is not allowed within text objects.");
        }
        writeAffineTransform(matrix.createAffineTransform());
        writeOperator(OperatorName.CONCAT);
    }

    /**
     * q operator. Saves the current graphics state.
     * 
     * @throws IOException If an error occurs while writing to the stream.
     */
    public void saveGraphicsState() throws IOException
    {
        if (inTextMode)
        {
            LOG.warn("Saving the graphics state is not allowed within text objects.");
        }
        if (!fontStack.isEmpty())
        {
            fontStack.push(fontStack.peek());
        }
        if (!strokingColorSpaceStack.isEmpty())
        {
            strokingColorSpaceStack.push(strokingColorSpaceStack.peek());
        }
        if (!nonStrokingColorSpaceStack.isEmpty())
        {
            nonStrokingColorSpaceStack.push(nonStrokingColorSpaceStack.peek());
        }
        writeOperator(OperatorName.SAVE);
    }

    /**
     * Q operator. Restores the current graphics state.
     * 
     * @throws IOException If an error occurs while writing to the stream.
     */
    public void restoreGraphicsState() throws IOException
    {
        if (inTextMode)
        {
            LOG.warn("Restoring the graphics state is not allowed within text objects.");
        }
        if (!fontStack.isEmpty())
        {
            fontStack.pop();
        }
        if (!strokingColorSpaceStack.isEmpty())
        {
            strokingColorSpaceStack.pop();
        }
        if (!nonStrokingColorSpaceStack.isEmpty())
        {
            nonStrokingColorSpaceStack.pop();
        }
        writeOperator(OperatorName.RESTORE);
    }

    private COSName getName(PDColorSpace colorSpace)
    {
        if (colorSpace instanceof PDDeviceGray || colorSpace instanceof PDDeviceRGB
                || colorSpace instanceof PDDeviceCMYK)
        {
            return COSName.getPDFName(colorSpace.getName());
        }
        return resources.add(colorSpace);
    }

    public void setTextRenderingMode(RenderingMode renderingMode) throws IOException
    {
        writeOperand(renderingMode.intValue());
        writeOperator(OperatorName.SET_TEXT_RENDERINGMODE);
    }

    /**
     * Sets the stroking color and, if necessary, the stroking color space.
     *
     * @param color Color in a specific color space.
     * @throws IOException If an IO error occurs while writing to the stream.
     */
    public void setStrokingColor(PDColor color) throws IOException
    {
        if (strokingColorSpaceStack.isEmpty()
                || strokingColorSpaceStack.peek() != color.getColorSpace()
                        && color.getColorSpace() != null)
        {
            writeOperand(getName(color.getColorSpace()));
            writeOperator(OperatorName.STROKING_COLORSPACE);
            setStrokingColorSpaceStack(color.getColorSpace());
        }

        for (float value : color.getComponents())
        {
            writeOperand(value);
        }

        if (color.getColorSpace() instanceof PDPattern)
        {
            writeOperand(color.getPatternName());
        }

        if (color.getColorSpace() instanceof PDPattern
                || color.getColorSpace() instanceof PDSeparation
                || color.getColorSpace() instanceof PDDeviceN
                || color.getColorSpace() instanceof PDICCBased)
        {
            writeOperator(OperatorName.STROKING_COLOR_N);
        }
        else
        {
            writeOperator(OperatorName.STROKING_COLOR);
        }
    }

    /**
     * Set the stroking color using an AWT color. Conversion uses the default sRGB color space.
     *
     * @param color The color to set.
     * @throws IOException If an IO error occurs while writing to the stream.
     */
    public void setStrokingColor(Color color) throws IOException
    {
        float[] components = new float[] { color.getRed() / 255f, color.getGreen() / 255f,
                color.getBlue() / 255f };
        PDColor pdColor = new PDColor(components, PDDeviceRGB.INSTANCE);
        setStrokingColor(pdColor);
    }

    /**
     * Set the stroking color in the DeviceRGB color space. Range is 0..1.
     *
     * @param r The red value
     * @param g The green value.
     * @param b The blue value.
     * @throws IOException If an IO error occurs while writing to the stream.
     * @throws IllegalArgumentException If the parameters are invalid.
     */
    public void setStrokingColor(float r, float g, float b) throws IOException
    {
        if (isOutsideOneInterval(r) || isOutsideOneInterval(g) || isOutsideOneInterval(b))
        {
            throw new IllegalArgumentException("Parameters must be within 0..1, but are "
                    + String.format("(%.2f,%.2f,%.2f)", r, g, b));
        }
        writeOperand(r);
        writeOperand(g);
        writeOperand(b);
        writeOperator(OperatorName.STROKING_COLOR_RGB);
        setStrokingColorSpaceStack(PDDeviceRGB.INSTANCE);
    }

    /**
     * Set the stroking color in the DeviceRGB color space. Range is 0..255.
     *
     * @param r The red value
     * @param g The green value.
     * @param b The blue value.
     * @throws IOException If an IO error occurs while writing to the stream.
     * @throws IllegalArgumentException If the parameters are invalid.
     * @deprecated use {@link #setStrokingColor(float, float, float) setStrokingColor(r/255f, g/255f, b/255f)}
     */
    @Deprecated
    public void setStrokingColor(int r, int g, int b) throws IOException
    {
        if (isOutside255Interval(r) || isOutside255Interval(g) || isOutside255Interval(b))
        {
            throw new IllegalArgumentException("Parameters must be within 0..255, but are "
                    + String.format("(%d,%d,%d)", r, g, b));
        }
        setStrokingColor(r / 255f, g / 255f, b / 255f);
    }

    /**
     * Set the stroking color in the DeviceCMYK color space. Range is 0..1
     *
     * @param c The cyan value.
     * @param m The magenta value.
     * @param y The yellow value.
     * @param k The black value.
     * @throws IOException If an IO error occurs while writing to the stream.
     * @throws IllegalArgumentException If the parameters are invalid.
     */
    public void setStrokingColor(float c, float m, float y, float k) throws IOException
    {
        if (isOutsideOneInterval(c) || isOutsideOneInterval(m) || isOutsideOneInterval(y)
                || isOutsideOneInterval(k))
        {
            throw new IllegalArgumentException("Parameters must be within 0..1, but are "
                    + String.format("(%.2f,%.2f,%.2f,%.2f)", c, m, y, k));
        }
        writeOperand(c);
        writeOperand(m);
        writeOperand(y);
        writeOperand(k);
        writeOperator(OperatorName.STROKING_COLOR_CMYK);
        setStrokingColorSpaceStack(PDDeviceCMYK.INSTANCE);
    }

    /**
     * Set the stroking color in the DeviceGray color space. Range is 0..1.
     *
     * @param g The gray value.
     * @throws IOException If an IO error occurs while writing to the stream.
     * @throws IllegalArgumentException If the parameter is invalid.
     */
    public void setStrokingColor(float g) throws IOException
    {
        if (isOutsideOneInterval(g))
        {
            throw new IllegalArgumentException("Parameter must be within 0..1, but is " + g);
        }
        writeOperand(g);
        writeOperator(OperatorName.STROKING_COLOR_GRAY);
        setStrokingColorSpaceStack(PDDeviceGray.INSTANCE);
    }

    /**
     * Set the stroking color in the DeviceGray color space. Range is 0..255.
     *
     * @param g The gray value.
     * @throws IOException If an IO error occurs while writing to the stream.
     * @throws IllegalArgumentException If the parameter is invalid.
     * @deprecated Use {@link #setStrokingColor(float) setStrokingColor(g/255f)} instead.
     */
    @Deprecated
    public void setStrokingColor(int g) throws IOException
    {
        if (isOutside255Interval(g))
        {
            throw new IllegalArgumentException("Parameter must be within 0..255, but is " + g);
        }
        setStrokingColor(g / 255f);
    }

    /**
     * Sets the non-stroking color and, if necessary, the non-stroking color space.
     *
     * @param color Color in a specific color space.
     * @throws IOException If an IO error occurs while writing to the stream.
     */
    public void setNonStrokingColor(PDColor color) throws IOException
    {
        if (nonStrokingColorSpaceStack.isEmpty()
                || nonStrokingColorSpaceStack.peek() != color.getColorSpace())
        {
            writeOperand(getName(color.getColorSpace()));
            writeOperator(OperatorName.NON_STROKING_COLORSPACE);
            setNonStrokingColorSpaceStack(color.getColorSpace());
        }

        for (float value : color.getComponents())
        {
            writeOperand(value);
        }

        if (color.getColorSpace() instanceof PDPattern)
        {
            writeOperand(color.getPatternName());
        }

        if (color.getColorSpace() instanceof PDPattern
                || color.getColorSpace() instanceof PDSeparation
                || color.getColorSpace() instanceof PDDeviceN
                || color.getColorSpace() instanceof PDICCBased)
        {
            writeOperator(OperatorName.NON_STROKING_COLOR_N);
        }
        else
        {
            writeOperator(OperatorName.NON_STROKING_COLOR);
        }
    }

    /**
     * Set the non-stroking color using an AWT color. Conversion uses the default sRGB color space.
     *
     * @param color The color to set.
     * @throws IOException If an IO error occurs while writing to the stream.
     */
    public void setNonStrokingColor(Color color) throws IOException
    {
        float[] components = new float[] { color.getRed() / 255f, color.getGreen() / 255f,
                color.getBlue() / 255f };
        PDColor pdColor = new PDColor(components, PDDeviceRGB.INSTANCE);
        setNonStrokingColor(pdColor);
    }

    /**
     * Set the non-stroking color in the DeviceRGB color space. Range is 0..255.
     *
     * @param r The red value.
     * @param g The green value.
     * @param b The blue value.
     * @throws IOException If an IO error occurs while writing to the stream.
     * @throws IllegalArgumentException If the parameters are invalid.
     */
    public void setNonStrokingColor(float r, float g, float b) throws IOException
    {
        if (isOutsideOneInterval(r) || isOutsideOneInterval(g) || isOutsideOneInterval(b))
        {
            throw new IllegalArgumentException("Parameters must be within 0..1, but are "
                    + String.format("(%.2f,%.2f,%.2f)", r, g, b));
        }
        writeOperand(r);
        writeOperand(g);
        writeOperand(b);
        writeOperator(OperatorName.NON_STROKING_RGB);
        setNonStrokingColorSpaceStack(PDDeviceRGB.INSTANCE);
    }

    /**
     * Set the non stroking color in the DeviceRGB color space. Range is 0..255.
     *
     * @param r The red value
     * @param g The green value.
     * @param b The blue value.
     * @throws IOException If an IO error occurs while writing to the stream.
     * @throws IllegalArgumentException If the parameters are invalid.
     * @deprecated use {@link #setNonStrokingColor(float, float, float) setNonStrokingColor(r/255f, g/255f, b/255f)}
     */
    @Deprecated
    public void setNonStrokingColor(int r, int g, int b) throws IOException
    {
        if (isOutside255Interval(r) || isOutside255Interval(g) || isOutside255Interval(b))
        {
            throw new IllegalArgumentException("Parameters must be within 0..255, but are "
                    + String.format("(%d,%d,%d)", r, g, b));
        }
        setNonStrokingColor(r / 255f, g / 255f, b / 255f);
    }

    /**
     * Set the non-stroking color in the DeviceCMYK color space. Range is 0..255.
     *
     * @param c The cyan value.
     * @param m The magenta value.
     * @param y The yellow value.
     * @param k The black value.
     * @throws IOException If an IO error occurs while writing to the stream.
     * @throws IllegalArgumentException If the parameters are invalid.
     */
    public void setNonStrokingColor(int c, int m, int y, int k) throws IOException
    {
        if (isOutside255Interval(c) || isOutside255Interval(m) || isOutside255Interval(y)
                || isOutside255Interval(k))
        {
            throw new IllegalArgumentException("Parameters must be within 0..255, but are "
                    + String.format("(%d,%d,%d,%d)", c, m, y, k));
        }
        setNonStrokingColor(c / 255f, m / 255f, y / 255f, k / 255f);
    }

    /**
     * Set the non-stroking color in the DeviceRGB color space. Range is 0..1.
     *
     * @param c The cyan value.
     * @param m The magenta value.
     * @param y The yellow value.
     * @param k The black value.
     * @throws IOException If an IO error occurs while writing to the stream.
     */
    public void setNonStrokingColor(float c, float m, float y, float k) throws IOException
    {
        if (isOutsideOneInterval(c) || isOutsideOneInterval(m) || isOutsideOneInterval(y)
                || isOutsideOneInterval(k))
        {
            throw new IllegalArgumentException("Parameters must be within 0..1, but are "
                    + String.format("(%.2f,%.2f,%.2f,%.2f)", c, m, y, k));
        }
        writeOperand(c);
        writeOperand(m);
        writeOperand(y);
        writeOperand(k);
        writeOperator(OperatorName.NON_STROKING_CMYK);
        setNonStrokingColorSpaceStack(PDDeviceCMYK.INSTANCE);
    }

    /**
     * Set the non-stroking color in the DeviceGray color space. Range is 0..255.
     *
     * @param g The gray value.
     * @throws IOException If an IO error occurs while writing to the stream.
     * @throws IllegalArgumentException If the parameter is invalid.
     */
    public void setNonStrokingColor(int g) throws IOException
    {
        if (isOutside255Interval(g))
        {
            throw new IllegalArgumentException("Parameter must be within 0..255, but is " + g);
        }
        setNonStrokingColor(g / 255f);
    }

    /**
     * Set the non-stroking color in the DeviceGray color space. Range is 0..1.
     *
     * @param g The gray value.
     * @throws IOException If an IO error occurs while writing to the stream.
     * @throws IllegalArgumentException If the parameter is invalid.
     */
    public void setNonStrokingColor(double g) throws IOException
    {
        if (isOutsideOneInterval(g))
        {
            throw new IllegalArgumentException("Parameter must be within 0..1, but is " + g);
        }
        writeOperand((float) g);
        writeOperator(OperatorName.NON_STROKING_GRAY);
        setNonStrokingColorSpaceStack(PDDeviceGray.INSTANCE);
    }

    /**
     * Add a rectangle to the current path.
     *
     * @param x The lower left x coordinate.
     * @param y The lower left y coordinate.
     * @param width The width of the rectangle.
     * @param height The height of the rectangle.
     * @throws IOException If the content stream could not be written.
     * @throws IllegalStateException If the method was called within a text block.
     */
    public void addRect(float x, float y, float width, float height) throws IOException
    {
        if (inTextMode)
        {
            throw new IllegalStateException("Error: addRect is not allowed within a text block.");
        }
        writeOperand(x);
        writeOperand(y);
        writeOperand(width);
        writeOperand(height);
        writeOperator(OperatorName.APPEND_RECT);
    }

    /**
     * Append a cubic Bézier curve to the current path. The curve extends from the current point to the point (x3, y3),
     * using (x1, y1) and (x2, y2) as the Bézier control points.
     *
     * @param x1 x coordinate of the point 1
     * @param y1 y coordinate of the point 1
     * @param x2 x coordinate of the point 2
     * @param y2 y coordinate of the point 2
     * @param x3 x coordinate of the point 3
     * @param y3 y coordinate of the point 3
     * @throws IOException If the content stream could not be written.
     * @throws IllegalStateException If the method was called within a text block.
     */
    public void curveTo(float x1, float y1, float x2, float y2, float x3, float y3)
            throws IOException
    {
        if (inTextMode)
        {
            throw new IllegalStateException("Error: curveTo is not allowed within a text block.");
        }
        writeOperand(x1);
        writeOperand(y1);
        writeOperand(x2);
        writeOperand(y2);
        writeOperand(x3);
        writeOperand(y3);
        writeOperator(OperatorName.CURVE_TO);
    }

    /**
     * Append a cubic Bézier curve to the current path. The curve extends from the current point to the point (x3, y3),
     * using the current point and (x2, y2) as the Bézier control points.
     *
     * @param x2 x coordinate of the point 2
     * @param y2 y coordinate of the point 2
     * @param x3 x coordinate of the point 3
     * @param y3 y coordinate of the point 3
     * @throws IllegalStateException If the method was called within a text block.
     * @throws IOException If the content stream could not be written.
     */
    public void curveTo2(float x2, float y2, float x3, float y3) throws IOException
    {
        if (inTextMode)
        {
            throw new IllegalStateException("Error: curveTo2 is not allowed within a text block.");
        }
        writeOperand(x2);
        writeOperand(y2);
        writeOperand(x3);
        writeOperand(y3);
        writeOperator(OperatorName.CURVE_TO_REPLICATE_INITIAL_POINT);
    }

    /**
     * Append a cubic Bézier curve to the current path. The curve extends from the current point to the point (x3, y3),
     * using (x1, y1) and (x3, y3) as the Bézier control points.
     *
     * @param x1 x coordinate of the point 1
     * @param y1 y coordinate of the point 1
     * @param x3 x coordinate of the point 3
     * @param y3 y coordinate of the point 3
     * @throws IOException If the content stream could not be written.
     * @throws IllegalStateException If the method was called within a text block.
     */
    public void curveTo1(float x1, float y1, float x3, float y3) throws IOException
    {
        if (inTextMode)
        {
            throw new IllegalStateException("Error: curveTo1 is not allowed within a text block.");
        }
        writeOperand(x1);
        writeOperand(y1);
        writeOperand(x3);
        writeOperand(y3);
        writeOperator(OperatorName.CURVE_TO_REPLICATE_FINAL_POINT);
    }

    /**
     * Move the current position to the given coordinates.
     *
     * @param x The x coordinate.
     * @param y The y coordinate.
     * @throws IOException If the content stream could not be written.
     * @throws IllegalStateException If the method was called within a text block.
     */
    public void moveTo(float x, float y) throws IOException
    {
        if (inTextMode)
        {
            throw new IllegalStateException("Error: moveTo is not allowed within a text block.");
        }
        writeOperand(x);
        writeOperand(y);
        writeOperator(OperatorName.MOVE_TO);
    }

    /**
     * Draw a line from the current position to the given coordinates.
     *
     * @param x The x coordinate.
     * @param y The y coordinate.
     * @throws IOException If the content stream could not be written.
     * @throws IllegalStateException If the method was called within a text block.
     */
    public void lineTo(float x, float y) throws IOException
    {
        if (inTextMode)
        {
            throw new IllegalStateException("Error: lineTo is not allowed within a text block.");
        }
        writeOperand(x);
        writeOperand(y);
        writeOperator(OperatorName.LINE_TO);
    }

    /**
     * Stroke the path.
     * 
     * @throws IOException If the content stream could not be written
     * @throws IllegalStateException If the method was called within a text block.
     */
    public void stroke() throws IOException
    {
        if (inTextMode)
        {
            throw new IllegalStateException("Error: stroke is not allowed within a text block.");
        }
        writeOperator(OperatorName.STROKE_PATH);
    }

    /**
     * Close and stroke the path.
     * 
     * @throws IOException If the content stream could not be written
     * @throws IllegalStateException If the method was called within a text block.
     */
    public void closeAndStroke() throws IOException
    {
        if (inTextMode)
        {
            throw new IllegalStateException(
                    "Error: closeAndStroke is not allowed within a text block.");
        }
        writeOperator(OperatorName.CLOSE_AND_STROKE);
    }

    /**
     * Fills the path using the nonzero winding rule.
     *
     * @throws IOException If the content stream could not be written
     * @throws IllegalStateException If the method was called within a text block.
     */
    public void fill() throws IOException
    {
        if (inTextMode)
        {
            throw new IllegalStateException("Error: fill is not allowed within a text block.");
        }
        writeOperator(OperatorName.FILL_NON_ZERO);
    }

    /**
     * Fills the path using the even-odd winding number rule.
     *
     * @throws IOException If the content stream could not be written
     * @throws IllegalStateException If the method was called within a text block.
     */
    public void fillEvenOdd() throws IOException
    {
        if (inTextMode)
        {
            throw new IllegalStateException(
                    "Error: fillEvenOdd is not allowed within a text block.");
        }
        writeOperator(OperatorName.FILL_NON_ZERO);
    }

    /**
     * Fill and then stroke the path, using the nonzero winding number rule to determine the region to fill. This shall
     * produce the same result as constructing two identical path objects, painting the first with {@link #fill() } and
     * the second with {@link #stroke() }.
     *
     * @throws IOException If the content stream could not be written
     * @throws IllegalStateException If the method was called within a text block.
     */
    public void fillAndStroke() throws IOException
    {
        if (inTextMode)
        {
            throw new IllegalStateException(
                    "Error: fillAndStroke is not allowed within a text block.");
        }
        writeOperator("B");
    }

    /**
     * Fill and then stroke the path, using the even-odd rule to determine the region to fill. This shall produce the
     * same result as constructing two identical path objects, painting the first with {@link #fillEvenOdd() } and the
     * second with {@link #stroke() }.
     *
     * @throws IOException If the content stream could not be written
     * @throws IllegalStateException If the method was called within a text block.
     */
    public void fillAndStrokeEvenOdd() throws IOException
    {
        if (inTextMode)
        {
            throw new IllegalStateException(
                    "Error: fillAndStrokeEvenOdd is not allowed within a text block.");
        }
        writeOperator(OperatorName.FILL_NON_ZERO_AND_STROKE);
    }

    /**
     * Close, fill, and then stroke the path, using the nonzero winding number rule to determine the region to fill.
     * This shall have the same effect as the sequence {@link #closePath() } and then {@link #fillAndStroke() }.
     *
     * @throws IOException If the content stream could not be written
     * @throws IllegalStateException If the method was called within a text block.
     */
    public void closeAndFillAndStroke() throws IOException
    {
        if (inTextMode)
        {
            throw new IllegalStateException(
                    "Error: closeAndFillAndStroke is not allowed within a text block.");
        }
        writeOperator(OperatorName.CLOSE_FILL_NON_ZERO_AND_STROKE);
    }

    /**
     * Close, fill, and then stroke the path, using the even-odd rule to determine the region to fill. This shall have
     * the same effect as the sequence {@link #closePath() } and then {@link #fillAndStrokeEvenOdd() }.
     *
     * @throws IOException If the content stream could not be written
     * @throws IllegalStateException If the method was called within a text block.
     */
    public void closeAndFillAndStrokeEvenOdd() throws IOException
    {
        if (inTextMode)
        {
            throw new IllegalStateException(
                    "Error: closeAndFillAndStrokeEvenOdd is not allowed within a text block.");
        }
        writeOperator(OperatorName.CLOSE_FILL_EVEN_ODD_AND_STROKE);
    }

    /**
     * Fills the clipping area with the given shading.
     *
     * @param shading Shading resource
     * @throws IOException If the content stream could not be written
     * @throws IllegalStateException If the method was called within a text block.
     */
    public void shadingFill(PDShading shading) throws IOException
    {
        if (inTextMode)
        {
            throw new IllegalStateException(
                    "Error: shadingFill is not allowed within a text block.");
        }

        writeOperand(resources.add(shading));
        writeOperator(OperatorName.SHADING_FILL);
    }

    /**
     * Closes the current subpath.
     *
     * @throws IOException If the content stream could not be written
     * @throws IllegalStateException If the method was called within a text block.
     */
    public void closePath() throws IOException
    {
        if (inTextMode)
        {
            throw new IllegalStateException("Error: closePath is not allowed within a text block.");
        }
        writeOperator(OperatorName.CLOSE_PATH);
    }

    /**
     * Intersects the current clipping path with the current path, using the nonzero rule.
     *
     * @throws IOException If the content stream could not be written
     * @throws IllegalStateException If the method was called within a text block.
     */
    public void clip() throws IOException
    {
        if (inTextMode)
        {
            throw new IllegalStateException("Error: clip is not allowed within a text block.");
        }
        writeOperator(OperatorName.CLIP_NON_ZERO);

        // end path without filling or stroking
        writeOperator(OperatorName.ENDPATH);
    }

    /**
     * Intersects the current clipping path with the current path, using the even-odd rule.
     *
     * @throws IOException If the content stream could not be written
     * @throws IllegalStateException If the method was called within a text block.
     */
    public void clipEvenOdd() throws IOException
    {
        if (inTextMode)
        {
            throw new IllegalStateException(
                    "Error: clipEvenOdd is not allowed within a text block.");
        }
        writeOperator(OperatorName.CLIP_EVEN_ODD);

        // end path without filling or stroking
        writeOperator(OperatorName.ENDPATH);
    }

    /**
     * Set line width to the given value.
     *
     * @param lineWidth The width which is used for drwaing.
     * @throws IOException If the content stream could not be written
     * @throws IllegalStateException If the method was called within a text block.
     */
    public void setLineWidth(float lineWidth) throws IOException
    {
        if (inTextMode)
        {
            throw new IllegalStateException(
                    "Error: setLineWidth is not allowed within a text block.");
        }
        writeOperand(lineWidth);
        writeOperator(OperatorName.SET_LINE_WIDTH);
    }

    /**
     * Set the line join style.
     *
     * @param lineJoinStyle 0 for miter join, 1 for round join, and 2 for bevel join.
     * @throws IOException If the content stream could not be written.
     * @throws IllegalStateException If the method was called within a text block.
     * @throws IllegalArgumentException If the parameter is not a valid line join style.
     */
    public void setLineJoinStyle(int lineJoinStyle) throws IOException
    {
        if (inTextMode)
        {
            throw new IllegalStateException(
                    "Error: setLineJoinStyle is not allowed within a text block.");
        }
        if (lineJoinStyle >= 0 && lineJoinStyle <= 2)
        {
            writeOperand(lineJoinStyle);
            writeOperator(OperatorName.SET_LINE_JOINSTYLE);
        }
        else
        {
            throw new IllegalArgumentException("Error: unknown value for line join style");
        }
    }

    /**
     * Set the line cap style.
     *
     * @param lineCapStyle 0 for butt cap, 1 for round cap, and 2 for projecting square cap.
     * @throws IOException If the content stream could not be written.
     * @throws IllegalStateException If the method was called within a text block.
     * @throws IllegalArgumentException If the parameter is not a valid line cap style.
     */
    public void setLineCapStyle(int lineCapStyle) throws IOException
    {
        if (inTextMode)
        {
            throw new IllegalStateException(
                    "Error: setLineCapStyle is not allowed within a text block.");
        }
        if (lineCapStyle >= 0 && lineCapStyle <= 2)
        {
            writeOperand(lineCapStyle);
            writeOperator(OperatorName.SET_LINE_CAPSTYLE);
        }
        else
        {
            throw new IllegalArgumentException("Error: unknown value for line cap style");
        }
    }

    /**
     * Set the line dash pattern.
     *
     * @param pattern The pattern array
     * @param phase The phase of the pattern
     * @throws IOException If the content stream could not be written.
     * @throws IllegalStateException If the method was called within a text block.
     */
    public void setLineDashPattern(float[] pattern, float phase) throws IOException
    {
        if (inTextMode)
        {
            throw new IllegalStateException(
                    "Error: setLineDashPattern is not allowed within a text block.");
        }
        write("[");
        for (float value : pattern)
        {
            writeOperand(value);
        }
        write("] ");
        writeOperand(phase);
        writeOperator(OperatorName.SET_LINE_DASHPATTERN);
    }

    /**
     * Set the miter limit.
     *
     * @param miterLimit the new miter limit.
     * @throws IOException If the content stream could not be written.
     */
    public void setMiterLimit(float miterLimit) throws IOException
    {
        requireState(!inTextMode, "setMiterLimit is not allowed within a text block");
        requireState(miterLimit > 0,
                "A miter limit <= 0 is invalid and will not render in Acrobat Reader");
        writeOperand(miterLimit);
        writeOperator(OperatorName.SET_LINE_MITERLIMIT);
    }

    /**
     * Begin a marked content sequence.
     *
     * @param tag the tag
     * @throws IOException If the content stream could not be written
     */
    public void beginMarkedContent(COSName tag) throws IOException
    {
        writeOperand(tag);
        writeOperator(OperatorName.BEGIN_MARKED_CONTENT);
    }

    /**
     * Begin a marked content sequence with a reference to an entry in the page resources' Properties dictionary.
     *
     * @param tag the tag
     * @param propertyList property list
     * @throws IOException If the content stream could not be written
     */
    public void beginMarkedContent(COSName tag, PDPropertyList propertyList) throws IOException
    {
        writeOperand(tag);
        writeOperand(resources.add(propertyList));
        writeOperator(OperatorName.BEGIN_MARKED_CONTENT_SEQ);
    }

    /**
     * End a marked content sequence.
     *
     * @throws IOException If the content stream could not be written
     */
    public void endMarkedContent() throws IOException
    {
        writeOperator(OperatorName.END_MARKED_CONTENT);
    }

    /**
     * Set an extended graphics state.
     * 
     * @param state The extended graphics state.
     * @throws IOException If the content stream could not be written.
     */
    public void setGraphicsStateParameters(PDExtendedGraphicsState state) throws IOException
    {
        writeOperand(resources.add(state));
        writeOperator(OperatorName.SET_GRAPHICS_STATE_PARAMS);
    }

    /**
     * Write a comment line.
     *
     * @param comment
     * @throws IOException If the content stream could not be written.
     * @throws IllegalArgumentException If the comment contains a newline. This is not allowed, because the next line
     * could be ordinary PDF content.
     */
    public void addComment(String comment) throws IOException
    {
        if (comment.indexOf('\n') >= 0 || comment.indexOf('\r') >= 0)
        {
            throw new IllegalArgumentException("comment should not include a newline");
        }
        writer.writeComment(comment);
    }

    protected void writeOperand(float real) throws IOException
    {
        // PDFbox up to 1st Aug has a new fast or fallback way to write things here, which breaks things
        // temporary reverted to old way of doing things, see if new commits fix this
        write(formatDecimal.format(real));
        writer.writeSpace();
    }

    private void writeOperand(int integer) throws IOException
    {
        write(formatDecimal.format(integer));
        writer.writeSpace();
    }

    /**
     * Writes a COSName to the content stream.
     */
    private void writeOperand(COSName name) throws IOException
    {
        name.accept(writer);
        writer.writeSpace();
    }

    /**
     * Writes a string to the content stream as ASCII.
     */
    private void writeOperator(String text) throws IOException
    {
        write(text);
        writer.writeEOL();
    }

    /**
     * Writes a string to the content stream as ASCII.
     */
    private void write(String text) throws IOException
    {
        writer.writeContent(text.getBytes(StandardCharsets.US_ASCII));
    }

    /**
     * Writes binary data to the content stream.
     */
    private void writeBytes(byte[] data) throws IOException
    {
        writer.writeContent(data);
    }

    /**
     * Writes an AffineTransform to the content stream as an array.
     */
    private void writeAffineTransform(AffineTransform transform) throws IOException
    {
        double[] values = new double[6];
        transform.getMatrix(values);
        for (double v : values)
        {
            writeOperand((float) v);
        }
    }

    /**
     * Close the content stream. This must be called when you are done with this object.
     *
     * @throws IOException If the underlying stream has a problem being written to.
     */
    @Override
    public void close() throws IOException
    {
        if (inTextMode)
        {
            LOG.warn("You did not call endText(), some viewers won't display your text");
        }
        IOUtils.close(writer);
    }

    private static boolean isOutside255Interval(int val)
    {
        return val < 0 || val > 255;
    }

    private static boolean isOutsideOneInterval(double val)
    {
        return val < 0 || val > 1;
    }

    private void setStrokingColorSpaceStack(PDColorSpace colorSpace)
    {
        if (strokingColorSpaceStack.isEmpty())
        {
            strokingColorSpaceStack.add(colorSpace);
        }
        else
        {
            strokingColorSpaceStack.setElementAt(colorSpace, strokingColorSpaceStack.size() - 1);
        }
    }

    private void setNonStrokingColorSpaceStack(PDColorSpace colorSpace)
    {
        if (nonStrokingColorSpaceStack.isEmpty())
        {
            nonStrokingColorSpaceStack.add(colorSpace);
        }
        else
        {
            nonStrokingColorSpaceStack.setElementAt(colorSpace,
                    nonStrokingColorSpaceStack.size() - 1);
        }
    }

    /**
     * Set the text rendering mode. This determines whether showing text shall cause glyph outlines to be stroked,
     * filled, used as a clipping boundary, or some combination of the three.
     *
     * @param rm The text rendering mode.
     * @throws IOException If the content stream could not be written.
     */
    public void setRenderingMode(RenderingMode rm) throws IOException
    {
        writeOperand(rm.intValue());
        writeOperator(OperatorName.SET_TEXT_RENDERINGMODE);
    }

    /**
     * Set the character spacing. The value shall be added to the horizontal or vertical component of the glyph's
     * displacement, depending on the writing mode.
     *
     * @param spacing character spacing
     * @throws IOException If the content stream could not be written.
     */
    public void setCharacterSpacing(float spacing) throws IOException
    {
        writeOperand(spacing);
        writeOperator(OperatorName.SET_CHAR_SPACING);
    }

    /**
     * Set the word spacing. The value shall be added to the horizontal or vertical component of the ASCII SPACE
     * character, depending on the writing mode.
     *
     * @param spacing word spacing
     * @throws IOException If the content stream could not be written.
     */
    public void setWordSpacing(float spacing) throws IOException
    {
        writeOperand(spacing);
        writeOperator(OperatorName.SET_WORD_SPACING);
    }

    /**
     * Set the horizontal scaling to scale / 100.
     *
     * @param scale number specifying the percentage of the normal width. Default value: 100 (normal width).
     * @throws IOException If the content stream could not be written.
     */
    public void setHorizontalScaling(float scale) throws IOException
    {
        writeOperand(scale);
        writeOperator(OperatorName.SET_TEXT_HORIZONTAL_SCALING);
    }

    /**
     * Set the text rise value, i.e. move the baseline up or down. This is useful for drawing superscripts or
     * subscripts.
     *
     * @param rise Specifies the distance, in unscaled text space units, to move the baseline up or down from its
     * default location. 0 restores the default location.
     * @throws IOException
     */
    public void setTextRise(float rise) throws IOException
    {
        writeOperand(rise);
        writeOperator(OperatorName.SET_TEXT_RISE);
    }
}
