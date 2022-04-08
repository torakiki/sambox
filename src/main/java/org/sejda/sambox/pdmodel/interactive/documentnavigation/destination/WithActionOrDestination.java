package org.sejda.sambox.pdmodel.interactive.documentnavigation.destination;
/*
 * Copyright 2022 Sober Lemur S.a.s. di Vacondio Andrea
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

import org.sejda.sambox.cos.COSObjectable;
import org.sejda.sambox.pdmodel.PDDocumentCatalog;
import org.sejda.sambox.pdmodel.interactive.action.PDAction;
import org.sejda.sambox.pdmodel.interactive.action.PDActionGoTo;

import java.io.IOException;
import java.util.Optional;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;

/**
 * @author Andrea Vacondio
 */
public interface WithActionOrDestination extends COSObjectable
{

    /**
     * @return The destination to be displayed when the annotation is activated. Shall not be
     * present if an A entry is present {@link #getAction()}
     * @throws IOException If there is an error creating the destination.
     */
    PDDestination getDestination() throws IOException;

    /**
     * @return The action that shall be performed when this item is activated
     */
    PDAction getAction();

    /**
     * @param catalog Catalog used to possibly resolve named destinations
     * @return the PDPageDestination or an empty {@link Optional} if the destination cannot be
     * resolved to a {@link PDPageDestination}. If necessary, It resolves GoTo action destinations
     * and named destination to return the actual PDPageDestination
     * @throws IOException
     */
    default Optional<PDPageDestination> resolveToPageDestination(PDDocumentCatalog catalog)
            throws IOException
    {
        PDDestination destination = getDestination();
        if (isNull(destination))
        {
            PDAction action = getAction();
            if (action instanceof PDActionGoTo)
            {
                destination = ((PDActionGoTo) action).getDestination();
            }
        }
        if (nonNull(catalog) && destination instanceof PDNamedDestination)
        {
            return Optional.ofNullable(
                    catalog.findNamedDestinationPage((PDNamedDestination) destination));
        }
        return ofNullable(destination).filter(PDPageDestination.class::isInstance)
                .map(PDPageDestination.class::cast);
    }
}
