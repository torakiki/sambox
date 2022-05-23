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

import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSArrayList;
import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSString;
import org.sejda.sambox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.sejda.sambox.pdmodel.interactive.annotation.PDAppearanceDictionary;
import org.sejda.sambox.pdmodel.interactive.annotation.PDAppearanceEntry;
import org.sejda.sambox.pdmodel.interactive.annotation.PDAppearanceStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A button field represents an interactive control on the screen that the user can manipulate with
 * the mouse.
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
     * A Ff flag. If set, radio buttons individual fields, using the same value for the on state
     * will turn on and off in unison.
     */
    static final int FLAG_RADIOS_IN_UNISON = 1 << 25;
    
    // SAMBOX SPECIFIC
    // PDFBox accepts field.setValue("Export option 1") and does some mapping between option/value
    // this mapping makes a lot of assumptions and breaks in several cases: 
    // 1) when widgets.size != exportOptions.size
    // 2) when options are numeric and match the appearance states, eg: export options /Opt = ["0", "1"]
    
    // We add an option to perform setValue()/getValue() by always passing the final dict /V value,
    // (the appearance state value), and ignoring any /Opt mapping
    private boolean ignoreExportOptions = false;

    /**
     * @param acroForm The acroform.
     * @see PDField#PDField(PDAcroForm)
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
     * @deprecated use {@link org.sejda.sambox.pdmodel.interactive.form.PDPushButton} instead
     * @param pushbutton if true the button field is treated as a push button field.
     */
    @Deprecated
    public void setPushButton(boolean pushbutton)
    {
        getCOSObject().setFlag(COSName.FF, FLAG_PUSHBUTTON, pushbutton);
        if (pushbutton)
        {
            setRadioButton(false);
        }
    }

    /**
     * Determines if radio button bit is set.
     *
     * @return true if type of button field is a radio button.
     */
    public boolean isRadioButton()
    {
        return getCOSObject().getFlag(COSName.FF, FLAG_RADIO);
    }

    /**
     * Set the radio button bit.
     *
     * @deprecated use {@link org.sejda.sambox.pdmodel.interactive.form.PDRadioButton} instead
     * @param radiobutton if true the button field is treated as a radio button field.
     */
    @Deprecated
    public void setRadioButton(boolean radiobutton)
    {
        getCOSObject().setFlag(COSName.FF, FLAG_RADIO, radiobutton);
        if (radiobutton)
        {
            setPushButton(false);
        }
    }

    /**
     * Returns the selected value.
     *
     * <p>Off is the default value which will also be returned if the
     * value hasn't been set at all.
     *
     * @return A non-null string.
     */
    public String getValue()
    {
        COSBase value = getInheritableAttribute(COSName.V);
        if (value instanceof COSName)
        {
            String stringValue = ((COSName)value).getName();
            
            if(ignoreExportOptions)
            {
                return stringValue;
            }
            
            List<String> exportValues = getExportValues();
            if (!exportValues.isEmpty())
            {
                try
                {
                    int idx = Integer.parseInt(stringValue, 10);
                    if (idx >= 0 && idx < exportValues.size())
                    {
                        return exportValues.get(idx);
                    }
                }
                catch (NumberFormatException nfe)
                {
                    return stringValue;
                }
            }
            return stringValue;
        }

        // Off is the default value if there is nothing else set.
        // See PDF Spec.
        return "Off";
    }

    /**
     * Sets the selected option given its name. It also tries to update the visual appearance,
     * unless {@link PDAcroForm#isNeedAppearances()} is true.
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
        boolean hasExportValues = getExportValues().size() > 0;

        if (hasExportValues && !ignoreExportOptions) {
            updateByOption(value);
        }
        else
        {
            updateByValue(value);
        }

        applyChange();
    }

    /**
     * Set the selected option given its index, and try to update the visual appearance.
     * <p>
     * NOTE: this method is only usable if there are export values and used for radio buttons with
     * FLAG_RADIOS_IN_UNISON not set.
     *
     * @param index index of option to be selected
     * @throws IOException if the value could not be set
     * @throws IllegalArgumentException if the index provided is not a valid index.
     */
    public void setValue(int index) throws IOException
    {
        if (getExportValues().isEmpty() || index < 0 || index >= getExportValues().size())
        {
            throw new IllegalArgumentException(
                    "index '" + index + "' is not a valid index for the field "
                            + getFullyQualifiedName() + ", valid indices are from 0 to " + (
                            getExportValues().size() - 1));
        }

        updateByValue(String.valueOf(index));

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
            return ((COSName)value).getName();
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
     * @return List containing all possible export values. If there is no Opt entry an empty list
     * will be returned.
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
            return COSArrayList.convertCOSStringCOSArrayToList((COSArray)value);
        }
        return Collections.emptyList();
    }

    /**
     * This will set the export values.
     *
     * @param values List containing all possible export values. Supplying null or an empty list
     *               will remove the Opt entry.
     * @see #getExportValues()
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
        if (exportValues.size() > 0 && !ignoreExportOptions)
        {
            // the value is the index value of the option. So we need to get that
            // and use it to set the value
            try
            {
                int optionsIndex = Integer.parseInt(getValue());
                if (optionsIndex < exportValues.size())
                {
                    updateByOption(exportValues.get(optionsIndex));
                }
            } catch (NumberFormatException e)
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
     * The On value could be an arbitrary string as long as it is within the limitations of a PDF
     * name object. The Off value shall always be 'Off'. If not set or not part of the normal
     * appearance keys 'Off' is the default
     * </p>
     *
     * @return the potential values setting the check box to the On state. If an empty Set is
     * returned there is no appearance definition.
     */
    public Set<String> getOnValues()
    {
        Set<String> onValues = new HashSet<>();
        if(!ignoreExportOptions) 
        {
            onValues.addAll(getExportValues());
        }
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
    public String getOnValueForWidget(PDAnnotationWidget widget)
    {
        PDAppearanceDictionary apDictionary = widget.getAppearance();
        if (apDictionary != null)
        {
            PDAppearanceEntry normalAppearance = apDictionary.getNormalAppearance();
            if (normalAppearance != null && normalAppearance.isSubDictionary())
            {
                // SAMBOX specific: there can be more than one entry, besides "Off"
                // pick the first one alphabetically, so it's consistently returning the same value each time
                Map<COSName, PDAppearanceStream> subDictionary = normalAppearance.getSubDictionary();
                SortedSet<COSName> entries = new TreeSet<>(subDictionary.keySet());
                for (COSName entry : entries)
                {
                    if (COSName.Off.compareTo(entry) != 0)
                    {
                        // SAMBOX: there could be invalid entries in the dict, for example /Bla: String
                        // skip the ones where the value is not a content stream
                        if (entries.size() > 2) // Hm, maybe do this always?
                        {
                            if(subDictionary.get(entry) == null)
                            {
                                continue;
                            }
                        }
                        return entry.getName();
                    }
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
    void checkValue(String value)
    {
        Set<String> onValues = getOnValues();

        if (onValues.isEmpty())
        {
            return;
        }

        if (COSName.Off.getName().compareTo(value) != 0 && !onValues.contains(value))
        {
            throw new IllegalArgumentException(
                    "value '" + value + "' is not a valid option for the field "
                            + getFullyQualifiedName() + ", valid values are: " + onValues + " and "
                            + COSName.Off.getName());
        }
    }

    private void updateByValue(String value)
    {
        getCOSObject().setName(COSName.V, value);
        // update the appearance state (AS)
        for (PDAnnotationWidget widget : getWidgets())
        {
            PDAppearanceDictionary appearance = widget.getAppearance();
            PDAppearanceEntry normalAppearance = null;
            Set<String> entries = new HashSet<>();
            if (appearance != null)
            {
                normalAppearance = widget.getAppearance().getNormalAppearance();

                if (normalAppearance != null && normalAppearance.isSubDictionary())
                {
                    entries = normalAppearance.getSubDictionary().keySet().stream()
                            .map(COSName::getName).collect(Collectors.toSet());

                    if (entries.contains(value))
                    {
                        widget.setAppearanceState(value);
                    }
                    else
                    {
                        widget.setAppearanceState(COSName.Off.getName());
                    }
                }
            }

            // FIX scenario: checkboxes with a malformed normal appearance (eg: stream instead of sub-dictionary with entries)
            // see forms-malformed-checkbox-normal-appearances.pdf
            // otherwise these checkboxes are not rendered at all in Adobe Reader
            // FIX scenario: checkbox that only has an "Off" entry in normal appearances
            if (entries.isEmpty() || (entries.size() == 1 && entries.contains("Off")))
            {
                if(this instanceof PDCheckBox)
                {
                    PDCheckBox checkBox = (PDCheckBox) this;
                    if (checkBox.getOnValue().equals(value)) {
                        // checkbox is being checked
                        widget.setAppearanceState(value);
                    } else {
                        // checkbox being unchecked
                        widget.setAppearanceState(COSName.Off.getName());
                    }
                }

                if(normalAppearance != null && !normalAppearance.isSubDictionary())
                {
                    if(this instanceof PDRadioButton || this instanceof PDCheckBox) 
                    {
                        // checkbox/radio has a single stream normal appearance
                        // this can cause problems, because the checkbox looks the same no matter if checked or not
                        throw new RuntimeException(
                                "Check/radio has a single normal appearance, might look the same when checked or not");
                    }
                }
            }
        }
    }

    private void updateByOption(String value) throws IOException
    {
        List<PDAnnotationWidget> widgets = getWidgets();
        List<String> options = getExportValues();

        if (widgets.size() != options.size())
        {
            throw new IllegalArgumentException("The number of options doesn't match the number of widgets");
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
                updateByValue(getOnValue(optionsIndex));
            }
        }
    }

    public boolean isIgnoreExportOptions() {
        return ignoreExportOptions;
    }

    public void setIgnoreExportOptions(boolean ignoreExportOptions) {
        this.ignoreExportOptions = ignoreExportOptions;
    }
}
