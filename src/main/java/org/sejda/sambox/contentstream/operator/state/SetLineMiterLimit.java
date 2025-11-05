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
package org.sejda.sambox.contentstream.operator.state;

import static org.sejda.commons.util.RequireUtils.require;
import static org.sejda.sambox.contentstream.operator.OperatorName.SET_LINE_MITERLIMIT;

import java.io.IOException;
import java.util.List;

import org.sejda.sambox.contentstream.operator.MissingOperandException;
import org.sejda.sambox.contentstream.operator.Operator;
import org.sejda.sambox.contentstream.operator.OperatorProcessor;
import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M: Set miter limit.
 *
 */
public class SetLineMiterLimit extends OperatorProcessor
{
    private static final Logger LOG = LoggerFactory.getLogger(SetLineMiterLimit.class);

    @Override
    public void process(Operator operator, List<COSBase> operands) throws IOException
    {
        require(!operands.isEmpty(), () -> new MissingOperandException(operator, operands));
        var base = operands.get(0);
        if (base instanceof COSNumber miter)
        {
            getContext().getGraphicsState().setMiterLimit(miter.floatValue());
        }
        else
        {
            LOG.warn("Invalid type {} operand for {} operator", base, SET_LINE_MITERLIMIT);
        }
    }

    @Override
    public String getName()
    {
        return SET_LINE_MITERLIMIT;
    }
}
