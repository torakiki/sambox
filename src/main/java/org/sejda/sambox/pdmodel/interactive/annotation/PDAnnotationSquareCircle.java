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
import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSFloat;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.pdmodel.common.PDRectangle;
import org.sejda.sambox.pdmodel.graphics.color.PDColor;
import org.sejda.sambox.pdmodel.interactive.annotation.handlers.PDAppearanceHandler;
import org.sejda.sambox.pdmodel.interactive.annotation.handlers.PDCircleAppearanceHandler;
import org.sejda.sambox.pdmodel.interactive.annotation.handlers.PDSquareAppearanceHandler;

/**
 * This is the class that represents a rectangular or eliptical annotation Introduced in PDF 1.3 specification .
 *
 * @author Paul King
 */
public class PDAnnotationSquareCircle extends PDAnnotationMarkup
{

    /**
     * Constant for a Rectangular type of annotation.
     */
    public static final String SUB_TYPE_SQUARE = "Square";
    /**
     * Constant for an elliptical type of annotation.
     */
    public static final String SUB_TYPE_CIRCLE = "Circle";

    private PDAppearanceHandler customAppearanceHandler;

    /**
     * Creates a Circle or Square annotation of the specified sub type.
     *
     * @param subType the subtype the annotation represents.
     */
    public PDAnnotationSquareCircle(String subType)
    {
        setSubtype(subType);
    }

    /**
     * Creates a Line annotation from a COSDictionary, expected to be a correct object definition.
     *
     * @param field the PDF object to represent as a field.
     */
    public PDAnnotationSquareCircle(COSDictionary field)
    {
        super(field);
    }

    /**
     * This will set interior color of the drawn area color is in DeviceRGB colo rspace.
     *
     * @param ic color in the DeviceRGB color space.
     *
     */
    @Override
    public void setInteriorColor(PDColor ic)
    {
        getCOSObject().setItem(COSName.IC, ic.toComponentsCOSArray());
    }

    /**
     * This will retrieve the interior color of the drawn area color is in DeviceRGB color space.
     *
     * @return object representing the color.
     */
    @Override
    public PDColor getInteriorColor()
    {
        return getColor(COSName.IC);
    }

    /**
     * This will set the border effect dictionary, specifying effects to be applied when drawing the line.
     *
     * @param be The border effect dictionary to set.
     *
     */
    @Override
    public void setBorderEffect(PDBorderEffectDictionary be)
    {
        getCOSObject().setItem(COSName.BE, be);
    }

    /**
     * This will retrieve the border effect dictionary, specifying effects to be applied used in drawing the line.
     *
     * @return The border effect dictionary
     */
    @Override
    public PDBorderEffectDictionary getBorderEffect()
    {
        return ofNullable(getCOSObject().getDictionaryObject(COSName.BE, COSDictionary.class))
                .map(PDBorderEffectDictionary::new).orElse(null);
    }

    /**
     * This will set the rectangle difference rectangle. Giving the difference between the annotations rectangle and
     * where the drawing occurs. (To take account of any effects applied through the BE entry forexample)
     *
     * @param rd the rectangle difference
     *
     */
    @Override
    public void setRectDifference(PDRectangle rd)
    {
        getCOSObject().setItem(COSName.RD, rd);
    }

    /**
     * This will get the rectangle difference rectangle. Giving the difference between the annotations rectangle and
     * where the drawing occurs. (To take account of any effects applied through the BE entry forexample)
     *
     * @return the rectangle difference
     */
    @Override
    public PDRectangle getRectDifference()
    {
        return ofNullable(getCOSObject().getDictionaryObject(COSName.RD, COSArray.class))
                .map(PDRectangle::new).orElse(null);
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
     * This will set the border style dictionary, specifying the width and dash pattern used in drawing the line.
     *
     * @param bs the border style dictionary to set. TODO not all annotations may have a BS entry
     *
     */
    @Override
    public void setBorderStyle(PDBorderStyleDictionary bs)
    {
        this.getCOSObject().setItem(COSName.BS, bs);
    }

    /**
     * This will retrieve the border style dictionary, specifying the width and dash pattern used in drawing the line.
     *
     * @return the border style dictionary. TODO not all annotations may have a BS entry
     */
    @Override
    public PDBorderStyleDictionary getBorderStyle()
    {
        return ofNullable(getCOSObject().getDictionaryObject(COSName.BS, COSDictionary.class))
                .map(PDBorderStyleDictionary::new).orElse(null);
    }

    /**
     * This will set the difference between the annotations "outer" rectangle defined by /Rect and the border.
     *
     * <p>
     * This will set an equal difference for all sides
     * </p>
     *
     * @param difference from the annotations /Rect entry
     */
    @Override
    public void setRectDifferences(float difference)
    {
        setRectDifferences(difference, difference, difference, difference);
    }

    /**
     * This will set the difference between the annotations "outer" rectangle defined by /Rect and the border.
     * 
     * @param differenceLeft left difference from the annotations /Rect entry
     * @param differenceTop top difference from the annotations /Rect entry
     * @param differenceRight right difference from the annotations /Rect entry
     * @param differenceBottom bottom difference from the annotations /Rect entry
     * 
     */
    @Override
    public void setRectDifferences(float differenceLeft, float differenceTop, float differenceRight,
            float differenceBottom)
    {
        COSArray margins = new COSArray();
        margins.add(new COSFloat(differenceLeft));
        margins.add(new COSFloat(differenceTop));
        margins.add(new COSFloat(differenceRight));
        margins.add(new COSFloat(differenceBottom));
        getCOSObject().setItem(COSName.RD, margins);
    }

    /**
     * This will get the differences between the annotations "outer" rectangle defined by /Rect and the border.
     * 
     * @return the differences. If the entry hasn't been set am empty array is returned.
     */
    @Override
    public float[] getRectDifferences()
    {
        COSBase margin = getCOSObject().getItem(COSName.RD);
        if (margin instanceof COSArray)
        {
            return ((COSArray) margin).toFloatArray();
        }
        return new float[] {};
    }

    /**
     * Set a custom appearance handler for generating the annotations appearance streams.
     * 
     * @param appearanceHandler
     */
    @Override
    public void setCustomAppearanceHandler(PDAppearanceHandler appearanceHandler)
    {
        customAppearanceHandler = appearanceHandler;
    }

    @Override
    public void constructAppearances()
    {
        if (customAppearanceHandler == null)
        {
            if (SUB_TYPE_CIRCLE.equals(getSubtype()))
            {
                PDCircleAppearanceHandler appearanceHandler = new PDCircleAppearanceHandler(this);
                appearanceHandler.generateAppearanceStreams();
            }
            else if (SUB_TYPE_SQUARE.equals(getSubtype()))
            {
                PDSquareAppearanceHandler appearanceHandler = new PDSquareAppearanceHandler(this);
                appearanceHandler.generateAppearanceStreams();
            }
        }
        else
        {
            customAppearanceHandler.generateAppearanceStreams();
        }
    }

}
