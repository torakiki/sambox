package org.sejda.sambox.pdmodel;
/*
 * Copyright 2025 Sober Lemur S.r.l.
 * Copyright 2025 Sejda BV
 *
 * Created 28/09/25
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

import static org.sejda.commons.util.RequireUtils.requireNotNullArg;

import java.io.IOException;
import java.util.function.Consumer;

import org.sejda.commons.util.IOUtils;
import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSDocument;
import org.sejda.sambox.input.IndirectObjectsProvider;
import org.sejda.sambox.pdmodel.encryption.SecurityHandler;

/**
 * @author Andrea Vacondio
 */
public class LoadedPDDocument extends PDDocument
{

    private final IndirectObjectsProvider provider;

    public LoadedPDDocument(COSDocument document, SecurityHandler securityHandler,
            IndirectObjectsProvider provider)
    {
        super(document, securityHandler);
        requireNotNullArg(provider, "IndirectObjectsProvider cannot be null");
        this.provider = provider;
    }

    public void inspect(Consumer<COSBase> inspector)
    {
        requireNotNullArg(inspector, "Inspector cannot be null");
        provider.inspect(inspector);
    }

    @Override
    public void close() throws IOException
    {
        super.close();
        IOUtils.close(provider);
    }
}
