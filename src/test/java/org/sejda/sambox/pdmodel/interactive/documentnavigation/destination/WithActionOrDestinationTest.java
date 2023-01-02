package org.sejda.sambox.pdmodel.interactive.documentnavigation.destination;

import org.junit.jupiter.api.Test;
import org.sejda.sambox.pdmodel.PDDocumentCatalog;
import org.sejda.sambox.pdmodel.PDPage;
import org.sejda.sambox.pdmodel.interactive.action.PDActionGoTo;
import org.sejda.sambox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/*
 * Copyright 2022 Sober Lemur S.r.l.
 * Copyright 2022 Sejda BV
 *
 * Created 08/04/22
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

class WithActionOrDestinationTest
{

    @Test
    public void resolveToPageDestinationNamedDestination() throws IOException
    {
        PDPageFitDestination dest = new PDPageFitDestination();
        dest.setPageNumber(5);
        PDNamedDestination destination = new PDNamedDestination();
        destination.setNamedDestination("ChuckNorris");
        PDOutlineItem victim = new PDOutlineItem();
        victim.setDestination(destination);
        PDDocumentCatalog catalog = mock(PDDocumentCatalog.class);
        when(catalog.findNamedDestinationPage(any())).thenReturn(dest);
        assertEquals(dest, victim.resolveToPageDestination(catalog).get());
    }

    @Test
    public void resolveToPageDestinationAction() throws IOException
    {
        PDPageFitDestination destination = new PDPageFitDestination();
        PDPage page = new PDPage();
        destination.setPage(page);
        PDActionGoTo action = new PDActionGoTo();
        action.setDestination(destination);
        PDOutlineItem victim = new PDOutlineItem();
        victim.setAction(action);
        assertEquals(destination.getPage(), victim.resolveToPageDestination(null).get().getPage());
    }
}