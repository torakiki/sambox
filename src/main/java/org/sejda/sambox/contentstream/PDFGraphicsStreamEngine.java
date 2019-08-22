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

import java.awt.geom.Point2D;
import java.io.IOException;

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
import org.sejda.sambox.contentstream.operator.graphics.AppendRectangleToPath;
import org.sejda.sambox.contentstream.operator.graphics.BeginInlineImage;
import org.sejda.sambox.contentstream.operator.graphics.ClipEvenOddRule;
import org.sejda.sambox.contentstream.operator.graphics.ClipNonZeroRule;
import org.sejda.sambox.contentstream.operator.graphics.CloseAndStrokePath;
import org.sejda.sambox.contentstream.operator.graphics.CloseFillEvenOddAndStrokePath;
import org.sejda.sambox.contentstream.operator.graphics.CloseFillNonZeroAndStrokePath;
import org.sejda.sambox.contentstream.operator.graphics.ClosePath;
import org.sejda.sambox.contentstream.operator.graphics.CurveTo;
import org.sejda.sambox.contentstream.operator.graphics.CurveToReplicateFinalPoint;
import org.sejda.sambox.contentstream.operator.graphics.CurveToReplicateInitialPoint;
import org.sejda.sambox.contentstream.operator.graphics.DrawObject;
import org.sejda.sambox.contentstream.operator.graphics.EndPath;
import org.sejda.sambox.contentstream.operator.graphics.FillEvenOddAndStrokePath;
import org.sejda.sambox.contentstream.operator.graphics.FillEvenOddRule;
import org.sejda.sambox.contentstream.operator.graphics.FillNonZeroAndStrokePath;
import org.sejda.sambox.contentstream.operator.graphics.FillNonZeroRule;
import org.sejda.sambox.contentstream.operator.graphics.LegacyFillNonZeroRule;
import org.sejda.sambox.contentstream.operator.graphics.LineTo;
import org.sejda.sambox.contentstream.operator.graphics.MoveTo;
import org.sejda.sambox.contentstream.operator.graphics.ShadingFill;
import org.sejda.sambox.contentstream.operator.graphics.StrokePath;
import org.sejda.sambox.contentstream.operator.markedcontent.BeginMarkedContentSequence;
import org.sejda.sambox.contentstream.operator.markedcontent.BeginMarkedContentSequenceWithProperties;
import org.sejda.sambox.contentstream.operator.markedcontent.EndMarkedContentSequence;
import org.sejda.sambox.contentstream.operator.state.Concatenate;
import org.sejda.sambox.contentstream.operator.state.Restore;
import org.sejda.sambox.contentstream.operator.state.Save;
import org.sejda.sambox.contentstream.operator.state.SetFlatness;
import org.sejda.sambox.contentstream.operator.state.SetGraphicsStateParameters;
import org.sejda.sambox.contentstream.operator.state.SetLineCapStyle;
import org.sejda.sambox.contentstream.operator.state.SetLineDashPattern;
import org.sejda.sambox.contentstream.operator.state.SetLineJoinStyle;
import org.sejda.sambox.contentstream.operator.state.SetLineMiterLimit;
import org.sejda.sambox.contentstream.operator.state.SetLineWidth;
import org.sejda.sambox.contentstream.operator.state.SetMatrix;
import org.sejda.sambox.contentstream.operator.state.SetRenderingIntent;
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
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.pdmodel.PDPage;
import org.sejda.sambox.pdmodel.graphics.image.PDImage;

/**
 * PDFStreamEngine subclass for advanced processing of graphics. This class should be subclassed by end users looking to
 * hook into graphics operations.
 *
 * @author John Hewson
 */
public abstract class PDFGraphicsStreamEngine extends PDFStreamEngine
{
    // may be null, for example if the stream is a tiling pattern
    private final PDPage page;

    /**
     * Constructor.
     */
    protected PDFGraphicsStreamEngine(PDPage page)
    {
        this.page = page;

        addOperator(new CloseFillNonZeroAndStrokePath());
        addOperator(new FillNonZeroAndStrokePath());
        addOperator(new CloseFillEvenOddAndStrokePath());
        addOperator(new FillEvenOddAndStrokePath());
        addOperator(new BeginInlineImage());
        addOperator(new BeginText());
        addOperator(new CurveTo());
        addOperator(new Concatenate());
        addOperator(new SetStrokingColorSpace());
        addOperator(new SetNonStrokingColorSpace());
        addOperator(new SetLineDashPattern());
        addOperator(new DrawObject()); // special graphics version
        addOperator(new EndText());
        addOperator(new FillNonZeroRule());
        addOperator(new LegacyFillNonZeroRule());
        addOperator(new FillEvenOddRule());
        addOperator(new SetStrokingDeviceGrayColor());
        addOperator(new SetNonStrokingDeviceGrayColor());
        addOperator(new SetGraphicsStateParameters());
        addOperator(new ClosePath());
        addOperator(new SetFlatness());
        addOperator(new SetLineJoinStyle());
        addOperator(new SetLineCapStyle());
        addOperator(new SetStrokingDeviceCMYKColor());
        addOperator(new SetNonStrokingDeviceCMYKColor());
        addOperator(new LineTo());
        addOperator(new MoveTo());
        addOperator(new SetLineMiterLimit());
        addOperator(new EndPath());
        addOperator(new Save());
        addOperator(new Restore());
        addOperator(new AppendRectangleToPath());
        addOperator(new SetStrokingDeviceRGBColor());
        addOperator(new SetNonStrokingDeviceRGBColor());
        addOperator(new SetRenderingIntent());
        addOperator(new CloseAndStrokePath());
        addOperator(new StrokePath());
        addOperator(new SetStrokingColor());
        addOperator(new SetNonStrokingColor());
        addOperator(new SetStrokingColorN());
        addOperator(new SetNonStrokingColorN());
        addOperator(new ShadingFill());
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
        addOperator(new CurveToReplicateInitialPoint());
        addOperator(new SetLineWidth());
        addOperator(new ClipNonZeroRule());
        addOperator(new ClipEvenOddRule());
        addOperator(new CurveToReplicateFinalPoint());
        addOperator(new ShowTextLine());
        addOperator(new ShowTextLineAndSpace());
        addOperator(new BeginMarkedContentSequence());
        addOperator(new BeginMarkedContentSequenceWithProperties());
        addOperator(new EndMarkedContentSequence());
    }

    /**
     * 
     * @return the page.
     * 
     */
    protected final PDPage getPage()
    {
        return page;
    }

    /**
     * Append a rectangle to the current path.
     * 
     * @param p0 point P0 of the rectangle.
     * @param p1 point P1 of the rectangle.
     * @param p2 point P2 of the rectangle.
     * @param p3 point P3 of the rectangle.
     * 
     * @throws IOException if something went wrong.
     */
    public abstract void appendRectangle(Point2D p0, Point2D p1, Point2D p2, Point2D p3)
            throws IOException;

    /**
     * Draw the image.
     *
     * @param pdImage The image to draw.
     * 
     * @throws IOException if something went wrong.
     */
    public abstract void drawImage(PDImage pdImage) throws IOException;

    /**
     * Modify the current clipping path by intersecting it with the current path. The clipping path will not be updated
     * until the succeeding painting operator is called.
     *
     * @param windingRule The winding rule which will be used for clipping.
     */
    public abstract void clip(int windingRule) throws IOException;

    /**
     * Starts a new path at (x,y).
     */
    public abstract void moveTo(float x, float y) throws IOException;

    /**
     * Draws a line from the current point to (x,y).
     */
    public abstract void lineTo(float x, float y) throws IOException;

    /**
     * Draws a curve from the current point to (x3,y3) using (x1,y1) and (x2,y2) as control points.
     */
    public abstract void curveTo(float x1, float y1, float x2, float y2, float x3, float y3)
            throws IOException;

    /**
     * Returns the current point of the current path.
     */
    public abstract Point2D getCurrentPoint() throws IOException;

    /**
     * Closes the current path.
     */
    public abstract void closePath() throws IOException;

    /**
     * Ends the current path without filling or stroking it. The clipping path is updated here.
     */
    public abstract void endPath() throws IOException;

    /**
     * Stroke the path.
     *
     * @throws IOException If there is an IO error while stroking the path.
     */
    public abstract void strokePath() throws IOException;

    /**
     * Fill the path.
     *
     * @param windingRule The winding rule this path will use.
     */
    public abstract void fillPath(int windingRule) throws IOException;

    /**
     * Fills and then strokes the path.
     *
     * @param windingRule The winding rule this path will use.
     */
    public abstract void fillAndStrokePath(int windingRule) throws IOException;

    /**
     * Fill with Shading.
     *
     * @param shadingName The name of the Shading Dictionary to use for this fill instruction.
     */
    public abstract void shadingFill(COSName shadingName) throws IOException;
}
