/*
 * Created on 23/11/26
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.sejda.sambox.contentstream.operator.OperatorName.FILL_NON_ZERO;
import static org.sejda.sambox.contentstream.operator.OperatorName.NON_STROKING_CMYK;
import static org.sejda.sambox.contentstream.operator.OperatorName.NON_STROKING_COLORSPACE;
import static org.sejda.sambox.contentstream.operator.OperatorName.NON_STROKING_GRAY;
import static org.sejda.sambox.contentstream.operator.OperatorName.NON_STROKING_RGB;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.sejda.sambox.contentstream.operator.Operator;
import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.input.ContentStreamParser;
import org.sejda.sambox.pdmodel.common.PDRectangle;
import org.sejda.sambox.pdmodel.graphics.color.PDColor;
import org.sejda.sambox.pdmodel.graphics.color.PDDeviceRGB;
import org.sejda.sambox.pdmodel.interactive.annotation.PDAnnotationMarkup;

class PDCaretAppearanceHandlerTest
{
    private static final Set<String> NON_STROKING_COLOR_OPERATORS = Set.of(NON_STROKING_GRAY,
            NON_STROKING_RGB, NON_STROKING_CMYK, NON_STROKING_COLORSPACE);

    @Test
    void caretIsFilledWithDefaultBlackWhenAnnotationHasNoColor() throws IOException
    {
        var tokens = generateAndParse(annotation());

        assertTrue(indexOfOperator(tokens, FILL_NON_ZERO) >= 0, "Caret should be filled");
        assertTrue(tokens.stream().noneMatch(
                t -> t instanceof Operator op && NON_STROKING_COLOR_OPERATORS.contains(
                        op.getName())), "No fill color expected, PDF default black should be used");
    }

    @Test
    void caretIsFilledWithAnnotationColor() throws IOException
    {
        var annotation = annotation();
        annotation.setColor(new PDColor(new float[] { 1, 0, 0 }, PDDeviceRGB.INSTANCE));

        var tokens = generateAndParse(annotation);

        int colorIndex = indexOfOperator(tokens, NON_STROKING_RGB);
        assertTrue(colorIndex >= 0, "Missing fill color");
        assertTrue(indexOfOperator(tokens, FILL_NON_ZERO) > colorIndex,
                "Color must be set before filling the caret");
    }

    @Test
    void nothingIsDrawnWhenColorIsTransparent() throws IOException
    {
        var annotation = annotation();
        annotation.getCOSObject().setItem(COSName.C, new COSArray());

        var tokens = generateAndParse(annotation);

        assertEquals(-1, indexOfOperator(tokens, FILL_NON_ZERO),
                "Empty /C means transparent, nothing should be filled");
    }

    private static PDAnnotationMarkup annotation()
    {
        var annotation = new PDAnnotationMarkup();
        annotation.getCOSObject().setName(COSName.SUBTYPE, PDAnnotationMarkup.SUB_TYPE_CARET);
        annotation.setRectangle(new PDRectangle(100, 150, 20, 30));
        return annotation;
    }

    private static List<Object> generateAndParse(PDAnnotationMarkup annotation) throws IOException
    {
        new PDCaretAppearanceHandler(annotation).generateNormalAppearance();
        try (var parser = new ContentStreamParser(annotation.getNormalAppearanceStream()))
        {
            return parser.tokens();
        }
    }

    private static int indexOfOperator(List<Object> tokens, String name)
    {
        return IntStream.range(0, tokens.size())
                .filter(i -> tokens.get(i) instanceof Operator op && op.getName().equals(name))
                .findFirst().orElse(-1);
    }
}
