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
package org.sejda.sambox.pdmodel.graphics.shading;

import java.awt.Paint;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.IOException;

import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.pdmodel.common.PDDictionaryWrapper;
import org.sejda.sambox.pdmodel.common.PDRectangle;
import org.sejda.sambox.pdmodel.common.function.PDFunction;
import org.sejda.sambox.pdmodel.graphics.color.PDColorSpace;
import org.sejda.sambox.util.Matrix;

/**
 * A Shading Resource.
 */
public abstract class PDShading extends PDDictionaryWrapper
{
    private PDColorSpace colorSpace = null;
    private PDFunction function = null;
    private PDFunction[] functionArray = null;

    /**
     * shading type 1 = function based shading.
     */
    public static final int SHADING_TYPE1 = 1;

    /**
     * shading type 2 = axial shading.
     */
    public static final int SHADING_TYPE2 = 2;

    /**
     * shading type 3 = radial shading.
     */
    public static final int SHADING_TYPE3 = 3;

    /**
     * shading type 4 = Free-Form Gouraud-Shaded Triangle Meshes.
     */
    public static final int SHADING_TYPE4 = 4;

    /**
     * shading type 5 = Lattice-Form Gouraud-Shaded Triangle Meshes.
     */
    public static final int SHADING_TYPE5 = 5;

    /**
     * shading type 6 = Coons Patch Meshes.
     */
    public static final int SHADING_TYPE6 = 6;

    /**
     * shading type 7 = Tensor-Product Patch Meshes.
     */
    public static final int SHADING_TYPE7 = 7;

    public PDShading()
    {
        super();
    }

    /**
     * @param shadingDictionary the dictionary for this shading
     */
    public PDShading(COSDictionary shadingDictionary)
    {
        super(shadingDictionary);
    }

    /**
     * @return the type of object that this is
     */
    public String getType()
    {
        return COSName.SHADING.getName();
    }

    /**
     * @param shadingType the new shading type
     */
    public void setShadingType(int shadingType)
    {
        getCOSObject().setInt(COSName.SHADING_TYPE, shadingType);
    }

    /**
     * @return the shading type
     */
    public abstract int getShadingType();

    public void setBackground(COSArray background)
    {
        getCOSObject().setItem(COSName.BACKGROUND, background);
    }

    public COSArray getBackground()
    {
        return getCOSObject().getDictionaryObject(COSName.BACKGROUND, COSArray.class);
    }

    /**
     * An array of four numbers in the form coordinate system (see below), giving the coordinates of
     * the left, bottom, right, and top edges, respectively, of the shading's bounding box.
     *
     * @return the BBox of the form
     */
    public PDRectangle getBBox()
    {
        COSArray array = getCOSObject().getDictionaryObject(COSName.BBOX, COSArray.class);
        if (array != null && array.size() >= 4)
        {
            return new PDRectangle(array);
        }
        return null;
    }

    public void setBBox(PDRectangle bbox)
    {
        if (bbox == null)
        {
            getCOSObject().removeItem(COSName.BBOX);
        }
        else
        {
            getCOSObject().setItem(COSName.BBOX, bbox.getCOSObject());
        }
    }

    /**
     * Calculate a bounding rectangle around the areas of this shading context.
     *
     * @return Bounding rectangle or null, if not supported by this shading type.
     */
    public Rectangle2D getBounds(AffineTransform xform, Matrix matrix) throws IOException
    {
        return null;
    }

    /**
     * @param antiAlias the new AntiAlias value
     */
    public void setAntiAlias(boolean antiAlias)
    {
        getCOSObject().setBoolean(COSName.ANTI_ALIAS, antiAlias);
    }

    public boolean getAntiAlias()
    {
        return getCOSObject().getBoolean(COSName.ANTI_ALIAS, false);
    }

    /**
     * @return the color space for the shading or null if none exists.
     * @throws IOException if there is an error getting the color space
     */
    public PDColorSpace getColorSpace() throws IOException
    {
        if (colorSpace == null)
        {
            COSBase colorSpaceDictionary = getCOSObject().getDictionaryObject(COSName.CS,
                    COSName.COLORSPACE);
            if (colorSpaceDictionary != null)
            {
                colorSpace = PDColorSpace.create(colorSpaceDictionary);
            }
        }
        return colorSpace;
    }

    /**
     * @param colorSpace the color space
     */
    public void setColorSpace(PDColorSpace colorSpace)
    {
        this.colorSpace = colorSpace;
        if (colorSpace != null)
        {
            getCOSObject().setItem(COSName.COLORSPACE, colorSpace.getCOSObject());
        }
        else
        {
            getCOSObject().removeItem(COSName.COLORSPACE);
        }
    }

    /**
     * Create the correct PD Model shading based on the COS base shading.
     *
     * @param shadingDictionary the COS shading dictionary
     * @return the newly created shading resources object
     * @throws IOException if we are unable to create the PDShading object
     */
    public static PDShading create(COSDictionary shadingDictionary) throws IOException
    {
        int shadingType = shadingDictionary.getInt(COSName.SHADING_TYPE, 0);
        return switch (shadingType)
        {
            case SHADING_TYPE1 -> new PDShadingType1(shadingDictionary);
            case SHADING_TYPE2 -> new PDShadingType2(shadingDictionary);
            case SHADING_TYPE3 -> new PDShadingType3(shadingDictionary);
            case SHADING_TYPE4 -> new PDShadingType4(shadingDictionary);
            case SHADING_TYPE5 -> new PDShadingType5(shadingDictionary);
            case SHADING_TYPE6 -> new PDShadingType6(shadingDictionary);
            case SHADING_TYPE7 -> new PDShadingType7(shadingDictionary);
            default -> throw new IOException("Error: Unknown shading type " + shadingType);
        };
    }

    /**
     * This will set the function for the color conversion.
     *
     * @param newFunction the new function
     */
    public void setFunction(PDFunction newFunction)
    {
        functionArray = null;
        function = newFunction;
        getCOSObject().setItem(COSName.FUNCTION, newFunction);
    }

    /**
     * This will set the functions COSArray for the color conversion.
     *
     * @param newFunctions the new COSArray containing all functions
     */
    public void setFunction(COSArray newFunctions)
    {
        functionArray = null;
        function = null;
        getCOSObject().setItem(COSName.FUNCTION, newFunctions);
    }

    /**
     * This will return the function used to convert the color values.
     *
     * @return the function
     * @throws java.io.IOException if we were not able to create the function.
     */
    public PDFunction getFunction() throws IOException
    {
        if (function == null)
        {
            COSBase dictionaryFunctionObject = getCOSObject().getDictionaryObject(COSName.FUNCTION);
            if (dictionaryFunctionObject != null)
            {
                function = PDFunction.create(dictionaryFunctionObject);
            }
        }
        return function;
    }

    /**
     * Provide the function(s) of the shading dictionary as array.
     *
     * @return an array containing the function(s)
     * @throws IOException if something went wrong
     */
    private PDFunction[] getFunctionsArray() throws IOException
    {
        if (functionArray == null)
        {
            COSBase functionObject = getCOSObject().getDictionaryObject(COSName.FUNCTION);
            if (functionObject instanceof COSDictionary)
            {
                functionArray = new PDFunction[1];
                functionArray[0] = PDFunction.create(functionObject);
            }
            else if (functionObject instanceof COSArray functionCOSArray)
            {
                int numberOfFunctions = functionCOSArray.size();
                functionArray = new PDFunction[numberOfFunctions];
                for (int i = 0; i < numberOfFunctions; i++)
                {
                    functionArray[i] = PDFunction.create(functionCOSArray.get(i));
                }
            }
            else
            {
                throw new IOException(
                        "mandatory /Function element must be a dictionary or an array");
            }
        }
        return functionArray;
    }

    /**
     * Convert the input value using the functions of the shading getCOSObject().
     *
     * @param inputValue the input value
     * @return the output values
     * @throws IOException thrown if something went wrong
     */
    public float[] evalFunction(float inputValue) throws IOException
    {
        return evalFunction(new float[] { inputValue });
    }

    /**
     * Convert the input values using the functions of the shading getCOSObject().
     *
     * @param input the input values
     * @return the output values
     * @throws IOException thrown if something went wrong
     */
    public float[] evalFunction(float[] input) throws IOException
    {
        PDFunction[] functions = getFunctionsArray();
        int numberOfFunctions = functions.length;
        float[] returnValues;
        if (numberOfFunctions == 1)
        {
            returnValues = functions[0].eval(input);
        }
        else
        {
            returnValues = new float[numberOfFunctions];
            for (int i = 0; i < numberOfFunctions; i++)
            {
                float[] newValue = functions[i].eval(input);
                returnValues[i] = newValue[0];
            }
        }
        // From the PDF spec:
        // "If the value returned by the function for a given colour component
        // is out of range, it shall be adjusted to the nearest valid value."
        for (int i = 0; i < returnValues.length; ++i)
        {
            if (returnValues[i] < 0)
            {
                returnValues[i] = 0;
            }
            else if (returnValues[i] > 1)
            {
                returnValues[i] = 1;
            }
        }
        return returnValues;
    }

    /**
     * Returns an AWT paint which corresponds to this shading
     *
     * @param matrix the pattern matrix concatenated with that of the parent content stream, this
     *               matrix which maps the pattern's internal coordinate system to user space
     * @return an AWT Paint instance
     */
    public abstract Paint toPaint(Matrix matrix);
}
