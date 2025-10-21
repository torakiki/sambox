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
package org.sejda.sambox.pdmodel.interactive.annotation;

import static java.util.Optional.ofNullable;

import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.pdmodel.interactive.annotation.handlers.PDAppearanceHandler;
import org.sejda.sambox.pdmodel.interactive.annotation.handlers.PDHighlightAppearanceHandler;
import org.sejda.sambox.pdmodel.interactive.annotation.handlers.PDSquigglyAppearanceHandler;
import org.sejda.sambox.pdmodel.interactive.annotation.handlers.PDStrikeoutAppearanceHandler;
import org.sejda.sambox.pdmodel.interactive.annotation.handlers.PDUnderlineAppearanceHandler;

/**
 * This is the abstract class that represents a text markup annotation Introduced in PDF 1.3 specification, except
 * Squiggly lines in 1.4.
 *
 * @author Paul King
 */
public class PDAnnotationTextMarkup extends PDAnnotationMarkup
{
    private PDAppearanceHandler customAppearanceHandler;

    /**
     * The types of annotation.
     */
    public static final String SUB_TYPE_HIGHLIGHT = "Highlight";
    /**
     * The types of annotation.
     */
    public static final String SUB_TYPE_UNDERLINE = "Underline";
    /**
     * The types of annotation.
     */
    public static final String SUB_TYPE_SQUIGGLY = "Squiggly";
    /**
     * The types of annotation.
     */
    public static final String SUB_TYPE_STRIKEOUT = "StrikeOut";

    /**
     * Creates a TextMarkup annotation of the specified sub type.
     *
     * @param subType the subtype the annotation represents
     */
    public PDAnnotationTextMarkup(String subType)
    {
        setSubtype(subType);

        // Quad points are required, set and empty array
        setQuadPoints(new float[0]);
    }

    /**
     * Creates a TextMarkup annotation from a COSDictionary, expected to be a correct object definition.
     *
     * @param field the PDF objet to represent as a field.
     */
    public PDAnnotationTextMarkup(COSDictionary field)
    {
        super(field);
    }

    /**
     * This will set the set of quadpoints which encompass the areas of this annotation.
     *
     * @param quadPoints an array representing the set of area covered
     */
    public void setQuadPoints(float[] quadPoints)
    {
        getCOSObject().setItem(COSName.QUADPOINTS, COSArray.fromFloats(quadPoints));
    }

    /**
     * This will retrieve the set of quadpoints which encompass the areas of this annotation.
     *
     * @return An array of floats representing the quad points or null if not present.
     */
    public float[] getQuadPoints()
    {
        return ofNullable(getCOSObject().getDictionaryObject(COSName.QUADPOINTS, COSArray.class))
                .map(COSArray::toFloatArray).orElse(null);
    }

    /**
     * This will set the sub type (and hence appearance, AP taking precedence) For this annotation. See the SUB_TYPE_XXX
     * constants for valid values.
     *
     * @param subType The subtype of the annotation
     */
    public void setSubtype(String subType)
    {
        getCOSObject().setName(COSName.SUBTYPE, subType);
    }

    /**
     * This will retrieve the sub type (and hence appearance, AP taking precedence) For this annotation.
     *
     * @return The subtype of this annotation, see the SUB_TYPE_XXX constants.
     */
    @Override
    public String getSubtype()
    {
        return getCOSObject().getNameAsString(COSName.SUBTYPE);
    }

    /**
     * Set a custom appearance handler for generating the annotations appearance streams.
     * 
     * @param appearanceHandler
     */
    public void setCustomAppearanceHandler(PDAppearanceHandler appearanceHandler)
    {
        customAppearanceHandler = appearanceHandler;
    }

    @Override
    public void constructAppearances()
    {
        if (customAppearanceHandler == null)
        {
            PDAppearanceHandler appearanceHandler = null;
            if (SUB_TYPE_HIGHLIGHT.equals(getSubtype()))
            {
                appearanceHandler = new PDHighlightAppearanceHandler(this);
            }
            else if (SUB_TYPE_SQUIGGLY.equals(getSubtype()))
            {
                appearanceHandler = new PDSquigglyAppearanceHandler(this);
            }
            else if (SUB_TYPE_STRIKEOUT.equals(getSubtype()))
            {
                appearanceHandler = new PDStrikeoutAppearanceHandler(this);
            }
            else if (SUB_TYPE_UNDERLINE.equals(getSubtype()))
            {
                appearanceHandler = new PDUnderlineAppearanceHandler(this);
            }

            if (appearanceHandler != null)
            {
                appearanceHandler.generateAppearanceStreams();
            }
        }
        else
        {
            customAppearanceHandler.generateAppearanceStreams();
        }
    }

}
