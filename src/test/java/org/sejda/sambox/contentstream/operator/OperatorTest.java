package org.sejda.sambox.contentstream.operator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/*
 * Copyright 2025 Sober Lemur S.r.l.
 * Copyright 2025 Sejda BV
 *
 * Created 05/11/25
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
public class OperatorTest
{
    @Test
    public void nullOperator()
    {
        assertThrows(IllegalArgumentException.class, () -> Operator.getOperator(null));
    }

    @Test
    public void slashOperator()
    {
        assertThrows(IllegalArgumentException.class, () -> Operator.getOperator("/op"));
    }

    @Test
    public void positive()
    {
        var op = Operator.getOperator(OperatorName.CURVE_TO);
        assertEquals(op, Operator.getOperator(OperatorName.CURVE_TO));
    }
}