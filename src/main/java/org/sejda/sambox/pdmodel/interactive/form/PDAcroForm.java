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
import static java.util.Optional.ofNullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSArrayList;
import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSString;
import org.sejda.sambox.pdmodel.PDDocument;
import org.sejda.sambox.pdmodel.PDPage;
import org.sejda.sambox.pdmodel.PDPageContentStream;
import org.sejda.sambox.pdmodel.PDPageContentStream.AppendMode;
import org.sejda.sambox.pdmodel.PDResources;
import org.sejda.sambox.pdmodel.common.PDDictionaryWrapper;
import org.sejda.sambox.pdmodel.graphics.form.PDFormXObject;
import org.sejda.sambox.pdmodel.interactive.annotation.PDAnnotation;
import org.sejda.sambox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.sejda.sambox.util.Matrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An interactive form, also known as an AcroForm.
 *
 * @author Ben Litchfield
 */
public final class PDAcroForm extends PDDictionaryWrapper
{
    private static final Logger LOG = LoggerFactory.getLogger(PDAcroForm.class);

    private static final int FLAG_SIGNATURES_EXIST = 1;
    private static final int FLAG_APPEND_ONLY = 1 << 1;

    private final PDDocument document;

    /**
     * @param doc The document that this form is part of.
     */
    public PDAcroForm(PDDocument document)
    {
        this.document = document;
        getCOSObject().setItem(COSName.FIELDS, new COSArray());
    }

    /**
     * @param doc The document that this form is part of.
     * @param form The existing acroForm.
     */
    public PDAcroForm(PDDocument document, COSDictionary form)
    {
        super(form);
        this.document = document;
    }

    /**
     * This will get the document associated with this form.
     *
     * @return The PDF document.
     */
    PDDocument getDocument()
    {
        return document;
    }

    /**
     * This will flatten all form fields.
     * 
     * <p>
     * Flattening a form field will take the current appearance and make that part of the pages content stream. All form
     * fields and annotations associated are removed.
     * </p>
     * 
     * <p>
     * The appearances for the form fields widgets will <strong>not</strong> be generated
     * <p>
     * 
     * @throws IOException
     */
    public void flatten() throws IOException
    {
        // for dynamic XFA forms there is no flatten as this would mean to do a rendering
        // from the XFA content into a static PDF.
        if (xfaIsDynamic())
        {
            LOG.warn("Flatten for a dynamic XFA form is not supported");
            return;
        }
        List<PDField> fields = new ArrayList<>();
        for (PDField field : getFieldTree())
        {
            fields.add(field);
        }
        flatten(fields, false);
    }

    /**
     * This will flatten the specified form fields.
     * 
     * <p>
     * Flattening a form field will take the current appearance and make that part of the pages content stream. All form
     * fields and annotations associated are removed.
     * </p>
     * 
     * @param refreshAppearances if set to true the appearances for the form field widgets will be updated
     * @throws IOException
     */
    public void flatten(List<PDField> fields, boolean refreshAppearances) throws IOException
    {
        // for dynamic XFA forms there is no flatten as this would mean to do a rendering
        // from the XFA content into a static PDF.
        if (xfaIsDynamic())
        {
            LOG.warn("Flatten for a dynamic XFA form is not supported");
            return;
        }
        if (refreshAppearances)
        {
            refreshAppearances(fields);
        }
        // indicates if the original content stream
        // has been wrapped in a q...Q pair.
        boolean isContentStreamWrapped = false;

        // the content stream to write to
        PDPageContentStream contentStream;

        // Hold a reference between the annotations and the page they are on.
        // This will only be used in case a PDAnnotationWidget doesn't contain
        // a /P entry specifying the page it's on as the /P entry is optional
        Map<COSDictionary, Integer> annotationToPageRef = null;

        // Iterate over all form fields and their widgets and create a
        // FormXObject at the page content level from that
        for (PDField field : fields)
        {
            for (PDAnnotationWidget widget : field.getWidgets())
            {
                if (widget.getNormalAppearanceStream() != null)
                {
                    PDPage page = widget.getPage();

                    // resolve the page from looking at the annotations
                    if (widget.getPage() == null)
                    {
                        if (annotationToPageRef == null)
                        {
                            annotationToPageRef = buildAnnotationToPageRef();
                        }
                        Integer pageRef = annotationToPageRef.get(widget.getCOSObject());
                        if (pageRef != null)
                        {
                            page = document.getPage((int) pageRef);
                        }
                    }

                    if (!isContentStreamWrapped)
                    {
                        contentStream = new PDPageContentStream(document, page, AppendMode.APPEND,
                                true, true);
                        isContentStreamWrapped = true;
                    }
                    else
                    {
                        contentStream = new PDPageContentStream(document, page, AppendMode.APPEND,
                                true);
                    }

                    PDFormXObject fieldObject = new PDFormXObject(
                            widget.getNormalAppearanceStream().getCOSObject());

                    Matrix translationMatrix = Matrix.getTranslateInstance(
                            widget.getRectangle().getLowerLeftX(),
                            widget.getRectangle().getLowerLeftY());
                    contentStream.saveGraphicsState();
                    contentStream.transform(translationMatrix);
                    contentStream.drawForm(fieldObject);
                    contentStream.restoreGraphicsState();
                    contentStream.close();
                }
            }
        }

        // preserve all non widget annotations
        for (PDPage page : document.getPages())
        {
            page.setAnnotations(page.getAnnotations().stream()
                    .filter(a -> !(a instanceof PDAnnotationWidget)).collect(Collectors.toList()));
        }

        // remove the fields
        setFields(Collections.<PDField> emptyList());
        // remove XFA for hybrid forms
        getCOSObject().removeItem(COSName.XFA);
    }

    /**
     * Refreshes the appearance streams and appearance dictionaries for the widget annotations of all fields.
     * 
     * @throws IOException
     */
    public void refreshAppearances() throws IOException
    {
        for (PDField field : getFieldTree())
        {
            if (field instanceof PDTerminalField)
            {
                ((PDTerminalField) field).constructAppearances();
            }
        }
    }

    /**
     * Refreshes the appearance streams and appearance dictionaries for the widget annotations of the specified fields.
     * 
     * @throws IOException
     */
    public void refreshAppearances(List<PDField> fields) throws IOException
    {
        for (PDField field : fields)
        {
            if (field instanceof PDTerminalField)
            {
                ((PDTerminalField) field).constructAppearances();
            }
        }
    }

    /**
     * This will return all of the documents root fields.
     * 
     * A field might have children that are fields (non-terminal field) or does not have children which are fields
     * (terminal fields).
     * 
     * The fields within an AcroForm are organized in a tree structure. The documents root fields might either be
     * terminal fields, non-terminal fields or a mixture of both. Non-terminal fields mark branches which contents can
     * be retrieved using {@link PDNonTerminalField#getChildren()}.
     * 
     * @return A list of the documents root fields.
     * 
     */
    public List<PDField> getFields()
    {
        List<PDField> pdFields = new ArrayList<>();
        COSArray fields = getCOSObject().getDictionaryObject(COSName.FIELDS, COSArray.class);
        if (nonNull(fields))
        {
            for (COSBase field : fields)
            {
                if (nonNull(field) && field.getCOSObject() instanceof COSDictionary)
                {
                    pdFields.add(PDField.fromDictionary(this, (COSDictionary) field.getCOSObject(),
                            null));
                }
            }
        }
        return pdFields;
    }

    /**
     * Adds the fields to the root fields of the form
     * 
     * @param fields
     */
    public void addFields(List<PDField> toAdd)
    {
        COSArray fields = ofNullable(
                getCOSObject().getDictionaryObject(COSName.FIELDS, COSArray.class))
                        .orElseGet(COSArray::new);
        for (PDField field : toAdd)
        {
            fields.add(field);
        }
        getCOSObject().setItem(COSName.FIELDS, fields);
    }

    /**
     * removes the given field from the root fields of the form
     */
    public void removeField(PDField remove)
    {
        COSArray fields = getCOSObject().getDictionaryObject(COSName.FIELDS, COSArray.class);
        if (nonNull(fields) && fields.contains(remove))
        {
            fields.remove(remove);
        }
    }

    /**
     * Set the documents root fields.
     *
     * @param fields The fields that are part of the documents root fields.
     */
    public void setFields(List<PDField> fields)
    {
        getCOSObject().setItem(COSName.FIELDS, COSArrayList.converterToCOSArray(fields));
    }

    /**
     * Returns an iterator which walks all fields in the field tree, in order.
     */
    public Iterator<PDField> getFieldIterator()
    {
        return new PDFieldTree(this).iterator();
    }

    /**
     * @return the field tree representing all form fields and allowing a post-order visit of the tree
     */
    public PDFieldTree getFieldTree()
    {
        return new PDFieldTree(this);
    }

    /**
     * This will get a field by name, possibly using the cache if setCache is true.
     *
     * @param fullyQualifiedName The name of the field to get.
     * @return The field with that name of null if one was not found.
     */
    public PDField getField(String fullyQualifiedName)
    {
        String[] nameSubSection = fullyQualifiedName.split("\\.");
        COSArray fields = getCOSObject().getDictionaryObject(COSName.FIELDS, COSArray.class);

        if (nonNull(fields))
        {
            for (int i = 0; i < fields.size(); i++)
            {
                COSDictionary element = (COSDictionary) fields.getObject(i);
                if (nonNull(element))
                {
                    String fieldName = ofNullable(
                            element.getDictionaryObject(COSName.T, COSString.class))
                                    .map(COSString::getString).orElse("");
                    if (fieldName.equals(fullyQualifiedName) || fieldName.equals(nameSubSection[0]))
                    {
                        PDField root = PDField.fromDictionary(this, element, null);
                        if (nonNull(root))
                        {
                            if (nameSubSection.length > 1)
                            {
                                return ofNullable(root.findKid(nameSubSection, 1)).orElse(root);

                            }
                            return root;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * @return the DA element of the dictionary object or null if nothing is defined
     */
    public String getDefaultAppearance()
    {
        return ofNullable(getCOSObject().getItem(COSName.DA)).map(i -> (COSString) i)
                .map(COSString::getString).orElse("");
    }

    /**
     * Set the default appearance.
     * 
     * @param daValue a string describing the default appearance
     */
    public void setDefaultAppearance(String daValue)
    {
        getCOSObject().setString(COSName.DA, daValue);
    }

    /**
     * True if the viewing application should construct the appearances of all field widgets. The default value is
     * false.
     * 
     * @return the value of NeedAppearances, false if the value isn't set
     */
    public boolean isNeedAppearances()
    {
        return getCOSObject().getBoolean(COSName.NEED_APPEARANCES, false);
    }

    /**
     * Set the NeedAppearances value. If this is false, PDFBox will create appearances for all field widget.
     * 
     * @param value the value for NeedAppearances
     */
    public void setNeedAppearances(Boolean value)
    {
        getCOSObject().setBoolean(COSName.NEED_APPEARANCES, value);
    }

    /**
     * This will get the default resources for the acro form.
     *
     * @return The default resources.
     */
    public PDResources getDefaultResources()
    {
        return ofNullable(getCOSObject().getDictionaryObject(COSName.DR, COSDictionary.class))
                .map(dr -> new PDResources(dr, document.getResourceCache())).orElse(null);
    }

    /**
     * This will set the default resources for the acroform.
     *
     * @param dr The new default resources.
     */
    public void setDefaultResources(PDResources dr)
    {
        getCOSObject().setItem(COSName.DR, dr);
    }

    /**
     * This will tell if the AcroForm has XFA content.
     *
     * @return true if the AcroForm is an XFA form
     */
    public boolean hasXFA()
    {
        return getCOSObject().containsKey(COSName.XFA);
    }

    /**
     * This will tell if the AcroForm is a dynamic XFA form.
     *
     * @return true if the AcroForm is a dynamic XFA form
     */
    public boolean xfaIsDynamic()
    {
        return hasXFA() && getFields().isEmpty();
    }

    /**
     * Get the XFA resource, the XFA resource is only used for PDF 1.5+ forms.
     *
     * @return The xfa resource or null if it does not exist.
     */
    public PDXFAResource getXFA()
    {
        return ofNullable(getCOSObject().getDictionaryObject(COSName.XFA, COSDictionary.class))
                .map(PDXFAResource::new).orElse(null);
    }

    /**
     * Set the XFA resource, this is only used for PDF 1.5+ forms.
     *
     * @param xfa The xfa resource.
     */
    public void setXFA(PDXFAResource xfa)
    {
        getCOSObject().setItem(COSName.XFA, xfa);
    }

    /**
     * This will get the 'quadding' or justification of the text to be displayed. 0 - Left(default)<br/>
     * 1 - Centered<br />
     * 2 - Right<br />
     * Please see the QUADDING_CONSTANTS.
     *
     * @return The justification of the text strings.
     */
    public int getQuadding()
    {
        return getCOSObject().getInt(COSName.Q, 0);
    }

    /**
     * This will set the quadding/justification of the text. See QUADDING constants.
     *
     * @param q The new text justification.
     */
    public void setQuadding(int q)
    {
        getCOSObject().setInt(COSName.Q, q);
    }

    /**
     * Determines if SignaturesExist is set.
     * 
     * @return true if the document contains at least one signature.
     */
    public boolean isSignaturesExist()
    {
        return getCOSObject().getFlag(COSName.SIG_FLAGS, FLAG_SIGNATURES_EXIST);
    }

    /**
     * Set the SignaturesExist bit.
     *
     * @param signaturesExist The value for SignaturesExist.
     */
    public void setSignaturesExist(boolean signaturesExist)
    {
        getCOSObject().setFlag(COSName.SIG_FLAGS, FLAG_SIGNATURES_EXIST, signaturesExist);
    }

    /**
     * Determines if AppendOnly is set.
     * 
     * @return true if the document contains signatures that may be invalidated if the file is saved.
     */
    public boolean isAppendOnly()
    {
        return getCOSObject().getFlag(COSName.SIG_FLAGS, FLAG_APPEND_ONLY);
    }

    /**
     * Set the AppendOnly bit.
     *
     * @param appendOnly The value for AppendOnly.
     */
    public void setAppendOnly(boolean appendOnly)
    {
        getCOSObject().setFlag(COSName.SIG_FLAGS, FLAG_APPEND_ONLY, appendOnly);
    }

    private Map<COSDictionary, Integer> buildAnnotationToPageRef()
    {
        Map<COSDictionary, Integer> annotationToPageRef = new HashMap<>();

        int idx = 0;
        for (PDPage page : document.getPages())
        {
            try
            {
                for (PDAnnotation annotation : page.getAnnotations())
                {
                    if (annotation instanceof PDAnnotationWidget)
                    {
                        annotationToPageRef.put(annotation.getCOSObject(), idx);
                    }
                }
            }
            catch (IllegalArgumentException e)
            {
                LOG.warn("Can't retrieve annotations for page {}", idx);
            }
            idx++;
        }
        return annotationToPageRef;
    }
}
