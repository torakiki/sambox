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
package org.sejda.sambox.contentstream;

import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;

import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sejda.sambox.contentstream.operator.MissingOperandException;
import org.sejda.sambox.contentstream.operator.Operator;
import org.sejda.sambox.contentstream.operator.OperatorProcessor;
import org.sejda.sambox.contentstream.operator.state.EmptyGraphicsStackException;
import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSNumber;
import org.sejda.sambox.cos.COSString;
import org.sejda.sambox.filter.MissingImageReaderException;
import org.sejda.sambox.input.ContentStreamParser;
import org.sejda.sambox.pdmodel.MissingResourceException;
import org.sejda.sambox.pdmodel.PDPage;
import org.sejda.sambox.pdmodel.PDResources;
import org.sejda.sambox.pdmodel.common.PDRectangle;
import org.sejda.sambox.pdmodel.font.PDFont;
import org.sejda.sambox.pdmodel.font.PDType1Font;
import org.sejda.sambox.pdmodel.font.PDType3CharProc;
import org.sejda.sambox.pdmodel.font.PDType3Font;
import org.sejda.sambox.pdmodel.graphics.PDLineDashPattern;
import org.sejda.sambox.pdmodel.graphics.blend.BlendMode;
import org.sejda.sambox.pdmodel.graphics.color.PDColor;
import org.sejda.sambox.pdmodel.graphics.color.PDColorSpace;
import org.sejda.sambox.pdmodel.graphics.color.PDDeviceGray;
import org.sejda.sambox.pdmodel.graphics.form.PDFormXObject;
import org.sejda.sambox.pdmodel.graphics.form.PDTransparencyGroup;
import org.sejda.sambox.pdmodel.graphics.pattern.PDTilingPattern;
import org.sejda.sambox.pdmodel.graphics.state.PDGraphicsState;
import org.sejda.sambox.pdmodel.graphics.state.PDTextState;
import org.sejda.sambox.pdmodel.interactive.annotation.PDAnnotation;
import org.sejda.sambox.pdmodel.interactive.annotation.PDAppearanceStream;
import org.sejda.sambox.util.Matrix;
import org.sejda.sambox.util.Vector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processes a PDF content stream and executes certain operations. Provides a callback interface for
 * clients that want to do things with the stream.
 *
 * @author Ben Litchfield
 */
public abstract class PDFStreamEngine
{
    private static final Logger LOG = LoggerFactory.getLogger(PDFStreamEngine.class);

    private final Map<String, OperatorProcessor> operators = new HashMap<>(80);

    private Deque<PDGraphicsState> graphicsStack = new ArrayDeque<>();

    private PDResources resources;
    private PDPage currentPage;
    private boolean isProcessingPage;
    private Matrix initialMatrix;

    // used to monitor potentially recursive operations.
    private int level = 0;

    /**
     * Creates a new PDFStreamEngine.
     */
    protected PDFStreamEngine()
    {
    }

    /**
     * Adds an operator processor to the engine.
     *
     * @param op operator processor
     */
    public final void addOperator(OperatorProcessor op)
    {
        op.setContext(this);
        operators.put(op.getName(), op);
    }

    /**
     * Adds an operator processor to the engine if there isn't an operator already associated with
     * the PDF operator.
     *
     * @param op operator processor
     * @return true if the operator is added, false if not (there's already an operator associated)
     */
    public final boolean addOperatorIfAbsent(OperatorProcessor op)
    {
        if (isNull(operators.putIfAbsent(op.getName(), op)))
        {
            op.setContext(this);
            return true;
        }
        return false;
    }

    /**
     * Initialises the stream engine for the given page.
     */
    private void initPage(PDPage page)
    {
        if (page == null)
        {
            throw new IllegalArgumentException("Page cannot be null");
        }
        currentPage = page;
        graphicsStack.clear();
        graphicsStack.push(new PDGraphicsState(page.getCropBox()));
        resources = null;
        initialMatrix = page.getMatrix();
    }

    /**
     * This will initialise and process the contents of the stream.
     *
     * @param page the page to process
     * @throws IOException if there is an error accessing the stream
     */
    public void processPage(PDPage page) throws IOException
    {
        initPage(page);
        if (page.hasContents())
        {
            isProcessingPage = true;
            processStream(page);
            isProcessingPage = false;
        }
    }

    /**
     * Shows a transparency group from the content stream.
     *
     * @param form transparency group (form) XObject
     * @throws IOException if the transparency group cannot be processed
     */
    public void showTransparencyGroup(PDTransparencyGroup form) throws IOException
    {
        processTransparencyGroup(form);
    }

    /**
     * Shows a form from the content stream.
     *
     * @param form form XObject
     * @throws IOException if the form cannot be processed
     */
    public void showForm(PDFormXObject form) throws IOException
    {
        if (currentPage == null)
        {
            throw new IllegalStateException("No current page, call "
                    + "#processChildStream(PDContentStream, PDPage) instead");
        }
        if (!form.getCOSObject().isEmpty())
        {
            processStream(form);
        }
    }

    /**
     * Processes a soft mask transparency group stream.
     *
     * @param group
     * @throws IOException
     */
    protected void processSoftMask(PDTransparencyGroup group) throws IOException
    {
        saveGraphicsState();
        Matrix softMaskCTM = getGraphicsState().getSoftMask().getInitialTransformationMatrix();
        getGraphicsState().setCurrentTransformationMatrix(softMaskCTM);
        getGraphicsState().setTextMatrix(new Matrix());
        getGraphicsState().setTextLineMatrix(new Matrix());
        getGraphicsState().setNonStrokingColorSpace(PDDeviceGray.INSTANCE);
        getGraphicsState().setNonStrokingColor(PDDeviceGray.INSTANCE.getInitialColor());
        getGraphicsState().setStrokingColorSpace(PDDeviceGray.INSTANCE);
        getGraphicsState().setStrokingColor(PDDeviceGray.INSTANCE.getInitialColor());
        try
        {
            processTransparencyGroup(group);
        }
        finally
        {
            restoreGraphicsState();
        }
    }

    /**
     * Processes a transparency group stream.
     *
     * @param group
     * @throws IOException
     */
    protected void processTransparencyGroup(PDTransparencyGroup group) throws IOException
    {
        if (currentPage == null)
        {
            throw new IllegalStateException("No current page, call "
                    + "#processChildStream(PDContentStream, PDPage) instead");
        }

        PDResources parent = pushResources(group);
        Deque<PDGraphicsState> savedStack = saveGraphicsStack();

        Matrix parentMatrix = initialMatrix;
        PDGraphicsState graphicsState = getGraphicsState();


        // the stream's initial matrix includes the parent CTM, e.g. this allows a scaled form
        initialMatrix = graphicsState.getCurrentTransformationMatrix().clone();

        // transform the CTM using the stream's matrix
        graphicsState.getCurrentTransformationMatrix().concatenate(group.getMatrix());

        // Before execution of the transparency group XObject’s content stream,
        // the current blend mode in the graphics state shall be initialized to Normal,
        // the current stroking and nonstroking alpha constants to 1.0, and the current soft mask to None.
        graphicsState.setBlendMode(BlendMode.NORMAL);
        graphicsState.setAlphaConstant(1);
        graphicsState.setNonStrokeAlphaConstants(1);
        graphicsState.setSoftMask(null);

        // clip to bounding box
        clipToRect(group.getBBox());

        try
        {
            processStreamOperators(group);
        }
        finally
        {
            initialMatrix = parentMatrix;

            restoreGraphicsStack(savedStack);
            popResources(parent);
        }
    }

    /**
     * Processes a Type 3 character stream.
     *
     * @param charProc            Type 3 character procedure
     * @param textRenderingMatrix the Text Rendering Matrix
     */
    protected void processType3Stream(PDType3CharProc charProc, Matrix textRenderingMatrix)
            throws IOException
    {
        if (currentPage == null)
        {
            throw new IllegalStateException("No current page, call "
                    + "#processChildStream(PDContentStream, PDPage) instead");
        }

        PDResources parent = pushResources(charProc);
        Deque<PDGraphicsState> savedStack = saveGraphicsStack();

        // replace the CTM with the TRM
        getGraphicsState().setCurrentTransformationMatrix(textRenderingMatrix);

        // transform the CTM using the stream's matrix (this is the FontMatrix)
        textRenderingMatrix.concatenate(charProc.getMatrix());

        // note: we don't clip to the BBox as it is often wrong, see PDFBOX-1917

        // save text matrices (Type 3 stream may contain BT/ET, see PDFBOX-2137)
        getGraphicsState().setTextMatrix(new Matrix());
        getGraphicsState().setTextLineMatrix(new Matrix());

        try
        {
            processStreamOperators(charProc);
        }
        finally
        {
            restoreGraphicsStack(savedStack);
            popResources(parent);
        }
    }

    /**
     * Process the given annotation with the specified appearance stream.
     *
     * @param annotation The annotation containing the appearance stream to process.
     * @param appearance The appearance stream to process.
     */
    protected void processAnnotation(PDAnnotation annotation, PDAppearanceStream appearance)
            throws IOException
    {
        PDRectangle bbox = appearance.getBBox();
        PDRectangle rect = annotation.getRectangle();

        // zero-sized rectangles are not valid
        if (rect != null && rect.getWidth() > 0 && rect.getHeight() > 0 && bbox != null
                && bbox.getWidth() > 0 && bbox.getHeight() > 0)
        {
            PDResources parent = pushResources(appearance);
            Deque<PDGraphicsState> savedStack = saveGraphicsStack();

            Matrix matrix = appearance.getMatrix();
            // transformed appearance box fixme: may be an arbitrary shape
            Rectangle2D transformedBox = bbox.transform(matrix).getBounds2D();

            // compute a matrix which scales and translates the transformed appearance box to align
            // with the edges of the annotation's rectangle
            Matrix a = Matrix.getTranslateInstance(rect.getLowerLeftX(), rect.getLowerLeftY());
            a.concatenate(
                    Matrix.getScaleInstance((float) (rect.getWidth() / transformedBox.getWidth()),
                            (float) (rect.getHeight() / transformedBox.getHeight())));
            a.concatenate(Matrix.getTranslateInstance((float) -transformedBox.getX(),
                    (float) -transformedBox.getY()));

            //
            // HOWEVER only the opposite order works for rotated pages with
            // filled fields / annotations that have a matrix in the appearance stream, see PDFBOX-3083
            Matrix aa = Matrix.concatenate(a, matrix);

            // make matrix AA the CTM
            getGraphicsState().setCurrentTransformationMatrix(aa);

            // clip to bounding box
            clipToRect(bbox);

            // needed for patterns in appearance streams, e.g. PDFBOX-2182
            initialMatrix = aa.clone();

            try
            {
                processStreamOperators(appearance);
            }
            finally
            {
                restoreGraphicsStack(savedStack);
                popResources(parent);
            }
        }
    }

    /**
     * Process the given tiling pattern.
     *
     * @param tilingPattern the tiling pattern
     * @param color         color to use, if this is an uncoloured pattern, otherwise null.
     * @param colorSpace    color space to use, if this is an uncoloured pattern, otherwise null.
     */
    public final void processTilingPattern(PDTilingPattern tilingPattern, PDColor color,
            PDColorSpace colorSpace) throws IOException
    {
        processTilingPattern(tilingPattern, color, colorSpace, tilingPattern.getMatrix());
    }

    /**
     * Process the given tiling pattern. Allows the pattern matrix to be overridden for custom
     * rendering.
     *
     * @param tilingPattern the tiling pattern
     * @param color         color to use, if this is an uncoloured pattern, otherwise null.
     * @param colorSpace    color space to use, if this is an uncoloured pattern, otherwise null.
     * @param patternMatrix the pattern matrix, may be overridden for custom rendering.
     */
    public final void processTilingPattern(PDTilingPattern tilingPattern, PDColor color,
            PDColorSpace colorSpace, Matrix patternMatrix) throws IOException
    {
        PDResources parent = pushResources(tilingPattern);

        Matrix parentMatrix = initialMatrix;
        initialMatrix = Matrix.concatenate(initialMatrix, patternMatrix);

        // save the original graphics state
        Deque<PDGraphicsState> savedStack = saveGraphicsStack();

        // save a clean state (new clipping path, line path, etc.)
        PDRectangle tilingBBox = tilingPattern.getBBox();
        Rectangle2D bbox = tilingBBox.transform(patternMatrix).getBounds2D();
        PDRectangle rect = new PDRectangle((float) bbox.getX(), (float) bbox.getY(),
                (float) bbox.getWidth(), (float) bbox.getHeight());
        graphicsStack.push(new PDGraphicsState(rect));
        PDGraphicsState graphicsState = getGraphicsState();

        // non-colored patterns have to be given a color
        if (colorSpace != null)
        {
            color = new PDColor(color.getComponents(), colorSpace);
            graphicsState.setNonStrokingColorSpace(colorSpace);
            graphicsState.setNonStrokingColor(color);
            graphicsState.setStrokingColorSpace(colorSpace);
            graphicsState.setStrokingColor(color);
        }

        // transform the CTM using the stream's matrix
        graphicsState.getCurrentTransformationMatrix().concatenate(patternMatrix);

        // clip to bounding box
        clipToRect(tilingBBox);

        try
        {
            processStreamOperators(tilingPattern);
        }
        finally
        {
            initialMatrix = parentMatrix;
            restoreGraphicsStack(savedStack);
            popResources(parent);
        }
    }

    /**
     * Shows the given annotation.
     *
     * @param annotation An annotation on the current page.
     * @throws IOException If an error occurred reading the annotation
     */
    public void showAnnotation(PDAnnotation annotation) throws IOException
    {
        PDAppearanceStream appearanceStream = getAppearance(annotation);
        if (appearanceStream != null)
        {
            processAnnotation(annotation, appearanceStream);
        }
    }

    /**
     * Returns the appearance stream to process for the given annotation. May be used to render a
     * specific appearance such as "hover".
     *
     * @param annotation The current annotation.
     * @return The stream to process.
     */
    public PDAppearanceStream getAppearance(PDAnnotation annotation)
    {
        return annotation.getNormalAppearanceStream();
    }

    /**
     * Process a child stream of the given page. Cannot be used with #processPage(PDPage).
     *
     * @param contentStream the child content stream
     * @throws IOException if there is an exception while processing the stream
     */
    protected void processChildStream(PDContentStream contentStream, PDPage page) throws IOException
    {
        if (isProcessingPage)
        {
            throw new IllegalStateException("Current page has already been set via "
                    + " #processPage(PDPage) call #processChildStream(PDContentStream) instead");
        }
        initPage(page);
        processStream(contentStream);
        currentPage = null;
    }

    /**
     * Process a content stream.
     *
     * @param contentStream the content stream
     * @throws IOException if there is an exception while processing the stream
     */
    public void processStream(PDContentStream contentStream) throws IOException
    {
        PDResources parent = pushResources(contentStream);
        Deque<PDGraphicsState> savedStack = saveGraphicsStack();
        Matrix parentMatrix = initialMatrix;

        // transform the CTM using the stream's matrix
        getGraphicsState().getCurrentTransformationMatrix().concatenate(contentStream.getMatrix());

        // the stream's initial matrix includes the parent CTM, e.g. this allows a scaled form
        initialMatrix = getGraphicsState().getCurrentTransformationMatrix().clone();

        // clip to bounding box
        PDRectangle bbox = contentStream.getBBox();
        clipToRect(bbox);

        try
        {
            processStreamOperators(contentStream);
        }
        finally
        {
            initialMatrix = parentMatrix;
            restoreGraphicsStack(savedStack);
            popResources(parent);
        }
    }

    /**
     * Processes the operators of the given content stream.
     */
    private void processStreamOperators(PDContentStream contentStream) throws IOException
    {
        List<COSBase> arguments = new ArrayList<>();
        ContentStreamParser parser = new ContentStreamParser(contentStream);
        Object token;
        while ((token = parser.nextParsedToken()) != null)
        {
            if (token instanceof Operator)
            {
                processOperator((Operator) token, arguments);
                arguments = new ArrayList<>();
            }
            else
            {
                arguments.add((COSBase) token);
            }
        }
    }

    /**
     * Pushes the given stream's resources, returning the previous resources.
     */
    private PDResources pushResources(PDContentStream contentStream)
    {
        // resource lookup: first look for stream resources, then fallback to the current page
        PDResources parentResources = resources;
        PDResources streamResources = contentStream.getResources();
        if (streamResources != null)
        {
            resources = streamResources;
        }
        else if (resources != null)
        {
            // inherit directly from parent stream, this is not in the PDF spec, but the file from
            // PDFBOX-1359 does this and works in Acrobat
        }
        else
        {
            resources = currentPage.getResources();
        }

        // resources are required in PDF
        if (resources == null)
        {
            resources = new PDResources();
        }
        return parentResources;
    }

    /**
     * Pops the current resources, replacing them with the given resources.
     */
    private void popResources(PDResources parentResources)
    {
        resources = parentResources;
    }

    /**
     * Transforms the given rectangle using the CTM and then intersects it with the current clipping
     * area.
     */
    private void clipToRect(PDRectangle rectangle)
    {
        if (rectangle != null)
        {
            PDGraphicsState graphicsState = getGraphicsState();
            GeneralPath clip = rectangle.transform(graphicsState.getCurrentTransformationMatrix());
            graphicsState.intersectClippingPath(clip);
        }
    }

    /**
     * Called when the BT operator is encountered. This method is for overriding in subclasses, the
     * default implementation does nothing.
     *
     * @throws IOException if there was an error processing the text
     */
    public void beginText() throws IOException
    {
        // overridden in subclasses
    }

    /**
     * Called when the ET operator is encountered. This method is for overriding in subclasses, the
     * default implementation does nothing.
     *
     * @throws IOException if there was an error processing the text
     */
    public void endText() throws IOException
    {
        // overridden in subclasses
    }

    /**
     * Called when a string of text is to be shown.
     *
     * @param string the encoded text
     * @throws IOException if there was an error showing the text
     */
    public void showTextString(byte[] string) throws IOException
    {
        showText(string);
    }

    /**
     * Called when a string of text with spacing adjustments is to be shown.
     *
     * @param array array of encoded text strings and adjustments
     * @throws IOException if there was an error showing the text
     */
    public void showTextStrings(COSArray array) throws IOException
    {
        PDTextState textState = getGraphicsState().getTextState();
        float fontSize = textState.getFontSize();
        float horizontalScaling = textState.getHorizontalScaling() / 100f;
        PDFont font = textState.getFont();
        boolean isVertical = ofNullable(font).map(PDFont::isVertical).orElse(false);

        for (COSBase obj : array)
        {
            if (obj instanceof COSNumber)
            {
                float tj = ((COSNumber) obj).floatValue();

                // calculate the combined displacements
                float tx, ty;
                if (isVertical)
                {
                    tx = 0;
                    ty = -tj / 1000 * fontSize;
                }
                else
                {
                    tx = -tj / 1000 * fontSize * horizontalScaling;
                    ty = 0;
                }

                applyTextAdjustment(tx, ty);
            }
            else if (obj instanceof COSString)
            {
                byte[] string = ((COSString) obj).getBytes();
                showText(string);
            }
            else if (obj instanceof COSArray)
            {
                LOG.error("Nested arrays are not allowed in an array for TJ operation: {}", obj);
            }
            else
            {
                // SAMBOX specific: we want to be less lenient here and throw an exception
                throw new IOException("Unknown type " + obj.getClass().getSimpleName()
                        + " in array for TJ operation:" + obj);
            }
        }
    }

    /**
     * Applies a text position adjustment from the TJ operator. May be overridden in subclasses.
     *
     * @param tx x-translation
     * @param ty y-translation
     */
    protected void applyTextAdjustment(float tx, float ty) throws IOException
    {
        // update the text matrix
        getGraphicsState().getTextMatrix().translate(tx, ty);
    }

    /**
     * Process text from the PDF Stream. You should override this method if you want to perform an
     * action when encoded text is being processed.
     *
     * @param string the encoded text
     * @throws IOException if there is an error processing the string
     */
    protected void showText(byte[] string) throws IOException
    {
        PDGraphicsState state = getGraphicsState();
        PDTextState textState = state.getTextState();

        // get the current font
        PDFont font = textState.getFont();
        if (font == null)
        {
            LOG.warn("No current font, will use default");
            font = PDType1Font.HELVETICA();
        }

        float fontSize = textState.getFontSize();
        float horizontalScaling = textState.getHorizontalScaling() / 100f;
        float charSpacing = textState.getCharacterSpacing();

        // put the text state parameters into matrix form
        Matrix parameters = new Matrix(fontSize * horizontalScaling, 0, // 0
                0, fontSize, // 0
                0, textState.getRise()); // 1

        Matrix textMatrix = getGraphicsState().getTextMatrix();

        // read the stream until it is empty
        InputStream in = new ByteArrayInputStream(string);
        while (in.available() > 0)
        {
            // decode a character
            int before = in.available();
            int code = font.readCode(in);
            int codeLength = before - in.available();

            // Word spacing shall be applied to every occurrence of the single-byte character code
            // 32 in a string when using a simple font or a composite font that defines code 32 as
            // a single-byte code.
            float wordSpacing = 0;
            if (codeLength == 1 && code == 32)
            {
                wordSpacing += textState.getWordSpacing();
            }

            // text rendering matrix (text space -> device space)
            Matrix ctm = state.getCurrentTransformationMatrix();
            Matrix textRenderingMatrix = parameters.multiply(textMatrix).multiply(ctm);

            // get glyph's position vector if this is vertical text
            // changes to vertical text should be tested with PDFBOX-2294 and PDFBOX-1422
            if (font.isVertical())
            {
                // position vector, in text space
                Vector v = font.getPositionVector(code);

                // apply the position vector to the horizontal origin to get the vertical origin
                textRenderingMatrix.translate(v);
            }

            // get glyph's horizontal and vertical displacements, in text space
            Vector w = font.getDisplacement(code);

            // process the decoded glyph
            showGlyph(textRenderingMatrix, font, code, w);

            // calculate the combined displacements
            float tx, ty;
            if (font.isVertical())
            {
                tx = 0;
                ty = w.getY() * fontSize + charSpacing + wordSpacing;
            }
            else
            {
                tx = (w.getX() * fontSize + charSpacing + wordSpacing) * horizontalScaling;
                ty = 0;
            }

            // update the text matrix
            textMatrix.translate(tx, ty);
        }
    }

    /**
     * Called when a glyph is to be processed. This method is intended for overriding in subclasses,
     * Called when a glyph is to be processed. This method is intended for overriding in subclasses,
     * the default the default implementation does nothing. implementation does nothing.
     *
     * @param textRenderingMatrix the current text rendering matrix, T<sub>rm</sub>
     * @param font                the current font
     * @param code                internal PDF character code for the glyph
     * @param displacement        the displacement (i.e. advance) of the glyph in text space
     * @throws IOException if the glyph cannot be processed
     */
    protected void showGlyph(Matrix textRenderingMatrix, PDFont font, int code, Vector displacement)
            throws IOException
    {
        if (font instanceof PDType3Font)
        {
            showType3Glyph(textRenderingMatrix, (PDType3Font) font, code, displacement);
        }
        else
        {
            showFontGlyph(textRenderingMatrix, font, code, displacement);
        }
    }

    /**
     * Called when a glyph is to be processed.This method is intended for overriding in subclasses,
     * the default implementation does nothing.
     *
     * @param textRenderingMatrix the current text rendering matrix, T<sub>rm</sub>
     * @param font                the current font
     * @param code                internal PDF character code for the glyph
     * @param displacement        the displacement (i.e. advance) of the glyph in text space
     * @throws IOException if the glyph cannot be processed
     */
    protected void showFontGlyph(Matrix textRenderingMatrix, PDFont font, int code,
            Vector displacement) throws IOException
    {
        // overridden in subclasses
    }

    /**
     * Called when a glyph is to be processed.This method is intended for overriding in subclasses,
     * the default implementation does nothing.
     *
     * @param textRenderingMatrix the current text rendering matrix, T<sub>rm</sub>
     * @param font                the current font
     * @param code                internal PDF character code for the glyph
     * @param displacement        the displacement (i.e. advance) of the glyph in text space
     * @throws IOException if the glyph cannot be processed
     */
    protected void showType3Glyph(Matrix textRenderingMatrix, PDType3Font font, int code,
            Vector displacement) throws IOException
    {
        PDType3CharProc charProc = font.getCharProc(code);
        if (charProc != null)
        {
            processType3Stream(charProc, textRenderingMatrix);
        }
    }

    /**
     * Called when a marked content group begins
     *
     * @param tag        content tag
     * @param properties optional properties
     */
    public void beginMarkedContentSequence(COSName tag, COSDictionary properties)
    {
        // overridden in subclasses
    }

    /**
     * Called when a a marked content group ends
     */
    public void endMarkedContentSequence()
    {
        // overridden in subclasses
    }

    /**
     * This is used to handle an operation.
     *
     * @param operation The operation to perform.
     * @param arguments The list of arguments.
     * @throws IOException If there is an error processing the operation.
     */
    public void processOperator(String operation, List<COSBase> arguments) throws IOException
    {
        Operator operator = Operator.getOperator(operation);
        processOperator(operator, arguments);
    }

    /**
     * This is used to handle an operation.
     *
     * @param operator The operation to perform.
     * @param operands The list of arguments.
     * @throws IOException If there is an error processing the operation.
     */
    protected void processOperator(Operator operator, List<COSBase> operands) throws IOException
    {
        String name = operator.getName();
        OperatorProcessor processor = operators.get(name);
        if (processor != null)
        {
            processor.setContext(this);
            try
            {
                processor.process(operator, operands);
            }
            catch (IOException e)
            {
                operatorException(operator, operands, e);
            }
        }
        else
        {
            unsupportedOperator(operator, operands);
        }
    }

    /**
     * Called when an unsupported operator is encountered.
     *
     * @param operator The unknown operator.
     * @param operands The list of operands.
     */
    protected void unsupportedOperator(Operator operator, List<COSBase> operands) throws IOException
    {
        // overridden in subclasses
    }

    /**
     * Called when an exception is thrown by an operator.
     *
     * @param operator The unknown operator.
     * @param operands The list of operands.
     */
    protected void operatorException(Operator operator, List<COSBase> operands, IOException e)
            throws IOException
    {
        if (e instanceof MissingOperandException || e instanceof MissingResourceException
                || e instanceof MissingImageReaderException)
        {
            LOG.error(e.getMessage());
        }
        else if (e instanceof EmptyGraphicsStackException)
        {
            LOG.warn(e.getMessage(), e);
        }
        else if (operator.getName().equals("Do"))
        {
            // todo: this too forgiving, but PDFBox has always worked this way for DrawObject
            // some careful refactoring is needed
            LOG.warn(e.getMessage());
        }
        else
        {
            throw e;
        }
    }

    /**
     * Pushes the current graphics state to the stack.
     */
    public void saveGraphicsState()
    {
        graphicsStack.push(graphicsStack.peek().clone());
    }

    /**
     * Pops the current graphics state from the stack.
     */
    public void restoreGraphicsState()
    {
        graphicsStack.pop();
    }

    /**
     * Saves the entire graphics stack.
     */
    protected final Deque<PDGraphicsState> saveGraphicsStack()
    {
        Deque<PDGraphicsState> savedStack = graphicsStack;
        graphicsStack = new ArrayDeque<>(1);
        graphicsStack.add(savedStack.peek().clone());
        return savedStack;
    }

    /**
     * Restores the entire graphics stack.
     */
    protected final void restoreGraphicsStack(Deque<PDGraphicsState> snapshot)
    {
        graphicsStack = snapshot;
    }

    /**
     * @return Returns the size of the graphicsStack.
     */
    public int getGraphicsStackSize()
    {
        return graphicsStack.size();
    }

    /**
     * @return Returns the graphicsState.
     */
    public PDGraphicsState getGraphicsState()
    {
        return graphicsStack.peek();
    }

    /**
     * @return Returns the textLineMatrix.
     */
    public Matrix getTextLineMatrix()
    {
        return getGraphicsState().getTextLineMatrix();
    }

    /**
     * @param value The textLineMatrix to set.
     */
    public void setTextLineMatrix(Matrix value)
    {
        getGraphicsState().setTextLineMatrix(value);
    }

    /**
     * @return Returns the textMatrix.
     */
    public Matrix getTextMatrix()
    {
        return getGraphicsState().getTextMatrix();
    }

    /**
     * @param value The textMatrix to set.
     */
    public void setTextMatrix(Matrix value)
    {
        getGraphicsState().setTextMatrix(value);
    }

    /**
     * @param array dash array
     * @param phase dash phase
     */
    public void setLineDashPattern(COSArray array, int phase)
    {
        if (phase < 0)
        {
            LOG.warn("Dash phase has negative value " + phase + ", set to 0");
            phase = 0;
        }
        PDLineDashPattern lineDash = new PDLineDashPattern(array, phase);
        getGraphicsState().setLineDashPattern(lineDash);
    }

    /**
     * @return the stream' resources. This is mainly to be used by the {@link OperatorProcessor}
     * classes
     */
    public PDResources getResources()
    {
        return resources;
    }

    /**
     * Returns the current page.
     */
    public PDPage getCurrentPage()
    {
        return currentPage;
    }

    /**
     * Gets the stream's initial matrix.
     */
    public Matrix getInitialMatrix()
    {
        return initialMatrix;
    }

    /**
     * Transforms a point using the CTM.
     */
    public Point2D.Float transformedPoint(float x, float y)
    {
        float[] position = { x, y };
        getGraphicsState().getCurrentTransformationMatrix().createAffineTransform()
                .transform(position, 0, position, 0, 1);
        return new Point2D.Float(position[0], position[1]);
    }

    /**
     * Transforms a width using the CTM.
     */
    protected float transformWidth(float width)
    {
        Matrix ctm = getGraphicsState().getCurrentTransformationMatrix();
        float x = ctm.getScaleX() + ctm.getShearX();
        float y = ctm.getScaleY() + ctm.getShearY();
        return width * (float) Math.sqrt((x * x + y * y) * 0.5);
    }

    /**
     * Get the current level. This can be used to decide whether a recursion has done too deep and
     * an operation should be skipped to avoid a stack overflow.
     *
     * @return the current level.
     */
    public int getLevel()
    {
        return level;
    }

    /**
     * Increase the level. Call this before running a potentially recursive operation.
     */
    public void increaseLevel()
    {
        ++level;
    }

    /**
     * Decrease the level. Call this after running a potentially recursive operation. A log message
     * is shown if the level is below 0. This can happen if the level is not decreased after an
     * operation is done, e.g. by using a "finally" block.
     */
    public void decreaseLevel()
    {
        --level;
        if (level < 0)
        {
            LOG.error("level is " + level);
        }
    }
}
