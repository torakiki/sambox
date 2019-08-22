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

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSArrayList;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSInteger;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.pdmodel.interactive.action.PDFormFieldAdditionalActions;
import org.sejda.sambox.pdmodel.interactive.annotation.PDAnnotationWidget;

/**
 * A field in an interactive form. Fields may be one of four types: button, text, choice, or signature.
 *
 * @author sug
 */
public abstract class PDTerminalField extends PDField
{
    /**
     * Constructor.
     * 
     * @param acroForm The form that this field is part of.
     */
    protected PDTerminalField(PDAcroForm acroForm)
    {
        super(acroForm);
    }

    /**
     * Constructor.
     * 
     * @param acroForm The form that this field is part of.
     * @param field the PDF object to represent as a field.
     * @param parent the parent node of the node
     */
    PDTerminalField(PDAcroForm acroForm, COSDictionary field, PDNonTerminalField parent)
    {
        super(acroForm, field, parent);
    }

    /**
     * Set the actions of the field.
     * 
     * @param actions The field actions.
     */
    public void setActions(PDFormFieldAdditionalActions actions)
    {
        getCOSObject().setItem(COSName.AA, actions);
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
        else if (getParent() != null)
        {
            retval = getParent().getFieldFlags();
        }
        return retval;
    }

    @Override
    public String getFieldType()
    {
        String fieldType = getCOSObject().getNameAsString(COSName.FT);
        if (fieldType == null && getParent() != null)
        {
            fieldType = getParent().getFieldType();
        }
        return fieldType;
    }

    /**
     * Returns the widget annotations associated with this field.
     * 
     * @return The list of widget annotations.
     */
    @Override
    public List<PDAnnotationWidget> getWidgets()
    {
        COSArray kids = getCOSObject().getDictionaryObject(COSName.KIDS, COSArray.class);
        if (isNull(kids))
        {
            // the field itself is a widget
            return Arrays.asList(new PDAnnotationWidget(getCOSObject()));
        }
        if (kids.size() > 0)
        {
            return kids.stream().filter(k -> nonNull(k)).map(k -> k.getCOSObject())
                    .filter(k -> k instanceof COSDictionary)
                    .map(k -> new PDAnnotationWidget((COSDictionary) k))
                    .collect(Collectors.toList());

        }
        return Collections.emptyList();
    }

    /**
     * Adds the given widget as child of this fields, only if not already present
     * 
     * @param widget
     */
    public void addWidgetIfMissing(PDAnnotationWidget widget)
    {
        if (nonNull(widget))
        {
            COSArray kids = (COSArray) getCOSObject().getDictionaryObject(COSName.KIDS);
            if (kids == null)
            {
                kids = new COSArray(widget.getCOSObject());
                widget.getCOSObject().setItem(COSName.PARENT, this);
                getCOSObject().setItem(COSName.KIDS, kids);
            }
            else if (!kids.contains(widget.getCOSObject()))
            {
                kids.add(widget.getCOSObject());
                widget.getCOSObject().setItem(COSName.PARENT, this);
            }
        }
    }

    /**
     * Sets the field's widget annotations.
     *
     * @param children The list of widget annotations.
     */
    public void setWidgets(List<PDAnnotationWidget> children)
    {
        getCOSObject().setItem(COSName.KIDS, COSArrayList.converterToCOSArray(children));
        for (PDAnnotationWidget widget : children)
        {
            widget.getCOSObject().setItem(COSName.PARENT, this);
        }
    }

    /**
     * Applies a value change to the field. Generates appearances if required and raises events.
     * 
     * @throws IOException if the appearance couldn't be generated
     */
    public final void applyChange() throws IOException
    {
        constructAppearances();
        // if we supported JavaScript we would raise a field changed event here
    }

    /**
     * Constructs appearance streams and appearance dictionaries for all widget annotations. Subclasses should not call
     * this method directly but via {@link #applyChange()}.
     * 
     * @throws IOException if the appearance couldn't be generated
     */
    abstract void constructAppearances() throws IOException;

    @Override
    public boolean isTerminal()
    {
        return true;
    }
}
