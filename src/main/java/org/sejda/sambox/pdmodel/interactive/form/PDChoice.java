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

import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toCollection;
import static org.sejda.commons.util.RequireUtils.requireArg;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSNumber;
import org.sejda.sambox.cos.COSString;
import org.sejda.sambox.pdmodel.interactive.form.FieldUtils.KeyValue;

/**
 * A choice field contains several text items, one or more of which shall be selected as the field
 * value.
 *
 * @author sug
 * @author John Hewson
 */
public abstract class PDChoice extends PDVariableText
{
    static final int FLAG_COMBO = 1 << 17;

    private static final int FLAG_SORT = 1 << 19;
    private static final int FLAG_MULTI_SELECT = 1 << 21;
    private static final int FLAG_DO_NOT_SPELL_CHECK = 1 << 22;
    private static final int FLAG_COMMIT_ON_SEL_CHANGE = 1 << 26;

    /**
     * @param acroForm The acroform.
     * @see PDField#PDField(PDAcroForm)
     */
    public PDChoice(PDAcroForm acroForm)
    {
        super(acroForm);
        getCOSObject().setItem(COSName.FT, COSName.CH);
    }

    /**
     * Constructor.
     *
     * @param acroForm The form that this field is part of.
     * @param field    the PDF object to represent as a field.
     * @param parent   the parent node of the node
     */
    PDChoice(PDAcroForm acroForm, COSDictionary field, PDNonTerminalField parent)
    {
        super(acroForm, field, parent);
    }

    /**
     * This will get the option values "Opt".
     *
     * <p>
     * For a choice field the options array can either be an array of text strings or an array of a
     * two-element arrays.<br/> The method always only returns either the text strings or, in case
     * of two-element arrays, an array of the first element of the two-element arrays
     * </p>
     * <p>
     * Use {@link #getOptionsExportValues()} and {@link #getOptionsDisplayValues()} to get the
     * entries of two-element arrays.
     * </p>
     *
     * @return List containing the export values.
     */
    public List<String> getOptions()
    {
        return FieldUtils.getPairableItems(getCOSObject().getDictionaryObject(COSName.OPT), 0);
    }

    /**
     * This will set the display values - the 'Opt' key.
     *
     * <p>
     * The Opt array specifies the list of options in the choice field either as an array of text
     * strings representing the display value or as an array of a two-element array where the first
     * element is the export value and the second the display value.
     * </p>
     * <p>
     * To set both the export and the display value use {@link #setOptions(List, List)}
     * </p>
     *
     * @param displayValues List containing all possible options.
     */
    public void setOptions(List<String> displayValues)
    {
        if (displayValues != null && !displayValues.isEmpty())
        {
            if (isSort())
            {
                Collections.sort(displayValues);
            }
            getCOSObject().setItem(COSName.OPT, COSArray.fromStrings(displayValues));
        }
        else
        {
            getCOSObject().removeItem(COSName.OPT);
        }
    }

    /**
     * This will set the display and export values - the 'Opt' key.
     *
     * <p>
     * This will set both, the export value and the display value of the choice field. If either one
     * of the parameters is null or an empty list is supplied the options will be removed.
     * </p>
     * <p>
     * An {@link IllegalArgumentException} will be thrown if the number of items in the list
     * differ.
     * </p>
     *
     * @param exportValues  List containing all possible export values.
     * @param displayValues List containing all possible display values.
     * @see #setOptions(List)
     */
    public void setOptions(List<String> exportValues, List<String> displayValues)
    {
        if (exportValues != null && displayValues != null && !exportValues.isEmpty()
                && !displayValues.isEmpty())
        {
            requireArg(exportValues.size() == displayValues.size(),
                    "The number of entries for exportValue and displayValue shall be the same.");
            List<KeyValue> keyValuePairs = FieldUtils.toKeyValueList(exportValues, displayValues);

            if (isSort())
            {
                FieldUtils.sortByValue(keyValuePairs);
            }

            COSArray options = new COSArray();
            for (int i = 0; i < exportValues.size(); i++)
            {
                COSArray entry = new COSArray();
                entry.add(COSString.parseLiteral(keyValuePairs.get(i).getKey()));
                entry.add(COSString.parseLiteral(keyValuePairs.get(i).getValue()));
                options.add(entry);
            }
            getCOSObject().setItem(COSName.OPT, options);
        }
        else
        {
            getCOSObject().removeItem(COSName.OPT);
        }
    }

    /**
     * This will get the display values from the options.
     *
     * <p>
     * For options with an array of text strings the display value and export value are the
     * same.<br/> For options with an array of two-element arrays the display value is the second
     * entry in the two-element array.
     * </p>
     *
     * @return List containing all the display values.
     */
    public List<String> getOptionsDisplayValues()
    {
        return FieldUtils.getPairableItems(getCOSObject().getDictionaryObject(COSName.OPT), 1);
    }

    /**
     * This will get the export values from the options.
     *
     * <p>
     * For options with an array of text strings the display value and export value are the
     * same.<br/> For options with an array of two-element arrays the export value is the first
     * entry in the two-element array.
     * </p>
     *
     * @return List containing all export values.
     */
    public List<String> getOptionsExportValues()
    {
        return getOptions();
    }

    /**
     * This will get the indices of the selected options - the 'I' key.
     * <p>
     * This is only needed if a choice field allows multiple selections and two different items have
     * the same export value or more than one values is selected.
     * </p>
     * <p>The indices are zero-based</p>
     *
     * @return A mutable list containing the indices of all selected options.
     */
    public List<Integer> getSelectedOptionsIndex()
    {
        COSArray value = getCOSObject().getDictionaryObject(COSName.I, COSArray.class);
        if (nonNull(value))
        {
            return value.stream().map(COSBase::getCOSObject).filter(e -> e instanceof COSNumber)
                    .map(n -> ((COSNumber) n).intValue()).collect(toCollection(ArrayList::new));
        }
        return new ArrayList<>();
    }

    /**
     * This will set the indices of the selected options - the 'I' key.
     * <p>
     * This method is preferred over {@link #setValue(List)} for choice fields which
     * <ul>
     *  <li>do support multiple selections</li>
     *  <li>have export values with the same value</li>
     * </ul>
     * </p>
     * <p>
     * Setting the index will set the value too.
     * </p>
     *
     * @param values List containing the indices of all selected options.
     */
    public void setSelectedOptionsIndex(List<Integer> values)
    {
        if (values != null && !values.isEmpty())
        {
            requireArg(isMultiSelect(),
                    "Setting the indices is not allowed for choice fields not allowing multiple selections.");
            getCOSObject().setItem(COSName.I, COSArray.fromIntegers(values));
        }
        else
        {
            getCOSObject().removeItem(COSName.I);
        }
    }

    /**
     * Determines if Sort is set.
     *
     * <p>
     * If set, the field’s option items shall be sorted alphabetically. The sorting has to be done
     * when writing the PDF. PDF Readers are supposed to display the options in the order in which
     * they occur in the Opt array.
     * </p>
     *
     * @return true if the options are sorted.
     */
    public boolean isSort()
    {
        return getCOSObject().getFlag(COSName.FF, FLAG_SORT);
    }

    /**
     * Set the Sort bit.
     *
     * @param sort The value for Sort.
     * @see #isSort()
     */
    public void setSort(boolean sort)
    {
        getCOSObject().setFlag(COSName.FF, FLAG_SORT, sort);
    }

    /**
     * Determines if MultiSelect is set.
     *
     * @return true if multi select is allowed.
     */
    public boolean isMultiSelect()
    {
        return getCOSObject().getFlag(COSName.FF, FLAG_MULTI_SELECT);
    }

    /**
     * Set the MultiSelect bit.
     *
     * @param multiSelect The value for MultiSelect.
     */
    public void setMultiSelect(boolean multiSelect)
    {
        getCOSObject().setFlag(COSName.FF, FLAG_MULTI_SELECT, multiSelect);
    }

    /**
     * Determines if DoNotSpellCheck is set.
     *
     * @return true if spell checker is disabled.
     */
    public boolean isDoNotSpellCheck()
    {
        return getCOSObject().getFlag(COSName.FF, FLAG_DO_NOT_SPELL_CHECK);
    }

    /**
     * Set the DoNotSpellCheck bit.
     *
     * @param doNotSpellCheck The value for DoNotSpellCheck.
     */
    public void setDoNotSpellCheck(boolean doNotSpellCheck)
    {
        getCOSObject().setFlag(COSName.FF, FLAG_DO_NOT_SPELL_CHECK, doNotSpellCheck);
    }

    /**
     * Determines if CommitOnSelChange is set.
     *
     * @return true if value shall be committed as soon as a selection is made.
     */
    public boolean isCommitOnSelChange()
    {
        return getCOSObject().getFlag(COSName.FF, FLAG_COMMIT_ON_SEL_CHANGE);
    }

    /**
     * Set the CommitOnSelChange bit.
     *
     * @param commitOnSelChange The value for CommitOnSelChange.
     */
    public void setCommitOnSelChange(boolean commitOnSelChange)
    {
        getCOSObject().setFlag(COSName.FF, FLAG_COMMIT_ON_SEL_CHANGE, commitOnSelChange);
    }

    /**
     * Determines if Combo is set.
     *
     * @return true if value the choice is a combo box..
     */
    public boolean isCombo()
    {
        return getCOSObject().getFlag(COSName.FF, FLAG_COMBO);
    }

    /**
     * Set the Combo bit.
     *
     * @param combo The value for Combo.
     */
    public void setCombo(boolean combo)
    {
        getCOSObject().setFlag(COSName.FF, FLAG_COMBO, combo);
    }

    /**
     * Sets the selected value of this field.
     *
     * @param value The name of the selected item.
     * @throws IOException if the value could not be set
     */
    @Override
    public void setValue(String value) throws IOException
    {
        getCOSObject().setString(COSName.V, value);

        // remove I key for single valued choice field
        setSelectedOptionsIndex(null);

        applyChange();
    }

    /**
     * Sets the default value of this field.
     *
     * @param value The name of the selected item.
     * @throws IOException if the value could not be set
     */
    public void setDefaultValue(String value) throws IOException
    {
        getCOSObject().setString(COSName.DV, value);
    }

    /**
     * Sets the entry "V" to the given values. Requires {@link #isMultiSelect()} to be true.
     *
     * @param values the list of values
     */
    public void setValue(List<String> values) throws IOException
    {
        if (values != null && !values.isEmpty())
        {
            requireArg(isMultiSelect(), "The list box does not allow multiple selections.");
            requireArg(getOptions().containsAll(values),
                    "The values are not contained in the selectable options.");
            getCOSObject().setItem(COSName.V, COSArray.fromStrings(values));
            updateSelectedOptionsIndex(values);
        }
        else
        {
            getCOSObject().removeItem(COSName.V);
            getCOSObject().removeItem(COSName.I);
        }
        applyChange();
    }

    /**
     * Returns the selected values, or an empty List. This list always contains a single item unless
     * {@link #isMultiSelect()} is true.
     *
     * @return A non-null string.
     */
    public List<String> getValue()
    {
        return getValueFor(COSName.V);
    }

    /**
     * Returns the default values, or an empty List. This list always contains a single item unless
     * {@link #isMultiSelect()} is true.
     *
     * @return A non-null string.
     */
    public List<String> getDefaultValue()
    {
        return getValueFor(COSName.DV);
    }

    /**
     * @return a mutable list of values for the input  name, or an empty List, for the given key.
     */
    private List<String> getValueFor(COSName name)
    {
        COSBase value = getCOSObject().getDictionaryObject(name);
        if (value instanceof COSString cosString)
        {
            return new ArrayList<>(List.of(cosString.getString()));
        }
        if (value instanceof COSArray cosArray)
        {
            return cosArray.stream().map(COSBase::getCOSObject).filter(e -> e instanceof COSString)
                    .map(n -> ((COSString) n).getString()).collect(toCollection(ArrayList::new));
        }
        return new ArrayList<>();
    }

    @Override
    public String getValueAsString()
    {
        return Arrays.toString(getValue().toArray());
    }

    /**
     * Update the 'I' key based on values set.
     */
    private void updateSelectedOptionsIndex(List<String> values)
    {
        List<String> options = getOptions();
        List<Integer> indices = new ArrayList<>();

        for (String value : values)
        {
            indices.add(options.indexOf(value));
        }

        // Indices have to be "... array of integers, sorted in ascending order ..."
        Collections.sort(indices);
        setSelectedOptionsIndex(indices);
    }

    @Override
    abstract void constructAppearances() throws IOException;
}
