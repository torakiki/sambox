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
import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSNumber;
import org.sejda.sambox.pdmodel.common.PDDictionaryWrapper;
import org.sejda.sambox.pdmodel.interactive.action.PDFormFieldAdditionalActions;
import org.sejda.sambox.pdmodel.interactive.annotation.PDAnnotationWidget;

import java.io.IOException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import static java.util.Optional.ofNullable;
import static org.sejda.commons.util.RequireUtils.requireArg;

/**
 * A field in an interactive form.
 */
public abstract class PDField extends PDDictionaryWrapper
{
    private static final int FLAG_READ_ONLY = 1;
    private static final int FLAG_REQUIRED = 1 << 1;
    private static final int FLAG_NO_EXPORT = 1 << 2;

    private final PDAcroForm acroForm;
    private final PDNonTerminalField parent;

    /**
     * Constructor.
     *
     * @param acroForm The form that this field is part of.
     */
    PDField(PDAcroForm acroForm)
    {
        this(acroForm, new COSDictionary(), null);
    }

    /**
     * Constructor.
     *
     * @param acroForm   The form that this field is part of.
     * @param dictionary the PDF object to represent as a field.
     * @param parent     the parent node of the node
     */
    PDField(PDAcroForm acroForm, COSDictionary dictionary, PDNonTerminalField parent)
    {
        super(dictionary);
        this.acroForm = acroForm;
        this.parent = parent;
    }

    /**
     * Creates a COSField subclass from the given COS field. This is for reading fields from PDFs.
     *
     * @param form   the form that the field is part of
     * @param field  the dictionary representing a field element
     * @param parent the parent node of the node to be created, or null if root.
     * @return a new PDField instance
     */
    static PDField fromDictionary(PDAcroForm form, COSDictionary field, PDNonTerminalField parent)
    {
        return PDFieldFactory.createField(form, field, parent);
    }

    /**
     * Returns the given attribute, inheriting from parent nodes if necessary.
     *
     * @param key the key to look up
     * @return COS value for the given key
     */
    protected COSBase getInheritableAttribute(COSName key)
    {
        if (getCOSObject().containsKey(key))
        {
            return getCOSObject().getDictionaryObject(key);
        }
        if (parent != null)
        {
            return parent.getInheritableAttribute(key);
        }
        return acroForm.getCOSObject().getDictionaryObject(key);
    }

    /**
     * Get the FT entry of the field. This is a read only field and is set depending on the actual
     * type. The field type is an inheritable attribute.
     *
     * @return The Field type.
     */
    public abstract String getFieldType();

    /**
     * Returns a string representation of the "V" entry, or an empty string.
     *
     * @return A non-null string.
     */
    public abstract String getValueAsString();

    /**
     * Sets the value of the field.
     *
     * @param value the new field value.
     * @throws IOException if the value could not be set
     */
    public abstract void setValue(String value) throws IOException;

    /**
     * Returns the widget annotations associated with this field.
     * <p>
     * For {@link PDNonTerminalField} the list will be empty as non terminal fields have no visual
     * representation in the form.
     *
     * @return A non-null string.
     */
    public abstract List<PDAnnotationWidget> getWidgets();

    /**
     * sets the field to be read-only.
     *
     * @param readonly The new flag for readonly.
     */
    public void setReadonly(boolean readonly)
    {
        setFieldFlag(FLAG_READ_ONLY, readonly);
    }

    /**
     * @return true if the field is readonly
     */
    public boolean isReadonly()
    {
        return isFieldFlagSet(FLAG_READ_ONLY);
    }

    /**
     * sets the field to be required.
     *
     * @param required The new flag for required.
     */
    public void setRequired(boolean required)
    {
        setFieldFlag(FLAG_REQUIRED, required);
    }

    /**
     * @return true if the field is required
     */
    public boolean isRequired()
    {
        return isFieldFlagSet(FLAG_REQUIRED);
    }

    /**
     * sets the field to be not exported.
     *
     * @param noExport The new flag for noExport.
     */
    public void setNoExport(boolean noExport)
    {
        setFieldFlag(FLAG_NO_EXPORT, noExport);
    }

    /**
     * @return true if the field is not to be exported.
     */
    public boolean isNoExport()
    {
        return isFieldFlagSet(FLAG_NO_EXPORT);
    }

    /**
     * Returns the field flags (/Ff) in effect for this field. The flags are inheritable: if the
     * field has no /Ff of its own, the value of the nearest ancestor (walking /Parent) that
     * defines /Ff applies. If no node defines it, the flags are 0. Inherited values are never
     * copied onto this field.
     *
     * @return flags The set of flags.
     */
    public int getFieldFlags()
    {
        return inheritedInt(getCOSObject(), COSName.FF, 0);
    }

    /**
     * @param bitFlag the flag bit(s)
     * @return true if the given bit(s) are set in the flags in effect for this field
     */
    protected boolean isFieldFlagSet(int bitFlag)
    {
        return (getFieldFlags() & bitFlag) == bitFlag;
    }

    /**
     * Sets or clears the given bit(s) for this field. The flags in effect (own or inherited) are
     * the starting point, so the other inherited bits are kept. The result is stored on this
     * field only; ancestors and siblings are not changed. Nothing is written when the field has
     * no /Ff of its own and the requested value is already in effect, so inheritance stays live.
     *
     * @param bitFlag the flag bit(s)
     * @param value   the value the bit(s) should have
     */
    protected void setFieldFlag(int bitFlag, boolean value)
    {
        int current = getFieldFlags();
        int updated = value ? current | bitFlag : current & ~bitFlag;
        if (updated != current || getCOSObject().getDictionaryObject(COSName.FF,
                COSNumber.class) != null)
        {
            getCOSObject().setInt(COSName.FF, updated);
        }
    }

    /**
     * Looks up an inheritable field attribute: the value on the given field dictionary if it
     * defines one, otherwise on the nearest ancestor reached through /Parent. Unlike
     * {@link #getInheritableAttribute(COSName)} this does not fall back to the AcroForm
     * dictionary and does not depend on how the {@link PDField} was obtained.
     *
     * @param field the field dictionary to start from
     * @param key   the inheritable key
     * @param defaultValue returned when no node in the hierarchy defines a numeric value
     * @return the inherited integer value or the default
     */
    static int inheritedInt(COSDictionary field, COSName key, int defaultValue)
    {
        Set<COSDictionary> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        COSDictionary current = field;
        while (current != null && visited.add(current))
        {
            COSNumber value = current.getDictionaryObject(key, COSNumber.class);
            if (value != null)
            {
                return value.intValue();
            }
            current = current.getDictionaryObject(COSName.PARENT, COSDictionary.class);
        }
        return defaultValue;
    }

    /**
     * @return true if the given field dictionary or one of its ancestors defines a numeric value
     * for the key
     */
    static boolean definesInherited(COSDictionary field, COSName key)
    {
        return inheritedInt(field, key, Integer.MIN_VALUE) != Integer.MIN_VALUE;
    }

    /**
     * This will set the flags for this field.
     *
     * @param flags The new flags.
     */
    public void setFieldFlags(int flags)
    {
        getCOSObject().setInt(COSName.FF, flags);
    }

    /**
     * Get the additional actions for this field. This will return null if there are no additional
     * actions for this field.
     *
     * @return The actions of the field.
     */
    public PDFormFieldAdditionalActions getActions()
    {
        return ofNullable(getCOSObject().getDictionaryObject(COSName.AA, COSDictionary.class)).map(
                PDFormFieldAdditionalActions::new).orElse(null);
    }

    /**
     * Get the parent field to this field, or null if none exists.
     *
     * @return The parent field.
     */
    public PDNonTerminalField getParent()
    {
        return parent;
    }

    /**
     * This will find one of the child elements. The name array are the components of the name to
     * search down the tree of names. The nameIndex is where to start in that array. This method is
     * called recursively until it finds the end point based on the name array.
     *
     * @param name      An array that picks the path to the field.
     * @param nameIndex The index into the array.
     * @return The field at the endpoint or null if none is found.
     */
    PDField findKid(String[] name, int nameIndex)
    {
        PDField retval = null;
        COSArray kids = (COSArray) getCOSObject().getDictionaryObject(COSName.KIDS);
        if (kids != null)
        {
            for (int i = 0; retval == null && i < kids.size(); i++)
            {
                COSDictionary kidDictionary = (COSDictionary) kids.getObject(i);
                if (name[nameIndex].equals(kidDictionary.getString(COSName.T)))
                {
                    retval = PDField.fromDictionary(acroForm, kidDictionary,
                            (PDNonTerminalField) this);
                    if (name.length > nameIndex + 1)
                    {
                        retval = retval.findKid(name, nameIndex + 1);
                    }
                }
            }
        }
        return retval;
    }

    /**
     * This will get the acroform that this field is part of.
     *
     * @return The form this field is on.
     */
    public PDAcroForm getAcroForm()
    {
        return acroForm;
    }

    /**
     * Returns the partial name of the field.
     *
     * @return the name of the field
     */
    public String getPartialName()
    {
        return getCOSObject().getString(COSName.T);
    }

    /**
     * This will set the partial name of the field.
     *
     * @param name The new name for the field.
     */
    public void setPartialName(String name)
    {
        requireArg(!name.contains("."),
                "A field partial name shall not contain a period character: " + name);
        getCOSObject().setString(COSName.T, name);
    }

    /**
     * Returns the fully qualified name of the field, which is a concatenation of the names of all
     * the parents fields.
     *
     * @return the name of the field
     */
    public String getFullyQualifiedName()
    {
        String finalName = getPartialName();
        String parentName = parent != null ? parent.getFullyQualifiedName() : null;
        if (parentName != null)
        {
            if (finalName != null)
            {
                finalName = parentName + "." + finalName;
            }
            else
            {
                finalName = parentName;
            }
        }
        return finalName;
    }

    /**
     * Gets the alternate name of the field ("shall be used in place of the actual field name
     * wherever the field shall be identified in the user interface (such as in error or status
     * messages referring to the field)").
     *
     * @return the alternate name of the field
     */
    public String getAlternateFieldName()
    {
        return getCOSObject().getString(COSName.TU);
    }

    /**
     * This will set the alternate name of the field ("shall be used in place of the actual field
     * name wherever the field shall be identified in the user interface (such as in error or status
     * messages referring to the field)"). The text appears as a tool tip in Adobe Reader. Because
     * of the usage for error or status messages, it should be different for each field.
     *
     * @param alternateFieldName the alternate name of the field
     */
    public void setAlternateFieldName(String alternateFieldName)
    {
        getCOSObject().setString(COSName.TU, alternateFieldName);
    }

    /**
     * Gets the mapping name of the field.
     * <p>
     * The mapping name shall be used when exporting interactive form field data from the document.
     *
     * @return the mapping name of the field
     */
    public String getMappingName()
    {
        return getCOSObject().getString(COSName.TM);
    }

    /**
     * This will set the mapping name of the field.
     *
     * @param mappingName the mapping name of the field
     */
    public void setMappingName(String mappingName)
    {
        getCOSObject().setString(COSName.TM, mappingName);
    }

    public abstract boolean isTerminal();

    @Override
    public String toString()
    {
        return getFullyQualifiedName() + "{type: " + getClass().getSimpleName() + " value: "
                + getInheritableAttribute(COSName.V) + "}";
    }
}
