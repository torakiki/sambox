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
package org.sejda.sambox.pdmodel.interactive.form;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSArrayList;
import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSString;
import org.sejda.sambox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.sejda.sambox.pdmodel.interactive.annotation.PDAppearanceDictionary;
import org.sejda.sambox.pdmodel.interactive.annotation.PDAppearanceEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A button field represents an interactive control on the screen that the user can manipulate with the mouse.
 *
 * @author sug
 */
public abstract class PDButton extends PDTerminalField
{

    private static final Logger LOG = LoggerFactory.getLogger(PDButton.class);

    /**
     * A Ff flag. If set, the field is a set of radio buttons
     */
    static final int FLAG_RADIO = 1 << 15;

    /**
     * A Ff flag. If set, the field is a pushbutton.
     */
    static final int FLAG_PUSHBUTTON = 1 << 16;

    /**
     * A Ff flag. If set, radio buttons individual fields, using the same value for the on state will turn on and off in
     * unison.
     */
    static final int FLAG_RADIOS_IN_UNISON = 1 << 25;

    /**
     * @see PDField#PDField(PDAcroForm)
     *
     * @param acroForm The acroform.
     */
    public PDButton(PDAcroForm acroForm)
    {
        super(acroForm);
        getCOSObject().setItem(COSName.FT, COSName.BTN);
    }

    /**
     * Constructor.
     * 
     * @param acroForm The form that this field is part of.
     * @param field the PDF object to represent as a field.
     * @param parent the parent node of the node
     */
    PDButton(PDAcroForm acroForm, COSDictionary field, PDNonTerminalField parent)
    {
        super(acroForm, field, parent);
    }

    /**
     * Determines if push button bit is set.
     * 
     * @return true if type of button field is a push button.
     */
    public boolean isPushButton()
    {
        return getCOSObject().getFlag(COSName.FF, FLAG_PUSHBUTTON);
    }

    /**
     * Set the push button bit.
     *
     * @param pushbutton if true the button field is treated as a push button field.
     */
    public void setPushButton(boolean pushbutton)
    {
        getCOSObject().setFlag(COSName.FF, FLAG_PUSHBUTTON, pushbutton);
    }

    /**
     * Determines if radio button bit is set.
     * 
     * @return true if type of button field is a push button.
     */
    public boolean isRadioButton()
    {
        return getCOSObject().getFlag(COSName.FF, FLAG_RADIO);
    }

    /**
     * Set the radio button bit.
     *
     * @param radiobutton if true the button field is treated as a radio button field.
     */
    public void setRadioButton(boolean radiobutton)
    {
        getCOSObject().setFlag(COSName.FF, FLAG_RADIO, radiobutton);
    }

    /**
     * Returns the selected value. May be empty if NoToggleToOff is set but there is no value selected.
     * 
     * @return A non-null string.
     */
    public String getValue()
    {
        COSBase value = getInheritableAttribute(COSName.V);
        if (value instanceof COSName)
        {
            return ((COSName) value).getName();
        }
        // Off is the default value if there is nothing else set.
        // See PDF Spec.
        return "Off";
    }

    /**
     * Sets the selected option given its name.
     * 
     * @param value Name of option to select
     * @throws IOException if the value could not be set
     * @throws IllegalArgumentException if the value is not a valid option.
     */
    @Override
    public void setValue(String value) throws IOException
    {
        checkValue(value);

        // if there are export values/an Opt entry there is a different
        // approach to setting the value
        List<String> exportValues = getExportValues();

        if (exportValues.contains(value))
        {
            updateByOption(value);
        }
        else
        {
            updateByValue(value);
        }

        applyChange();
    }

    public void setValueIgnoreExportOptions(String value) throws IOException
    {
        checkValue(value);
        updateByValue(value);

        applyChange();
    }

    /**
     * Returns the default value, if any.
     *
     * @return A non-null string.
     */
    public String getDefaultValue()
    {
        COSBase value = getInheritableAttribute(COSName.DV);
        if (value instanceof COSName)
        {
            return ((COSName) value).getName();
        }
        return "";
    }

    /**
     * Sets the default value.
     *
     * @param value Name of option to select
     * @throws IllegalArgumentException if the value is not a valid option.
     */
    public void setDefaultValue(String value)
    {
        checkValue(value);
        getCOSObject().setName(COSName.DV, value);
    }

    @Override
    public String getValueAsString()
    {
        return getValue();
    }

    /**
     * This will get the export values.
     * 
     * <p>
     * The export values are defined in the field dictionaries /Opt key.
     * </p>
     * 
     * <p>
     * The option values are used to define the export values for the field to
     * <ul>
     * <li>hold values in non-Latin writing systems as name objects, which represent the field value, are limited to
     * PDFDocEncoding</li>
     * <li>allow radio buttons having the same export value to be handled independently</li>
     * </ul>
     * </p>
     * 
     * @return List containing all possible export values. If there is no Opt entry an empty list will be returned.
     */
    public List<String> getExportValues()
    {
        COSBase value = getInheritableAttribute(COSName.OPT);
        if (value instanceof COSString)
        {
            List<String> array = new ArrayList<>();
            array.add(((COSString) value).getString());
            return array;
        }
        else if (value instanceof COSArray)
        {
            return COSArrayList.convertCOSStringCOSArrayToList((COSArray) value);
        }
        return Collections.emptyList();
    }

    /**
     * This will set the export values.
     * 
     * @see #getExportValues()
     * @param values List containing all possible export values. Supplying null or an empty list will remove the Opt
     * entry.
     */
    public void setExportValues(List<String> values)
    {
        COSArray cosValues;
        if (values != null && !values.isEmpty())
        {
            cosValues = COSArrayList.convertStringListToCOSStringCOSArray(values);
            getCOSObject().setItem(COSName.OPT, cosValues);
        }
        else
        {
            getCOSObject().removeItem(COSName.OPT);
        }
    }

    @Override
    void constructAppearances() throws IOException
    {
        List<String> exportValues = getExportValues();
        if (exportValues.contains(getValue()))
        {
            // the value is the index value of the option. So we need to get that
            // and use it to set the value
            try
            {
                int optionsIndex = Integer.parseInt(getValue());
                if (optionsIndex >= 0 && optionsIndex < exportValues.size())
                {
                    updateByOption(exportValues.get(optionsIndex));
                }
            }
            catch (NumberFormatException e)
            {
                // silently ignore that
                // and don't update the appearance
            }
        }
        else
        {
            updateByValue(getValue());
        }
    }

    /**
     * Get the values to set individual buttons within a group to the on state.
     *
     * <p>
     * The On value could be an arbitrary string as long as it is within the limitations of a PDF name object. The Off
     * value shall always be 'Off'. If not set or not part of the normal appearance keys 'Off' is the default
     * </p>
     *
     * @return the potential values setting the check box to the On state. If an empty Set is returned there is no
     * appearance definition.
     */
    public Set<String> getOnValues()
    {
        Set<String> onValues = new HashSet<>();
        onValues.addAll(getExportValues());
        onValues.addAll(getNormalAppearanceValues());
        return onValues;
    }

    public List<String> getNormalAppearanceValues()
    {
        List<String> values = new ArrayList<>();
        List<PDAnnotationWidget> widgets = this.getWidgets();
        for (PDAnnotationWidget widget : widgets)
        {
            String value = getOnValueForWidget(widget);
            if (value != null)
            {
                values.add(value);
            }
        }
        return values;
    }

    /*
     * Get the on value for an individual widget by it's index.
     */
    private String getOnValue(int index)
    {
        List<PDAnnotationWidget> widgets = this.getWidgets();
        if (index < widgets.size())
        {
            return getOnValueForWidget(widgets.get(index));
        }
        // PDFBox returns an empty string here.
        // Chose to return null so it's clear it's an undefined value
        // Caller should choose to not act on these values
        return null;
    }

    /*
     * Get the on value for an individual widget.
     */
    private String getOnValueForWidget(PDAnnotationWidget widget)
    {
        PDAppearanceDictionary apDictionary = widget.getAppearance();
        if (apDictionary != null)
        {
            PDAppearanceEntry normalAppearance = apDictionary.getNormalAppearance();
            if (normalAppearance != null)
            {
                try
                {
                    Set<COSName> entries = normalAppearance.getSubDictionary().keySet();
                    for (COSName entry : entries)
                    {
                        if (COSName.Off.compareTo(entry) != 0)
                        {
                            return entry.getName();
                        }
                    }
                }
                catch (IllegalStateException ex)
                {
                    LOG.warn("Could not parse normal appearances sub-dictionary for field {}",
                            this.getFullyQualifiedName());
                }
            }
        }
        // PDFBox returns an empty string here.
        // Chose to return null so it's clear it's an undefined value
        // Caller should choose to not act on these values
        return null;
    }

    /**
     * Checks value.
     *
     * @param value Name of radio button to select
     * @throws IllegalArgumentException if the value is not a valid option.
     */
    void checkValue(String value) throws IllegalArgumentException
    {
        Set<String> onValues = getOnValues();

        if (onValues.isEmpty())
        {
            return;
        }

        if (COSName.Off.getName().compareTo(value) != 0 && !onValues.contains(value))
        {
            throw new IllegalArgumentException("value '" + value
                    + "' is not a valid option for the field " + getFullyQualifiedName()
                    + ", valid values are: " + onValues + " and " + COSName.Off.getName());
        }
    }

    private void updateByValue(String value)
    {
        getCOSObject().setName(COSName.V, value);
        // update the appearance state (AS)
        for (PDAnnotationWidget widget : getWidgets())
        {
            boolean matchesAppearance = false;
            // don't crash when there's no appearances (eg: checkboxes)
            if (widget.getAppearance() != null
                    && widget.getAppearance().getNormalAppearance() != null)
            {
                matchesAppearance = ((COSDictionary) widget.getAppearance().getNormalAppearance()
                        .getCOSObject()).containsKey(value);
            }

            // checkbox with no appearances scenario
            if (!COSName.OFF.getName().equals(value) && widget.getAppearance() == null
                    && getWidgets().size() == 1)
            {
                matchesAppearance = true;
            }

            if (matchesAppearance)
            {
                widget.getCOSObject().setName(COSName.AS, value);
            }
            else
            {
                widget.getCOSObject().setItem(COSName.AS, COSName.Off);
            }
        }

    }

    private void updateByOption(String value) throws IOException
    {
        List<PDAnnotationWidget> widgets = getWidgets();
        List<String> options = getExportValues();
        Set<String> uniqueOptions = new HashSet<>(options);

        if (widgets.size() != options.size() && uniqueOptions.size() > 1)
        {
            throw new IllegalArgumentException(
                    "The number of options doesn't match the number of widgets");
        }

        if (value.equals(COSName.Off.getName()))
        {
            updateByValue(value);
        }
        else
        {
            // the value is the index of the matching option
            int optionsIndex = options.indexOf(value);

            // get the values the options are pointing to as
            // this might not be numerical
            // see PDFBOX-3682
            if (optionsIndex != -1)
            {
                String onValue = getOnValue(optionsIndex);
                if (onValue != null)
                {
                    updateByValue(onValue);
                }
            }
        }
    }
}
