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

import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.pdmodel.PDDocument;
import org.sejda.sambox.pdmodel.PDResources;
import org.sejda.sambox.pdmodel.font.PDType1Font;
import org.sejda.sambox.pdmodel.interactive.form.PDAcroForm;


/**
 * Verify and ensure default resources for /AcroForm.
 * 
 * <ul>
 *   <li>a default appearance string is defined</li>
 *   <li>default resources are defined</li>
 *   <li>Helvetica as <code>/Helv</code> and Zapf Dingbats as <code>ZaDb</code> are included.
 *       ZaDb is required for most check boxes and radio buttons</li>
 * </ul>
 * 
 */
public class AcroFormDefaultsProcessor extends AbstractProcessor
{
    public AcroFormDefaultsProcessor(PDDocument document)
    { 
        super(document); 
    }

    @Override
    public void process() {
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
            verifyOrCreateDefaults(acroForm);
        } 
    }

   /*
     * Verify that there are default entries for required 
     * properties.
     * 
     * If these are missing create default entries similar to
     * Adobe Reader / Adobe Acrobat
     *  
     */
    private void verifyOrCreateDefaults(PDAcroForm acroForm)
    {
        final String adobeDefaultAppearanceString = "/Helv 0 Tf 0 g ";

        // DA entry is required
        if (acroForm.getDefaultAppearance().length() == 0)
        {
            acroForm.setDefaultAppearance(adobeDefaultAppearanceString);
        }

        // DR entry is required
        PDResources defaultResources = acroForm.getDefaultResources();
        if (defaultResources == null)
        {
            defaultResources = new PDResources();
            acroForm.setDefaultResources(defaultResources);
        }

        // PDFBOX-3732: Adobe Acrobat uses Helvetica as a default font and 
        // stores that under the name '/Helv' in the resources dictionary
        // Zapf Dingbats is included per default for check boxes and 
        // radio buttons as /ZaDb.
        // PDFBOX-4393: the two fonts are added by Adobe when signing
        // and this breaks a previous signature. (Might be an Adobe bug)
        COSDictionary fontDict = defaultResources.getCOSObject().getDictionaryObject(COSName.FONT, COSDictionary.class);
        if (fontDict == null)
        {
            fontDict = new COSDictionary();
            defaultResources.getCOSObject().setItem(COSName.FONT, fontDict);
        }
        if (!fontDict.containsKey(COSName.HELV))
        {
            defaultResources.put(COSName.HELV, PDType1Font.HELVETICA());
        }
        if (!fontDict.containsKey(COSName.ZA_DB))
        {
            defaultResources.put(COSName.ZA_DB, PDType1Font.ZAPF_DINGBATS());
        }
    }
} 
