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

import org.sejda.sambox.pdmodel.PDAppearanceContentStream;
import org.sejda.sambox.pdmodel.common.PDRectangle;
import org.sejda.sambox.pdmodel.graphics.color.PDColor;
import org.sejda.sambox.pdmodel.interactive.annotation.PDAnnotation;
import org.sejda.sambox.pdmodel.interactive.annotation.PDAnnotationMarkup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler to generate the ink annotations appearance.
 */
public class PDInkAppearanceHandler extends PDAbstractAppearanceHandler
{
    private static final Logger LOG = LoggerFactory.getLogger(PDInkAppearanceHandler.class);

    public PDInkAppearanceHandler(PDAnnotation annotation)
    {
        super(annotation);
    }

    @Override
    public void generateNormalAppearance()
    {
        PDAnnotationMarkup ink = (PDAnnotationMarkup) getAnnotation();
        // PDF spec does not mention /Border for ink annotations, but it is used if /BS is not available
        AnnotationBorder ab = AnnotationBorder.getAnnotationBorder(ink, ink.getBorderStyle());
        PDColor color = ink.getColor();
        if (color == null || color.getComponents().length == 0 || Float.compare(ab.width, 0) == 0)
        {
            return;
        }

        // Adjust rectangle even if not empty
        // file from PDF.js issue 13447
        //TODO in a class structure this should be overridable
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE;
        float maxY = Float.MIN_VALUE;
        for (float[] pathArray : ink.getInkList())
        {
            int nPoints = pathArray.length / 2;
            for (int i = 0; i < nPoints; ++i)
            {
                float x = pathArray[i * 2];
                float y = pathArray[i * 2 + 1];
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
            }
        }
        PDRectangle rect = ink.getRectangle();
        if (rect == null)
        {
            return;
        }
        rect.setLowerLeftX(Math.min(minX - ab.width * 2, rect.getLowerLeftX()));
        rect.setLowerLeftY(Math.min(minY - ab.width * 2, rect.getLowerLeftY()));
        rect.setUpperRightX(Math.max(maxX + ab.width * 2, rect.getUpperRightX()));
        rect.setUpperRightY(Math.max(maxY + ab.width * 2, rect.getUpperRightY()));
        ink.setRectangle(rect);

        try (PDAppearanceContentStream cs = getNormalAppearanceAsContentStream())
        {
            setOpacity(cs, ink.getConstantOpacity());

            cs.setStrokingColor(color);
            if (ab.dashArray != null)
            {
                cs.setLineDashPattern(ab.dashArray, 0);
            }
            cs.setLineWidth(ab.width);

            for (float[] pathArray : ink.getInkList())
            {
                int nPoints = pathArray.length / 2;

                // "When drawn, the points shall be connected by straight lines or curves
                // in an implementation-dependent way" - we do lines.
                for (int i = 0; i < nPoints; ++i)
                {
                    float x = pathArray[i * 2];
                    float y = pathArray[i * 2 + 1];

                    if (i == 0)
                    {
                        cs.moveTo(x, y);
                    }
                    else
                    {
                        cs.lineTo(x, y);
                    }
                }
                cs.stroke();
            }
        }
        catch (IOException e)
        {
            LOG.error("Error writing to the content stream", e);
        }
    }

    @Override
    public void generateRolloverAppearance()
    {
        // No rollover appearance generated
    }

    @Override
    public void generateDownAppearance()
    {
        // No down appearance generated
    }
}
