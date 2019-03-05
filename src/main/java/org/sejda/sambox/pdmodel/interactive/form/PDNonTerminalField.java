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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSArrayList;
import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSInteger;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A non terminal field in an interactive form.
 * 
 * A non terminal field is a node in the fields tree node whose descendants are fields.
 * 
 * The attributes such as FT (field type) or V (field value) do not logically belong to the non terminal field but are
 * inheritable attributes for descendant terminal fields.
 */
public class PDNonTerminalField extends PDField
{

    private static final Logger LOG = LoggerFactory.getLogger(PDNonTerminalField.class);

    /**
     * Constructor.
     * 
     * @param acroForm The form that this field is part of.
     */
    public PDNonTerminalField(PDAcroForm acroForm)
    {
        super(acroForm);
    }

    /**
     * Constructor.
     * 
     * @param acroForm The form that this field is part of.
     * @param field the PDF object to represent as a field.
     * @param parent the parent node of the node to be created
     */
    PDNonTerminalField(PDAcroForm acroForm, COSDictionary field, PDNonTerminalField parent)
    {
        super(acroForm, field, parent);
    }

    @Override
    public int getFieldFlags()
    {
        int retval = 0;
        COSInteger ff = (COSInteger) getCOSObject().getDictionaryObject(COSName.FF);
        if (ff != null)
        {
            retval = ff.intValue();
        }
        // There is no need to look up the parent hierarchy within a non terminal field
        return retval;
    }

    /**
     * @return this field's children. These may be either terminal or non-terminal fields.
     */
    public List<PDField> getChildren()
    {
        List<PDField> children = new ArrayList<>();
        COSArray kids = getCOSObject().getDictionaryObject(COSName.KIDS, COSArray.class);
        if (kids != null)
        {
            for (COSBase kid : kids)
            {
                if (nonNull(kid) && kid.getCOSObject() instanceof COSDictionary)
                {
                    if (kid.getCOSObject() == this.getCOSObject())
                    {
                        LOG.warn("Child field is same object as parent");
                        continue;
                    }
                    children.add(PDField.fromDictionary(getAcroForm(),
                            (COSDictionary) kid.getCOSObject(), this));
                }
            }
        }
        return children;
    }

    /**
     * 
     * @return true if the field has at least one child
     */
    public boolean hasChildren()
    {
        return getChildren().size() > 0;
    }

    /**
     * Sets the child fields.
     *
     * @param children The list of child fields.
     */
    public void setChildren(List<PDField> children)
    {
        getCOSObject().setItem(COSName.KIDS, COSArrayList.converterToCOSArray(children));
    }

    /**
     * Adds a child to the array of children
     * 
     * @param field
     */
    public void addChild(PDField field)
    {
        COSArray kids = (COSArray) getCOSObject().getDictionaryObject(COSName.KIDS);
        if (kids == null)
        {
            kids = new COSArray();
        }
        if (!kids.contains(field))
        {
            kids.add(field);
            field.getCOSObject().setItem(COSName.PARENT, this);
            getCOSObject().setItem(COSName.KIDS, kids);
        }
    }

    /**
     * Removes the given node from the children list
     * 
     * @param field
     * @return the removed COSBase or null
     */
    public COSBase removeChild(PDField field)
    {
        COSArray kids = getCOSObject().getDictionaryObject(COSName.KIDS, COSArray.class);
        if (nonNull(kids))
        {
            int removeIdx = kids.indexOfObject(field.getCOSObject());
            if (removeIdx >= 0)
            {
                return kids.remove(removeIdx);
            }
        }
        return null;
    }

    /**
     * <p>
     * <b>Note:</b> while non-terminal fields <b>do</b> inherit field values, this method returns the local value,
     * without inheritance.
     */
    @Override
    public String getFieldType()
    {
        return getCOSObject().getNameAsString(COSName.FT);
    }

    /**
     * <p>
     * <b>Note:</b> while non-terminal fields <b>do</b> inherit field values, this method returns the local value,
     * without inheritance.
     */
    public COSBase getValue()
    {
        return getCOSObject().getDictionaryObject(COSName.V);
    }

    /**
     * <p>
     * <b>Note:</b> while non-terminal fields <b>do</b> inherit field values, this method returns the local value,
     * without inheritance.
     */
    @Override
    public String getValueAsString()
    {
        return getCOSObject().getDictionaryObject(COSName.V).toString();
    }

    /**
     * Sets the value of this field. This may be of any kind which is valid for this field's children.
     *
     * <p>
     * <b>Note:</b> while non-terminal fields <b>do</b> inherit field values, this method returns the local value,
     * without inheritance.
     */
    public void setValue(COSBase object)
    {
        getCOSObject().setItem(COSName.V, object);
        // todo: propagate change event to children?
        // todo: construct appearances of children?
    }

    /**
     * Sets the plain text value of this field.
     * 
     * @param value Plain text
     * @throws IOException if the value could not be set
     */
    @Override
    public void setValue(String value)
    {
        getCOSObject().setString(COSName.V, value);
        // todo: propagate change event to children?
        // todo: construct appearances of children?
    }

    /**
     * Returns the default value of this field. This may be of any kind which is valid for this field's children.
     *
     * <p>
     * <b>Note:</b> while non-terminal fields <b>do</b> inherit field values, this method returns the local value,
     * without inheritance.
     */
    public COSBase getDefaultValue()
    {
        return getCOSObject().getDictionaryObject(COSName.DV);
    }

    /**
     * Sets the default of this field. This may be of any kind which is valid for this field's children.
     *
     * <p>
     * <b>Note:</b> while non-terminal fields <b>do</b> inherit field values, this method returns the local value,
     * without inheritance.
     */
    public void setDefaultValue(COSBase value)
    {
        getCOSObject().setItem(COSName.V, value);
    }

    @Override
    public List<PDAnnotationWidget> getWidgets()
    {
        return Collections.unmodifiableList(Collections.emptyList());
    }

    @Override
    public boolean isTerminal()
    {
        return false;
    }
}
