/*
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
package org.sejda.sambox.contentstream.operator.text;

import java.io.IOException;
import java.util.List;

import org.sejda.sambox.contentstream.operator.MissingOperandException;
import org.sejda.sambox.contentstream.operator.Operator;
import org.sejda.sambox.contentstream.operator.OperatorName;
import org.sejda.sambox.contentstream.operator.OperatorProcessor;
import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSNumber;
import org.sejda.sambox.pdmodel.graphics.state.RenderingMode;

/**
 * Tr: Set text rendering mode.
 *
 * @author Ben Litchfield
 */
public class SetTextRenderingMode extends OperatorProcessor
{
    @Override
    public void process(Operator operator, List<COSBase> arguments) throws IOException
    {
        if (arguments.isEmpty())
        {
            throw new MissingOperandException(operator, arguments);
        }
        COSBase base0 = arguments.get(0);
        if (base0 instanceof COSNumber mode)
        {
            int val = mode.intValue();
            if (val >= 0 && val < RenderingMode.values().length)
            {
                getContext().getGraphicsState().getTextState()
                        .setRenderingMode(RenderingMode.fromInt(val));
            }
        }

    }

    @Override
    public String getName()
    {
        return OperatorName.SET_TEXT_RENDERINGMODE;
    }
}
