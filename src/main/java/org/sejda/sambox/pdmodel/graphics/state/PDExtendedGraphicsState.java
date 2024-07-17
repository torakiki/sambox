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
package org.sejda.sambox.pdmodel.graphics.state;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;

import java.io.IOException;

import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSFloat;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSNumber;
import org.sejda.sambox.pdmodel.common.PDDictionaryWrapper;
import org.sejda.sambox.pdmodel.graphics.PDFontSetting;
import org.sejda.sambox.pdmodel.graphics.PDLineDashPattern;
import org.sejda.sambox.pdmodel.graphics.blend.BlendMode;

/**
 * An extended graphics state dictionary.
 *
 * @author Ben Litchfield
 */
public class PDExtendedGraphicsState extends PDDictionaryWrapper
{

    /**
     * Creates blank graphics state.
     */
    public PDExtendedGraphicsState()
    {
        super(COSDictionary.of(COSName.TYPE, COSName.EXT_G_STATE));
    }

    /**
     * Create a graphics state from an existing dictionary.
     */
    public PDExtendedGraphicsState(COSDictionary dictionary)
    {
        super(dictionary);
    }

    /**
     * This will implement the gs operator.
     *
     * @param gs The state to copy this dictionaries values into.
     *
     * @throws IOException If there is an error copying font information.
     */
    public void copyIntoGraphicsState(PDGraphicsState gs) throws IOException
    {
        for (COSName key : getCOSObject().keySet())
        {
            if (key.equals(COSName.LW))
            {
                gs.setLineWidth(defaultIfNull(getLineWidth(), 1));
            }
            else if (key.equals(COSName.LC))
            {
                gs.setLineCap(getLineCapStyle());
            }
            else if (key.equals(COSName.LJ))
            {
                gs.setLineJoin(getLineJoinStyle());
            }
            else if (key.equals(COSName.ML))
            {
                gs.setMiterLimit(defaultIfNull(getMiterLimit(), 10));
            }
            else if (key.equals(COSName.D))
            {
                gs.setLineDashPattern(getLineDashPattern());
            }
            else if (key.equals(COSName.RI))
            {
                gs.setRenderingIntent(getRenderingIntent());
            }
            else if (key.equals(COSName.OPM))
            {
                gs.setOverprintMode(defaultIfNull(getOverprintMode(), 0));
            }
            else if (key.equals(COSName.FONT))
            {
                PDFontSetting setting = getFontSetting();
                if (setting != null)
                {
                    gs.getTextState().setFont(setting.getFont());
                    gs.getTextState().setFontSize(setting.getFontSize());
                }
            }
            else if (key.equals(COSName.FL))
            {
                gs.setFlatness(defaultIfNull(getFlatnessTolerance(), 1.0f));
            }
            else if (key.equals(COSName.SM))
            {
                gs.setSmoothness(defaultIfNull(getSmoothnessTolerance(), 0));
            }
            else if (key.equals(COSName.SA))
            {
                gs.setStrokeAdjustment(getAutomaticStrokeAdjustment());
            }
            else if (key.equals(COSName.CA))
            {
                gs.setAlphaConstant(defaultIfNull(getStrokingAlphaConstant(), 1.0f));
            }
            else if (key.equals(COSName.CA_NS))
            {
                gs.setNonStrokeAlphaConstants(defaultIfNull(getNonStrokingAlphaConstant(), 1.0f));
            }
            else if (key.equals(COSName.AIS))
            {
                gs.setAlphaSource(getAlphaSourceFlag());
            }
            else if (key.equals(COSName.TK))
            {
                gs.getTextState().setKnockoutFlag(getTextKnockoutFlag());
            }
            else if (key.equals(COSName.SMASK))
            {
                PDSoftMask softmask = getSoftMask();
                if (softmask != null)
                {
                    // Softmask must know the CTM at the time the ExtGState is activated. Read
                    // https://bugs.ghostscript.com/show_bug.cgi?id=691157#c7 for a good explanation.
                    softmask.setInitialTransformationMatrix(
                            gs.getCurrentTransformationMatrix().clone());
                }
                gs.setSoftMask(softmask);
            }
            else if (key.equals(COSName.BM))
            {
                gs.setBlendMode(getBlendMode());
            }
            else if (key.equals(COSName.TR))
            {
                if (getCOSObject().containsKey(COSName.TR2))
                {
                    // "If both TR and TR2 are present in the same graphics state parameter dictionary,
                    // TR2 shall take precedence."
                    continue;
                }
                gs.setTransfer(getTransfer());
            }
            else if (key.equals(COSName.TR2))
            {
                gs.setTransfer(getTransfer2());
            }
        }
    }

    private float defaultIfNull(Float val, float fallback)
    {
        return nonNull(val) ? val : fallback;
    }

    /**
     * @return null or the LW value of the dictionary.
     */
    public Float getLineWidth()
    {
        return getFloatItem(COSName.LW);
    }

    public void setLineWidth(Float width)
    {
        setFloatItem(COSName.LW, width);
    }

    /**
     * @return null or the LC value of the dictionary.
     */
    public int getLineCapStyle()
    {
        return getCOSObject().getInt(COSName.LC);
    }

    public void setLineCapStyle(int style)
    {
        getCOSObject().setInt(COSName.LC, style);
    }

    /**
     * @return null or the LJ value in the dictionary.
     */
    public int getLineJoinStyle()
    {
        return getCOSObject().getInt(COSName.LJ);
    }

    public void setLineJoinStyle(int style)
    {
        getCOSObject().setInt(COSName.LJ, style);
    }

    /**
     * @return null or the ML value in the dictionary.
     */
    public Float getMiterLimit()
    {
        return getFloatItem(COSName.ML);
    }

    public void setMiterLimit(Float miterLimit)
    {
        setFloatItem(COSName.ML, miterLimit);
    }

    /**
     * @return null or the D value in the dictionary.
     */
    public PDLineDashPattern getLineDashPattern()
    {
        COSArray dp = getCOSObject().getDictionaryObject(COSName.D, COSArray.class);
        if (nonNull(dp) && dp.size() == 2)
        {
            if (dp.getObject(0) instanceof COSArray dashArray && dp.getObject(
                    1) instanceof COSNumber phase)
            {
                return new PDLineDashPattern(dashArray, phase.intValue());
            }
        }
        return null;
    }

    public void setLineDashPattern(PDLineDashPattern dashPattern)
    {
        getCOSObject().setItem(COSName.D, dashPattern.getCOSObject());
    }

    /**
     * @return null or the RI value in the dictionary.
     */
    public RenderingIntent getRenderingIntent()
    {
        String ri = getCOSObject().getNameAsString("RI");
        if (ri != null)
        {
            return RenderingIntent.fromString(ri);
        }
        return null;
    }

    public void setRenderingIntent(String ri)
    {
        getCOSObject().setName("RI", ri);
    }

    /**
     * @return The overprint control or null if one has not been set.
     */
    public boolean getStrokingOverprintControl()
    {
        return getCOSObject().getBoolean(COSName.OP, false);
    }

    public void setStrokingOverprintControl(boolean op)
    {
        getCOSObject().setBoolean(COSName.OP, op);
    }

    /**
     * This will get the overprint control for non stroking operations. If this value is null then the regular overprint
     * control value will be returned.
     *
     * @return The overprint control or null if one has not been set.
     */
    public boolean getNonStrokingOverprintControl()
    {
        return getCOSObject().getBoolean(COSName.OP_NS, getStrokingOverprintControl());
    }

    public void setNonStrokingOverprintControl(boolean op)
    {
        getCOSObject().setBoolean(COSName.OP_NS, op);
    }

    /**
     * @return The overprint control mode or null if one has not been set.
     */
    public Float getOverprintMode()
    {
        return getFloatItem(COSName.OPM);
    }

    public void setOverprintMode(Float overprintMode)
    {
        if (overprintMode == null)
        {
            getCOSObject().removeItem(COSName.OPM);
        }
        else
        {
            getCOSObject().setInt(COSName.OPM, overprintMode.intValue());
        }
    }

    /**
     * @return The font setting.
     */
    public PDFontSetting getFontSetting()
    {
        PDFontSetting setting = null;
        COSBase base = getCOSObject().getDictionaryObject(COSName.FONT);
        if (base instanceof COSArray font)
        {
            setting = new PDFontSetting(font);
        }
        return setting;
    }

    public void setFontSetting(PDFontSetting fs)
    {
        getCOSObject().setItem(COSName.FONT, fs);
    }

    /**
     * @return The flatness tolerance or null if one has not been set.
     */
    public Float getFlatnessTolerance()
    {
        return getFloatItem(COSName.FL);
    }

    public void setFlatnessTolerance(Float flatness)
    {
        setFloatItem(COSName.FL, flatness);
    }

    /**
     * @return The smothness tolerance or null if one has not been set.
     */
    public Float getSmoothnessTolerance()
    {
        return getFloatItem(COSName.SM);
    }

    public void setSmoothnessTolerance(Float smoothness)
    {
        setFloatItem(COSName.SM, smoothness);
    }

    /**
     * @return The automatic stroke adjustment flag or null if one has not been set.
     */
    public boolean getAutomaticStrokeAdjustment()
    {
        return getCOSObject().getBoolean(COSName.SA, false);
    }

    public void setAutomaticStrokeAdjustment(boolean sa)
    {
        getCOSObject().setBoolean(COSName.SA, sa);
    }

    /**
     * @return The stroking alpha constant or null if one has not been set.
     */
    public Float getStrokingAlphaConstant()
    {
        return getFloatItem(COSName.CA);
    }

    public void setStrokingAlphaConstant(Float alpha)
    {
        setFloatItem(COSName.CA, alpha);
    }

    /**
     * @return The non stroking alpha constant or null if one has not been set.
     */
    public Float getNonStrokingAlphaConstant()
    {
        return getFloatItem(COSName.CA_NS);
    }

    public void setNonStrokingAlphaConstant(Float alpha)
    {
        setFloatItem(COSName.CA_NS, alpha);
    }

    /**
     * This will get the alpha source flag (“alpha is shape”), that specifies whether the current soft mask and alpha
     * constant shall be interpreted as shape values (true) or opacity values (false).
     *
     * @return The alpha source flag.
     */
    public boolean getAlphaSourceFlag()
    {
        return getCOSObject().getBoolean(COSName.AIS, false);
    }

    public void setAlphaSourceFlag(boolean alpha)
    {
        getCOSObject().setBoolean(COSName.AIS, alpha);
    }

    /**
     * @return the blending mode
     */
    public BlendMode getBlendMode()
    {
        return BlendMode.getInstance(getCOSObject().getDictionaryObject(COSName.BM));
    }

    public void setBlendMode(BlendMode bm)
    {
        getCOSObject().setItem(COSName.BM, BlendMode.getCOSName(bm));
    }

    /**
     * @return the soft mask
     */
    public PDSoftMask getSoftMask()
    {
        if (!getCOSObject().containsKey(COSName.SMASK))
        {
            return null;
        }
        return PDSoftMask.create(getCOSObject().getDictionaryObject(COSName.SMASK));
    }

    /**
     * @return The text knockout flag.
     */
    public boolean getTextKnockoutFlag()
    {
        return getCOSObject().getBoolean(COSName.TK, true);
    }

    public void setTextKnockoutFlag(boolean tk)
    {
        getCOSObject().setBoolean(COSName.TK, tk);
    }

    /**
     * This will get a float item from the dictionary.
     *
     * @param key The key to the item.
     * @return The value for that item.
     */
    private Float getFloatItem(COSName key)
    {
        return ofNullable(getCOSObject().getDictionaryObject(key, COSNumber.class)).map(
                        COSNumber::floatValue)
                .orElse(null);
    }

    /**
     * This will set a float object.
     *
     * @param key The key to the data that we are setting.
     * @param value The value that we are setting.
     */
    private void setFloatItem(COSName key, Float value)
    {
        if (value == null)
        {
            getCOSObject().removeItem(key);
        }
        else
        {
            getCOSObject().setItem(key, new COSFloat(value));
        }
    }

    /**
     * This will get the transfer function of the /TR dictionary.
     *
     * @return The transfer function. According to the PDF specification, this is either a single function (which
     * applies to all process colorants) or an array of four functions (which apply to the process colorants
     * individually). The name Identity may be used to represent the identity function.
     */
    public COSBase getTransfer()
    {
        COSBase base = getCOSObject().getDictionaryObject(COSName.TR);
        if (base instanceof COSArray a && a.size() != 4)
        {
            return null;
        }
        return base;
    }

    /**
     * This will set the transfer function of the /TR dictionary.
     *
     * @param transfer The transfer function. According to the PDF specification, this is either a single function
     * (which applies to all process colorants) or an array of four functions (which apply to the process colorants
     * individually). The name Identity may be used to represent the identity function.
     */
    public void setTransfer(COSBase transfer)
    {
        getCOSObject().setItem(COSName.TR, transfer);
    }

    /**
     * This will get the transfer function of the /TR2 dictionary.
     *
     * @return The transfer function. According to the PDF specification, this is either a single function (which
     * applies to all process colorants) or an array of four functions (which apply to the process colorants
     * individually). The name Identity may be used to represent the identity function, and the name Default denotes the
     * transfer function that was in effect at the start of the page.
     */
    public COSBase getTransfer2()
    {
        COSBase base = getCOSObject().getDictionaryObject(COSName.TR2);
        if (base instanceof COSArray a && a.size() != 4)
        {
            return null;
        }
        return base;
    }

    /**
     * This will set the transfer function of the /TR2 dictionary.
     *
     * @param transfer2 The transfer function. According to the PDF specification, this is either a single function
     * (which applies to all process colorants) or an array of four functions (which apply to the process colorants
     * individually). The name Identity may be used to represent the identity function, and the name Default denotes the
     * transfer function that was in effect at the start of the page.
     */
    public void setTransfer2(COSBase transfer2)
    {
        getCOSObject().setItem(COSName.TR2, transfer2);
    }
}
