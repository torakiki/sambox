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

import org.sejda.sambox.pdmodel.interactive.form.PDAcroForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * @author Andrea Vacondio
 */
public class AcroFormAppearancesGenerator implements Consumer<PDAcroForm>
{
    private static final Logger LOG = LoggerFactory.getLogger(AcroFormAppearancesGenerator.class);

    @Override
    public void accept(PDAcroForm acroForm)
    {
        // PDFBOX-4985
        // build the visual appearance as there is none for the widgets
        if (acroForm.isNeedAppearances())
        {
            try
            {
                LOG.debug("Generating appearance streams for fields as NeedAppearances is true()");
                acroForm.refreshAppearances();
                acroForm.setNeedAppearances(false);
            }
            catch (IOException | IllegalArgumentException e)
            {
                LOG.error("Could not generate appearance for some fields", e);
            }

        }
    }
}
