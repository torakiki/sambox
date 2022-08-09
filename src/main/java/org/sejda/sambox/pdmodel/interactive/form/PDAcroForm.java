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
import org.sejda.sambox.pdmodel.PDDocument;
import org.sejda.sambox.pdmodel.PDPage;
import org.sejda.sambox.pdmodel.PDPageContentStream;
import org.sejda.sambox.pdmodel.PDPageContentStream.AppendMode;
import org.sejda.sambox.pdmodel.PDResources;
import org.sejda.sambox.pdmodel.common.PDDictionaryWrapper;
import org.sejda.sambox.pdmodel.common.PDRectangle;
import org.sejda.sambox.pdmodel.graphics.form.PDFormXObject;
import org.sejda.sambox.pdmodel.interactive.annotation.PDAnnotation;
import org.sejda.sambox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.sejda.sambox.pdmodel.interactive.annotation.PDAppearanceStream;
import org.sejda.sambox.util.Matrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;

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
    private ScriptingHandler scriptingHandler;

    /**
     * @param document The document that this form is part of.
     */
    public PDAcroForm(PDDocument document)
    {
        this.document = document;
        getCOSObject().setItem(COSName.FIELDS, new COSArray());
    }

    /**
     * @param document The document that this form is part of.
     * @param form     The existing acroForm.
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
    public PDDocument getDocument()
    {
        return document;
    }

    /**
     * This will flatten all form fields.
     *
     * <p>
     * Flattening a form field will take the current appearance and make that part of the pages
     * content stream. All form fields and annotations associated are removed.
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
     * Flattening a form field will take the current appearance and make that part of the pages
     * content stream. All form fields and annotations associated are removed.
     * </p>
     *
     * @param refreshAppearances if set to true the appearances for the form field widgets will be
     *                           updated
     * @throws IOException
     */
    public void flatten(List<PDField> fields, boolean refreshAppearances) throws IOException
    {
        // Nothing to flatten if there are no fields provided
        if (fields.isEmpty())
        {
            return;
        }

        // for dynamic XFA forms there is no flatten as this would mean to do a rendering
        // from the XFA content into a static PDF.
        if (xfaIsDynamic())
        {
            LOG.warn("Flatten for a dynamix XFA form is not supported");
            return;
        }

        if (!refreshAppearances && isNeedAppearances())
        {
            LOG.warn(
                    "AcroForm NeedAppearances is true, visual field appearances may not have been set");
            LOG.warn(
                    "call acroForm.refreshAppearances() or use the flatten() method with refreshAppearances parameter");
        }

        // refresh the appearances if set
        if (refreshAppearances)
        {
            refreshAppearances(fields);
        }

        // indicates if the original content stream
        // has been wrapped in a q...Q pair.
        boolean isContentStreamWrapped;

        Map<COSDictionary, PDAnnotationWidget> toFlatten = widgets(fields);

        // preserve all non widget annotations
        for (PDPage page : document.getPages())
        {
            isContentStreamWrapped = false;

            List<PDAnnotation> annotations = new ArrayList<>();

            for (PDAnnotation annotation : page.getAnnotations())
            {
                PDAnnotationWidget widget = toFlatten.get(annotation.getCOSObject());
                if (isNull(widget))
                {
                    annotations.add(annotation);
                }
                else if (isVisibleAnnotation(annotation))
                {
                    try (PDPageContentStream contentStream = new PDPageContentStream(document, page,
                            AppendMode.APPEND, true, !isContentStreamWrapped))
                    {
                        isContentStreamWrapped = true;

                        PDAppearanceStream appearanceStream = annotation.getNormalAppearanceStream();

                        PDFormXObject fieldObject = new PDFormXObject(
                                appearanceStream.getCOSObject());

                        contentStream.saveGraphicsState();

                        // see https://stackoverflow.com/a/54091766/1729265 for an explanation
                        // of the steps required
                        // this will transform the appearance stream form object into the rectangle of the
                        // annotation bbox and map the coordinate systems
                        Matrix transformationMatrix = resolveTransformationMatrix(annotation,
                                appearanceStream);
                        contentStream.transform(transformationMatrix);

                        contentStream.drawForm(fieldObject);
                        contentStream.restoreGraphicsState();
                    }
                }
            }
            page.setAnnotations(annotations);
        }

        removeFields(fields);

        // remove XFA for hybrid forms
        getCOSObject().removeItem(COSName.XFA);

    }

    private boolean isVisibleAnnotation(PDAnnotation annotation)
    {
        if (annotation.isInvisible() || annotation.isHidden())
        {
            return false;
        }
        PDAppearanceStream normalAppearanceStream = annotation.getNormalAppearanceStream();
        if (normalAppearanceStream == null)
        {
            return false;
        }
        PDRectangle bbox = normalAppearanceStream.getBBox();
        return bbox != null && bbox.getWidth() > 0 && bbox.getHeight() > 0;
    }

    /**
     * Refreshes the appearance streams and appearance dictionaries for the widget annotations of
     * all fields.
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
     * Refreshes the appearance streams and appearance dictionaries for the widget annotations of
     * the specified fields.
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
     * <p>
     * A field might have children that are fields (non-terminal field) or does not have children
     * which are fields (terminal fields).
     * <p>
     * The fields within an AcroForm are organized in a tree structure. The documents root fields
     * might either be terminal fields, non-terminal fields or a mixture of both. Non-terminal
     * fields mark branches which contents can be retrieved using {@link
     * PDNonTerminalField#getChildren()}.
     *
     * @return A list of the documents root fields, never null. If there are no fields then this
     * method returns an empty list.
     */
    public List<PDField> getFields()
    {
        return fieldsFromArray(getCOSObject().getDictionaryObject(COSName.FIELDS, COSArray.class));
    }

    private List<PDField> fieldsFromArray(COSArray array)
    {
        if (nonNull(array) && array.size() > 0)
        {
            return array.stream().filter(Objects::nonNull).map(COSBase::getCOSObject)
                    .filter(d -> d instanceof COSDictionary)
                    .map(d -> PDField.fromDictionary(this, (COSDictionary) d, null))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    /**
     * Adds the fields to the root fields of the form
     *
     * @param toAdd
     */
    public void addFields(Collection<PDField> toAdd)
    {
        COSArray fields = ofNullable(
                getCOSObject().getDictionaryObject(COSName.FIELDS, COSArray.class)).orElseGet(
                COSArray::new);
        for (PDField field : toAdd)
        {
            fields.add(field);
        }
        getCOSObject().setItem(COSName.FIELDS, fields);
    }

    /**
     * removes the given field from the root fields of the form
     *
     * @return the removed element or null
     */
    public COSBase removeField(PDField remove)
    {
        COSArray fields = getCOSObject().getDictionaryObject(COSName.FIELDS, COSArray.class);
        if (nonNull(fields))
        {
            int removeIdx = fields.indexOfObject(remove.getCOSObject());
            if (removeIdx >= 0)
            {
                return fields.remove(removeIdx);
            }
        }
        return null;
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
     * Returns an iterator which walks all fields in the field tree, post-order.
     */
    public Iterator<PDField> getFieldIterator()
    {
        return new PDFieldTree(this).iterator();
    }

    /**
     * @return the field tree representing all form fields and allowing a post-order visit of the
     * tree
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
        if (fullyQualifiedName == null)
        {
            return null;
        }

        return getFieldTree().stream()
                .filter(f -> f != null && fullyQualifiedName.equals(f.getFullyQualifiedName()))
                .findFirst().orElse(null);
    }

    /**
     * @return the DA element of the dictionary object or null if nothing is defined
     */
    public String getDefaultAppearance()
    {
        return ofNullable(getCOSObject().getDictionaryObject(COSName.DA, COSString.class)).map(
                COSString::getString).orElse("");
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

    public List<PDField> getCalculationOrder()
    {
        return fieldsFromArray(getCOSObject().getDictionaryObject(COSName.CO, COSArray.class));
    }

    public void setCalculationOrder(COSArray co)
    {
        getCOSObject().setItem(COSName.CO, co);
    }

    /**
     * True if the viewing application should construct the appearances of all field widgets. The
     * default value is false.
     *
     * @return the value of NeedAppearances, false if the value isn't set
     */
    public boolean isNeedAppearances()
    {
        return getCOSObject().getBoolean(COSName.NEED_APPEARANCES, false);
    }

    /**
     * Set the NeedAppearances value. If this is false, PDFBox will create appearances for all field
     * widget.
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
        return ofNullable(getCOSObject().getDictionaryObject(COSName.DR, COSDictionary.class)).map(
                dr -> new PDResources(dr, document.getResourceCache())).orElse(null);
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
        return ofNullable(getCOSObject().getDictionaryObject(COSName.XFA, COSDictionary.class)).map(
                PDXFAResource::new).orElse(null);
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
     * This will get the 'quadding' or justification of the text to be displayed. 0 -
     * Left(default)<br/> 1 - Centered<br /> 2 - Right<br /> See the QUADDING constants of {@link
     * PDVariableText}.
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
     * @return true if the document contains signatures that may be invalidated if the file is
     * saved.
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

    /**
     * Set a handler to support JavaScript actions in the form.
     *
     * @return scriptingHandler
     */
    public ScriptingHandler getScriptingHandler()
    {
        return scriptingHandler;
    }

    /**
     * Set a handler to support JavaScript actions in the form.
     *
     * @param scriptingHandler
     */
    public void setScriptingHandler(ScriptingHandler scriptingHandler)
    {
        this.scriptingHandler = scriptingHandler;
    }

    private Matrix resolveTransformationMatrix(PDAnnotation annotation,
            PDAppearanceStream appearanceStream)
    {
        // 1st step transform appearance stream bbox with appearance stream matrix
        Rectangle2D transformedAppearanceBox = getTransformedAppearanceBBox(appearanceStream);
        PDRectangle annotationRect = annotation.getRectangle();

        // 2nd step caclulate matrix to transform calculated rectangle into the annotation Rect boundaries
        Matrix transformationMatrix = new Matrix();
        transformationMatrix.translate(
                (float) (annotationRect.getLowerLeftX() - transformedAppearanceBox.getX()),
                (float) (annotationRect.getLowerLeftY() - transformedAppearanceBox.getY()));
        transformationMatrix.scale(
                (float) (annotationRect.getWidth() / transformedAppearanceBox.getWidth()),
                (float) (annotationRect.getHeight() / transformedAppearanceBox.getHeight()));
        return transformationMatrix;
    }

    /**
     * Calculate the transformed appearance box.
     * <p>
     * Apply the Matrix (or an identity transform) to the BBox of the appearance stream
     *
     * @param appearanceStream
     * @return the transformed rectangle
     */
    private Rectangle2D getTransformedAppearanceBBox(PDAppearanceStream appearanceStream)
    {
        Matrix appearanceStreamMatrix = appearanceStream.getMatrix();
        PDRectangle appearanceStreamBBox = appearanceStream.getBBox();
        GeneralPath transformedAppearanceBox = appearanceStreamBBox.transform(
                appearanceStreamMatrix);
        return transformedAppearanceBox.getBounds2D();
    }

    private Map<COSDictionary, PDAnnotationWidget> widgets(List<PDField> fields)
    {
        Map<COSDictionary, PDAnnotationWidget> widgetMap = new HashMap<>();
        for (PDField field : fields)
        {
            for (PDAnnotationWidget widget : field.getWidgets())
            {
                widgetMap.put(widget.getCOSObject(), widget);
            }
        }

        return widgetMap;
    }

    private Map<COSDictionary, Set<COSDictionary>> buildPagesWidgetsMap(List<PDField> fields)
            throws IOException
    {
        Map<COSDictionary, Set<COSDictionary>> pagesAnnotationsMap = new HashMap<>();
        boolean hasMissingPageRef = false;

        for (PDField field : fields)
        {
            List<PDAnnotationWidget> widgets = field.getWidgets();
            for (PDAnnotationWidget widget : widgets)
            {
                PDPage page = widget.getPage();
                if (page != null)
                {
                    fillPagesAnnotationMap(pagesAnnotationsMap, page, widget);
                }
                else
                {
                    hasMissingPageRef = true;
                }
            }
        }

        if (!hasMissingPageRef)
        {
            return pagesAnnotationsMap;
        }

        // If there is a widget with a missing page reference we need to build the map reverse i.e.
        // from the annotations to the widget.
        LOG.warn(
                "There has been a widget with a missing page reference, will check all page annotations");
        for (PDPage page : document.getPages())
        {
            for (PDAnnotation annotation : page.getAnnotations())
            {
                if (annotation instanceof PDAnnotationWidget)
                {
                    fillPagesAnnotationMap(pagesAnnotationsMap, page,
                            (PDAnnotationWidget) annotation);
                }
            }
        }

        return pagesAnnotationsMap;
    }

    private void fillPagesAnnotationMap(Map<COSDictionary, Set<COSDictionary>> pagesAnnotationsMap,
            PDPage page, PDAnnotationWidget widget)
    {
        if (pagesAnnotationsMap.get(page.getCOSObject()) == null)
        {
            Set<COSDictionary> widgetsForPage = new HashSet<>();
            widgetsForPage.add(widget.getCOSObject());
            pagesAnnotationsMap.put(page.getCOSObject(), widgetsForPage);
        }
        else
        {
            Set<COSDictionary> widgetsForPage = pagesAnnotationsMap.get(page.getCOSObject());
            widgetsForPage.add(widget.getCOSObject());
        }
    }

    private void removeFields(List<PDField> fields)
    {
        for (PDField current : fields)
        {
            if (nonNull(current.getParent()))
            {
                current.getParent().removeChild(current);
            }
            else
            {
                // it's a root field
                if (isNull(removeField(current)))
                {
                    LOG.warn("Unable to remove root field {}, not found",
                            current.getFullyQualifiedName());
                }
            }

        }
    }
}
