package org.sejda.sambox.pdmodel;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.sejda.io.SeekableSources;
import org.sejda.sambox.input.PDFParser;

/*
 * Copyright 2026 Sober Lemur S.r.l.
 * Copyright 2026 Sejda BV
 *
 * Created 05/05/26
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
class LoadedPDDocumentTest
{

    @Test
    public void testInspectAll() throws Exception
    {
        try (var document = PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(
                getClass().getResourceAsStream("/sambox/simple_test.pdf"))))
        {
            var i = new AtomicInteger();
            document.inspectAll(_ -> i.getAndIncrement());
            Assertions.assertEquals(10, i.get());
        }
    }

    @Test
    public void testInspect() throws Exception
    {
        try (var document = PDFParser.parse(SeekableSources.inMemorySeekableSourceFrom(
                getClass().getResourceAsStream("/sambox/simple_test.pdf"))))
        {
            var i = new AtomicInteger();
            document.inspect(_ -> i.getAndIncrement());
            Assertions.assertEquals(1, i.get());
        }
    }
}