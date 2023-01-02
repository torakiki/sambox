package org.sejda.sambox.pdmodel.common;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.sejda.io.SeekableSources;
import org.sejda.sambox.input.PDFParser;
import org.sejda.sambox.pdmodel.PDDocument;

import java.io.IOException;

/*
 * Copyright 2022 Sober Lemur S.r.l.
 * Copyright 2022 Sejda BV
 *
 * Created 31/03/22
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
class PDPageLabelsTest
{

    @Test
    void getLabels() throws IOException
    {
        try (PDDocument doc = PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(
                getClass().getResourceAsStream("/sambox/page-labels.pdf"))))
        {
            Assertions.assertNotNull(doc.getDocumentCatalog().getPageLabels());
        }
    }
}
