package org.sejda.sambox.pdmodel.interactive.documentnavigation.destination;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.sejda.sambox.util.Matrix;

import java.awt.geom.AffineTransform;

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
class PDPageFitHeightDestinationTest
{

    @Test
    void transform()
    {
        PDPageFitHeightDestination victim = new PDPageFitHeightDestination();
        victim.setLeft(50);
        victim.transform(new Matrix(AffineTransform.getTranslateInstance(20, 10)));
        Assertions.assertEquals(70, victim.getLeft());
    }
}