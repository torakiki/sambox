/*
 * Copyright 2018 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sejda.sambox.pdmodel.interactive.annotation.handlers;

import java.io.IOException;

import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.pdmodel.PDAppearanceContentStream;
import org.sejda.sambox.pdmodel.common.PDRectangle;
import org.sejda.sambox.pdmodel.interactive.annotation.PDAnnotation;
import org.sejda.sambox.pdmodel.interactive.annotation.PDAnnotationMarkup;
import org.sejda.sambox.pdmodel.interactive.annotation.PDAppearanceStream;
import org.sejda.sambox.util.Matrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler to generate the caret annotations appearance.
 *
 * @author Tilman Hausherr
 */
public class PDCaretAppearanceHandler extends PDAbstractAppearanceHandler
{
    private static final Logger LOG = LoggerFactory.getLogger(PDCaretAppearanceHandler.class);

    public PDCaretAppearanceHandler(PDAnnotation annotation)
    {
        super(annotation);
    }

    @Override
    public void generateNormalAppearance()
    {
        PDAnnotationMarkup annotation = (PDAnnotationMarkup) getAnnotation();

        try (PDAppearanceContentStream contentStream = getNormalAppearanceAsContentStream())
        {
            contentStream.setStrokingColor(getColor());
            contentStream.setNonStrokingColor(getColor());

            setOpacity(contentStream, annotation.getConstantOpacity());

            PDRectangle rect = getRectangle();
            PDRectangle bbox = new PDRectangle(rect.getWidth(), rect.getHeight());
            PDAppearanceStream pdAppearanceStream = annotation.getNormalAppearanceStream();
            if (!annotation.getCOSObject().containsKey(COSName.RD))
            {
                // Adobe creates the /RD entry with a number that is decided
                // by dividing the height by 10, with a maximum result of 5.
                // That number is then used to enlarge the bbox and the rectangle and added to the
                // translation values in the matrix and also used for the line width
                // (not here because it has no effect, see comment near fill() ).
                // The curves are based on the original rectangle.
                float rd = Math.min(rect.getHeight() / 10, 5);
                annotation.setRectDifferences(rd);
                bbox = new PDRectangle(-rd, -rd, rect.getWidth() + 2 * rd,
                        rect.getHeight() + 2 * rd);
                Matrix matrix = pdAppearanceStream.getMatrix();
                matrix.transformPoint(rd, rd);
                pdAppearanceStream.setMatrix(matrix.createAffineTransform());
                PDRectangle rect2 = new PDRectangle(rect.getLowerLeftX() - rd,
                        rect.getLowerLeftY() - rd, rect.getWidth() + 2 * rd,
                        rect.getHeight() + 2 * rd);
                annotation.setRectangle(rect2);
            }
            pdAppearanceStream.setBBox(bbox);

            float halfX = rect.getWidth() / 2;
            float halfY = rect.getHeight() / 2;
            contentStream.moveTo(0, 0);
            contentStream.curveTo(halfX, 0, halfX, halfY, halfX, rect.getHeight());
            contentStream.curveTo(halfX, halfY, halfX, 0, rect.getWidth(), 0);
            contentStream.closePath();
            contentStream.fill();
            // Adobe has an additional stroke, but it has no effect
            // because fill "consumes" the path.
        }
        catch (IOException e)
        {
            LOG.error("Error writing to the content stream", e);
        }
    }

    @Override
    public void generateRolloverAppearance()
    {
        // TODO to be implemented
    }

    @Override
    public void generateDownAppearance()
    {
        // TODO to be implemented
    }
}
