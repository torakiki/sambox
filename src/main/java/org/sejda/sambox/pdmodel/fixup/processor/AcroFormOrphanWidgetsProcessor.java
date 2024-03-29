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
package org.sejda.sambox.pdmodel.fixup.processor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.fontbox.ttf.TrueTypeFont;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.pdmodel.PDDocument;
import org.sejda.sambox.pdmodel.PDPage;
import org.sejda.sambox.pdmodel.PDResources;
import org.sejda.sambox.pdmodel.font.FontMapper;
import org.sejda.sambox.pdmodel.font.FontMappers;
import org.sejda.sambox.pdmodel.font.FontMapping;
import org.sejda.sambox.pdmodel.font.PDType0Font;
import org.sejda.sambox.pdmodel.interactive.annotation.PDAnnotation;
import org.sejda.sambox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.sejda.sambox.pdmodel.interactive.annotation.PDAppearanceStream;
import org.sejda.sambox.pdmodel.interactive.form.PDAcroForm;
import org.sejda.sambox.pdmodel.interactive.form.PDField;
import org.sejda.sambox.pdmodel.interactive.form.PDFieldFactory;
import org.sejda.sambox.pdmodel.interactive.form.PDVariableText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  Generate field entries from page level widget annotations
 *  if there AcroForm /Fields entry is empty.
 * 
 */
public class AcroFormOrphanWidgetsProcessor extends AbstractProcessor
{

    private static final Logger LOG = LoggerFactory.getLogger(AcroFormOrphanWidgetsProcessor.class);

    public AcroFormOrphanWidgetsProcessor(PDDocument document)
    {
        super(document);
    }

    @Override
    public void process()
    {
        /*
         * Get the AcroForm in it's current state.
         *
         * Also note: getAcroForm() applies a default fixup which this processor
         * is part of. So keep the null parameter otherwise this will end
         * in an endless recursive call
         */
        PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm(null);

        if (acroForm != null)
        {
            resolveFieldsFromWidgets(acroForm);
        }
    }

    private void resolveFieldsFromWidgets(PDAcroForm acroForm)
    {
        LOG.debug("rebuilding fields from widgets");

        PDResources resources = acroForm.getDefaultResources();
        if (resources == null)
        {
            // failsafe. Currently resources is never null because defaultfixup is called first.
            LOG.debug("AcroForm default resources is null");
            return;
        }

        List<PDField> fields = new ArrayList<>();
        Map<String, PDField> nonTerminalFieldsMap = new HashMap<>();
        for (PDPage page : document.getPages())
        {
            handleAnnotations(acroForm, resources, fields, page.getAnnotations(),
                    nonTerminalFieldsMap);
        }

        acroForm.setFields(fields);

        for (PDField field : acroForm.getFieldTree())
        {
            if (field instanceof PDVariableText)
            {
                ensureFontResources(resources, (PDVariableText) field);
            }
        }
    }

    private void handleAnnotations(PDAcroForm acroForm, PDResources acroFormResources,
            List<PDField> fields, List<PDAnnotation> annotations,
            Map<String, PDField> nonTerminalFieldsMap)
    {
        for (PDAnnotation annot : annotations)
        {
            if (annot instanceof PDAnnotationWidget)
            {
                addFontFromWidget(acroFormResources, annot);

                COSDictionary parent = annot.getCOSObject().getDictionaryObject(COSName.PARENT, COSDictionary.class);
                if (parent != null)
                {
                    PDField resolvedField = resolveNonRootField(acroForm, parent, nonTerminalFieldsMap);
                    if (resolvedField != null)
                    {
                        fields.add(resolvedField);
                    }
                }
                else
                {
                    fields.add(PDFieldFactory.createField(acroForm, annot.getCOSObject(), null));
                }
            }
        }
    }

    /**
     * Add font resources from the widget to the AcroForm to make sure embedded fonts are being used
     * and not added by ensureFontResources potentially using a fallback font.
     *
     * @param acroFormResources AcroForm default resources, should not be null.
     * @param annotation annotation, should not be null.
     */
    private void addFontFromWidget(PDResources acroFormResources, PDAnnotation annotation)
    {
        PDAppearanceStream normalAppearanceStream = annotation.getNormalAppearanceStream();
        if (normalAppearanceStream == null)
        {
            return;
        }
        PDResources widgetResources = normalAppearanceStream.getResources();
        if (widgetResources == null)
        {
            return;
        }
        for (COSName fontName : widgetResources.getFontNames())
        {
            if (!fontName.getName().startsWith("+"))
            {
                try
                {
                    if (acroFormResources.getFont(fontName) == null)
                    {
                        acroFormResources.put(fontName, widgetResources.getFont(fontName));
                        LOG.debug("added font resource to AcroForm from widget for font name " + fontName.getName());
                    }
                }
                catch (IOException ioe)
                {
                    LOG.debug("unable to add font to AcroForm for font name " + fontName.getName());
                }
            }
            else
            {
                LOG.debug("font resource for widget was a subsetted font - ignored: " + fontName.getName());
            }
        }
    }

    /*
     *  Widgets having a /Parent entry are non root fields. Go up until the root node is found
     *  and handle from there.
     */
    private PDField resolveNonRootField(PDAcroForm acroForm, COSDictionary parent, Map<String, PDField> nonTerminalFieldsMap)
    {
        while (parent.containsKey(COSName.PARENT))
        {
            parent = parent.getDictionaryObject(COSName.PARENT, COSDictionary.class);
            if (parent == null)
            {
                return null;
            }
        }

        if (nonTerminalFieldsMap.get(parent.getString(COSName.T)) == null)
        {
            PDField field = PDFieldFactory.createField(acroForm, parent, null);
            if (field != null)
            {
                nonTerminalFieldsMap.put(field.getFullyQualifiedName(), field);
            }
            return field;
        }

        // this should not happen, likely broken PDF
        return null;
    }

    /*
     *  Lookup the font used in the default appearance and if this is
     *  not available try to find a suitable font and use that.
     *  This may not be the original font but a similar font replacement
     *
     *  TODO: implement a font lookup similar as discussed in PDFBOX-2661 so that already existing
     *        font resources might be accepatble.
     *        In such case this must be implemented in PDDefaultAppearanceString too!
     */
    private void ensureFontResources(PDResources defaultResources, PDVariableText field)
    {
        String daString = field.getDefaultAppearance();
        if (daString.startsWith("/") && daString.length() > 1)
        {
            COSName fontName = COSName.getPDFName(daString.substring(1, daString.indexOf(" ")));
            try
            {
                if (defaultResources.getFont(fontName) == null)
                {
                    LOG.debug("Trying to add missing font resource for field "
                            + field.getFullyQualifiedName());
                    FontMapper mapper = FontMappers.instance();
                    FontMapping<TrueTypeFont> fontMapping = mapper.getTrueTypeFont(fontName.getName() , null);
                    if (fontMapping != null)
                    {
                        PDType0Font pdFont = PDType0Font.load(document, fontMapping.getFont(), false);
                        LOG.debug("looked up font for " + fontName.getName() + " - found " + fontMapping.getFont().getName());
                        defaultResources.put(fontName, pdFont);
                    }
                    else
                    {
                        LOG.debug("no suitable font found for field " + field.getFullyQualifiedName() + " for font name " + fontName.getName());
                    }
                }
            }
            catch (IOException ioe)
            {
                LOG.debug("Unable to handle font resources for field " + field.getFullyQualifiedName() + ": " + ioe.getMessage());
            }
        }
    }
}
