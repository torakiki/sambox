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

import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.pdmodel.PDResources;
import org.sejda.sambox.pdmodel.font.PDType1Font;
import org.sejda.sambox.pdmodel.interactive.form.PDAcroForm;

import java.util.function.Consumer;

import static java.util.Optional.ofNullable;

/**
 * @author Andrea Vacondio
 */
public class AcroFormDefaultValuesGenerator implements Consumer<PDAcroForm>
{
    @Override
    public void accept(PDAcroForm acroForm)
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
        COSDictionary fontDict = ofNullable(defaultResources.getCOSObject()
                .getDictionaryObject(COSName.FONT, COSDictionary.class)).orElseGet(
                COSDictionary::new);
        defaultResources.getCOSObject().putIfAbsent(COSName.FONT, fontDict);

        if (!fontDict.containsKey(COSName.HELV))
        {
            defaultResources.put(COSName.HELV, PDType1Font.HELVETICA);
        }
        if (!fontDict.containsKey(COSName.ZA_DB))
        {
            defaultResources.put(COSName.ZA_DB, PDType1Font.ZAPF_DINGBATS);
        }
    }
}
