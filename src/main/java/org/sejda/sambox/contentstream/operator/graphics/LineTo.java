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
package org.sejda.sambox.contentstream.operator.graphics;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.List;

import org.sejda.sambox.contentstream.operator.MissingOperandException;
import org.sejda.sambox.contentstream.operator.Operator;
import org.sejda.sambox.contentstream.operator.OperatorName;
import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * l Append straight line segment to path.
 *
 * @author Ben Litchfield
 */
public class LineTo extends GraphicsOperatorProcessor
{
    private static final Logger LOG = LoggerFactory.getLogger(LineTo.class);

    @Override
    public void process(Operator operator, List<COSBase> operands) throws IOException
    {
        if (operands.size() < 2)
        {
            throw new MissingOperandException(operator, operands);
        }
        COSBase base0 = operands.get(0);
        if (!(base0 instanceof COSNumber x))
        {
            return;
        }
        COSBase base1 = operands.get(1);
        if (!(base1 instanceof COSNumber y))
        {
            return;
        }
        // append straight line segment from the current point to the point

        Point2D.Float pos = getContext().transformedPoint(x.floatValue(), y.floatValue());

        if (getContext().getCurrentPoint() == null)
        {
            LOG.warn("LineTo (" + pos.x + "," + pos.y + ") without initial MoveTo");
            getContext().moveTo(pos.x, pos.y);
        }
        else
        {
            getContext().lineTo(pos.x, pos.y);
        }
    }

    @Override
    public String getName()
    {
        return OperatorName.LINE_TO;
    }
}
