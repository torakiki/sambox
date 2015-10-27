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
import java.util.Optional;

import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.pdmodel.interactive.annotation.PDAnnotationWidget;

/**
 * A signature field is a form field that contains a digital signature.
 *
 * @author Ben Litchfield
 * @author Thomas Chojecki
 */
// TODO this is currently just an empty shell to distinguish the type of a field
public class PDSignatureField extends PDTerminalField
{
    /**
     * @see PDTerminalField#PDTerminalField(PDAcroForm)
     *
     * @param acroForm The acroForm for this field.
     * @throws IOException If there is an error while resolving partial name for the signature field or getting the
     * widget object.
     */
    public PDSignatureField(PDAcroForm acroForm)
    {
        super(acroForm);
        getCOSObject().setItem(COSName.FT, COSName.SIG);
        getWidgets().get(0).setLocked(true);
        getWidgets().get(0).setPrinted(true);
    }

    /**
     * Constructor.
     * 
     * @param acroForm The form that this field is part of.
     * @param field the PDF object to represent as a field.
     * @param parent the parent node of the node to be created
     */
    PDSignatureField(PDAcroForm acroForm, COSDictionary field, PDNonTerminalField parent)
    {
        super(acroForm, field, parent);
    }

    @Override
    public String getValueAsString()
    {
        return Optional.ofNullable(getCOSObject().getDictionaryObject(COSName.V))
                .map(COSBase::toString).orElse("");
    }

    @Override
    void constructAppearances()
    {
        PDAnnotationWidget widget = this.getWidgets().get(0);
        if (widget != null)
        {
            // check if the signature is visible
            if (widget.getRectangle() == null
                    || widget.getRectangle().getHeight() == 0
                            && widget.getRectangle().getWidth() == 0
                    || widget.isNoView() || widget.isHidden())
            {
                return;
            }

            // TODO: implement appearance generation for signatures
            throw new UnsupportedOperationException("not implemented");
        }
    }

    /**
     * Sets the value of this field.
     * 
     * <b>This will throw an UnsupportedOperationException if used as the signature fields value can't be set using a
     * String</>
     * 
     * @param value the plain text value.
     * 
     * @throws UnsupportedOperationException in all cases!
     */
    @Override
    public void setValue(String value) throws UnsupportedOperationException
    {
        throw new UnsupportedOperationException(
                "Signature fields don't support setting the value as String "
                        + "- use setValue(PDSignature value) instead");
    }
}
