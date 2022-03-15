package org.sejda.sambox.pdmodel.fixup;
/*
 * Copyright 2022 Sober Lemur S.a.s. di Vacondio Andrea
 * Copyright 2022 Sejda BV
 *
 * Created 09/03/22
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.fontbox.ttf.TrueTypeFont;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static java.util.Objects.nonNull;

/**
 * Generate field entries from page level widget annotations if there AcroForm /Fields entry is
 * empty.
 *
 * @author Andrea Vacondio
 */
public class AcroFormOrphanWindgetsGenerator implements Consumer<PDAcroForm>
{
    private static final Logger LOG = LoggerFactory.getLogger(
            AcroFormOrphanWindgetsGenerator.class);

    @Override
    public void accept(PDAcroForm acroForm)
    {
        if (acroForm.isNeedAppearances() && acroForm.getFields().isEmpty())
        {
            Map<String, PDField> nonTerminalFieldsMap = new HashMap<>();

            LOG.debug("Rebuilding fields from widgets");
            List<PDField> fields = new ArrayList<>();
            for (PDPage page : acroForm.getDocument().getPages())
            {
                handleAnnotations(acroForm, fields, page.getAnnotations(), nonTerminalFieldsMap);
            }

            acroForm.setFields(fields);

            // ensure that PDVariableText fields have the necessary resources
            for (PDField field : acroForm.getFieldTree())
            {
                if (field instanceof PDVariableText)
                {
                    ensureFontResources(acroForm, (PDVariableText) field);
                }
            }
        }
    }

    private void handleAnnotations(PDAcroForm acroForm, List<PDField> fields,
            List<PDAnnotation> annotations, Map<String, PDField> nonTerminalFieldsMap)
    {
        PDResources acroFormResources = acroForm.getDefaultResources();

        for (PDAnnotation annot : annotations)
        {
            if (annot instanceof PDAnnotationWidget)
            {
                addFontFromWidget(acroFormResources, annot);

                if (annot.getCOSObject().containsKey(COSName.PARENT))
                {
                    PDField resolvedField = resolveNonRootField(acroForm,
                            (PDAnnotationWidget) annot, nonTerminalFieldsMap);
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

    /*
     *  Add font resources from the widget to the AcroForm to make sure embedded fonts are being
     *  used and not added by ensureFontResources potentially using a fallback font
     */
    private void addFontFromWidget(PDResources acroFormResources, PDAnnotation annotation)
    {
        PDAppearanceStream normalAppearanceStream = annotation.getNormalAppearanceStream();
        if (normalAppearanceStream != null && normalAppearanceStream.getResources() != null)
        {
            PDResources widgetResources = normalAppearanceStream.getResources();
            for (COSName fontName : widgetResources.getFontNames())
            {
                if (!fontName.getName().startsWith("+"))
                {
                    try
                    {
                        if (acroFormResources.getFont(fontName) == null)
                        {
                            acroFormResources.put(fontName, widgetResources.getFont(fontName));
                            LOG.debug(
                                    "Added font resource to AcroForm from widget for font name {}",
                                    fontName.getName());
                        }
                    }
                    catch (IOException ioe)
                    {
                        LOG.debug("unable to add font to AcroForm for font name "
                                + fontName.getName());
                    }
                }
            }
        }
    }

    /*
     *  Widgets having a /Parent entry are non root fields. Go up until the root node is found
     *  and handle from there.
     */
    private PDField resolveNonRootField(PDAcroForm acroForm, PDAnnotationWidget widget,
            Map<String, PDField> nonTerminalFieldsMap)
    {
        COSDictionary parent = widget.getCOSObject()
                .getDictionaryObject(COSName.PARENT, COSDictionary.class);
        while (nonNull(parent) && parent.containsKey(COSName.PARENT))
        {
            parent = parent.getDictionaryObject(COSName.PARENT, COSDictionary.class);
        }

        if (nonTerminalFieldsMap.get(parent.getString(COSName.T)) == null)
        {
            PDField field = PDFieldFactory.createField(acroForm, parent, null);
            nonTerminalFieldsMap.put(field.getFullyQualifiedName(), field);

            return field;
        }

        // this should not happen
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
    private void ensureFontResources(PDAcroForm acroForm, PDVariableText field)
    {
        PDResources defaultResources = acroForm.getDefaultResources();
        String daString = field.getDefaultAppearance();
        if (daString.startsWith("/") && daString.length() > 1)
        {
            COSName fontName = COSName.getPDFName(daString.substring(1, daString.indexOf(" ")));
            try
            {
                if (defaultResources != null && defaultResources.getFont(fontName) == null)
                {
                    LOG.debug("trying to add missing font resource for field "
                            + field.getFullyQualifiedName());
                    FontMapper mapper = FontMappers.instance();
                    FontMapping<TrueTypeFont> fontMapping = mapper.getTrueTypeFont(
                            fontName.getName(), null);
                    if (fontMapping != null)
                    {
                        PDType0Font pdFont = PDType0Font.load(acroForm.getDocument(),
                                fontMapping.getFont(), false);
                        LOG.debug("looked up font for " + fontName.getName() + " - found "
                                + fontMapping.getFont().getName());
                        defaultResources.put(fontName, pdFont);
                    }
                    else
                    {
                        LOG.debug(
                                "no suitable font found for field " + field.getFullyQualifiedName()
                                        + " for font name " + fontName.getName());
                    }
                }
            }
            catch (IOException ioe)
            {
                LOG.debug(
                        "Unable to handle font resources for field " + field.getFullyQualifiedName()
                                + ": " + ioe.getMessage());
            }
        }
    }
}
