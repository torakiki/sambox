/*
 * Created on 01/11/26
 * Copyright 2026 by Sober Lemur S.r.l. (info@soberlemur.com).
 *
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
package org.sejda.sambox.rendering;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.pdmodel.PDDocument;
import org.sejda.sambox.pdmodel.PDPage;
import org.sejda.sambox.pdmodel.interactive.annotation.PDAnnotationMarkup;

class PageDrawerTest
{
    @Test
    void annotationWithoutRectIsSkippedOnRotatedPage() throws IOException
    {
        var annotation = new PDAnnotationMarkup();
        annotation.getCOSObject().setName(COSName.SUBTYPE, PDAnnotationMarkup.SUB_TYPE_FREETEXT);
        annotation.setNoRotate(true);
        var page = new PDPage();
        page.setRotation(90);
        page.setAnnotations(List.of(annotation));
        try (var doc = new PDDocument())
        {
            doc.addPage(page);
            assertNotNull(new PDFRenderer(doc).renderImage(0));
        }
    }
}
