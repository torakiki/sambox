/*
 * Created on 13/11/26
 * Copyright 2026 by Sober Lemur S.r.l. (info@soberlemur.com).
 *
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
package org.sejda.sambox.pdmodel.interactive.annotation.handlers;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.sejda.sambox.contentstream.operator.OperatorName.FILL_NON_ZERO;
import static org.sejda.sambox.contentstream.operator.OperatorName.NON_STROKING_CMYK;
import static org.sejda.sambox.contentstream.operator.OperatorName.NON_STROKING_GRAY;
import static org.sejda.sambox.contentstream.operator.OperatorName.NON_STROKING_RGB;
import static org.sejda.sambox.contentstream.operator.OperatorName.SET_GRAPHICS_STATE_PARAMS;
import static org.sejda.sambox.pdmodel.interactive.annotation.PDAnnotationFileAttachment.ATTACHMENT_NAME_GRAPH;
import static org.sejda.sambox.pdmodel.interactive.annotation.PDAnnotationFileAttachment.ATTACHMENT_NAME_PAPERCLIP;
import static org.sejda.sambox.pdmodel.interactive.annotation.PDAnnotationFileAttachment.ATTACHMENT_NAME_PUSH_PIN;
import static org.sejda.sambox.pdmodel.interactive.annotation.PDAnnotationFileAttachment.ATTACHMENT_NAME_TAG;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.sejda.sambox.contentstream.operator.Operator;
import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSNumber;
import org.sejda.sambox.input.ContentStreamParser;
import org.sejda.sambox.pdmodel.common.PDRectangle;
import org.sejda.sambox.pdmodel.graphics.color.PDColor;
import org.sejda.sambox.pdmodel.graphics.color.PDDeviceCMYK;
import org.sejda.sambox.pdmodel.graphics.color.PDDeviceGray;
import org.sejda.sambox.pdmodel.graphics.color.PDDeviceRGB;
import org.sejda.sambox.pdmodel.interactive.annotation.PDAnnotationFileAttachment;

class PDFileAttachmentAppearanceHandlerTest
{
    private static final Set<String> NON_STROKING_COLOR_OPERATORS = Set.of(NON_STROKING_GRAY,
            NON_STROKING_RGB, NON_STROKING_CMYK);

    @Test
    void rgbColorFillsTheIcon() throws IOException
    {
        var annotation = annotation();
        annotation.setColor(new PDColor(new float[] { 1, 0.5f, 0 }, PDDeviceRGB.INSTANCE));

        var tokens = generateAndParse(annotation);

        assertColorSetBeforeFill(tokens, NON_STROKING_RGB, 1, 0.5f, 0);
    }

    @Test
    void grayColorFillsTheIcon() throws IOException
    {
        var annotation = annotation();
        annotation.setColor(new PDColor(new float[] { 0.3f }, PDDeviceGray.INSTANCE));

        var tokens = generateAndParse(annotation);

        assertColorSetBeforeFill(tokens, NON_STROKING_GRAY, 0.3f);
    }

    @Test
    void cmykColorFillsTheIcon() throws IOException
    {
        var annotation = annotation();
        annotation.setColor(
                new PDColor(new float[] { 0.1f, 0.2f, 0.3f, 0.4f }, PDDeviceCMYK.INSTANCE));

        var tokens = generateAndParse(annotation);

        assertColorSetBeforeFill(tokens, NON_STROKING_CMYK, 0.1f, 0.2f, 0.3f, 0.4f);
    }

    @Test
    void colorFillsEveryIcon()
    {
        assertAll(Stream.of(ATTACHMENT_NAME_PUSH_PIN, ATTACHMENT_NAME_GRAPH,
                        ATTACHMENT_NAME_PAPERCLIP, ATTACHMENT_NAME_TAG, "Unknown")
                .map(name -> (Executable) () -> assertIconFilledWithBlue(name)));
    }

    @Test
    void noColorIsSetWhenAnnotationHasNoColor() throws IOException
    {
        var tokens = generateAndParse(annotation());

        assertTrue(indexOfOperator(tokens, FILL_NON_ZERO) >= 0, "Icon should be filled");
        assertTrue(tokens.stream().noneMatch(
                t -> t instanceof Operator op && NON_STROKING_COLOR_OPERATORS.contains(
                        op.getName())), "No fill color expected, PDF default black should be used");
    }

    @Test
    void nothingIsDrawnWhenColorIsTransparent() throws IOException
    {
        var annotation = annotation();
        annotation.getCOSObject().setItem(COSName.C, new COSArray());

        var tokens = generateAndParse(annotation);

        assertEquals(-1, indexOfOperator(tokens, FILL_NON_ZERO),
                "Empty /C means transparent, nothing should be filled");
        var bbox = annotation.getNormalAppearanceStream().getBBox();
        assertEquals(18, bbox.getWidth());
        assertEquals(18, bbox.getHeight());
    }

    @Test
    void opacityIsAppliedTogetherWithColor() throws IOException
    {
        var annotation = annotation();
        annotation.setConstantOpacity(0.5f);
        annotation.setColor(new PDColor(new float[] { 1, 0, 0 }, PDDeviceRGB.INSTANCE));

        var tokens = generateAndParse(annotation);

        assertTrue(indexOfOperator(tokens, SET_GRAPHICS_STATE_PARAMS) >= 0,
                "Opacity graphics state expected");
        assertColorSetBeforeFill(tokens, NON_STROKING_RGB, 1, 0, 0);
    }

    @Test
    void rectangleAndBBoxAreResizedToIconSize()
    {
        var annotation = annotation();

        new PDFileAttachmentAppearanceHandler(annotation).generateNormalAppearance();

        var rect = annotation.getRectangle();
        assertEquals(100, rect.getLowerLeftX());
        assertEquals(182, rect.getLowerLeftY());
        assertEquals(118, rect.getUpperRightX());
        assertEquals(200, rect.getUpperRightY());
        var bbox = annotation.getNormalAppearanceStream().getBBox();
        assertEquals(18, bbox.getWidth());
        assertEquals(18, bbox.getHeight());
    }

    @Test
    void noAppearanceWhenRectangleIsMissing()
    {
        var annotation = new PDAnnotationFileAttachment();
        annotation.setColor(new PDColor(new float[] { 1, 0, 0 }, PDDeviceRGB.INSTANCE));

        new PDFileAttachmentAppearanceHandler(annotation).generateNormalAppearance();

        assertNull(annotation.getAppearance());
    }

    private static PDAnnotationFileAttachment annotation()
    {
        var annotation = new PDAnnotationFileAttachment();
        annotation.setRectangle(new PDRectangle(100, 150, 50, 50));
        return annotation;
    }

    private static void assertIconFilledWithBlue(String attachmentName) throws IOException
    {
        var annotation = annotation();
        annotation.setAttachmentName(attachmentName);
        annotation.setColor(new PDColor(new float[] { 0, 0, 1 }, PDDeviceRGB.INSTANCE));

        assertColorSetBeforeFill(generateAndParse(annotation), NON_STROKING_RGB, 0, 0, 1);
    }

    private static List<Object> generateAndParse(PDAnnotationFileAttachment annotation)
            throws IOException
    {
        new PDFileAttachmentAppearanceHandler(annotation).generateNormalAppearance();
        try (var parser = new ContentStreamParser(annotation.getNormalAppearanceStream()))
        {
            return parser.tokens();
        }
    }

    private static void assertColorSetBeforeFill(List<Object> tokens, String colorOperator,
            float... expectedComponents)
    {
        int colorIndex = indexOfOperator(tokens, colorOperator);
        int fillIndex = indexOfOperator(tokens, FILL_NON_ZERO);
        assertTrue(colorIndex >= 0, "Missing color operator " + colorOperator);
        assertTrue(fillIndex > colorIndex, "Color must be set before filling the icon");
        assertArrayEquals(expectedComponents,
                operandsOf(tokens, colorIndex, expectedComponents.length), 0.0001f);
    }

    private static int indexOfOperator(List<Object> tokens, String name)
    {
        return IntStream.range(0, tokens.size())
                .filter(i -> tokens.get(i) instanceof Operator op && op.getName().equals(name))
                .findFirst().orElse(-1);
    }

    private static float[] operandsOf(List<Object> tokens, int operatorIndex, int count)
    {
        var operands = tokens.subList(operatorIndex - count, operatorIndex);
        var values = new float[count];
        for (int i = 0; i < count; i++)
        {
            values[i] = ((COSNumber) operands.get(i)).floatValue();
        }
        return values;
    }
}
