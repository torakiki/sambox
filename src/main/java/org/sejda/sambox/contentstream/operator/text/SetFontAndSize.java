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

import static java.util.Objects.isNull;
import static org.sejda.commons.util.RequireUtils.require;

import java.io.IOException;
import java.util.List;

import org.sejda.sambox.contentstream.operator.MissingOperandException;
import org.sejda.sambox.contentstream.operator.Operator;
import org.sejda.sambox.contentstream.operator.OperatorName;
import org.sejda.sambox.contentstream.operator.OperatorProcessor;
import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSNumber;
import org.sejda.sambox.pdmodel.font.PDFont;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tf: Set text font and size.
 *
 * @author Laurent Huault
 */
public class SetFontAndSize extends OperatorProcessor
{
    private static final Logger LOG = LoggerFactory.getLogger(SetFontAndSize.class);

    @Override
    public void process(Operator operator, List<COSBase> operands) throws IOException
    {
        require(operands.size() >= 2, () -> new MissingOperandException(operator, operands));

        if ((operands.get(0) instanceof COSName name) && (operands.get(
                1) instanceof COSNumber size))
        {
            getContext().getGraphicsState().getTextState().setFontSize(size.floatValue());
            PDFont font = getContext().getResources().getFont(name);
            if (isNull(font))
            {
                LOG.warn("font '{}' not found in resources", name.getName());
            }
            getContext().getGraphicsState().getTextState().setFont(font);
        }
    }

    @Override
    public String getName()
    {
        return OperatorName.SET_FONT_AND_SIZE;
    }
}
